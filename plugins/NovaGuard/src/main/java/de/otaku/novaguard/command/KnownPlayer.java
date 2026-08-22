package de.otaku.novaguard.command;

import java.time.Instant;
import java.util.UUID;

public record KnownPlayer(UUID uuid, String lastKnownName, Instant lastSeen) {
}
