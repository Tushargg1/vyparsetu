package com.vyaparsetu.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TextOrderRequest(
        @NotNull Long supplierId,
        @NotBlank String text
) {
}
