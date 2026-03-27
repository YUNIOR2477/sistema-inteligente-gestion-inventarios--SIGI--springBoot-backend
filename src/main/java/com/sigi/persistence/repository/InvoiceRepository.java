package com.sigi.persistence.repository;

import com.sigi.persistence.entity.Invoice;
import com.sigi.persistence.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    Optional<Invoice> findByNumberAndActiveTrue(String number);

    Page<Invoice> findByClientNameContainingIgnoreCaseAndActiveTrue(String clientName, Pageable pageable);

    Page<Invoice> findByStatusAndActiveTrue(InvoiceStatus status, Pageable pageable);

    Page<Invoice> findByActiveTrue(Pageable pageable);

    Page<Invoice> findByActiveFalse(Pageable pageable);

    Page<Invoice> findByNumberContainingIgnoreCaseAndActiveTrue(String number, Pageable pageable);

    Optional<Invoice> findByIdAndActiveFalse(UUID id);

    Optional<Invoice> findByOrderId(UUID orderId);

}