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

import com.epam.reportportal.formatting.http.HttpPartFormatter;
import com.epam.reportportal.formatting.http.HttpRequestFormatter;
import com.epam.reportportal.formatting.http.HttpResponseFormatter;
import com.epam.reportportal.formatting.http.entities.BodyType;
import com.epam.reportportal.formatting.http.entities.Cookie;
import com.epam.reportportal.formatting.http.entities.Header;
import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.utils.files.Utils;
import io.restassured.response.Response;
import io.restassured.response.ResponseBodyData;
import io.restassured.specification.FilterableRequestSpecification;
import org.apache.http.entity.ContentType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Map;
import java.util.function.Function;

import static com.epam.reportportal.formatting.http.HttpFormatUtils.getBodyType;
import static com.epam.reportportal.formatting.http.HttpFormatUtils.getMimeType;
import static java.util.Optional.ofNullable;

/**
 * Factory class to convert Rest-Assured entities to internal ones.
 */
public class HttpEntityFactory {

	@Nonnull
	public static HttpRequestFormatter createHttpRequestFormatter(
			@Nonnull FilterableRequestSpecification requestSpecification,
			@Nullable Function<String, String> uriConverter, @Nullable Function<Header, String> headerConverter,
			@Nullable Function<Cookie, String> cookieConverter,
			@Nullable Map<String, Function<String, String>> prettiers,
			@Nullable Function<Header, String> partHeaderConverter, @Nonnull Map<String, BodyType> bodyTypeMap) {
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
		builder.uriConverter(uriConverter)
				.headerConverter(headerConverter)
				.cookieConverter(cookieConverter)
				.prettiers(prettiers);
		String mimeType = getMimeType(requestSpecification.getContentType());
		BodyType bodyType = getBodyType(requestSpecification.getContentType(), bodyTypeMap);
		switch (bodyType) {
			case TEXT:
				builder.bodyText(mimeType, requestSpecification.getBody());
				break;
			case FORM:
				builder.bodyParams(requestSpecification.getFormParams());
				break;
			case MULTIPART:
				ofNullable(requestSpecification.getMultiPartParams()).ifPresent(params -> params.forEach(it -> {
					String partMimeType = ofNullable(it.getMimeType()).orElse(ContentType.APPLICATION_OCTET_STREAM.getMimeType());
					HttpPartFormatter.Builder partBuilder;
					try {
						Object body = it.getContent();
						HttpPartFormatter.PartType partType;
						if (BodyType.TEXT == getBodyType(partMimeType, bodyTypeMap)) {
							partType = HttpPartFormatter.PartType.TEXT;
						} else {
							partType = HttpPartFormatter.PartType.BINARY;
						}

						Object content;
						if (body instanceof File) {
							TypeAwareByteSource file = Utils.getFile((File) body);
							byte[] data = file.read();
							if (partType == HttpPartFormatter.PartType.TEXT) {
								content = ofNullable(data).map(d -> {
									try {
										return new String(d,
												ofNullable(it.getCharset()).orElse(StandardCharsets.UTF_8.name())
										);
									} catch (UnsupportedEncodingException e) {
										throw new IllegalStateException(e);
									}
								}).orElse("");
							} else {
								content = data;
							}
						} else {
							content = body;
						}
						partBuilder = new HttpPartFormatter.Builder(partType, partMimeType, content);

						ofNullable(it.getHeaders()).ifPresent(headers -> headers.forEach((key, value) -> partBuilder.addHeader(
								new Header(key, value))));
						partBuilder.controlName(it.getControlName());
						partBuilder.charset(it.getCharset());
						partBuilder.fileName(it.getFileName());
						partBuilder.headerConverter(partHeaderConverter);
						builder.addBodyPart(partBuilder.build());
					} catch (IOException e) {
						ReportPortal.emitLog("Unable to read file: " + e.getMessage(),
								"ERROR",
								Calendar.getInstance().getTime()
						);
					}
				}));
				break;
			default:
				builder.bodyBytes(mimeType, requestSpecification.getBody());
		}
		return builder.build();
	}

	@Nonnull
	public static HttpResponseFormatter createHttpResponseFormatter(@Nonnull Response response,
			@Nullable Function<Header, String> headerConverter, @Nullable Function<Cookie, String> cookieConverter,
			@Nullable Map<String, Function<String, String>> prettiers, @Nonnull Map<String, BodyType> bodyTypeMap) {
		HttpResponseFormatter.Builder builder = new HttpResponseFormatter.Builder(response.statusCode(),
				response.getStatusLine()
		);
		ofNullable(response.getHeaders()).ifPresent(headers -> headers.forEach(h -> builder.addHeader(h.getName(),
				h.getValue()
		)));
		ofNullable(response.getDetailedCookies()).ifPresent(cookies -> cookies.forEach(c -> builder.addCookie(c.getName(),
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
		builder.headerConverter(headerConverter).cookieConverter(cookieConverter).prettiers(prettiers);

		String type = getMimeType(response.getContentType());
		BodyType bodyType = getBodyType(response.getContentType(), bodyTypeMap);
		if (BodyType.TEXT == bodyType) {
			builder.bodyText(type, ofNullable(response.getBody()).map(ResponseBodyData::asString).orElse(null));
		} else {
			builder.bodyBytes(type, ofNullable(response.getBody()).map(ResponseBodyData::asByteArray).orElse(null));
		}
		return builder.build();
	}
}
