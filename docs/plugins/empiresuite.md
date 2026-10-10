# EmpireSuite

Minimal suite bootstrap for shared server modules.

| Field | Value |
| --- | --- |
| Version | `1.0.0` |
| Build | Maven |
| Main class | `de.otaku.mythicempire.MythicEmpirePlugin` |
| Source files | 1 |
| Tests | 0 |
| Dependencies | Maven · paper-api, Vault |

## Technical Notes

- Category: Core / Infrastructure
- Focus: Minimal suite bootstrap for shared server modules

## Code Snippets

### 1. src/main/java/de/otaku/mythicempire/MythicEmpirePlugin.java
```java
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        loadMessages();
        dataFile = new File(getDataFolder(), "data.yml");
        data = YamlConfiguration.loadConfiguration(dataFile);
        loadData();
        setupEconomy();
        Bukkit.getPluginManager().registerEvents(this, this);
        updateBountyGrowthTask();
        getLogger().info("EmpireSuite wurde aktiviert.");
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
