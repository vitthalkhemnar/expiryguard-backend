package com.expiryguard.product.dto;

import java.time.LocalDateTime;

public record ProductDto(
        Long id,
        String sku,
        String barcode,
        String name,
        String description,
        CategoryDto category,
        int minReorderLevel,
        boolean active,
        LocalDateTime createdAt,
        int totalAvailableStock
) {}
