package de.otaku.birthdayevent.reward;

import java.time.Instant;
import java.util.UUID;

public record CongratulationRecord(
        UUID uuid,
        String lastKnownName,
        Instant congratulatedAt,
        String crate,
        int amount,
        CongratulationStatus status
) {
    public CongratulationRecord withStatus(CongratulationStatus newStatus) {
        return new CongratulationRecord(uuid, lastKnownName, congratulatedAt, crate, amount, newStatus);
    }
}
