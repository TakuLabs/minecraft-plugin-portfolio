# OtakuSuite

Marriage/social feature suite for Paper servers.

| Field | Value |
| --- | --- |
| Version | `1.2.3` |
| Build | Maven |
| Main class | `de.otaku.marry.OtakuSuitePlugin` |
| Source files | 9 |
| Tests | 0 |
| Dependencies | Maven · paper-api, Vault |

## Technical Notes

- Category: Core / Infrastructure
- Focus: Marriage/social feature suite for Paper servers
- Public page uses selected excerpts only; full private source is not copied.

## Code Snippets

### 1. src/main/java/de/otaku/marry/troll/TrollCommand.java

Command handling from the plugin source.

```java
    public TrollCommand(OtakuSuitePlugin plugin, TrollGui gui, boolean admin) {
        this.plugin = plugin;
        this.gui = gui;
        this.admin = admin;
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
