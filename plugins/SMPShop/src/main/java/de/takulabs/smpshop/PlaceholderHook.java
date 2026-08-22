package de.takulabs.smpshop;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

final class PlaceholderHook {
    private final Method setPlaceholders;

    PlaceholderHook(SMPShopPlugin plugin) {
        Method method = null;
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                Class<?> placeholderApi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                method = placeholderApi.getMethod("setPlaceholders", Player.class, String.class);
            } catch (ReflectiveOperationException exception) {
                plugin.getLogger().warning("PlaceholderAPI hook failed: " + exception.getMessage());
            }
        }
        this.setPlaceholders = method;
    }

    String apply(Player player, String text) {
        if (player == null || setPlaceholders == null) {
            return text;
        }
        try {
            return (String) setPlaceholders.invoke(null, player, text);
        } catch (ReflectiveOperationException exception) {
            return text;
        }
    }
}

