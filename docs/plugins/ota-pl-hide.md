# Ota-Pl-Hide

Permission-based command visibility control.

| Field | Value |
| --- | --- |
| Version | `1.3.0` |
| Build | Maven |
| Main class | `de.otaku.permissioncommandvisibility.PermissionCommandVisibilityPlugin` |
| Source files | 1 |
| Tests | 0 |
| Dependencies | Maven · paper-api |

## Technical Notes

- Category: Moderation / Security
- Focus: Permission-based command visibility control

## Code Snippets

### 1. src/main/java/de/otaku/permissioncommandvisibility/PermissionCommandVisibilityPlugin.java
```java
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        knownCommands = resolveKnownCommands();
        luckPerms = Bukkit.getServicesManager().load(LuckPerms.class);

        if (knownCommands == null) {
            getLogger().warning("Could not access Bukkit's command map. Command visibility filtering is disabled.");
            return;
        }

        Bukkit.getPluginManager().registerEvents(this, this);
        PluginCommand reloadCommand = getCommand("otaplhide");
        if (reloadCommand != null) {
            reloadCommand.setExecutor(this);
            reloadCommand.setTabCompleter(this);
        }

        getLogger().info("Filtering visible slash commands by command permissions.");
        getLogger().info(luckPerms == null ? "LuckPerms not found; using Bukkit permission checks." : "LuckPerms found; wildcard permissions are supported.");
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
