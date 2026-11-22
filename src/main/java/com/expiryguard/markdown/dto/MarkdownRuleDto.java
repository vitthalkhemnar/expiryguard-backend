package com.expiryguard.markdown.dto;

import java.time.LocalDateTime;

public record MarkdownRuleDto(
        Long id,
        String ruleName,
        int daysBeforeExpiry,
        int discountPercentage,
        boolean active,
        LocalDateTime createdAt
) {}
