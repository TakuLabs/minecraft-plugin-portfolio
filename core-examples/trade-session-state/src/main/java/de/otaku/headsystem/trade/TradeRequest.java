package de.otaku.headsystem.trade;

import java.time.Instant;
import java.util.UUID;

public record TradeRequest(UUID requester, UUID target, Instant expiresAt) {
    public boolean expired() {
        return Instant.now().isAfter(expiresAt);
    }
}
