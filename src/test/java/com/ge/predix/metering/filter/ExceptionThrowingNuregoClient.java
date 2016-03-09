package com.ge.predix.metering.filter;

import org.springframework.stereotype.Component;

import com.ge.predix.metering.customer.Customer;
import com.ge.predix.metering.data.entity.MeteredResource;
import com.ge.predix.metering.nurego.NuregoClient;

@Component
public class ExceptionThrowingNuregoClient implements NuregoClient {

    private boolean isCircuitOpen = false;

    @Override
    public void updateAmount(final Customer customer, final MeteredResource meter, final int amount) {
        throw new IllegalStateException("Failure.");
    }

    @Override
    public void logOpenCircuit() {
        this.isCircuitOpen = true;
    }

    public boolean isCircuitOpen() {
        return this.isCircuitOpen;
    }
}
