package com.vyaparsetu.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TotpLoginRequest(
        @NotBlank String challengeToken,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "Enter a valid 6-digit code") String code
) {
}
