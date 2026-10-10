# MythicEmpireLottery

Lottery command flow and reward drawing logic.

| Field | Value |
| --- | --- |
| Version | `1.0.2` |
| Build | Maven |
| Main class | `de.otaku.mythicempire.lottery.MythicEmpireLotteryPlugin` |
| Source files | 2 |
| Tests | 0 |
| Dependencies | Maven · paper-api, Vault |

## Technical Notes

- Category: Economy
- Focus: Lottery command flow and reward drawing logic
- Public page uses selected excerpts only; full private source is not copied.

## Code Snippets

### 1. src/main/java/de/otaku/mythicempire/lottery/OfflinePayouts.java

Representative source excerpt.

```java
    private static void save(JavaPlugin plugin, YamlConfiguration yaml, File file) {
        try {
            yaml.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Offline-Auszahlungen konnten nicht gespeichert werden: " + exception.getMessage());
        }
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
