package com.ge.predix.metering.filter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.nurego.exception.InvalidRequestException;
import com.nurego.model.Entitlement;
import com.nurego.model.Subscription;

@ContextConfiguration("classpath:integration-test-spring-context.xml")
public class NuregoIT extends AbstractTestNGSpringContextTests {

    private static final String ACS_PLAN_ID = "pla_1ba8-5fe8-474f-8211-163649417d8e";
    private static final String ACS_SUBSCRIPTION_ID = "sub_1ed3-c08d-4c92-8c9b-710862ba893b";
    private static final String ORGANIZATION_ID = "ff85feb9-be02-4a73-9b13-9e1970abf09c";
    private static final String UAA_PLAN_ID = "pla_b77c-e9fd-434d-afad-c80e45f712fd";
    private static final String UAA_SUBSCRIPTION_ID = "sub_b0b8-7fdc-4be8-8109-58bd89dce477";

    @Autowired
    private MeteringFilter meteringFilter;

    @Test(dataProvider = "requestProvider")
    public void testNuregoIntegration(final String featureId, final String planId, final String subscriptionId,
            final ServletRequest request, final ServletResponse response) throws Exception {

        Subscription subscription;
        try {
            // see if subscription is already created
            subscription = Subscription.retrieve(subscriptionId);
        } catch (InvalidRequestException ex) {
            // if subscription is not created, then create one and copy the subscription ids to the appropriate
            // subscription id fields above.
            Map<String, Object> params = new HashMap<>();
            params.put("plan_id", planId);
            subscription = Subscription.create(ORGANIZATION_ID, params);
        }
        System.out.println("Subscription Id: " + subscription.getId());
        Assert.assertEquals(subscription.getId(), subscriptionId);
        Double beforeUsedAmount = getEntitlementUsageByFeatureId(featureId, subscriptionId);

        this.meteringFilter.doFilter(request, response, new MockFilterChain());
        Thread.sleep(2100);
        this.meteringFilter.doFilter(request, response, new MockFilterChain());

        Double afterUsedAmount = getEntitlementUsageByFeatureId(featureId, subscriptionId);
        Assert.assertEquals(afterUsedAmount - beforeUsedAmount, 2.0);
    }

    private Double getEntitlementUsageByFeatureId(final String featureId, final String subscriptionId)
            throws Exception {
        Entitlement entitlement = getEntitlementByFeatureId(featureId, subscriptionId);
        if (entitlement == null) {
            throw new IllegalArgumentException(String.format("Feature '%s' does not exist.", featureId));
        }
        return entitlement.getCurrentUsedAmount();
    }

    private Entitlement getEntitlementByFeatureId(final String featureId, final String subscriptionId)
            throws Exception {
        List<Entitlement> entitlements = Entitlement.retrieve(subscriptionId).getData();
        for (Entitlement entitlement : entitlements) {
            if (entitlement.getFeatureId().equals(featureId)) {
                return entitlement;
            }
        }

        return null;
    }

    @DataProvider(name = "requestProvider")
    public Object[][] getRequestProvider() {

        MockHttpServletResponse createdResponse = new MockHttpServletResponse();
        createdResponse.setStatus(201);
        MockHttpServletResponse okResponse = new MockHttpServletResponse();
        okResponse.setStatus(200);

        MockHttpServletRequest policyEvalsRequest = new MockHttpServletRequest("POST", "/v1/policy-evaluation");
        policyEvalsRequest.addHeader("Predix-Zone-Id", ACS_SUBSCRIPTION_ID);

        MockHttpServletRequest policySetUpdatesRequest = new MockHttpServletRequest("PUT", "/v1/policy-set/policy-007");
        policySetUpdatesRequest.addHeader("Predix-Zone-Id", ACS_SUBSCRIPTION_ID);

        MockHttpServletRequest numberOfTokensRequest = new MockHttpServletRequest("POST", "/oauth/token");
        numberOfTokensRequest.addHeader("Predix-Zone-Id", UAA_SUBSCRIPTION_ID);

        MockHttpServletRequest numberOfUsersRequest = new MockHttpServletRequest("POST", "/users");
        numberOfUsersRequest.addHeader("Predix-Zone-Id", UAA_SUBSCRIPTION_ID);

        Object[][] data = new Object[][] {
                { "policy_eval", ACS_PLAN_ID, ACS_SUBSCRIPTION_ID, policyEvalsRequest, okResponse },
                { "policyset_update", ACS_PLAN_ID, ACS_SUBSCRIPTION_ID, policySetUpdatesRequest, createdResponse },
                { "number_of_tokens", UAA_PLAN_ID, UAA_SUBSCRIPTION_ID, numberOfTokensRequest, okResponse },
                { "number_of_users", UAA_PLAN_ID, UAA_SUBSCRIPTION_ID, numberOfUsersRequest, createdResponse } };

        return data;
    }
}
