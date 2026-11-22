package com.expiryguard.auth.dto;

import com.expiryguard.auth.model.Role;

import java.time.LocalDateTime;

public record UserDto(
        Long id,
        String username,
        String fullName,
        Role role,
        boolean active,
        LocalDateTime createdAt
) {
    public UserDto(Long id, String username, String fullName, Role role) {
        this(id, username, fullName, role, true, null);
    }
}
