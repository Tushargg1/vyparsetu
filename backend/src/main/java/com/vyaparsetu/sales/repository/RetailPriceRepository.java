package com.vyaparsetu.sales.repository;

import com.vyaparsetu.sales.entity.RetailPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RetailPriceRepository extends JpaRepository<RetailPrice, Long> {
    Optional<RetailPrice> findByRetailerIdAndProductId(Long retailerId, Long productId);

    List<RetailPrice> findByRetailerId(Long retailerId);
}
