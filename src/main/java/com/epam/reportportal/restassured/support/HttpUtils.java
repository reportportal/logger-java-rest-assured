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

import org.apache.http.entity.ContentType;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Optional.ofNullable;

public class HttpUtils {

	public static final Set<String> MULTIPART_TYPES = Collections.singleton(ContentType.MULTIPART_FORM_DATA.getMimeType());

	public static final Set<String> TEXT_TYPES = new HashSet<>(Arrays.asList(
			ContentType.APPLICATION_JSON.getMimeType(),
			ContentType.TEXT_PLAIN.getMimeType(),
			ContentType.TEXT_HTML.getMimeType(),
			ContentType.TEXT_XML.getMimeType(),
			ContentType.APPLICATION_XML.getMimeType(),
			ContentType.DEFAULT_TEXT.getMimeType()
	));

	public static String getMimeType(@Nullable String contentType) {
		return ofNullable(contentType).filter(ct -> !ct.isEmpty())
				.map(ct -> ContentType.parse(contentType).getMimeType())
				.orElse(ContentType.APPLICATION_OCTET_STREAM.getMimeType());
	}
}
