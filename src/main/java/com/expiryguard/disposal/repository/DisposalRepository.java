package com.expiryguard.disposal.repository;

import com.expiryguard.disposal.model.DisposalLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface DisposalRepository extends JpaRepository<DisposalLog, Long> {
    Page<DisposalLog> findAllByOrderByDisposedAtDesc(Pageable pageable);

    @Query("SELECT COALESCE(SUM(d.financialLossAmount), 0) FROM DisposalLog d")
    BigDecimal sumTotalFinancialLoss();

    @Query("SELECT COALESCE(SUM(d.quantityDisposed), 0) FROM DisposalLog d")
    long sumTotalQuantityDisposed();
}
