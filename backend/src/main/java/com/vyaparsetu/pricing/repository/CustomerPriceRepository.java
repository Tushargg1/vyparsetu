package com.vyaparsetu.pricing.repository;

import com.vyaparsetu.pricing.entity.CustomerPrice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerPriceRepository extends JpaRepository<CustomerPrice, Long> {

    Optional<CustomerPrice> findBySupplierIdAndRetailerIdAndProductIdAndActiveTrue(
            Long supplierId, Long retailerId, Long productId);

    List<CustomerPrice> findBySupplierIdAndRetailerId(Long supplierId, Long retailerId);
}
