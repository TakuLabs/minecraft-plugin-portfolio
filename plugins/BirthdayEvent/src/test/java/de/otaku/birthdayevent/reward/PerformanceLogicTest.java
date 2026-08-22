package de.otaku.birthdayevent.reward;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PerformanceLogicTest {
    @Test
    void handlesOneThousandUniqueCongratulationsWithConstantLookupStructure() {
        CongratulationRegistry registry = new CongratulationRegistry(List.of());
        for (int i = 0; i < 1000; i++) {
            UUID uuid = UUID.randomUUID();
            assertTrue(registry.tryBegin(uuid, "Player" + i, new RewardSelection("tool", 1), Instant.now()).accepted());
        }
        assertEquals(1000, registry.size());
    }
}
