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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

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
    private Map<String, String> credentials;
    private LocalDateTime tokenExpiration;
    private String token;

    @Autowired
    private AsyncRestTemplate meteringAsyncRestTemplate;

    @Autowired
    private RestTemplate meteringRestTemplate;

    private DateTime nextSend;
    private boolean batchUpdate = true;

    public boolean isBatchUpdate() {
        return batchUpdate;
    }

    public void setBatchUpdate(boolean batchUpdate) {
        this.batchUpdate = batchUpdate;
    }

    public AsyncNuregoClient(final String url, final int batchIntervalSeconds, final int batchMaxMapSize,
            final String nuregoUsername, final String nuregoPassword, final String nuregoInstanceId) {

        this.tokenExpiration = LocalDateTime.now().minus(1, ChronoUnit.SECONDS);

        setupAuthentication(nuregoUsername, nuregoPassword, nuregoInstanceId);

        if (StringUtils.isNotEmpty(url)) {
            Nurego.setApiBase(url);
        }
        this.batchIntervalSeconds = batchIntervalSeconds;
        this.batchMaxMapSize = batchMaxMapSize;
        this.nextSend = DateTime.now().plusSeconds(this.batchIntervalSeconds);
        LOGGER.debug("url: '{}'", url);
        LOGGER.debug("batchIntervalSeconds: '{}'", batchIntervalSeconds);
        LOGGER.debug("batchMaxMapSize: '{}'", batchMaxMapSize);
    }
    
    @Override
    public void updateAmount(final Customer customer, final MeteredResource meter, final int amount) {
        Map<CustomerMeteredResource, Integer> tempMap = new HashMap<>();
        synchronized (this.updateMap) {
            CustomerMeteredResource update = new CustomerMeteredResource(customer, meter);
            this.updateMap.merge(update, amount, Integer::sum);

            if (!isTimeToSend()) {
                return;
            }
            tempMap.putAll(this.updateMap);
            this.updateMap.clear();
        }

        updateMeteringProvider(tempMap);
    }

    private void updateMeteringProvider(final Map<CustomerMeteredResource, Integer> meterEntries) {
        if (this.batchUpdate) {
            updateMeteringProviderPerBatch(meterEntries);
        } else {
            updateMeteringProviderPerSubscription(meterEntries);
        }
    }
    
    private void updateMeteringProviderPerBatch(Map<CustomerMeteredResource, Integer> meterEntries) {
        if (meterEntries == null || meterEntries.isEmpty()) return;
        
        LOGGER.info("Report usage to metering provider in a batch. EntryCount = " + meterEntries.size());

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-NUREGO-AUTHORIZATION", String.format("Bearer %s", Nurego.apiKey));
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Collection<UsageDetails> usageData = new ArrayList<UsageDetails>();
        int id = 0;
        for (Entry<CustomerMeteredResource, Integer> entry : meterEntries.entrySet()) {
            CustomerMeteredResource customerMeteredResource = entry.getKey();
            Integer entryCurrentAmount = entry.getValue();
            
            UsageDetails usageDetails = new UsageDetails(customerMeteredResource.getCustomer().getSubscriptionId(), 
                    customerMeteredResource.getMeteredResource().getFeatureId(), entryCurrentAmount, 
                    Integer.toString(id++));
            
            usageData.add(usageDetails);
        }
        
        UsageDetailsSet usageDetailsSet = new UsageDetailsSet(usageData);
        String usageDetailsContent = usageDetailsSet.toJSON();
        LOGGER.debug("Usage details in JSON format are: " + usageDetailsContent);
        
        String url = String.format("%s/v1/usages", Nurego.getApiBase());

        HttpEntity<?> request = new HttpEntity<String>(usageDetailsContent, headers);
        LOGGER.debug("The request in spring metering filter to report usage is: " + request.toString());

        try {
            // Fire and forget.. do not wait to for the results in this thread
            this.asyncRestTemplate.postForEntity(url, request, String.class);
        } catch (RestClientException ex) {
            LOGGER.error(String.format("Failed to batch update usages."));
        }
    }

    private void updateMeteringProviderPerSubscription(Map<CustomerMeteredResource, Integer> meterEntries) {

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", String.format("bearer %s", getNuregoToken()));
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> usageParams = new HashMap<>();
        usageParams.put("provider", "cloud-foundry");

        LOGGER.debug("start: update metering provider per subscription. entryCount = {}", meterEntries.size());

        for (Map.Entry<CustomerMeteredResource, Integer> entry : meterEntries.entrySet()) {
            CustomerMeteredResource customerMeteredResource = entry.getKey();
            Integer entryCurrentAmount = entry.getValue();
            String url = String.format("%s/v1/subscriptions/%s/entitlements/usage", Nurego.getApiBase(),
                    customerMeteredResource.getCustomer().getSubscriptionId());

            usageParams.put("feature_id", customerMeteredResource.getMeteredResource().getFeatureId());
            usageParams.put("amount", entryCurrentAmount);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(usageParams, headers);

            LOGGER.debug("The request in spring metering filter is :" + request.toString());
            try {
                // Fire and forget.. do not wait to for the results in this thread
                this.meteringAsyncRestTemplate.postForEntity(url, request, String.class);
            } catch (RestClientException ex) {
                LOGGER.error("Failed to update usage for featureId '{}'.",
                        customerMeteredResource.getMeteredResource().getFeatureId());
            }
        }
        LOGGER.debug("end: update metering provider per subscription. entryCount = {}", meterEntries.size());
    }

    String getNuregoToken() {
        if (LocalDateTime.now().isAfter(this.tokenExpiration)) {
            String tokenUrl = String.format("%s/v1/auth/token", Nurego.getApiBase());
            NuregoTokenResponse response;
            try {
                response = this.meteringRestTemplate.postForEntity(tokenUrl, credentials,
                        NuregoTokenResponse.class).getBody();
            } catch (RestClientException ex) {
                LOGGER.error("Unable to get token", ex);
                return "";
            }

            this.tokenExpiration = LocalDateTime.now().plus(response.getExpiry() - 1, ChronoUnit.SECONDS);
            this.token = response.getAccessToken();
        }
        return this.token;
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
        this.meteringAsyncRestTemplate = asyncRestTemplate;
    }

    public void setRestTemplate(final RestTemplate restTemplate) {
        this.meteringRestTemplate = restTemplate;
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

    private void setupAuthentication(final String nuregoUsername, final String nuregoPassword,
            final String nuregoInstanceId) {
        if (StringUtils.isNotEmpty(nuregoUsername) && StringUtils.isNotEmpty(nuregoPassword)
                && StringUtils.isNotEmpty(nuregoInstanceId)) {
            this.credentials = new HashMap<String, String>() {
                {
                    put("username", nuregoUsername);
                    put("password", nuregoPassword);
                    put("instance_id", nuregoInstanceId);
                }
            };
        }
    }
}
