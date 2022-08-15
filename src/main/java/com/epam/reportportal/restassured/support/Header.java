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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

public class Header implements Cloneable {

	private final String name;
	private final String value;
	private final Function<Header, String> converter;

	public Header(@Nonnull String headerName, @Nonnull String headerValue,
			@Nonnull Function<Header, String> headerConverter) {
		name = headerName;
		value = headerValue;
		converter = headerConverter;
	}

	@Nullable
	public String format() {
		return converter.apply(this);
	}

	@Nonnull
	public String getName() {
		return name;
	}

	@Nonnull
	public String getValue() {
		return value;
	}

	@Override
	public Header clone() {
		return new Header(name, value, converter);
	}
}
