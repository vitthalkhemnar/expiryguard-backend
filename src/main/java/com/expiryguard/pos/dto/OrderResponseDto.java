package com.expiryguard.pos.dto;

import com.expiryguard.pos.model.PaymentMethod;
import com.expiryguard.pos.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponseDto(
        Long id,
        String orderNumber,
        String cashierName,
        String customerName,
        String customerPhone,
        BigDecimal subtotal,
        BigDecimal totalDiscount,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        List<OrderItemDto> items,
        LocalDateTime createdAt
) {
    public record OrderItemDto(
            Long id,
            String productName,
            String productSku,
            String batchNumber,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal discountApplied,
            BigDecimal totalPrice
    ) {}
}
