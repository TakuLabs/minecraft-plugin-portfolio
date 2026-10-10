# OtakuLevelEnchants

Level-based enchantment progression plugin.

| Field | Value |
| --- | --- |
| Version | `1.0.1` |
| Build | Maven |
| Main class | `de.otaku.levelenchants.OtakuLevelEnchantsPlugin` |
| Source files | 5 |
| Tests | 0 |
| Dependencies | Maven · paper-api |

## Technical Notes

- Category: Gameplay
- Focus: Level-based enchantment progression plugin

## Code Snippets

### 1. src/main/java/de/otaku/levelenchants/LevelEnchantCommand.java
```java
    public LevelEnchantCommand(JavaPlugin plugin, LevelEnchantService service) {
        this.plugin = plugin;
        this.service = service;
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
