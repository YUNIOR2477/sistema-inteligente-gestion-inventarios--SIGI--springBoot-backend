package com.sigi.persistence.repository;

import com.sigi.persistence.entity.Inventory;
import com.sigi.persistence.entity.Movement;
import com.sigi.persistence.entity.Order;
import com.sigi.persistence.entity.Product;
import com.sigi.persistence.enums.MovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MovementRepository extends JpaRepository<Movement, UUID> {
    Page<Movement> findByProductNameContainingIgnoreCaseAndActiveTrue(String productName, Pageable pageable);

    Page<Movement> findByOrderClientNameContainingIgnoreCaseAndActiveTrue(String clientName, Pageable pageable);

    Page<Movement> findByDispatcherNameContainingIgnoreCaseAndActiveTrue(String dispatcherName, Pageable pageable);

    Page<Movement> findByTypeAndActiveTrue(MovementType type, Pageable pageable);

    boolean existsByDispatcherIdAndActiveTrue(UUID id);

    Page<Movement> findByActiveTrue(Pageable pageable);

    Page<Movement> findByActiveFalse(Pageable pageable);

    Page<Movement> findByTypeAndActiveFalse(MovementType type, Pageable pageable);

    Optional<Movement> findByIdAndActiveFalse(UUID id);

    boolean existsByInventoryAndProductAndOrderAndTypeAndQuantity(Inventory inv, Product p, Order order, MovementType type, BigDecimal qty);
}
