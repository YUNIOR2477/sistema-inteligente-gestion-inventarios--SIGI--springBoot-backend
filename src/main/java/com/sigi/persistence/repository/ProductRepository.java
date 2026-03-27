package com.sigi.persistence.repository;

import com.sigi.persistence.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findBySkuAndActiveTrue(String sku);

    boolean existsBySkuAndActiveTrue(String sku);

    boolean existsBySku(String sku);

    Page<Product> findByActiveTrue(Pageable pageable);

    Page<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable);

    Page<Product> findByCategoryContainingIgnoreCaseAndActiveTrue(String category, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :min AND :max")
    Page<Product> findByPriceRangeAndActiveTrue(BigDecimal min, BigDecimal max, Pageable pageable);

    Page<Product> findByActiveFalse(Pageable pageable);

    Page<Product> findByNameContainingIgnoreCaseAndActiveFalse(String name, Pageable pageable);

    Optional<Product> findByIdAndActiveFalse(UUID id);

    boolean existsByBarcode(String barcode);
}
