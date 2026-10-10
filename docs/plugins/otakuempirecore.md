# OtakuEmpireCore

Large shared core module with economy and GUI systems.

| Field | Value |
| --- | --- |
| Version | `3.19.0` |
| Build | Maven |
| Main class | `de.otaku.enderchest.OtakuEnderChestPlugin` |
| Source files | 230 |
| Tests | 8 |
| Dependencies | Maven · paper-api, Vault |

## Technical Notes

- Category: Core / Infrastructure
- Focus: Large shared core module with economy and GUI systems

## Code Snippets

### 1. src/main/java/de/otaku/keys/KeyCommand.java
```java
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.message("only-player"));
            return true;
        }

        if (command.getName().equalsIgnoreCase("keyadmin")) {
            if (!player.hasPermission("otakukeys.admin")) {
                player.sendMessage(plugin.message("no-permission"));
                return true;
            }
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                plugin.reloadPlugin();
                player.sendMessage(plugin.message("reloaded"));
                return true;
            }
            KeyMenus.openAdmin(player, plugin);
            return true;
        }

        KeyMenus.openShop(player, plugin);
        return true;
    }
```

### 2. src/main/java/de/otaku/store/StorePlugin.java
```java
    public void onEnable() {
        configFile = ModuleFiles.copyDefault(plugin, "Store/config.yml", new File(ModuleFiles.folder(plugin, "Store"), "config.yml"));
        reload();
        if (plugin.getCommand("store") == null) {
            plugin.getLogger().warning("Command missing in plugin.yml: store");
            return;
        }
        plugin.getCommand("store").setExecutor(this);
    }
```

### 3. src/main/java/de/otaku/daily/DailyCommand.java
```java
    public DailyCommand(OtakuDailyRewardsPlugin plugin, DailyRewardService rewards, DailyGuiListener guiListener) {
        this.plugin = plugin;
        this.rewards = rewards;
        this.guiListener = guiListener;
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
