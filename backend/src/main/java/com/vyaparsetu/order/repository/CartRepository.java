package com.vyaparsetu.order.repository;

import com.vyaparsetu.order.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByRetailerIdAndSupplierId(Long retailerId, Long supplierId);

    List<Cart> findByRetailerId(Long retailerId);
}
