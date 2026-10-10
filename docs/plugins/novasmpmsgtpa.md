# NovaSMPMsgTpa

Private messaging and teleport request commands.

| Field | Value |
| --- | --- |
| Version | `1.0.6` |
| Build | Maven |
| Main class | `de.otaku.novasmp.msgtpa.NovaSMPMsgTpaPlugin` |
| Source files | 1 |
| Tests | 0 |
| Dependencies | Maven · paper-api |

## Technical Notes

- Category: Utilities
- Focus: Private messaging and teleport request commands

## Code Snippets

### 1. src/main/java/de/otaku/novasmp/msgtpa/NovaSMPMsgTpaPlugin.java
```java
    private static final Set<String> PRIMARY_COMMANDS = Set.of(
            "msg", "tell", "whisper", "w",
            "reply", "r",
            "msgblock", "msgunblock", "msgtoggle", "msgspy",
            "tpa", "tpahere", "tpaccept", "tpadeny", "tpdeny", "tpacancel",
            "tpatoggle", "tpaheretoggle", "tpauto", "tpareload"
    );

    private final Map<UUID, UUID> lastReply = new HashMap<>();
    private final Map<UUID, Set<UUID>> msgBlocks = new HashMap<>();
    private final Set<UUID> msgDisabled = new HashSet<>();
    private final Set<UUID> msgSpyEnabled = new HashSet<>();
    private final Set<UUID> tpaDisabled = new HashSet<>();
    private final Set<UUID> tpaHereDisabled = new HashSet<>();
    private final Set<UUID> tpaAuto = new HashSet<>();
    private final Map<UUID, Long> tpaCooldowns = new HashMap<>();
    private final Map<UUID, TpaRequest> incomingRequests = new HashMap<>();
    private final Map<UUID, TpaRequest> outgoingRequests = new HashMap<>();
    private final Map<UUID, BukkitTask> teleportTasks = new HashMap<>();

    private File dataFile;
    private FileConfiguration data;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadData();
        registerCommands();
        forcePrimaryCommandOwnership();
        Bukkit.getScheduler().runTask(this, this::forcePrimaryCommandOwnership);
        Bukkit.getPluginManager().registerEvents(this, this);
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
