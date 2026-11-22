package com.expiryguard.product.controller;

import com.expiryguard.product.dto.CategoryDto;
import com.expiryguard.product.dto.CreateProductRequest;
import com.expiryguard.product.dto.ProductDto;
import com.expiryguard.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Catalog", description = "Endpoints for managing master catalog items and categories")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Search products with pagination and category filter")
    public ResponseEntity<Page<ProductDto>> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(productService.getProducts(search, categoryId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/barcode/{barcode}")
    @Operation(summary = "Find product by barcode for POS scanner integration")
    public ResponseEntity<ProductDto> getProductByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(productService.getProductByBarcode(barcode));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new product in master catalog (Admin only)")
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return new ResponseEntity<>(productService.createProduct(request), HttpStatus.CREATED);
    }

    @GetMapping("/categories")
    @Operation(summary = "List all product categories")
    public ResponseEntity<List<CategoryDto>> getCategories() {
        return ResponseEntity.ok(productService.getAllCategories());
    }
}
