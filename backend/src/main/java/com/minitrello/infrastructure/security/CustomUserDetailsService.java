package com.minitrello.infrastructure.security;

import com.minitrello.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /** Used by Spring Security's form-login style flows (not used directly by our JSON login endpoint, but kept for completeness / Actuator / future admin login). */
    @Override
    public UserDetails loadUserByUsername(String email) {
        return userRepository.findByEmail(email)
                .map(CustomUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with email: " + email));
    }

    /** Used by JwtAuthenticationFilter — the JWT subject claim is the user's UUID, not their email. */
    public UserDetails loadUserById(UUID id) {
        return userRepository.findById(id)
                .map(CustomUserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with id: " + id));
    }
}
