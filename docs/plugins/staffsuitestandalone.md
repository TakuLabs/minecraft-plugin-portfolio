# StaffSuiteStandalone

Standalone staff tools and moderation utility suite.

| Field | Value |
| --- | --- |
| Version | `1.0.0` |
| Build | Maven |
| Main class | `de.otaku.staffsuite.StaffSuiteStandalonePlugin` |
| Source files | 20 |
| Tests | 0 |
| Dependencies | Maven · paper-api |

## Technical Notes

- Category: Core / Infrastructure
- Focus: Standalone staff tools and moderation utility suite

## Code Snippets

### 1. src/main/java/de/otaku/staff/StaffInventoryService.java
```java
    public StaffInventoryService(JavaPlugin plugin, OfflinePlayerStore offlinePlayerStore, EnderChestStorage enderChestStorage, EmpireCore empireCore, int enderChestPages) {
        this.plugin = plugin;
        this.offlinePlayerStore = offlinePlayerStore;
        this.enderChestStorage = enderChestStorage;
        this.empireCore = empireCore;
        this.enderChestPages = enderChestPages;
        this.liveUpdateTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::refreshOnlineSessions, 1L, 1L);
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
