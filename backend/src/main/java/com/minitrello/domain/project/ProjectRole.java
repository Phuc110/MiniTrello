package com.minitrello.domain.project;

/**
 * Per-project role — this is what actually gates most actions in the
 * system (see Phase 2 authorization flow: system role tells you almost
 * nothing, this row is what matters).
 *
 * Ordered loosely by privilege for readability; do NOT rely on enum
 * ordinal() for permission comparisons — use explicit checks in
 * ProjectAuthorizationService instead, so re-ordering this enum can never
 * silently change security behavior.
 */
public enum ProjectRole {
    OWNER,
    MANAGER,
    CONTRIBUTOR,
    VIEWER
}
