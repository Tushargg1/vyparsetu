package com.vyaparsetu.auth.dto;

import com.vyaparsetu.user.dto.UserResponse;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        UserResponse user,
        String nextStep,
        String challengeToken
) {
    public static AuthTokenResponse authenticated(
            String accessToken, String refreshToken, long expiresInSeconds, UserResponse user) {
        return new AuthTokenResponse(accessToken, refreshToken, expiresInSeconds, user, "AUTHENTICATED", null);
    }
}
