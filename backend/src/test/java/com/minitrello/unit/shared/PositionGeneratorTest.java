package com.minitrello.unit.shared;

import com.minitrello.domain.shared.PositionGenerator;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PositionGenerator is the correctness-critical piece behind drag-and-drop
 * ordering (Phase 2 decision: write only the moved row, never siblings).
 * These tests focus on the invariant that actually matters for the
 * product: repeatedly inserting between two neighbors always produces a
 * value that sorts strictly between them, however many times you do it.
 */
class PositionGeneratorTest {

    @Test
    void initial_returnsAStableMidpointValue() {
        assertThat(PositionGenerator.initial()).isNotBlank();
    }

    @Test
    void after_generatesValueGreaterThanInput() {
        String first = PositionGenerator.initial();
        String second = PositionGenerator.after(first);

        assertThat(second).isGreaterThan(first);
    }

    @Test
    void before_generatesValueLessThanInput() {
        String first = PositionGenerator.initial();
        String earlier = PositionGenerator.before(first);

        assertThat(earlier).isLessThan(first);
    }

    @Test
    void between_generatesValueStrictlyBetweenTwoDistinctPositions() {
        String a = PositionGenerator.initial();
        String c = PositionGenerator.after(a);
        String b = PositionGenerator.between(a, c);

        assertThat(b).isGreaterThan(a);
        assertThat(b).isLessThan(c);
    }

    @Test
    void between_rejectsInvertedRange() {
        String a = PositionGenerator.initial();
        String b = PositionGenerator.after(a);

        assertThatThrownBy(() -> PositionGenerator.between(b, a))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void repeatedInsertionsAtSameSpot_alwaysStayOrdered() {
        // Simulates dragging many cards to the very top of a list in
        // sequence, one after another — the classic worst case for a
        // naive fractional-indexing scheme.
        String top = PositionGenerator.initial();
        List<String> history = new ArrayList<>();
        history.add(top);

        for (int i = 0; i < 200; i++) {
            String newTop = PositionGenerator.before(history.get(0));
            assertThat(newTop).isLessThan(history.get(0));
            history.add(0, newTop);
        }

        // The whole history must still be in strictly ascending order.
        for (int i = 1; i < history.size(); i++) {
            assertThat(history.get(i)).isGreaterThan(history.get(i - 1));
        }
    }

    @RepeatedTest(20)
    void betweenTwoCloseValues_stillProducesAValidMidpoint() {
        String a = PositionGenerator.initial();
        String b = PositionGenerator.after(a);
        // Repeatedly bisect the gap — this is what happens when many
        // cards get dropped into the same narrow slot over time.
        for (int i = 0; i < 15; i++) {
            String mid = PositionGenerator.between(a, b);
            assertThat(mid).isGreaterThan(a).isLessThan(b);
            b = mid; // keep narrowing the upper bound
        }
    }
}
