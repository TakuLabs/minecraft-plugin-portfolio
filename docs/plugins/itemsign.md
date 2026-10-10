# ItemSign

Sign-based item display and interaction helper.

| Field | Value |
| --- | --- |
| Version | `1.6.1` |
| Build | Maven |
| Main class | `de.otaakuu.itemsign.ItemSignPlugin` |
| Source files | 13 |
| Tests | 0 |
| Dependencies | Maven · paper-api |

## Technical Notes

- Category: Utilities
- Focus: Sign-based item display and interaction helper

## Code Snippets

### 1. src/main/java/de/otaakuu/itemsign/LoreCommand.java
```java
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (!sender.hasPermission(LORE_PERMISSION)) {
            sender.sendMessage(parse(plugin.messages().noPermission()));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(parse(plugin.messages().onlyPlayers()));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(parse(plugin.messages().loreUsage()));
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            player.sendMessage(parse(plugin.messages().noItem()));
            return true;
        }

        String action = args[0].toLowerCase();
        return switch (action) {
            case "add" -> addLore(player, item, args);
            case "set" -> setLore(player, item, args);
            case "clear" -> clearLore(player, item);
            default -> {
                player.sendMessage(parse(plugin.messages().loreUsage()));
                yield true;
            }
        };
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
