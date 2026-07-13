package com.vyaparsetu.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long cartId,
        Long supplierId,
        List<Line> items,
        BigDecimal estimatedTotal
) {
    public record Line(
            Long cartItemId,
            Long productId,
            String productName,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {
    }
}
