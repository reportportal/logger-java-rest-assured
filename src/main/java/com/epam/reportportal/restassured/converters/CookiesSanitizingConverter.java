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

package com.epam.reportportal.restassured.converters;

import com.epam.reportportal.restassured.ReportPortalRestAssuredLoggingFilter;
import io.restassured.http.Cookie;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * Cookies converter for {@link ReportPortalRestAssuredLoggingFilter}. Removes any data in session-related cookies.
 */
public class CookiesSanitizingConverter implements Function<Cookie, String> {

	public static final String REMOVED_TAG = "&lt;removed&gt;";

	public static final Set<String> SESSION_COOKIES = new HashSet<>(Arrays.asList("sid",
			"session",
			"session_id",
			"sessionId",
			"sessionid"
	));

	@Override
	public String apply(Cookie cookie) {
		return SESSION_COOKIES.contains(cookie.getName()) ? cookie.getName() + "=" + REMOVED_TAG : cookie.toString();
	}
}
