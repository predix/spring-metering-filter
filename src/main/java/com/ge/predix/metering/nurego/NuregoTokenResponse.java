
package com.ge.predix.metering.nurego;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NuregoTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("expires_in")
    private int expiry;

    public NuregoTokenResponse() {
        // needs to be here for Jacskon
    }

    public NuregoTokenResponse(final String accessToken, final int expiry) {
        this.accessToken = accessToken;
        this.expiry = expiry;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public void setAccessToken(final String accessToken) {
        this.accessToken = accessToken;
    }

    public int getExpiry() {
        return this.expiry;
    }

    public void setExpiry(final int expiry) {
        this.expiry = expiry;
    }

}