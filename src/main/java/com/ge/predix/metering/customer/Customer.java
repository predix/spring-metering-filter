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

public class Customer {

    private String organizationId;
    private String subscriptionId;

    public Customer() {
        // Default constructor.
    }

    public Customer(final String organizationId, final String subscriptionId) {

        this.organizationId = organizationId;
        this.subscriptionId = subscriptionId;
    }

    public String getOrganizationId() {
        return this.organizationId;
    }

    public void setOrganizationId(final String organizationId) {
        this.organizationId = organizationId;
    }

    public String getSubscriptionId() {
        return this.subscriptionId;
    }

    public void setSubscriptionId(final String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    @Override
    public String toString() {
        return "CustomerInfo [organizationId=" + this.organizationId + ", subscriptionId=" + this.subscriptionId + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.organizationId == null) ? 0 : this.organizationId.hashCode());
        result = prime * result + ((this.subscriptionId == null) ? 0 : this.subscriptionId.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Customer)) {
            return false;
        }
        Customer other = (Customer) obj;
        if (this.organizationId == null) {
            if (other.organizationId != null) {
                return false;
            }
        } else if (!this.organizationId.equals(other.organizationId)) {
            return false;
        }
        if (this.subscriptionId == null) {
            if (other.subscriptionId != null) {
                return false;
            }
        } else if (!this.subscriptionId.equals(other.subscriptionId)) {
            return false;
        }
        return true;
    }
}
