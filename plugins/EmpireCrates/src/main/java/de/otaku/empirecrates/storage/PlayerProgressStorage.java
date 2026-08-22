package de.otaku.empirecrates.storage;

import de.otaku.empirecrates.EmpireCratesPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class PlayerProgressStorage {
    private final EmpireCratesPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public PlayerProgressStorage(EmpireCratesPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "progress.yml");
    }

    public void load() {
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save progress.yml: " + exception.getMessage());
        }
    }

    public int get(Player player, String crateId) {
        return config.getInt(path(player.getUniqueId(), crateId), 0);
    }

    public int add(Player player, String crateId, int amount) {
        int value = get(player, crateId) + amount;
        config.set(path(player.getUniqueId(), crateId), value);
        save();
        return value;
    }

    public void set(Player player, String crateId, int value) {
        config.set(path(player.getUniqueId(), crateId), Math.max(0, value));
        save();
    }

    private String path(UUID uuid, String crateId) {
        return "players." + uuid + "." + crateId.toLowerCase();
    }
}
