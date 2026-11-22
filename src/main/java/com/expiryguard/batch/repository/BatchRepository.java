package com.expiryguard.batch.repository;

import com.expiryguard.batch.model.Batch;
import com.expiryguard.batch.model.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {

    Optional<Batch> findByBatchNumber(String batchNumber);

    boolean existsByBatchNumber(String batchNumber);

    List<Batch> findByProductId(Long productId);

    /**
     * Core FEFO (First-Expired, First-Out) query:
     * Retrieves all valid, unexpired batches for a product that have stock available,
     * sorted strictly by expiry_date ASC.
     */
    @Query("SELECT b FROM Batch b " +
           "WHERE b.product.id = :productId " +
           "  AND b.availableQuantity > 0 " +
           "  AND b.expiryDate > :today " +
           "  AND b.status != com.expiryguard.batch.model.BatchStatus.EXPIRED " +
           "  AND b.status != com.expiryguard.batch.model.BatchStatus.DISPOSED " +
           "ORDER BY b.expiryDate ASC, b.id ASC")
    List<Batch> findAvailableBatchesForFefo(@Param("productId") Long productId, @Param("today") LocalDate today);

    /**
     * Opt-Out Query:
     * Retrieves valid, unexpired batches sorted strictly by expiry_date DESC (freshest first)
     * for customers who opt out of discounted clearance stock.
     */
    @Query("SELECT b FROM Batch b " +
           "WHERE b.product.id = :productId " +
           "  AND b.availableQuantity > 0 " +
           "  AND b.expiryDate > :today " +
           "  AND b.status != com.expiryguard.batch.model.BatchStatus.EXPIRED " +
           "  AND b.status != com.expiryguard.batch.model.BatchStatus.DISPOSED " +
           "ORDER BY b.expiryDate DESC, b.id ASC")
    List<Batch> findFreshBatchesForProduct(@Param("productId") Long productId, @Param("today") LocalDate today);

    /**
     * Finds all unexpired batches for a product that currently have clearance discounts (> 0%),
     * sorted by highest discount percentage first, then earliest expiry date.
     */
    @Query("SELECT b FROM Batch b " +
           "WHERE b.product.id = :productId " +
           "  AND b.availableQuantity > 0 " +
           "  AND b.expiryDate > :today " +
           "  AND b.discountPercentage > 0 " +
           "  AND b.status != com.expiryguard.batch.model.BatchStatus.EXPIRED " +
           "  AND b.status != com.expiryguard.batch.model.BatchStatus.DISPOSED " +
           "ORDER BY b.discountPercentage DESC, b.expiryDate ASC")
    List<Batch> findDiscountedBatchesForProduct(@Param("productId") Long productId, @Param("today") LocalDate today);

    /**
     * Finds active batches that need to be evaluated by the markdown scheduler.
     */
    @Query("SELECT b FROM Batch b " +
           "WHERE b.status != com.expiryguard.batch.model.BatchStatus.DISPOSED " +
           "  AND b.availableQuantity > 0")
    List<Batch> findActiveInventoryBatches();

    /**
     * Finds batches expiring on or before a given date.
     */
    List<Batch> findByExpiryDateLessThanEqualAndStatusNot(LocalDate date, BatchStatus status);

    /**
     * Aggregates total available stock across all active batches for a product.
     */
    @Query("SELECT COALESCE(SUM(b.availableQuantity), 0) FROM Batch b " +
           "WHERE b.product.id = :productId " +
           "  AND b.expiryDate > :today " +
           "  AND b.status != com.expiryguard.batch.model.BatchStatus.EXPIRED " +
           "  AND b.status != com.expiryguard.batch.model.BatchStatus.DISPOSED")
    int sumAvailableQuantityByProductId(@Param("productId") Long productId, @Param("today") LocalDate today);

    /**
     * Count batches by status.
     */
    long countByStatus(BatchStatus status);
}
