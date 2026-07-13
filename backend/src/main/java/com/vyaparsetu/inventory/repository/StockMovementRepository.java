package com.vyaparsetu.inventory.repository;

import com.vyaparsetu.inventory.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByInventoryItemIdOrderByCreatedAtDesc(Long inventoryItemId);
}
