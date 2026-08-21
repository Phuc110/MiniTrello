package com.minitrello.domain.user;

/**
 * System-wide role. ADMIN is a platform-operator concept (support/ops tooling,
 * not a per-workspace owner) — most users are MEMBER and get all their
 * real permissions from workspace membership rows.
 */
public enum SystemRole {
    ADMIN,
    MEMBER
}
