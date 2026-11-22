package com.expiryguard.batch.controller;

import com.expiryguard.batch.dto.BatchDto;
import com.expiryguard.batch.dto.CreateBatchRequest;
import com.expiryguard.batch.service.BatchService;
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
@RequestMapping("/api/v1/batches")
@RequiredArgsConstructor
@Tag(name = "Batch & Lot Management", description = "Endpoints for registering and tracking inventory batches and expiry dates")
public class BatchController {

    private final BatchService batchService;

    @GetMapping
    @Operation(summary = "Get all inventory batches")
    public ResponseEntity<List<BatchDto>> getAllBatches() {
        return ResponseEntity.ok(batchService.getAllBatches());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get batch by ID")
    public ResponseEntity<BatchDto> getBatchById(@PathVariable Long id) {
        return ResponseEntity.ok(batchService.getBatchById(id));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get all batches for a specific product")
    public ResponseEntity<List<BatchDto>> getBatchesByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(batchService.getBatchesByProduct(productId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register a new shipment lot / batch (Admin only)")
    public ResponseEntity<BatchDto> createBatch(@Valid @RequestBody CreateBatchRequest request) {
        return new ResponseEntity<>(batchService.createBatch(request), HttpStatus.CREATED);
    }
}
