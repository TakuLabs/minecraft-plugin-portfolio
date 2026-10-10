# MythicEmpireShop

Network shop module with inventory menus and item pricing.

| Field | Value |
| --- | --- |
| Version | `1.2.10` |
| Build | Maven |
| Main class | `de.otaku.mythicempire.shop.MythicEmpireShopPlugin` |
| Source files | 31 |
| Tests | 0 |
| Dependencies | Maven · paper-api |

## Technical Notes

- Category: Economy
- Focus: Network shop module with inventory menus and item pricing

## Code Snippets

### 1. src/main/java/de/otaku/mythicempire/shop/ShopCommand.java
```java
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player commandPlayer && !plugin.commandCooldownManager().check(commandPlayer, command)) {
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!isAdmin(sender)) {
                send(sender, "messages.no-permission", null, Map.of());
                return true;
            }
            plugin.reloadComponents();
            send(sender, "messages.reloaded", null, Map.of());
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("open")) {
            if (!isAdmin(sender)) {
                send(sender, "messages.no-permission", null, Map.of());
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("/" + label + " open <player>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                send(sender, "messages.unknown-player", null, Map.of());
                return true;
            }
            plugin.shopManager().openMain(target);
            return true;
        }

        if (!(sender instanceof Player player)) {
            send(sender, "messages.only-player", null, Map.of());
            return true;
        }
        if (!player.hasPermission("mythicempire.use")) {
            send(player, "messages.no-permission", player, Map.of());
            return true;
        }
        plugin.shopManager().openMain(player);
        return true;
    }
```

### 2. src/main/java/de/otaku/mythicempire/shop/AuctionStore.java
```java
package de.otaku.mythicempire.shop;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class AuctionStore {
    private final MythicEmpireShopPlugin plugin;
    private final File file;
    private final List<AuctionListing> listings = new ArrayList<>();

    AuctionStore(MythicEmpireShopPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "auctionhouse.yml");
        load();
    }

    void load() {
        listings.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("listings");
        if (section == null) {
            return;
```

[← Back to Plugin Catalog](../plugin-catalog.md)
