package de.otaku.novaguard.config;

import org.bukkit.configuration.file.FileConfiguration;

public record PacketWeights(
        double movement,
        double inventory,
        double interaction,
        double command,
        double chat,
        double customPayload,
        double other
) {
    private static final double MAX_REASONABLE_WEIGHT = 100.0;

    public static PacketWeights from(FileConfiguration config) {
        String base = "packet-protection.packet-weights.";
        return new PacketWeights(
                bounded(config.getDouble(base + "movement", 1.0), 1.0),
                bounded(config.getDouble(base + "inventory", 1.5), 1.5),
                bounded(config.getDouble(base + "interaction", 1.5), 1.5),
                bounded(config.getDouble(base + "command", 2.0), 2.0),
                bounded(config.getDouble(base + "chat", 1.5), 1.5),
                bounded(config.getDouble(base + "custom-payload", 3.0), 3.0),
                bounded(config.getDouble(base + "other", 1.0), 1.0)
        );
    }

    private static double bounded(double value, double fallback) {
        return value > 0.0 && value <= MAX_REASONABLE_WEIGHT && !Double.isNaN(value) && !Double.isInfinite(value) ? value : fallback;
    }
}
