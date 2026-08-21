package com.minitrello.domain.shared;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;

/**
 * Base for entities that support soft delete.
 *
 * @SQLRestriction transparently appends "deleted_at IS NULL" to every
 * SELECT Hibernate generates for the concrete subclass — so no repository
 * method needs to remember to filter deleted rows manually, and it's
 * impossible to accidentally leak a soft-deleted row through a forgotten
 * WHERE clause.
 *
 * IMPORTANT: Hibernate does NOT inherit @SQLRestriction from a
 * @MappedSuperclass. Every concrete entity extending this class MUST
 * repeat @SQLRestriction("deleted_at IS NULL") on itself, or soft-deleted
 * rows will silently leak through every derived-query SELECT.
 *
 * A hard delete (real SQL DELETE, and the associated FK ON DELETE CASCADE)
 * only happens via the scheduled purge job for rows older than the
 * configured retention window — see PurgeSoftDeletedRowsJob (Sprint 10).
 */
@Getter
@Setter
@MappedSuperclass
@SQLRestriction("deleted_at IS NULL")
@SuperBuilder
@NoArgsConstructor
public abstract class SoftDeletableEntity extends BaseEntity {

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    public void restore() {
        this.deletedAt = null;
    }
}
