# ThrowableSpawnEggs

Throwable spawn egg gameplay mechanic.

| Field | Value |
| --- | --- |
| Version | `1.0.2` |
| Build | Maven |
| Main class | `de.otaku.throwablespawneggs.ThrowableSpawnEggsPlugin` |
| Source files | 1 |
| Tests | 0 |
| Dependencies | Maven · paper-api |

## Technical Notes

- Category: Gameplay
- Focus: Throwable spawn egg gameplay mechanic
- Public page uses selected excerpts only; full private source is not copied.

## Code Snippets

### 1. src/main/java/de/otaku/throwablespawneggs/ThrowableSpawnEggsPlugin.java

Representative source excerpt.

```java
    public void onEnable() {
        entityTypeKey = new NamespacedKey(this, "entity_type");
        getServer().getPluginManager().registerEvents(this, this);
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
