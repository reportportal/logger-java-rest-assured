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

import com.epam.reportportal.internal.support.Prettiers;
import com.epam.reportportal.listeners.LogLevel;
import com.epam.reportportal.message.ReportPortalMessage;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.utils.files.Utils;
import io.restassured.filter.FilterContext;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.RequestSender;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReportPortalRestAssuredLoggingFilterTest {

	private static final String HTML_TYPE = "text/html";
	private static final String JSON_TYPE = "application/json";
	private static final String METHOD = "POST";
	private static final String URI = "http://docker.local:8080/app";
	private static final int STATUS_CODE = 201;
	private static final String STATUS_LINE = String.format("HTTP/1.1 %d", STATUS_CODE);
	private static final String EMPTY_REQUEST = "**>>> REQUEST**\n" + METHOD + " to " + URI;
	private static final String EMPTY_RESPONSE = "**<<< RESPONSE**\n" + STATUS_LINE;
	private static final String HTTP_HEADER = HttpHeaders.CONTENT_TYPE;
	private static final String HTTP_HEADER_VALUE = JSON_TYPE;

	private static FilterContext getFilterContext(Response response) {
		return new FilterContext() {
			@Override
			public void setValue(String name, Object value) {

			}

			@Override
			public <T> T getValue(String name) {
				return null;
			}

			@Override
			public boolean hasValue(String name) {
				return false;
			}

			@Override
			public Response send(RequestSender requestSender) {
				return null;
			}

			@Override
			public Response next(FilterableRequestSpecification requestSpecification,
					FilterableResponseSpecification responseSpecification) {
				return response;
			}
		};
	}

	public static Iterable<Object[]> requestData() {
		return Arrays.asList(new Object[] { JSON_TYPE, "{\"object\": {\"key\": \"value\"}}", "{\"object\": {\"key\": \"value\"}}",
						Prettiers.JSON_PRETTIER, null, null },
				new Object[] { "application/xml", "<test><key><value>value</value></key></test>",
						"<test><key><value>value</value></key></test>", Prettiers.XML_PRETTIER, null, null }
		);
	}

	private ArgumentCaptor<String> runFilterTextMessageCapture(FilterableRequestSpecification requestSpecification, Response responseObject) {
		ArgumentCaptor<String> logCapture = ArgumentCaptor.forClass(String.class);
		try (MockedStatic<ReportPortal> utilities = Mockito.mockStatic(ReportPortal.class)) {
			utilities.when(() -> ReportPortal.emitLog(logCapture.capture(), anyString(), any(Date.class))).thenReturn(Boolean.TRUE);
			new ReportPortalRestAssuredLoggingFilter(42, LogLevel.INFO).filter(requestSpecification,
					null,
					getFilterContext(responseObject)
			);
		}
		return logCapture;
	}

	private ArgumentCaptor<ReportPortalMessage> runFilterComplexMessageCapture(FilterableRequestSpecification requestSpecification, Response responseObject) {
		ArgumentCaptor<ReportPortalMessage> logCapture = ArgumentCaptor.forClass(ReportPortalMessage.class);
		try (MockedStatic<ReportPortal> utilities = Mockito.mockStatic(ReportPortal.class)) {
			utilities.when(() -> ReportPortal.emitLog(logCapture.capture(), anyString(), any(Date.class))).thenReturn(Boolean.TRUE);
			new ReportPortalRestAssuredLoggingFilter(42, LogLevel.INFO).filter(requestSpecification,
					null,
					getFilterContext(responseObject)
			);
		}
		return logCapture;
	}

	private static FilterableRequestSpecification mockBasicRequest(String contentType) {
		FilterableRequestSpecification requestSpecification = mock(FilterableRequestSpecification.class);
		when(requestSpecification.getMethod()).thenReturn(METHOD);
		when(requestSpecification.getURI()).thenReturn(URI);
		when(requestSpecification.getContentType()).thenReturn(contentType);
		return requestSpecification;
	}

	private static Response mockBasicResponse(String contentType) {
		Response responseObject = mock(Response.class);
		when(responseObject.getStatusLine()).thenReturn(STATUS_LINE);
		when(responseObject.getContentType()).thenReturn(contentType);
		return responseObject;
	}

	@Test
	public void test_rest_assured_logger_null_values() {
		FilterableRequestSpecification requestSpecification = mockBasicRequest(HTML_TYPE);
		Response responseObject = mockBasicResponse(HTML_TYPE);

		ArgumentCaptor<String> logCapture = runFilterTextMessageCapture(requestSpecification, responseObject);

		List<String> logs = logCapture.getAllValues();
		assertThat(logs, hasSize(2)); // Request + Response

		assertThat(logs.get(0), equalTo(EMPTY_REQUEST));
		assertThat(logs.get(1), equalTo(EMPTY_RESPONSE));
	}

	@ParameterizedTest
	@MethodSource("requestData")
	@SuppressWarnings("rawtypes")
	public void test_rest_assured_logger_text_body(String mimeType, Object requestBody, Object responseBody,
			Function<String, String> prettier) {
		FilterableRequestSpecification requestSpecification = mockBasicRequest(mimeType);
		when(requestSpecification.getBody()).thenReturn(requestBody);

		Response responseObject = mockBasicResponse(mimeType);
		ResponseBody responseBodyObject = mock(ResponseBody.class);
		when(responseObject.getBody()).thenReturn(responseBodyObject);
		when(responseBodyObject.asString()).thenReturn((String) responseBody);

		ArgumentCaptor<String> logCapture = runFilterTextMessageCapture(requestSpecification, responseObject);

		List<String> logs = logCapture.getAllValues();
		assertThat(logs, hasSize(2)); // Request + Response

		String expectedRequest = EMPTY_REQUEST + "\n\n**Body**\n```\n" + prettier.apply((String) requestBody) + "\n```";
		String request = logs.get(0);
		assertThat(request, equalTo(expectedRequest));

		String expectedResponse = EMPTY_RESPONSE + "\n\n**Body**\n```\n" + prettier.apply((String) responseBody) + "\n```";
		String response = logs.get(1);
		assertThat(response, equalTo(expectedResponse));
	}

	@Test
	public void test_rest_assured_logger_headers() {
		Headers headers = new Headers(new Header(HTTP_HEADER, HTTP_HEADER_VALUE));
		FilterableRequestSpecification requestSpecification = mockBasicRequest(HTML_TYPE);
		when(requestSpecification.getHeaders()).thenReturn(headers);
		Response responseObject = mockBasicResponse(HTML_TYPE);
		when(responseObject.getHeaders()).thenReturn(headers);

		ArgumentCaptor<String> logCapture = runFilterTextMessageCapture(requestSpecification, responseObject);

		List<String> logs = logCapture.getAllValues();
		assertThat(logs, hasSize(2)); // Request + Response

		String headerString = "\n\n**Headers**\n" + HTTP_HEADER + ": " + HTTP_HEADER_VALUE;

		assertThat(logs.get(0), equalTo(EMPTY_REQUEST + headerString));
		assertThat(logs.get(1), equalTo(EMPTY_RESPONSE + headerString));
	}

	@Test
	public void test_rest_assured_logger_cookies() {
		Cookie cookie = new Cookie.Builder("test").setComment("test comment")
				.setDomain("example.com")
				.setHttpOnly(false)
				.setVersion(1)
				.setExpiryDate(new Date())
				.build();
		Cookies cookies = new Cookies(cookie);
		FilterableRequestSpecification requestSpecification = mockBasicRequest(HTML_TYPE);
		when(requestSpecification.getCookies()).thenReturn(cookies);
		Response responseObject = mockBasicResponse(HTML_TYPE);
		when(responseObject.getDetailedCookies()).thenReturn(cookies);

		ArgumentCaptor<String> logCapture = runFilterTextMessageCapture(requestSpecification, responseObject);

		List<String> logs = logCapture.getAllValues();
		assertThat(logs, hasSize(2)); // Request + Response

		String headerString = "\n\n**Cookies**\n" + cookie.toString();

		assertThat(logs.get(0), equalTo(EMPTY_REQUEST + headerString));
		assertThat(logs.get(1), equalTo(EMPTY_RESPONSE + headerString));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void test_rest_assured_logger_null_image_body() {
		String mimeType = ContentType.IMAGE_JPEG.getMimeType();
		FilterableRequestSpecification requestSpecification = mockBasicRequest(mimeType);
		when(requestSpecification.getBody()).thenReturn(null);

		Response responseObject = mockBasicResponse(mimeType);
		ResponseBody responseBodyObject = mock(ResponseBody.class);
		when(responseObject.getBody()).thenReturn(responseBodyObject);
		when(responseBodyObject.asByteArray()).thenReturn(null);

		ArgumentCaptor<String> logCapture = runFilterTextMessageCapture(requestSpecification, responseObject);

		List<String> logs = logCapture.getAllValues();
		assertThat(logs, hasSize(2)); // Request + Response
		assertThat(logs.get(0), equalTo(EMPTY_REQUEST));
		assertThat(logs.get(1), equalTo(EMPTY_RESPONSE));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void test_rest_assured_logger_image_body() throws IOException {
		String mimeType = ContentType.IMAGE_JPEG.getMimeType();
		FilterableRequestSpecification requestSpecification = mockBasicRequest(mimeType);
		byte[] image = ofNullable(this.getClass().getClassLoader().getResourceAsStream("pug/lucky.jpg")).map(is -> {
			try {
				return Utils.readInputStreamToBytes(is);
			} catch (IOException e) {
				return null;
			}
		}).orElse(null);
		when(requestSpecification.getBody()).thenReturn(image);

		Response responseObject = mockBasicResponse(mimeType);
		ResponseBody responseBodyObject = mock(ResponseBody.class);
		when(responseObject.getBody()).thenReturn(responseBodyObject);
		when(responseBodyObject.asByteArray()).thenReturn(image);

		ArgumentCaptor<ReportPortalMessage> logCapture = runFilterComplexMessageCapture(requestSpecification, responseObject);

		List<ReportPortalMessage> logs = logCapture.getAllValues();
		assertThat(logs, hasSize(2)); // Request + Response
		assertThat(logs.get(0).getMessage(), equalTo(EMPTY_REQUEST));
		assertThat(logs.get(1).getMessage(), equalTo(EMPTY_RESPONSE));

		assertThat(logs.get(0).getData().getMediaType(), equalTo(mimeType));
		assertThat(logs.get(1).getData().getMediaType(), equalTo(mimeType));

		assertThat(logs.get(0).getData().read(), equalTo(image));
		assertThat(logs.get(1).getData().read(), equalTo(image));
	}
}
