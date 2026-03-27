package com.sigi.persistence.repository;

import com.sigi.persistence.entity.Inventory;
import com.sigi.persistence.entity.Product;
import com.sigi.persistence.entity.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {

    Page<Inventory> findByWarehouseNameContainingIgnoreCaseAndActiveTrue(String warehouseName, Pageable pageable);

    Page<Inventory> findByProductIdAndActiveTrue(UUID productId, Pageable pageable);

    @Query("SELECT i FROM Inventory i WHERE i.availableQuantity < :threshold")
    Page<Inventory> findLowStockAndActiveTrue(BigDecimal threshold, Pageable pageable);

    @Query("SELECT i FROM Inventory i WHERE i.warehouse.id = :warehouseId AND i.availableQuantity > 0 AND i.active = true")
    Page<Inventory> findAvailableByWarehouseId(UUID warehouseId, Pageable pageable);

    boolean existsByProductIdAndWarehouseIdAndLotAndActiveTrue(UUID productId, UUID warehouseId, String lot);

    boolean existsByProductIdAndActiveTrue(UUID id);

    boolean existsByWarehouseIdAndActiveTrue(UUID id);

    @Query("SELECT COALESCE(SUM(i.availableQuantity + i.reservedQuantity), 0) " +
            "FROM Inventory i " +
            "WHERE i.warehouse.id = :warehouseId AND i.active = true")
    Optional<BigDecimal> sumUsedCapacityByWarehouseId(UUID warehouseId);

    Page<Inventory> findByActiveFalse(Pageable pageable);

    Page<Inventory> findByActiveTrue(Pageable pageable);

    Page<Inventory> findByProductNameContainingIgnoreCaseAndActiveFalse(String productName, Pageable pageable);

    Page<Inventory> findByProductNameContainingIgnoreCaseAndActiveTrue(String productName, Pageable pageable);

    Page<Inventory> findByProductSkuContainingIgnoreCaseAndActiveTrueAndIdNot(
            String searchValue,
            UUID excludedId,
            Pageable pageable
    );

    Optional<Inventory> findByIdAndActiveFalse(UUID id);

    boolean existsByProductAndWarehouseAndLot(Product p, Warehouse w, String lot);
}
