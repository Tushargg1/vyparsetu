package com.vyaparsetu.auth.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record PasskeyOptionsResponse(String ceremonyId, JsonNode options) {
}
