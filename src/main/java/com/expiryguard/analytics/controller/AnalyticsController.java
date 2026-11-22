package com.expiryguard.analytics.controller;

import com.expiryguard.analytics.dto.AnalyticsSummaryDto;
import com.expiryguard.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics & Dashboard", description = "Endpoints for executive KPIs, expiry risk exposure, and waste metrics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get high-level dashboard metrics, inventory values, and urgent expiring lots (Admin only)")
    public ResponseEntity<AnalyticsSummaryDto> getDashboardSummary() {
        return ResponseEntity.ok(analyticsService.getDashboardSummary());
    }
}
