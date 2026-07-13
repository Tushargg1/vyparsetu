package com.vyaparsetu.ai.repository;

import com.vyaparsetu.inventory.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface DemandRepository extends JpaRepository<InventoryItem, Long> {

    interface DemandRow {
        Long getProductId();

        java.math.BigDecimal getSold();

        java.math.BigDecimal getCurrentQty();
    }

    @Query(value = """
            SELECT ii.product_id AS productId,
                   COALESCE(SUM(CASE WHEN sm.movement_type = 'SALE' AND sm.created_at >= :since
                                     THEN -sm.quantity_delta ELSE 0 END), 0) AS sold,
                   ii.quantity AS currentQty
            FROM inventory_items ii
            LEFT JOIN stock_movements sm ON sm.inventory_item_id = ii.id
            WHERE ii.retailer_id = :rid
            GROUP BY ii.product_id, ii.quantity
            """, nativeQuery = true)
    List<DemandRow> demand(@Param("rid") Long retailerId, @Param("since") Instant since);
}
