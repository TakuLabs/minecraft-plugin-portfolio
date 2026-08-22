package de.otaku.birthdayevent.reward;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RewardSelectorTest {
    private final RewardSelector selector = new RewardSelector(List.of(
            new RewardCrate("apex", 1, 3, 1),
            new RewardCrate("dragon", 1, 3, 1),
            new RewardCrate("gott", 1, 2, 1),
            new RewardCrate("infinity", 1, 1, 1),
            new RewardCrate("keyall", 1, 3, 1),
            new RewardCrate("nova", 1, 3, 1),
            new RewardCrate("tool", 1, 1, 1)
    ));

    @Test
    void toolAlwaysOne() {
        assertRange("tool", 1, 1);
    }

    @Test
    void infinityAlwaysOne() {
        assertRange("infinity", 1, 1);
    }

    @Test
    void gottNeverMoreThanTwo() {
        assertRange("gott", 1, 2);
    }

    @Test
    void apexBetweenOneAndThree() {
        assertRange("apex", 1, 3);
    }

    @Test
    void dragonBetweenOneAndThree() {
        assertRange("dragon", 1, 3);
    }

    @Test
    void keyallBetweenOneAndThree() {
        assertRange("keyall", 1, 3);
    }

    @Test
    void novaBetweenOneAndThree() {
        assertRange("nova", 1, 3);
    }

    private void assertRange(String crate, int min, int max) {
        for (int i = 0; i < 500; i++) {
            RewardSelection selection = selector.selectFixedCrate(crate);
            assertEquals(crate, selection.crate());
            assertTrue(selection.amount() >= min, crate + " underflow");
            assertTrue(selection.amount() <= max, crate + " overflow");
        }
    }
}
