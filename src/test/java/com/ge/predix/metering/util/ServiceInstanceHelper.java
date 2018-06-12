package com.ge.predix.metering.util;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.ResponseEntity;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;


@ContextConfiguration("classpath:integration-test-spring-context.xml")
public class ServiceInstanceHelper extends AbstractTestNGSpringContextTests {

	@Value("${CF_SPACE_GUID}")
	private String cfSpaceGuid;

	@Value("${UAA_SERVICE_PLAN_GUID}")
	private String uaaServicePlanGuid;

	@Value("${ACS_SERVICE_PLAN_GUID}")
	private String acsServicePlanGuid;

	@Autowired
	@Qualifier("cfTemplate")
	OAuth2RestTemplate cfRestTemplate;

	@Value("${CLOUD_CONTROLLER_URI}")
	private String cfControllerURL;

	@Value("${CF_USERNAME}")
	private String cfUser;

	@Value("${CF_PASSWORD}")
	private String cfPassword;

	@Value("${CF_ORG}")
	private String cfOrg;

	@Value("${CF_SPACE}")
	private String cfSpace;

	@Value("${UAA_BASE_DOMAIN}")
	private String uaaBaseDomain;

	private final Map<String, String> headers = new HashMap<String, String>();

	@BeforeClass
	private void setup() {
		this.headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		this.headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
	}


	public String createAcsInstance(final String trustedIssuerId)
			throws Exception {

		String serviceInstanceName = Constants.ACS_SERVICE_INSTANCE_NAME;
		HashMap<String, Object> serviceRequest = new HashMap<String, Object>();
		serviceRequest.put("space_guid", this.cfSpaceGuid);
		serviceRequest.put("name", serviceInstanceName);
		serviceRequest.put("service_plan_guid", this.acsServicePlanGuid);
		Map<String, List<String>> issuerMap = new HashMap<String, List<String>>();
		issuerMap.put("trustedIssuerIds", Arrays.asList(trustedIssuerId));
		serviceRequest.put("parameters", issuerMap);

		URI createInstanceURI = URI.create(this.cfControllerURL + Constants.CREATE_SERVICE_INSTANCE_URL+ Constants.ACCEPTS_INCOMPLETE);
		String response = this.cfRestTemplate.postForObject(createInstanceURI, serviceRequest, String.class);

		@SuppressWarnings("unchecked")
		Map<String, Object> responseMap = new ObjectMapper().readValue(response, Map.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> metadataMap = (Map<String, Object>) responseMap.get(Constants.METADATA);
		String acsGuid = (String) metadataMap.get(Constants.GUID);
		try {
			verifyServiceInstanceCreated(acsGuid);
		} catch (Exception e) {
			Assert.fail(e.toString());
		} 
		return acsGuid;
	}

	public UaaInstance createUaaInstance() throws JsonParseException, JsonMappingException, IOException {
		String serviceInstanceName = Constants.UAA_SERVICE_INSTANCE_NAME;
		String secret = Constants.SECRET;
		HashMap<String, Object> serviceRequest = new HashMap<String, Object>();
		serviceRequest.put("space_guid", this.cfSpaceGuid);
		serviceRequest.put("name", serviceInstanceName);
		serviceRequest.put("service_plan_guid", this.uaaServicePlanGuid);

		Map<String, String> issuerMap = new HashMap<String, String>();
		issuerMap.put("adminClientSecret", secret);
		serviceRequest.put(Constants.PARAMETERS, issuerMap);

		URI createInstanceURI = URI.create(this.cfControllerURL + Constants.CREATE_SERVICE_INSTANCE_URL + Constants.ACCEPTS_INCOMPLETE);
		String response = "";
		try {
			 response = this.cfRestTemplate.postForObject(createInstanceURI, serviceRequest, String.class);
		} catch(Exception e) {
			e.printStackTrace();
		}

		@SuppressWarnings("unchecked")
		Map<String, Object> responseMap = new ObjectMapper().readValue(response, Map.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> metadataMap = (Map<String, Object>) responseMap.get(Constants.METADATA);
		String uaaGuid = (String)metadataMap.get(Constants.GUID);
		StringBuilder uaaUrl = new StringBuilder().append("https://").append(uaaGuid).append(".").append(uaaBaseDomain).append(Constants.UAA_OAUTH_TOKEN);
		UaaInstance uaa = new UaaInstance(uaaUrl.toString(),uaaGuid);
		try {
			verifyServiceInstanceCreated(uaaGuid);
		} catch (Exception e) {
			Assert.fail(e.toString());
		} 
		return uaa;

	}

	private void verifyServiceInstanceCreated(final String serviceInstanceGuid) {
		URI getServiceInstance = URI.create(this.cfControllerURL + "/v2/service_instances/" + serviceInstanceGuid);
		ResponseEntity<String> response = this.cfRestTemplate.getForEntity(getServiceInstance, String.class);
		Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
	}

	public void deleteServiceInstance(final String serviceInstanceGuid) {
		URI createInstanceURI = URI.create(this.cfControllerURL + "/v2/service_instances/" + serviceInstanceGuid);
		this.cfRestTemplate.delete(createInstanceURI);
	}

}
