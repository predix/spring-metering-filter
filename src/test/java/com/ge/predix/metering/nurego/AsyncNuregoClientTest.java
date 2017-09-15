/*******************************************************************************
 * Copyright 2017 General Electric Company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.ge.predix.metering.nurego;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ge.predix.metering.customer.Customer;
import com.ge.predix.metering.data.entity.MeteredResource;

@Test(groups = { "asyncNuregoSingleThreadedTest" })
public class AsyncNuregoClientTest {

    private static final String SUBSCRIPTION_1 = "subscription_1";
    private static final String SUBSCRIPTION_2 = "subscription_2";
    private static final String SUBSCRIPTION_3 = "subscription_3";
    private static final String NUREGO_USERNAME = "nuregoUsername";
    private static final String NUREGO_PASSWORD = "nuregoPassword";
    private static final String NUREGO_INSTANCE_ID = "nuregoInstanceId";
    private static final int EXPIRY = 5;

    @Mock
    private AsyncRestTemplate asyncRestTemplate;

    @Mock
    private RestTemplate restTemplate;

    @SuppressWarnings("unchecked")
    @BeforeClass
    public void setup() {
        @SuppressWarnings("rawtypes")
        ListenableFuture value = mock(ListenableFuture.class);
        MockitoAnnotations.initMocks(this);
        when(this.asyncRestTemplate.postForEntity(Matchers.anyString(), Matchers.any(HttpEntity.class),
                Matchers.any(Class.class))).thenReturn(value);
        when(this.restTemplate.postForEntity(Matchers.anyString(), Matchers.any(HttpEntity.class),
                Matchers.any(Class.class)))
                        .thenReturn(new ResponseEntity<>(new NuregoTokenResponse("1234", EXPIRY), HttpStatus.OK));
    }

    @Test
    public void testExceedMaxMapSize() {

        AsyncNuregoClient nuregoClient = new AsyncNuregoClient("https://mockNuregoUrl.com", 3, 3, NUREGO_USERNAME,
                NUREGO_PASSWORD, NUREGO_INSTANCE_ID);
        nuregoClient.setAsyncRestTemplate(this.asyncRestTemplate);
        nuregoClient.setRestTemplate(this.restTemplate);

        MeteredResource meter = new MeteredResource("POST", "/users", 201, "5813");
        nuregoClient.updateAmount(new Customer(null, SUBSCRIPTION_1), meter, 1);
        nuregoClient.updateAmount(new Customer(null, SUBSCRIPTION_2), meter, 1);
        @SuppressWarnings("unchecked")
        Map<CustomerMeteredResource, Integer> internalState = (Map<CustomerMeteredResource, Integer>) Whitebox
                .getInternalState(nuregoClient, "updateMap");
        Assert.assertEquals(internalState.size(), 2);

        nuregoClient.updateAmount(new Customer(null, SUBSCRIPTION_3), meter, 1);
        @SuppressWarnings("unchecked")
        Map<CustomerMeteredResource, Integer> internalState2 = (Map<CustomerMeteredResource, Integer>) Whitebox
                .getInternalState(nuregoClient, "updateMap");
        Assert.assertEquals(internalState2.size(), 0);
    }

    @Test
    public void testExceedBatchIntervalSeconds() throws InterruptedException {

        AsyncNuregoClient nuregoClient = new AsyncNuregoClient("https://mockNuregoUrl.com", 3, 3, NUREGO_USERNAME,
                NUREGO_PASSWORD, NUREGO_INSTANCE_ID);
        nuregoClient.setAsyncRestTemplate(this.asyncRestTemplate);
        nuregoClient.setRestTemplate(this.restTemplate);

        MeteredResource meter = new MeteredResource("POST", "/users", 201, "5813");
        Customer customer = new Customer(null, SUBSCRIPTION_1);
        nuregoClient.updateAmount(customer, meter, 1);
        @SuppressWarnings("unchecked")
        Map<CustomerMeteredResource, Integer> internalState = (Map<CustomerMeteredResource, Integer>) Whitebox
                .getInternalState(nuregoClient, "updateMap");
        Assert.assertEquals(internalState.size(), 1);
        Thread.sleep(3000);

        nuregoClient.updateAmount(customer, meter, 1);
        @SuppressWarnings("unchecked")
        Map<CustomerMeteredResource, Integer> internalState2 = (Map<CustomerMeteredResource, Integer>) Whitebox
                .getInternalState(nuregoClient, "updateMap");
        Assert.assertEquals(internalState2.size(), 0);
    }

    @Test
    public void testGetNuregoToken() throws Exception {
        setup();
        AsyncNuregoClient nuregoClient = new AsyncNuregoClient("https://mockNuregoUrl.com", 3, 3, NUREGO_USERNAME,
                NUREGO_PASSWORD, NUREGO_INSTANCE_ID);
        nuregoClient.setRestTemplate(this.restTemplate);
        Assert.assertNotNull(nuregoClient.getNuregoToken());
        Mockito.verify(this.restTemplate, times(1)).postForEntity(Matchers.anyString(), Matchers.any(HttpEntity.class),
                Matchers.any(Class.class));
        // verify we used the cached value
        Assert.assertNotNull(nuregoClient.getNuregoToken());
        Mockito.verify(this.restTemplate, times(1)).postForEntity(Matchers.anyString(), Matchers.any(HttpEntity.class),
                Matchers.any(Class.class));
        Thread.sleep(EXPIRY * 1000);
        // verify once the token expires, we get a new token from nurego
        Assert.assertNotNull(nuregoClient.getNuregoToken());
        Mockito.verify(this.restTemplate, times(2)).postForEntity(Matchers.anyString(), Matchers.any(HttpEntity.class),
                Matchers.any(Class.class));

    }

}
