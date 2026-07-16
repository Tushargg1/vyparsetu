package com.vyaparsetu.common.security;

import java.time.Instant;
import java.util.Set;

/** Authenticated principal stored in the SecurityContext. */
public record AppPrincipal(
        Long userId,
        String uuid,
        String phone,
        Set<String> roles,
        Instant authenticatedAt,
        String authenticationMethod
) {
}
