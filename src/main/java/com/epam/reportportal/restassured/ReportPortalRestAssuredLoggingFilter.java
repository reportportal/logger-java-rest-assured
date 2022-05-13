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
import com.epam.reportportal.listeners.LogLevel;
import com.epam.reportportal.message.ReportPortalMessage;
import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.step.StepReporter;
import com.epam.reportportal.support.Prettiers;
import com.epam.reportportal.utils.files.Utils;
import com.google.common.io.ByteSource;
import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.http.Header;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Optional.ofNullable;

public class ReportPortalRestAssuredLoggingFilter implements OrderedFilter {

	private static final Set<String> MULTIPART_TYPES = Collections.singleton(ContentType.MULTIPART_FORM_DATA.getMimeType());

	private static final Set<String> TEXT_TYPES = new HashSet<>(Arrays.asList(ContentType.APPLICATION_JSON.getMimeType(),
			ContentType.TEXT_PLAIN.getMimeType(),
			ContentType.TEXT_HTML.getMimeType(),
			ContentType.TEXT_XML.getMimeType(),
			ContentType.APPLICATION_XML.getMimeType(),
			ContentType.DEFAULT_TEXT.getMimeType()
	));

	private static final Set<String> IMAGE_TYPES = new HashSet<>(Arrays.asList(ContentType.IMAGE_BMP.getMimeType(),
			ContentType.IMAGE_GIF.getMimeType(),
			ContentType.IMAGE_JPEG.getMimeType(),
			ContentType.IMAGE_PNG.getMimeType(),
			ContentType.IMAGE_SVG.getMimeType(),
			ContentType.IMAGE_TIFF.getMimeType(),
			ContentType.IMAGE_WEBP.getMimeType()
	));

	private static final Function<Header, String> DEFAULT_HEADER_CONVERTER = h -> h.getName() + ": " + h.getValue();

	private static final Function<Cookie, String> DEFAULT_COOKIE_CONVERTER = Cookie::toString;

	private static final Map<String, Function<String, String>> DEFAULT_PRETTIERS = new HashMap<String, Function<String, String>>() {{
		put(ContentType.APPLICATION_XML.getMimeType(), Prettiers.XML_PRETTIER);
		put(ContentType.APPLICATION_SOAP_XML.getMimeType(), Prettiers.XML_PRETTIER);
		put(ContentType.APPLICATION_ATOM_XML.getMimeType(), Prettiers.XML_PRETTIER);
		put(ContentType.APPLICATION_SVG_XML.getMimeType(), Prettiers.XML_PRETTIER);
		put(ContentType.APPLICATION_XHTML_XML.getMimeType(), Prettiers.XML_PRETTIER);
		put(ContentType.TEXT_XML.getMimeType(), Prettiers.XML_PRETTIER);
		put(ContentType.APPLICATION_JSON.getMimeType(), Prettiers.JSON_PRETTIER);
		put("text/json", Prettiers.JSON_PRETTIER);
		put(ContentType.TEXT_HTML.getMimeType(), Prettiers.HTML_PRETTIER);
	}};

	private final int order;
	private final String logLevel;

	private final Function<Header, String> headerConverter;
	private final Function<Cookie, String> cookieConverter;

	private Set<String> textContentTypes = TEXT_TYPES;
	private Set<String> imageContentTypes = IMAGE_TYPES;
	private Set<String> multipartContentTypes = MULTIPART_TYPES;

	private Map<String, Function<String, String>> contentPrettiers = DEFAULT_PRETTIERS;

	public ReportPortalRestAssuredLoggingFilter(int filterOrder, @Nonnull LogLevel defaultLogLevel,
			@Nullable Function<Header, String> headerConvertFunction, @Nullable Function<Cookie, String> cookieConvertFunction) {
		order = filterOrder;
		logLevel = defaultLogLevel.name();
		headerConverter = headerConvertFunction;
		cookieConverter = cookieConvertFunction;
	}

	public ReportPortalRestAssuredLoggingFilter(int filterOrder, @Nonnull LogLevel defaultLogLevel,
			@Nullable Function<Header, String> headerConvertFunction) {
		this(filterOrder, defaultLogLevel, headerConvertFunction, DEFAULT_COOKIE_CONVERTER);
	}

	public ReportPortalRestAssuredLoggingFilter(int filterOrder, @Nonnull LogLevel defaultLogLevel) {
		this(filterOrder, defaultLogLevel, DEFAULT_HEADER_CONVERTER, DEFAULT_COOKIE_CONVERTER);
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Nonnull
	private String convertHeaders(@Nullable Headers headers) {
		if (headerConverter == null) {
			return "";
		}

		return ofNullable(headers).map(nonnullHeaders -> StreamSupport.stream(nonnullHeaders.spliterator(), false)
				.map(headerConverter)
				.filter(Objects::nonNull)
				.collect(Collectors.joining("\n"))).orElse("");
	}

	@Nonnull
	private String convertCookies(@Nullable Cookies cookies) {
		if (cookieConverter == null) {
			return "";
		}

		return ofNullable(cookies).map(nonnullCookies -> StreamSupport.stream(cookies.spliterator(), false)
				.map(cookieConverter)
				.filter(Objects::nonNull)
				.collect(Collectors.joining("\n"))).orElse("");
	}

	private String formatTextHeader(Headers headers, Cookies cookies) {
		StringBuilder result = new StringBuilder();
		String headersString = convertHeaders(headers);
		if (!headersString.isEmpty()) {
			result.append("\n\n**Headers**\n").append(headersString);
		}
		String cookiesString = convertCookies(cookies);
		if (!cookiesString.isEmpty()) {
			result.append("\n\n**Cookies**\n").append(cookiesString);
		}
		return result.toString();
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

	private void logMultiPartRequest(FilterableRequestSpecification request) {
		Date currentDate = new Date();
		ReportPortal.emitLog(formatTextHeader(request.getHeaders(), request.getCookies()), logLevel, currentDate);
		request.getMultiPartParams().forEach(it -> {
			Date myDate = new Date(currentDate.getTime() + 1);
			String partMimeType = it.getMimeType();
			if (TEXT_TYPES.contains(partMimeType)) {
				String body = it.getContent().toString();
				ReportPortal.emitLog("**Body part**\n" + (contentPrettiers.containsKey(partMimeType) ?
						contentPrettiers.get(partMimeType).apply(body) :
						body), logLevel, myDate);
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
						attachAsBinary("**Body part**\n" + partMimeType, (byte[]) body, partMimeType);
					}
				} else {
					ReportPortal.emitLog("**Body part**\nNULL", logLevel, currentDate);
				}
			}
		});
	}

	private void emitLog(FilterableRequestSpecification request) {
		String logText = String.format("**>>> REQUEST**" + "\n%s to %s", request.getMethod(), request.getURI());
		String rqContent = ContentType.parse(request.getContentType()).getMimeType();

		if (textContentTypes.contains(rqContent)) {
			ReportPortal.emitLog(logText + formatTextEntity(request.getHeaders(), request.getCookies(), request.getBody(), rqContent),
					logLevel,
					new Date()
			);
		} else if (imageContentTypes.contains(rqContent)) {
			attachAsBinary(logText, request.getBody(), rqContent);
		} else if (multipartContentTypes.contains(rqContent)) {
			if (request.getMultiPartParams().isEmpty()) {
				ReportPortal.emitLog(logText + formatTextHeader(request.getHeaders(), request.getCookies()), logLevel, new Date());
				return;
			}

			Optional<StepReporter> sr = ofNullable(Launch.currentLaunch()).map(Launch::getStepReporter);
			sr.ifPresent(r -> r.sendStep(ItemStatus.INFO, logText));
			logMultiPartRequest(request);
			sr.ifPresent(StepReporter::finishPreviousStep);
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
		if (TEXT_TYPES.contains(mimeType)) {
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

	public void setTextContentTypes(Set<String> textContentTypes) {
		this.textContentTypes = textContentTypes;
	}

	public void setImageContentTypes(Set<String> imageContentTypes) {
		this.imageContentTypes = imageContentTypes;
	}

	public void setMultipartContentTypes(Set<String> multipartContentTypes) {
		this.multipartContentTypes = multipartContentTypes;
	}

	public void setContentPrettiers(Map<String, Function<String, String>> contentPrettiers) {
		this.contentPrettiers = contentPrettiers;
	}
}
