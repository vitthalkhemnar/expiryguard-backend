package com.expiryguard.pos.controller;

import com.expiryguard.pos.dto.CartPreviewResponse;
import com.expiryguard.pos.dto.CheckoutRequest;
import com.expiryguard.pos.dto.OrderResponseDto;
import com.expiryguard.pos.dto.PreviewCartRequest;
import com.expiryguard.pos.service.PosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pos")
@RequiredArgsConstructor
@Tag(name = "Point of Sale (POS)", description = "Endpoints for previewing FEFO allocation, calculating clearance discounts, and processing checkouts")
public class PosController {

    private final PosService posService;

    @PostMapping("/preview-cart")
    @Operation(summary = "Preview cart items with dynamic FEFO batch allocations and calculated clearance discounts")
    public ResponseEntity<CartPreviewResponse> previewCart(@Valid @RequestBody PreviewCartRequest request) {
        return ResponseEntity.ok(posService.previewCart(request));
    }

    @PostMapping("/checkout")
    @Operation(summary = "Execute atomic checkout, deduct batch inventory, enforce expiry guardrail, and generate receipt")
    public ResponseEntity<OrderResponseDto> checkout(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CheckoutRequest request
    ) {
        return ResponseEntity.ok(posService.checkout(userDetails.getUsername(), request));
    }

    @GetMapping("/orders")
    @Operation(summary = "List past orders with pagination and optional search by order number, customer name, or phone")
    public ResponseEntity<Page<OrderResponseDto>> getOrders(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(posService.getOrders(search, pageable));
    }

    @GetMapping("/orders/{orderNumber}")
    @Operation(summary = "Get specific order receipt details by order number")
    public ResponseEntity<OrderResponseDto> getOrderByNumber(@PathVariable String orderNumber) {
        return ResponseEntity.ok(posService.getOrderByNumber(orderNumber));
    }
}
