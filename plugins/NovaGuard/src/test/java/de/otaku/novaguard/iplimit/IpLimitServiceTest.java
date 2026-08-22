package de.otaku.novaguard.iplimit;

import de.otaku.novaguard.util.IpNormalizer;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IpLimitServiceTest {
    @Test
    void thirdAccountSameIpIsDeniedAndQuitFreesSlot() {
        IpLimitService service = new IpLimitService();
        String ip = "203.0.113.20";
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        assertTrue(service.reserve(a, ip, 2, 30_000).allowed());
        service.activate(a);
        assertTrue(service.reserve(b, ip, 2, 30_000).allowed());
        service.activate(b);
        assertFalse(service.reserve(c, ip, 2, 30_000).allowed());
        service.release(a);
        assertTrue(service.reserve(c, ip, 2, 30_000).allowed());
    }

    @Test
    void limitOneDeniesSecondUuidOnSameIp() {
        IpLimitService service = new IpLimitService();
        String ip = "203.0.113.20";
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertTrue(service.reserve(a, ip, 1, 30_000).allowed());
        service.activate(a);
        assertFalse(service.reserve(b, ip, 1, 30_000).allowed());
    }

    @Test
    void limitTwoDeniesThirdUuidOnSameIp() {
        IpLimitService service = new IpLimitService();
        String ip = "203.0.113.20";
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        assertTrue(service.reserve(a, ip, 2, 30_000).allowed());
        service.activate(a);
        assertTrue(service.reserve(b, ip, 2, 30_000).allowed());
        service.activate(b);
        assertFalse(service.reserve(c, ip, 2, 30_000).allowed());
    }

    @Test
    void limitFifteenAllowsFiveAccountsOnSameIp() {
        IpLimitService service = new IpLimitService();
        String ip = "203.0.113.20";
        for (int i = 0; i < 5; i++) {
            UUID uuid = UUID.randomUUID();
            assertTrue(service.reserve(uuid, ip, 15, 30_000).allowed());
            service.activate(uuid);
        }
        assertEquals(5, service.snapshot(ip).active());
    }

    @Test
    void reducingLimitFromFifteenToTwoDisconnectsNewestSessions() {
        MutableClock clock = new MutableClock();
        IpLimitService service = new IpLimitService(clock);
        String ip = "203.0.113.20";
        UUID a = reserveAndActivate(service, clock, ip, 15);
        UUID b = reserveAndActivate(service, clock, ip, 15);
        UUID c = reserveAndActivate(service, clock, ip, 15);
        UUID d = reserveAndActivate(service, clock, ip, 15);

        LimitReevaluationResult result = service.reevaluateLimits(ignored -> 2);
        assertEquals(1, result.affectedGroups());
        assertEquals(List.of(c, d), result.disconnects());
        assertEquals(2, service.snapshot(ip).active());
        assertEquals(ip, service.ipFor(a));
        assertEquals(ip, service.ipFor(b));
        assertNull(service.ipFor(c));
        assertNull(service.ipFor(d));
    }

    @Test
    void reducingLimitFromFifteenToOneKeepsOldestSession() {
        MutableClock clock = new MutableClock();
        IpLimitService service = new IpLimitService(clock);
        String ip = "203.0.113.20";
        UUID a = reserveAndActivate(service, clock, ip, 15);
        UUID b = reserveAndActivate(service, clock, ip, 15);
        UUID c = reserveAndActivate(service, clock, ip, 15);

        LimitReevaluationResult result = service.reevaluateLimits(ignored -> 1);
        assertEquals(List.of(b, c), result.disconnects());
        assertEquals(ip, service.ipFor(a));
        assertEquals(1, service.snapshot(ip).active());
    }

    @Test
    void increasingLimitDisconnectsNobody() {
        IpLimitService service = new IpLimitService();
        String ip = "203.0.113.20";
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        service.reserve(a, ip, 2, 30_000);
        service.activate(a);
        service.reserve(b, ip, 2, 30_000);
        service.activate(b);
        LimitReevaluationResult result = service.reevaluateLimits(ignored -> 15);
        assertTrue(result.disconnects().isEmpty());
        assertEquals(2, service.snapshot(ip).active());
    }

    @Test
    void overrideLimitIsUsedDuringReevaluation() {
        IpLimitService service = new IpLimitService();
        String ip = "203.0.113.20";
        for (int i = 0; i < 4; i++) {
            UUID uuid = UUID.randomUUID();
            service.reserve(uuid, ip, 15, 30_000);
            service.activate(uuid);
        }
        LimitReevaluationResult result = service.reevaluateLimits(ignored -> 4);
        assertTrue(result.disconnects().isEmpty());
        assertEquals(4, service.snapshot(ip).active());
    }

    @Test
    void updatedLimitIsUsedAfterReloadScenario() {
        IpLimitService service = new IpLimitService();
        String ip = "203.0.113.20";
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        assertTrue(service.reserve(a, ip, 2, 30_000).allowed());
        service.activate(a);
        assertFalse(service.reserve(b, ip, 1, 30_000).allowed());
    }

    @Test
    void rebuildActiveSessionsKeepsOnlinePlayerCountedAfterReload() {
        IpLimitService service = new IpLimitService();
        String ip = "203.0.113.20";
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        service.rebuildActiveSessions(Map.of(a, ip));
        assertEquals(1, service.snapshot(ip).active());
        assertFalse(service.reserve(b, ip, 1, 30_000).allowed());
    }

    @Test
    void bypassPolicyRequiresWhitelistOrExactBypassPermission() {
        assertFalse(IpLimitAccessPolicy.bypassesIpLimit(false, false));
        assertTrue(IpLimitAccessPolicy.bypassesIpLimit(false, true));
        assertTrue(IpLimitAccessPolicy.bypassesIpLimit(true, false));
    }

    @Test
    void concurrentLoginRaceAllowsOnlyRemainingSlot() throws Exception {
        IpLimitService service = new IpLimitService();
        String ip = "203.0.113.20";
        UUID existing = UUID.randomUUID();
        service.reserve(existing, ip, 2, 30_000);
        service.activate(existing);

        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var one = executor.submit(() -> reserveAfter(start, service, ip));
            var two = executor.submit(() -> reserveAfter(start, service, ip));
            start.countDown();
            int allowed = (one.get().allowed() ? 1 : 0) + (two.get().allowed() ? 1 : 0);
            assertEquals(1, allowed);
        }
    }

    @Test
    void overrideLimitAllowsMoreAccounts() {
        IpLimitService service = new IpLimitService();
        String ip = "203.0.113.20";
        for (int i = 0; i < 4; i++) {
            UUID uuid = UUID.randomUUID();
            assertTrue(service.reserve(uuid, ip, 4, 30_000).allowed());
            service.activate(uuid);
        }
        assertFalse(service.reserve(UUID.randomUUID(), ip, 4, 30_000).allowed());
    }

    @Test
    void expiredReservationIsCleaned() {
        MutableClock clock = new MutableClock();
        IpLimitService service = new IpLimitService(clock);
        String ip = "203.0.113.20";
        service.reserve(UUID.randomUUID(), ip, 1, 30_000);
        assertEquals(1, service.snapshot(ip).reserved());
        clock.advanceMillis(31_000);
        service.cleanupExpired(30_000);
        assertEquals(0, service.snapshot(ip).reserved());
    }

    @Test
    void movingReservationToAnotherIpDoesNotLeaveStaleSlot() {
        IpLimitService service = new IpLimitService();
        UUID uuid = UUID.randomUUID();
        assertTrue(service.reserve(uuid, "203.0.113.20", 1, 30_000).allowed());
        assertTrue(service.reserve(uuid, "203.0.113.21", 1, 30_000).allowed());
        assertEquals(0, service.snapshot("203.0.113.20").reserved());
        assertEquals(1, service.snapshot("203.0.113.21").reserved());
    }

    @Test
    void repeatedReleaseIsIdempotent() {
        IpLimitService service = new IpLimitService();
        UUID uuid = UUID.randomUUID();
        assertTrue(service.reserve(uuid, "203.0.113.20", 1, 30_000).allowed());
        service.activate(uuid);
        service.release(uuid);
        service.release(uuid);
        assertEquals(0, service.snapshot("203.0.113.20").active());
        assertEquals(0, service.snapshot("203.0.113.20").reserved());
    }

    @Test
    void normalizesIpv4Ipv6AndMappedIpv6() throws Exception {
        assertEquals("203.0.113.20", IpNormalizer.normalize("203.0.113.20:25565").orElseThrow());
        assertEquals("2001:db8:0:0:0:0:0:1", IpNormalizer.normalize("[2001:db8::1]:25565").orElseThrow());
        assertTrue(IpNormalizer.normalize(InetAddress.getByName("2001:db8::1")).contains(":"));
        assertEquals("203.0.113.20", IpNormalizer.normalize("::ffff:203.0.113.20").orElseThrow());
        assertTrue(IpNormalizer.normalize("not-an-ip.example").isEmpty());
    }

    private ReservationResult reserveAfter(CountDownLatch start, IpLimitService service, String ip) throws Exception {
        start.await();
        return service.reserve(UUID.randomUUID(), ip, 2, 30_000);
    }

    private UUID reserveAndActivate(IpLimitService service, MutableClock clock, String ip, int limit) {
        UUID uuid = UUID.randomUUID();
        assertTrue(service.reserve(uuid, ip, limit, 30_000).allowed());
        service.activate(uuid);
        clock.advanceMillis(1_000);
        return uuid;
    }

    private static final class MutableClock extends Clock {
        private long millis;

        void advanceMillis(long amount) {
            millis += amount;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis);
        }

        @Override
        public long millis() {
            return millis;
        }
    }
}
