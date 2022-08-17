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
import io.restassured.specification.FilterableRequestSpecification;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.function.Function;

import static com.epam.reportportal.restassured.support.Constants.MULTIPART_TYPES;
import static com.epam.reportportal.restassured.support.Constants.TEXT_TYPES;
import static com.epam.reportportal.restassured.support.HttpUtils.getMimeType;

public class HttpEntityFactory {

	@Nonnull
	public static HttpRequestFormatter createHttpRequestFormatter(
			@Nonnull FilterableRequestSpecification requestSpecification, Function<String, String> uriConverter,
			Function<Header, String> headerConverter, Function<Cookie, String> cookieConverter,
			Map<String, Function<String, String>> prettiers) {
		HttpRequestFormatter.Builder builder = new HttpRequestFormatter.Builder(requestSpecification.getMethod(),
				requestSpecification.getURI()
		);
		requestSpecification.getHeaders().forEach(h -> builder.addHeader(h.getName(), h.getValue()));
		requestSpecification.getCookies().forEach(c -> builder.addCookie(c.getName(),
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
		));
		builder.uriConverter(uriConverter).headerConverter(headerConverter).cookieConverter(cookieConverter);
		String type = getMimeType(requestSpecification.getContentType());
		if (TEXT_TYPES.contains(type)) {
			builder.bodyText(type, requestSpecification.getBody());
		} else if (MULTIPART_TYPES.contains(type)) {

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
				Constants.DEFAULT_PRETTIERS
		);
	}

}
