# NovaAH

Player marketplace and item listings.

| Field | Value |
| --- | --- |
| Version | `1.0.2` |
| Build | Maven |
| Main class | `de.otaku.nova.ah.NovaAH` |
| Source files | 21 |
| Tests | 0 |
| Dependencies | Maven · paper-api, Vault |

## Technical Notes

- Category: Economy
- Focus: Player marketplace and item listings
- Public page uses selected excerpts only; full private source is not copied.

## Code Snippets

### 1. src/main/java/de/otaku/nova/ah/commands/AhCommand.java

Command handling from the plugin source.

```java
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player && isOnCooldown(player)) {
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                msg(sender, "<red>Only players can browse the auction house.");
                return true;
            }
            if (!player.hasPermission("novaah.use")) {
                msg(player, "<red>You don't have permission to use the auction house.");
                return true;
            }
            new BrowseMenu(plugin, player, Category.ALL, null, 0).open();
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "sell" -> handleSell(sender, args);
            case "expired", "collect", "collection" -> handleExpired(sender);
            case "cancel" -> handleCancel(sender, args);
            case "search" -> handleSearch(sender, args);
            case "reload" -> handleReload(sender);
            case "blacklist" -> handleBlacklist(sender, args);
            default -> msg(sender, "<red>Usage: /ah [sell <price>|expired|cancel [id]|search <term>]");
        }
        return true;
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
