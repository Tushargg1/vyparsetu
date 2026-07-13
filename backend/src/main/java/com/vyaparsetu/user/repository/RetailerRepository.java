package com.vyaparsetu.user.repository;

import com.vyaparsetu.user.entity.Retailer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RetailerRepository extends JpaRepository<Retailer, Long> {
    Optional<Retailer> findByUserId(Long userId);

    List<Retailer> findByDistributorId(Long distributorId);
}
