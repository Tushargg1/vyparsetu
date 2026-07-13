package com.vyaparsetu.catalog.repository;

import com.vyaparsetu.catalog.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByUuid(String uuid);

    Optional<Product> findByBarcodeAndActiveTrue(String barcode);

    Optional<Product> findByBarcodeAndSupplierIdAndActiveTrue(String barcode, Long supplierId);

    Page<Product> findBySupplierIdAndActiveTrue(Long supplierId, Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            WHERE p.active = true
              AND (:q IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:categoryId IS NULL OR p.categoryId = :categoryId)
              AND (:supplierId IS NULL OR p.supplierId = :supplierId)
            """)
    Page<Product> search(@Param("q") String q,
                         @Param("categoryId") Long categoryId,
                         @Param("supplierId") Long supplierId,
                         Pageable pageable);

    List<Product> findByIdInAndActiveTrue(List<Long> ids);
}
