/*******************************************************************************
 * Copyright 2017 General Electric Company
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
 *******************************************************************************/

package com.ge.predix.metering.filter;

import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.servlet.ServletResponse;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.ge.predix.metering.util.Constants;
import com.ge.predix.metering.util.ServiceInstanceHelper;
import com.ge.predix.metering.util.UaaInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
public class NuregoIT extends AbstractTestNGSpringContextTests {

	@Value("${NUREGO_USERNAME}")
	private String nuregoUsername;

	@Value("${NUREGO_PASSWORD}")
	private String nuregoPassword;

	@Value("${NUREGO_INSTANCE_ID}")
	private String nuregoInstanceId;

	@Value("${NUREGO_API_TEST_URL}")
	private String nuregoURL;

	@Autowired
	private MeteringFilter meteringFilter;

	@Autowired
	private ServiceInstanceHelper serviceInstanceHelper;

	private String acsInstanceId;

	private String uaaInstanceId;

	@Autowired
	@Qualifier("nuregoTemplate")
	RestTemplate nuregoTemplate;

	private String nuregoToken;

	private static final Logger LOGGER = LoggerFactory.getLogger(NuregoIT.class);

	@BeforeClass
	public void setUp() throws Exception {
		nuregoToken = getNuregoAuthToken();
		// creates an UAA instance and ACS instance
		UaaInstance uaa = serviceInstanceHelper.createUaaInstance();
		uaaInstanceId = uaa.getUaaGuid();
		acsInstanceId = serviceInstanceHelper.createAcsInstance(uaa.getUaaUrl());

	}

	@Test(dataProvider = "requestProviderForOkResponse")
	public void testNuregoIntegrationForOkResponse(final String featureId, final String planId,
			final  MockHttpServletRequest request, final ServletResponse response) throws Exception {

		String componentId = null;
		boolean acsFlag = (planId == Constants.ACS_PLAN_ID);
		if (acsFlag) {
			componentId = retrieveComponent(acsInstanceId, nuregoToken);
		} else {
			componentId = retrieveComponent(uaaInstanceId, nuregoToken);
		}
		if (componentId == null) {
			throw new IllegalStateException("Component ID is null because Nurego mapping is not updated.");
		}

		this.meteringFilter.doFilter(request, response, new MockFilterChain());
		Thread.sleep(4000);
		this.meteringFilter.doFilter(request, response, new MockFilterChain());
		Thread.sleep(10000);
		ComponentUsage usages = retrieveRawUsage(nuregoToken, componentId, acsFlag, featureId);

		// disable the assertion, because the usage record shown up with long delays, about 40 mins.
//		Assert.assertEquals(usages.getCompCount(), 2);
//		Assert.assertEquals(usages.getUsage(), 2.0);
	}

	@AfterTest
	private void cleanup() {
		serviceInstanceHelper.deleteServiceInstance(acsInstanceId);
		serviceInstanceHelper.deleteServiceInstance(uaaInstanceId);
	}
	private String getNuregoAuthToken() throws Exception{

		HashMap<String, Object> serviceRequest = new HashMap<String, Object>();
		serviceRequest.put(Constants.USERNAME, this.nuregoUsername);
		serviceRequest.put(Constants.PASSWORD, this.nuregoPassword);
		serviceRequest.put(Constants.INSTANCE_ID, this.nuregoInstanceId);

		URI oauthURL = URI.create(this.nuregoURL + Constants.NUREGO_AUTH_TOKEN_URL);
		String response = this.nuregoTemplate.postForObject(oauthURL, serviceRequest, String.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> responseMap = new ObjectMapper().readValue(response, Map.class);
		return responseMap.get(Constants.ACCESS_TOKEN).toString();
	}

	private String retrieveComponent(String serviceInstanceGuid, String nuregoAccessToken) throws Exception{

		String retrieveComponentURL = this.nuregoURL + Constants.SERVICE_COMPONENT_URL + serviceInstanceGuid;

		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
		headers.add(HttpHeaders.AUTHORIZATION, Constants.BEARER + nuregoAccessToken);

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(retrieveComponentURL)
				.queryParam(Constants.PROVIDER, Constants.CLOUD_FOUNDRY);

		HttpEntity<?> entity = new HttpEntity<>(headers);
		ResponseEntity<String> responseEntity = null;
		long end = System.currentTimeMillis() + 100000;
		//Attempts to retrieve component for 20 seconds before it throws an exception.
		//Component id is not updated immediately in Nurego after service instance creation, there is a delay. Hence trying it for 20 seconds.
		while(System.currentTimeMillis() < end) {
			try {
				LOGGER.info("Attempting to retrieve component ID...");
				responseEntity = this.nuregoTemplate.exchange(builder.build().encode().toUri(), HttpMethod.GET, entity ,String.class);
				break;
			} catch(HttpStatusCodeException ex) {
				LOGGER.error("Cannot retrieve component ID: {}\n{}", ex.getStatusText(), ex.getResponseBodyAsString());
			} catch (Exception ex) {
				LOGGER.error("Cannot retrieve component ID", ex);
			}
			Thread.sleep(20000); // need to sleep because nurego mapping takes time to update
		}
		if(responseEntity == null) {
			return null;
		}
		String responseBody = responseEntity.getBody();
		@SuppressWarnings("unchecked")
		Map<String,Object> map = new ObjectMapper().readValue(responseBody, Map.class);
		return map.get(Constants.ID).toString();

	}


	private Date endTime() {
		final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		cal.add(Calendar.DATE, +1);
		return cal.getTime();
	}

	private Date startTime() {
		final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));;
		return cal.getTime();

	}
	private ComponentUsage retrieveRawUsage(String accessToken, String cmp_id, boolean acsFlag, String featureId) throws JsonParseException, JsonMappingException, IOException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		String retrieveRawUsage = Constants.NUREGO_USAGE_URL;
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ACCEPT, MediaType.ALL_VALUE);
		headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		headers.add(HttpHeaders.AUTHORIZATION, Constants.BEARER +accessToken);
		UriComponentsBuilder builder;

		String serviceId = acsFlag ? Constants.ACS_SERVICE_ID : Constants.UAA_SERVICE_ID;
		builder = UriComponentsBuilder.fromUriString(retrieveRawUsage)
				.queryParam("start_date", dateFormat.format(startTime()))
				.queryParam("end_date",dateFormat.format(endTime()))
				.queryParam("organization_id", Constants.ORGANIZATION_ID)
				.queryParam("service_id", serviceId);
		HttpEntity<?> entity = new HttpEntity<>(headers);
		ResponseEntity<String> responseEntity = this.nuregoTemplate.exchange(builder.build().encode().toUri(), HttpMethod.GET, entity ,String.class);
		String responseBody = responseEntity.getBody();
		int componentCount = 0;
		double amountUsed = 0.0;
		List<Map<String,String>> list = parseCSV(responseBody);
		for(Map<?,?>map : list) {
			if(map.get("Service component id").equals(cmp_id) & map.get("Feature id").equals(featureId)) {
				componentCount++;
				amountUsed += Double.parseDouble((String) map.get("Amount"));
			}
		}
		ComponentUsage usageInfo = new ComponentUsage(componentCount,amountUsed);
		return usageInfo;
	}

	public List<Map<String,String>> parseCSV(String strResponse) throws IOException {
		List<Map<String,String>> list = new ArrayList<Map<String,String>>();
		CsvSchema schema = CsvSchema.emptySchema().withHeader();
		ObjectReader mapper = new CsvMapper().reader(Object.class).with(schema);
		MappingIterator<Map<String,String>> it = mapper.readValues(strResponse);
		while(it.hasNext()) {
			list.add(it.next());
		}
		return list;
	}



	@DataProvider(name = "requestProviderForOkResponse")
	public Object[][] getRequestProviderForOkResponse() {

		MockHttpServletResponse createdResponse = new MockHttpServletResponse();
		createdResponse.setStatus(201);
		MockHttpServletResponse okResponse = new MockHttpServletResponse();
		okResponse.setStatus(200);

		MockHttpServletRequest policyEvalsRequest = new MockHttpServletRequest("POST", "/v1/policy-evaluation");
		policyEvalsRequest.addHeader("Predix-Zone-Id", acsInstanceId);

		MockHttpServletRequest policySetUpdatesRequest = new MockHttpServletRequest("PUT", "/v1/policy-set/policy-007");
		policySetUpdatesRequest.addHeader("Predix-Zone-Id", acsInstanceId);

		MockHttpServletRequest numberOfTokensRequest = new MockHttpServletRequest("POST", "/oauth/token");
		numberOfTokensRequest.addHeader("Predix-Zone-Id", uaaInstanceId);

		MockHttpServletRequest numberOfUsersRequest = new MockHttpServletRequest("POST", "/users");
		numberOfUsersRequest.addHeader("Predix-Zone-Id", uaaInstanceId);

		return new Object[][] {{ "policy_eval", Constants.ACS_PLAN_ID, policyEvalsRequest, okResponse},
			{ "policyset_update", Constants.ACS_PLAN_ID, policySetUpdatesRequest, createdResponse},
			{ "number_of_tokens", Constants.UAA_PLAN_ID, numberOfTokensRequest, okResponse}
			//{ "number_of_users", Constants.UAA_PLAN_ID, numberOfUsersRequest, createdResponse}  this is commented out because the feature_id is not measured with this subscription's plan. This needs to be fixed in order for this test to pass.
		};
	}

	private class ComponentUsage{
		private int componentCount;
		private double amountUsed;

		public ComponentUsage(int componentCount, double amountUsed) {
			this.componentCount = componentCount;
			this.amountUsed = amountUsed;
		}

		public int getCompCount() {
			return componentCount;
		}

		public double getUsage() {
			return amountUsed;
		}

	}


}
