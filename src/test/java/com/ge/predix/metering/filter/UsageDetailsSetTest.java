package com.ge.predix.metering.filter;

import java.util.ArrayList;
import java.util.Collection;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.ge.predix.metering.nurego.UsageDetails;
import com.ge.predix.metering.nurego.UsageDetailsSet;

public class UsageDetailsSetTest {
    private final String testJson = 
            "{\"data\":[" +
                    "{\"provider\":\"cloud-foundry\",\"subscription_id\":\"subscriptionOne\"," +
                     "\"feature_id\":\"fetaureOne\",\"amount\":100,\"id\":\"1\"}," +
                    "{\"provider\":\"cloud-foundry\",\"subscription_id\":\"subscriptionTwo\"," +
                     "\"feature_id\":\"fetaureTwo\",\"amount\":200,\"id\":\"2\"}" +
            "],\"object\":\"list\",\"count\":2}";
    
    @Test
    public void testUsageDetailsSetToJSON() {
        UsageDetails usageDetailsOne = new UsageDetails("subscriptionOne", "fetaureOne", 100, "1");
        UsageDetails usageDetailsTwo = new UsageDetails("subscriptionTwo", "fetaureTwo", 200, "2");

        Collection<UsageDetails> usageData = new ArrayList<UsageDetails>();
        usageData.add(usageDetailsOne);
        usageData.add(usageDetailsTwo);

        UsageDetailsSet usageDetailsSet = new UsageDetailsSet(usageData);
        String usageDetailsContent = usageDetailsSet.toJSON();
        Assert.assertNotNull(usageDetailsContent);
        Assert.assertEquals(usageDetailsContent, testJson);
        System.out.println("usageDetailsContent: " + usageDetailsContent);
    }

    @Test
    public void testUsageDetailsSetFromJSON() {
        UsageDetailsSet usageDetailsSet = (new UsageDetailsSet()).fromJSON(testJson);
        Assert.assertNotNull(usageDetailsSet);
        Assert.assertEquals(usageDetailsSet.getCount().intValue(), 2);
        Assert.assertEquals(usageDetailsSet.getObject(), "list");
        
        Collection<UsageDetails> data = usageDetailsSet.getData();
        Assert.assertNotNull(data);
        Assert.assertEquals(data.size(), 2);
        
        assertUsageDetails(((ArrayList<UsageDetails>)data).get(0), "subscriptionOne", "fetaureOne", 100, "1"); 
        assertUsageDetails(((ArrayList<UsageDetails>)data).get(1), "subscriptionTwo", "fetaureTwo", 200, "2"); 
    }

    private void assertUsageDetails(UsageDetails usageDetails, String subscription, 
            String fetaure, int amount, String id) {
        Assert.assertEquals(usageDetails.getProvider(), "cloud-foundry");
        Assert.assertEquals(usageDetails.getId(), id);
        Assert.assertEquals(usageDetails.getSubscription_id(), subscription);
        Assert.assertEquals(usageDetails.getFeature_id(), fetaure);
        Assert.assertEquals(usageDetails.getAmount().intValue(), amount);
    }
}
