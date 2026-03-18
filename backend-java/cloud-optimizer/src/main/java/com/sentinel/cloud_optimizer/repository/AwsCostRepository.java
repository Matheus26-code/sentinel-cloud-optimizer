package com.sentinel.cloud_optimizer.repository;

import com.sentinel.cloud_optimizer.model.AwsCost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AwsCostRepository extends JpaRepository<AwsCost, Long> {

}