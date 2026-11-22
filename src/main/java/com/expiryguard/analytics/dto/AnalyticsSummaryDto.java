package com.expiryguard.analytics.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AnalyticsSummaryDto(
        BigDecimal totalInventoryValue,
        BigDecimal atRiskInventoryValue,
        BigDecimal totalSalesRevenue,
        BigDecimal totalDiscountsGiven,
        BigDecimal totalDisposalLoss,
        long totalProductsCount,
        long totalActiveBatchesCount,
        Map<String, Long> batchStatusBreakdown,
        List<UrgentBatchDto> topUrgentBatches
) {}
