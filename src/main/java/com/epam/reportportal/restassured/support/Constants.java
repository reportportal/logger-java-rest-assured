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

import com.epam.reportportal.restassured.support.prettiers.HtmlPrettier;
import com.epam.reportportal.restassured.support.prettiers.JsonPrettier;
import com.epam.reportportal.restassured.support.prettiers.XmlPrettier;
import org.apache.http.entity.ContentType;

import java.util.*;
import java.util.function.Function;

public class Constants {
	public static final String REMOVED_TAG = "&lt;removed&gt;";
	public static final String LINE_DELIMITER = "\n";
	public static final String HEADERS_TAG = "**Headers**";
	public static final String REQUEST_TAG = "**>>> REQUEST**";
	public static final String RESPONSE_TAG = "**<<< RESPONSE**";
	public static final String BODY_TAG = "**Body**";
	public static final String COOKIES_TAG = "**Cookies**";
	public static final String NULL_RESPONSE = "NULL response from RestAssured";
	public static final String BODY_PART_TAG = "**Body part**";

	public static final Set<String> MULTIPART_TYPES = Collections.singleton(ContentType.MULTIPART_FORM_DATA.getMimeType());

	public static final Set<String> TEXT_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			ContentType.APPLICATION_JSON.getMimeType(),
			ContentType.TEXT_PLAIN.getMimeType(),
			ContentType.TEXT_HTML.getMimeType(),
			ContentType.TEXT_XML.getMimeType(),
			ContentType.APPLICATION_XML.getMimeType(),
			ContentType.DEFAULT_TEXT.getMimeType()
	)));

	public static final Map<String, Function<String, String>> DEFAULT_PRETTIERS = Collections.unmodifiableMap(new HashMap<String, Function<String, String>>() {{
		put(ContentType.APPLICATION_XML.getMimeType(), XmlPrettier.INSTANCE);
		put(ContentType.APPLICATION_SOAP_XML.getMimeType(), XmlPrettier.INSTANCE);
		put(ContentType.APPLICATION_ATOM_XML.getMimeType(), XmlPrettier.INSTANCE);
		put(ContentType.APPLICATION_SVG_XML.getMimeType(), XmlPrettier.INSTANCE);
		put(ContentType.APPLICATION_XHTML_XML.getMimeType(), XmlPrettier.INSTANCE);
		put(ContentType.TEXT_XML.getMimeType(), XmlPrettier.INSTANCE);
		put(ContentType.APPLICATION_JSON.getMimeType(), JsonPrettier.INSTANCE);
		put("text/json", JsonPrettier.INSTANCE);
		put(ContentType.TEXT_HTML.getMimeType(), HtmlPrettier.INSTANCE);
	}});
}
