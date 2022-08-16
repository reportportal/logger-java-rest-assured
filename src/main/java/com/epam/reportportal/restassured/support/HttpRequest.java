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
import javax.annotation.Nullable;
import java.util.*;
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

	private Function<Header, String> headerConverter;

	private Function<Cookie, String> cookieConverter;

	private List<Header> headers;

	private List<Cookie> cookies;

	private BodyType type = BodyType.NONE;

	private Object body;

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
				.map(headerConverter)
				.filter(h -> h != null && !h.isEmpty())
				.collect(Collectors.joining(LINE_DELIMITER,
						LINE_DELIMITER + HEADERS_TAG + LINE_DELIMITER,
						LINE_DELIMITER
				))).orElse("");
	}

	@Nonnull
	public String getCookiesString() {
		return of(cookies.stream()
				.map(cookieConverter)
				.filter(c -> c != null && !c.isEmpty())
				.collect(Collectors.joining(LINE_DELIMITER,
						LINE_DELIMITER + COOKIES_TAG + LINE_DELIMITER,
						LINE_DELIMITER
				))).orElse("");
	}

	public void setUriConverter(@Nonnull Function<String, String> uriConverter) {
		this.uriConverter = uriConverter;
	}

	public void setHeaderConverter(@Nonnull Function<Header, String> headerConverter) {
		this.headerConverter = headerConverter;
	}

	public void setCookieConverter(@Nonnull Function<Cookie, String> cookieConverter) {
		this.cookieConverter = cookieConverter;
	}

	public void setHeaders(@Nonnull List<Header> requestHeaders) {
		headers = requestHeaders;
	}

	public void setCookies(@Nonnull List<Cookie> cookies) {
		this.cookies = cookies;
	}

	public void setType(@Nonnull BodyType type) {
		this.type = type;
	}

	public BodyType getType() {
		return type;
	}

	public <T> void setBody(@Nullable T body) {
		if (body == null) {
			setType(BodyType.NONE);
		}
		this.body = body;
	}

	public String getTextBody() {
		Objects.requireNonNull(body);
		if (BodyType.TEXT == type) {
			return (String) body;
		}
		throw new ClassCastException("Cannot return text body for body type: " + type.name());

	}

	public byte[] getBinaryBody() {
		Objects.requireNonNull(body);
		if (BodyType.BINARY == type) {
			return (byte[]) body;
		}
		throw new ClassCastException("Cannot return binary body for body type: " + type.name());
	}

	@SuppressWarnings("unchecked")
	public List<Part> getMultipartBody() {
		Objects.requireNonNull(body);
		if (BodyType.MULTIPART == type) {
			return (List<Part>) body;
		}
		throw new ClassCastException("Cannot return multipart body for body type: " + type.name());
	}

	@Nullable
	public <T> T getBody(Class<T> type) {
		return type.cast(body);
	}

	public boolean hasBody() {
		return null != type && BodyType.NONE != type;
	}

	public static class Part {
		private final PartType type;
		private final Object payload;

		private String controlName;
		private String mimeType;
		private Map<String, String> headers;
		private String charset;
		private String fileName;

		public Part(@Nonnull PartType type, @Nonnull Object payload) {
			this.type = type;
			this.payload = payload;
		}

		public String getTextPayload() {
			if (PartType.TEXT == type) {
				return (String) payload;
			}
			throw new ClassCastException("Cannot return text for payload type: " + type.name());
		}

		public byte[] getBinaryPayload() {
			if (PartType.BINARY == type) {
				return (byte[]) payload;
			}
			throw new ClassCastException("Cannot return binary data for payload type: " + type.name());
		}

		public String getControlName() {
			return controlName;
		}

		public void setControlName(String controlName) {
			this.controlName = controlName;
		}

		public String getMimeType() {
			return mimeType;
		}

		public void setMimeType(String mimeType) {
			this.mimeType = mimeType;
		}

		public Map<String, String> getHeaders() {
			return headers;
		}

		public void setHeaders(Map<String, String> headers) {
			this.headers = headers;
		}

		public String getCharset() {
			return charset;
		}

		public void setCharset(String charset) {
			this.charset = charset;
		}

		public String getFileName() {
			return fileName;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}
	}

	public enum PartType {
		TEXT,
		BINARY
	}

	public enum BodyType {
		TEXT,
		MULTIPART,
		BINARY,
		NONE
	}

	public static class Builder {
		private final String method;
		private final String uri;
		private final List<Header> headers = new ArrayList<>();
		private final List<Cookie> cookies = new ArrayList<>();

		private Function<String, String> uriConverter;
		private Function<Header, String> headerConverter;
		private Function<Cookie, String> cookieConverter;

		private BodyType type;

		private Object body;

		public Builder(@Nonnull String requestMethod, @Nonnull String requestUri) {
			method = requestMethod;
			uri = requestUri;
		}

		public Builder uriConverter(Function<String, String> uriConverter) {
			this.uriConverter = uriConverter;
			return this;
		}

		public Builder headerConverter(Function<Header, String> headerConverter) {
			this.headerConverter = headerConverter;
			return this;
		}

		public Builder cookieConverter(Function<Cookie, String> cookieConverter) {
			this.cookieConverter = cookieConverter;
			return this;
		}

		public Builder addHeader(String name, String value) {
			headers.add(new Header(name, value));
			return this;
		}

		public Builder addCookie(String name, String value, String comment, String path, String domain, Long maxAge,
				Boolean secured, Boolean httpOnly, Date expiryDate, Integer version, String sameSite) {
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
			cookies.add(cookie);
			return this;
		}

		public Builder addCookie(String name, String value) {
			return addCookie(name, value, null, null, null, null, null, null, null, null, null);
		}

		public Builder addCookie(String name) {
			return addCookie(name, null);
		}

		public Builder bodyText(String body) {
			type = BodyType.TEXT;
			this.body = body;
			return this;
		}

		public Builder bodyBytes(byte[] body) {
			type = BodyType.BINARY;
			this.body = body;
			return this;
		}

		@SuppressWarnings("unchecked")
		public Builder addBodyPart(HttpRequest.Part part) {
			if (body != null && type == BodyType.MULTIPART) {
				((List<HttpRequest.Part>) body).add(part);
			} else {
				type = BodyType.MULTIPART;
				body = new ArrayList<>(Collections.singleton(part));
			}
			return this;
		}

		public HttpRequest build() {
			HttpRequest result = new HttpRequest(method, uri);
			result.setUriConverter(ofNullable(uriConverter).orElse(Converters.DEFAULT_URI_CONVERTER));
			result.setHeaderConverter(ofNullable(headerConverter).orElse(DefaultHttpHeaderConverter.INSTANCE));
			result.setCookieConverter(ofNullable(cookieConverter).orElse(DefaultCookieConverter.INSTANCE));
			result.setHeaders(headers);
			result.setCookies(cookies);
			if (body != null) {
				result.setType(type);
				result.setBody(body);
			} else {
				result.setType(BodyType.NONE);
			}
			return result;
		}
	}
}
