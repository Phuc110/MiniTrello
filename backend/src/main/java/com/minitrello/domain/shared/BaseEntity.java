package com.minitrello.domain.shared;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Base for every persistent entity in the system.
 *
 * Design notes:
 *  - UUIDv7 (time-ordered) primary keys: no enumeration/volume leakage via
 *    URLs, and — unlike UUIDv4 — insert locality stays good for the InnoDB
 *    clustered index because the value is monotonically increasing.
 *  - createdAt/updatedAt are populated by Spring Data JPA auditing
 *    (see JpaAuditingConfig) and are separate from the AuditLog table,
 *    which tracks *business* changes, not raw persistence timestamps.
 *  - This class deliberately has NO deletedAt — only entities that actually
 *    need soft delete extend SoftDeletableEntity instead. Not every entity
 *    (e.g. junction tables, audit logs) should carry that concept.
 *  - @SuperBuilder here (rather than plain @Builder) so that concrete
 *    subclasses (User, RefreshToken, ...) can also use @SuperBuilder and
 *    get a fluent builder that includes THIS class's fields too. Lombok
 *    requires every class in the hierarchy to opt into @SuperBuilder for
 *    that chain to work, hence it's declared all the way up here.
 */
@Getter
@Setter
@EqualsAndHashCode(of = "id")
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@SuperBuilder
@NoArgsConstructor
public abstract class BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(columnDefinition = "BINARY(16)", updatable = false, nullable = false)
    private UUID id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}
