package com.sigi.persistence.repository;

import com.sigi.persistence.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Notificaciones por usuario (receptor)
    Page<Notification> findByUserId(UUID userId, Pageable pageable);

    // Notificaciones no leídas
    Page<Notification> findByUserIdAndIsReadFalse(UUID userId, Pageable pageable);

    // Contar notificaciones no leídas
    long countByUserIdAndIsReadFalse(UUID userId);

    // Notificaciones por tipo (ORDER_CONFIRMED, STOCK_LOW, etc.)
    Page<Notification> findByUserIdAndTitleContainingIgnoreCase(UUID userId, String title, Pageable pageable);
}

