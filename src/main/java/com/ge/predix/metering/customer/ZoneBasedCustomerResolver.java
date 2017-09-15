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

package com.ge.predix.metering.customer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

public class ZoneBasedCustomerResolver implements CustomerResolver {

    @Value("${METER_BASE_DOMAIN:localhost}")
    private String serviceBaseDomain;

    private static final Logger LOGGER = LoggerFactory.getLogger(ZoneBasedCustomerResolver.class);

    @Override
    public Customer resolveCustomer(final HttpServletRequest request) {

        String zone;
        if (StringUtils.isNotEmpty(request.getHeader("Predix-Zone-Id"))) {
            zone = request.getHeader("Predix-Zone-Id");

        } else if (StringUtils.isNotEmpty(request.getHeader("X-Identity-Zone-Id"))) {
            zone = request.getHeader("X-Identity-Zone-Id");
        } else {

            // get zone from subdomain
            zone = getZoneNameFromRequestHostName(request.getServerName(), this.serviceBaseDomain);
            if (StringUtils.isEmpty(zone)) {
                LOGGER.debug("Failed to resolve customer from request because the request does not contain a"
                        + " 'Predix-Zone-Id' or 'X-Identity-Zone-Id' header or a subdomain.");
                return null;
            }
        }

        return new Customer(null, zone);
    }

    /**
     * @return empty string if requestHostname and baseDomain are identical, null if domain is not a sub-string of
     *         requestHostname
     */
    static String getZoneNameFromRequestHostName(final String requestHostname, final String baseDomain) {

        if (requestHostname.equals(baseDomain)) {
            return "";
        }

        String regexPattern = "^(.*?)\\." + Pattern.quote(baseDomain) + "$";
        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(requestHostname);
        if (!matcher.matches()) {
            // There is no zone scope for this request. Return null
            return null;
        }

        String subdomain = matcher.group(1);

        return subdomain;
    }

    public void setServiceBaseDomain(final String serviceBaseDomain) {
        this.serviceBaseDomain = serviceBaseDomain;
    }

}
