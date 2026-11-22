package com.expiryguard.auth.dto;

public record AuthResponse(
        String token,
        UserDto user
) {}
