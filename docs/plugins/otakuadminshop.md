# OtakuAdminShop

Inventory shop and economy workflows.

| Field | Value |
| --- | --- |
| Version | `1.0.0` |
| Build | Maven |
| Main class | `de.otaku.adminshop.OtakuAdminShopPlugin` |
| Source files | 11 |
| Tests | 0 |
| Dependencies | Maven · Vault, PlaceholderAPI, paper-api |

## Technical Notes

- Category: Economy
- Focus: Inventory shop and economy workflows
- Public page uses selected excerpts only; full private source is not copied.

## Code Snippets

### 1. src/main/java/de/otaku/adminshop/ShopCommand.java

Command handling from the plugin source.

```java
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("otakuadminshop.reload")) {
                send(sender, "messages.no-permission", null, Map.of());
                return true;
            }
            plugin.reloadComponents();
            send(sender, "messages.reloaded", null, Map.of());
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("open")) {
            if (!sender.hasPermission("otakuadminshop.open.others")) {
                send(sender, "messages.no-permission", null, Map.of());
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("/" + label + " open <player>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                send(sender, "messages.unknown-player", null, Map.of());
                return true;
            }
            plugin.shopManager().openRoot(target);
            return true;
        }

        if (!(sender instanceof Player player)) {
            send(sender, "messages.only-player", null, Map.of());
            return true;
        }
        if (!player.hasPermission("otakuadminshop.use")) {
            send(player, "messages.no-permission", player, Map.of());
            return true;
        }
        plugin.shopManager().openRoot(player);
        return true;
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
