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

import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.reportportal.restassured.support.converters.DefaultHttpHeaderConverter;
import com.epam.reportportal.restassured.support.http.Header;
import com.epam.reportportal.utils.files.Utils;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.epam.reportportal.restassured.support.Constants.*;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

public class HttpPartFormatter {
	private final PartType type;
	private final Object payload;
	private final String mimeType;
	private String controlName;
	private List<Header> headers;
	private String charset;
	private String fileName;

	private Function<Header, String> headerConverter;

	public HttpPartFormatter(@Nonnull PartType type, @Nonnull String mimeType, @Nonnull Object payload) {
		this.type = type;
		this.payload = payload;
		this.mimeType = mimeType;
	}

	@Nonnull
	public String formatHeaders() {
		return of(headers.stream()
				.map(headerConverter)
				.filter(h -> h != null && !h.isEmpty())
				.collect(Collectors.joining(LINE_DELIMITER,
						LINE_DELIMITER + HEADERS_TAG + LINE_DELIMITER,
						LINE_DELIMITER
				))).orElse("");
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

	public String formatAsText() {
		String prefix = formatHeaders();
		String body = getTextPayload();
		if (body.isEmpty()) {
			return prefix;
		} else {
			return prefix + LINE_DELIMITER + BODY_PART_TAG + LINE_DELIMITER + body;
		}
	}

	public String formatForBinaryDataPrefix() {
		String prefix = formatHeaders();
		String postfix = BODY_PART_TAG + LINE_DELIMITER + mimeType;
		return prefix.isEmpty() ? postfix : prefix + LINE_DELIMITER + postfix;
	}

	public PartType getType() {
		return type;
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

	public List<Header> getHeaders() {
		return headers;
	}

	public void setHeaders(List<Header> headers) {
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

	public Function<Header, String> getHeaderConverter() {
		return headerConverter;
	}

	public void setHeaderConverter(Function<Header, String> headerConverter) {
		this.headerConverter = headerConverter;
	}

	public enum PartType {
		TEXT,
		BINARY
	}

	public static class Builder {
		private final PartType type;
		private final Object payload;
		private final String mimeType;

		private String controlName;
		private List<Header> headers = new ArrayList<>();
		private String charset;
		private String fileName;

		private Function<Header, String> headerConverter;

		public Builder(@Nonnull PartType partType, @Nonnull File body) throws IOException {
			type = partType;
			TypeAwareByteSource file = Utils.getFile(body);
			this.mimeType = file.getMediaType();
			payload = file.read();
		}

		public Builder(@Nonnull PartType partType, @Nonnull String mimeType, @Nonnull Object body) {
			type = partType;
			this.mimeType = mimeType;
			payload = body;
		}

		public Builder controlName(String name) {
			controlName = name;
			return this;
		}

		public Builder headers(List<Header> headerList) {
			headers = new ArrayList<>(headerList);
			return this;
		}

		public Builder addHeader(Header header) {
			headers.add(header);
			return this;
		}

		public Builder charset(String charset) {
			this.charset = charset;
			return this;
		}

		public Builder fileName(String fileName) {
			this.fileName = fileName;
			return this;
		}

		public Builder headerConverter(Function<Header, String> converter) {
			headerConverter = converter;
			return this;
		}

		public HttpPartFormatter build() {
			HttpPartFormatter formatter = new HttpPartFormatter(type, mimeType, payload);
			formatter.setControlName(controlName);
			formatter.setHeaders(headers);
			formatter.setCharset(charset);
			formatter.setFileName(fileName);
			formatter.setHeaderConverter(ofNullable(headerConverter).orElse(DefaultHttpHeaderConverter.INSTANCE));
			return formatter;
		}
	}
}
