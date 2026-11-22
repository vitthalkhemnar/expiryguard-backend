package com.expiryguard.analytics.dto;

import com.expiryguard.batch.model.BatchStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UrgentBatchDto(
        Long batchId,
        String batchNumber,
        String productName,
        int availableQuantity,
        BigDecimal currentSellingPrice,
        int discountPercentage,
        LocalDate expiryDate,
        long daysUntilExpiry,
        BatchStatus status
) {}
