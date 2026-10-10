# EmpireSellStandalone

Standalone sell menu and price-list economy module.

| Field | Value |
| --- | --- |
| Version | `1.0.0` |
| Build | Maven |
| Main class | `de.otaku.empiresell.EmpireSellStandalonePlugin` |
| Source files | 1 |
| Tests | 0 |
| Dependencies | Maven · paper-api, Vault |

## Technical Notes

- Category: Economy
- Focus: Standalone sell menu and price-list economy module

## Code Snippets

### 1. src/main/java/de/otaku/empiresell/EmpireSellStandalonePlugin.java
```java
    public void onEnable() {
        sellChestKey = new NamespacedKey(this, "sellchest");
        actionKey = new NamespacedKey(this, "action");
        saveDefaults();
        reloadEverything();
        setupEconomy();
        Bukkit.getPluginManager().registerEvents(this, this);
        for (String command : List.of("sell", "worth", "empiresell", "sellchest")) {
            if (getCommand(command) != null) {
                getCommand(command).setExecutor(this);
                getCommand(command).setTabCompleter(this);
            }
        }
        startSellChestTask();
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
