package com.expiryguard.markdown.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateMarkdownRuleRequest(
        @NotBlank(message = "Rule name is required")
        String ruleName,

        @Min(value = 1, message = "Days before expiry must be at least 1")
        int daysBeforeExpiry,

        @Min(value = 1, message = "Discount percentage must be at least 1%")
        @Max(value = 99, message = "Discount percentage cannot exceed 99%")
        int discountPercentage
) {}
