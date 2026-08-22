package de.otaku.novaguard.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;

public final class PluginSettings {
    private static final int MAX_REASONABLE_ACCOUNTS_PER_IP = 10_000;
    private static final int MAX_REASONABLE_SECONDS = 86_400;
    private static final double MAX_REASONABLE_PACKET_LIMIT = 100_000.0;

    private final boolean ipLimitEnabled;
    private final int maxAccountsPerIp;
    private final long reservationTimeoutMillis;
    private final boolean packetProtectionEnabled;
    private final PacketMode packetMode;
    private final double sustainedLimit;
    private final double burstLimit;
    private final int violationThreshold;
    private final long violationResetMillis;
    private final boolean customPayloadEnabled;
    private final boolean customPayloadSeparateLimit;
    private final double customPayloadSustainedLimit;
    private final double customPayloadBurstLimit;
    private final PacketWeights packetWeights;
    private final boolean debug;
    private final boolean logFullIps;
    private final boolean saveLastKnownPlayerName;
    private final int historyRetentionDays;
    private final boolean batchWrites;

    private PluginSettings(FileConfiguration config, JavaPlugin plugin) {
        ipLimitEnabled = config.getBoolean("ip-limit.enabled", true);
        maxAccountsPerIp = boundedInt(config, plugin, "ip-limit.max-accounts-per-ip", 15, MAX_REASONABLE_ACCOUNTS_PER_IP);
        reservationTimeoutMillis = boundedInt(config, plugin, "ip-limit.reservation-timeout-seconds", 30, MAX_REASONABLE_SECONDS) * 1000L;
        packetProtectionEnabled = config.getBoolean("packet-protection.enabled", true);
        packetMode = parseMode(config.getString("packet-protection.mode", "MONITOR"), plugin);
        sustainedLimit = boundedDouble(config, plugin, "packet-protection.sustained-limit", 450.0, MAX_REASONABLE_PACKET_LIMIT);
        burstLimit = boundedDouble(config, plugin, "packet-protection.burst-limit", 800.0, MAX_REASONABLE_PACKET_LIMIT);
        violationThreshold = boundedInt(config, plugin, "packet-protection.violation-threshold", 3, 1000);
        violationResetMillis = boundedInt(config, plugin, "packet-protection.violation-reset-seconds", 10, MAX_REASONABLE_SECONDS) * 1000L;
        customPayloadEnabled = config.getBoolean("packet-protection.custom-payload.enabled", true);
        customPayloadSeparateLimit = config.getBoolean("packet-protection.custom-payload.separate-limit", true);
        customPayloadSustainedLimit = boundedDouble(config, plugin, "packet-protection.custom-payload.sustained-limit", 80.0, MAX_REASONABLE_PACKET_LIMIT);
        customPayloadBurstLimit = boundedDouble(config, plugin, "packet-protection.custom-payload.burst-limit", 160.0, MAX_REASONABLE_PACKET_LIMIT);
        packetWeights = PacketWeights.from(config);
        debug = config.getBoolean("logging.debug", false);
        logFullIps = config.getBoolean("logging.log-full-ips", false);
        saveLastKnownPlayerName = config.getBoolean("storage.save-last-known-player-name", true);
        batchWrites = config.getBoolean("storage.batch-writes", true);
        historyRetentionDays = boundedInt(config, plugin, "connection-history.retention-days", 30, 3650);
    }

    public static PluginSettings load(JavaPlugin plugin) {
        return new PluginSettings(plugin.getConfig(), plugin);
    }

    private static int boundedInt(FileConfiguration config, JavaPlugin plugin, String path, int fallback, int max) {
        int value = config.getInt(path, fallback);
        if (value < 1 || value > max) {
            plugin.getLogger().warning("Invalid value for " + path + ", using " + fallback);
            return fallback;
        }
        return value;
    }

    private static double boundedDouble(FileConfiguration config, JavaPlugin plugin, String path, double fallback, double max) {
        double value = config.getDouble(path, fallback);
        if (value <= 0.0 || value > max || Double.isNaN(value) || Double.isInfinite(value)) {
            plugin.getLogger().warning("Invalid value for " + path + ", using " + fallback);
            return fallback;
        }
        return value;
    }

    private static PacketMode parseMode(String raw, JavaPlugin plugin) {
        try {
            return PacketMode.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Invalid packet protection mode '" + raw + "', using MONITOR");
            return PacketMode.MONITOR;
        }
    }

    public boolean ipLimitEnabled() { return ipLimitEnabled; }
    public int maxAccountsPerIp() { return maxAccountsPerIp; }
    public long reservationTimeoutMillis() { return reservationTimeoutMillis; }
    public boolean packetProtectionEnabled() { return packetProtectionEnabled; }
    public PacketMode packetMode() { return packetMode; }
    public double sustainedLimit() { return sustainedLimit; }
    public double burstLimit() { return burstLimit; }
    public int violationThreshold() { return violationThreshold; }
    public long violationResetMillis() { return violationResetMillis; }
    public boolean customPayloadEnabled() { return customPayloadEnabled; }
    public boolean customPayloadSeparateLimit() { return customPayloadSeparateLimit; }
    public double customPayloadSustainedLimit() { return customPayloadSustainedLimit; }
    public double customPayloadBurstLimit() { return customPayloadBurstLimit; }
    public PacketWeights packetWeights() { return packetWeights; }
    public boolean debug() { return debug; }
    public boolean logFullIps() { return logFullIps; }
    public boolean saveLastKnownPlayerName() { return saveLastKnownPlayerName; }
    public int historyRetentionDays() { return historyRetentionDays; }
    public boolean batchWrites() { return batchWrites; }
}
