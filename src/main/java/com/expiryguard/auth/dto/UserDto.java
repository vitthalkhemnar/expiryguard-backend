package com.expiryguard.auth.dto;

import com.expiryguard.auth.model.Role;

public record UserDto(
        Long id,
        String username,
        String fullName,
        Role role
) {}
