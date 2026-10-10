# OrderShop

Order-based shop commands with Vault economy support.

| Field | Value |
| --- | --- |
| Version | `1.0.2` |
| Build | Maven |
| Main class | `de.otaku.nova.ordershop.OrderShop` |
| Source files | 37 |
| Tests | 0 |
| Dependencies | Maven · paper-api, Vault, bukkit |

## Technical Notes

- Category: Economy
- Focus: Order-based shop commands with Vault economy support

## Code Snippets

### 1. src/main/java/de/otaku/nova/ordershop/command/OrderCommand.java
```java
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players.", NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("ordershop.use")) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(Lang.MSG_NO_PERMISSION.get(player)));
            return true;
        }

        if (!player.hasPermission("ordershop.cooldown.bypass") && isOnCooldown(player)) {
            return true;
        }

        MainMenuGui.open(player, Lang.MAIN_TITLE.get(player));
        return true;
    }
```

### 2. src/main/java/de/otaku/nova/ordershop/ConfigVersionUpdater.java
```java
    public static void update(JavaPlugin plugin) {
        plugin.saveDefaultConfig();

        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.isFile()) {
            plugin.reloadConfig();
            return;
        }

        try (InputStream stream = plugin.getResource("config.yml")) {
            if (stream == null) {
                plugin.reloadConfig();
                return;
            }

            YamlConfiguration current = YamlConfiguration.loadConfiguration(configFile);
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));

            int currentVersion = current.getInt("config-version", 0);
            int bundledVersion = defaults.getInt("config-version", 1);
            boolean changed = false;

            if (currentVersion < bundledVersion) {
                backupConfig(configFile, currentVersion, bundledVersion);
            }

            changed |= copyMissing(defaults, current, "");
            if (currentVersion != bundledVersion) {
                current.set("config-version", bundledVersion);
                changed = true;
            }

            if (changed) {
                current.save(configFile);
                plugin.getLogger().info("config.yml wurde sicher auf Config-Version " + bundledVersion
                        + " migriert. Bestehende Werte und Daten bleiben erhalten.");
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Config-Migration fehlgeschlagen: " + exception.getMessage());
        }

        plugin.reloadConfig();
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
