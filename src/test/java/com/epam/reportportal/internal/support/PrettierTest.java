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

package com.epam.reportportal.internal.support;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.function.Function;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class PrettierTest {

	public static Iterable<Object[]> prettierData() {
		return Arrays.asList(
				new Object[] { Prettiers.JSON_PRETTIER, "{\"object\": {\"key\": \"value\"}}",
						"{\n  \"object\" : {\n    \"key\" : \"value\"\n  }\n}" },
				new Object[] { Prettiers.XML_PRETTIER, "<test><key><value>value</value></key></test>",
						"<test>\n  <key>\n    <value>value</value>\n  </key>\n</test>" },
				new Object[] { Prettiers.HTML_PRETTIER, "<html><body><h1>hello world</h1></body></html>",
						"<html>\n  <head></head>\n  <body>\n    <h1>hello world</h1>\n  </body>\n</html>" }
		);
	}

	@ParameterizedTest
	@MethodSource("prettierData")
	public void test_prettiers(Function<String, String> prettier, String input, String expected) {
		assertThat(prettier.apply(input), equalTo(expected));
	}

}
