package com.vyaparsetu.whatsapp.repository;

import com.vyaparsetu.whatsapp.entity.ProductAlias;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductAliasRepository extends JpaRepository<ProductAlias, Long> {
    Optional<ProductAlias> findBySupplierIdAndAliasIgnoreCase(Long supplierId, String alias);

    List<ProductAlias> findBySupplierId(Long supplierId);

    boolean existsBySupplierIdAndAliasIgnoreCase(Long supplierId, String alias);
}
