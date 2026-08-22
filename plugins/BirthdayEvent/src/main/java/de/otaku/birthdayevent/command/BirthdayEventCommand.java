package de.otaku.birthdayevent.command;

import de.otaku.birthdayevent.BirthdayEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BirthdayEventCommand implements CommandExecutor, TabCompleter {
    private static final DateTimeFormatter STATUS_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss", Locale.GERMANY);
    private final BirthdayEvent plugin;

    public BirthdayEventCommand(BirthdayEvent plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "gratulieren", "congratulate" -> congratulate(sender);
            case "testclick" -> testClick(sender);
            case "help" -> {
                help(sender, label);
                yield true;
            }
            case "status" -> admin(sender, () -> status(sender));
            case "test" -> admin(sender, () -> test(sender));
            case "testall" -> admin(sender, () -> testAll(sender));
            case "reload" -> admin(sender, () -> reload(sender));
            default -> {
                plugin.messages().send(sender, "unknown-command");
                yield true;
            }
        };
    }

    private boolean congratulate(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "only-players");
            return true;
        }
        plugin.congratulationService().handle(player);
        return true;
    }

    private boolean testClick(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "only-players");
            return true;
        }
        if (!player.hasPermission("birthdayevent.admin")) {
            plugin.messages().send(player, "no-permission");
            return true;
        }
        plugin.congratulationService().handleTestClick(player);
        return true;
    }

    private boolean admin(CommandSender sender, Runnable action) {
        if (!sender.hasPermission("birthdayevent.admin")) {
            plugin.messages().send(sender, "no-permission");
            return true;
        }
        action.run();
        return true;
    }

    private void help(CommandSender sender, String label) {
        sender.sendMessage(plugin.messages().component("help-header"));
        sendHelp(sender, label, "gratulieren", "BirthdayPlayer gratulieren und Reward erhalten.");
        if (sender.hasPermission("birthdayevent.admin")) {
            sendHelp(sender, label, "status", "Eventstatus anzeigen.");
            sendHelp(sender, label, "test", "Darstellung nur für dich testen.");
            sendHelp(sender, label, "testall", "Darstellung für alle online testen.");
            sendHelp(sender, label, "reload", "Config und Nachrichten neu laden.");
        }
    }

    private void sendHelp(CommandSender sender, String label, String subcommand, String description) {
        sender.sendMessage(plugin.messages().component(
                "help-line",
                Map.of(
                        "command", label,
                        "subcommand", subcommand,
                        "description", description
                )
        ));
    }

    private void status(CommandSender sender) {
        var window = plugin.eventWindow();
        var now = window.now();
        plugin.messages().send(sender, "status-header");
        sender.sendMessage(plugin.messages().deserialize("<gray>Eventdatum: <light_purple>%date%</light_purple></gray>",
                Map.of("date", window.start().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))));
        sender.sendMessage(plugin.messages().deserialize("<gray>Zeitzone: <light_purple>%zone%</light_purple></gray>",
                Map.of("zone", window.settings().zone().getId())));
        sender.sendMessage(plugin.messages().deserialize("<gray>Deutsche Zeit: <light_purple>%time%</light_purple></gray>",
                Map.of("time", now.format(STATUS_TIME))));
        sender.sendMessage(plugin.messages().deserialize("<gray>Event gestartet: <light_purple>%value%</light_purple></gray>",
                Map.of("value", window.hasStarted(now) ? "Ja" : "Nein")));
        sender.sendMessage(plugin.messages().deserialize("<gray>Event aktiv: <light_purple>%value%</light_purple></gray>",
                Map.of("value", window.isActive(now) ? "Ja" : "Nein")));
        sender.sendMessage(plugin.messages().deserialize("<gray>Event ausgelöst: <light_purple>%value%</light_purple></gray>",
                Map.of("value", plugin.eventStateStore().isEventTriggered() ? "Ja" : "Nein")));
        sender.sendMessage(plugin.messages().deserialize("<gray>Gratulationen: <light_purple>%count%</light_purple></gray>",
                Map.of("count", String.valueOf(plugin.congratulationService().count()))));
    }

    private void test(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "only-players");
            return;
        }
        plugin.sendBirthdayExperience(player, true);
        plugin.messages().send(player, "test-self");
    }

    private void testAll(CommandSender sender) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.sendBirthdayExperience(player, true);
        }
        plugin.messages().send(sender, "test-all");
    }

    private void reload(CommandSender sender) {
        plugin.reloadPlugin();
        plugin.messages().send(sender, "reload");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        List<String> suggestions = new ArrayList<>();
        suggestions.add("gratulieren");
        if (sender.hasPermission("birthdayevent.admin")) {
            suggestions.addAll(List.of("help", "status", "test", "testall", "reload"));
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        return suggestions.stream()
                .filter(value -> value.startsWith(prefix))
                .sorted()
                .toList();
    }
}
