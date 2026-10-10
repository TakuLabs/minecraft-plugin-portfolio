# OtakusStats

Player statistics tracking.

| Field | Value |
| --- | --- |
| Version | `1.4.2` |
| Build | Maven |
| Main class | `de.otaku.otakusstats.OtakusStatsPlugin` |
| Source files | 11 |
| Tests | 0 |
| Dependencies | Maven · paper-api, Vault |

## Technical Notes

- Category: Utilities
- Focus: Player statistics tracking
- Public page uses selected excerpts only; full private source is not copied.

## Code Snippets

### 1. src/main/java/de/otaku/otakusstats/StatsCommand.java

Command handling from the plugin source.

```java
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "stats" -> stats(sender, args);
            case "milestones" -> milestones(sender, args);
            case "topstats" -> top(sender, args);
            case "otakusstats" -> admin(sender, args);
            default -> false;
        };
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
