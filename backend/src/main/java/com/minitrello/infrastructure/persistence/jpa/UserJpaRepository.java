package com.minitrello.infrastructure.persistence.jpa;

import com.minitrello.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA interface — infrastructure-only. Never injected
 * directly into services; always accessed through the domain-facing
 * UserRepositoryImpl adapter so business code depends on the domain
 * port, not this Spring Data type.
 */
public interface UserJpaRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
