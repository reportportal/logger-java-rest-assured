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
import com.epam.reportportal.restassured.support.http.Cookie;
import com.epam.reportportal.restassured.support.http.Header;
import org.apache.http.entity.ContentType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.epam.reportportal.restassured.support.Constants.*;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

public class HttpFormatUtils {

	private static final String BODY_HIGHLIGHT = "```";

	private HttpFormatUtils() {
		throw new IllegalStateException("Static only class");
	}

	public static String getMimeType(@Nullable String contentType) {
		return ofNullable(contentType).filter(ct -> !ct.isEmpty())
				.map(ct -> ContentType.parse(contentType).getMimeType())
				.orElse(ContentType.APPLICATION_OCTET_STREAM.getMimeType());
	}

	public static String joinParts(String delimiter, String... parts) {
		if (parts == null) {
			return "";
		}
		return Arrays.stream(parts)
				.filter(p -> Objects.nonNull(p) && !p.isEmpty())
				.collect(Collectors.joining(delimiter));
	}

	@Nonnull
	public static String formatHeaders(@Nullable List<Header> headers,
			@Nullable Function<Header, String> headerConverter) {
		if (headers == null || headers.isEmpty()) {
			return "";
		}
		Function<Header, String> converter =
				headerConverter == null ? DefaultHttpHeaderConverter.INSTANCE : headerConverter;
		return of(headers.stream()
				.map(converter)
				.filter(h -> h != null && !h.isEmpty())
				.collect(Collectors.joining(LINE_DELIMITER, HEADERS_TAG + LINE_DELIMITER, ""))).orElse("");
	}

	@Nonnull
	public static String formatCookies(@Nullable List<Cookie> cookies,
			@Nullable Function<Cookie, String> cookieConverter) {
		if (cookies == null || cookies.isEmpty()) {
			return "";
		}
		Function<Cookie, String> converter =
				cookieConverter == null ? DefaultCookieConverter.INSTANCE : cookieConverter;
		return of(cookies.stream()
				.map(converter)
				.filter(c -> c != null && !c.isEmpty())
				.collect(Collectors.joining(LINE_DELIMITER, COOKIES_TAG + LINE_DELIMITER, ""))).orElse("");
	}

	@Nonnull
	public static String formatText(@Nullable String header, @Nullable String body, @Nullable String tag,
			@Nullable Map<String, Function<String, String>> contentPrettiers, String contentType) {
		Map<String, Function<String, String>> prettiers = contentPrettiers;
		if (contentPrettiers == null) {
			prettiers = Collections.emptyMap();
		}
		if (body == null || body.isEmpty()) {
			return header == null ? "" : header;
		} else {
			return (header == null || header.isEmpty() ? "" : header + LINE_DELIMITER + LINE_DELIMITER) + (
					tag == null || tag.isEmpty() ? "" : tag + LINE_DELIMITER) + BODY_HIGHLIGHT + LINE_DELIMITER + (
					prettiers.containsKey(contentType) ?
							prettiers.get(contentType).apply(body) :
							body) + LINE_DELIMITER + BODY_HIGHLIGHT;
		}
	}

	@Nonnull
	public static Header toHeader(@Nonnull String name, @Nonnull String value) {
		return new Header(name, value);
	}

	@Nonnull
	public static Cookie toCookie(@Nonnull String name, @Nullable String value, @Nullable String comment,
			@Nullable String path, @Nullable String domain, @Nullable Long maxAge, @Nullable Boolean secured,
			@Nullable Boolean httpOnly, @Nullable Date expiryDate, @Nullable Integer version,
			@Nullable String sameSite) {
		Cookie cookie = new Cookie(name);
		cookie.setValue(value);
		cookie.setComment(comment);
		cookie.setPath(path);
		cookie.setDomain(domain);
		cookie.setMaxAge(maxAge);
		cookie.setSecured(secured);
		cookie.setHttpOnly(httpOnly);
		cookie.setExpiryDate(expiryDate);
		cookie.setVersion(version);
		cookie.setSameSite(sameSite);
		return cookie;
	}
}
