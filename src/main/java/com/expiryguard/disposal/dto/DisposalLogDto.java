package com.expiryguard.disposal.dto;

import com.expiryguard.disposal.model.DisposalReason;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DisposalLogDto(
        Long id,
        Long batchId,
        String batchNumber,
        String productName,
        String approvedByName,
        int quantityDisposed,
        BigDecimal financialLossAmount,
        DisposalReason reason,
        String notes,
        LocalDateTime disposedAt
) {}
