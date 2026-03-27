package com.sigi.persistence.repository;

import com.sigi.persistence.entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {

    Page<Client> findByNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable);

    Optional<Client> findByIdentificationAndActiveTrue(String identification);

    Page<Client> findByActiveTrue(Pageable pageable);

    Page<Client> findByNameContainingIgnoreCaseAndActiveFalse(String name, Pageable pageable);

    Page<Client> findByActiveFalse(Pageable pageable);

    Optional<Client> findByIdAndActiveFalse(UUID id);

    boolean existsByIdentification(String identification);

    boolean existsByEmail(String email);
}
