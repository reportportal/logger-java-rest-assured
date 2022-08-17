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

package com.epam.reportportal.restassured.support.prettiers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonPrettier implements Prettier {

	public static final JsonPrettier INSTANCE = new JsonPrettier();

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final ObjectMapper mapper;

	public JsonPrettier(ObjectMapper objectMapper) {
		mapper = objectMapper;
	}

	private JsonPrettier() {
		this(OBJECT_MAPPER);
	}

	@Override
	public String apply(String json) {
		try {
			JsonNode node = mapper.readTree(json);
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node).trim();
		} catch (Exception ignore) {
			return json;
		}
	}
}
