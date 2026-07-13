package com.vyaparsetu.sales.repository;

import com.vyaparsetu.sales.entity.RetailerDiscount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RetailerDiscountRepository extends JpaRepository<RetailerDiscount, Long> {
    List<RetailerDiscount> findByRetailerId(Long retailerId);
}
