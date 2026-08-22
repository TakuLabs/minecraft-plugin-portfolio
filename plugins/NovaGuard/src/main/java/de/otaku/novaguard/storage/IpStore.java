package de.otaku.novaguard.storage;

import de.otaku.novaguard.model.ListEntry;
import de.otaku.novaguard.util.IpNormalizer;
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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class IpStore {
    private final JavaPlugin plugin;
    private final String fileName;
    private final ConcurrentMap<String, ListEntry> entries = new ConcurrentHashMap<>();
    private File file;

    public IpStore(JavaPlugin plugin, String fileName) {
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
            Optional<String> normalized = IpNormalizer.normalize(key);
            if (normalized.isEmpty()) {
                plugin.getLogger().warning("Ignoring invalid IP in " + fileName + ": " + key);
                continue;
            }
            ConfigurationSection item = section.getConfigurationSection(key);
            String reason = item == null ? null : item.getString("reason");
            String addedBy = item == null ? null : item.getString("added-by");
            Instant created = parseInstant(item == null ? null : item.getString("created-at"));
            entries.put(normalized.get(), ListEntry.ip(normalized.get(), reason, addedBy, created));
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (ListEntry entry : entries.values()) {
            String path = "entries." + entry.key();
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

    public void add(String normalizedIp, String reason, String addedBy) {
        entries.put(normalizedIp, ListEntry.ip(normalizedIp, reason, addedBy, Instant.now()));
        save();
    }

    public boolean remove(String normalizedIp) {
        boolean removed = entries.remove(normalizedIp) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public boolean contains(String normalizedIp) {
        return entries.containsKey(normalizedIp);
    }

    public Optional<ListEntry> find(String normalizedIp) {
        return Optional.ofNullable(entries.get(normalizedIp));
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
