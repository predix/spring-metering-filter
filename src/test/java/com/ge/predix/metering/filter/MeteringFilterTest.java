package com.ge.predix.metering.filter;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.FilterChain;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.ge.predix.metering.customer.Customer;
import com.ge.predix.metering.customer.ZoneBasedCustomerResolver;
import com.ge.predix.metering.data.entity.MeteredResource;
import com.ge.predix.metering.data.repository.MeteredResourceRepository;

import junit.framework.Assert;

public class MeteringFilterTest {

    private static final String SUBSCRIPTION = "subscription_123";
    private final MeteringFilter filter = new MeteringFilter();

    @BeforeClass
    public void setup() {
        List<MeteredResource> meters = new ArrayList<>();
        MeteredResource meter = new MeteredResource("POST", "/users", 201, "5813");
        meters.add(meter);
        MeteredResourceRepository repository = mock(MeteredResourceRepository.class);
        when(repository.findAll()).thenReturn(meters);

        this.filter.setRepository(repository);

        ZoneBasedCustomerResolver customerResolver = new ZoneBasedCustomerResolver();
        customerResolver.setServiceBaseDomain("ge.com");
        this.filter.setCustomerResolver(customerResolver);
    }

    @Test
    public void testFilter() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Predix-Zone-Id", SUBSCRIPTION);
        request.setMethod("POST");
        request.setRequestURI("/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);

        FilterChain filterChain = mock(FilterChain.class);
        doNothing().when(filterChain).doFilter(request, response);

        final MockNuregoClient nuregoClient = new MockNuregoClient();
        this.filter.setNuregoClient(nuregoClient);
        this.filter.doFilter(request, response, filterChain);

        Customer customer = new Customer(null, SUBSCRIPTION);
        MeteredResource meter = new MeteredResource("POST", "/users", 201, "5813");
        int actualMeterAmount = nuregoClient.getMeterAmount(customer, meter);
        Assert.assertEquals(1, actualMeterAmount);
    }

    @Test
    public void testFilterCaseInsensitivity() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Predix-Zone-Id", SUBSCRIPTION);
        request.setMethod("POST");
        request.setRequestURI("/Users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);

        FilterChain filterChain = mock(FilterChain.class);
        doNothing().when(filterChain).doFilter(request, response);

        final MockNuregoClient nuregoClient = new MockNuregoClient();
        this.filter.setNuregoClient(nuregoClient);
        this.filter.doFilter(request, response, filterChain);

        Customer customer = new Customer(null, SUBSCRIPTION);
        MeteredResource meter = new MeteredResource("POST", "/users", 201, "5813");
        int actualMeterAmount = nuregoClient.getMeterAmount(customer, meter);
        Assert.assertEquals(1, actualMeterAmount);
    }

    @Test
    public void testFilterUaaZoneHeader() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Identity-Zone-Id", SUBSCRIPTION);
        request.setMethod("POST");
        request.setRequestURI("/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);

        FilterChain filterChain = mock(FilterChain.class);
        doNothing().when(filterChain).doFilter(request, response);

        final MockNuregoClient nuregoClient = new MockNuregoClient();
        this.filter.setNuregoClient(nuregoClient);
        this.filter.doFilter(request, response, filterChain);

        Customer customer = new Customer(null, SUBSCRIPTION);
        MeteredResource meter = new MeteredResource("POST", "/users", 201, "5813");
        int actualMeterAmount = nuregoClient.getMeterAmount(customer, meter);
        Assert.assertEquals(1, actualMeterAmount);
    }

    @Test
    public void testFilterSubdomain() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setServerName(SUBSCRIPTION + ".ge.com");
        request.setRequestURI("/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);

        FilterChain filterChain = mock(FilterChain.class);
        doNothing().when(filterChain).doFilter(request, response);

        final MockNuregoClient nuregoClient = new MockNuregoClient();
        this.filter.setNuregoClient(nuregoClient);
        this.filter.doFilter(request, response, filterChain);

        Customer customer = new Customer(null, SUBSCRIPTION);
        MeteredResource meter = new MeteredResource("POST", "/users", 201, "5813");
        int actualMeterAmount = nuregoClient.getMeterAmount(customer, meter);
        Assert.assertEquals(1, actualMeterAmount);
    }

    @Test
    public void testFilterSubdomainAndHeader() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.addHeader("X-Identity-Zone-Id", SUBSCRIPTION);
        request.setServerName("123456789.ge.com");
        request.setRequestURI("/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);

        FilterChain filterChain = mock(FilterChain.class);
        doNothing().when(filterChain).doFilter(request, response);

        final MockNuregoClient nuregoClient = new MockNuregoClient();
        this.filter.setNuregoClient(nuregoClient);
        this.filter.doFilter(request, response, filterChain);

        Customer customer = new Customer(null, SUBSCRIPTION);
        MeteredResource meter = new MeteredResource("POST", "/users", 201, "5813");
        int actualMeterAmount = nuregoClient.getMeterAmount(customer, meter);
        Assert.assertEquals(1, actualMeterAmount);
    }

    @Test
    public void testFilterNoZoneHeader() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);

        FilterChain filterChain = mock(FilterChain.class);
        doNothing().when(filterChain).doFilter(request, response);

        final MockNuregoClient nuregoClient = new MockNuregoClient();
        this.filter.setNuregoClient(nuregoClient);
        this.filter.doFilter(request, response, filterChain);

        Customer customer = new Customer(null, SUBSCRIPTION);
        MeteredResource meter = new MeteredResource("POST", "/users", 201, "5813");
        Integer actualMeterAmount = nuregoClient.getMeterAmount(customer, meter);
        Assert.assertNull(actualMeterAmount);
    }

    @Test
    public void testCustomerResolverNoMeterAmount() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Predix-Zone-Id", SUBSCRIPTION);
        request.setMethod("POST");
        request.setRequestURI("/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);

        FilterChain filterChain = mock(FilterChain.class);
        doNothing().when(filterChain).doFilter(request, response);

        final MockNuregoClient nuregoClient = new MockNuregoClient();
        this.filter.setNuregoClient(nuregoClient);
        this.filter.doFilter(request, response, filterChain);

        Customer otherCustomer = new Customer(null, "OTHER_SUBSCRIPTION");
        MeteredResource meter = new MeteredResource("POST", "/users", 201, "5813");
        Assert.assertEquals(null, nuregoClient.getMeterAmount(otherCustomer, meter));
    }

    @Test
    public void testFilterNoHttpMethodMatch() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);

        FilterChain filterChain = mock(FilterChain.class);
        doNothing().when(filterChain).doFilter(request, response);

        final MockNuregoClient nuregoClient = new MockNuregoClient();
        this.filter.setNuregoClient(nuregoClient);
        this.filter.doFilter(request, response, filterChain);

        Customer customer = new Customer(null, SUBSCRIPTION);
        MeteredResource meter = new MeteredResource("POST", "/users", 201, "5813");
        Integer actualMeterAmount = nuregoClient.getMeterAmount(customer, meter);
        Assert.assertNull(actualMeterAmount);
    }

    @Test
    public void testFilterNoUriMatch() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/clients");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(201);

        FilterChain filterChain = mock(FilterChain.class);
        doNothing().when(filterChain).doFilter(request, response);

        final MockNuregoClient nuregoClient = new MockNuregoClient();
        this.filter.setNuregoClient(nuregoClient);
        this.filter.doFilter(request, response, filterChain);

        Customer customer = new Customer(null, SUBSCRIPTION);
        MeteredResource meter = new MeteredResource("POST", "/users", 201, "5813");
        Integer actualMeterAmount = nuregoClient.getMeterAmount(customer, meter);
        Assert.assertNull(actualMeterAmount);
    }

    @Test
    public void testFilterNoHttpStatusCodeMatch() throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(401);

        FilterChain filterChain = mock(FilterChain.class);
        doNothing().when(filterChain).doFilter(request, response);

        final MockNuregoClient nuregoClient = new MockNuregoClient();
        this.filter.setNuregoClient(nuregoClient);
        this.filter.doFilter(request, response, filterChain);

        Customer customer = new Customer(null, SUBSCRIPTION);
        MeteredResource meter = new MeteredResource("POST", "/users", 201, "5813");
        Integer actualMeterAmount = nuregoClient.getMeterAmount(customer, meter);
        Assert.assertNull(actualMeterAmount);
    }

}
