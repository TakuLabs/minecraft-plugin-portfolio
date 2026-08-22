package de.takulabs.smpshop;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class ShopCommand implements CommandExecutor, TabCompleter {
    private final SMPShopPlugin plugin;

    ShopCommand(SMPShopPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
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
        if (!player.hasPermission("smpshop.use")) {
            send(player, "messages.no-permission", player, Map.of());
            return true;
        }
        plugin.shopManager().openMain(player);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (isAdmin(sender)) {
                completions.add("reload");
                completions.add("open");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("open") && isAdmin(sender)) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        }
        return completions.stream().filter(value -> value.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).toList();
    }

    private boolean isAdmin(CommandSender sender) {
        return sender.isOp() || sender.hasPermission("smpshop.*");
    }

    private void send(CommandSender sender, String path, Player player, Map<String, String> replacements) {
        Map<String, String> merged = new java.util.HashMap<>(replacements);
        merged.put("%prefix%", plugin.getConfig().getString("messages.prefix", ""));
        sender.sendMessage(Text.component(plugin, player, Text.replace(plugin.getConfig().getString(path, ""), merged)));
    }
}

