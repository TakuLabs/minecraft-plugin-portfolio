package de.otaku.core;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

public final class OfflinePlayerResolver {
    private OfflinePlayerResolver() {
    }

    public static OfflinePlayer resolve(String name) {
        UUID uuid = parseUuid(name);
        if (uuid != null) {
            return Bukkit.getOfflinePlayer(uuid);
        }
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }
        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        if (cached != null) {
            return cached;
        }
        String normalized = name.toLowerCase(Locale.ROOT);
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            String playerName = player.getName();
            if (playerName != null && playerName.toLowerCase(Locale.ROOT).equals(normalized)) {
                return player;
            }
        }
        return Bukkit.getOfflinePlayer(name);
    }

    public static boolean isKnown(OfflinePlayer player) {
        return player.getPlayer() != null || player.hasPlayedBefore() || player.getName() != null;
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}

