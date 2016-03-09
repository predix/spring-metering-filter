package com.ge.predix.metering.data.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.ge.predix.metering.data.entity.MeteredResource;

@Repository
public interface MeteredResourceRepository extends CrudRepository<MeteredResource, Long> {

    // Just a repository.
}
