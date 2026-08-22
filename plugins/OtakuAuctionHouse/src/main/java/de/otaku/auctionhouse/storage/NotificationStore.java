package de.otaku.auctionhouse.storage;

import de.otaku.auctionhouse.OtakuAuctionHousePlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class NotificationStore {
    private final OtakuAuctionHousePlugin plugin;
    private final File file;
    private final Map<UUID, List<String>> notifications = new HashMap<>();

    public NotificationStore(OtakuAuctionHousePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "notifications.yml");
    }

    public void load() {
        notifications.clear();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            notifications.put(UUID.fromString(key), new ArrayList<>(config.getStringList(key)));
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        notifications.forEach((uuid, messages) -> config.set(uuid.toString(), messages));
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("notifications.yml konnte nicht gespeichert werden: " + exception.getMessage());
        }
    }

    public void add(UUID playerId, String message) {
        notifications.computeIfAbsent(playerId, ignored -> new ArrayList<>()).add(message);
        save();
    }

    public List<String> pop(UUID playerId) {
        List<String> messages = notifications.remove(playerId);
        if (messages == null) {
            return List.of();
        }
        save();
        return messages;
    }
}
