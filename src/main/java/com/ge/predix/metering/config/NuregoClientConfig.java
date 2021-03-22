package com.ge.predix.metering.config;

import com.ge.predix.metering.nurego.AsyncNuregoClient;
import com.ge.predix.metering.nurego.NuregoClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

@Configuration
public class NuregoClientConfig {

    @Bean
    public NuregoClient nuregoClient(
            @Value("${NUREGO_API_URL}") final String url,
            @Value("${NUREGO_BATCH_INTERVAL_SECONDS:3600}") final int batchIntervalSeconds,
            @Value("${NUREGO_BATCH_MAX_MAP_SIZE:1024}") final int batchMaxMapSize,
            @Value("${NUREGO_USERNAME:nuregoUsername}") final String nuregoUsername,
            @Value("${NUREGO_PASSWORD:nuregoPassword}") final String nuregoPassword,
            @Value("${NUREGO_INSTANCE_ID:nuregoInstanceId}") final String nuregoInstanceId,
            @Value("${httpAsyncClient.maxTotal:64}") final int asyncMaxTotal
    ) throws IOReactorException {

        AsyncRestTemplate meteringAsyncRestTemplate = buildMeteringAsyncRestTemplate(asyncMaxTotal);
        RestTemplate meteringRestTemplate = buildMeteringRestTemplate(64);

        AsyncNuregoClient nuregoClient = new AsyncNuregoClient(url, batchIntervalSeconds, batchMaxMapSize,
                nuregoUsername, nuregoPassword, nuregoInstanceId);
        nuregoClient.setAsyncRestTemplate(meteringAsyncRestTemplate);
        nuregoClient.setRestTemplate(meteringRestTemplate);
        return nuregoClient;
    }

    private static AsyncRestTemplate buildMeteringAsyncRestTemplate(final int maxTotal) throws IOReactorException {

        PoolingNHttpClientConnectionManager meteringAsyncConnectionManager =
                new PoolingNHttpClientConnectionManager(new DefaultConnectingIOReactor());
        meteringAsyncConnectionManager.setMaxTotal(maxTotal);

        CloseableHttpAsyncClient meteringAsyncHttpClient = HttpAsyncClientBuilder.create()
                .useSystemProperties()
                .setConnectionManager(meteringAsyncConnectionManager)
                .build();

        HttpComponentsAsyncClientHttpRequestFactory meteringAsyncRequestFactory =
                new HttpComponentsAsyncClientHttpRequestFactory(meteringAsyncHttpClient);

        return new AsyncRestTemplate(meteringAsyncRequestFactory);
    }

    private static RestTemplate buildMeteringRestTemplate(final int maxTotal) {

        PoolingHttpClientConnectionManager meteringConnectionManager = new PoolingHttpClientConnectionManager();
        meteringConnectionManager.setMaxTotal(maxTotal);

        CloseableHttpClient meteringHttpClient = HttpClientBuilder.create()
                .useSystemProperties()
                .setConnectionManager(meteringConnectionManager)
                .build();

        HttpComponentsClientHttpRequestFactory meteringHttpRequestFactory =
                new HttpComponentsClientHttpRequestFactory(meteringHttpClient);

        return new RestTemplate(meteringHttpRequestFactory);
    }
}
