package com.vyaparsetu.auth.dto;

import com.vyaparsetu.user.dto.UserResponse;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        UserResponse user
) {
}
