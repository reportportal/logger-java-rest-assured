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

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import static java.util.Optional.ofNullable;

public class SanitizingCookieConverter implements Function<Cookie, String> {

	public static final Set<String> SESSION_COOKIES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("sid",
			"session",
			"session_id",
			"sessionId",
			"sessionid"
	)));

	public static final Function<Cookie, String> INSTANCE = new SanitizingCookieConverter();

	private final Set<String> sanitizeSet;

	public SanitizingCookieConverter(Set<String> sanitizeCookies) {
		sanitizeSet = sanitizeCookies;
	}

	private SanitizingCookieConverter() {
		this(SESSION_COOKIES);
	}

	@Override
	public @Nullable String apply(@Nullable Cookie cookie) {
		return DefaultCookieConverter.INSTANCE.apply(ofNullable(cookie).filter(c -> sanitizeSet.contains(cookie.getName()))
				.map(c -> {
					Cookie newCookie = c.clone();
					newCookie.setValue(Constants.REMOVED_TAG);
					return newCookie;
				})
				.orElse(cookie));
	}
}
