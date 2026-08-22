package de.otaku.auctionhouse.command;

import de.otaku.auctionhouse.OtakuAuctionHousePlugin;
import de.otaku.auctionhouse.gui.AuctionGui;
import de.otaku.auctionhouse.gui.SellSetupGui;
import de.otaku.auctionhouse.util.LimitResolver;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class AuctionCommand implements CommandExecutor, TabCompleter {
    private final OtakuAuctionHousePlugin plugin;

    public AuctionCommand(OtakuAuctionHousePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                plugin.texts().send(sender, "<red>Nur Spieler können das Auktionshaus öffnen.</red>");
                return true;
            }
            if (!player.hasPermission("otakuauctionhouse.use")) {
                plugin.texts().send(player, "<red>Dafür fehlt dir die Permission.</red>");
                return true;
            }
            new AuctionGui(plugin).open(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("sell")) {
            sell(sender, args);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            reload(sender);
            return true;
        }

        sendHelp(sender, label);
        return true;
    }

    private void sell(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.texts().send(sender, "<red>Nur Spieler können Items verkaufen.</red>");
            return;
        }
        if (!player.hasPermission("otakuauctionhouse.sell")) {
            plugin.texts().send(player, "<red>Dafür fehlt dir die Permission.</red>");
            return;
        }
        int active = plugin.listingStore().activeListings(player.getUniqueId());
        int max = LimitResolver.maxListings(plugin, player);
        if (active >= max) {
            plugin.texts().send(player, "<red>Du hast dein Limit erreicht:</red> <gradient:#6D28D9:#06B6D4>" + active + "/" + max + "</gradient>");
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() == Material.AIR || hand.getAmount() <= 0) {
            plugin.texts().send(player, "<red>Halte das Item in der Hand, das du verkaufen möchtest.</red>");
            return;
        }

        SellSetupGui sellSetupGui = new SellSetupGui(plugin);
        if (args.length >= 2) {
            try {
                double price = Double.parseDouble(args[1].replace(',', '.'));
                if (price <= 0D) {
                    plugin.texts().send(player, "<red>Der Preis muss größer als 0 sein.</red>");
                    return;
                }
                sellSetupGui.open(player, hand.clone(), price);
                return;
            } catch (NumberFormatException exception) {
                plugin.texts().send(player, "<red>Bitte gib einen gültigen Preis ein.</red>");
                return;
            }
        }

        sellSetupGui.open(player, hand.clone());
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("otakuauctionhouse.admin")) {
            plugin.texts().send(sender, "<red>Dafür fehlt dir die Permission.</red>");
            return;
        }
        plugin.reloadPlugin();
        plugin.texts().send(sender, "<green>Konfiguration und Angebote wurden neu geladen.</green>");
    }

    private void sendHelp(CommandSender sender, String label) {
        plugin.texts().send(sender, "<gradient:#6D28D9:#06B6D4>/" + label + "</gradient> <gray>öffnet das Auktionshaus</gray>");
        plugin.texts().send(sender, "<gradient:#6D28D9:#06B6D4>/" + label + " sell [preis]</gradient> <gray>öffnet das Verkaufsmenü für dein Hand-Item</gray>");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            if ("sell".startsWith(args[0].toLowerCase())) {
                suggestions.add("sell");
            }
            if (sender.hasPermission("otakuauctionhouse.admin") && "reload".startsWith(args[0].toLowerCase())) {
                suggestions.add("reload");
            }
            return suggestions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("sell")) {
            return List.of("100", "500", "1000");
        }
        return List.of();
    }
}
