# OtakusEnderChestSystem

Ender chest persistence and restore behavior.

| Field | Value |
| --- | --- |
| Version | `1.2.6` |
| Build | Maven |
| Main class | `de.otaku.enderchest.OtakuEnderChestPlugin` |
| Source files | 6 |
| Tests | 1 |
| Dependencies | Maven · paper-api |

## Technical Notes

- Category: Gameplay
- Focus: Ender chest persistence and restore behavior

## Code Snippets

### 1. src/main/java/de/otaku/enderchest/CombatCommandPolicy.java
```java
    private static final Set<String> BLOCKED_COMMANDS = Set.of(
            "ec",
            "enderchest",
            "echest",
            "ender_chest",
            "eenderchest",
            "eechest",
            "eec",
            "ecsee",
            "otakuenderchest",
            "oec"
    );

    private CombatCommandPolicy() {
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
