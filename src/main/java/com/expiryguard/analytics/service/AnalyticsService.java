package com.expiryguard.analytics.service;

import com.expiryguard.analytics.dto.AnalyticsSummaryDto;
import com.expiryguard.analytics.dto.UrgentBatchDto;
import com.expiryguard.batch.model.Batch;
import com.expiryguard.batch.model.BatchStatus;
import com.expiryguard.batch.repository.BatchRepository;
import com.expiryguard.disposal.repository.DisposalRepository;
import com.expiryguard.pos.repository.OrderRepository;
import com.expiryguard.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final BatchRepository batchRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final DisposalRepository disposalRepository;

    @Transactional(readOnly = true)
    public AnalyticsSummaryDto getDashboardSummary() {
        LocalDate today = LocalDate.now();
        List<Batch> allBatches = batchRepository.findAll();

        BigDecimal totalInventoryValue = BigDecimal.ZERO;
        BigDecimal atRiskValue = BigDecimal.ZERO;

        Map<String, Long> statusBreakdown = new HashMap<>();
        for (BatchStatus status : BatchStatus.values()) {
            statusBreakdown.put(status.name(), 0L);
        }

        List<UrgentBatchDto> urgentCandidates = new ArrayList<>();

        for (Batch batch : allBatches) {
            statusBreakdown.put(batch.getStatus().name(), statusBreakdown.get(batch.getStatus().name()) + 1);

            if (batch.getStatus() != BatchStatus.DISPOSED && batch.getAvailableQuantity() > 0) {
                BigDecimal batchValue = batch.getCurrentSellingPrice().multiply(BigDecimal.valueOf(batch.getAvailableQuantity()));
                totalInventoryValue = totalInventoryValue.add(batchValue);

                long daysUntil = ChronoUnit.DAYS.between(today, batch.getExpiryDate());
                if (daysUntil <= 30 && daysUntil > 0) {
                    atRiskValue = atRiskValue.add(batchValue);
                }

                if (batch.getStatus() != BatchStatus.EXPIRED) {
                    urgentCandidates.add(new UrgentBatchDto(
                            batch.getId(),
                            batch.getBatchNumber(),
                            batch.getProduct().getName(),
                            batch.getAvailableQuantity(),
                            batch.getCurrentSellingPrice(),
                            batch.getDiscountPercentage(),
                            batch.getExpiryDate(),
                            daysUntil,
                            batch.getStatus()
                    ));
                }
            }
        }

        // Sort urgent batches by days until expiry ascending
        urgentCandidates.sort(Comparator.comparingLong(UrgentBatchDto::daysUntilExpiry));
        List<UrgentBatchDto> top5Urgent = urgentCandidates.stream().limit(5).toList();

        BigDecimal totalSalesRevenue = orderRepository.sumTotalSalesRevenue();
        BigDecimal totalDiscountsGiven = orderRepository.sumTotalDiscountsGiven();
        BigDecimal totalDisposalLoss = disposalRepository.sumTotalFinancialLoss();
        long totalProducts = productRepository.count();
        long activeBatchesCount = batchRepository.countByStatus(BatchStatus.ACTIVE)
                + batchRepository.countByStatus(BatchStatus.NEAR_EXPIRY)
                + batchRepository.countByStatus(BatchStatus.CRITICAL);

        return new AnalyticsSummaryDto(
                totalInventoryValue,
                atRiskValue,
                totalSalesRevenue,
                totalDiscountsGiven,
                totalDisposalLoss,
                totalProducts,
                activeBatchesCount,
                statusBreakdown,
                top5Urgent
        );
    }
}
