package com.vyaparsetu.order.dto;

import com.vyaparsetu.common.enums.Enums;
import jakarta.validation.constraints.NotNull;

public record PlaceOrderRequest(
        @NotNull Long supplierId,
        @NotNull Enums.PaymentMode paymentMode,
        Long deliveryAddressId,
        Enums.OrderSource orderSource
) {
}
