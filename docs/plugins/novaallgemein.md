# NovaAllgemein

General server utility commands and listeners.

| Field | Value |
| --- | --- |
| Version | `1.0.2` |
| Build | Maven |
| Main class | `de.otaku.nova.allgemein.NovaAllgemein` |
| Source files | 50 |
| Tests | 0 |
| Dependencies | Maven · paper-api, Vault |

## Technical Notes

- Category: Other
- Focus: General server utility commands and listeners

## Code Snippets

### 1. src/main/java/de/otaku/nova/allgemein/region/RegionStore.java
```java
    public RegionStore(NovaAllgemein plugin, String fileName) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), fileName);
        load();
    }
```

### 2. src/main/java/de/otaku/nova/allgemein/commands/WertCommand.java
```java
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Text.legacy("&cDieser Befehl kann nur von Spielern verwendet werden."));
            return true;
        }
        if (!CooldownGuard.check(plugin, player, "wert")) {
            return true;
        }
        if (plugin.getShopCatalog().getItems().isEmpty()) {
            player.sendMessage(Text.legacy("&cEs sind keine Items in der Preisliste (shop-prices.yml) hinterlegt."));
            return true;
        }

        int deduction = plugin.getConfig().getInt("wert.deduction", 20);
        WertGui.open(player, plugin.getShopCatalog().getItems(), 0, deduction, WertCategory.ALL);
        return true;
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
