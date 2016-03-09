package com.ge.predix.metering.filter;

import java.util.HashMap;
import java.util.Map;

import com.ge.predix.metering.customer.Customer;
import com.ge.predix.metering.data.entity.MeteredResource;
import com.ge.predix.metering.nurego.NuregoClient;

public class MockNuregoClient implements NuregoClient {

    private final Map<String, Integer> meterMap = new HashMap<>();

    @Override
    public void updateAmount(final Customer customer, final MeteredResource meter, final int amount) {
        String key = customer.getSubscriptionId() + meter.getFeatureId();
        this.meterMap.put(key, amount);
    }

    public Integer getMeterAmount(final Customer customer, final MeteredResource meter) {
        String key = customer.getSubscriptionId() + meter.getFeatureId();
        return this.meterMap.get(key);
    }

    @Override
    public void logOpenCircuit() {
        // Do nothing!
    }
}
