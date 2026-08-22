package de.otaku.novaguard.storage;

import de.otaku.novaguard.model.ListEntry;
import de.otaku.novaguard.util.ResourceUtil;
import de.otaku.novaguard.util.YamlFiles;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class UuidStore {
    private final JavaPlugin plugin;
    private final String fileName;
    private final ConcurrentMap<UUID, ListEntry> entries = new ConcurrentHashMap<>();
    private File file;

    public UuidStore(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
    }

    public void load() {
        file = ResourceUtil.ensureResource(plugin, fileName);
        YamlConfiguration yaml;
        try {
            yaml = YamlFiles.load(file.toPath());
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not load " + fileName + " as UTF-8: " + ex.getMessage());
            yaml = new YamlConfiguration();
        }
        entries.clear();
        ConfigurationSection section = yaml.getConfigurationSection("entries");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                ConfigurationSection item = section.getConfigurationSection(key);
                String name = item == null ? null : item.getString("last-known-name");
                String reason = item == null ? null : item.getString("reason");
                String addedBy = item == null ? null : item.getString("added-by");
                Instant created = parseInstant(item == null ? null : item.getString("created-at"));
                entries.put(uuid, ListEntry.uuid(uuid, name, reason, addedBy, created));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Ignoring invalid UUID in " + fileName + ": " + key);
            }
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (ListEntry entry : entries.values()) {
            String path = "entries." + entry.uuid();
            yaml.set(path + ".last-known-name", entry.lastKnownName());
            yaml.set(path + ".reason", entry.reason());
            yaml.set(path + ".added-by", entry.addedBy());
            yaml.set(path + ".created-at", entry.createdAt().toString());
        }
        try {
            YamlFiles.save(yaml, file.toPath());
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save " + fileName + ": " + ex.getMessage());
        }
    }

    public void add(UUID uuid, String name, String reason, String addedBy) {
        entries.put(uuid, ListEntry.uuid(uuid, name, reason, addedBy, Instant.now()));
        save();
    }

    public boolean remove(UUID uuid) {
        boolean removed = entries.remove(uuid) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public boolean contains(UUID uuid) {
        return entries.containsKey(uuid);
    }

    public Optional<ListEntry> find(UUID uuid) {
        return Optional.ofNullable(entries.get(uuid));
    }

    public Optional<ListEntry> findByName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return entries.values().stream()
                .filter(entry -> entry.lastKnownName() != null && entry.lastKnownName().toLowerCase(Locale.ROOT).equals(lower))
                .findFirst();
    }

    public List<ListEntry> list() {
        return new ArrayList<>(entries.values());
    }

    private static Instant parseInstant(String value) {
        if (value == null) {
            return Instant.now();
        }
        try {
            return Instant.parse(value);
        } catch (RuntimeException ex) {
            return Instant.now();
        }
    }
}
