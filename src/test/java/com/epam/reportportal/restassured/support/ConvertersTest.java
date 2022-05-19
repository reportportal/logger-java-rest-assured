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

import io.restassured.http.Cookie;
import io.restassured.http.Header;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ConvertersTest {

	@Test
	public void testSessionIdHeaderRemove() {
		Cookie cookie = new Cookie.Builder("session_id", "my_test_session_id").setComment("test comment")
				.setDomain("example.com")
				.setHttpOnly(true)
				.setPath("/")
				.build();
		String result = Converters.COOKIE_SANITIZING_CONVERTER.apply(cookie);
		assertThat(result, equalTo(cookie.getName() + "=" + Converters.REMOVED_TAG));
	}

	@Test
	public void testAuthorizationHeaderRemove() {
		String result = Converters.HEADER_SANITIZING_CONVERTER.apply(new Header(HttpHeaders.AUTHORIZATION, "Bearer test_token"));
		assertThat(result, equalTo(HttpHeaders.AUTHORIZATION + ": " + Converters.REMOVED_TAG));
	}

	@Test
	public void testUriPasswordRemove() {
		String result = Converters.URI_SANITIZING_CONVERTER.apply("https://test:password@example.com/my/api");
		assertThat(result, equalTo("https://test:" + Converters.REMOVED_TAG + "@example.com/my/api"));
	}

	public static Iterable<Object[]> prettierData() {
		return Arrays.asList(
				new Object[] { Converters.JSON_PRETTIER, "{\"object\": {\"key\": \"value\"}}",
						"{\n  \"object\" : {\n    \"key\" : \"value\"\n  }\n}" },
				new Object[] { Converters.XML_PRETTIER, "<test><key><value>value</value></key></test>",
						"<test>\n  <key>\n    <value>value</value>\n  </key>\n</test>" },
				new Object[] { Converters.HTML_PRETTIER, "<html><body><h1>hello world</h1></body></html>",
						"<html>\n  <head></head>\n  <body>\n    <h1>hello world</h1>\n  </body>\n</html>" }
		);
	}

	@ParameterizedTest
	@MethodSource("prettierData")
	public void test_prettiers(Function<String, String> prettier, String input, String expected) {
		assertThat(prettier.apply(input), equalTo(expected));
	}
}
