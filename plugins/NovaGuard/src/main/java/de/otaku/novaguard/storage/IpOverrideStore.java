package de.otaku.novaguard.storage;

import de.otaku.novaguard.util.IpNormalizer;
import de.otaku.novaguard.util.ResourceUtil;
import de.otaku.novaguard.util.YamlFiles;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class IpOverrideStore {
    private final JavaPlugin plugin;
    private final ConcurrentMap<String, Integer> overrides = new ConcurrentHashMap<>();
    private File file;

    public IpOverrideStore(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        file = ResourceUtil.ensureResource(plugin, "ip-overrides.yml");
        YamlConfiguration yaml;
        try {
            yaml = YamlFiles.load(file.toPath());
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not load ip-overrides.yml as UTF-8: " + ex.getMessage());
            yaml = new YamlConfiguration();
        }
        overrides.clear();
        ConfigurationSection section = yaml.getConfigurationSection("ip-overrides");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            Optional<String> normalized = IpNormalizer.normalize(key);
            int limit = section.getInt(key, -1);
            if (normalized.isEmpty() || limit < 0) {
                plugin.getLogger().warning("Ignoring invalid IP override: " + key);
                continue;
            }
            overrides.put(normalized.get(), limit);
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        overrides.forEach((ip, limit) -> yaml.set("ip-overrides." + ip, limit));
        try {
            YamlFiles.save(yaml, file.toPath());
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save ip-overrides.yml: " + ex.getMessage());
        }
    }

    public void set(String normalizedIp, int limit) {
        overrides.put(normalizedIp, limit);
        save();
    }

    public boolean reset(String normalizedIp) {
        boolean removed = overrides.remove(normalizedIp) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public Optional<Integer> get(String normalizedIp) {
        return Optional.ofNullable(overrides.get(normalizedIp));
    }

    public Set<String> keys() {
        return new HashSet<>(overrides.keySet());
    }
}
