package com.vyaparsetu.inventory.dto;

import com.vyaparsetu.inventory.entity.InventoryItem;

import java.math.BigDecimal;

public record InventoryItemResponse(
        Long id,
        Long productId,
        BigDecimal quantity,
        BigDecimal reorderLevel,
        BigDecimal costPrice,
        boolean lowStock
) {
    public static InventoryItemResponse from(InventoryItem i) {
        boolean low = i.getQuantity().compareTo(i.getReorderLevel()) <= 0;
        return new InventoryItemResponse(
                i.getId(), i.getProductId(), i.getQuantity(),
                i.getReorderLevel(), i.getCostPrice(), low);
    }
}
