package com.minitrello.domain.workspace;

import com.minitrello.domain.shared.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "workspaces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Workspace extends SoftDeletableEntity {

    @Column(nullable = false, length = 150)
    private String name;

    /** URL-friendly, globally-unique identifier (e.g. "acme-corp") used for shareable links. Generated from name at creation time. */
    @Column(nullable = false, unique = true, length = 150)
    private String slug;

    /**
     * The workspace owner. Deliberately a plain FK column (not a
     * WorkspaceMember row) — ownership is a distinct, single-holder
     * concept from general membership, and is what gates destructive
     * actions like deleting the workspace or transferring ownership.
     */
    @Column(name = "owner_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID ownerId;
}
