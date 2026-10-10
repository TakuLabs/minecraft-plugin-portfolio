# EmpireSuiteStandalone

Standalone suite bootstrap with module file setup.

| Field | Value |
| --- | --- |
| Version | `1.0.0` |
| Build | Maven |
| Main class | `de.otaku.empiresuite.EmpireSuiteStandalonePlugin` |
| Source files | 4 |
| Tests | 0 |
| Dependencies | Maven · paper-api, Vault |

## Technical Notes

- Category: Core / Infrastructure
- Focus: Standalone suite bootstrap with module file setup

## Code Snippets

### 1. src/main/java/de/otaku/core/ModuleFiles.java
```java
    public static File folder(JavaPlugin plugin, String moduleName) {
        File folder = new File(plugin.getDataFolder(), moduleName);
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Konnte Modulordner nicht erstellen: " + folder.getPath());
        }
        return folder;
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
