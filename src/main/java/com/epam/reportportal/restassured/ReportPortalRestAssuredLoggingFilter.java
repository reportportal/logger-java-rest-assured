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

import com.epam.reportportal.formatting.AbstractHttpFormatter;
import com.epam.reportportal.formatting.http.converters.DefaultCookieConverter;
import com.epam.reportportal.formatting.http.converters.DefaultHttpHeaderConverter;
import com.epam.reportportal.formatting.http.converters.DefaultUriConverter;
import com.epam.reportportal.formatting.http.entities.BodyType;
import com.epam.reportportal.formatting.http.entities.Cookie;
import com.epam.reportportal.formatting.http.entities.Header;
import com.epam.reportportal.listeners.LogLevel;
import com.epam.reportportal.restassured.support.HttpEntityFactory;
import com.epam.reportportal.service.ReportPortal;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.epam.reportportal.formatting.http.Constants.REMOVED_TAG;
import static java.util.Optional.ofNullable;

/**
 * REST Assured Request/Response logging filter for Report Portal.
 * <p>
 * The filter intercept and logs all Requests and Responses issued by REST Assured into Report Portal in Markdown
 * format, including multipart requests. It recognizes payload types and attach them in corresponding manner: image
 * types will be logged as images with thumbnails, binary types will be logged as entry attachments, text types will be
 * formatted and logged in Markdown code blocks.
 * <p>
 * Basic usage:
 * <pre>
 *     RestAssured.filters(new ReportPortalRestAssuredLoggingFilter(42, LogLevel.INFO));
 * </pre>
 */
public class ReportPortalRestAssuredLoggingFilter extends AbstractHttpFormatter<ReportPortalRestAssuredLoggingFilter>
		implements OrderedFilter {

	public static final String NULL_RESPONSE = "NULL response from RestAssured";

	private final List<Predicate<FilterableRequestSpecification>> requestFilters = new CopyOnWriteArrayList<>();

	private final int order;

	/**
	 * Create an ordered REST Assured filter with the log level and different converters.
	 *
	 * @param filterOrder               if you have different filters which modify requests on fly this parameter allows
	 *                                  you to control the order when Report Portal logger will be called, and therefore
	 *                                  log or don't log some data.
	 * @param defaultLogLevel           log level on which REST Assured requests/responses will appear on Report Portal
	 * @param headerConvertFunction     if you want to preprocess your HTTP Headers before they appear on Report Portal
	 *                                  provide this custom function for the class, default function formats it like
	 *                                  that: <code>header.getName() + ": " + header.getValue()</code>
	 * @param partHeaderConvertFunction the same as fot HTTP Headers, but for parts in Multipart request
	 * @param cookieConvertFunction     the same as 'headerConvertFunction' param but for Cookies, default function
	 *                                  formats Cookies with <code>toString</code> method
	 * @param uriConverterFunction      the same as 'headerConvertFunction' param but for URI, default function returns
	 *                                  URI "as is"
	 */
	public ReportPortalRestAssuredLoggingFilter(int filterOrder, @Nonnull LogLevel defaultLogLevel,
			@Nullable Function<Header, String> headerConvertFunction, @Nullable Function<Header, String> partHeaderConvertFunction,
			@Nullable Function<Cookie, String> cookieConvertFunction, @Nullable Function<String, String> uriConverterFunction) {
		super(defaultLogLevel, headerConvertFunction, partHeaderConvertFunction, cookieConvertFunction, uriConverterFunction);
		order = filterOrder;
	}

	/**
	 * Create an ordered REST Assured filter with the log level and different converters.
	 *
	 * @param filterOrder               if you have different filters which modify requests on fly this parameter allows
	 *                                  you to control the order when Report Portal logger will be called, and therefore
	 *                                  log or don't log some data.
	 * @param defaultLogLevel           log level on which REST Assured requests/responses will appear on Report Portal
	 * @param headerConvertFunction     if you want to preprocess your HTTP Headers before they appear on Report Portal
	 *                                  provide this custom function for the class, default function formats it like
	 *                                  that: <code>header.getName() + ": " + header.getValue()</code>
	 * @param partHeaderConvertFunction the same as fot HTTP Headers, but for parts in Multipart request
	 * @param cookieConvertFunction     the same as 'headerConvertFunction' param but for Cookies, default function
	 *                                  formats Cookies with <code>toString</code> method
	 */
	public ReportPortalRestAssuredLoggingFilter(int filterOrder, @Nonnull LogLevel defaultLogLevel,
			@Nullable Function<Header, String> headerConvertFunction, @Nullable Function<Header, String> partHeaderConvertFunction,
			@Nullable Function<Cookie, String> cookieConvertFunction) {
		this(
				filterOrder,
				defaultLogLevel,
				headerConvertFunction,
				partHeaderConvertFunction,
				cookieConvertFunction,
				DefaultUriConverter.INSTANCE
		);
	}

	/**
	 * Create an ordered REST Assured filter with the log level and header converters.
	 *
	 * @param filterOrder               if you have different filters which modify requests on fly this parameter allows
	 *                                  you to control the order when Report Portal logger will be called, and therefore
	 *                                  log or don't log some data.
	 * @param defaultLogLevel           log level on which REST Assured requests/responses will appear on Report Portal
	 * @param headerConvertFunction     if you want to preprocess your HTTP Headers before they appear on Report Portal
	 *                                  provide this custom function for the class, default function formats it like
	 *                                  that: <code>header.getName() + ": " + header.getValue()</code>
	 * @param partHeaderConvertFunction the same as fot HTTP Headers, but for parts in Multipart request
	 */
	public ReportPortalRestAssuredLoggingFilter(int filterOrder, @Nonnull LogLevel defaultLogLevel,
			@Nullable Function<Header, String> headerConvertFunction, @Nullable Function<Header, String> partHeaderConvertFunction) {
		this(filterOrder, defaultLogLevel, headerConvertFunction, partHeaderConvertFunction, DefaultCookieConverter.INSTANCE);
	}

	/**
	 * Create an ordered REST Assured filter with the log level.
	 *
	 * @param filterOrder     if you have different filters which modify requests on fly this parameter allows
	 *                        you to control the order when Report Portal logger will be called, and therefore
	 *                        log or don't log some data.
	 * @param defaultLogLevel log level on which REST Assured requests/responses will appear on Report Portal
	 */
	public ReportPortalRestAssuredLoggingFilter(int filterOrder, @Nonnull LogLevel defaultLogLevel) {
		this(filterOrder, defaultLogLevel, DefaultHttpHeaderConverter.INSTANCE, DefaultHttpHeaderConverter.INSTANCE);
	}

	@Override
	public int getOrder() {
		return order;
	}

	@Override
	public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
		if (requestSpec == null || requestFilters.stream().anyMatch(f -> f.test(requestSpec))) {
			return ctx.next(requestSpec, responseSpec);
		}

		Set<String> blacklistedHeaders = ofNullable(requestSpec.getConfig()).map(RestAssuredConfig::getLogConfig)
				.map(LogConfig::blacklistedHeaders)
				.filter(headers -> !headers.isEmpty())
				.orElse(null);
		Function<Header, String> myHeaderConverter = headerConverter;
		if (blacklistedHeaders != null) {
			myHeaderConverter = header -> {
				if (!blacklistedHeaders.contains(header.getName())) {
					return headerConverter.apply(header);
				}
				Header newHeader = header.clone();
				newHeader.setValue(REMOVED_TAG);
				return headerConverter.apply(newHeader);
			};
		}

		emitLog(HttpEntityFactory.createHttpRequestFormatter(
				requestSpec,
				uriConverter,
				myHeaderConverter,
				cookieConverter,
				contentPrettiers,
				partHeaderConverter,
				bodyTypeMap
		));
		Response response = ctx.next(requestSpec, responseSpec);
		if (response == null) {
			ReportPortal.emitLog(NULL_RESPONSE, logLevel, Calendar.getInstance().getTime());
		} else {
			emitLog(HttpEntityFactory.createHttpResponseFormatter(
					response,
					headerConverter,
					cookieConverter,
					contentPrettiers,
					bodyTypeMap
			));
		}
		return response;
	}

	public ReportPortalRestAssuredLoggingFilter setBodyTypeMap(@Nonnull Map<String, BodyType> typeMap) {
		this.bodyTypeMap = Collections.unmodifiableMap(new HashMap<>(typeMap));
		return this;
	}

	public ReportPortalRestAssuredLoggingFilter setContentPrettiers(@Nonnull Map<String, Function<String, String>> contentPrettiers) {
		this.contentPrettiers = Collections.unmodifiableMap(new HashMap<>(contentPrettiers));
		return this;
	}

	public ReportPortalRestAssuredLoggingFilter addRequestFilter(@Nonnull Predicate<FilterableRequestSpecification> requestFilter) {
		requestFilters.add(requestFilter);
		return this;
	}
}
