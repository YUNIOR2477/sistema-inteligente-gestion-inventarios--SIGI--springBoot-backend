package com.sigi.persistence.repository;

import com.sigi.persistence.entity.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {
    Optional<Warehouse> findByName(String name);

    Page<Warehouse> findByActiveTrue(Pageable pageable);

    boolean existsByName(String name);

    boolean existsByNameAndActiveTrue(String name);

    @Query("SELECT b FROM Warehouse b WHERE b.totalCapacity >= :minCapacity")
    Page<Warehouse> findByGreaterOrEqualCapacityAndActiveTrue(Integer minCapacity, Pageable pageable);

    Page<Warehouse> findByActiveFalse(Pageable pageable);

    Page<Warehouse> findByNameContainingIgnoreCaseAndActiveFalse(String name, Pageable pageable);

    Page<Warehouse> findByNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable);

    Optional<Warehouse> findByIdAndActiveFalse(UUID id);
}
