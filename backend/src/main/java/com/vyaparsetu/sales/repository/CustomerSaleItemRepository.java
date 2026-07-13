package com.vyaparsetu.sales.repository;

import com.vyaparsetu.sales.entity.CustomerSaleItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerSaleItemRepository extends JpaRepository<CustomerSaleItem, Long> {
    List<CustomerSaleItem> findBySaleId(Long saleId);
}
