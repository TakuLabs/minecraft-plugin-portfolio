package de.otaku.empirecrates.storage;

import de.otaku.empirecrates.EmpireCratesPlugin;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PlacedCrateStorage {
    private final EmpireCratesPlugin plugin;
    private final File file;
    private final Map<String, String> crates = new HashMap<>();
    private final Map<String, Float> rotations = new HashMap<>();

    public PlacedCrateStorage(EmpireCratesPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "placed-crates.yml");
    }

    public void load() {
        crates.clear();
        rotations.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("crates");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                crates.put(key, section.getString(key, ""));
            }
        }
        ConfigurationSection rotationSection = config.getConfigurationSection("rotations");
        if (rotationSection != null) {
            for (String key : rotationSection.getKeys(false)) {
                rotations.put(key, (float) rotationSection.getDouble(key, 0.0D));
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        crates.forEach((key, crateId) -> config.set("crates." + key, crateId));
        rotations.forEach((key, rotation) -> config.set("rotations." + key, rotation));
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save placed-crates.yml: " + exception.getMessage());
        }
    }

    public void set(Block block, String crateId) {
        set(block, crateId, 0.0F);
    }

    public void set(Block block, String crateId, float yaw) {
        String key = key(block);
        crates.put(key, crateId);
        rotations.put(key, normalizeYaw(yaw));
        save();
    }

    public String get(Block block) {
        return crates.get(key(block));
    }

    public float rotation(Block block) {
        return rotations.getOrDefault(key(block), 0.0F);
    }

    public void remove(Block block) {
        String key = key(block);
        crates.remove(key);
        rotations.remove(key);
        save();
    }

    public void removeKeys(Collection<String> keys) {
        if (keys.isEmpty()) {
            return;
        }
        keys.forEach(crates::remove);
        keys.forEach(rotations::remove);
        save();
    }

    public Map<String, String> all() {
        return Map.copyOf(crates);
    }

    public Optional<Location> location(String key) {
        String[] parts = key.split(",");
        if (parts.length != 4) {
            return Optional.empty();
        }
        try {
            World world = plugin.getServer().getWorld(UUID.fromString(parts[0]));
            if (world == null) {
                return Optional.empty();
            }
            return Optional.of(new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3])));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private String key(Block block) {
        Location location = block.getLocation();
        World world = location.getWorld();
        return world.getUID() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private float normalizeYaw(float yaw) {
        float normalized = yaw % 360.0F;
        return normalized < 0.0F ? normalized + 360.0F : normalized;
    }
}
