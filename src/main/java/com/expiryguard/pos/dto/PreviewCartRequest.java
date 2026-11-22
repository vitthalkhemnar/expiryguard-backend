package com.expiryguard.pos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PreviewCartRequest(
        @NotEmpty(message = "Cart cannot be empty")
        List<@Valid CartItemRequest> items
) {}
