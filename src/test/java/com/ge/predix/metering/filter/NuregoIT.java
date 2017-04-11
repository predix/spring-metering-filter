/*******************************************************************************
 * Copyright 2016 General Electric Company.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
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

    private static final String ACS_PLAN_ID = "pla_b70d-6248-4a0b-8018-9fc6b9de29e6";
    private static final String ACS_SUBSCRIPTION_ID = "sub_e854-2e8a-4f14-9d20-e45848e3c3ce";
    private static final String ORGANIZATION_ID = "050e3f85-4706-4d88-8e87-7488cc84089c";
    private static final String UAA_PLAN_ID = "pla_8f0b-d679-463c-b3d6-977c10414aba";
    private static final String UAA_SUBSCRIPTION_ID = "sub_d5a8-ff81-4722-9251-836b5508ed54";

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
        this.meteringFilter.doFilter(request, response, new MockFilterChain());
        //2-3 second delay doesnt seem to be enough for the nurego server to reflect the udpated count.
        Thread.sleep(5000);

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
