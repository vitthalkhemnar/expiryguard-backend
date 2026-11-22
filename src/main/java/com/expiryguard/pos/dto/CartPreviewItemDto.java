package com.expiryguard.pos.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartPreviewItemDto(
        Long productId,
        String productName,
        String barcode,
        String sku,
        int requestedQuantity,
        Long selectedBatchId,
        boolean preferExpiringDiscount,
        List<ClearanceOptionDto> availableDiscounts,
        List<AllocatedBatchItemDto> allocatedBatches,
        BigDecimal itemSubtotal,
        BigDecimal itemDiscount,
        BigDecimal itemTotal
) {}
