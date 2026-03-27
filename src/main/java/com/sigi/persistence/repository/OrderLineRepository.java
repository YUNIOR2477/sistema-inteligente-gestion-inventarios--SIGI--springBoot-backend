package com.sigi.persistence.repository;

import com.sigi.persistence.entity.OrderLine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderLineRepository extends JpaRepository<OrderLine, UUID> {
    Page<OrderLine> findByOrderIdAndActiveTrue(UUID orderId, Pageable pageable);

    Page<OrderLine> findByActiveTrue(Pageable pageable);

    Page<OrderLine> findByProductNameContainingIgnoreCaseAndActiveTrue(String productName, Pageable pageable);

    Page<OrderLine> findByActiveFalse(Pageable pageable);

    Page<OrderLine> findByProductNameContainingIgnoreCaseAndActiveFalse(String productName, Pageable pageable);

    Optional<OrderLine> findByIdAndActiveFalse(UUID id);

    List<OrderLine> findByOrderId(UUID orderId);
}
