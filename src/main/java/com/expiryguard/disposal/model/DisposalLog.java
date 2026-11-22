package com.expiryguard.disposal.model;

import com.expiryguard.auth.model.User;
import com.expiryguard.batch.model.Batch;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "disposal_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisposalLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id", nullable = false)
    private User approvedBy;

    @Column(name = "quantity_disposed", nullable = false)
    private int quantityDisposed;

    @Column(name = "financial_loss_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal financialLossAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DisposalReason reason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Column(name = "disposed_at", nullable = false, updatable = false)
    private LocalDateTime disposedAt = LocalDateTime.now();
}
