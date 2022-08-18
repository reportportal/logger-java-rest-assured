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

package com.epam.reportportal.restassured.support;

import com.epam.reportportal.restassured.support.converters.DefaultCookieConverter;
import com.epam.reportportal.restassured.support.converters.DefaultHttpHeaderConverter;
import com.epam.reportportal.restassured.support.converters.DefaultUriConverter;
import com.epam.reportportal.restassured.support.http.Cookie;
import com.epam.reportportal.restassured.support.http.Header;
import com.epam.reportportal.service.ReportPortal;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import org.apache.http.entity.ContentType;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Map;
import java.util.function.Function;

import static com.epam.reportportal.restassured.support.Constants.MULTIPART_TYPES;
import static com.epam.reportportal.restassured.support.Constants.TEXT_TYPES;
import static com.epam.reportportal.restassured.support.HttpUtils.getMimeType;
import static java.util.Optional.ofNullable;

public class HttpEntityFactory {

	@Nonnull
	public static HttpRequestFormatter createHttpRequestFormatter(
			@Nonnull FilterableRequestSpecification requestSpecification, Function<String, String> uriConverter,
			Function<Header, String> headerConverter, Function<Cookie, String> cookieConverter,
			Map<String, Function<String, String>> prettiers, Function<Header, String> partHeaderConverter) {
		HttpRequestFormatter.Builder builder = new HttpRequestFormatter.Builder(requestSpecification.getMethod(),
				requestSpecification.getURI()
		);
		ofNullable(requestSpecification.getHeaders()).ifPresent(headers -> headers.forEach(h -> builder.addHeader(h.getName(),
				h.getValue()
		)));
		ofNullable(requestSpecification.getCookies()).ifPresent(cookies -> cookies.forEach(c -> builder.addCookie(c.getName(),
				c.getValue(),
				c.getComment(),
				c.getPath(),
				c.getDomain(),
				c.getMaxAge(),
				c.isSecured(),
				c.isHttpOnly(),
				c.getExpiryDate(),
				c.getVersion(),
				c.getSameSite()
		)));
		builder.uriConverter(uriConverter).headerConverter(headerConverter).cookieConverter(cookieConverter);
		String type = getMimeType(requestSpecification.getContentType());
		if (TEXT_TYPES.contains(type)) {
			builder.bodyText(type, requestSpecification.getBody());
		} else if (MULTIPART_TYPES.contains(type)) {
			ofNullable(requestSpecification.getMultiPartParams()).ifPresent(params -> params.forEach(it -> {
				String partMimeType = ofNullable(it.getMimeType()).orElse(ContentType.APPLICATION_OCTET_STREAM.getMimeType());
				HttpPartFormatter.Builder partBuilder;
				try {
					if (TEXT_TYPES.contains(partMimeType)) {
						partBuilder = new HttpPartFormatter.Builder(HttpPartFormatter.PartType.TEXT,
								partMimeType,
								it.getContent()
						);
					} else {
						Object body = it.getContent();
						if (body instanceof File) {
							partBuilder = new HttpPartFormatter.Builder(HttpPartFormatter.PartType.BINARY,
									(File) it.getContent()
							);
						} else {
							partBuilder = new HttpPartFormatter.Builder(HttpPartFormatter.PartType.BINARY,
									partMimeType,
									it.getContent()
							);
						}
					}
					ofNullable(it.getHeaders()).ifPresent(headers -> headers.forEach((key, value) -> partBuilder.addHeader(
							new Header(key, value))));
					partBuilder.controlName(it.getControlName());
					partBuilder.charset(it.getCharset());
					partBuilder.fileName(it.getFileName());
					partBuilder.headerConverter(partHeaderConverter);
				} catch (IOException e) {
					ReportPortal.emitLog("Unable to read file: " + e.getMessage(),
							"ERROR",
							Calendar.getInstance().getTime()
					);
				}

			}));
		} else {
			builder.bodyBytes(type, requestSpecification.getBody());
		}
		return builder.build();
	}

	@Nonnull
	public static HttpRequestFormatter createHttpRequestFormatter(
			@Nonnull FilterableRequestSpecification requestSpecification) {
		return createHttpRequestFormatter(requestSpecification,
				DefaultUriConverter.INSTANCE,
				DefaultHttpHeaderConverter.INSTANCE,
				DefaultCookieConverter.INSTANCE,
				Constants.DEFAULT_PRETTIERS,
				DefaultHttpHeaderConverter.INSTANCE
		);
	}

	public static HttpResponseFormatter createHttpResponseFormatter(Response response) {
		return null;
	}
}
