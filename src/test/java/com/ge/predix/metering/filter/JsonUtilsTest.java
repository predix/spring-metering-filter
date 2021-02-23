/*******************************************************************************
 * Copyright 2021 General Electric Company
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
        List<MeteredResource> meteredResource = this.jsonUtils.deserializeFromFile("metered-resources.json",
                List.class);
        Assert.assertEquals(meteredResource.size(), 7);
    }

    @Test
    public void testIterateThroughMeteredResources() {
        Iterable<MeteredResource> meters = this.jsonUtils.deserializeFromFile("metered-resources.json",
                MeteredResources.class);

        for (MeteredResource meter : meters) {
            Assert.assertNotNull(meter);
        }
    }
}