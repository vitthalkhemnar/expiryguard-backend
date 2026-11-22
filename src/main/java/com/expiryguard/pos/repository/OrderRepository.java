package com.expiryguard.pos.repository;

import com.expiryguard.pos.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.repository.query.Param;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
    boolean existsByCashierId(Long cashierId);

    @Query("SELECT o FROM Order o WHERE " +
           "LOWER(o.orderNumber) LIKE :searchPattern " +
           "OR LOWER(o.customerName) LIKE :searchPattern " +
           "OR o.customerPhone LIKE :searchPattern " +
           "ORDER BY o.createdAt DESC")
    Page<Order> searchOrders(@Param("searchPattern") String searchPattern, Pageable pageable);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o")
    BigDecimal sumTotalSalesRevenue();

    @Query("SELECT COALESCE(SUM(o.totalDiscount), 0) FROM Order o")
    BigDecimal sumTotalDiscountsGiven();
}
