package com.sigi.persistence.repository;

import com.sigi.persistence.entity.Client;
import com.sigi.persistence.entity.Dispatcher;
import com.sigi.persistence.entity.Order;
import com.sigi.persistence.entity.Warehouse;
import com.sigi.persistence.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findByClientIdAndActiveTrue(UUID clientId, Pageable pageable);

    Page<Order> findByUserEmailContainingIgnoreCaseAndActiveTrue(String userEmail, Pageable pageable);

    Page<Order> findByActiveTrue(Pageable pageable);

    Page<Order> findByActiveFalse(Pageable pageable);

    Optional<Order> findByIdAndActiveFalse(UUID id);

    @Query("SELECT DISTINCT o FROM Order o " +
            "JOIN o.lines l " +
            "WHERE l.inventory.id = :inventoryId")
    Page<Order> findOrdersByInventoryId(@Param("inventoryId") UUID inventoryId, Pageable pageable);

    Page<Order> findByClientNameContainingIgnoreCaseAndActiveTrueAndStatus(
            String clientName,
            OrderStatus status,
            Pageable pageable
    );

    boolean existsByClientAndWarehouseAndDispatcherAndTotal(Client c, Warehouse w, Dispatcher d, BigDecimal total);
}

