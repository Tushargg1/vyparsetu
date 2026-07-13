package com.vyaparsetu.catalog.dto;

import com.vyaparsetu.catalog.entity.Product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String uuid,
        Long supplierId,
        Long categoryId,
        String name,
        String brand,
        String barcode,
        String sku,
        String unit,
        String packSize,
        BigDecimal mrp,
        BigDecimal sellingPrice,
        BigDecimal gstRate,
        String hsnCode,
        String imageUrl,
        boolean active,
        BigDecimal stockQty,
        BigDecimal reservedQty,
        BigDecimal availableQty,
        BigDecimal lowStockThreshold,
        boolean trackStock,
        boolean lowStock
) {
    public static ProductResponse from(Product p) {
        boolean low = p.isTrackStock()
                && p.getAvailableQty().compareTo(p.getLowStockThreshold()) <= 0;
        return new ProductResponse(
                p.getId(), p.getUuid(), p.getSupplierId(), p.getCategoryId(),
                p.getName(), p.getBrand(), p.getBarcode(), p.getSku(), p.getUnit(),
                p.getPackSize(), p.getMrp(), p.getSellingPrice(), p.getGstRate(),
                p.getHsnCode(), p.getImageUrl(), p.isActive(),
                p.getStockQty(), p.getReservedQty(), p.getAvailableQty(),
                p.getLowStockThreshold(), p.isTrackStock(), low
        );
    }
}
