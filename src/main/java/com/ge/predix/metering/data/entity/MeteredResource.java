package com.ge.predix.metering.data.entity;

import org.springframework.web.util.UriTemplate;

public class MeteredResource {

    // For Nurego
    private String featureId;
    // To match requests to features
    private String httpMethod;
    private String uriTemplate;
    private UriTemplate internalUriTemplate;
    private int expectedHttpStatusCode;

    public MeteredResource() {
        // Default constructor.
    }

    public MeteredResource(final String httpMethod, final String uriTemplate, final int expectedHttpStatusCode,
            final String featureId) {

        this.httpMethod = httpMethod;
        this.uriTemplate = uriTemplate;
        this.internalUriTemplate = new UriTemplate(this.uriTemplate);
        this.expectedHttpStatusCode = expectedHttpStatusCode;
        this.featureId = featureId;
    }

    public String getFeatureId() {
        return this.featureId;
    }

    public void setFeatureId(final String featureId) {
        this.featureId = featureId;
    }

    public String getHttpMethod() {
        return this.httpMethod;
    }

    public void setHttpMethod(final String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getUriTemplate() {
        return this.uriTemplate;
    }

    public void setUriTemplate(final String uriTemplate) {
        this.uriTemplate = uriTemplate;
        this.internalUriTemplate = new UriTemplate(uriTemplate);
    }

    public boolean isUriTemplateMatch(final String uri) {
        return this.internalUriTemplate.matches(uri.toLowerCase());
    }

    public int getExpectedHttpStatusCode() {
        return this.expectedHttpStatusCode;
    }

    public void setExpectedHttpStatusCode(final int expectedHttpStatusCode) {
        this.expectedHttpStatusCode = expectedHttpStatusCode;
    }

    @Override
    public String toString() {
        return "MeteredResource [featureId=" + this.featureId + ", httpMethod=" + this.httpMethod + ", uriTemplate="
                + this.uriTemplate + ", expectedHttpStatusCode=" + this.expectedHttpStatusCode + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.featureId == null) ? 0 : this.featureId.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof MeteredResource)) {
            return false;
        }
        MeteredResource other = (MeteredResource) obj;
        if (this.featureId == null) {
            if (other.featureId != null) {
                return false;
            }
        } else if (!this.featureId.equals(other.featureId)) {
            return false;
        }
        return true;
    }
}
