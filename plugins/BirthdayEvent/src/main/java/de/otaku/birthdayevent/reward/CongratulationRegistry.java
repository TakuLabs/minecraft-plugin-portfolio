package de.otaku.birthdayevent.reward;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CongratulationRegistry {
    private final Map<UUID, CongratulationRecord> records = new HashMap<>();

    public CongratulationRegistry(Collection<CongratulationRecord> initialRecords) {
        if (initialRecords != null) {
            for (CongratulationRecord record : initialRecords) {
                records.put(record.uuid(), record);
            }
        }
    }

    public synchronized CongratulationClaim tryBegin(UUID uuid, String playerName, RewardSelection selection, Instant now) {
        CongratulationRecord existing = records.get(uuid);
        if (existing != null) {
            return CongratulationClaim.rejected(existing);
        }

        CongratulationRecord record = new CongratulationRecord(
                uuid,
                playerName,
                now,
                selection.crate(),
                selection.amount(),
                CongratulationStatus.PENDING
        );
        records.put(uuid, record);
        return CongratulationClaim.accepted(record);
    }

    public synchronized CongratulationRecord complete(UUID uuid) {
        CongratulationRecord existing = records.get(uuid);
        if (existing == null) {
            return null;
        }
        CongratulationRecord completed = existing.withStatus(CongratulationStatus.COMPLETED);
        records.put(uuid, completed);
        return completed;
    }

    public synchronized void rollbackPending(UUID uuid) {
        CongratulationRecord existing = records.get(uuid);
        if (existing != null && existing.status() == CongratulationStatus.PENDING) {
            records.remove(uuid);
        }
    }

    public synchronized boolean hasCongratulated(UUID uuid) {
        return records.containsKey(uuid);
    }

    public synchronized CongratulationRecord find(UUID uuid) {
        return records.get(uuid);
    }

    public synchronized int size() {
        return records.size();
    }

    public synchronized Collection<CongratulationRecord> snapshot() {
        return Map.copyOf(records).values();
    }
}
