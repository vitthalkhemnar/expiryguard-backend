package com.expiryguard.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProductRequest(
        @NotBlank(message = "SKU cannot be blank")
        String sku,

        @NotBlank(message = "Barcode cannot be blank")
        String barcode,

        @NotBlank(message = "Product name cannot be blank")
        String name,

        String description,

        @NotNull(message = "Category ID is required")
        Long categoryId,

        @Min(value = 0, message = "Minimum reorder level must be >= 0")
        int minReorderLevel
) {}
