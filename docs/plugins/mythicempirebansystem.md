# MythicEmpireBanSystem

Moderation commands and punishment state.

| Field | Value |
| --- | --- |
| Version | `1.1.9` |
| Build | Maven |
| Main class | `de.mythicempire.bansystem.MythicEmpireBanSystem` |
| Source files | 32 |
| Tests | 2 |
| Dependencies | Maven · paper-api |

## Technical Notes

- Category: Moderation / Security
- Focus: Moderation commands and punishment state

## Code Snippets

### 1. src/main/java/de/mythicempire/bansystem/model/BanRecord.java
```java
public record BanRecord(
        UUID targetUuid,
        String targetName,
        String ipAddress,
        String reason,
        String staff,
        Instant createdAt,
        Instant expiresAt
) {
    public boolean isIpBan() {
        return ipAddress != null && !ipAddress.isBlank();
    }

    public boolean isPermanent() {
        return expiresAt == null;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}
```

### 2. src/main/java/de/mythicempire/bansystem/model/MuteRecord.java
```java
public record MuteRecord(
        UUID targetUuid,
        String targetName,
        String reason,
        String staff,
        Instant createdAt,
        Instant expiresAt
) {
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}
```

[← Back to Plugin Catalog](../plugin-catalog.md)
