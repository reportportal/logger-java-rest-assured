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

package com.epam.reportportal.restassured.support.http;

import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.epam.reportportal.restassured.support.Constants.*;
import static java.util.Optional.of;

public class Part {
	private final PartType type;
	private final Object payload;
	private String controlName;
	private String mimeType;
	private List<Pair<String, String>> headers;
	private String charset;
	private String fileName;

	private Function<Pair<String, String>, String> headerConverter;

	public Part(@Nonnull PartType type, @Nonnull Object payload) {
		this.type = type;
		this.payload = payload;
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

	public String formatAsText(){
		String prefix = formatHeaders();
		String body = getTextPayload();
		if (body.isEmpty()) {
			return prefix;
		} else {
			return prefix + LINE_DELIMITER + BODY_PART_TAG + LINE_DELIMITER + body;
		}
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

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public List<Pair<String, String>> getHeaders() {
		return headers;
	}

	public void setHeaders(List<Pair<String, String>> headers) {
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

	public Function<Pair<String, String>, String> getHeaderConverter() {
		return headerConverter;
	}

	public void setHeaderConverter(Function<Pair<String, String>, String> headerConverter) {
		this.headerConverter = headerConverter;
	}

	public enum PartType {
		TEXT,
		BINARY,
		FILE
	}

	public static class Builder {
		public Builder(@Nonnull PartType type, @Nonnull Object payload) {

		}
	}
}
