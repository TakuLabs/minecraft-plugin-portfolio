package de.otaku.birthdayevent.reward;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CongratulationRegistryTest {
    @Test
    void newUuidCanCongratulate() {
        CongratulationRegistry registry = new CongratulationRegistry(List.of());
        CongratulationClaim claim = registry.tryBegin(UUID.randomUUID(), "ExamplePlayer", new RewardSelection("tool", 1), Instant.now());
        assertTrue(claim.accepted());
    }

    @Test
    void sameUuidCannotCongratulateTwice() {
        CongratulationRegistry registry = new CongratulationRegistry(List.of());
        UUID uuid = UUID.randomUUID();
        assertTrue(registry.tryBegin(uuid, "ExamplePlayer", new RewardSelection("tool", 1), Instant.now()).accepted());
        assertFalse(registry.tryBegin(uuid, "ExamplePlayer", new RewardSelection("tool", 1), Instant.now()).accepted());
    }

    @Test
    void changedNameWithSameUuidStillCannotCongratulateTwice() {
        CongratulationRegistry registry = new CongratulationRegistry(List.of());
        UUID uuid = UUID.randomUUID();
        assertTrue(registry.tryBegin(uuid, "OldName", new RewardSelection("tool", 1), Instant.now()).accepted());
        assertFalse(registry.tryBegin(uuid, "NewName", new RewardSelection("nova", 2), Instant.now()).accepted());
    }

    @Test
    void completedRecordLoadedAfterRestartBlocksSecondReward() {
        UUID uuid = UUID.randomUUID();
        CongratulationRegistry beforeRestart = new CongratulationRegistry(List.of());
        beforeRestart.tryBegin(uuid, "PlayerA", new RewardSelection("apex", 2), Instant.now());
        beforeRestart.complete(uuid);

        CongratulationRegistry afterRestart = new CongratulationRegistry(beforeRestart.snapshot());
        CongratulationClaim claim = afterRestart.tryBegin(uuid, "PlayerA", new RewardSelection("dragon", 3), Instant.now());

        assertFalse(claim.accepted());
        assertEquals(CongratulationStatus.COMPLETED, claim.record().status());
        assertEquals("apex", claim.record().crate());
        assertEquals(2, claim.record().amount());
    }

    @Test
    void playerWithoutStoredRecordCanCongratulateAfterRestartDuringEvent() {
        CongratulationRegistry afterRestart = new CongratulationRegistry(List.of());
        CongratulationClaim claim = afterRestart.tryBegin(UUID.randomUUID(), "NewPlayer", new RewardSelection("nova", 1), Instant.now());

        assertTrue(claim.accepted());
    }

    @Test
    void pendingRecordLoadedAfterCrashBlocksNewRandomReward() {
        UUID uuid = UUID.randomUUID();
        CongratulationRecord pending = new CongratulationRecord(
                uuid,
                "CrashPlayer",
                Instant.now(),
                "gott",
                2,
                CongratulationStatus.PENDING
        );
        CongratulationRegistry afterCrash = new CongratulationRegistry(List.of(pending));

        CongratulationClaim claim = afterCrash.tryBegin(uuid, "CrashPlayer", new RewardSelection("tool", 1), Instant.now());

        assertFalse(claim.accepted());
        assertEquals(CongratulationStatus.PENDING, claim.record().status());
        assertEquals("gott", claim.record().crate());
        assertEquals(2, claim.record().amount());
    }

    @Test
    void concurrentAttemptsAllowExactlyOneWinner() throws Exception {
        CongratulationRegistry registry = new CongratulationRegistry(List.of());
        UUID uuid = UUID.randomUUID();
        int attempts = 32;
        AtomicInteger winners = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(attempts);
        for (int i = 0; i < attempts; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
                if (registry.tryBegin(uuid, "SpeedClicker", new RewardSelection("apex", 1), Instant.now()).accepted()) {
                    winners.incrementAndGet();
                }
            });
        }
        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(1, winners.get());
        assertEquals(1, registry.size());
    }
}
