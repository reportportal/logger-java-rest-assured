# Report Portal logger for REST Assured

[![Maven Central](https://img.shields.io/maven-central/v/com.epam.reportportal/logger-java-rest-assured.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.epam.reportportal/logger-java-rest-assured)
[![CI Build](https://github.com/reportportal/logger-java-rest-assured/actions/workflows/ci.yml/badge.svg)](https://github.com/reportportal/logger-java-rest-assured/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/reportportal/logger-java-rest-assured/branch/develop/graph/badge.svg?token=W3MTDF607A)](https://codecov.io/gh/reportportal/logger-java-rest-assured)
[![Join Slack chat!](https://img.shields.io/badge/slack-join-brightgreen.svg)](https://slack.epmrpp.reportportal.io/)
[![stackoverflow](https://img.shields.io/badge/reportportal-stackoverflow-orange.svg?style=flat)](http://stackoverflow.com/questions/tagged/reportportal)
[![Build with Love](https://img.shields.io/badge/build%20with-❤%EF%B8%8F%E2%80%8D-lightgrey.svg)](http://reportportal.io?style=flat)

The latest version: 5.4.0. Please use `Maven Central` link above to get the logger.

## Overview

REST Assured Request/Response logger for Report Portal

The logger intercept and logs all Requests and Responses issued by REST Assured into Report Portal in Markdown format, including multipart
requests. It recognizes payload types and attach them in corresponding manner: image types will be logged as images with thumbnails, binary
types will be logged as entry attachments, text types will be formatted and logged in Markdown code blocks.

## Configuration

### Build system configuration

You need to add the logger as one of your dependencies in Maven or Gradle.

#### Maven

`pom.xml`

```xml

<project>
    <!-- project declaration omitted -->

    <dependencies>
        <dependency>
            <groupId>com.epam.reportportal</groupId>
            <artifactId>logger-java-rest-assured</artifactId>
            <version>5.4.0</version>
        </dependency>
    </dependencies>

    <!-- build config omitted -->
</project>
```

#### Gradle

`build.gradle`

```groovy
dependencies {
    testCompile 'com.epam.reportportal:logger-java-rest-assured:5.4.0'
}
```

### REST Assured configuration

To start getting Request and Response logging in Report Portal you need to add the logger as one of your REST Assured filters. The best
place for it is one which will be initialized at the earliest moment once during the test execution. E.G. a static initialization block in a
base class for all tests:

```java
public class BaseTest {
	static {
		RestAssured.filters(new ReportPortalRestAssuredLoggingFilter(42, LogLevel.INFO));
	}
}
```

If you don't have a base class, you can put initialization into one of the most general initialization block. E.G. for
TestNG it may be `@BeforeSuite`:

```java
public class ApiTest {
	@BeforeSuite
	public void setupRestAssured() {
		RestAssured.filters(new ReportPortalRestAssuredLoggingFilter(42, LogLevel.INFO));
	}
}
```

> **NOTE**: If you have more than one suite in your execution then it will mean REST Assured will be initialized over
> and over again with the Logger and you will get log duplication in the following suites. You can apply
> `RestAssured.reset();` before the filter adding to avoid that. But this also means you will have to configure
> REST Assured anew each suite.

### Sanitize Request / Response data

To avoid logging sensitive data into Report Portal you can use corresponding converters:

* Cookie converter
* Header converter
* URI converter
* Content prettiers

Cookie, Header and URI converters are set in the logger constructor:

```java
public class BaseTest {
	static {
		RestAssured.filters(new ReportPortalRestAssuredLoggingFilter(
				42,
				LogLevel.INFO,
				SanitizingHttpHeaderConverter.INSTANCE,
				DefaultHttpHeaderConverter.INSTANCE,
				DefaultCookieConverter.INSTANCE,
				DefaultUriConverter.INSTANCE
		));
	}
}
```

You are free to implement any converter by yourself with `java.util.function.Function` interface.

Content prettier are more complex, they parse data based on its content type and apply defined transformations. Default prettiers just
pretty-print JSON, HTML and XML data. To apply a custom content prettier call `ReportPortalRestAssuredLoggingFilter.setContentPrettiers`.
E.G.:

```java
public class BaseTest {
	private static final Map<String, Function<String, String>> MY_PRETTIERS = new HashMap<String, Function<String, String>>() {{
		put(ContentType.APPLICATION_XML.getMimeType(), XmlPrettier.INSTANCE);
		put(ContentType.APPLICATION_SOAP_XML.getMimeType(), XmlPrettier.INSTANCE);
		put(ContentType.APPLICATION_ATOM_XML.getMimeType(), XmlPrettier.INSTANCE);
		put(ContentType.APPLICATION_SVG_XML.getMimeType(), XmlPrettier.INSTANCE);
		put(ContentType.APPLICATION_XHTML_XML.getMimeType(), XmlPrettier.INSTANCE);
		put(ContentType.TEXT_XML.getMimeType(), XmlPrettier.INSTANCE);
		put(ContentType.APPLICATION_JSON.getMimeType(), JsonPrettier.INSTANCE);
		put("text/json", JsonPrettier.INSTANCE);
		put(ContentType.TEXT_HTML.getMimeType(), HtmlPrettier.INSTANCE);
	}};

	static {
		RestAssured.filters(new ReportPortalRestAssuredLoggingFilter(42, LogLevel.INFO).setContentPrettiers(MY_PRETTIERS));
	}
}
```
