package com.ge.predix.metering.util;

public class UaaInstance {
	private String uaaUrl;
	private String uaaInstanceId;

	public UaaInstance() {
	};

	public UaaInstance(String uaaUrl, String uaaInstanceId) {
		this.uaaUrl = uaaUrl;
		this.uaaInstanceId = uaaInstanceId;
	}

	public String getUaaUrl() {
		return uaaUrl;
	}

	public String getUaaGuid() {
		return uaaInstanceId;
	}
}
