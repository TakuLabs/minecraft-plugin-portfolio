# BuildServerTools

Server maintenance commands and operational helpers.

| Field | Value |
| --- | --- |
| Version | `1.0.0` |
| Build | Maven |
| Main class | `de.buildservertools.BuildServerToolsPlugin` |
| Source files | 9 |
| Tests | 2 |
| Dependencies | Maven · paper-api |

## Technical Notes

- Category: Utilities
- Focus: Server maintenance commands and operational helpers

## Code Snippets

### 1. src/main/java/de/buildservertools/BuildToolsCommand.java
```java
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String @NotNull [] args
    ) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(Component.text("Dafuer hast du keine Berechtigung.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadBuildServerToolsSettings();
            sender.sendMessage(Component.text("BuildServerTools config neu geladen.", NamedTextColor.GREEN));
            return true;
        }

        sender.sendMessage(Component.text("Verwendung: /" + label + " reload", NamedTextColor.YELLOW));
        return true;
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
