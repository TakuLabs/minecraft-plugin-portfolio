package de.otaku.novaguard.packet;

import de.otaku.novaguard.config.PacketMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PacketRateStateTest {
    @Test
    void normalPlayerDoesNotViolate() {
        PacketRateState state = new PacketRateState(10, 20, 5, 10, 0);
        for (int i = 0; i < 10; i++) {
            PacketDecision decision = state.record(PacketCategory.MOVEMENT, 1.0, true, 3, seconds(10), PacketMode.ENFORCE, i * seconds(1));
            assertFalse(decision.violation());
        }
    }

    @Test
    void shortBurstIsTolerated() {
        PacketRateState state = new PacketRateState(10, 20, 5, 10, 0);
        for (int i = 0; i < 20; i++) {
            assertFalse(state.record(PacketCategory.MOVEMENT, 1.0, true, 3, seconds(10), PacketMode.ENFORCE, i).violation());
        }
    }

    @Test
    void sustainedSpamReachesThresholdInEnforce() {
        PacketRateState state = new PacketRateState(1, 1, 1, 1, 0);
        PacketDecision first = state.record(PacketCategory.CHAT, 2.0, true, 3, seconds(10), PacketMode.ENFORCE, 1);
        PacketDecision second = state.record(PacketCategory.CHAT, 2.0, true, 3, seconds(10), PacketMode.ENFORCE, 2);
        PacketDecision third = state.record(PacketCategory.CHAT, 2.0, true, 3, seconds(10), PacketMode.ENFORCE, 3);
        assertTrue(first.violation());
        assertTrue(second.violation());
        assertTrue(third.disconnect());
    }

    @Test
    void monitorNeverDisconnects() {
        PacketRateState state = new PacketRateState(1, 1, 1, 1, 0);
        PacketDecision decision = null;
        for (int i = 0; i < 5; i++) {
            decision = state.record(PacketCategory.CHAT, 2.0, true, 3, seconds(10), PacketMode.MONITOR, i);
        }
        assertNotNull(decision);
        assertTrue(decision.violation());
        assertFalse(decision.disconnect());
    }

    @Test
    void violationResetClearsCounter() {
        PacketRateState state = new PacketRateState(1, 1, 1, 1, 0);
        state.record(PacketCategory.CHAT, 2.0, true, 3, seconds(10), PacketMode.ENFORCE, 1);
        PacketDecision afterReset = state.record(PacketCategory.CHAT, 2.0, true, 3, seconds(10), PacketMode.ENFORCE, seconds(11));
        assertEquals(1, afterReset.violations());
        assertFalse(afterReset.disconnect());
    }

    @Test
    void customPayloadSeparateLimitCanViolate() {
        PacketRateState state = new PacketRateState(100, 100, 1, 1, 0);
        PacketDecision first = state.record(PacketCategory.CUSTOM_PAYLOAD, 1.0, true, 2, seconds(10), PacketMode.ENFORCE, 1);
        PacketDecision second = state.record(PacketCategory.CUSTOM_PAYLOAD, 1.0, true, 2, seconds(10), PacketMode.ENFORCE, 2);
        assertFalse(first.violation());
        assertTrue(second.violation());
    }

    @Test
    void highPpsSpamEscalatesPredictably() {
        assertSpamAtPps(1_000);
        assertSpamAtPps(2_000);
        assertSpamAtPps(5_000);
    }

    @Test
    void inventoryAndInteractionSpamUseWeightedCost() {
        PacketRateState state = new PacketRateState(10, 10, 5, 5, 0);
        PacketDecision inventory = state.record(PacketCategory.INVENTORY, 5.0, true, 2, seconds(10), PacketMode.ENFORCE, 1);
        PacketDecision interaction = state.record(PacketCategory.INTERACTION, 10.0, true, 2, seconds(10), PacketMode.ENFORCE, 2);
        assertFalse(inventory.violation());
        assertTrue(interaction.violation());
    }

    @Test
    void manyNormalSamplesDoNotGrowStateObjectExternally() {
        PacketRateState state = new PacketRateState(500, 800, 80, 160, 0);
        PacketDecision last = null;
        for (int i = 0; i < 10_000; i++) {
            last = state.record(PacketCategory.MOVEMENT, 1.0, true, 3, seconds(10), PacketMode.MONITOR, i * 2_000_000L);
        }
        assertNotNull(last);
        assertFalse(last.disconnect());
    }

    private void assertSpamAtPps(int pps) {
        PacketRateState state = new PacketRateState(450, 800, 80, 160, 0);
        PacketDecision last = null;
        long step = 1_000_000_000L / pps;
        for (int i = 0; i < pps * 2; i++) {
            last = state.record(PacketCategory.OTHER, 1.0, true, 3, seconds(10), PacketMode.ENFORCE, i * step);
        }
        assertNotNull(last);
        assertTrue(last.violation());
        assertTrue(last.disconnect());
    }

    private static long seconds(long seconds) {
        return seconds * 1_000_000_000L;
    }
}
