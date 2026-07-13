package com.vyaparsetu.auth.dto;

import com.vyaparsetu.common.enums.Enums;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SendOtpRequest(
        @NotBlank String identifier,
        @NotNull Enums.OtpChannel channel,
        @NotNull Enums.OtpPurpose purpose
) {
}
