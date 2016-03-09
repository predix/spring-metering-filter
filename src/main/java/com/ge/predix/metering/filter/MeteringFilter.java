package com.ge.predix.metering.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ge.predix.metering.customer.Customer;
import com.ge.predix.metering.customer.CustomerResolver;
import com.ge.predix.metering.data.entity.MeteredResource;
import com.ge.predix.metering.data.repository.MeteredResourceRepository;
import com.ge.predix.metering.nurego.NuregoClient;


public class MeteringFilter extends OncePerRequestFilter {

    @Autowired
    private CustomerResolver customerResolver;

    @Autowired
    private NuregoClient nuregoClient;

    @Autowired
    private MeteredResourceRepository repository;

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {

        filterChain.doFilter(request, response);

        if ( (200 > response.getStatus()) || (400 <= response.getStatus())) {
            return;
        }

        Iterable<MeteredResource> meters = this.repository.findAll();
        for (MeteredResource meter : meters) {
            if (!request.getMethod().equalsIgnoreCase(meter.getHttpMethod())) {
                continue;
            }

            if (!meter.isUriTemplateMatch(request.getRequestURI())) {
                continue;
            }

            if (response.getStatus() != meter.getExpectedHttpStatusCode()) {
                continue;
            }

            Customer customer = this.customerResolver.resolveCustomer(request);
            if (null != customer) {
                this.nuregoClient.updateAmount(customer, meter, 1);
            }
        }
    }

    public void setCustomerResolver(CustomerResolver customerResolver) {
        this.customerResolver = customerResolver;
    }

    public void setNuregoClient(final NuregoClient nuregoClient) {
        this.nuregoClient = nuregoClient;
    }

    public void setRepository(final MeteredResourceRepository repository) {
        this.repository = repository;
    }

}
