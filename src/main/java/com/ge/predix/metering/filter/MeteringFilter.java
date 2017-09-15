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
        int responseStatus = response.getStatus();

        if ((200 > responseStatus) || (400 <= responseStatus)) {
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

            if (responseStatus != meter.getExpectedHttpStatusCode()) {
                continue;
            }

            Customer customer = this.customerResolver.resolveCustomer(request);
            if (null != customer) {
                this.nuregoClient.updateAmount(customer, meter, 1);
            }
        }
    }

    void setCustomerResolver(final CustomerResolver customerResolver) {
        this.customerResolver = customerResolver;
    }

    void setNuregoClient(final NuregoClient nuregoClient) {
        this.nuregoClient = nuregoClient;
    }

    void setRepository(final MeteredResourceRepository repository) {
        this.repository = repository;
    }

}
