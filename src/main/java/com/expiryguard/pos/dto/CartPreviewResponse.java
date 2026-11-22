package com.expiryguard.pos.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartPreviewResponse(
        List<CartPreviewItemDto> items,
        BigDecimal subtotal,
        BigDecimal totalDiscount,
        BigDecimal totalPayable
) {}
