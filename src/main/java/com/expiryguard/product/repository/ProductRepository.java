package com.expiryguard.product.repository;

import com.expiryguard.product.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByBarcode(String barcode);

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsByBarcode(String barcode);

    @Query("SELECT p FROM Product p " +
           "WHERE (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:searchPattern IS NULL OR LOWER(p.name) LIKE :searchPattern " +
           "OR LOWER(p.sku) LIKE :searchPattern " +
           "OR LOWER(p.barcode) LIKE :searchPattern)")
    Page<Product> searchProducts(@Param("searchPattern") String searchPattern, 
                                 @Param("categoryId") Long categoryId, 
                                 Pageable pageable);
}
