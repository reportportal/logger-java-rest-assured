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
import com.epam.reportportal.restassured.support.http.BodyType;
import com.epam.reportportal.restassured.support.http.Cookie;
import com.epam.reportportal.restassured.support.http.Header;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.epam.reportportal.restassured.support.Constants.*;
import static com.epam.reportportal.restassured.support.HttpFormatUtils.joinParts;
import static java.util.Optional.ofNullable;

public class HttpResponseFormatter {

	private final int code;
	private final String phrase;

	private Function<Header, String> headerConverter;
	private Function<Cookie, String> cookieConverter;
	private Map<String, Function<String, String>> prettiers;

	private List<Header> headers;
	private List<Cookie> cookies;

	private BodyType type = BodyType.NONE;

	private String mimeType;
	private Object body;

	public HttpResponseFormatter(int statusCode, String reasonPhrase) {
		this.code = statusCode;
		this.phrase = reasonPhrase;
	}

	public String formatResponse() {
		return RESPONSE_TAG + LINE_DELIMITER + phrase;
	}

	@Nonnull
	public String formatHeaders() {
		return HttpFormatUtils.formatHeaders(headers, headerConverter);
	}

	@Nonnull
	public String formatCookies() {
		return HttpFormatUtils.formatCookies(cookies, cookieConverter);
	}

	public String formatHead() {
		return joinParts(LINE_DELIMITER + LINE_DELIMITER, formatResponse(), formatHeaders(), formatCookies());
	}

	@Nonnull
	public BodyType getType() {
		return type;
	}

	public void setType(@Nonnull BodyType type) {
		this.type = type;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setBody(Object body) {
		this.body = body;
	}

	@Nonnull
	public String formatAsText() {
		return HttpFormatUtils.formatText(formatHead(), getTextBody(), BODY_TAG, prettiers, mimeType);
	}

	public void setHeaderConverter(Function<Header, String> headerConverter) {
		this.headerConverter = headerConverter;
	}

	public void setCookieConverter(Function<Cookie, String> cookieConverter) {
		this.cookieConverter = cookieConverter;
	}

	public void setHeaders(List<Header> headers) {
		this.headers = headers;
	}

	public void setCookies(List<Cookie> cookies) {
		this.cookies = cookies;
	}

	public String getTextBody() {
		if (BodyType.TEXT == type) {
			return (String) body;
		}
		throw new ClassCastException("Cannot return text body for body type: " + type.name());

	}

	public byte[] getBinaryBody() {
		if (BodyType.BINARY == type) {
			return (byte[]) body;
		}
		throw new ClassCastException("Cannot return binary body for body type: " + type.name());
	}

	public void setPrettiers(Map<String, Function<String, String>> prettiers) {
		this.prettiers = prettiers;
	}

	public static class Builder {
		private final int code;
		private final String phrase;

		private Function<Header, String> headerConverter;
		private Function<Cookie, String> cookieConverter;

		private final List<Header> headers = new ArrayList<>();
		private final List<Cookie> cookies = new ArrayList<>();

		private BodyType type;
		private String mimeType;

		private Object body;

		private Map<String, Function<String, String>> prettiers;

		public Builder(int statusCode, String reasonPhrase) {
			this.code = statusCode;
			this.phrase = reasonPhrase;
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
			headers.add(HttpFormatUtils.toHeader(name, value));
			return this;
		}

		public Builder addCookie(String name, String value, String comment, String path, String domain, Long maxAge,
				Boolean secured, Boolean httpOnly, Date expiryDate, Integer version, String sameSite) {
			cookies.add(HttpFormatUtils.toCookie(name,
					value,
					comment,
					path,
					domain,
					maxAge,
					secured,
					httpOnly,
					expiryDate,
					version,
					sameSite
			));
			return this;
		}

		public Builder addCookie(String name, String value) {
			return addCookie(name, value, null, null, null, null, null, null, null, null, null);
		}

		public Builder addCookie(String name) {
			return addCookie(name, null);
		}

		public Builder bodyText(String mimeType, String payload) {
			type = BodyType.TEXT;
			this.mimeType = mimeType;
			body = payload;
			return this;
		}

		public Builder bodyBytes(String mimeType, byte[] payload) {
			type = BodyType.BINARY;
			this.mimeType = mimeType;
			body = payload;
			return this;
		}

		public Builder prettiers(Map<String, Function<String, String>> formatPrettiers) {
			this.prettiers = formatPrettiers;
			return this;
		}

		public HttpResponseFormatter build() {
			HttpResponseFormatter result = new HttpResponseFormatter(code, phrase);
			result.setHeaderConverter(ofNullable(headerConverter).orElse(DefaultHttpHeaderConverter.INSTANCE));
			result.setCookieConverter(ofNullable(cookieConverter).orElse(DefaultCookieConverter.INSTANCE));
			result.setPrettiers(ofNullable(prettiers).orElse(Constants.DEFAULT_PRETTIERS));
			result.setHeaders(headers);
			result.setCookies(cookies);
			if (body != null) {
				result.setType(type);
				result.setMimeType(mimeType);
				result.setBody(body);
			} else {
				result.setType(BodyType.NONE);
			}
			return result;
		}
	}
}
