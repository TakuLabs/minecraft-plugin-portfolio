package de.otaku.empirecrates.placeholder;

import de.otaku.empirecrates.EmpireCratesPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class EmpireCratesPlaceholderExpansion extends PlaceholderExpansion {
    private final EmpireCratesPlugin plugin;

    public EmpireCratesPlaceholderExpansion(EmpireCratesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "empirecrates";
    }

    @Override
    public String getAuthor() {
        return "ExamplePlayer";
    }

    @Override
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null) {
            return "";
        }
        return switch (params.toLowerCase()) {
            case "keyall_time", "keyall_countdown" -> plugin.keyall().remainingFormatted();
            case "keyall_seconds" -> String.valueOf(plugin.keyall().remainingSeconds());
            case "keyall_crate" -> plugin.keyall().configuredCrateId();
            case "keyall_enabled" -> plugin.keyall().isEnabled() ? "true" : "false";
            default -> "";
        };
    }
}
