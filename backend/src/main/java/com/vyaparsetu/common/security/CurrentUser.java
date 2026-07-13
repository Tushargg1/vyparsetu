package com.vyaparsetu.common.security;

import com.vyaparsetu.common.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Helper to access the authenticated principal in the service layer.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static AppPrincipal get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppPrincipal principal)) {
            throw new UnauthorizedException("Not authenticated");
        }
        return principal;
    }

    public static Long id() {
        return get().userId();
    }

    public static class UnauthorizedException extends BaseException {
        public UnauthorizedException(String message) {
            super("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, message);
        }
    }
}
