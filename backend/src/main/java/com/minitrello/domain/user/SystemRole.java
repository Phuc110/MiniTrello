package com.minitrello.domain.user;

/**
 * System-wide role, distinct from per-project roles (ProjectRole, added
 * in Phase 6). ADMIN is a platform-operator concept (support/ops tooling,
 * not a per-workspace owner) — most users are MEMBER and get all their
 * real permissions from workspace/project membership rows instead.
 */
public enum SystemRole {
    ADMIN,
    MEMBER
}
