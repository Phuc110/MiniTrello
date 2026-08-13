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

/**
 * A Kanban column ("To Do", "In Progress", "Done", ...). `position` is a
 * lexicographically-sortable string (see PositionGenerator) — reordering
 * a list only ever writes to the moved row, never its siblings.
 */
@Entity
@Table(name = "board_lists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BoardList extends SoftDeletableEntity {

    @Column(name = "board_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID boardId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 255)
    private String position;
}
