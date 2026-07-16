package com.vyaparsetu.auth.dto;

public record TotpSetupResponse(String secret, String otpauthUri) {
}
