package de.takulabs.smpshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class Text {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private Text() {
    }

    static Component component(SMPShopPlugin plugin, Player player, String text) {
        return LEGACY.deserialize(plugin.placeholderHook().apply(player, text == null ? "" : text));
    }

    static List<Component> components(SMPShopPlugin plugin, Player player, List<String> lines, Map<String, String> replacements) {
        List<Component> components = new ArrayList<>();
        for (String line : lines) {
            components.add(component(plugin, player, replace(line, replacements)));
        }
        return components;
    }

    static String replace(String text, Map<String, String> replacements) {
        String result = text == null ? "" : text;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
}

