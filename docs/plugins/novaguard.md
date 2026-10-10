# NovaGuard

Connection and packet safeguards.

| Field | Value |
| --- | --- |
| Version | `1.1.4` |
| Build | Maven |
| Main class | `de.otaku.novaguard.NovaGuard` |
| Source files | 65 |
| Tests | 12 |
| Dependencies | Maven · paper-api, ProtocolLib |

## Technical Notes

- Category: Moderation / Security
- Focus: Connection and packet safeguards
- Public page uses selected excerpts only; full private source is not copied.

## Code Snippets

### 1. src/main/java/de/otaku/novaguard/model/ListEntry.java

Representative source excerpt.

```java
public record ListEntry(
        UUID uuid,
        String key,
        String lastKnownName,
        String reason,
        String addedBy,
        Instant createdAt
) {
    public static ListEntry uuid(UUID uuid, String lastKnownName, String reason, String addedBy, Instant createdAt) {
        return new ListEntry(uuid, uuid.toString(), lastKnownName, reason, addedBy, createdAt);
    }

    public static ListEntry ip(String normalizedIp, String reason, String addedBy, Instant createdAt) {
        return new ListEntry(null, normalizedIp, null, reason, addedBy, createdAt);
    }
}
```

### 2. src/main/java/de/otaku/novaguard/storage/IpStore.java

Persistence or state handling from the plugin source.

```java
    public IpStore(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.saveGuard = new YamlStoreSaveGuard(fileName);
    }
```

### 3. src/main/java/de/otaku/novaguard/storage/UuidStore.java

Persistence or state handling from the plugin source.

```java
    public UuidStore(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.saveGuard = new YamlStoreSaveGuard(fileName);
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
