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

import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class XmlPrettier implements Prettier {

	public static final XmlPrettier INSTANCE = new XmlPrettier();

	private static final int DEFAULT_INDENT = 2;

	private static final Map<String, String> DEFAULT_OUTPUT_PROPERTIES = new HashMap<String, String>() {{
		put(OutputKeys.ENCODING, "UTF-8");
		put(OutputKeys.OMIT_XML_DECLARATION, "yes");
		put(OutputKeys.INDENT, "yes");
	}};

	private final ThreadLocal<Transformer> threadLocal;

	public XmlPrettier(int indent, Map<String, String> outputSettings) {
		threadLocal = ThreadLocal.withInitial(() -> {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setAttribute("indent-number", indent);
			Transformer transformer;
			try {
				transformer = transformerFactory.newTransformer();
			} catch (TransformerConfigurationException e) {
				throw new IllegalArgumentException(e.getMessage(), e);
			}
			outputSettings.forEach(transformer::setOutputProperty);
			return transformer;
		});
	}

	private XmlPrettier() {
		this(DEFAULT_INDENT, DEFAULT_OUTPUT_PROPERTIES);
	}

	@Override
	public String apply(String xml) {
		try {
			InputSource src = new InputSource(new StringReader(xml));
			org.w3c.dom.Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src);
			Writer out = new StringWriter();
			threadLocal.get().transform(new DOMSource(document), new StreamResult(out));
			return out.toString().trim();
		} catch (Exception ignore) {
			return xml;
		}
	}
}
