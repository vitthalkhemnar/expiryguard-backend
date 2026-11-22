package com.expiryguard.pos.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemRequest(
        @NotNull(message = "Product ID is required")
        Long productId,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity,

        Boolean preferExpiringDiscount, // backward compatibility

        Long selectedBatchId // optional explicit clearance batch selected by user
) {
    public CartItemRequest(Long productId, int quantity, Boolean preferExpiringDiscount) {
        this(productId, quantity, preferExpiringDiscount, null);
    }

    public CartItemRequest(Long productId, int quantity) {
        this(productId, quantity, false, null);
    }

    public boolean isPreferExpiringDiscount() {
        if (selectedBatchId != null) {
            return true;
        }
        return preferExpiringDiscount != null && preferExpiringDiscount;
    }
}
