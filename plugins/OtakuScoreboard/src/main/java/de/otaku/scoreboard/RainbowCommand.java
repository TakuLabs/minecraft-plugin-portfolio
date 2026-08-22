package de.otaku.scoreboard;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class RainbowCommand implements CommandExecutor {
    private final OtakuScoreboardPlugin plugin;
    private final TablistService tablistService;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public RainbowCommand(OtakuScoreboardPlugin plugin, TablistService tablistService) {
        this.plugin = plugin;
        this.tablistService = tablistService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("otakuscoreboard.rainbow")) {
            send(sender, "messages.no-permission");
            return true;
        }

        boolean enabled = tablistService.toggleRainbow();
        send(sender, enabled ? "messages.rainbow-on" : "messages.rainbow-off");
        return true;
    }

    private void send(CommandSender sender, String path) {
        sender.sendMessage(miniMessage.deserialize(plugin.getConfig().getString(path, "")));
    }
}
