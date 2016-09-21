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
package com.ge.predix.metering.nurego;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestClientException;

import com.ge.predix.metering.customer.Customer;
import com.ge.predix.metering.data.entity.MeteredResource;
import com.nurego.Nurego;

@Component
public class AsyncNuregoClient implements NuregoClient, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncNuregoClient.class);

    private final Map<CustomerMeteredResource, Integer> updateMap = Collections
            .synchronizedMap(new HashMap<CustomerMeteredResource, Integer>());

    private final int batchIntervalSeconds;
    private final int batchMaxMapSize;

    @Autowired
    private AsyncRestTemplate asyncRestTemplate;

    private DateTime nextSend;

    public AsyncNuregoClient(final String url, final String apiKey, final int batchIntervalSeconds,
            final int batchMaxMapSize) {
        Nurego.apiKey = apiKey;
        if (StringUtils.isNotEmpty(url)) {
            Nurego.setApiBase(url);
        }
        this.batchIntervalSeconds = batchIntervalSeconds;
        this.batchMaxMapSize = batchMaxMapSize;
        this.nextSend = DateTime.now().plusSeconds(this.batchIntervalSeconds);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("url: '%s'", url));
            LOGGER.debug(String.format("batchIntervalSeconds: '%s'", batchIntervalSeconds));
            LOGGER.debug(String.format("batchMaxMapSize: '%s'", batchMaxMapSize));
        }
    }

    @Override
    public void updateAmount(final Customer customer, final MeteredResource meter, final int amount) {
        Map<CustomerMeteredResource, Integer> tempMap = new HashMap<>();
        synchronized (this.updateMap) {
            CustomerMeteredResource update = new CustomerMeteredResource(customer, meter);
            Integer currentAmount = this.updateMap.get(update);
            if (null == currentAmount) {
                this.updateMap.put(update, amount);
            } else {
                this.updateMap.put(update, currentAmount + amount);
            }

            if (!isTimeToSend()) {
                return;
            }
            tempMap.putAll(this.updateMap);
            this.updateMap.clear();
        }

        updateMeteringProvider(tempMap);
    }

    private void updateMeteringProvider(final Map<CustomerMeteredResource, Integer> meterEntries) {

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-NUREGO-AUTHORIZATION", String.format("Bearer %s", Nurego.apiKey));
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> usageParams = new HashMap<String, Object>();
        usageParams.put("provider", "cloud-foundry");

        LOGGER.info("start: update metering provider. entryCount = " + meterEntries.size());

        for (Entry<CustomerMeteredResource, Integer> entry : meterEntries.entrySet()) {
            CustomerMeteredResource customerMeteredResource = entry.getKey();
            Integer entryCurrentAmount = entry.getValue();
            String url = String.format("%s/v1/subscriptions/%s/entitlements/usage", Nurego.getApiBase(),
                    customerMeteredResource.getCustomer().getSubscriptionId());

            usageParams.put("feature_id", customerMeteredResource.getMeteredResource().getFeatureId());
            usageParams.put("amount", entryCurrentAmount);

            HttpEntity<?> request = new HttpEntity<Map<String, Object>>(usageParams, headers);

            LOGGER.debug("The request in spring metering filter is :" + request.toString());
            try {
                // Fire and forget.. do not wait to for the results in this thread
                ListenableFuture<ResponseEntity<String>> future =
                        this.asyncRestTemplate.postForEntity(url, request, String.class);

                future.addCallback(new ListenableFutureCallback<ResponseEntity>() {
                    @Override
                    public void onSuccess(final ResponseEntity result) {
                        LOGGER.info("Response received (async callable): " + result.getStatusCode());
                        // Need assertions
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        // Need assertions
                        LOGGER.error("Response failed.", t);
                    }
                });
            } catch (RestClientException ex) {
                LOGGER.error(String.format("Failed to update usage for featureId '%s'.",
                        customerMeteredResource.getMeteredResource().getFeatureId()));
            }
        }
        LOGGER.info("end: update metering provider. entryCount = " + meterEntries.size());
    }

    private boolean isTimeToSend() {

        if ((this.batchMaxMapSize <= this.updateMap.size()) || (DateTime.now().isAfter(this.nextSend))) {
            this.nextSend = DateTime.now().plusSeconds(this.batchIntervalSeconds);
            return true;
        }

        return false;
    }

    @Override
    public void logOpenCircuit() {
        LOGGER.error("Too many failures calling the Nurego API. Consequently, we are opening the circuit.");
    }

    public void setAsyncRestTemplate(final AsyncRestTemplate asyncRestTemplate) {
        this.asyncRestTemplate = asyncRestTemplate;
    }

    public void flushMeterUpdates() {
        Map<CustomerMeteredResource, Integer> tempMap = new HashMap<>();
        synchronized (this.updateMap) {
            tempMap.putAll(this.updateMap);
            this.updateMap.clear();
        }
        updateMeteringProvider(tempMap);
    }

    @Override
    public void destroy() throws Exception {
        flushMeterUpdates();
    }
}
