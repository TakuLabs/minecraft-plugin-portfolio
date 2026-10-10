# OtakuKeys

Key item and reward command handling.

| Field | Value |
| --- | --- |
| Version | `1.0.0` |
| Build | Maven |
| Main class | `de.otaku.keys.OtakuKeysPlugin` |
| Source files | 13 |
| Tests | 0 |
| Dependencies | Maven · paper-api, Vault |

## Technical Notes

- Category: Economy
- Focus: Key item and reward command handling
- Public page uses selected excerpts only; full private source is not copied.

## Code Snippets

### 1. src/main/java/de/otaku/keys/KeyCommand.java

Command handling from the plugin source.

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
                player.sendMessage(Text.color("&8[&bOtakuKeys&8] &aConfig neu geladen."));
                return true;
            }
            KeyMenus.openAdmin(player, plugin);
            return true;
        }

        KeyMenus.openShop(player, plugin);
        return true;
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
