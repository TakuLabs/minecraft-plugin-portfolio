# OtakuGrabstein

Gravestone handling for player death drops.

| Field | Value |
| --- | --- |
| Version | `1.2.0` |
| Build | Maven |
| Main class | `de.otaku.grabstein.OtakuGrabsteinPlugin` |
| Source files | 1 |
| Tests | 0 |
| Dependencies | Maven · paper-api |

## Technical Notes

- Category: Gameplay
- Focus: Gravestone handling for player death drops

## Code Snippets

### 1. src/main/java/de/otaku/grabstein/OtakuGrabsteinPlugin.java
```java
    public void onEnable() {
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this::animateFloatingHeads, 1L, 1L);
        getLogger().info("OtakuGrabstein wurde aktiviert.");
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
