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

package com.epam.reportportal.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.w3c.dom.Document;
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
import java.util.function.Function;

public class Prettiers {

	public static final Function<String, String> JSON_PRETTIER = json -> {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = mapper.valueToTree(json);
		try {
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
		} catch (JsonProcessingException e) {
			return null;
		}
	};

	public static final Function<String, String> XML_PRETTIER = xml -> {
		InputSource src = new InputSource(new StringReader(xml));
		try {
			Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src);

			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setAttribute("indent-number", 2);
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			Writer out = new StringWriter();
			transformer.transform(new DOMSource(document), new StreamResult(out));
			return out.toString();
		} catch (IOException | ParserConfigurationException | TransformerException | SAXException e) {
			return null;
		}
	};

	public static final Function<String, String> HTML_PRETTIER = html -> Jsoup.parse(html).body().html();

	private Prettiers() {
		throw new IllegalStateException("Static only class");
	}
}
