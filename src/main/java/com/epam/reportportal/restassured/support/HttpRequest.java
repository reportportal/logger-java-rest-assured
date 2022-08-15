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

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

public class HttpRequest {

	public static final String LINE_DELIMITER = "\n";
	public static final String REQUEST_TAG = "**>>> REQUEST**";
	public static final String HEADERS_TAG = "**Headers**";

	public static final String COOKIES_TAG = "**Cookies**";

	private final String method;
	private final String uri;

	private Function<String, String> uriConverter;

	private List<Header> headers;

	private List<Cookie> cookies;

	public HttpRequest(@Nonnull String requestMethod, @Nonnull String requestUri) {
		method = requestMethod;
		uri = requestUri;
	}

	@Nonnull
	public String getRequestString() {
		return REQUEST_TAG + LINE_DELIMITER + String.format("%s to %s", method, uriConverter.apply(uri))
				+ LINE_DELIMITER;
	}

	@Nonnull
	public String getHeadersString() {
		return of(headers.stream()
				.map(Header::format)
				.filter(h -> h != null && !h.isEmpty())
				.collect(Collectors.joining(LINE_DELIMITER,
						LINE_DELIMITER + HEADERS_TAG + LINE_DELIMITER,
						LINE_DELIMITER
				))).orElse("");
	}

	@Nonnull
	public String getCookiesString() {
		return of(cookies.stream()
				.map(Cookie::format)
				.filter(c -> c != null && !c.isEmpty())
				.collect(Collectors.joining(LINE_DELIMITER,
						LINE_DELIMITER + COOKIES_TAG + LINE_DELIMITER,
						LINE_DELIMITER
				))).orElse("");
	}

	public void setUriConverter(@Nonnull Function<String, String> uriConverter) {
		this.uriConverter = uriConverter;
	}

	public void setHeaders(@Nonnull List<Header> requestHeaders) {
		headers = requestHeaders;
	}

	public void setCookies(@Nonnull List<Cookie> cookies) {
		this.cookies = cookies;
	}

	public static class Builder {
		private final String method;
		private final String uri;
		private final List<Header> headers = new ArrayList<>();
		private final List<Cookie> cookies = new ArrayList<>();

		private Function<String, String> uriConverter;

		public Builder(@Nonnull String requestMethod, @Nonnull String requestUri) {
			method = requestMethod;
			uri = requestUri;
		}

		public Builder uriConverter(Function<String, String> uriConverter) {
			this.uriConverter = uriConverter;
			return this;
		}

		public Builder addHeader(String name, String value, Function<Header, String> converter) {
			headers.add(new Header(name, value, converter));
			return this;
		}

		public Builder addHeader(String name, String value) {
			return addHeader(name, value, DefaultHttpHeaderConverter.INSTANCE);
		}

		public Builder addCookie(String name) {
			cookies.add(new Cookie(name, DefaultCookieConverter.INSTANCE));
			return this;
		}

		public HttpRequest build() {
			HttpRequest result = new HttpRequest(method, uri);
			result.setUriConverter(ofNullable(uriConverter).orElse(Converters.DEFAULT_URI_CONVERTER));
			result.setHeaders(headers);
			result.setCookies(cookies);
			return result;
		}
	}
}
