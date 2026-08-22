package de.otaku.birthdayevent.storage;

import de.otaku.birthdayevent.reward.CongratulationRecord;
import de.otaku.birthdayevent.reward.CongratulationStatus;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class CongratulationStore {
    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration yaml;

    public CongratulationStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "congratulations.yml");
        reload();
    }

    public void reload() {
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    public List<CongratulationRecord> loadRecords() {
        List<CongratulationRecord> records = new ArrayList<>();
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return records;
        }

        for (String uuidString : players.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidString);
                ConfigurationSection section = players.getConfigurationSection(uuidString);
                if (section == null) {
                    continue;
                }
                String name = section.getString("last-known-name", "Unknown");
                Instant at = Instant.parse(section.getString("congratulated-at", Instant.EPOCH.toString()));
                String crate = section.getString("crate", "unknown");
                int amount = section.getInt("amount", 1);
                CongratulationStatus status = CongratulationStatus.valueOf(section.getString("status", "COMPLETED"));
                records.add(new CongratulationRecord(uuid, name, at, crate, amount, status));
            } catch (Exception exception) {
                plugin.getLogger().warning("Ungültiger Eintrag in congratulations.yml: " + uuidString);
            }
        }
        return records;
    }

    public void saveRecord(CongratulationRecord record) {
        String path = "players." + record.uuid();
        yaml.set(path + ".last-known-name", record.lastKnownName());
        yaml.set(path + ".congratulated-at", record.congratulatedAt().toString());
        yaml.set(path + ".crate", record.crate());
        yaml.set(path + ".amount", record.amount());
        yaml.set(path + ".status", record.status().name());
        save();
    }

    public void remove(UUID uuid) {
        yaml.set("players." + uuid, null);
        save();
    }

    public void saveAll(Collection<CongratulationRecord> records) {
        yaml.set("players", null);
        for (CongratulationRecord record : records) {
            String path = "players." + record.uuid();
            yaml.set(path + ".last-known-name", record.lastKnownName());
            yaml.set(path + ".congratulated-at", record.congratulatedAt().toString());
            yaml.set(path + ".crate", record.crate());
            yaml.set(path + ".amount", record.amount());
            yaml.set(path + ".status", record.status().name());
        }
        save();
    }

    private void save() {
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("congratulations.yml konnte nicht gespeichert werden: " + exception.getMessage());
        }
    }
}
