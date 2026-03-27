package com.sigi.persistence.repository;

import com.sigi.persistence.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    // Mensajes por room (grupal/personalizado)
    Page<ChatMessage> findByRoomId(UUID roomId, Pageable pageable);

    // Mensajes enviados por un usuario
    Page<ChatMessage> findBySenderEmail(String senderEmail, Pageable pageable);

    // Mensajes no leídos en un room para un usuario
    long countByRoomIdAndIsReadFalseAndSenderIdNot(UUID roomId, UUID senderId);
}
