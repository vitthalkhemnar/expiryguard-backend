package com.expiryguard.batch.dto;

import com.expiryguard.batch.model.BatchStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BatchDto(
        Long id,
        Long productId,
        String productName,
        String productSku,
        String batchNumber,
        int initialQuantity,
        int availableQuantity,
        BigDecimal costPrice,
        BigDecimal baseSellingPrice,
        BigDecimal currentSellingPrice,
        int discountPercentage,
        LocalDate manufacturingDate,
        LocalDate expiryDate,
        long daysUntilExpiry,
        BatchStatus status,
        LocalDateTime createdAt
) {}
