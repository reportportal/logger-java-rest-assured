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

import javax.annotation.Nullable;
import java.util.function.Function;

import static java.util.Optional.ofNullable;

public class DefaultHttpHeaderConverter implements Function<Header, String> {

	public static final Function<Header, String> INSTANCE = new DefaultHttpHeaderConverter();

	private DefaultHttpHeaderConverter() {
	}

	@Override
	public @Nullable String apply(@Nullable Header header) {
		return ofNullable(header).map(h -> h.getName() + ": " + h.getValue().replace("*", "\\*")).orElse(null);
	}
}
