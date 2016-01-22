package com.ge.predix.metering.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.ge.predix.metering.customer.Customer;
import com.ge.predix.metering.data.entity.MeteredResource;
import com.ge.predix.metering.nurego.AsyncNuregoClient;
import com.ge.predix.metering.nurego.UsageDetails;
import com.ge.predix.metering.nurego.UsageDetailsSet;

@Test
public class AsyncNuregoClientUsageByBatchMultiThreadTest {
    private final static String FEATURE_1 = "f1";
    private final static String FEATURE_2 = "f2";
    private static final String SUBSCRIPTION_1 = "subscription_123";
    private static final String SUBSCRIPTION_2 = "subscription_456";

    private AsyncNuregoClient asyncNuregoClient = null;
    final List<String> payloads = Collections.synchronizedList(new ArrayList<String>());
    
    @SuppressWarnings("unchecked")
    @BeforeClass
    public void setup() {
        AsyncRestTemplate asyncRestTemplate = Mockito.mock(AsyncRestTemplate.class);
        Mockito.when(asyncRestTemplate.postForEntity(Matchers.anyString(), Matchers.any(HttpEntity.class),
                Matchers.any(Class.class))).thenAnswer(new Answer<ListenableFuture<?>>() {
                    @Override
                    public ListenableFuture<?> answer(final InvocationOnMock invocation) throws Throwable {
                        Object[] args = invocation.getArguments();
                        String url = (String) args[0];
                        HttpEntity<?> request = (HttpEntity<?>) args[1];
                        String body = request.getBody().toString();
                        System.out.println("Thread id: " + Thread.currentThread().getId() + " url: " + url
                                + " payload: " + body);
                        
                        AsyncNuregoClientUsageByBatchMultiThreadTest.this.payloads.add(body);
                        return null;
                    }
                });
        
        this.asyncNuregoClient = new AsyncNuregoClient("https://hello", "hello", 3600, 4);
        this.asyncNuregoClient.setAsyncRestTemplate(asyncRestTemplate);
        this.asyncNuregoClient.setBatchUpdate(true);
    }

    @Test(threadPoolSize=5, invocationCount=5, dataProvider="meterDataProvider")
    public void testUpdateAmount(Customer customer, MeteredResource meter, int amount) {
        this.asyncNuregoClient.updateAmount(customer, meter, amount);
    }

    @DataProvider(parallel = true)
    public Object[][] meterDataProvider() {
        Customer customer_1 = new Customer(null, SUBSCRIPTION_1);
        Customer customer_2 = new Customer(null, SUBSCRIPTION_2);
        return new Object[][] {
                new Object[] { customer_1, new MeteredResource("POST", "/users", 201, FEATURE_1),
                        1 },
                new Object[] { customer_1, new MeteredResource("POST", "/users", 201, FEATURE_2),
                        1 },
                new Object[] { customer_2, new MeteredResource("POST", "/users", 201, FEATURE_1),
                        1 },
                new Object[] { customer_2, new MeteredResource("POST", "/users", 201, FEATURE_2),
                        1 }
                 };
    }
    
    @Test(dependsOnMethods="testUpdateAmount")
    public void testAssertReportedData() {
        //flush any updates in cache
        this.asyncNuregoClient.flushMeterUpdates();

        Map<String, Integer> recordsReceivedByProvider = new HashMap<>();
        for (String payload : this.payloads) {
            UsageDetailsSet usageDetailsSet = (new UsageDetailsSet()).fromJSON(payload);
            Assert.assertNotNull(usageDetailsSet);
            
            Collection<UsageDetails> data = usageDetailsSet.getData();
            Assert.assertNotNull(data);
            
            for (UsageDetails usageDetails : data) {
                String key = usageDetails.getSubscription_id() + usageDetails.getFeature_id();
                Integer currentAmount = recordsReceivedByProvider.get(key);
                if (currentAmount == null) {
                    recordsReceivedByProvider.put(key, usageDetails.getAmount());
                } else {
                    recordsReceivedByProvider.put(key, currentAmount + usageDetails.getAmount());
                }
            }
        }

        Assert.assertEquals(recordsReceivedByProvider.size(), 4);
        Assert.assertEquals(recordsReceivedByProvider.get(SUBSCRIPTION_1+FEATURE_1).intValue(), 5);
        Assert.assertEquals(recordsReceivedByProvider.get(SUBSCRIPTION_1+FEATURE_2).intValue(), 5);
        Assert.assertEquals(recordsReceivedByProvider.get(SUBSCRIPTION_2+FEATURE_1).intValue(), 5);
        Assert.assertEquals(recordsReceivedByProvider.get(SUBSCRIPTION_2+FEATURE_2).intValue(), 5);
    }
}
