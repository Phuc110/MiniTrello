package com.minitrello.domain.user;

import java.util.Optional;
import java.util.UUID;

/**
 * Domain-level repository port. Lives in the domain layer with zero
 * Spring/JPA imports so services can be unit-tested against a fake
 * implementation with no Spring context at all. The real implementation
 * (backed by Spring Data JPA) lives in infrastructure.persistence.impl —
 * see UserRepositoryImpl.
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
