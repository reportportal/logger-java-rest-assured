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

package com.epam.reportportal.restassured;

import com.epam.reportportal.message.ReportPortalMessage;
import com.epam.reportportal.service.ReportPortal;
import com.google.common.io.ByteSource;
import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.apache.http.entity.ContentType;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class ReportPortalRestAssuredLoggingFilter implements OrderedFilter {

	private static final String MULTIPART_TYPE = ContentType.MULTIPART_FORM_DATA.getMimeType();

	private static final Set<String> TEXT_CONTENT_TYPES = new HashSet<>(Arrays.asList(ContentType.APPLICATION_JSON.getMimeType(),
			ContentType.TEXT_PLAIN.getMimeType(),
			ContentType.TEXT_HTML.getMimeType(),
			ContentType.TEXT_XML.getMimeType(),
			ContentType.APPLICATION_XML.getMimeType(),
			ContentType.DEFAULT_TEXT.getMimeType()
	));

	private static final Set<String> IMAGE_CONTENT_TYPES = new HashSet<>(Arrays.asList(ContentType.IMAGE_BMP.getMimeType(),
			ContentType.IMAGE_GIF.getMimeType(),
			ContentType.IMAGE_JPEG.getMimeType(),
			ContentType.IMAGE_PNG.getMimeType(),
			ContentType.IMAGE_SVG.getMimeType(),
			ContentType.IMAGE_TIFF.getMimeType(),
			ContentType.IMAGE_WEBP.getMimeType()
	));

	private final int order;
	private final String logLevel;

	public ReportPortalRestAssuredLoggingFilter(int filterOrder, String defaultLogLevel) {
		order = filterOrder;
		logLevel = defaultLogLevel;
	}

	@Override
	public int getOrder() {
		return order;
	}

	private void attachAsBinary(String message, byte[] attachment, String contentType) {
		if (attachment == null) {
			ReportPortal.emitLog(message, logLevel, new Date());
		} else {
			ReportPortal.emitLog(new ReportPortalMessage(ByteSource.wrap(attachment), contentType, message), logLevel, new Date());
		}
	}

	private void emitLog(FilterableRequestSpecification request) {
		String logText = String.format("**>>> REQUEST**" + "\n%s to %s", request.getMethod(), request.getURI());
		String rqContent = ContentType.parse(request.getContentType()).getMimeType();

		if (TEXT_CONTENT_TYPES.contains(rqContent)) {

		} else if (IMAGE_CONTENT_TYPES.contains(rqContent)) {

		} else if (MULTIPART_TYPE.equals(rqContent)) {

		} else {
			attachAsBinary(logText, request.getBody(), rqContent);
		}
	}

	private void emitLog(Response response) {

	}

	@Override
	public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
		emitLog(requestSpec);
		Response response = ctx.next(requestSpec, responseSpec);
		emitLog(response);
		return response;
	}
}