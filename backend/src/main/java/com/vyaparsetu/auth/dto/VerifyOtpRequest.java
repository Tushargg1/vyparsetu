package com.vyaparsetu.auth.dto;

import com.vyaparsetu.common.enums.Enums;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VerifyOtpRequest(
        @NotBlank String identifier,
        @NotBlank String code,
        @NotNull Enums.OtpPurpose purpose
) {
}
