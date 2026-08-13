package com.minitrello.infrastructure.persistence.impl;

import com.minitrello.domain.user.User;
import com.minitrello.domain.user.UserRepository;
import com.minitrello.infrastructure.persistence.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter implementing the domain UserRepository port on top of Spring
 * Data JPA. This is the dependency-inversion seam described in Phase 2:
 * the domain and application layers only ever see UserRepository
 * (the interface); this class is wired in by Spring at runtime.
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;

    @Override
    public User save(User user) {
        return jpaRepository.save(user);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }
}
