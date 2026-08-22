package de.otaku.scoreboard;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.plugin.Plugin;

public final class TextRenderer {
    private static final String ITEMS_ADDER_FONT_IMAGE_WRAPPER = "dev.lone.itemsadder.api.FontImages.FontImageWrapper";
    private static final Pattern AMPERSAND_HEX_PATTERN = Pattern.compile("(?i)&#([a-f0-9]{6})");
    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("(?i)[&§]x([&§][a-f0-9]){6}");

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final OtakuScoreboardPlugin plugin;

    private Method itemsAdderPermissibleReplaceMethod;
    private Method itemsAdderStringReplaceMethod;
    private boolean itemsAdderLookupDone;

    public TextRenderer(OtakuScoreboardPlugin plugin) {
        this.plugin = plugin;
    }

    public Component render(Player player, String input, boolean placeholderApiAvailable) {
        return miniMessage.deserialize(legacyToMiniMessage(parse(player, input, placeholderApiAvailable)));
    }

    public String parse(Player player, String input, boolean placeholderApiAvailable) {
        String parsed = input == null ? "" : input;
        parsed = parsed
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%max_players%", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("%player%", player.getName())
                .replace("%world%", worldName(player.getWorld()))
                .replace("%ping%", String.valueOf(player.getPing()))
                .replace("%player_ping%", String.valueOf(player.getPing()))
                .replace("%player_ping_colored%", coloredPing(player.getPing()))
                .replace("%health%", String.valueOf((int) Math.ceil(player.getHealth())))
                .replace("%health_rounded%", String.valueOf((int) Math.round(player.getHealth())));

        parsed = replacePlaceholderApiPlaceholders(player, parsed);

        parsed = replaceItemsAdderFontImages(player, parsed);
        parsed = replacePlaceholderApiPlaceholders(player, parsed);

        return parsed;
    }

    private String worldName(World world) {
        return world == null ? "" : world.getName();
    }

    private String replacePlaceholderApiPlaceholders(Player player, String input) {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return input;
        }

        int passes = Math.max(1, plugin.getConfig().getInt("integrations.placeholder-passes", 3));
        String parsed = input;
        for (int index = 0; index < passes; index++) {
            String before = parsed;
            if (plugin.getConfig().getBoolean("integrations.placeholderapi", true)) {
                parsed = PlaceholderAPI.setPlaceholders(player, parsed);
                parsed = PlaceholderAPI.setRelationalPlaceholders(player, player, parsed);
            }
            if (plugin.getConfig().getBoolean("integrations.placeholderapi-brackets", true)) {
                parsed = PlaceholderAPI.setBracketPlaceholders(player, parsed);
            }
            if (parsed.equals(before)) {
                break;
            }
        }
        return parsed;
    }

    private String coloredPing(int ping) {
        if (ping <= 60) {
            return "&a" + ping;
        }
        if (ping <= 120) {
            return "&e" + ping;
        }
        if (ping <= 200) {
            return "&6" + ping;
        }
        return "&c" + ping;
    }

    private String replaceItemsAdderFontImages(Player player, String input) {
        if (!plugin.getConfig().getBoolean("integrations.itemsadder-font-images", true)) {
            return input;
        }

        Plugin itemsAdder = Bukkit.getPluginManager().getPlugin("ItemsAdder");
        if (itemsAdder == null || !itemsAdder.isEnabled()) {
            return input;
        }

        loadItemsAdderMethods();
        try {
            if (itemsAdderPermissibleReplaceMethod != null) {
                return (String) itemsAdderPermissibleReplaceMethod.invoke(null, player, input);
            }
            if (itemsAdderStringReplaceMethod != null) {
                return (String) itemsAdderStringReplaceMethod.invoke(null, input);
            }
        } catch (IllegalAccessException | InvocationTargetException exception) {
            plugin.getLogger().warning("ItemsAdder font image placeholders konnten nicht ersetzt werden: " + exception.getMessage());
        }

        return input;
    }

    private void loadItemsAdderMethods() {
        if (itemsAdderLookupDone) {
            return;
        }
        itemsAdderLookupDone = true;

        try {
            Class<?> wrapperClass = Class.forName(ITEMS_ADDER_FONT_IMAGE_WRAPPER);
            itemsAdderPermissibleReplaceMethod = wrapperClass.getMethod("replaceFontImages", Permissible.class, String.class);
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            itemsAdderPermissibleReplaceMethod = null;
        }

        try {
            Class<?> wrapperClass = Class.forName(ITEMS_ADDER_FONT_IMAGE_WRAPPER);
            itemsAdderStringReplaceMethod = wrapperClass.getMethod("replaceFontImages", String.class);
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            itemsAdderStringReplaceMethod = null;
        }
    }

    private String legacyToMiniMessage(String input) {
        String converted = convertHexColors(input.replace('§', '&'));
        return converted
                .replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&k", "<obfuscated>")
                .replace("&l", "<bold>")
                .replace("&m", "<strikethrough>")
                .replace("&n", "<underlined>")
                .replace("&o", "<italic>")
                .replace("&r", "<reset>");
    }

    private String convertHexColors(String input) {
        Matcher legacyMatcher = LEGACY_HEX_PATTERN.matcher(input);
        StringBuffer legacyBuffer = new StringBuffer();
        while (legacyMatcher.find()) {
            String hex = legacyMatcher.group().replace("&x", "").replace("§x", "").replace("&", "").replace("§", "");
            legacyMatcher.appendReplacement(legacyBuffer, "<#" + hex + ">");
        }
        legacyMatcher.appendTail(legacyBuffer);

        Matcher ampersandMatcher = AMPERSAND_HEX_PATTERN.matcher(legacyBuffer.toString());
        StringBuffer ampersandBuffer = new StringBuffer();
        while (ampersandMatcher.find()) {
            ampersandMatcher.appendReplacement(ampersandBuffer, "<#" + ampersandMatcher.group(1) + ">");
        }
        ampersandMatcher.appendTail(ampersandBuffer);
        return ampersandBuffer.toString();
    }
}
