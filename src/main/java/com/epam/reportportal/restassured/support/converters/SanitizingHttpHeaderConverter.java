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

package com.epam.reportportal.restassured.support.converters;

import com.epam.reportportal.restassured.support.http.Header;
import org.apache.http.HttpHeaders;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import static com.epam.reportportal.restassured.support.Constants.REMOVED_TAG;
import static java.util.Optional.ofNullable;

public class SanitizingHttpHeaderConverter implements Function<Header, String> {

	public static final Function<Header, String> INSTANCE = new SanitizingHttpHeaderConverter();

	public static final Set<String> SENSITIVE_HEADERS = Collections.unmodifiableSet(new HashSet<>(Collections.singletonList(
			HttpHeaders.AUTHORIZATION)));

	private final Set<String> sanitizeSet;

	private SanitizingHttpHeaderConverter(Set<String> sanitizeHeaders) {
		sanitizeSet = sanitizeHeaders;
	}

	private SanitizingHttpHeaderConverter() {
		this(SENSITIVE_HEADERS);
	}

	@Override
	public @Nullable String apply(@Nullable Header header) {
		return DefaultHttpHeaderConverter.INSTANCE.apply(ofNullable(header).filter(h -> sanitizeSet.contains(h.getName()))
				.map(h -> {
					Header newHeader = h.clone();
					newHeader.setValue(REMOVED_TAG);
					return newHeader;
				})
				.orElse(header));
	}
}
