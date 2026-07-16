package com.vyaparsetu.auth.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PasskeyFinishRequest(
        @NotBlank String ceremonyId,
        @NotNull JsonNode credential,
        String name
) {
}
