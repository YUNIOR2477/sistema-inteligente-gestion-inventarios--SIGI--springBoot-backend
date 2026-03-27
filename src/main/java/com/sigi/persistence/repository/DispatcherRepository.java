package com.sigi.persistence.repository;

import com.sigi.persistence.entity.Dispatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DispatcherRepository extends JpaRepository<Dispatcher, UUID> {
    boolean existsByNameAndActiveTrue(String name);

    Page<Dispatcher> findByNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable);

    Page<Dispatcher> findByActiveTrue(Pageable pageable);

    Page<Dispatcher> findByNameContainingIgnoreCaseAndActiveFalse(String name, Pageable pageable);

    Page<Dispatcher> findByActiveFalse(Pageable pageable);

    Optional<Dispatcher> findByIdAndActiveFalse(UUID id);

    boolean existsByIdentification(String identification);

    boolean existsByEmail(String email);
}

