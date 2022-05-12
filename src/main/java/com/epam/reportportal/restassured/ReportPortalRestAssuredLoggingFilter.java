/*
 * Copyright 2022 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.restassured;

import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.message.ReportPortalMessage;
import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.step.StepReporter;
import com.epam.reportportal.utils.files.Utils;
import com.google.common.io.ByteSource;
import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.http.Cookies;
import io.restassured.http.Headers;
import io.restassured.internal.support.Prettifier;
import io.restassured.parsing.Parser;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.apache.http.entity.ContentType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Optional.ofNullable;

public class ReportPortalRestAssuredLoggingFilter implements OrderedFilter {

	private static final String MULTIPART_TYPE = ContentType.MULTIPART_FORM_DATA.getMimeType();

	private static final Set<String> TEXT_CONTENT_TYPES = new HashSet<>(Arrays.asList(ContentType.APPLICATION_JSON.getMimeType(),
			ContentType.TEXT_PLAIN.getMimeType(),
			ContentType.TEXT_HTML.getMimeType(),
			ContentType.TEXT_XML.getMimeType(),
			ContentType.APPLICATION_XML.getMimeType(),
			ContentType.DEFAULT_TEXT.getMimeType()
	));

	private static final Set<String> IMAGE_CONTENT_TYPES = new HashSet<>(Arrays.asList(ContentType.IMAGE_BMP.getMimeType(),
			ContentType.IMAGE_GIF.getMimeType(),
			ContentType.IMAGE_JPEG.getMimeType(),
			ContentType.IMAGE_PNG.getMimeType(),
			ContentType.IMAGE_SVG.getMimeType(),
			ContentType.IMAGE_TIFF.getMimeType(),
			ContentType.IMAGE_WEBP.getMimeType()
	));

	private static final String REMOVED_TAG = "&lt;removed&gt;";

	private final int order;
	private final String logLevel;

	public ReportPortalRestAssuredLoggingFilter(int filterOrder, String defaultLogLevel) {
		order = filterOrder;
		logLevel = defaultLogLevel;
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Nonnull
	private String convertHeaders(@Nullable Headers headers) {
		return ofNullable(headers).map(nonnullHeaders -> StreamSupport.stream(nonnullHeaders.spliterator(), false).map(h -> {
			String value = "Authorization".equals(h.getName()) ? REMOVED_TAG : h.getValue();
			return h.getName() + ": " + value;
		}).collect(Collectors.joining("\n"))).orElse("");
	}

	private String formatTextHeader(Headers headers, Cookies cookies) {
		String result = "";
		String headersString = convertHeaders(headers);
		if (!headersString.isEmpty()) {
			result += "\n\n**Headers**\n" + headersString;
		}
		String cookiesString = ofNullable(cookies).map(Cookies::toString).orElse("");
		if (!cookiesString.isEmpty()) {
			result += "\n\n**Cookies**\n" + cookiesString;
		}
		return result;
	}

	private String formatTextEntity(Headers headers, Cookies cookies, String body, String contentType) {
		String result = formatTextHeader(headers, cookies);
		return ofNullable(body).map(b -> result + "\n\n**Body**\n```\n" + new Prettifier().prettify(body,
				Parser.fromContentType(contentType)
		) + "\n```").orElse(result);
	}

	private void attachAsBinary(String message, byte[] attachment, String contentType) {
		if (attachment == null) {
			ReportPortal.emitLog(message, logLevel, new Date());
		} else {
			ReportPortal.emitLog(new ReportPortalMessage(ByteSource.wrap(attachment), contentType, message), logLevel, new Date());
		}
	}

	private void emitLog(FilterableRequestSpecification request) {
		String logText = String.format("**>>> REQUEST**" + "\n%s to %s", request.getMethod(), request.getURI());
		String rqContent = ContentType.parse(request.getContentType()).getMimeType();

		if (TEXT_CONTENT_TYPES.contains(rqContent)) {
			ReportPortal.emitLog(logText + formatTextEntity(request.getHeaders(), request.getCookies(), request.getBody(), rqContent),
					logLevel,
					new Date()
			);
		} else if (IMAGE_CONTENT_TYPES.contains(rqContent)) {
			attachAsBinary(logText, request.getBody(), rqContent);
		} else if (MULTIPART_TYPE.equals(rqContent)) {
			if (request.getMultiPartParams().isEmpty()) {
				ReportPortal.emitLog(logText + formatTextHeader(request.getHeaders(), request.getCookies()), logLevel, new Date());
				return;
			}

			StepReporter sr = ofNullable(Launch.currentLaunch()).map(Launch::getStepReporter).orElse(null);
			ofNullable(sr).ifPresent(r -> r.sendStep(ItemStatus.INFO, logText));
			Date currentDate = new Date();
			ReportPortal.emitLog(formatTextHeader(request.getHeaders(), request.getCookies()), logLevel, currentDate);
			request.getMultiPartParams().forEach(it -> {
				Date myDate = new Date(currentDate.getTime() + 1);
				String partMimeType = it.getMimeType();
				if (TEXT_CONTENT_TYPES.contains(partMimeType)) {
					ReportPortal.emitLog(
							"**Body part**\n" + new Prettifier().prettify(it.getContent().toString(),
									Parser.fromContentType(it.getMimeType())
							),
							logLevel,
							myDate
					);
				} else {
					Object body = it.getContent();
					if (body != null) {
						if (body instanceof File) {
							try {
								TypeAwareByteSource file = Utils.getFile((File) body);
								attachAsBinary("**Body part**\n" + file.getMediaType(), file.read(), file.getMediaType());
							} catch (IOException exc) {
								ReportPortal.emitLog("**Body part**\nUnable to read file: " + exc.getMessage(), "ERROR", currentDate);
							}
						} else {
							attachAsBinary("**Body part**\n" + rqContent, (byte[]) body, rqContent);
						}
					} else {
						ReportPortal.emitLog("**Body part**\nNULL", logLevel, currentDate);
					}
				}
			});
			ofNullable(sr).ifPresent(StepReporter::finishPreviousStep);
		} else {
			attachAsBinary(logText, request.getBody(), rqContent);
		}
	}

	private void emitLog(Response response) {
		if (response == null) {
			ReportPortal.emitLog("NULL response from RestAssured", logLevel, new Date());
			return;
		}

		String logText = "**<<< RESPONSE**" + "\n" + response.getStatusLine();
		String mimeType = ContentType.parse(response.getContentType()).getMimeType();
		if (TEXT_CONTENT_TYPES.contains(mimeType)) {
			logText += formatTextEntity(response.getHeaders(), response.getDetailedCookies(), response.getBody().asString(), mimeType);
			ReportPortal.emitLog(logText, logLevel, new Date());
		} else {
			attachAsBinary(logText, response.getBody().asByteArray(), mimeType);
		}
	}

	@Override
	public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
		emitLog(requestSpec);
		Response response = ctx.next(requestSpec, responseSpec);
		emitLog(response);
		return response;
	}
}
