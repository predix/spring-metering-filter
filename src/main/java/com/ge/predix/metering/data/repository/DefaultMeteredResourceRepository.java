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
