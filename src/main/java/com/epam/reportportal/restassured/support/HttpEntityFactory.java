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

import io.restassured.specification.FilterableRequestSpecification;

import javax.annotation.Nonnull;

import static com.epam.reportportal.restassured.support.HttpUtils.*;

public class HttpEntityFactory {

	@Nonnull
	public static HttpRequest createHttpRequest(@Nonnull FilterableRequestSpecification requestSpecification) {
		HttpRequest.Builder builder = new HttpRequest.Builder(requestSpecification.getMethod(),
				requestSpecification.getURI()
		);
		requestSpecification.getHeaders().forEach(h -> builder.addHeader(h.getName(), h.getValue()));
		requestSpecification.getCookies().forEach(c -> builder.addCookie(c.getName(), c.getValue()));
		String type = getMimeType(requestSpecification.getContentType());
		if (TEXT_TYPES.contains(type)) {
			builder.bodyText(requestSpecification.getBody());
		} else if (MULTIPART_TYPES.contains(type)) {

		} else {

		}
		return builder.build();
	}

}
