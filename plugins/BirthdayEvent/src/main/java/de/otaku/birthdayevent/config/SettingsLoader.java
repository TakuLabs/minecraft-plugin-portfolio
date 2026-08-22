package de.otaku.birthdayevent.config;

import de.otaku.birthdayevent.event.EventSettings;
import de.otaku.birthdayevent.reward.RewardCrate;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class SettingsLoader {
    private static final EventSettings DEFAULT_EVENT = EventSettings.defaults();

    private SettingsLoader() {
    }

    public static PluginSettings load(FileConfiguration config, Consumer<String> warn) {
        EventSettings event = loadEvent(config, warn);
        List<RewardCrate> crates = loadCrates(config, warn);
        if (crates.isEmpty()) {
            warn.accept("Keine gültige Crate aktiviert. Sichere Defaults werden verwendet.");
            crates = defaultCrates();
        }

        String rewardCommand = config.getString("rewards.command", "crate key give %player% %crate% %amount%");
        if (rewardCommand == null || rewardCommand.isBlank()) {
            warn.accept("rewards.command ist leer. Default-Command wird verwendet.");
            rewardCommand = "crate key give %player% %crate% %amount%";
        }

        return new PluginSettings(
                event,
                config.getBoolean("title.enabled", true),
                positiveInt(config, "title.fade-in-ticks", 10, warn),
                positiveInt(config, "title.stay-ticks", 80, warn),
                positiveInt(config, "title.fade-out-ticks", 20, warn),
                config.getBoolean("firework.enabled", true),
                rewardCommand,
                crates,
                config.getBoolean("logging.rewards", true)
        );
    }

    private static EventSettings loadEvent(FileConfiguration config, Consumer<String> warn) {
        ZoneId zone;
        String configuredZone = config.getString("event.timezone", DEFAULT_EVENT.zone().getId());
        try {
            zone = ZoneId.of(configuredZone);
            if (!DEFAULT_EVENT.zone().equals(zone)) {
                warn.accept("event.timezone muss für dieses Event Europe/Berlin sein. Europe/Berlin wird verwendet.");
                zone = DEFAULT_EVENT.zone();
            }
        } catch (Exception exception) {
            warn.accept("Ungültige event.timezone '" + configuredZone + "'. Europe/Berlin wird verwendet.");
            zone = DEFAULT_EVENT.zone();
        }

        LocalDate date;
        String configuredDate = config.getString("event.date", DEFAULT_EVENT.date().toString());
        try {
            date = LocalDate.parse(configuredDate);
        } catch (DateTimeParseException exception) {
            warn.accept("Ungültiges event.date '" + configuredDate + "'. 2026-08-22 wird verwendet.");
            date = DEFAULT_EVENT.date();
        }

        LocalTime startTime;
        String configuredTime = config.getString("event.start-time", DEFAULT_EVENT.startTime().toString());
        try {
            startTime = LocalTime.parse(configuredTime);
        } catch (DateTimeParseException exception) {
            warn.accept("Ungültige event.start-time '" + configuredTime + "'. 00:00:00 wird verwendet.");
            startTime = DEFAULT_EVENT.startTime();
        }

        int durationHours = positiveInt(config, "event.duration-hours", DEFAULT_EVENT.durationHours(), warn);
        int age = positiveInt(config, "event.age", DEFAULT_EVENT.age(), warn);
        String birthdayPlayerName = config.getString("event.birthday-player-name", DEFAULT_EVENT.birthdayPlayerName());
        if (birthdayPlayerName == null || birthdayPlayerName.isBlank()) {
            warn.accept("event.birthday-player-name ist leer. BirthdayPlayer wird verwendet.");
            birthdayPlayerName = DEFAULT_EVENT.birthdayPlayerName();
        }

        return new EventSettings(zone, date, startTime, durationHours, age, birthdayPlayerName);
    }

    private static List<RewardCrate> loadCrates(FileConfiguration config, Consumer<String> warn) {
        ConfigurationSection section = config.getConfigurationSection("rewards.crates");
        if (section == null) {
            return defaultCrates();
        }

        List<RewardCrate> crates = new ArrayList<>();
        Set<String> keys = section.getKeys(false);
        for (String name : keys) {
            ConfigurationSection crateSection = section.getConfigurationSection(name);
            if (crateSection == null || !crateSection.getBoolean("enabled", true)) {
                continue;
            }

            int min = crateSection.getInt("min-keys", 1);
            int max = crateSection.getInt("max-keys", min);
            int weight = crateSection.getInt("weight", 1);
            if (min < 1) {
                warn.accept("Crate '" + name + "' hat min-keys < 1 und wird übersprungen.");
                continue;
            }
            if (max < min) {
                warn.accept("Crate '" + name + "' hat max-keys < min-keys und wird übersprungen.");
                continue;
            }
            if (weight < 1) {
                warn.accept("Crate '" + name + "' hat weight < 1 und wird übersprungen.");
                continue;
            }
            crates.add(new RewardCrate(name, min, max, weight));
        }
        return crates;
    }

    private static int positiveInt(FileConfiguration config, String path, int fallback, Consumer<String> warn) {
        int value = config.getInt(path, fallback);
        if (value < 1) {
            warn.accept(path + " muss größer als 0 sein. Default " + fallback + " wird verwendet.");
            return fallback;
        }
        return value;
    }

    private static List<RewardCrate> defaultCrates() {
        return List.of(
                new RewardCrate("apex", 1, 3, 1),
                new RewardCrate("dragon", 1, 3, 1),
                new RewardCrate("gott", 1, 2, 1),
                new RewardCrate("infinity", 1, 1, 1),
                new RewardCrate("keyall", 1, 3, 1),
                new RewardCrate("nova", 1, 3, 1),
                new RewardCrate("tool", 1, 1, 1)
        );
    }
}
