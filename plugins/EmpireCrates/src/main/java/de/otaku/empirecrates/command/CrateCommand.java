package de.otaku.empirecrates.command;

import de.otaku.empirecrates.EmpireCratesPlugin;
import de.otaku.empirecrates.model.CrateDefinition;
import de.otaku.empirecrates.util.ModelEngineBridge;
import de.otaku.empirecrates.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CrateCommand implements CommandExecutor, TabCompleter {
    private final EmpireCratesPlugin plugin;
    private final ModelEngineBridge modelEngine;

    public CrateCommand(EmpireCratesPlugin plugin) {
        this.plugin = plugin;
        this.modelEngine = new ModelEngineBridge(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!canSeeAdminCommands(sender)) {
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }
        if (command.getName().equalsIgnoreCase("keyall")) {
            return handleKeyAll(sender, label, args, 0);
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
            sendInfo(sender);
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }
        if (args.length == 0) {
            sendInfo(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("admin")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can open the GUI.");
                return true;
            }
            plugin.gui().openMain(player);
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            plugin.reloadAll();
            sender.sendMessage(plugin.msg("reloaded"));
            return true;
        }
        if (args[0].equalsIgnoreCase("remove")) {
            return handleRemove(sender, args);
        }
        if (args[0].equalsIgnoreCase("set")) {
            return handleSet(sender, args);
        }
        if (args[0].equalsIgnoreCase("model")) {
            return handleModel(sender, args);
        }
        if (args[0].equalsIgnoreCase("import")) {
            sender.sendMessage(Text.chat("<red>Der ExcellentCrates Import wurde entfernt.</red>"));
            return true;
        }
        if (args[0].equalsIgnoreCase("keyall")) {
            return handleKeyAll(sender, label, args, 1);
        }
        if (args.length < 3 || (!args[0].equalsIgnoreCase("givekey") && !args[0].equalsIgnoreCase("givecrate"))) {
            sendHelp(sender, label);
            return true;
        }
        return handleGive(sender, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (!canSeeAdminCommands(sender)) {
            return suggestions;
        }
        if (command.getName().equalsIgnoreCase("keyall")) {
            if (args.length == 1) {
                plugin.crates().all().forEach(crate -> suggestions.add(crate.getId()));
            }
            return filter(suggestions, args);
        }
        if (args.length == 1) {
            suggestions.addAll(List.of("info", "help", "admin", "set", "model", "remove", "givekey", "givecrate", "keyall", "reload"));
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("model"))) {
            plugin.crates().all().forEach(crate -> suggestions.add(crate.getId()));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            suggestions.addAll(List.of("all", "near"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("remove")
                && (args[1].equalsIgnoreCase("near") || args[1].equalsIgnoreCase("radius"))) {
            suggestions.addAll(List.of("8", "16", "32", "64"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("model")) {
            suggestions.add("none");
            suggestions.addAll(modelEngine.availableModelIds());
        } else if (args.length >= 4 && args.length <= 6 && args[0].equalsIgnoreCase("model")) {
            suggestions.add("none");
            suggestions.addAll(modelEngine.animationIds(args[2]));
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("givekey") || args[0].equalsIgnoreCase("givecrate"))) {
            Bukkit.getOnlinePlayers().forEach(player -> suggestions.add(player.getName()));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("keyall")) {
            plugin.crates().all().forEach(crate -> suggestions.add(crate.getId()));
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("givekey") || args[0].equalsIgnoreCase("givecrate"))) {
            plugin.crates().all().forEach(crate -> suggestions.add(crate.getId()));
        }
        return filter(suggestions, args);
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[1].equalsIgnoreCase("all")) {
            int removed = plugin.crateListener().removeAllPlacedCrates();
            sender.sendMessage(Text.chat("<green>" + removed + " platzierte Crates wurden entfernt.</green>"));
            return true;
        }
        if (args.length >= 2 && (args[1].equalsIgnoreCase("near") || args[1].equalsIgnoreCase("radius"))) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can remove nearby crates.");
                return true;
            }
            double radius = args.length >= 3 ? parseRadius(args[2]) : 16.0D;
            int removed = plugin.crateListener().removePlacedCratesNear(player.getLocation(), radius);
            player.sendMessage(Text.chat("<green>" + removed + " platzierte Crates im Radius " + cleanNumber(radius) + " wurden entfernt.</green>"));
            return true;
        }
        if (args.length >= 2 && isNumber(args[1])) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Only players can remove nearby crates.");
                return true;
            }
            double radius = parseRadius(args[1]);
            int removed = plugin.crateListener().removePlacedCratesNear(player.getLocation(), radius);
            player.sendMessage(Text.chat("<green>" + removed + " platzierte Crates im Radius " + cleanNumber(radius) + " wurden entfernt.</green>"));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can remove looked-at crates.");
            return true;
        }
        plugin.crateListener().removeLookedAtCrate(player);
        return true;
    }

    private boolean handleSet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can set looked-at crates.");
            return true;
        }
        if (args.length < 2) {
            player.sendMessage(Text.chat("<gray>Nutze: <gradient:#F5B642:#9B4DFF>/crate set [Crate]</gradient></gray>"));
            return true;
        }
        CrateDefinition crate = plugin.crates().find(args[1]).orElse(null);
        if (crate == null) {
            player.sendMessage(Text.chat("<red>Crate nicht gefunden.</red>"));
            return true;
        }
        plugin.crateListener().setLookedAtCrate(player, crate);
        return true;
    }

    private boolean handleModel(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Text.chat("<gray>Nutze: <gradient:#F5B642:#9B4DFF>/crate model [Crate] [ModelEngine-ID|none] [open] [idle] [close]</gradient></gray>"));
            return true;
        }
        CrateDefinition crate = plugin.crates().find(args[1]).orElse(null);
        if (crate == null) {
            sender.sendMessage(Text.chat("<red>Crate nicht gefunden.</red>"));
            return true;
        }
        crate.setModelEngineModelId(args[2]);
        if (args.length >= 4) {
            crate.setModelEngineOpenAnimation(args[3]);
        }
        if (args.length >= 5) {
            crate.setModelEngineIdleAnimation(args[4]);
        }
        if (args.length >= 6) {
            crate.setModelEngineCloseAnimation(args[5]);
        }
        plugin.crates().save();
        plugin.crateListener().refreshCrateVisuals(crate.getId());
        if (sender instanceof Player player) {
            plugin.crateListener().refreshLookedAtCrate(player);
        }
        String modelId = crate.getModelEngineModelId().isBlank() ? "deaktiviert" : crate.getModelEngineModelId();
        sender.sendMessage(Text.chat("<green>ModelEngine-Model für </green>" + crate.getDisplayName() + " <green>gesetzt: </green><gradient:#F5B642:#9B4DFF>" + modelId + "</gradient>"));
        sender.sendMessage(Text.chat("<gray>Open:</gray> <gradient:#F5B642:#9B4DFF>" + empty(crate.getModelEngineOpenAnimation()) + "</gradient> <dark_gray>|</dark_gray> <gray>Idle:</gray> <gradient:#7CFFB2:#F5B642>" + empty(crate.getModelEngineIdleAnimation()) + "</gradient> <dark_gray>|</dark_gray> <gray>Close:</gray> <gradient:#F5B642:#9B4DFF>" + empty(crate.getModelEngineCloseAnimation()) + "</gradient>"));
        return true;
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Text.chat("<red>Spieler nicht gefunden.</red>"));
            return true;
        }
        CrateDefinition crate = plugin.crates().find(args[2]).orElse(null);
        if (crate == null) {
            sender.sendMessage(Text.chat("<red>Crate nicht gefunden.</red>"));
            return true;
        }
        int amount = args.length >= 4 ? parseAmount(args[3]) : 1;
        if (args[0].equalsIgnoreCase("givekey")) {
            var item = plugin.markedItems().markKey(crate);
            item.setAmount(amount);
            target.getInventory().addItem(item);
            sender.sendMessage(plugin.msg("key-given"));
        } else {
            var item = plugin.markedItems().markCrate(crate);
            item.setAmount(amount);
            target.getInventory().addItem(item);
            sender.sendMessage(plugin.msg("crate-given"));
        }
        return true;
    }

    private boolean handleKeyAll(CommandSender sender, String label, String[] args, int crateArgIndex) {
        if (args.length <= crateArgIndex) {
            sender.sendMessage(Text.chat("<bold><gradient:#F5B642:#9B4DFF>EmpireCrates</gradient></bold> <dark_gray>></dark_gray> <gray>/" + label + " [Crate] [Anzahl]</gray>"));
            return true;
        }
        CrateDefinition crate = plugin.crates().find(args[crateArgIndex]).orElse(null);
        if (crate == null) {
            sender.sendMessage(Text.chat("<red>Crate nicht gefunden.</red>"));
            return true;
        }
        int amountIndex = crateArgIndex + 1;
        int amount = args.length > amountIndex ? parseAmount(args[amountIndex]) : 1;
        plugin.keyall().giveKeyall(crate, amount, false);
        sender.sendMessage(plugin.msg("keyall", Map.of("crate", crate.getDisplayName())));
        return true;
    }

    private List<String> filter(List<String> suggestions, String[] args) {
        return suggestions.stream().filter(value -> value.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).toList();
    }

    private int parseAmount(String input) {
        try {
            return Math.max(1, Math.min(64, Integer.parseInt(input)));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private double parseRadius(String input) {
        try {
            return Math.max(1.0D, Math.min(512.0D, Double.parseDouble(input)));
        } catch (NumberFormatException ignored) {
            return 16.0D;
        }
    }

    private boolean isNumber(String input) {
        try {
            Double.parseDouble(input);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private String cleanNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((int) value);
        }
        return String.valueOf(value);
    }

    private String empty(String value) {
        return value == null || value.isBlank() ? "none" : value;
    }

    private boolean canSeeAdminCommands(CommandSender sender) {
        return !(sender instanceof Player player) || player.isOp();
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(Text.chat(""));
        sender.sendMessage(Text.chat("<bold><gradient:#F5B642:#9B4DFF>EmpireCrates Help</gradient></bold>"));
        sender.sendMessage(Text.chat("<dark_gray>--------------------------------</dark_gray>"));
        helpLine(sender, label, "info", "Zeigt Version, Beschreibung und Autor.");
        helpLine(sender, label, "admin", "Öffnet das Admin-GUI für Crates, Keys, Rewards und Milestones.");
        helpLine(sender, label, "set [Crate]", "Setzt den Block, auf den du schaust, manuell als Crate.");
        helpLine(sender, label, "model [Crate] [ModelEngine-ID|none] [open] [idle] [close]", "Setzt ModelEngine-Model und optionale Animationen einer Crate.");
        helpLine(sender, label, "remove", "Entfernt die platzierte Crate, auf die du schaust.");
        helpLine(sender, label, "remove near [Radius]", "Entfernt platzierte Crates in deiner Nähe, ohne dass du sie anschauen musst.");
        helpLine(sender, label, "remove all", "Entfernt alle gespeicherten platzierten Crates inklusive Model und Hologramm.");
        helpLine(sender, label, "givekey [Spieler] [Crate] [Anzahl]", "Gibt einem Spieler den Key einer Crate.");
        helpLine(sender, label, "givecrate [Spieler] [Crate] [Anzahl]", "Gibt einem Spieler ein platzierbares Crate-Item.");
        helpLine(sender, label, "keyall [Crate] [Anzahl]", "Gibt allen Online-Spielern den Key einer Crate.");
        sender.sendMessage(Text.chat("<gradient:#F5B642:#9B4DFF>/keyall [Crate] [Anzahl]</gradient>"));
        sender.sendMessage(Text.chat("<gray>Kurzform für den Keyall-Befehl.</gray>"));
        sender.sendMessage(Text.chat(""));
        helpLine(sender, label, "reload", "Lädt config.yml, crates.yml und progress.yml neu.");
        sender.sendMessage(Text.chat("<dark_gray>--------------------------------</dark_gray>"));
    }

    private void helpLine(CommandSender sender, String label, String command, String description) {
        sender.sendMessage(Text.chat("<gradient:#F5B642:#9B4DFF>/" + label + " " + command + "</gradient>"));
        sender.sendMessage(Text.chat("<gray>" + description + "</gray>"));
        sender.sendMessage(Text.chat(""));
    }

    private void sendInfo(CommandSender sender) {
        String version = plugin.getPluginMeta().getVersion();
        sender.sendMessage(Text.mm("<gradient:#7CFFB2:#F5B642>EmpireCrates Plugin</gradient> <gray>Version</gray> <gradient:#F5B642:#9B4DFF>" + version + "</gradient>"));
        sender.sendMessage(Text.mm("<gray>Configurable Paper crate plugin.</gray>"));
        sender.sendMessage(Text.mm("<gray>Autor: </gray><green>ExamplePlayer</green>"));
    }
}
