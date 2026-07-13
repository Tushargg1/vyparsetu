package com.vyaparsetu.inventory.repository;

import com.vyaparsetu.inventory.entity.InventoryBatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, Long> {

    // FEFO: earliest expiry first, with remaining quantity
    List<InventoryBatch> findByInventoryItemIdAndQuantityGreaterThanOrderByExpiryDateAsc(
            Long inventoryItemId, java.math.BigDecimal quantity);

    List<InventoryBatch> findByExpiryDateBeforeAndQuantityGreaterThan(LocalDate date, java.math.BigDecimal quantity);
}
