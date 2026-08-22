package de.otaku.novaguard.model;

import java.time.Instant;
import java.util.UUID;

public record ListEntry(
        UUID uuid,
        String key,
        String lastKnownName,
        String reason,
        String addedBy,
        Instant createdAt
) {
    public static ListEntry uuid(UUID uuid, String lastKnownName, String reason, String addedBy, Instant createdAt) {
        return new ListEntry(uuid, uuid.toString(), lastKnownName, reason, addedBy, createdAt);
    }

    public static ListEntry ip(String normalizedIp, String reason, String addedBy, Instant createdAt) {
        return new ListEntry(null, normalizedIp, null, reason, addedBy, createdAt);
    }
}
