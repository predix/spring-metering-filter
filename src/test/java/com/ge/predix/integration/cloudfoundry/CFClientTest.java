package com.ge.predix.integration.cloudfoundry;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@Test
@ContextConfiguration("classpath:integration-test-spring-context.xml")
public class CFClientTest extends AbstractTestNGSpringContextTests {
	
	@Value("${CF_SPACE_GUID}")
	private String cfSpaceGuid;

	@Value("${SERVICE_PLAN_GUID}")
	private String servicePlanGuid;

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
	
	private final Map<String, String> headers = new HashMap<String, String>();
		
	@BeforeClass
	private void setup() {
		this.headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
		this.headers.put(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
	}
	
	public String testCreateServiceInstance() throws Exception {

		String serviceInstanceName = "dcs-metering-test";
		String trustedIssuerId = "https://uaadummyurl.com/oauth/token";

		String serviceInstanceGuid = createNewServiceInstance(serviceInstanceName, trustedIssuerId);
		try {
			verifyServiceInstanceCreated(serviceInstanceGuid);
			
			System.out.println("Inside testCreateServiceInstance ********");			
		} catch (Exception e) {
			Assert.fail(e.toString());
		} 
		return serviceInstanceGuid;
	}
	
	private String createNewServiceInstance(final String serviceInstanceName, final String trustedIssuerId)
			throws Exception {

		HashMap<String, Object> serviceRequest = new HashMap<String, Object>();
		serviceRequest.put("space_guid", this.cfSpaceGuid);
		serviceRequest.put("name", serviceInstanceName);
		serviceRequest.put("service_plan_guid", this.servicePlanGuid);
		Map<String, List<String>> issuerMap = new HashMap<String, List<String>>();
		issuerMap.put("trustedIssuerIds", Arrays.asList(trustedIssuerId));
		serviceRequest.put("parameters", issuerMap);

		URI createInstanceURI = URI.create(this.cfControllerURL + "/v2/service_instances");
		String response = this.cfRestTemplate.postForObject(createInstanceURI, serviceRequest, String.class);

		@SuppressWarnings("unchecked")
		Map<String, Object> responseMap = new ObjectMapper().readValue(response, Map.class);
		@SuppressWarnings("unchecked")
		Map<String, Object> metadataMap = (Map<String, Object>) responseMap.get("metadata");
		return (String) metadataMap.get("guid");
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
