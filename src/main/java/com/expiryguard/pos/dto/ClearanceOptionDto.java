package com.expiryguard.pos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ClearanceOptionDto(
        Long batchId,
        String batchNumber,
        int discountPercentage,
        BigDecimal discountedPrice,
        BigDecimal originalPrice,
        LocalDate expiryDate,
        long daysUntilExpiry,
        int availableQuantity
) {}
