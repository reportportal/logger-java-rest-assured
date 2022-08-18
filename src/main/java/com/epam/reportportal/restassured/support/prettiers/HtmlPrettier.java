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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class HtmlPrettier implements Prettier {
	private static final Document.OutputSettings OUTPUT_SETTINGS = new Document.OutputSettings().indentAmount(2);

	public static final HtmlPrettier INSTANCE = new HtmlPrettier();

	private final Document.OutputSettings settings;

	public HtmlPrettier(Document.OutputSettings outputSettings) {
		settings = outputSettings;
	}

	private HtmlPrettier() {
		this(OUTPUT_SETTINGS);
	}

	@Override
	public String apply(String html) {
		try {
			return Jsoup.parse(html).outputSettings(settings).html().trim();
		} catch (Exception ignore) {
			return html;
		}
	}
}
