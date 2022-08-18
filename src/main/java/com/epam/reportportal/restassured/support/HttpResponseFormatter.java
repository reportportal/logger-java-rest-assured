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

import com.epam.reportportal.message.ReportPortalMessage;
import com.epam.reportportal.restassured.support.http.BodyType;

import javax.annotation.Nonnull;

public class HttpResponseFormatter {

	@Nonnull
	public BodyType getType() {
		return null;
	}

	public String formatHead() {
		return null;
	}

	public ReportPortalMessage formatAsText() {
		return null;
	}

	public byte[] getBinaryBody() {
		return new byte[0];
	}

	public String getMimeType() {
		return null;
	}
}
