package com.ge.predix.metering.nurego;

public class UsageDetails {
    private String provider = "cloud-foundry";
    private String subscription_id;
    private String feature_id;
    private Integer amount;
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public String getSubscription_id() {
        return subscription_id;
    }
    
    public void setSubscription_id(String subscription_id) {
        this.subscription_id = subscription_id;
    }
    
    public String getFeature_id() {
        return feature_id;
    }
    
    public void setFeature_id(String feature_id) {
        this.feature_id = feature_id;
    }
    
    public Integer getAmount() {
        return amount;
    }
    
    public void setAmount(Integer amount) {
        this.amount = amount;
    }
    

    public UsageDetails(String subscription_id, String feature_id, Integer amount, String id) {
        super();
        this.subscription_id = subscription_id;
        this.feature_id = feature_id;
        this.amount = amount;
        this.id = id;
    }

    // Default constructor is required for deserialization
    public UsageDetails() {
        super();
        this.subscription_id = null;
        this.feature_id = null;
        this.amount = 0;
        this.id = null;
    }
}
