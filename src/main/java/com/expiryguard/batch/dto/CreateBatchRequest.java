package com.expiryguard.batch.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateBatchRequest(
        @NotNull(message = "Product ID is required")
        Long productId,

        @NotBlank(message = "Batch number cannot be blank")
        String batchNumber,

        @Min(value = 1, message = "Initial quantity must be at least 1")
        int initialQuantity,

        @NotNull(message = "Cost price is required")
        @DecimalMin(value = "0.01", message = "Cost price must be > 0")
        BigDecimal costPrice,

        @NotNull(message = "Base selling price is required")
        @DecimalMin(value = "0.01", message = "Base selling price must be > 0")
        BigDecimal baseSellingPrice,

        LocalDate manufacturingDate,

        @NotNull(message = "Expiry date is required")
        @Future(message = "Expiry date must be in the future")
        LocalDate expiryDate
) {}
