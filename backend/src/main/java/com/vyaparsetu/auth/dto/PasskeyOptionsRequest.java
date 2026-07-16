package com.vyaparsetu.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record PasskeyOptionsRequest(@NotBlank String identifier) {
}
