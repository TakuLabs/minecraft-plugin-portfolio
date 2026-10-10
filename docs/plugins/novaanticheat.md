# NovaAnticheat

Security architecture and event pipeline.

| Field | Value |
| --- | --- |
| Version | `1.0.2` |
| Build | Maven |
| Main class | `de.otaku.nova.anticheat.NovaAnticheat` |
| Source files | 37 |
| Tests | 0 |
| Dependencies | Maven · paper-api |

## Technical Notes

- Category: Moderation / Security
- Focus: Security architecture and event pipeline
- Public page uses selected excerpts only; full private source is not copied.

## Code Snippets

### 1. src/main/java/de/otaku/nova/anticheat/commands/NacCommand.java

Command handling from the plugin source.

```java
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("novaanticheat.admin")) {
            sender.sendMessage(legacy("&cYou do not have permission to use this command."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(legacy(USAGE));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadConfig();
                sender.sendMessage(legacy("&aNovaAnticheat config reloaded."));
            }
            case "vl" -> {
                if (args.length < 2) {
                    sender.sendMessage(legacy("&cUsage: /nac vl <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(legacy("&cPlayer not found."));
                    return true;
                }
                PlayerData data = plugin.getPlayerDataManager().get(target);
                boolean exempt = plugin.getPunishmentManager().isExempt(target);
                sender.sendMessage(legacy("&8[&cNovaAC&8] &7Violations for &f" + target.getName()
                        + (exempt ? " &8(&aexempt&8)" : "") + "&7:"));
                sender.sendMessage(legacy("  &7Speed: &f" + data.getViolations("Speed")
                        + " &7Flight: &f" + data.getViolations("Flight")
                        + " &7KillAura: &f" + data.getViolations("KillAura")
                        + " &7Reach: &f" + data.getViolations("Reach")
                        + " &7AutoClicker: &f" + data.getViolations("AutoClicker")
                        + " &7AutoTotem: &f" + data.getViolations("AutoTotem")
                        + " &7BlockESP: &f" + data.getViolations("BlockESP")));
            }
            case "bypass" -> {
                if (args.length < 2) {
                    sender.sendMessage(legacy("&cUsage: /nac bypass <player>"));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(legacy("&cPlayer not found."));
```

### 2. src/main/java/de/otaku/nova/anticheat/ConfigVersionUpdater.java

Configuration handling from the plugin source.

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
