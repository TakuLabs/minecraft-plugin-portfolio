package de.otaku.novaguard.iplimit;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.ToIntFunction;

public final class IpLimitService {
    private final ConcurrentMap<String, IpSlots> slotsByIp = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, String> reservations = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, String> sessions = new ConcurrentHashMap<>();
    private final AtomicLong sessionSequence = new AtomicLong();
    private final Clock clock;

    public IpLimitService() {
        this(Clock.systemUTC());
    }

    public IpLimitService(Clock clock) {
        this.clock = clock;
    }

    public ReservationResult reserve(UUID uuid, String normalizedIp, int limit, long timeoutMillis) {
        cleanupExpired(timeoutMillis);
        String oldReservationIp = reservations.remove(uuid);
        if (oldReservationIp != null && !oldReservationIp.equals(normalizedIp)) {
            removeReservationFromIp(uuid, oldReservationIp);
        }
        AtomicReference<ReservationResult> result = new AtomicReference<>();
        slotsByIp.compute(normalizedIp, (ip, slots) -> {
            IpSlots current = slots == null ? new IpSlots() : slots;
            current.removeReservation(uuid);
            int active = current.active.size();
            int reserved = current.reservations.size();
            if (active + reserved >= limit) {
                result.set(ReservationResult.denied(active, reserved, limit));
                return current.isEmpty() ? null : current;
            }
            current.reservations.put(uuid, clock.millis());
            reservations.put(uuid, normalizedIp);
            result.set(ReservationResult.allowed(active, reserved + 1, limit));
            return current;
        });
        return result.get();
    }

    public void activate(UUID uuid) {
        String ip = reservations.remove(uuid);
        if (ip == null) {
            return;
        }
        slotsByIp.computeIfPresent(ip, (ignored, slots) -> {
            slots.reservations.remove(uuid);
            slots.active.put(uuid, new SessionInfo(clock.millis(), sessionSequence.incrementAndGet()));
            sessions.put(uuid, ip);
            return slots.isEmpty() ? null : slots;
        });
    }

    public void release(UUID uuid) {
        String reservationIp = reservations.remove(uuid);
        String sessionIp = sessions.remove(uuid);
        if (reservationIp != null) {
            removeReservationFromIp(uuid, reservationIp);
        }
        if (sessionIp != null) {
            removeSessionFromIp(uuid, sessionIp);
        }
    }

    public void cleanupExpired(long timeoutMillis) {
        long cutoff = clock.millis() - timeoutMillis;
        for (Map.Entry<String, IpSlots> entry : slotsByIp.entrySet()) {
            String ip = entry.getKey();
            slotsByIp.computeIfPresent(ip, (ignored, slots) -> {
                slots.reservations.entrySet().removeIf(reservation -> {
                    boolean expired = reservation.getValue() < cutoff;
                    if (expired) {
                        reservations.remove(reservation.getKey(), ip);
                    }
                    return expired;
                });
                return slots.isEmpty() ? null : slots;
            });
        }
    }

    public IpSnapshot snapshot(String normalizedIp) {
        IpSlots slots = slotsByIp.get(normalizedIp);
        if (slots == null) {
            return new IpSnapshot(0, 0);
        }
        return new IpSnapshot(slots.active.size(), slots.reservations.size());
    }

    public String ipFor(UUID uuid) {
        String session = sessions.get(uuid);
        return session != null ? session : reservations.get(uuid);
    }

    public void rebuildActiveSessions(Map<UUID, String> activeSessions) {
        Set<UUID> activeUuids = new HashSet<>(activeSessions.keySet());
        reservations.keySet().removeAll(activeUuids);
        Map<UUID, SessionInfo> previous = new ConcurrentHashMap<>();
        slotsByIp.forEach((ip, slots) -> slots.active.forEach(previous::put));
        sessions.clear();
        slotsByIp.forEach((ip, slots) -> {
            slots.active.clear();
            slots.reservations.keySet().removeAll(activeUuids);
        });
        activeSessions.forEach((uuid, ip) -> {
            sessions.put(uuid, ip);
            slotsByIp.compute(ip, (ignored, slots) -> {
                IpSlots current = slots == null ? new IpSlots() : slots;
                current.reservations.remove(uuid);
                current.active.put(uuid, previous.getOrDefault(uuid, new SessionInfo(clock.millis(), sessionSequence.incrementAndGet())));
                return current;
            });
        });
        slotsByIp.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public LimitReevaluationResult reevaluateLimits(ToIntFunction<String> limitResolver) {
        List<UUID> disconnects = new ArrayList<>();
        Map<UUID, Integer> limitsByUuid = new ConcurrentHashMap<>();
        int affectedGroups = 0;
        for (String ip : slotsByIp.keySet()) {
            AtomicReference<List<UUID>> groupDisconnects = new AtomicReference<>(List.of());
            slotsByIp.computeIfPresent(ip, (ignored, slots) -> {
                int limit = limitResolver.applyAsInt(ip);
                List<Map.Entry<UUID, SessionInfo>> active = new ArrayList<>(slots.active.entrySet());
                if (active.size() <= limit) {
                    return slots;
                }
                active.sort(Comparator
                        .comparingLong((Map.Entry<UUID, SessionInfo> entry) -> entry.getValue().joinedAtMillis())
                        .thenComparingLong(entry -> entry.getValue().sequence())
                        .thenComparing(entry -> entry.getKey().toString()));
                Set<UUID> keep = new HashSet<>();
                for (int i = 0; i < limit && i < active.size(); i++) {
                    keep.add(active.get(i).getKey());
                }
                List<UUID> remove = new ArrayList<>();
                for (Map.Entry<UUID, SessionInfo> entry : active) {
                    UUID uuid = entry.getKey();
                    if (!keep.contains(uuid)) {
                        remove.add(uuid);
                        limitsByUuid.put(uuid, limit);
                        slots.active.remove(uuid);
                        sessions.remove(uuid, ip);
                    }
                }
                groupDisconnects.set(remove);
                return slots.isEmpty() ? null : slots;
            });
            List<UUID> removed = groupDisconnects.get();
            if (!removed.isEmpty()) {
                affectedGroups++;
                disconnects.addAll(removed);
            }
        }
        return new LimitReevaluationResult(affectedGroups, List.copyOf(disconnects), Map.copyOf(limitsByUuid));
    }

    public void clear() {
        slotsByIp.clear();
        reservations.clear();
        sessions.clear();
    }

    public int activeGroupCount() {
        return slotsByIp.size();
    }

    public int activeSessionCount() {
        return sessions.size();
    }

    public int reservationCount() {
        return reservations.size();
    }

    private void removeReservationFromIp(UUID uuid, String ip) {
        slotsByIp.computeIfPresent(ip, (ignored, slots) -> {
            slots.reservations.remove(uuid);
            return slots.isEmpty() ? null : slots;
        });
    }

    private void removeSessionFromIp(UUID uuid, String ip) {
        slotsByIp.computeIfPresent(ip, (ignored, slots) -> {
            slots.active.remove(uuid);
            return slots.isEmpty() ? null : slots;
        });
    }

    private static final class IpSlots {
        private final Map<UUID, SessionInfo> active = new ConcurrentHashMap<>();
        private final Map<UUID, Long> reservations = new ConcurrentHashMap<>();

        private boolean isEmpty() {
            return active.isEmpty() && reservations.isEmpty();
        }

        private void removeReservation(UUID uuid) {
            reservations.remove(uuid);
        }
    }

    private record SessionInfo(long joinedAtMillis, long sequence) {
    }
}
