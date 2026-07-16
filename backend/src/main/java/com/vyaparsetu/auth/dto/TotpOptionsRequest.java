package com.vyaparsetu.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TotpOptionsRequest(@NotBlank String identifier) {
}
