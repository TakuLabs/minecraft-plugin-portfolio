# TakuRTP

Teleport queue/random teleport logic.

| Field | Value |
| --- | --- |
| Version | `1.0.2` |
| Build | Maven |
| Main class | `de.takulabs.rtp.TakuRTPPlugin` |
| Source files | 48 |
| Tests | 6 |
| Dependencies | Maven · paper-api |

## Technical Notes

- Category: Utilities
- Focus: Teleport queue/random teleport logic
- Public page uses selected excerpts only; full private source is not copied.

## Code Snippets

### 1. src/main/java/de/takulabs/rtp/command/RTPCommand.java

Command handling from the plugin source.

```java
    public RTPCommand(ConfigManager config, RTPGui gui, RTPService rtpService, CooldownService cooldowns, ProtectionService protection) {
        this.config = config;
        this.gui = gui;
        this.rtpService = rtpService;
        this.cooldowns = cooldowns;
        this.protection = protection;
    }
```

### 2. src/main/java/de/takulabs/rtp/teleport/RTPService.java

Service-level logic selected from the plugin source.

```java
    public RTPService(Plugin plugin, RTPSessionRegistry sessions, CooldownService cooldowns, BukkitCooldownStore cooldownStore, ProtectionService protection, DestinationSearchService search, BukkitSafeLocationValidator validator, WarmupService warmup, TeleportAnimation animation, MessageManager messages) {
        this.plugin = plugin;
        this.sessions = sessions;
        this.cooldowns = cooldowns;
        this.cooldownStore = cooldownStore;
        this.protection = protection;
        this.search = search;
        this.validator = validator;
        this.warmup = warmup;
        this.animation = animation;
        this.messages = messages;
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
