package com.vyaparsetu.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthExchangeRequest(@NotBlank String code) {
}