package com.vyaparsetu.ai.service;

import com.vyaparsetu.inventory.dto.InventoryItemResponse;
import com.vyaparsetu.inventory.service.InventoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Smart reorder recommendations. V1 uses a simple reorder-level heuristic;
 * future versions incorporate demand forecasting and seasonality.
 */
@Service
public class RecommendationService {

    private final InventoryService inventoryService;

    public RecommendationService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    public record Recommendation(Long productId, BigDecimal currentQty,
                                 BigDecimal reorderLevel, BigDecimal suggestedQty, String reason) {
    }

    @Transactional(readOnly = true)
    public List<Recommendation> smartReorder() {
        return inventoryService.myLowStock().stream()
                .map(this::toRecommendation)
                .toList();
    }

    private Recommendation toRecommendation(InventoryItemResponse item) {
        // suggest topping up to twice the reorder level (simple heuristic)
        BigDecimal target = item.reorderLevel().multiply(BigDecimal.valueOf(2));
        BigDecimal suggested = target.subtract(item.quantity()).max(BigDecimal.ONE);
        return new Recommendation(item.productId(), item.quantity(), item.reorderLevel(),
                suggested, "Stock at or below reorder level");
    }
}
