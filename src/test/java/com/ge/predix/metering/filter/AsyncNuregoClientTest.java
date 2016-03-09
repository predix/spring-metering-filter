package com.ge.predix.metering.filter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;
import org.springframework.http.HttpEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ge.predix.metering.customer.Customer;
import com.ge.predix.metering.data.entity.MeteredResource;
import com.ge.predix.metering.nurego.AsyncNuregoClient;
import com.ge.predix.metering.nurego.CustomerMeteredResource;

@Test(groups = { "asyncNuregoSingleThreadedTest" })
public class AsyncNuregoClientTest {

    private static final String SUBSCRIPTION_1 = "subscription_1";
    private static final String SUBSCRIPTION_2 = "subscription_2";
    private static final String SUBSCRIPTION_3 = "subscription_3";

    @Mock
    private AsyncRestTemplate asyncRestTemplate;

    @SuppressWarnings("unchecked")
    @BeforeClass
    public void setup() {
        @SuppressWarnings("rawtypes")
        ListenableFuture value = mock(ListenableFuture.class);
        MockitoAnnotations.initMocks(this);
        when(this.asyncRestTemplate.postForEntity(Matchers.anyString(), Matchers.any(HttpEntity.class),
                Matchers.any(Class.class))).thenReturn(value);
    }

    @Test
    public void testExceedMaxMapSize() {

        AsyncNuregoClient nuregoClient = new AsyncNuregoClient("https://mockNuregoUrl.com", "", 3, 3);
        nuregoClient.setAsyncRestTemplate(this.asyncRestTemplate);

        MeteredResource meter = new MeteredResource("POST", "/users", 201, "5813");
        nuregoClient.updateAmount(new Customer(null, SUBSCRIPTION_1), meter, 1);
        nuregoClient.updateAmount(new Customer(null, SUBSCRIPTION_2), meter, 1);
        @SuppressWarnings("unchecked")
        Map<CustomerMeteredResource, Integer> internalState = (Map<CustomerMeteredResource, Integer>) Whitebox.getInternalState(nuregoClient,
                "updateMap");
        Assert.assertEquals(internalState.size(), 2);

        nuregoClient.updateAmount(new Customer(null, SUBSCRIPTION_3), meter, 1);
        @SuppressWarnings("unchecked")
        Map<CustomerMeteredResource, Integer> internalState2 = (Map<CustomerMeteredResource, Integer>) Whitebox.getInternalState(nuregoClient,
                "updateMap");
        Assert.assertEquals(internalState2.size(), 0);
    }

    @Test
    public void testExceedBatchIntervalSeconds() throws InterruptedException {

        AsyncNuregoClient nuregoClient = new AsyncNuregoClient("https://mockNuregoUrl.com", "", 3, 3);
        nuregoClient.setAsyncRestTemplate(this.asyncRestTemplate);

        MeteredResource meter = new MeteredResource("POST", "/users", 201, "5813");
        Customer customer = new Customer(null, SUBSCRIPTION_1);
        nuregoClient.updateAmount(customer, meter, 1);
        @SuppressWarnings("unchecked")
        Map<CustomerMeteredResource, Integer> internalState = (Map<CustomerMeteredResource, Integer>) Whitebox.getInternalState(nuregoClient,
                "updateMap");
        Assert.assertEquals(internalState.size(), 1);
        Thread.sleep(3000);

        nuregoClient.updateAmount(customer, meter, 1);
        @SuppressWarnings("unchecked")
        Map<CustomerMeteredResource, Integer> internalState2 = (Map<CustomerMeteredResource, Integer>) Whitebox.getInternalState(nuregoClient,
                "updateMap");
        Assert.assertEquals(internalState2.size(), 0);
    }

}
