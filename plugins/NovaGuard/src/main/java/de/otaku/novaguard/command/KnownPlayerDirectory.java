package de.otaku.novaguard.command;

import de.otaku.novaguard.model.ListEntry;
import de.otaku.novaguard.storage.SecurityStores;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class KnownPlayerDirectory {
    private static final int DEFAULT_LIMIT = 40;

    private final ConcurrentMap<UUID, KnownPlayer> byUuid = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUID> byLowerName = new ConcurrentHashMap<>();

    public void rebuild(SecurityStores stores) {
        byUuid.clear();
        byLowerName.clear();
        for (var player : stores.connectionHistory().knownPlayers()) {
            remember(player.uuid(), player.lastKnownName(), player.lastSeen());
        }
        rememberEntries(stores.whitelist().list());
        rememberEntries(stores.uuidBlacklist().list());
    }

    public void remember(UUID uuid, String name) {
        remember(uuid, name, Instant.now());
    }

    public void remember(UUID uuid, String name, Instant lastSeen) {
        if (uuid == null) {
            return;
        }
        byUuid.compute(uuid, (ignored, previous) -> {
            String previousName = previous == null ? null : previous.lastKnownName();
            String nextName = name == null || name.isBlank() ? previousName : name;
            Instant previousSeen = previous == null ? Instant.EPOCH : previous.lastSeen();
            Instant nextSeen = lastSeen == null || lastSeen.isBefore(previousSeen) ? previousSeen : lastSeen;
            if (previousName != null && nextName != null && !previousName.equalsIgnoreCase(nextName)) {
                byLowerName.remove(previousName.toLowerCase(Locale.ROOT), uuid);
            }
            if (nextName != null && !nextName.isBlank()) {
                byLowerName.put(nextName.toLowerCase(Locale.ROOT), uuid);
            }
            return new KnownPlayer(uuid, nextName, nextSeen);
        });
    }

    public KnownPlayer find(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        try {
            return byUuid.get(UUID.fromString(input));
        } catch (IllegalArgumentException ignored) {
            UUID uuid = byLowerName.get(input.toLowerCase(Locale.ROOT));
            return uuid == null ? null : byUuid.get(uuid);
        }
    }

    public List<String> suggest(String token) {
        return suggest(token, DEFAULT_LIMIT);
    }

    public List<String> suggest(String token, int limit) {
        String prefix = token == null ? "" : token.toLowerCase(Locale.ROOT);
        List<KnownPlayer> matches = new ArrayList<>();
        boolean uuidPrefix = looksLikeUuidPrefix(prefix);
        for (KnownPlayer player : byUuid.values()) {
            String name = player.lastKnownName();
            if (name != null && name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(player);
            }
        }
        matches.sort(Comparator.comparing(KnownPlayer::lastKnownName, String.CASE_INSENSITIVE_ORDER));
        List<String> result = new ArrayList<>(Math.min(limit, matches.size()));
        for (KnownPlayer player : matches) {
            if (result.size() >= limit) {
                return result;
            }
            result.add(player.lastKnownName());
        }
        if (uuidPrefix && result.size() < limit) {
            for (UUID uuid : byUuid.keySet()) {
                String value = uuid.toString();
                if (value.startsWith(prefix) && !result.contains(value)) {
                    result.add(value);
                    if (result.size() >= limit) {
                        return result;
                    }
                }
            }
        }
        return result;
    }

    public int size() {
        return byUuid.size();
    }

    private void rememberEntries(List<ListEntry> entries) {
        for (ListEntry entry : entries) {
            if (entry.uuid() != null) {
                remember(entry.uuid(), entry.lastKnownName(), entry.createdAt());
            }
        }
    }

    private static boolean looksLikeUuidPrefix(String value) {
        if (value.length() < 4) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char c = value.charAt(index);
            boolean ok = c == '-' || (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
            if (!ok) {
                return false;
            }
        }
        return true;
    }
}
