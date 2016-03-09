package com.ge.predix.metering.filter;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.ge.predix.metering.data.entity.JsonUtils;
import com.ge.predix.metering.data.entity.MeteredResource;
import com.ge.predix.metering.data.entity.MeteredResources;

public class JsonUtilsTest {

    private final JsonUtils jsonUtils = new JsonUtils();

    
    @Test
    public void testDeserializeCollectionFromFile() {
        @SuppressWarnings("unchecked")
        List<MeteredResource> meteredResource = this.jsonUtils.
                deserializeFromFile("metered-resources.json", List.class);
        Assert.assertEquals(meteredResource.size(), 6);
    }

    @Test
    public void testIterateThroughMeteredResources() {
        Iterable<MeteredResource> meters = this.jsonUtils.
                deserializeFromFile("metered-resources.json", MeteredResources.class);

        for (MeteredResource meter : meters) {
            Assert.assertNotNull(meter);
        }
    }
}