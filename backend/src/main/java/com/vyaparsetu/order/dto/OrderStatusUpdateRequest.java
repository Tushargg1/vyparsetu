package com.vyaparsetu.order.dto;

import com.vyaparsetu.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(
        @NotNull OrderStatus status,
        String note
) {
}
