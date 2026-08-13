package com.minitrello.domain.workspace;

import com.minitrello.domain.shared.BaseEntity;
import com.minitrello.domain.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Pure membership — no role here by design (see Phase 2 open question):
 * joining a Workspace grants no automatic Project access. Real
 * permissions come from ProjectMember rows. Workspace-level destructive
 * actions are gated by Workspace.ownerId, not by anything on this row.
 */
@Entity
@Table(name = "workspace_members", uniqueConstraints = {
        @UniqueConstraint(name = "uq_workspace_members_workspace_user", columnNames = {"workspace_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class WorkspaceMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
