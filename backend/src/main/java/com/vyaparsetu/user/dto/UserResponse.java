package com.vyaparsetu.user.dto;

import com.vyaparsetu.common.enums.Enums;
import com.vyaparsetu.user.entity.Role;
import com.vyaparsetu.user.entity.User;

import java.util.Set;
import java.util.stream.Collectors;

public record UserResponse(
        String uuid,
        String name,
        String phone,
        String email,
        Enums.Language preferredLanguage,
        Enums.UserStatus status,
        Set<String> roles
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUuid(),
                user.getName(),
                user.getPhone(),
                user.getEmail(),
                user.getPreferredLanguage(),
                user.getStatus(),
                user.getRoles().stream().map(Role::getName).map(Enum::name).collect(Collectors.toSet())
        );
    }
}
