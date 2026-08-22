package de.otaku.scoreboard;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public final class TablistService {
    private final OtakuScoreboardPlugin plugin;
    private final TextRenderer textRenderer;

    private boolean enabled;
    private boolean placeholderApiAvailable;
    private List<String> header;
    private List<String> footer;
    private String tabPrefix;
    private String customTabName;
    private String tabSuffix;
    private long suffixRotationInterval;
    private List<String> suffixRotationTexts;
    private boolean rainbowEnabled;
    private int rainbowSpeedTicks;
    private double rainbowColorStep;
    private double rainbowMoveStep;
    private float rainbowSaturation;
    private float rainbowBrightness;
    private boolean rainbowBold;

    public TablistService(OtakuScoreboardPlugin plugin, TextRenderer textRenderer) {
        this.plugin = plugin;
        this.textRenderer = textRenderer;
        reload();
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean("tablist.enabled", true);
        header = config.getStringList("tablist.header");
        footer = config.getStringList("tablist.footer");
        tabPrefix = config.getString("tablist.groups._DEFAULT_.tabprefix", "");
        customTabName = config.getString("tablist.groups._DEFAULT_.customtabname", "%player%");
        tabSuffix = config.getString("tablist.groups._DEFAULT_.tabsuffix", "");
        suffixRotationInterval = Math.max(1L, config.getLong("tablist.animations.suffix_rotation.change-interval", 10000L));
        suffixRotationTexts = config.getStringList("tablist.animations.suffix_rotation.texts");
        rainbowEnabled = config.getBoolean("tablist.rainbow.enabled-by-default", false);
        rainbowSpeedTicks = Math.max(1, config.getInt("tablist.rainbow.speed-ticks", 4));
        rainbowColorStep = Math.max(0.005D, config.getDouble("tablist.rainbow.color-step", 0.045D));
        rainbowMoveStep = Math.max(0.001D, config.getDouble("tablist.rainbow.move-step", 0.012D));
        rainbowSaturation = clampColor(config.getDouble("tablist.rainbow.saturation", 0.72D));
        rainbowBrightness = clampColor(config.getDouble("tablist.rainbow.brightness", 1.0D));
        rainbowBold = config.getBoolean("tablist.rainbow.bold", false);
        placeholderApiAvailable = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        updateAll();
    }

    public void updateAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            update(player);
        }
    }

    public void update(Player player) {
        if (!enabled) {
            player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
            return;
        }

        player.sendPlayerListHeaderAndFooter(renderLines(player, header), renderLines(player, footer));
        player.playerListName(rainbowEnabled ? renderRainbowTabName(player) : renderTabName(player));
    }

    public boolean toggleRainbow() {
        rainbowEnabled = !rainbowEnabled;
        updateAll();
        return rainbowEnabled;
    }

    private Component renderLines(Player player, List<String> lines) {
        Component result = Component.empty();
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) {
                result = result.append(Component.newline());
            }
            result = result.append(textRenderer.render(player, lines.get(index), placeholderApiAvailable));
        }
        return result;
    }

    private Component renderTabName(Player player) {
        Component result = Component.empty();
        result = result.append(textRenderer.render(player, tabPrefix, placeholderApiAvailable));
        result = result.append(textRenderer.render(player, customTabName, placeholderApiAvailable));
        result = result.append(textRenderer.render(player, applyAnimations(player, tabSuffix), placeholderApiAvailable));
        return result;
    }

    private Component renderRainbowTabName(Player player) {
        String text = textRenderer.parse(player, tabPrefix + customTabName, placeholderApiAvailable);
        text = stripFormatting(text);

        Component result = Component.empty();
        long phase = Bukkit.getCurrentTick() / rainbowSpeedTicks;
        int visibleIndex = 0;
        for (int offset = 0; offset < text.length(); offset++) {
            char character = text.charAt(offset);
            if (Character.isWhitespace(character)) {
                result = result.append(Component.text(character));
                continue;
            }

            float hue = (float) ((visibleIndex * rainbowColorStep + phase * rainbowMoveStep) % 1.0D);
            int rgb = java.awt.Color.HSBtoRGB(hue, rainbowSaturation, rainbowBrightness) & 0xFFFFFF;
            Component part = Component.text(character).color(TextColor.color(rgb));
            if (rainbowBold) {
                part = part.decorate(TextDecoration.BOLD);
            }
            result = result.append(part);
            visibleIndex++;
        }
        return result;
    }

    private float clampColor(double value) {
        return (float) Math.max(0.0D, Math.min(1.0D, value));
    }

    private String applyAnimations(Player player, String input) {
        return input.replace("%animation:suffix_rotation%", currentSuffixRotationText(player));
    }

    private String currentSuffixRotationText(Player player) {
        if (suffixRotationTexts.isEmpty()) {
            return "";
        }

        int startIndex = (int) (System.currentTimeMillis() / suffixRotationInterval % suffixRotationTexts.size());
        for (int offset = 0; offset < suffixRotationTexts.size(); offset++) {
            String text = suffixRotationTexts.get((startIndex + offset) % suffixRotationTexts.size());
            String parsed = textRenderer.parse(player, text, placeholderApiAvailable).trim();
            if (!parsed.isEmpty() && !parsed.equals(text) && !containsUnresolvedPlaceholder(parsed)) {
                return text;
            }
        }

        return "";
    }

    private boolean containsUnresolvedPlaceholder(String input) {
        return input.matches(".*%[^% ]+%.*") || input.matches(".*\\{[^{} ]+}.*");
    }

    private String stripFormatting(String input) {
        return input
                .replaceAll("(?i)<[^>]+>", "")
                .replaceAll("(?i)&x(&[0-9a-f]){6}", "")
                .replaceAll("(?i)&#[0-9a-f]{6}", "")
                .replaceAll("(?i)&[0-9a-fk-or]", "")
                .replaceAll("(?i)§x(§[0-9a-f]){6}", "")
                .replaceAll("(?i)§#[0-9a-f]{6}", "")
                .replaceAll("(?i)§[0-9a-fk-or]", "");
    }
}
