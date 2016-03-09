package com.ge.predix.metering.customer;

import javax.servlet.http.HttpServletRequest;

/**
 * This class translates a request to the customer involved.
 */
public interface CustomerResolver {

    Customer resolveCustomer(HttpServletRequest request);
}
