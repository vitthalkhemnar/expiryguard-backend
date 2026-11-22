package com.expiryguard.markdown.service;

import com.expiryguard.batch.model.Batch;
import com.expiryguard.batch.model.BatchStatus;
import com.expiryguard.batch.repository.BatchRepository;
import com.expiryguard.common.exception.BusinessException;
import com.expiryguard.common.exception.ResourceNotFoundException;
import com.expiryguard.markdown.dto.CreateMarkdownRuleRequest;
import com.expiryguard.markdown.dto.MarkdownRuleDto;
import com.expiryguard.markdown.model.MarkdownRule;
import com.expiryguard.markdown.repository.MarkdownRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarkdownService {

    private final BatchRepository batchRepository;
    private final MarkdownRuleRepository markdownRuleRepository;

    public record SweepResult(int totalEvaluated, int discounted, int expired) {}

    @Scheduled(cron = "${expiryguard.markdown.cron:0 0 2 * * ?}")
    @Transactional
    public void scheduledMarkdownSweep() {
        log.info("Starting scheduled markdown and expiry evaluation sweep...");
        SweepResult result = executeMarkdownSweep();
        log.info("Completed markdown sweep. Evaluated: {}, Discounted: {}, Expired: {}",
                result.totalEvaluated(), result.discounted(), result.expired());
    }

    @Transactional
    public SweepResult executeMarkdownSweep() {
        LocalDate today = LocalDate.now();
        List<Batch> activeBatches = batchRepository.findActiveInventoryBatches();
        List<MarkdownRule> rules = markdownRuleRepository.findByActiveTrueOrderByDaysBeforeExpiryAsc();

        int discountedCount = 0;
        int expiredCount = 0;

        for (Batch batch : activeBatches) {
            long daysUntil = ChronoUnit.DAYS.between(today, batch.getExpiryDate());

            if (daysUntil <= 0) {
                // Batch is expired -> hard lock
                batch.setStatus(BatchStatus.EXPIRED);
                batch.setDiscountPercentage(0);
                expiredCount++;
            } else {
                // Check if any rule matches (rule with smallest daysBeforeExpiry >= daysUntil)
                MarkdownRule matchedRule = null;
                for (MarkdownRule rule : rules) {
                    if (daysUntil <= rule.getDaysBeforeExpiry()) {
                        matchedRule = rule;
                        break;
                    }
                }

                if (matchedRule != null) {
                    batch.applyDiscount(matchedRule.getDiscountPercentage());
                    batch.setStatus(daysUntil <= 7 ? BatchStatus.CRITICAL : BatchStatus.NEAR_EXPIRY);
                    discountedCount++;
                } else {
                    batch.applyDiscount(0);
                    batch.setStatus(BatchStatus.ACTIVE);
                }
            }
        }

        batchRepository.saveAll(activeBatches);
        return new SweepResult(activeBatches.size(), discountedCount, expiredCount);
    }

    @Transactional(readOnly = true)
    public List<MarkdownRuleDto> getAllRules() {
        return markdownRuleRepository.findAll().stream()
                .map(this::mapToDto)
                .toList();
    }

    @Transactional
    public MarkdownRuleDto createRule(CreateMarkdownRuleRequest request) {
        if (markdownRuleRepository.existsByDaysBeforeExpiry(request.daysBeforeExpiry())) {
            throw new BusinessException("A rule for " + request.daysBeforeExpiry() + " days before expiry already exists");
        }

        MarkdownRule rule = MarkdownRule.builder()
                .ruleName(request.ruleName())
                .daysBeforeExpiry(request.daysBeforeExpiry())
                .discountPercentage(request.discountPercentage())
                .active(true)
                .build();

        return mapToDto(markdownRuleRepository.save(rule));
    }

    @Transactional
    public void deleteRule(Long id) {
        if (!markdownRuleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Rule not found with id: " + id);
        }
        markdownRuleRepository.deleteById(id);
    }

    private MarkdownRuleDto mapToDto(MarkdownRule rule) {
        return new MarkdownRuleDto(
                rule.getId(),
                rule.getRuleName(),
                rule.getDaysBeforeExpiry(),
                rule.getDiscountPercentage(),
                rule.isActive(),
                rule.getCreatedAt()
        );
    }
}
