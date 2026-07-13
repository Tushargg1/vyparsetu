package com.vyaparsetu.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentConfirmRequest(
        @NotBlank String gatewayRef
) {
}
