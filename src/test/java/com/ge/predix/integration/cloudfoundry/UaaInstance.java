package com.ge.predix.integration.cloudfoundry;

public class UaaInstance {
	private String uaaUrl;
	private String uaaGuid;
	
	public UaaInstance() {};
	
	public UaaInstance(String uaaUrl, String uaaGuid) {
		this.uaaUrl = uaaUrl;
		this.uaaGuid = uaaGuid;
	}

	
	public String getUaaUrl(){
		return uaaUrl;
	}
	
	public String getUaaGuid() {
		return uaaGuid;
	}
}
