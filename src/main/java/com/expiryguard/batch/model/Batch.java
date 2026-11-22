package com.expiryguard.batch.model;

import com.expiryguard.product.model.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "batch_number", nullable = false, unique = true, length = 100)
    private String batchNumber;

    @Column(name = "initial_quantity", nullable = false)
    private int initialQuantity;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "cost_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "base_selling_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal baseSellingPrice;

    @Column(name = "current_selling_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentSellingPrice;

    @Builder.Default
    @Column(name = "discount_percentage", nullable = false)
    private int discountPercentage = 0;

    @Column(name = "manufacturing_date")
    private LocalDate manufacturingDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BatchStatus status = BatchStatus.ACTIVE;

    @Builder.Default
    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return this.expiryDate.isBefore(LocalDate.now()) || this.expiryDate.isEqual(LocalDate.now());
    }

    public void applyDiscount(int discountPercent) {
        this.discountPercentage = discountPercent;
        if (discountPercent <= 0) {
            this.currentSellingPrice = this.baseSellingPrice;
        } else {
            BigDecimal discountMultiplier = BigDecimal.valueOf(100 - discountPercent)
                    .divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
            this.currentSellingPrice = this.baseSellingPrice.multiply(discountMultiplier)
                    .setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }
}
