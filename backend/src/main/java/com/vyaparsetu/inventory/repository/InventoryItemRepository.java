package com.vyaparsetu.inventory.repository;

import com.vyaparsetu.inventory.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    Optional<InventoryItem> findByRetailerIdAndProductId(Long retailerId, Long productId);

    List<InventoryItem> findByRetailerId(Long retailerId);

    @Query("SELECT i FROM InventoryItem i WHERE i.retailerId = :retailerId AND i.quantity <= i.reorderLevel")
    List<InventoryItem> findLowStock(@Param("retailerId") Long retailerId);
}
