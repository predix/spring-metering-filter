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

package com.ge.predix.metering.data.repository;

import com.ge.predix.metering.data.entity.JsonUtils;
import com.ge.predix.metering.data.entity.MeteredResource;
import com.ge.predix.metering.data.entity.MeteredResources;

public class DefaultMeteredResourceRepository implements MeteredResourceRepository {

    private final MeteredResources meters;

    private final JsonUtils jsonUtils = new JsonUtils();

    public DefaultMeteredResourceRepository() {
        this.meters = this.jsonUtils.deserializeFromFile("metered-resources.json", MeteredResources.class);
    }

    @Override
    public <S extends MeteredResource> S save(final S entity) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public <S extends MeteredResource> Iterable<S> save(final Iterable<S> entities) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public MeteredResource findOne(final Long id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean exists(final Long id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Iterable<MeteredResource> findAll() {
        return this.meters;
    }

    @Override
    public Iterable<MeteredResource> findAll(final Iterable<Long> ids) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long count() {
        return this.meters.size();
    }

    @Override
    public void delete(final Long id) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(final MeteredResource entity) {
        // TODO Auto-generated method stub

    }

    @Override
    public void delete(final Iterable<? extends MeteredResource> entities) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteAll() {
        // TODO Auto-generated method stub
    }

}
