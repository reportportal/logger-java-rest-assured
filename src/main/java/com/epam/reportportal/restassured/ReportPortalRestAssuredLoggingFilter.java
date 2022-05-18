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

import com.epam.reportportal.internal.support.Prettiers;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.LogLevel;
import com.epam.reportportal.message.ReportPortalMessage;
import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.step.StepReporter;
import com.epam.reportportal.utils.files.Utils;
import com.google.common.io.ByteSource;
import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.restassured.response.ResponseBodyData;
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

/**
 * REST Assured Request/Response logging filter for Report Portal.
 *
 * The filter intercept and all Requests and Responses into Report Portal in Markdown format, including multipart requests. It recognizes
 * payload types and attach them in corresponding manner: image types will be logged as images with thumbnails, binary types will be logged
 * as entry attachments, text types will be formatted and logged in Markdown code blocks.
 *
 * Basic usage:
 * <pre>
 *
 * </pre>
 */
public class ReportPortalRestAssuredLoggingFilter implements OrderedFilter {

	private static final Set<String> MULTIPART_TYPES = Collections.singleton(ContentType.MULTIPART_FORM_DATA.getMimeType());

	private static final Set<String> TEXT_TYPES = new HashSet<>(Arrays.asList(ContentType.APPLICATION_JSON.getMimeType(),
			ContentType.TEXT_PLAIN.getMimeType(),
			ContentType.TEXT_HTML.getMimeType(),
			ContentType.TEXT_XML.getMimeType(),
			ContentType.APPLICATION_XML.getMimeType(),
			ContentType.DEFAULT_TEXT.getMimeType()
	));

	private static final Function<Header, String> DEFAULT_HEADER_CONVERTER = h -> h.getName() + ": " + h.getValue();

	private static final Function<Cookie, String> DEFAULT_COOKIE_CONVERTER = Cookie::toString;

	private static final Function<String, String> DEFAULT_URI_CONVERTER = u -> u;

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
	public static final String NULL_RESPONSE = "NULL response from RestAssured";
	public static final String BODY_TAG = "**Body**";
	public static final String BODY_PART_TAG = "**Body part**";
	public static final String HEADERS_TAG = "**Headers**";
	public static final String COOKIES_TAG = "**Cookies**";
	public static final String REQUEST_TAG = "**>>> REQUEST**";
	public static final String RESPONSE_TAG = "**<<< RESPONSE**";

	private final int order;
	private final String logLevel;

	private final Function<Header, String> headerConverter;
	private final Function<Cookie, String> cookieConverter;
	private final Function<String, String> uriConverter;

	private Set<String> textContentTypes = TEXT_TYPES;
	private Set<String> multipartContentTypes = MULTIPART_TYPES;

	private Map<String, Function<String, String>> contentPrettiers = DEFAULT_PRETTIERS;

	public ReportPortalRestAssuredLoggingFilter(int filterOrder, @Nonnull LogLevel defaultLogLevel,
			@Nullable Function<Header, String> headerConvertFunction, @Nullable Function<Cookie, String> cookieConvertFunction,
			@Nullable Function<String, String> uriConverterFunction) {
		order = filterOrder;
		logLevel = defaultLogLevel.name();
		headerConverter = headerConvertFunction;
		cookieConverter = cookieConvertFunction;
		uriConverter = uriConverterFunction;
	}

	public ReportPortalRestAssuredLoggingFilter(int filterOrder, @Nonnull LogLevel defaultLogLevel,
			@Nullable Function<Header, String> headerConvertFunction, @Nullable Function<Cookie, String> cookieConvertFunction) {
		this(filterOrder, defaultLogLevel, headerConvertFunction, cookieConvertFunction, DEFAULT_URI_CONVERTER);
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

	private String formatTextHeader(@Nullable Headers headers, @Nullable Cookies cookies) {
		StringBuilder result = new StringBuilder();
		String headersString = convertHeaders(headers);
		if (!headersString.isEmpty()) {
			result.append(HEADERS_TAG).append("\n").append(headersString);
		}
		String cookiesString = convertCookies(cookies);
		if (!cookiesString.isEmpty()) {
			result.append(COOKIES_TAG).append("\n").append(cookiesString);
		}
		return result.toString();
	}

	private String formatTextEntity(@Nonnull String entityName, @Nullable Headers headers, @Nullable Cookies cookies, @Nullable String body,
			@Nonnull String contentType) {
		String prefix = formatTextHeader(headers, cookies);
		String indent = prefix.isEmpty() ? entityName : "\n\n" + entityName;
		return ofNullable(body).map(b -> prefix + indent + "\n```\n" + (contentPrettiers.containsKey(contentType) ?
				contentPrettiers.get(contentType).apply(body) :
				body) + "\n```").orElse(prefix);
	}

	private void attachAsBinary(@Nullable String message, @Nullable byte[] attachment, @Nonnull String contentType) {
		if (attachment == null) {
			ReportPortal.emitLog(message, logLevel, new Date());
		} else {
			ReportPortal.emitLog(new ReportPortalMessage(ByteSource.wrap(attachment), contentType, message), logLevel, new Date());
		}
	}

	private void logMultiPartRequest(FilterableRequestSpecification request) {
		Date currentDate = new Date();
		String headers = formatTextHeader(request.getHeaders(), request.getCookies());
		if (!headers.isEmpty()) {
			ReportPortal.emitLog(formatTextHeader(request.getHeaders(), request.getCookies()), logLevel, currentDate);
		}
		request.getMultiPartParams().forEach(it -> {
			Date myDate = new Date(currentDate.getTime() + 1);
			String partMimeType = ofNullable(it.getMimeType()).orElse(ContentType.APPLICATION_OCTET_STREAM.getMimeType());
			if (TEXT_TYPES.contains(partMimeType)) {
				String body = it.getContent().toString();
				Headers partHeaders = new Headers(ofNullable(it.getHeaders()).map(headerMap -> headerMap.entrySet()
						.stream()
						.map(h -> new Header(h.getKey(), h.getValue()))
						.collect(Collectors.toList())).orElse(null));
				ReportPortal.emitLog(formatTextEntity(BODY_PART_TAG, partHeaders, null, it.getContent().toString(), body),
						logLevel,
						myDate
				);
			} else {
				Object body = it.getContent();
				if (body != null) {
					if (body instanceof File) {
						try {
							TypeAwareByteSource file = Utils.getFile((File) body);
							attachAsBinary(BODY_PART_TAG + "\n" + file.getMediaType(), file.read(), file.getMediaType());
						} catch (IOException exc) {
							ReportPortal.emitLog(BODY_PART_TAG + "\nUnable to read file: " + exc.getMessage(), "ERROR", currentDate);
						}
					} else {
						attachAsBinary(BODY_PART_TAG + "\n" + partMimeType, (byte[]) body, partMimeType);
					}
				} else {
					ReportPortal.emitLog(BODY_PART_TAG + "\nNULL", logLevel, currentDate);
				}
			}
		});
	}

	private void emitLog(FilterableRequestSpecification request) {
		String requestString = ofNullable(uriConverter).map(u -> String.format("%s to %s", request.getMethod(), u.apply(request.getURI())))
				.orElse(request.getMethod());
		String logText = REQUEST_TAG + "\n" + requestString;
		String rqContent = ofNullable(request.getContentType()).map(ct -> ContentType.parse(ct).getMimeType())
				.orElse(ContentType.APPLICATION_OCTET_STREAM.getMimeType());

		if (textContentTypes.contains(rqContent)) {
			String body = formatTextEntity(BODY_TAG, request.getHeaders(), request.getCookies(), request.getBody(), rqContent);
			String entry = body.isEmpty() ? logText + body : logText + "\n\n" + body;
			ReportPortal.emitLog(entry, logLevel, new Date());
		} else if (multipartContentTypes.contains(rqContent)) {
			if (!ofNullable(request.getMultiPartParams()).filter(p -> !p.isEmpty()).isPresent()) {
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
			ReportPortal.emitLog(NULL_RESPONSE, logLevel, new Date());
			return;
		}

		String logText = RESPONSE_TAG + "\n" + response.getStatusLine();
		String mimeType = ofNullable(response.getContentType()).map(ct -> ContentType.parse(ct).getMimeType())
				.orElse(ContentType.APPLICATION_OCTET_STREAM.getMimeType());
		if (TEXT_TYPES.contains(mimeType)) {
			String body = formatTextEntity(BODY_TAG,
					response.getHeaders(),
					response.getDetailedCookies(),
					ofNullable(response.getBody()).map(ResponseBodyData::asString).orElse(null),
					mimeType
			);
			String entry = body.isEmpty() ? logText : logText + "\n\n" + body;
			ReportPortal.emitLog(entry, logLevel, new Date());
		} else {
			attachAsBinary(logText, ofNullable(response.getBody()).map(ResponseBodyData::asByteArray).orElse(null), mimeType);
		}
	}

	@Override
	public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
		emitLog(requestSpec);
		Response response = ctx.next(requestSpec, responseSpec);
		emitLog(response);
		return response;
	}

	public ReportPortalRestAssuredLoggingFilter setTextContentTypes(Set<String> textContentTypes) {
		this.textContentTypes = textContentTypes;
		return this;
	}

	public ReportPortalRestAssuredLoggingFilter setMultipartContentTypes(Set<String> multipartContentTypes) {
		this.multipartContentTypes = multipartContentTypes;
		return this;
	}

	public ReportPortalRestAssuredLoggingFilter setContentPrettiers(Map<String, Function<String, String>> contentPrettiers) {
		this.contentPrettiers = contentPrettiers;
		return this;
	}
}
