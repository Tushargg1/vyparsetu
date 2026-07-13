package com.vyaparsetu.user.repository;

import com.vyaparsetu.user.entity.DistributorPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DistributorPolicyRepository extends JpaRepository<DistributorPolicy, Long> {
    Optional<DistributorPolicy> findBySupplierId(Long supplierId);
}
