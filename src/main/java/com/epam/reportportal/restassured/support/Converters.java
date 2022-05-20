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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.Cookie;
import io.restassured.http.Header;
import org.apache.http.HttpHeaders;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import static java.util.Optional.ofNullable;

/**
 * Static converters class, contains all basic converters used by the logger.
 */
public class Converters {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private static final Document.OutputSettings OUTPUT_SETTINGS = new Document.OutputSettings().indentAmount(2);

	public static final Function<String, String> JSON_PRETTIER = json -> {
		try {
			JsonNode node = OBJECT_MAPPER.readTree(json);
			return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node).trim();
		} catch (Exception ignore) {
			return json;
		}
	};

	public static final Function<String, String> XML_PRETTIER = xml -> {
		try {
			InputSource src = new InputSource(new StringReader(xml));
			org.w3c.dom.Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src);

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setAttribute("indent-number", 2);
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			Writer out = new StringWriter();
			transformer.transform(new DOMSource(document), new StreamResult(out));
			return out.toString().trim();
		} catch (Exception ignore) {
			return xml;
		}
	};

	public static final Function<String, String> HTML_PRETTIER = html -> {
		try {
			return Jsoup.parse(html).outputSettings(OUTPUT_SETTINGS).html().trim();
		} catch (Exception ignore) {
			return html;
		}
	};

	public static final Function<Cookie, String> DEFAULT_COOKIE_CONVERTER = Cookie::toString;

	public static final Function<Header, String> DEFAULT_HEADER_CONVERTER = h -> h.getName() + ": " + h.getValue().replace("*", "\\*");

	public static final Function<String, String> DEFAULT_URI_CONVERTER = u -> u;

	public static final String REMOVED_TAG = "&lt;removed&gt;";

	public static final Set<String> SESSION_COOKIES = new HashSet<>(Arrays.asList("sid",
			"session",
			"session_id",
			"sessionId",
			"sessionid"
	));

	public static final Function<Cookie, String> COOKIE_SANITIZING_CONVERTER = cookie -> SESSION_COOKIES.contains(cookie.getName()) ?
			cookie.getName() + "=" + REMOVED_TAG :
			DEFAULT_COOKIE_CONVERTER.apply(cookie);

	public static final Function<Header, String> HEADER_SANITIZING_CONVERTER = header -> HttpHeaders.AUTHORIZATION.equals(header.getName()) ?
			header.getName() + ": " + REMOVED_TAG :
			DEFAULT_HEADER_CONVERTER.apply(header);

	public static final Function<String, String> URI_SANITIZING_CONVERTER = uriStr -> {
		try {
			URI uri = URI.create(uriStr);
			String userInfo = ofNullable(uri.getUserInfo()).filter(info -> !info.isEmpty()).map(info -> info.split(":", 2)).map(info -> {
				if (info.length > 1) {
					return new String[] { info[0], REMOVED_TAG };
				}
				return info;
			}).map(info -> String.join(":", info)).orElse(null);
			return new URI(uri.getScheme(),
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
	};

	private Converters() {
		throw new IllegalStateException("Static only class");
	}
}
