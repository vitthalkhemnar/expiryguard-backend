package com.expiryguard.pos.dto;

import com.expiryguard.pos.model.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CheckoutRequest(
        @NotEmpty(message = "Cart cannot be empty")
        List<@Valid CartItemRequest> items,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        String customerName,

        String customerPhone
) {
    public CheckoutRequest(List<@Valid CartItemRequest> items, PaymentMethod paymentMethod) {
        this(items, paymentMethod, null, null);
    }
}
