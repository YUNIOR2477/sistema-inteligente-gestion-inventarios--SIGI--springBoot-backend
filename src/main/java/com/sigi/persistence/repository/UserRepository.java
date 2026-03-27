package com.sigi.persistence.repository;

import com.sigi.persistence.entity.User;
import com.sigi.persistence.enums.RoleList;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    @EntityGraph(attributePaths = {"role"})
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findByActiveTrue(Pageable pageable);

    Page<User> findByActiveFalse(Pageable pageable);

    List<User> findByRole_Name(RoleList roleName);

    Page<User> findByActiveFalseAndNameContainingIgnoreCase(String name, Pageable pageable);

    Optional<User> findByIdAndActiveFalse(UUID id);

    Page<User> findByNameContainingIgnoreCaseAndActiveTrue( String name, Pageable pageable);
}