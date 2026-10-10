# MythicSpawner

Spawner tooling and persistent item/block state.

| Field | Value |
| --- | --- |
| Version | `1.0.0` |
| Build | Maven |
| Main class | `de.takulabs.mythicspawner.MythicSpawnerPlugin` |
| Source files | 37 |
| Tests | 8 |
| Dependencies | Maven · paper-api, Vault |

## Technical Notes

- Category: Gameplay
- Focus: Spawner tooling and persistent item/block state
- Public page uses selected excerpts only; full private source is not copied.

## Code Snippets

### 1. src/main/java/de/takulabs/mythicspawner/ConfigService.java

Service-level logic selected from the plugin source.

```java
    public void reload() {
        plugin.reloadConfig();
        spawnerDropChance = chance("spawner-pickaxe.drop-chance", 30.0);
        destroyOnFailureChance = chance("conversion.destroy-on-failure-chance", 50.0);
        debug = plugin.getConfig().getBoolean("debug", false);
        guiTitle = plugin.getConfig().getString("gui.title", "<gradient:#f7c948:#9b5cff>Mythic Spawner</gradient>");
        loadMessages();
        loadTitles();
        loadRarities();
        loadRecipe();
        loadVirtualProtection();
        loadMobs();
    }
```

### 2. src/main/java/de/takulabs/mythicspawner/RecipeService.java

Service-level logic selected from the plugin source.

```java
    public RecipeService(JavaPlugin plugin, PluginKeys keys, ConfigService config, ItemFactory itemFactory) {
        this.plugin = plugin;
        this.keys = keys;
        this.config = config;
        this.itemFactory = itemFactory;
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
