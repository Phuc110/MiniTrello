package com.minitrello.domain.task;

import com.minitrello.domain.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/** Tags are scoped to a Project (per Phase 3 ERD: PROJECTS ||--o{ TAGS) so each project curates its own tag vocabulary rather than sharing a global list. */
@Entity
@Table(name = "tags", uniqueConstraints = {
        @UniqueConstraint(name = "uq_tags_project_name", columnNames = {"project_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Tag extends BaseEntity {

    @Column(name = "project_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID projectId;

    @Column(nullable = false, length = 50)
    private String name;

    /** Hex color, e.g. "#FF5733", validated at the DTO layer. */
    @Column(nullable = false, length = 7)
    private String color;
}
