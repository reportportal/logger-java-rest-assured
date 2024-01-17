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

import com.epam.reportportal.formatting.http.Constants;
import com.epam.reportportal.formatting.http.converters.DefaultCookieConverter;
import com.epam.reportportal.formatting.http.prettiers.JsonPrettier;
import com.epam.reportportal.formatting.http.prettiers.XmlPrettier;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.LogLevel;
import com.epam.reportportal.message.ReportPortalMessage;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.step.StepReporter;
import com.epam.reportportal.utils.files.Utils;
import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.http.Cookie;
import io.restassured.http.Cookies;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import io.restassured.specification.MultiPartSpecification;
import io.restassured.specification.RequestSender;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.epam.reportportal.restassured.ReportPortalRestAssuredLoggingFilter.NULL_RESPONSE;
import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class ReportPortalRestAssuredLoggingFilterTest {

	private static final String IMAGE = "pug/lucky.jpg";
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
						JsonPrettier.INSTANCE, null, null },
				new Object[] { "application/xml", "<test><key><value>value</value></key></test>",
						"<test><key><value>value</value></key></test>", XmlPrettier.INSTANCE, null, null }
		);
	}

	private void runFilter(FilterableRequestSpecification requestSpecification, Response responseObject,
			Consumer<MockedStatic<ReportPortal>> mocks, OrderedFilter filter) {
		try (MockedStatic<ReportPortal> utilities = Mockito.mockStatic(ReportPortal.class)) {
			mocks.accept(utilities);
			filter.filter(requestSpecification, null, getFilterContext(responseObject));
		}
	}

	private void runFilter(FilterableRequestSpecification requestSpecification, Response responseObject,
			Consumer<MockedStatic<ReportPortal>> mocks) {
		runFilter(requestSpecification, responseObject, mocks, new ReportPortalRestAssuredLoggingFilter(42, LogLevel.INFO));
	}

	private List<String> runFilterTextMessageCapture(FilterableRequestSpecification requestSpecification, Response responseObject) {
		ArgumentCaptor<String> logCapture = ArgumentCaptor.forClass(String.class);
		runFilter(requestSpecification,
				responseObject,
				mock -> mock.when(() -> ReportPortal.emitLog(logCapture.capture(), anyString(), any(Date.class))).thenReturn(Boolean.TRUE)
		);
		return logCapture.getAllValues();
	}

	private List<ReportPortalMessage> runFilterBinaryMessageCapture(FilterableRequestSpecification requestSpecification,
			Response responseObject) {
		ArgumentCaptor<ReportPortalMessage> logCapture = ArgumentCaptor.forClass(ReportPortalMessage.class);
		runFilter(requestSpecification,
				responseObject,
				mock -> mock.when(() -> ReportPortal.emitLog(logCapture.capture(), anyString(), any(Date.class))).thenReturn(Boolean.TRUE)
		);
		return logCapture.getAllValues();
	}

	private Triple<List<String>, List<String>, List<ReportPortalMessage>> runFilterComplexMessageCapture(
			FilterableRequestSpecification requestSpecification, Response responseObject, OrderedFilter filter) {
		ArgumentCaptor<String> stepCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<ReportPortalMessage> messageArgumentCaptor = ArgumentCaptor.forClass(ReportPortalMessage.class);
		try (MockedStatic<Launch> utilities = Mockito.mockStatic(Launch.class)) {
			Launch launch = mock(Launch.class);
			StepReporter reporter = mock(StepReporter.class);
			utilities.when(Launch::currentLaunch).thenReturn(launch);
			when(launch.getStepReporter()).thenReturn(reporter);
			doNothing().when(reporter).sendStep(any(ItemStatus.class), stepCaptor.capture());
			runFilter(requestSpecification, responseObject, mock -> {
				mock.when(() -> ReportPortal.emitLog(stringArgumentCaptor.capture(), anyString(), any(Date.class)))
						.thenReturn(Boolean.TRUE);
				mock.when(() -> ReportPortal.emitLog(messageArgumentCaptor.capture(), anyString(), any(Date.class)))
						.thenReturn(Boolean.TRUE);
			}, filter);
		}
		return Triple.of(stepCaptor.getAllValues(), stringArgumentCaptor.getAllValues(), messageArgumentCaptor.getAllValues());
	}

	@SuppressWarnings("SameParameterValue")
	private Triple<List<String>, List<String>, List<ReportPortalMessage>> runFilterComplexMessageCapture(
			FilterableRequestSpecification requestSpecification, Response responseObject) {
		return runFilterComplexMessageCapture(requestSpecification,
				responseObject,
				new ReportPortalRestAssuredLoggingFilter(42, LogLevel.INFO)
		);
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
		FilterableRequestSpecification requestSpecification = mockBasicRequest(null);
		Response responseObject = mockBasicResponse(null);

		List<String> logs = runFilterTextMessageCapture(requestSpecification, responseObject);

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

		List<String> logs = runFilterTextMessageCapture(requestSpecification, responseObject);
		assertThat(logs, hasSize(2)); // Request + Response

		String expectedRequest = EMPTY_REQUEST + "\n\n**Body**\n```\n" + prettier.apply((String) requestBody) + "\n```";
		String request = logs.get(0);
		assertThat(request, equalTo(expectedRequest));

		String expectedResponse = EMPTY_RESPONSE + "\n\n**Body**\n```\n" + prettier.apply((String) responseBody) + "\n```";
		String response = logs.get(1);
		assertThat(response, equalTo(expectedResponse));
	}

	public static Iterable<Object[]> testTypes() {
		return Arrays.asList(new Object[] { HTML_TYPE }, new Object[] { null });
	}

	@ParameterizedTest
	@MethodSource("testTypes")
	public void test_rest_assured_logger_headers(String contentType) {
		Headers headers = new Headers(new Header(HTTP_HEADER, HTTP_HEADER_VALUE));
		FilterableRequestSpecification requestSpecification = mockBasicRequest(contentType);
		when(requestSpecification.getHeaders()).thenReturn(headers);
		Response responseObject = mockBasicResponse(contentType);
		when(responseObject.getHeaders()).thenReturn(headers);

		List<String> logs = runFilterTextMessageCapture(requestSpecification, responseObject);
		assertThat(logs, hasSize(2)); // Request + Response

		String headerString = "\n\n**Headers**\n" + HTTP_HEADER + ": " + HTTP_HEADER_VALUE;

		assertThat(logs.get(0), equalTo(EMPTY_REQUEST + headerString));
		assertThat(logs.get(1), equalTo(EMPTY_RESPONSE + headerString));
	}

	private static String formatCookie(Cookie cookie) {
		SimpleDateFormat sdf = new SimpleDateFormat(DefaultCookieConverter.DEFAULT_COOKIE_DATE_FORMAT);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return "**Cookies**\n" + String.format("%s: Comment=%s; Domain=%s; Expires=%s; Version=%d",
				cookie.getName(),
				cookie.getComment(),
				cookie.getDomain(),
				sdf.format(cookie.getExpiryDate()),
				cookie.getVersion()
		);
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

		List<String> logs = runFilterTextMessageCapture(requestSpecification, responseObject);
		assertThat(logs, hasSize(2)); // Request + Response

		String headerString = "\n\n" + formatCookie(cookie);

		assertThat(logs.get(0), equalTo(EMPTY_REQUEST + headerString));
		assertThat(logs.get(1), equalTo(EMPTY_RESPONSE + headerString));
	}

	@ParameterizedTest
	@MethodSource("testTypes")
	public void test_rest_assured_logger_headers_and_cookies(String contentType) {
		Cookie cookie = new Cookie.Builder("test").setComment("test comment")
				.setDomain("example.com")
				.setHttpOnly(false)
				.setVersion(1)
				.setExpiryDate(new Date())
				.build();
		Cookies cookies = new Cookies(cookie);
		Headers headers = new Headers(new Header(HTTP_HEADER, HTTP_HEADER_VALUE));
		FilterableRequestSpecification requestSpecification = mockBasicRequest(contentType);
		when(requestSpecification.getHeaders()).thenReturn(headers);
		when(requestSpecification.getCookies()).thenReturn(cookies);
		Response responseObject = mockBasicResponse(contentType);
		when(responseObject.getHeaders()).thenReturn(headers);
		when(responseObject.getDetailedCookies()).thenReturn(cookies);

		List<String> logs = runFilterTextMessageCapture(requestSpecification, responseObject);
		assertThat(logs, hasSize(2)); // Request + Response

		String headerString = "\n\n**Headers**\n" + HTTP_HEADER + ": " + HTTP_HEADER_VALUE + "\n\n" + formatCookie(cookie);

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

		List<String> logs = runFilterTextMessageCapture(requestSpecification, responseObject);
		assertThat(logs, hasSize(2)); // Request + Response
		assertThat(logs.get(0), equalTo(EMPTY_REQUEST));
		assertThat(logs.get(1), equalTo(EMPTY_RESPONSE));
	}

	@SuppressWarnings("SameParameterValue")
	private byte[] getResource(String imagePath) {
		return ofNullable(this.getClass().getClassLoader().getResourceAsStream(imagePath)).map(is -> {
			try {
				return Utils.readInputStreamToBytes(is);
			} catch (IOException e) {
				return null;
			}
		}).orElse(null);
	}

	private static final String IMAGE_TYPE = "image/jpeg";
	private static final String WILDCARD_TYPE = "*/*";

	@ParameterizedTest
	@ValueSource(strings = { IMAGE_TYPE, WILDCARD_TYPE })
	@SuppressWarnings("rawtypes")
	public void test_rest_assured_logger_image_body(String mimeType) throws IOException {
		FilterableRequestSpecification requestSpecification = mockBasicRequest(mimeType);
		byte[] image = getResource(IMAGE);
		when(requestSpecification.getBody()).thenReturn(image);

		Response responseObject = mockBasicResponse(mimeType);
		ResponseBody responseBodyObject = mock(ResponseBody.class);
		when(responseObject.getBody()).thenReturn(responseBodyObject);
		when(responseBodyObject.asByteArray()).thenReturn(image);

		List<ReportPortalMessage> logs = runFilterBinaryMessageCapture(requestSpecification, responseObject);
		assertThat(logs, hasSize(2)); // Request + Response
		assertThat(logs.get(0).getMessage(), equalTo(EMPTY_REQUEST));
		assertThat(logs.get(1).getMessage(), equalTo(EMPTY_RESPONSE));

		assertThat(logs.get(0).getData().getMediaType(), equalTo(mimeType));
		assertThat(logs.get(1).getData().getMediaType(), equalTo(mimeType));

		assertThat(logs.get(0).getData().read(), equalTo(image));
		assertThat(logs.get(1).getData().read(), equalTo(image));
	}

	@Test
	public void test_rest_assured_logger_null_response() {
		String mimeType = ContentType.IMAGE_JPEG.getMimeType();
		FilterableRequestSpecification requestSpecification = mockBasicRequest(mimeType);
		when(requestSpecification.getBody()).thenReturn(null);

		List<String> logs = runFilterTextMessageCapture(requestSpecification, null);
		assertThat(logs, hasSize(2)); // Request + Response
		assertThat(logs.get(0), equalTo(EMPTY_REQUEST));
		assertThat(logs.get(1), equalTo(NULL_RESPONSE));
	}

	public static Iterable<Object> emptyMultipartData() {
		return Arrays.asList(null, Collections.emptyList());
	}

	@ParameterizedTest
	@MethodSource("emptyMultipartData")
	public void test_rest_assured_logger_empty_multipart(List<MultiPartSpecification> data) {
		String mimeType = ContentType.MULTIPART_FORM_DATA.getMimeType();
		FilterableRequestSpecification requestSpecification = mockBasicRequest(mimeType);
		when(requestSpecification.getMultiPartParams()).thenReturn(data);

		List<String> logs = runFilterTextMessageCapture(requestSpecification, null);
		assertThat(logs, hasSize(2)); // Request + Response
		assertThat(logs.get(0), equalTo(EMPTY_REQUEST));
	}

	private MultiPartSpecification getBinaryPart(String mimeType, String filePath, boolean file, Map<String, String> headers) {
		return new MultiPartSpecification() {
			@Override
			public Object getContent() {
				return file ?
						new File(System.getProperty("user.dir") + FileSystems.getDefault().getSeparator()
								+ String.join(FileSystems.getDefault().getSeparator(), "src", "test", "resources", filePath)) :
						getResource(filePath);
			}

			@Override
			public String getControlName() {
				return null;
			}

			@Override
			public String getMimeType() {
				return mimeType;
			}

			@Override
			public Map<String, String> getHeaders() {
				return headers;
			}

			@Override
			public String getCharset() {
				return null;
			}

			@Override
			public String getFileName() {
				return null;
			}

			@Override
			public boolean hasFileName() {
				return false;
			}
		};
	}

	@SuppressWarnings("SameParameterValue")
	private MultiPartSpecification getBinaryPart(String mimeType, String filePath, boolean file) {
		return getBinaryPart(mimeType, filePath, file, null);
	}

	@ParameterizedTest
	@ValueSource(booleans = { true, false })
	public void test_rest_assured_logger_image_multipart(boolean isFile) throws IOException {
		byte[] image = getResource(IMAGE);
		String imageType = ContentType.IMAGE_JPEG.getMimeType();
		MultiPartSpecification part = getBinaryPart(ContentType.IMAGE_JPEG.getMimeType(), IMAGE, isFile);
		String mimeType = ContentType.MULTIPART_FORM_DATA.getMimeType();
		FilterableRequestSpecification requestSpecification = mockBasicRequest(mimeType);
		when(requestSpecification.getMultiPartParams()).thenReturn(Collections.singletonList(part));

		Triple<List<String>, List<String>, List<ReportPortalMessage>> logs = runFilterComplexMessageCapture(requestSpecification, null);
		assertThat(logs.getLeft(), hasSize(1));
		assertThat(logs.getMiddle(), hasSize(1));
		assertThat(logs.getRight(), hasSize(1));

		assertThat(logs.getLeft().get(0), equalTo(EMPTY_REQUEST));
		assertThat(logs.getMiddle().get(0), equalTo(NULL_RESPONSE));
		assertThat(logs.getRight().get(0).getMessage(), equalTo(Constants.BODY_PART_TAG + "\n" + imageType));
		assertThat(logs.getRight().get(0).getData().getMediaType(), equalTo(imageType));
		assertThat(logs.getRight().get(0).getData().read(), equalTo(image));
	}

	@SuppressWarnings("SameParameterValue")
	private MultiPartSpecification getTextPart(String mimeType, String text) {
		return new MultiPartSpecification() {
			@Override
			public Object getContent() {
				return text;
			}

			@Override
			public String getControlName() {
				return null;
			}

			@Override
			public String getMimeType() {
				return mimeType;
			}

			@Override
			public Map<String, String> getHeaders() {
				return Collections.singletonMap(HttpHeaders.CONTENT_TYPE, mimeType);
			}

			@Override
			public String getCharset() {
				return null;
			}

			@Override
			public String getFileName() {
				return null;
			}

			@Override
			public boolean hasFileName() {
				return false;
			}
		};
	}

	@Test
	public void test_rest_assured_logger_text_and_image_multipart() throws IOException {
		byte[] image = getResource(IMAGE);
		String requestType = ContentType.MULTIPART_FORM_DATA.getMimeType();
		String imageType = ContentType.IMAGE_JPEG.getMimeType();
		String textType = ContentType.TEXT_PLAIN.getMimeType();

		String message = "test_message";
		FilterableRequestSpecification requestSpecification = mockBasicRequest(requestType);
		when(requestSpecification.getMultiPartParams()).thenReturn(Arrays.asList(getTextPart(textType, message),
				getBinaryPart(ContentType.IMAGE_JPEG.getMimeType(), IMAGE, false)
		));

		Triple<List<String>, List<String>, List<ReportPortalMessage>> logs = runFilterComplexMessageCapture(requestSpecification, null);
		assertThat(logs.getLeft(), hasSize(1));
		assertThat(logs.getMiddle(), hasSize(2));
		assertThat(logs.getRight(), hasSize(1));

		assertThat(logs.getLeft().get(0), equalTo(EMPTY_REQUEST));

		assertThat(logs.getMiddle().get(0),
				equalTo(Constants.HEADERS_TAG + "\n" + HttpHeaders.CONTENT_TYPE + ": " + textType + "\n\n" + Constants.BODY_PART_TAG
						+ "\n```\n" + message + "\n```")
		);
		assertThat(logs.getMiddle().get(1), equalTo(NULL_RESPONSE));

		assertThat(logs.getRight().get(0).getMessage(), equalTo(Constants.BODY_PART_TAG + "\n" + imageType));
		assertThat(logs.getRight().get(0).getData().getMediaType(), equalTo(imageType));
		assertThat(logs.getRight().get(0).getData().read(), equalTo(image));
	}

	public static Iterable<Object[]> invalidContentTypes() {
		return Arrays.asList(new Object[] { "", ContentType.APPLICATION_OCTET_STREAM.getMimeType() },
				new Object[] { null, ContentType.APPLICATION_OCTET_STREAM.getMimeType() },
				new Object[] { "*/*", "*/*" },
				new Object[] { "something invalid", "something invalid" },
				new Object[] { "/", "/" },
				new Object[] { "#*'\\`%^!@/\"$;", "#*'\\`%^!@/\"$" },
				new Object[] { "a/a;F#%235f\\=f324$%^&", "a/a" }
		);
	}

	@ParameterizedTest
	@MethodSource("invalidContentTypes")
	@SuppressWarnings("rawtypes")
	public void test_rest_assured_logger_invalid_content_type(String mimeType, String expectedType) throws IOException {
		byte[] image = getResource(IMAGE);
		FilterableRequestSpecification requestSpecification = mockBasicRequest(mimeType);

		when(requestSpecification.getBody()).thenReturn(image);

		Response responseObject = mockBasicResponse(mimeType);
		ResponseBody responseBodyObject = mock(ResponseBody.class);
		when(responseObject.getBody()).thenReturn(responseBodyObject);
		when(responseBodyObject.asByteArray()).thenReturn(image);

		Triple<List<String>, List<String>, List<ReportPortalMessage>> logs = runFilterComplexMessageCapture(requestSpecification,
				responseObject
		);
		assertThat(logs.getRight(), hasSize(2)); // Request + Response
		assertThat(logs.getRight().get(0).getMessage(), equalTo(EMPTY_REQUEST));
		assertThat(logs.getRight().get(1).getMessage(), equalTo(EMPTY_RESPONSE));

		assertThat(logs.getRight().get(0).getData().getMediaType(), equalTo(expectedType));
		assertThat(logs.getRight().get(1).getData().getMediaType(), equalTo(expectedType));

		assertThat(logs.getRight().get(0).getData().read(), equalTo(image));
		assertThat(logs.getRight().get(1).getData().read(), equalTo(image));
	}

	@Test
	public void test_rest_assured_log_filter_type() {
		FilterableRequestSpecification requestSpecification = mockBasicRequest(HTML_TYPE);
		Response responseObject = mockBasicResponse(HTML_TYPE);
		Triple<List<String>, List<String>, List<ReportPortalMessage>> logs = runFilterComplexMessageCapture(requestSpecification,
				responseObject,
				new ReportPortalRestAssuredLoggingFilter(42, LogLevel.INFO).addRequestFilter(r -> true)
		);
		assertThat(logs.getRight(), hasSize(0));
	}

	@Test
	public void test_rest_assured_logger_text_as_file_multipart() {
		String textPath = "test.json";
		String text = JsonPrettier.INSTANCE.apply(new String(getResource(textPath)));
		String requestType = ContentType.MULTIPART_FORM_DATA.getMimeType();
		String textType = ContentType.APPLICATION_JSON.getMimeType();

		FilterableRequestSpecification requestSpecification = mockBasicRequest(requestType);
		when(requestSpecification.getMultiPartParams()).thenReturn(Collections.singletonList(getBinaryPart(textType,
				textPath,
				true,
				Collections.singletonMap(HttpHeaders.CONTENT_TYPE, textType)
		)));

		Triple<List<String>, List<String>, List<ReportPortalMessage>> logs = runFilterComplexMessageCapture(requestSpecification, null);
		assertThat(logs.getLeft(), hasSize(1));
		assertThat(logs.getMiddle(), hasSize(2));
		assertThat(logs.getRight(), hasSize(0));

		assertThat(logs.getLeft().get(0), equalTo(EMPTY_REQUEST));

		assertThat(logs.getMiddle().get(0),
				equalTo(Constants.HEADERS_TAG + "\n" + HttpHeaders.CONTENT_TYPE + ": " + textType + "\n\n" + Constants.BODY_PART_TAG
						+ "\n```\n" + text + "\n```")
		);
		assertThat(logs.getMiddle().get(1), equalTo(NULL_RESPONSE));
	}
}
