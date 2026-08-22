package de.otaku.novaguard.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class ResourceUtil {
    private ResourceUtil() {
    }

    public static File ensureResource(JavaPlugin plugin, String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        return file;
    }
}
