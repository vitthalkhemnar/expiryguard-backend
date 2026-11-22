package com.expiryguard.markdown.controller;

import com.expiryguard.markdown.dto.CreateMarkdownRuleRequest;
import com.expiryguard.markdown.dto.MarkdownRuleDto;
import com.expiryguard.markdown.service.MarkdownService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/markdown-rules")
@RequiredArgsConstructor
@Tag(name = "Markdown Rules Engine", description = "Endpoints for configuring automated dynamic clearance pricing and manual sweeps")
public class MarkdownController {

    private final MarkdownService markdownService;

    @GetMapping
    @Operation(summary = "List all configured markdown rules")
    public ResponseEntity<List<MarkdownRuleDto>> getAllRules() {
        return ResponseEntity.ok(markdownService.getAllRules());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new dynamic markdown rule (Admin only)")
    public ResponseEntity<MarkdownRuleDto> createRule(@Valid @RequestBody CreateMarkdownRuleRequest request) {
        return new ResponseEntity<>(markdownService.createRule(request), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a markdown rule (Admin only)")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        markdownService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/trigger-sweep")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Manually trigger the markdown and expiry evaluation sweep (Admin only)")
    public ResponseEntity<MarkdownService.SweepResult> triggerSweep() {
        return ResponseEntity.ok(markdownService.executeMarkdownSweep());
    }
}
