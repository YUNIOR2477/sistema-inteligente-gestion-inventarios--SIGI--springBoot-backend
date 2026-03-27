package com.sigi.persistence.repository;

import com.sigi.persistence.entity.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    Page<ChatRoom> findByNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable);

    Page<ChatRoom> findByActiveTrue(Pageable pageable);

    boolean existsByIdAndParticipants_Id(UUID roomId, UUID userId);

    Page<ChatRoom> findByParticipants_Id(UUID userId,Pageable pageable);
}
