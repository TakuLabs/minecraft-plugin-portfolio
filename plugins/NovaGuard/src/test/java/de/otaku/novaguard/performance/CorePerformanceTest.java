package de.otaku.novaguard.performance;

import de.otaku.novaguard.config.PacketMode;
import de.otaku.novaguard.iplimit.IpLimitService;
import de.otaku.novaguard.packet.PacketCategory;
import de.otaku.novaguard.packet.PacketRateState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class CorePerformanceTest {
    @Test
    void thousandSessionsCanBeAddedAndRemovedWithoutResidualState() {
        IpLimitService service = new IpLimitService();
        List<UUID> uuids = new ArrayList<>();
        for (int i = 0; i < 1_000; i++) {
            UUID uuid = UUID.randomUUID();
            uuids.add(uuid);
            String ip = "203.0.113." + (i % 200);
            assertTrue(service.reserve(uuid, ip, 15, 30_000).allowed());
            service.activate(uuid);
        }
        assertEquals(1_000, service.activeSessionCount());
        assertTrue(service.activeGroupCount() <= 200);
        for (UUID uuid : uuids) {
            service.release(uuid);
        }
        assertEquals(0, service.activeSessionCount());
        assertEquals(0, service.reservationCount());
        assertEquals(0, service.activeGroupCount());
    }

    @Test
    void repeatedJoinQuitDoesNotGrowMaps() {
        IpLimitService service = new IpLimitService();
        String ip = "203.0.113.20";
        for (int i = 0; i < 10_000; i++) {
            UUID uuid = UUID.randomUUID();
            assertTrue(service.reserve(uuid, ip, 15, 30_000).allowed());
            service.activate(uuid);
            service.release(uuid);
        }
        assertEquals(0, service.activeSessionCount());
        assertEquals(0, service.reservationCount());
        assertEquals(0, service.activeGroupCount());
    }

    @Test
    void concurrentDifferentIpLoginsDoNotBlockEachOtherLogically() throws Exception {
        IpLimitService service = new IpLimitService();
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(8)) {
            List<java.util.concurrent.Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < 250; i++) {
                int index = i;
                futures.add(executor.submit(() -> {
                    start.await();
                    UUID uuid = UUID.randomUUID();
                    return service.reserve(uuid, "203.0.113." + index, 1, 30_000).allowed();
                }));
            }
            start.countDown();
            for (var future : futures) {
                assertTrue(future.get());
            }
        }
        assertEquals(250, service.reservationCount());
    }

    @Test
    void sameIpConcurrentLoginsRespectLimit() throws Exception {
        IpLimitService service = new IpLimitService();
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(8)) {
            List<java.util.concurrent.Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return service.reserve(UUID.randomUUID(), "203.0.113.20", 15, 30_000).allowed();
                }));
            }
            start.countDown();
            int allowed = 0;
            for (var future : futures) {
                if (future.get()) {
                    allowed++;
                }
            }
            assertEquals(15, allowed);
        }
    }

    @Test
    void packetRateStateHandlesHighPacketLoadWithoutPerPacketHistory() {
        PacketRateState state = new PacketRateState(450, 800, 80, 160, 0);
        int status = 0;
        long now = 0;
        for (int i = 0; i < 100 * 1_000; i++) {
            status = state.recordStatus(PacketCategory.MOVEMENT, 1.0, true, 3, 10_000_000_000L, PacketMode.ENFORCE, now);
            now += 1_000_000L;
        }
        assertTrue(status < 0);
    }
}
