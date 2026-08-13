package com.minitrello.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Activates @CreatedDate / @LastModifiedDate handling declared on
 * BaseEntity. Kept as its own tiny config class (rather than bolted onto
 * the main application class) so it's easy to find and disable in tests
 * that don't need it.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
