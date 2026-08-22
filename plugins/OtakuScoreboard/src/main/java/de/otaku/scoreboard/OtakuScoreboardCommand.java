package de.otaku.scoreboard;

import java.util.List;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class OtakuScoreboardCommand implements CommandExecutor, TabCompleter {
    private final OtakuScoreboardPlugin plugin;
    private final ScoreboardService scoreboardService;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public OtakuScoreboardCommand(OtakuScoreboardPlugin plugin, ScoreboardService scoreboardService) {
        this.plugin = plugin;
        this.scoreboardService = scoreboardService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("/" + label + " reload");
            sender.sendMessage("/" + label + " toggle");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("otakuscoreboard.admin")) {
                send(sender, "messages.no-permission");
                return true;
            }

            plugin.reloadPlugin();
            send(sender, "messages.reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("toggle")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Dieser Befehl ist nur fuer Spieler.");
                return true;
            }
            if (!player.hasPermission("otakuscoreboard.toggle")) {
                send(player, "messages.no-permission");
                return true;
            }

            boolean enabled = scoreboardService.toggle(player);
            send(player, enabled ? "messages.toggled-on" : "messages.toggled-off");
            return true;
        }

        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("reload", "toggle").stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }

    private void send(CommandSender sender, String path) {
        String message = plugin.getConfig().getString(path, "");
        sender.sendMessage(miniMessage.deserialize(message));
    }
}
