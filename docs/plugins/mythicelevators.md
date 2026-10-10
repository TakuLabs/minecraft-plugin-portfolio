# MythicElevators

Elevator blocks, movement and configuration.

| Field | Value |
| --- | --- |
| Version | `1.0.8` |
| Build | Maven |
| Main class | `de.takulabs.mythicelevators.MythicElevatorsPlugin` |
| Source files | 61 |
| Tests | 15 |
| Dependencies | Maven · paper-api, Vault |

## Technical Notes

- Category: Gameplay
- Focus: Elevator blocks, movement and configuration
- Public page uses selected excerpts only; full private source is not copied.

## Code Snippets

### 1. src/main/java/de/takulabs/mythicelevators/config/WorldMode.java

Configuration handling from the plugin source.

```java
package de.takulabs.mythicelevators.config;

public enum WorldMode {
    WHITELIST,
    BLACKLIST
}

```

### 2. src/main/java/de/takulabs/mythicelevators/config/WorldRules.java

Configuration handling from the plugin source.

```java
public record WorldRules(WorldMode mode, Set<String> worlds) {
    public boolean allows(World world) {
        return allows(world.getName());
    }

    public boolean allows(String worldName) {
        boolean listed = worlds.contains(worldName);
        return mode == WorldMode.WHITELIST ? listed : !listed;
    }
}
```

### 3. src/main/java/de/takulabs/mythicelevators/config/SoundEffect.java

Configuration handling from the plugin source.

```java
package de.takulabs.mythicelevators.config;

import org.bukkit.Sound;

public record SoundEffect(boolean enabled, Sound sound, float volume, float pitch) {
}

```

[← Back to Plugin Catalog](../plugin-catalog.md)
