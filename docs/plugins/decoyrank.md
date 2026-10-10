# DecoyRank

Command-based visible rank decoy utility.

| Field | Value |
| --- | --- |
| Version | `1.0.0` |
| Build | Maven |
| Main class | `otaku.DecoyRankPlugin` |
| Source files | 1 |
| Tests | 0 |
| Dependencies | Maven · paper-api |

## Technical Notes

- Category: Utilities
- Focus: Command-based visible rank decoy utility
- Public page uses selected excerpts only; full private source is not copied.

## Code Snippets

### 1. src/main/java/otaku/DecoyRankPlugin.java

Representative source excerpt.

```java
    private static final List<String> KNOWN_RANKS = List.of(
            "skyempire",
            "skylord",
            "mvip_plus",
            "overlord",
            "ultimate",
            "champion",
            "master",
            "hero",
            "elite",
            "default"
    );
    private static final Map<String, Integer> RANK_TAB_ORDER = createRankTabOrder();

    private LuckPerms luckPerms;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        luckPerms = LuckPermsProvider.get();
        Objects.requireNonNull(getCommand(COMMAND), "Command missing from plugin.yml").setExecutor(this);
        Objects.requireNonNull(getCommand(COMMAND), "Command missing from plugin.yml").setTabCompleter(this);
        warnAboutMissingKnownRanks();
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
