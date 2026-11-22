package com.expiryguard.disposal.dto;

import com.expiryguard.disposal.model.DisposalReason;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateDisposalRequest(
        @NotNull(message = "Batch ID is required")
        Long batchId,

        @Min(value = 1, message = "Quantity disposed must be at least 1")
        int quantity,

        @NotNull(message = "Disposal reason is required")
        DisposalReason reason,

        String notes
) {}
