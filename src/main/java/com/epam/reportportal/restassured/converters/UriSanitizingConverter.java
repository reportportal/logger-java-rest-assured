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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Function;

import static java.util.Optional.ofNullable;

/**
 * URI converter for {@link ReportPortalRestAssuredLoggingFilter}. Removes passwords in URIs.
 */
public class UriSanitizingConverter implements Function<String, String> {

	public static final String REMOVED_TAG = "&lt;removed&gt;";

	@Override
	public String apply(String uriStr) {
		try {
			URI uri = URI.create(uriStr);
			String userInfo = ofNullable(uri.getUserInfo()).map(info -> info.split(":", 2)).map(info -> {
				if (info.length > 1) {
					return new String[] { info[0], REMOVED_TAG };
				}
				return info;
			}).map(info -> String.join(":", info)).orElse(null);

			return new URI(
					uri.getScheme(),
					userInfo,
					uri.getHost(),
					uri.getPort(),
					uri.getPath(),
					uri.getQuery(),
					uri.getFragment()
			).toString();
		} catch (URISyntaxException | IllegalArgumentException e) {
			return uriStr;
		}
	}
}
