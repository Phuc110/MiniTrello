package com.minitrello.domain.shared;

/**
 * Generates lexicographically-sortable position strings for drag-and-drop
 * ordering (BoardList and Task), per the Phase 2 design decision: moving
 * an item only ever writes ONE row (the moved item's new position),
 * never its siblings — critical for avoiding write amplification and
 * race conditions when multiple users drag cards concurrently.
 *
 * IMPORTANT — pragmatic implementation note: this is a simplified
 * fractional-indexing scheme (base-36 digit-by-digit midpoint search), not
 * a full LexoRank implementation. It handles the common cases correctly
 * (initial insert, insert at start/end, insert between two distinct
 * neighbors) but repeated insertions at the exact same spot will grow the
 * string length over time. That is an accepted tradeoff: the Phase 3
 * design already calls for a periodic background rebalancing job
 * (Sprint 10) that recomputes clean, short positions for an entire list
 * — this class does not need to be perfect forever, just correct and
 * fast for normal usage between rebalances. Before relying on this at
 * very large scale, consider swapping in a battle-tested library instead
 * of this in-house version.
 */
public final class PositionGenerator {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";
    private static final int BASE = ALPHABET.length();
    private static final int MAX_DEPTH = 100;

    private PositionGenerator() {
    }

    /** First item ever inserted into an empty list — starts at the middle of the keyspace so there's room to insert on both sides. */
    public static String initial() {
        return String.valueOf(ALPHABET.charAt(BASE / 2));
    }

    /** Position for an item inserted before everything currently in the list. */
    public static String before(String next) {
        return between(null, next);
    }

    /** Position for an item inserted after everything currently in the list. */
    public static String after(String prev) {
        return between(prev, null);
    }

    /**
     * Position for an item inserted strictly between two existing
     * positions. Pass null for prev/next when inserting at an end.
     */
    public static String between(String prev, String next) {
        if (prev != null && next != null && prev.compareTo(next) >= 0) {
            throw new IllegalArgumentException(
                    "prev [%s] must sort strictly before next [%s]".formatted(prev, next));
        }

        StringBuilder result = new StringBuilder();
        boolean nextStillBounds = true;

        for (int depth = 0; depth < MAX_DEPTH; depth++) {
            int p = digitAt(prev, depth, 0);
            int n = nextStillBounds ? digitAt(next, depth, BASE) : BASE;

            if (n - p > 1) {
                result.append(ALPHABET.charAt(p + (n - p) / 2));
                return result.toString();
            }

            // Digits are equal or adjacent at this depth — lock this digit
            // in and continue searching at the next depth.
            result.append(ALPHABET.charAt(p));
            if (n - p == 1) {
                // We've now dipped strictly below `next` in sort order, so
                // `next` no longer constrains any deeper digit.
                nextStillBounds = false;
            }
        }

        // Reaching MAX_DEPTH means many insertions have collided at the
        // same spot without a rebalance — extremely rare in practice.
        // Append one more digit rather than fail the request outright.
        result.append(ALPHABET.charAt(BASE / 2));
        return result.toString();
    }

    private static int digitAt(String s, int index, int fallback) {
        if (s == null || index >= s.length()) {
            return fallback;
        }
        return ALPHABET.indexOf(s.charAt(index));
    }
}
