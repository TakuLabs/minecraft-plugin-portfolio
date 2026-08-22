package de.otaku.birthdayevent.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;

public final class EventStateStore {
    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration yaml;

    public EventStateStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "event-state.yml");
        reload();
    }

    public void reload() {
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public boolean isEventTriggered() {
        return yaml.getBoolean("event.global-triggered", yaml.getBoolean("event-triggered", false));
    }

    public void markEventTriggered(ZonedDateTime triggeredAt, ZonedDateTime eventStart) {
        yaml.set("event.global-triggered", true);
        yaml.set("event.triggered-at", triggeredAt.toOffsetDateTime().toString());
        yaml.set("event.event-start", eventStart.toOffsetDateTime().toString());
        yaml.set("event.timezone", eventStart.getZone().getId());
        yaml.set("event-triggered", null);
        yaml.set("triggered-at", null);
        yaml.set("event-start", null);
        save();
    }

    public void save() {
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("event-state.yml konnte nicht gespeichert werden: " + exception.getMessage());
        }
    }
}
