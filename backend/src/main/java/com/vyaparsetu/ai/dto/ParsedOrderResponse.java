package com.vyaparsetu.ai.dto;

import java.math.BigDecimal;
import java.util.List;

public record ParsedOrderResponse(
        List<ParsedLine> matched,
        List<String> unmatched
) {
    public record ParsedLine(
            Long productId,
            String productName,
            BigDecimal quantity,
            double confidence
    ) {
    }
}
