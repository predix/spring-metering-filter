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

import com.ge.predix.metering.customer.Customer;
import com.ge.predix.metering.data.entity.MeteredResource;

public class CustomerMeteredResource {

    private final Customer customer;
    private final MeteredResource meteredResource;

    public Customer getCustomer() {
        return this.customer;
    }

    public MeteredResource getMeteredResource() {
        return this.meteredResource;
    }

    public CustomerMeteredResource(final Customer customer, final MeteredResource meteredResource) {

        this.customer = customer;
        this.meteredResource = meteredResource;
    }

    @Override
    public String toString() {
        return "Update [customer=" + this.customer + ", meteredResource=" + this.meteredResource + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.customer == null) ? 0 : this.customer.hashCode());
        result = prime * result + ((this.meteredResource == null) ? 0 : this.meteredResource.hashCode());
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
        if (!(obj instanceof CustomerMeteredResource)) {
            return false;
        }
        CustomerMeteredResource other = (CustomerMeteredResource) obj;
        if (this.customer == null) {
            if (other.customer != null) {
                return false;
            }
        } else if (!this.customer.equals(other.customer)) {
            return false;
        }
        if (this.meteredResource == null) {
            if (other.meteredResource != null) {
                return false;
            }
        } else if (!this.meteredResource.equals(other.meteredResource)) {
            return false;
        }
        return true;
    }
}
