package com.ge.predix.metering.nurego;

import com.ge.predix.metering.customer.Customer;
import com.ge.predix.metering.data.entity.MeteredResource;

public interface NuregoClient {

    void updateAmount(Customer customer, MeteredResource meter, int amount);

    void logOpenCircuit();
}
