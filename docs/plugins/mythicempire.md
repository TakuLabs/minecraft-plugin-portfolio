# MythicEmpire

Head-system plugin with Paper command bootstrap.

| Field | Value |
| --- | --- |
| Version | `1.0.2` |
| Build | Maven |
| Main class | `de.otaku.headsystem.HeadSystemPlugin` |
| Source files | 28 |
| Tests | 0 |
| Dependencies | Maven · paper-api, Vault |

## Technical Notes

- Category: Other
- Focus: Head-system plugin with Paper command bootstrap
- Public page uses selected excerpts only; full private source is not copied.

## Code Snippets

### 1. src/main/java/de/otaku/staff/StaffInventoryService.java

Service-level logic selected from the plugin source.

```java
    public StaffInventoryService(JavaPlugin plugin, OfflinePlayerStore offlinePlayerStore) {
        this.plugin = plugin;
        this.offlinePlayerStore = offlinePlayerStore;
        this.liveUpdateTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::refreshOnlineSessions, 1L, 1L);
    }
```

### 2. src/main/java/de/otaku/headsystem/head/HeadResetCommand.java

Command handling from the plugin source.

```java
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("mythicempire.headreset")) {
            sender.sendMessage(prefix() + ChatColor.RED + "Dafür hast du keine Rechte.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(prefix() + ChatColor.GRAY + "/" + label + " <Spieler|all>");
            return true;
        }

        if (args[0].equalsIgnoreCase("all")) {
            headManager.resetAllProgress();
            sender.sendMessage(prefix() + ChatColor.GREEN + "Der Head-Fortschritt wurde komplett zurückgesetzt.");
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(prefix() + ChatColor.RED + "Dieser Spieler wurde nicht gefunden.");
            return true;
        }

        headManager.resetOwnerProgress(target.getUniqueId());
        sender.sendMessage(prefix() + ChatColor.GREEN + "Der Head-Fortschritt für " + target.getName() + " wurde zurückgesetzt.");
        return true;
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
