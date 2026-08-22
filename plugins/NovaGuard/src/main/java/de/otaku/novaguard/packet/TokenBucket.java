package de.otaku.novaguard.packet;

public final class TokenBucket {
    private final double refillPerSecond;
    private final double capacity;
    private double tokens;
    private long lastRefillNanos;

    public TokenBucket(double refillPerSecond, double capacity, long nowNanos) {
        this.refillPerSecond = refillPerSecond;
        this.capacity = capacity;
        this.tokens = capacity;
        this.lastRefillNanos = nowNanos;
    }

    public boolean tryConsume(double cost, long nowNanos) {
        refill(nowNanos);
        if (tokens >= cost) {
            tokens -= cost;
            return true;
        }
        tokens = Math.max(0.0, tokens - cost);
        return false;
    }

    public double tokens(long nowNanos) {
        refill(nowNanos);
        return tokens;
    }

    public double ratePerSecond() {
        return refillPerSecond;
    }

    private void refill(long nowNanos) {
        if (nowNanos <= lastRefillNanos) {
            return;
        }
        double elapsedSeconds = (nowNanos - lastRefillNanos) / 1_000_000_000.0;
        tokens = Math.min(capacity, tokens + elapsedSeconds * refillPerSecond);
        lastRefillNanos = nowNanos;
    }
}
