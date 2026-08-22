package de.otaku.novaguard.packet;

import de.otaku.novaguard.config.PacketMode;

public final class PacketRateState {
    private TokenBucket mainBucket;
    private TokenBucket customPayloadBucket;
    private int violations;
    private long lastViolationNanos;

    public PacketRateState(double sustainedLimit, double burstLimit, double customSustained, double customBurst, long nowNanos) {
        this.mainBucket = new TokenBucket(sustainedLimit, burstLimit, nowNanos);
        this.customPayloadBucket = new TokenBucket(customSustained, customBurst, nowNanos);
    }

    public synchronized PacketDecision record(
            PacketCategory category,
            double cost,
            boolean separateCustomPayload,
            int threshold,
            long resetNanos,
            PacketMode mode,
            long nowNanos
    ) {
        if (lastViolationNanos > 0 && nowNanos - lastViolationNanos > resetNanos) {
            violations = 0;
            lastViolationNanos = 0;
        }
        boolean allowed = mainBucket.tryConsume(cost, nowNanos);
        if (category == PacketCategory.CUSTOM_PAYLOAD && separateCustomPayload) {
            allowed = customPayloadBucket.tryConsume(1.0, nowNanos) && allowed;
        }
        if (!allowed) {
            violations++;
            lastViolationNanos = nowNanos;
        }
        boolean disconnect = !allowed && violations >= threshold && mode == PacketMode.ENFORCE;
        return new PacketDecision(!allowed, disconnect, mainBucket.tokens(nowNanos), mainBucket.ratePerSecond(), violations);
    }

    public synchronized int recordStatus(
            PacketCategory category,
            double cost,
            boolean separateCustomPayload,
            int threshold,
            long resetNanos,
            PacketMode mode,
            long nowNanos
    ) {
        if (lastViolationNanos > 0 && nowNanos - lastViolationNanos > resetNanos) {
            violations = 0;
            lastViolationNanos = 0;
        }
        boolean allowed = mainBucket.tryConsume(cost, nowNanos);
        if (category == PacketCategory.CUSTOM_PAYLOAD && separateCustomPayload) {
            allowed = customPayloadBucket.tryConsume(1.0, nowNanos) && allowed;
        }
        if (allowed) {
            return 0;
        }
        violations++;
        lastViolationNanos = nowNanos;
        return violations >= threshold && mode == PacketMode.ENFORCE ? -violations : violations;
    }

    public synchronized PacketSnapshot snapshot(long nowNanos) {
        return new PacketSnapshot(mainBucket.tokens(nowNanos), mainBucket.ratePerSecond(), violations);
    }
}
