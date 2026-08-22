package de.otaku.birthdayevent.storage;

import de.otaku.birthdayevent.reward.CongratulationRegistry;
import de.otaku.birthdayevent.reward.RewardSelection;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceSimulationTest {
    @Test
    void uuidRemainsKnownAfterRegistryReloadSimulation() {
        UUID uuid = UUID.randomUUID();
        CongratulationRegistry beforeReload = new CongratulationRegistry(List.of());
        assertTrue(beforeReload.tryBegin(uuid, "ExamplePlayer", new RewardSelection("nova", 2), Instant.now()).accepted());
        beforeReload.complete(uuid);

        CongratulationRegistry afterReload = new CongratulationRegistry(beforeReload.snapshot());
        assertTrue(afterReload.hasCongratulated(uuid));
        assertFalse(afterReload.tryBegin(uuid, "ExamplePlayer", new RewardSelection("nova", 2), Instant.now()).accepted());
    }

    @Test
    void serverRestartSimulationKeepsCongratulationStatus() {
        UUID uuid = UUID.randomUUID();
        CongratulationRegistry firstRun = new CongratulationRegistry(List.of());
        firstRun.tryBegin(uuid, "RestartFan", new RewardSelection("dragon", 3), Instant.now());

        CongratulationRegistry secondRun = new CongratulationRegistry(firstRun.snapshot());
        assertEquals(1, secondRun.size());
        assertTrue(secondRun.hasCongratulated(uuid));
    }
}
