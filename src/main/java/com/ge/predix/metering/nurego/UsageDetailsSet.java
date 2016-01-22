package com.ge.predix.metering.nurego;

import java.util.ArrayList;
import java.util.Collection;
import com.ge.predix.metering.data.entity.JsonUtils;

public class UsageDetailsSet {
    private Collection<UsageDetails> data = new ArrayList<UsageDetails>();
    private String object = "list";
    private Integer count;

    private final JsonUtils jsonUtils = new JsonUtils();

    public Collection<UsageDetails> getData() {
        return data;
    }
    
    public void setData(Collection<UsageDetails> data) {
        this.data = data;
        this.count = (data == null? 0 : new Integer(data.size()));
    }
    
    public String getObject() {
        return object;
    }
    
    public void setObject(String object) {
        this.object = object;
    }
    
    public Integer getCount() {
        return count;
    }
    
    public void setCount(Integer count) {
        this.count = count;
    }

    public UsageDetailsSet(Collection<UsageDetails> data) {
        super();
        this.data = data;
        this.count = (data == null? 0 : new Integer(data.size()));
    }
    
    // Default constructor is required for deserialization
    public UsageDetailsSet() {
        super();
        this.data = null;
        this.count = 0;
    }

    public String toJSON() {
        return this.jsonUtils.serialize(this);
    }
    
    public UsageDetailsSet fromJSON(String json) {
        UsageDetailsSet usageDetailsSet = this.jsonUtils.deserialize(json, UsageDetailsSet.class);
        return usageDetailsSet;
    }
}
