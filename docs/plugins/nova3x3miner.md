# Nova3x3Miner

Custom 3x3 mining tool behavior.

| Field | Value |
| --- | --- |
| Version | `1.0.3` |
| Build | Maven |
| Main class | `de.otaku.nova.miner.NovaMinerPlugin` |
| Source files | 22 |
| Tests | 0 |
| Dependencies | Maven · paper-api, Vault |

## Technical Notes

- Category: Gameplay
- Focus: Custom 3x3 mining tool behavior
- Public page uses selected excerpts only; full private source is not copied.

## Code Snippets

### 1. src/main/java/de/otaku/nova/miner/sell/SellCommand.java

Command handling from the plugin source.

```java
    public SellCommand(SellService sellService, CommandCooldownManager cooldownManager) {
        this.sellService = sellService;
        this.cooldownManager = cooldownManager;
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
