package com.minitrello.domain.board;

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
@Table(name = "boards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Board extends SoftDeletableEntity {

    @Column(name = "project_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID projectId;

    @Column(nullable = false, length = 150)
    private String name;
}
