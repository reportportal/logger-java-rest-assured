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

import io.restassured.filter.FilterContext;
import io.restassured.filter.OrderedFilter;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

public class ReportPortalRestAssuredLoggingFilter implements OrderedFilter {

	private final int order;

	public ReportPortalRestAssuredLoggingFilter(int filterOrder) {
		order = filterOrder;
	}

	@Override
	public int getOrder() {
		return order;
	}

	private void emitLog(FilterableRequestSpecification requestSpec) {

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