package com.vyaparsetu.sales.repository;

import com.vyaparsetu.sales.entity.CustomerSale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerSaleRepository extends JpaRepository<CustomerSale, Long> {
    Page<CustomerSale> findByRetailerIdOrderByCreatedAtDesc(Long retailerId, Pageable pageable);
}
