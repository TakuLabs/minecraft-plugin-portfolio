# CookieClicker

Clicker gameplay systems and persistence.

| Field | Value |
| --- | --- |
| Version | `2.1.2` |
| Build | Maven |
| Main class | `de.otaku.cookieclicker.CookieClickerPlugin` |
| Source files | 199 |
| Tests | 75 |
| Dependencies | Maven · Vault, paper-api |

## Technical Notes

- Category: Gameplay
- Focus: Clicker gameplay systems and persistence

## Code Snippets

### 1. src/test/java/de/otaku/cookieclicker/config/ConfigTestFiles.java
```java
package de.otaku.cookieclicker.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class ConfigTestFiles {
    private ConfigTestFiles() {
    }

    static void writeValidSet(Path directory) throws IOException {
        Files.writeString(directory.resolve("config.yml"), """
            schema-version: 2
            game:
              base-cookies-per-click: 1.0
            combo:
              reset-after-ms: 2000
              clicks-per-tier: 10
              growth-per-tier: 0.05
              max-multiplier: 2.0
            critical:
              base-chance: 0.01
              multiplier: 2.0
            clicker:
              cooldown-ms: 250
            clicker-break:
              sound:
                name: BLOCK_HONEY_BLOCK_BREAK
            anti-autoclicker:
              max-clicks-per-second: 8
              sample-window-ms: 1000
              block-ms: 3000
            hologram:
              player-info-radius: 12.0
```

### 2. src/main/java/de/otaku/cookieclicker/ConfigMigrator.java
```java
    public void migrate() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
            return;
        }

        YamlConfiguration current = YamlConfiguration.loadConfiguration(configFile);
        YamlConfiguration defaults = defaultConfig();
        boolean changed = mergeSection(current, defaults, "");
        if (!changed) {
            return;
        }

        try {
            current.save(configFile);
            plugin.getLogger().info("config.yml wurde automatisch mit neuen CookieClicker-Optionen ergänzt.");
        } catch (IOException exception) {
            plugin.getLogger().severe("Konnte config.yml nicht migrieren: " + exception.getMessage());
        }
    }
```

### 3. src/main/java/de/otaku/cookieclicker/menu/MenuService.java
```java
package de.otaku.cookieclicker.menu;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MenuService {
    private final Set<UUID> purchaseLocks = ConcurrentHashMap.newKeySet();

    public boolean tryLock(UUID playerId) {
        return purchaseLocks.add(playerId);
    }

    public void unlock(UUID playerId) {
        purchaseLocks.remove(playerId);
    }
}

```

[← Back to Plugin Catalog](../plugin-catalog.md)
