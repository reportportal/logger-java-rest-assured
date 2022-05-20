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
import com.epam.reportportal.restassured.support.Converters;
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
 * <p>
 * The filter intercept and logs all Requests and Responses issued by REST Assured into Report Portal in Markdown format, including
 * multipart requests. It recognizes payload types and attach them in corresponding manner: image types will be logged as images with
 * thumbnails, binary types will be logged as entry attachments, text types will be formatted and logged in Markdown code blocks.
 * <p>
 * Basic usage:
 * <pre>
 *     RestAssured.filters(new ReportPortalRestAssuredLoggingFilter(42, LogLevel.INFO));
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

	private static final Map<String, Function<String, String>> DEFAULT_PRETTIERS = new HashMap<String, Function<String, String>>() {{
		put(ContentType.APPLICATION_XML.getMimeType(), Converters.XML_PRETTIER);
		put(ContentType.APPLICATION_SOAP_XML.getMimeType(), Converters.XML_PRETTIER);
		put(ContentType.APPLICATION_ATOM_XML.getMimeType(), Converters.XML_PRETTIER);
		put(ContentType.APPLICATION_SVG_XML.getMimeType(), Converters.XML_PRETTIER);
		put(ContentType.APPLICATION_XHTML_XML.getMimeType(), Converters.XML_PRETTIER);
		put(ContentType.TEXT_XML.getMimeType(), Converters.XML_PRETTIER);
		put(ContentType.APPLICATION_JSON.getMimeType(), Converters.JSON_PRETTIER);
		put("text/json", Converters.JSON_PRETTIER);
		put(ContentType.TEXT_HTML.getMimeType(), Converters.HTML_PRETTIER);
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

	@Nonnull
	private Set<String> textContentTypes = TEXT_TYPES;
	@Nonnull
	private Set<String> multipartContentTypes = MULTIPART_TYPES;
	@Nonnull
	private Map<String, Function<String, String>> contentPrettiers = DEFAULT_PRETTIERS;

	/**
	 * Create an ordered REST Assured filter with the specific log level and header converter.
	 *
	 * @param filterOrder           if you have different filters which modify requests on fly this parameter allows you to control the
	 *                              order when Report Portal logger will be called, and therefore log or don't log some data.
	 * @param defaultLogLevel       log leve on which REST Assured requests/responses will appear on Report Portal
	 * @param headerConvertFunction if you want to preprocess your HTTP Headers before they appear on Report Portal provide this custom
	 *                              function for the class, default function formats it like that:
	 *                              <code>header.getName() + ": " + header.getValue()</code>
	 * @param cookieConvertFunction the same as 'headerConvertFunction' param but for Cookies, default function formats Cookies with
	 *                              <code>toString</code> method
	 * @param uriConverterFunction  the same as 'headerConvertFunction' param but for URI, default function returns URI "as is"
	 */
	public ReportPortalRestAssuredLoggingFilter(int filterOrder, @Nonnull LogLevel defaultLogLevel,
			@Nullable Function<Header, String> headerConvertFunction, @Nullable Function<Cookie, String> cookieConvertFunction,
			@Nullable Function<String, String> uriConverterFunction) {
		order = filterOrder;
		logLevel = defaultLogLevel.name();
		headerConverter = headerConvertFunction;
		cookieConverter = cookieConvertFunction;
		uriConverter = uriConverterFunction;
	}

	/**
	 * Create an ordered REST Assured filter with the specific log level and header converter.
	 *
	 * @param filterOrder           if you have different filters which modify requests on fly this parameter allows you to control the
	 *                              order when Report Portal logger will be called, and therefore log or don't log some data.
	 * @param defaultLogLevel       log leve on which REST Assured requests/responses will appear on Report Portal
	 * @param headerConvertFunction if you want to preprocess your HTTP Headers before they appear on Report Portal provide this custom
	 *                              function for the class, default function formats it like that:
	 *                              <code>header.getName() + ": " + header.getValue()</code>
	 * @param cookieConvertFunction the same as 'headerConvertFunction' param but for Cookies, default function formats Cookies with
	 *                              <code>toString</code> method
	 */
	public ReportPortalRestAssuredLoggingFilter(int filterOrder, @Nonnull LogLevel defaultLogLevel,
			@Nullable Function<Header, String> headerConvertFunction, @Nullable Function<Cookie, String> cookieConvertFunction) {
		this(filterOrder, defaultLogLevel, headerConvertFunction, cookieConvertFunction, Converters.DEFAULT_URI_CONVERTER);
	}

	/**
	 * Create an ordered REST Assured filter with the specific log level and header converter.
	 *
	 * @param filterOrder           if you have different filters which modify requests on fly this parameter allows you to control the
	 *                              order when Report Portal logger will be called, and therefore log or don't log some data.
	 * @param defaultLogLevel       log leve on which REST Assured requests/responses will appear on Report Portal
	 * @param headerConvertFunction if you want to preprocess your HTTP Headers before they appear on Report Portal provide this custom
	 *                              function for the class, default function formats it like that:
	 *                              <code>header.getName() + ": " + header.getValue()</code>
	 */
	public ReportPortalRestAssuredLoggingFilter(int filterOrder, @Nonnull LogLevel defaultLogLevel,
			@Nullable Function<Header, String> headerConvertFunction) {
		this(filterOrder, defaultLogLevel, headerConvertFunction, Converters.DEFAULT_COOKIE_CONVERTER);
	}

	/**
	 * Create an ordered REST Assured filter with the specific log level and header converter.
	 *
	 * @param filterOrder     if you have different filters which modify requests on fly this parameter allows you to control the
	 *                        order when Report Portal logger will be called, and therefore log or don't log some data.
	 * @param defaultLogLevel log leve on which REST Assured requests/responses will appear on Report Portal
	 */
	public ReportPortalRestAssuredLoggingFilter(int filterOrder, @Nonnull LogLevel defaultLogLevel) {
		this(filterOrder, defaultLogLevel, Converters.DEFAULT_HEADER_CONVERTER);
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
			if (!headersString.isEmpty()) {
				result.append("\n\n");
			}
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

	private void logMultiPartRequest(@Nonnull FilterableRequestSpecification request) {
		Date currentDate = new Date();
		String headers = formatTextHeader(request.getHeaders(), request.getCookies());
		if (!headers.isEmpty()) {
			ReportPortal.emitLog(formatTextHeader(request.getHeaders(), request.getCookies()), logLevel, currentDate);
		}
		request.getMultiPartParams().forEach(it -> {
			Date myDate = new Date(currentDate.getTime() + 1);
			String partMimeType = ofNullable(it.getMimeType()).orElse(ContentType.APPLICATION_OCTET_STREAM.getMimeType());
			Headers partHeaders = ofNullable(it.getHeaders()).map(headerMap -> headerMap.entrySet()
					.stream()
					.map(h -> new Header(h.getKey(), h.getValue()))
					.collect(Collectors.toList())).map(Headers::new).orElse(null);
			if (TEXT_TYPES.contains(partMimeType)) {
				String body = it.getContent().toString();
				ReportPortal.emitLog(formatTextEntity(BODY_PART_TAG, partHeaders, null, it.getContent().toString(), body),
						logLevel,
						myDate
				);
			} else {
				String prefix = formatTextHeader(partHeaders, null);
				Object body = it.getContent();
				String logText = BODY_PART_TAG + "\n";
				String entry = prefix.isEmpty() ? logText : prefix + "\n\n" + logText;
				if (body != null) {
					if (body instanceof File) {
						try {
							TypeAwareByteSource file = Utils.getFile((File) body);
							attachAsBinary(entry + file.getMediaType(), file.read(), file.getMediaType());
						} catch (IOException exc) {
							ReportPortal.emitLog(entry + "Unable to read file: " + exc.getMessage(), "ERROR", currentDate);
						}
					} else {
						attachAsBinary(entry + partMimeType, (byte[]) body, partMimeType);
					}
				} else {
					ReportPortal.emitLog(entry + "NULL", logLevel, currentDate);
				}
			}
		});
	}

	private String getMimeType(@Nullable String contentType) {
		return ofNullable(contentType).filter(ct -> !ct.isEmpty())
				.map(ct -> ContentType.parse(contentType).getMimeType())
				.orElse(ContentType.APPLICATION_OCTET_STREAM.getMimeType());
	}

	private void emitLog(@Nonnull FilterableRequestSpecification request) {
		String requestString = ofNullable(uriConverter).map(u -> String.format("%s to %s", request.getMethod(), u.apply(request.getURI())))
				.orElse(request.getMethod());
		String logText = REQUEST_TAG + "\n" + requestString;
		String rqContent = getMimeType(request.getContentType());

		if (textContentTypes.contains(rqContent)) {
			String body = formatTextEntity(BODY_TAG, request.getHeaders(), request.getCookies(), request.getBody(), rqContent);
			String entry = body.isEmpty() ? logText : logText + "\n\n" + body;
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
			String prefix = formatTextHeader(request.getHeaders(), request.getCookies());
			String entry = prefix.isEmpty() ? logText : logText + "\n\n" + prefix;
			attachAsBinary(entry, request.getBody(), rqContent);
		}
	}

	private void emitLog(@Nullable Response response) {
		if (response == null) {
			ReportPortal.emitLog(NULL_RESPONSE, logLevel, new Date());
			return;
		}

		String logText = RESPONSE_TAG + "\n" + response.getStatusLine();
		String mimeType = getMimeType(response.getContentType());
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
			String prefix = formatTextHeader(response.getHeaders(), response.getDetailedCookies());
			String entry = prefix.isEmpty() ? logText : logText + "\n\n" + prefix;
			attachAsBinary(entry, ofNullable(response.getBody()).map(ResponseBodyData::asByteArray).orElse(null), mimeType);
		}
	}

	@Override
	public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
		emitLog(requestSpec);
		Response response = ctx.next(requestSpec, responseSpec);
		emitLog(response);
		return response;
	}

	public ReportPortalRestAssuredLoggingFilter setTextContentTypes(@Nonnull Set<String> textContentTypes) {
		this.textContentTypes = textContentTypes;
		return this;
	}

	public ReportPortalRestAssuredLoggingFilter setMultipartContentTypes(@Nonnull Set<String> multipartContentTypes) {
		this.multipartContentTypes = multipartContentTypes;
		return this;
	}

	public ReportPortalRestAssuredLoggingFilter setContentPrettiers(@Nonnull Map<String, Function<String, String>> contentPrettiers) {
		this.contentPrettiers = contentPrettiers;
		return this;
	}
}
