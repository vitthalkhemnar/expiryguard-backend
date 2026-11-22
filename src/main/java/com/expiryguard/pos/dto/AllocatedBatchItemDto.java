package com.expiryguard.pos.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AllocatedBatchItemDto(
        Long batchId,
        String batchNumber,
        LocalDate expiryDate,
        long daysUntilExpiry,
        int allocatedQuantity,
        BigDecimal basePrice,
        BigDecimal currentPrice,
        int discountPercentage,
        BigDecimal lineDiscount,
        BigDecimal lineTotal
) {}
