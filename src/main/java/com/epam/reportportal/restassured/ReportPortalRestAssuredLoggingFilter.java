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

import com.epam.reportportal.formatting.http.HttpPartFormatter;
import com.epam.reportportal.formatting.http.HttpRequestFormatter;
import com.epam.reportportal.formatting.http.HttpResponseFormatter;
import com.epam.reportportal.formatting.http.converters.DefaultCookieConverter;
import com.epam.reportportal.formatting.http.converters.DefaultHttpHeaderConverter;
import com.epam.reportportal.formatting.http.converters.DefaultUriConverter;
import com.epam.reportportal.formatting.http.entities.BodyType;
import com.epam.reportportal.formatting.http.entities.Cookie;
import com.epam.reportportal.formatting.http.entities.Header;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.LogLevel;
import com.epam.reportportal.message.ReportPortalMessage;
import com.epam.reportportal.restassured.support.HttpEntityFactory;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.step.StepReporter;
import com.google.common.io.ByteSource;
import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

import static com.epam.reportportal.formatting.http.Constants.*;
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

	public static final String NULL_RESPONSE = "NULL response from RestAssured";

	private final int order;
	private final String logLevel;

	private final Function<Header, String> headerConverter;
	private final Function<Header, String> partHeaderConverter;
	private final Function<Cookie, String> cookieConverter;
	private final Function<String, String> uriConverter;
	private Map<String, Function<String, String>> contentPrettiers = DEFAULT_PRETTIERS;

	private Set<String> textContentTypes = TEXT_TYPES;
	private Set<String> multipartContentTypes = MULTIPART_TYPES;

	/**
	 * Create an ordered REST Assured filter with the specific log level and header converter.
	 *
	 * @param filterOrder               if you have different filters which modify requests on fly this parameter allows you to control the
	 *                                  order when Report Portal logger will be called, and therefore log or don't log some data.
	 * @param defaultLogLevel           log leve on which REST Assured requests/responses will appear on Report Portal
	 * @param headerConvertFunction     if you want to preprocess your HTTP Headers before they appear on Report Portal provide this custom
	 *                                  function for the class, default function formats it like that:
	 *                                  <code>header.getName() + ": " + header.getValue()</code>
	 * @param partHeaderConvertFunction the same as fot HTTP Headers, but for parts in Multipart request
	 * @param cookieConvertFunction     the same as 'headerConvertFunction' param but for Cookies, default function formats Cookies with
	 *                                  <code>toString</code> method
	 * @param uriConverterFunction      the same as 'headerConvertFunction' param but for URI, default function returns URI "as is"
	 */
	public ReportPortalRestAssuredLoggingFilter(int filterOrder, @Nonnull LogLevel defaultLogLevel,
			@Nullable Function<Header, String> headerConvertFunction,
			@Nullable Function<Header, String> partHeaderConvertFunction,
			@Nullable Function<Cookie, String> cookieConvertFunction,
			@Nullable Function<String, String> uriConverterFunction) {
		order = filterOrder;
		logLevel = defaultLogLevel.name();
		headerConverter = headerConvertFunction;
		partHeaderConverter = partHeaderConvertFunction;
		cookieConverter = cookieConvertFunction;
		uriConverter = uriConverterFunction;
	}

	/**
	 * Create an ordered REST Assured filter with the specific log level and header converter.
	 *
	 * @param filterOrder               if you have different filters which modify requests on fly this parameter allows you to control the
	 *                                  order when Report Portal logger will be called, and therefore log or don't log some data.
	 * @param defaultLogLevel           log leve on which REST Assured requests/responses will appear on Report Portal
	 * @param headerConvertFunction     if you want to preprocess your HTTP Headers before they appear on Report Portal provide this custom
	 *                                  function for the class, default function formats it like that:
	 *                                  <code>header.getName() + ": " + header.getValue()</code>
	 * @param partHeaderConvertFunction the same as fot HTTP Headers, but for parts in Multipart request
	 * @param cookieConvertFunction     the same as 'headerConvertFunction' param but for Cookies, default function formats Cookies with
	 *                                  <code>toString</code> method
	 */
	public ReportPortalRestAssuredLoggingFilter(int filterOrder, @Nonnull LogLevel defaultLogLevel,
			@Nullable Function<Header, String> headerConvertFunction,
			@Nullable Function<Header, String> partHeaderConvertFunction,
			@Nullable Function<Cookie, String> cookieConvertFunction) {
		this(filterOrder,
				defaultLogLevel,
				headerConvertFunction,
				partHeaderConvertFunction,
				cookieConvertFunction,
				DefaultUriConverter.INSTANCE
		);
	}

	/**
	 * Create an ordered REST Assured filter with the specific log level and header converter.
	 *
	 * @param filterOrder               if you have different filters which modify requests on fly this parameter allows you to control the
	 *                                  order when Report Portal logger will be called, and therefore log or don't log some data.
	 * @param defaultLogLevel           log leve on which REST Assured requests/responses will appear on Report Portal
	 * @param headerConvertFunction     if you want to preprocess your HTTP Headers before they appear on Report Portal provide this custom
	 *                                  function for the class, default function formats it like that:
	 *                                  <code>header.getName() + ": " + header.getValue()</code>
	 * @param partHeaderConvertFunction the same as fot HTTP Headers, but for parts in Multipart request
	 */
	public ReportPortalRestAssuredLoggingFilter(int filterOrder, @Nonnull LogLevel defaultLogLevel,
			@Nullable Function<Header, String> headerConvertFunction,
			@Nullable Function<Header, String> partHeaderConvertFunction) {
		this(filterOrder,
				defaultLogLevel,
				headerConvertFunction,
				partHeaderConvertFunction,
				DefaultCookieConverter.INSTANCE
		);
	}

	/**
	 * Create an ordered REST Assured filter with the specific log level and header converter.
	 *
	 * @param filterOrder     if you have different filters which modify requests on fly this parameter allows you to control the
	 *                        order when Report Portal logger will be called, and therefore log or don't log some data.
	 * @param defaultLogLevel log leve on which REST Assured requests/responses will appear on Report Portal
	 */
	public ReportPortalRestAssuredLoggingFilter(int filterOrder, @Nonnull LogLevel defaultLogLevel) {
		this(filterOrder, defaultLogLevel, DefaultHttpHeaderConverter.INSTANCE, DefaultHttpHeaderConverter.INSTANCE);
	}

	@Override
	public int getOrder() {
		return order;
	}

	private void attachAsBinary(@Nullable String message, @Nullable byte[] attachment, @Nonnull String contentType) {
		if (attachment == null) {
			ReportPortal.emitLog(message, logLevel, Calendar.getInstance().getTime());
		} else {
			ReportPortal.emitLog(new ReportPortalMessage(ByteSource.wrap(attachment), contentType, message),
					logLevel,
					Calendar.getInstance().getTime()
			);
		}
	}

	private void logMultiPartRequest(@Nonnull HttpRequestFormatter formatter) {
		Date currentDate = Calendar.getInstance().getTime();
		String headers = formatter.formatHeaders() + formatter.formatCookies();
		if (!headers.isEmpty()) {
			ReportPortal.emitLog(headers, logLevel, currentDate);
		}

		Date myDate = currentDate;
		for (HttpPartFormatter part : formatter.getMultipartBody()) {
			myDate = new Date(myDate.getTime() + 1);
			HttpPartFormatter.PartType partType = part.getType();
			switch (partType) {
				case TEXT:
					ReportPortal.emitLog(part.formatAsText(), logLevel, myDate);
					break;
				case BINARY:
					attachAsBinary(part.formatForBinaryDataPrefix(), part.getBinaryPayload(), part.getMimeType());
			}
		}
	}

	private void emitLog(@Nonnull FilterableRequestSpecification request) {
		HttpRequestFormatter formatter = HttpEntityFactory.createHttpRequestFormatter(request,
				uriConverter,
				headerConverter,
				cookieConverter,
				contentPrettiers,
				partHeaderConverter,
				textContentTypes,
				multipartContentTypes
		);

		BodyType type = formatter.getType();
		switch (type) {
			case NONE:
				ReportPortal.emitLog(formatter.formatHead(), logLevel, Calendar.getInstance().getTime());
				break;
			case TEXT:
				ReportPortal.emitLog(formatter.formatAsText(), logLevel, Calendar.getInstance().getTime());
				break;
			case BINARY:
				attachAsBinary(formatter.formatHead(), formatter.getBinaryBody(), formatter.getMimeType());
				break;
			case MULTIPART:
				Optional<StepReporter> sr = ofNullable(Launch.currentLaunch()).map(Launch::getStepReporter);
				sr.ifPresent(r -> r.sendStep(ItemStatus.INFO, formatter.formatRequest()));
				logMultiPartRequest(formatter);
				sr.ifPresent(StepReporter::finishPreviousStep);
		}
	}

	private void emitLog(@Nullable Response response) {
		if (response == null) {
			ReportPortal.emitLog(NULL_RESPONSE, logLevel, Calendar.getInstance().getTime());
			return;
		}

		HttpResponseFormatter formatter = HttpEntityFactory.createHttpResponseFormatter(response,
				headerConverter,
				cookieConverter,
				contentPrettiers,
				textContentTypes
		);
		BodyType type = formatter.getType();
		switch (type) {
			case NONE:
				ReportPortal.emitLog(formatter.formatHead(), logLevel, Calendar.getInstance().getTime());
				break;
			case TEXT:
				ReportPortal.emitLog(formatter.formatAsText(), logLevel, Calendar.getInstance().getTime());
				break;
			case BINARY:
				attachAsBinary(formatter.formatHead(), formatter.getBinaryBody(), formatter.getMimeType());
				break;
			default:
				ReportPortal.emitLog("Unknown response type: " + type.name(),
						LogLevel.ERROR.name(),
						Calendar.getInstance().getTime()
				);
		}
	}

	@Override
	public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec,
			FilterContext ctx) {
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

	public ReportPortalRestAssuredLoggingFilter setContentPrettiers(
			@Nonnull Map<String, Function<String, String>> contentPrettiers) {
		this.contentPrettiers = contentPrettiers;
		return this;
	}
}
