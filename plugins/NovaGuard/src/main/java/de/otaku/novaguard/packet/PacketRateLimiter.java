package de.otaku.novaguard.packet;

import de.otaku.novaguard.config.PacketMode;
import de.otaku.novaguard.config.PluginSettings;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PacketRateLimiter {
    private final ConcurrentMap<UUID, PacketRateState> states = new ConcurrentHashMap<>();
    private volatile PluginSettings settings;

    public PacketRateLimiter(PluginSettings settings) {
        this.settings = settings;
    }

    public void updateSettings(PluginSettings settings) {
        this.settings = settings;
        states.clear();
    }

    public PacketDecision record(UUID uuid, PacketCategory category, double cost, long nowNanos) {
        PluginSettings current = settings;
        PacketRateState state = states.computeIfAbsent(uuid, ignored -> new PacketRateState(
                current.sustainedLimit(),
                current.burstLimit(),
                current.customPayloadSustainedLimit(),
                current.customPayloadBurstLimit(),
                nowNanos
        ));
        boolean separateCustom = current.customPayloadEnabled() && current.customPayloadSeparateLimit();
        return state.record(
                category,
                cost,
                separateCustom,
                current.violationThreshold(),
                current.violationResetMillis() * 1_000_000L,
                current.packetMode(),
                nowNanos
        );
    }

    public int recordStatus(UUID uuid, PacketCategory category, double cost, long nowNanos) {
        PluginSettings current = settings;
        PacketRateState state = states.computeIfAbsent(uuid, ignored -> new PacketRateState(
                current.sustainedLimit(),
                current.burstLimit(),
                current.customPayloadSustainedLimit(),
                current.customPayloadBurstLimit(),
                nowNanos
        ));
        return state.recordStatus(
                category,
                cost,
                current.customPayloadEnabled() && current.customPayloadSeparateLimit(),
                current.violationThreshold(),
                current.violationResetMillis() * 1_000_000L,
                current.packetMode(),
                nowNanos
        );
    }

    public PacketSnapshot snapshot(UUID uuid, long nowNanos) {
        PacketRateState state = states.get(uuid);
        if (state == null) {
            return new PacketSnapshot(settings.burstLimit(), settings.sustainedLimit(), 0);
        }
        return state.snapshot(nowNanos);
    }

    public void remove(UUID uuid) {
        states.remove(uuid);
    }

    public void clear() {
        states.clear();
    }

    public int stateCount() {
        return states.size();
    }
}
