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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.ge.predix.integration.cloudfoundry.CFClientTest;
import com.ge.predix.metering.util.Constants;
import com.nurego.model.Entitlement;
import com.nurego.model.Subscription;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.LoggingFilter;

import com.google.common.base.Splitter;


@ContextConfiguration("classpath:integration-test-spring-context.xml")
public class NuregoIT extends AbstractTestNGSpringContextTests {

    private static final String ACS_PLAN_ID = "pla_b70d-6248-4a0b-8018-9fc6b9de29e6";
    private static final String ACS_SUBSCRIPTION_ID = "sub_e854-2e8a-4f14-9d20-e45848e3c3ce";
    private static final String UAA_SUBSCRIPTION_ID = "sub_d5a8-ff81-4722-9251-836b5508ed54";
    private static final String UAA_PLAN_ID = "pla_8f0b-d679-463c-b3d6-977c10414aba";
    @Value("${NUREGO_USERNAME}")
    private String nuregoUsername;

    @Value("${NUREGO_PASSWORD}")
    private String nuregoPassword;

    @Value("${NUREGO_INSTANCE_ID}")
    private String nuregoInstanceId;
    
    @Value("${NUREGO_OAUTH_URL}")
    private String nuregoOauthURL;
    
    @Value("${NUREGO_API_TEST_URL}")
    private String nuregoURL;

    @Autowired
    private MeteringFilter meteringFilter;
    
    @Autowired
    private CFClientTest cfClientTest;
    
    private String serviceInstanceGuid;
    
    @Autowired
	@Qualifier("nuregoTemplate")
	RestTemplate nuregoTemplate;
    
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
    

    
    @Test(dataProvider = "requestProviderForOkResponse")
    public void testNuregoIntegrationForOkResponse(final String featureId, final String planId, final String subscriptionId,
            final  MockHttpServletRequest request, final ServletResponse response) throws Exception {
    	
      try {
     	String uaaUrl = cfClientTest.setUAAInstance("secret","uaa-pooja-10");
     	serviceInstanceGuid = cfClientTest.testCreateServiceInstance(uaaUrl); 
    		
	    	System.out.println("Created service Instance ::"+serviceInstanceGuid);
	    	
	    	String accessToken = getNuregoAuthToken();   	
	    	System.out.println("accessToken::"+accessToken);	
	    	String componentId= retrieveComponent(serviceInstanceGuid, accessToken);

	   	System.out.println("componentId::"+componentId);
	    	
	   	request.addHeader("Predix-Zone-Id", serviceInstanceGuid);
	    this.meteringFilter.doFilter(request, response, new MockFilterChain());
	    Thread.sleep(4000);
	    this.meteringFilter.doFilter(request, response, new MockFilterChain());
	    Thread.sleep(4000);

	    ComponentUsage usages = retrieveRawUsage(accessToken, componentId);
	    Assert.assertEquals(usages.getCompCount(), 2);
	    Assert.assertEquals(usages.getUsage(), 2.0);
	    
    	} catch(Exception e){
    		e.printStackTrace();
    	}finally {
    		cleanup();
    	}
	    
    }
    
//    @Test(dataProvider = "requestProviderForCreatedResponse")
//    public void testNuregoIntegrationForCreatedResponse(final String featureId, final String planId, final String subscriptionId,
//            final  MockHttpServletRequest request, final ServletResponse response) throws Exception {
//    		try {
//			serviceInstanceGuid = cfClientTest.testCreateServiceInstance();
//	
//		    	System.out.println("Created service Instance ::"+serviceInstanceGuid);
//		    	
//		    	String accessToken = getNuregoAuthToken();   	
//		    	System.out.println("accessToken::"+accessToken);	
//		    	String componentId= retrieveComponent(serviceInstanceGuid, accessToken);
//	
//		   	System.out.println("componentId::"+componentId);
//		    	
//		   	request.addHeader("Predix-Zone-Id", serviceInstanceGuid);
//		    this.meteringFilter.doFilter(request, response, new MockFilterChain());
//		    Thread.sleep(4000);
//		    this.meteringFilter.doFilter(request, response, new MockFilterChain());
//		    Thread.sleep(4000);
//	
//		    ComponentUsage usages = retrieveRawUsage(accessToken, componentId);
//		    Assert.assertEquals(usages.getCompCount(), 0);
//		    Assert.assertEquals(usages.getUsage(), 0.0);
//    		} finally {
//    			cleanup();
//    		}
//	    
//    }
    
    private void cleanup() {
        cfClientTest.deleteServiceInstance(serviceInstanceGuid);
        System.out.println("Successfully deleted " + serviceInstanceGuid);
 
    }
    private String getNuregoAuthToken() throws Exception{
    	
    		HashMap<String, Object> serviceRequest = new HashMap<String, Object>();
		serviceRequest.put(Constants.USERNAME, this.nuregoUsername);
		serviceRequest.put(Constants.PASSWORD, this.nuregoPassword);
		serviceRequest.put(Constants.INSTANCE_ID, this.nuregoInstanceId);

		URI oauthURL = URI.create(this.nuregoOauthURL);
		String response = this.nuregoTemplate.postForObject(oauthURL, serviceRequest, String.class);

		@SuppressWarnings("unchecked")
		Map<String, Object> responseMap = new ObjectMapper().readValue(response, Map.class);
		return responseMap.get(Constants.ACCESS_TOKEN).toString();
    }
    
	private String retrieveComponent(String serviceInstanceGuid, String accessToken) throws Exception{

		String retrieveComponentURL = this.nuregoURL + Constants.SERVICE_COMPONENT_URL+serviceInstanceGuid;
		
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
		headers.add(HttpHeaders.AUTHORIZATION, Constants.BEARER +accessToken);
		
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(retrieveComponentURL)
				.queryParam("provider", "cloud-foundry");
		
		HttpEntity<?> entity = new HttpEntity<>(headers);
		ResponseEntity<String> responseEntity;
		
	    while(true) {
	    		try {
	    			Thread.sleep(2000); // need to sleep because nurego mapping takes time to update
	    			responseEntity = this.nuregoTemplate.exchange(builder.build().encode().toUri(), HttpMethod.GET, entity ,String.class);
	    			break;
	    		} catch(Exception ex) {
	    			System.out.println("Attempting to retrieve component ID...");
	    		}
	    }
	    String responseBody = responseEntity.getBody();
	    
	    @SuppressWarnings("unchecked")
		Map<String,Object> map = new ObjectMapper().readValue(responseBody, Map.class);	    
	    	return map.get(Constants.ID).toString();

	}
	

	private Date tomorrow() {
		final Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, +1);
		return cal.getTime();
	}
	
	private Date today() {
		final Calendar cal = Calendar.getInstance();
		return cal.getTime();
	}
	private ComponentUsage retrieveRawUsage(String accessToken, String cmp_id) throws JsonParseException, JsonMappingException, IOException {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		String retrieveRawUsage = Constants.NUREGO_USAGE_URL;
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ACCEPT, MediaType.ALL_VALUE);
		headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		headers.add(HttpHeaders.AUTHORIZATION, Constants.BEARER +accessToken);
		
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(retrieveRawUsage)
				.queryParam("start_date", dateFormat.format(today()))
				.queryParam("end_date",dateFormat.format(tomorrow()))
				.queryParam("organization_id", Constants.ORGANIZATION_ID)
				.queryParam("service_id", Constants.SERVICE_ID);
		
		HttpEntity<?> entity = new HttpEntity<>(headers);
		System.out.println("url:"+builder.build().encode().toUri());
		
	    ResponseEntity<String> responseEntity = this.nuregoTemplate.exchange(builder.build().encode().toUri(), HttpMethod.GET, entity ,String.class);  
	    String responseBody = responseEntity.getBody();
	    System.out.println(responseBody);
	    int componentCount = 0;
	    double amountUsed = 0.0;
	    List<Map<String,String>> list = parseCSV(responseBody);
	    for(Map<?,?>map : list) {
	    		if(map.get("Service component id").equals(cmp_id)) {
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
     //   policyEvalsRequest.addHeader("Predix-Zone-Id", serviceInstanceGuid);

        MockHttpServletRequest policySetUpdatesRequest = new MockHttpServletRequest("PUT", "/v1/policy-set/policy-007");
        policySetUpdatesRequest.addHeader("Predix-Zone-Id", ACS_SUBSCRIPTION_ID);

        MockHttpServletRequest numberOfTokensRequest = new MockHttpServletRequest("POST", "/oauth/token");
     //   numberOfTokensRequest.addHeader("Predix-Zone-Id", UAA_SUBSCRIPTION_ID);

        MockHttpServletRequest numberOfUsersRequest = new MockHttpServletRequest("POST", "/users");
        numberOfUsersRequest.addHeader("Predix-Zone-Id", UAA_SUBSCRIPTION_ID);

        return new Object[][] {//{ "policy_eval", ACS_PLAN_ID, ACS_SUBSCRIPTION_ID, policyEvalsRequest, okResponse },
                //{ "policyset_update", ACS_PLAN_ID, ACS_SUBSCRIPTION_ID, policySetUpdatesRequest, createdResponse },
                { "number_of_tokens", UAA_PLAN_ID, UAA_SUBSCRIPTION_ID, numberOfTokensRequest, okResponse },
               // { "number_of_users", UAA_PLAN_ID, UAA_SUBSCRIPTION_ID, numberOfUsersRequest, createdResponse } 
                };
    }
    
//    @DataProvider(name = "requestProviderForCreatedResponse")
//    public Object[][] getRequestProviderForCreatedResponse() {
//
//        MockHttpServletResponse createdResponse = new MockHttpServletResponse();
//        createdResponse.setStatus(201);
//        MockHttpServletResponse okResponse = new MockHttpServletResponse();
//        okResponse.setStatus(200);
//
//        MockHttpServletRequest policyEvalsRequest = new MockHttpServletRequest("POST", "/v1/policy-evaluation");
//     //   policyEvalsRequest.addHeader("Predix-Zone-Id", serviceInstanceGuid);
//
//        MockHttpServletRequest policySetUpdatesRequest = new MockHttpServletRequest("PUT", "/v1/policy-set/policy-007");
//        policySetUpdatesRequest.addHeader("Predix-Zone-Id", ACS_SUBSCRIPTION_ID);
//
//        MockHttpServletRequest numberOfTokensRequest = new MockHttpServletRequest("POST", "/oauth/token");
//        numberOfTokensRequest.addHeader("Predix-Zone-Id", UAA_SUBSCRIPTION_ID);
//
//        MockHttpServletRequest numberOfUsersRequest = new MockHttpServletRequest("POST", "/users");
//        numberOfUsersRequest.addHeader("Predix-Zone-Id", UAA_SUBSCRIPTION_ID);
//
//        return new Object[][] {//{ "policy_eval", ACS_PLAN_ID, ACS_SUBSCRIPTION_ID, policyEvalsRequest, okResponse },
//                { "policyset_update", ACS_PLAN_ID, ACS_SUBSCRIPTION_ID, policySetUpdatesRequest, createdResponse },
//               // { "number_of_tokens", UAA_PLAN_ID, UAA_SUBSCRIPTION_ID, numberOfTokensRequest, okResponse },
//                { "number_of_users", UAA_PLAN_ID, UAA_SUBSCRIPTION_ID, numberOfUsersRequest, createdResponse } 
//                };
//    }
}
