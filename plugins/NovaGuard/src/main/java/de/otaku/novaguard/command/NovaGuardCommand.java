package de.otaku.novaguard.command;

import de.otaku.novaguard.NovaGuard;
import de.otaku.novaguard.config.MessageService;
import de.otaku.novaguard.config.PluginSettings;
import de.otaku.novaguard.iplimit.IpSnapshot;
import de.otaku.novaguard.model.ListEntry;
import de.otaku.novaguard.packet.PacketSnapshot;
import de.otaku.novaguard.storage.SecurityStores;
import de.otaku.novaguard.storage.UuidStore;
import de.otaku.novaguard.util.IpNormalizer;
import de.otaku.novaguard.util.PermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class NovaGuardCommand implements CommandExecutor, TabCompleter {
    private static final List<String> LIST_ACTIONS = List.of("add", "remove", "list", "check");
    private static final List<String> IPLIMIT_ACTIONS = List.of("set", "reset", "check");

    private final NovaGuard plugin;
    private final MessageService messages;
    private final SecurityStores stores;
    private final KnownPlayerDirectory knownPlayers;

    public NovaGuardCommand(NovaGuard plugin, MessageService messages, SecurityStores stores, KnownPlayerDirectory knownPlayers) {
        this.plugin = plugin;
        this.messages = messages;
        this.stores = stores;
        this.knownPlayers = knownPlayers;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            help(sender, null);
            return true;
        }
        String root = args[0].toLowerCase(Locale.ROOT);
        switch (root) {
            case "help" -> help(sender, args.length > 1 ? args[1] : null);
            case "reload" -> reload(sender);
            case "status" -> status(sender, args);
            case "packetinfo" -> packetInfo(sender, args);
            case "whitelist" -> uuidList(sender, args, stores.whitelist(), "novaguard.whitelist.manage", false);
            case "blacklist" -> uuidList(sender, args, stores.uuidBlacklist(), "novaguard.blacklist.manage", true);
            case "ipblacklist" -> ipBlacklist(sender, args);
            case "iplimit" -> ipLimit(sender, args);
            case "perf" -> perf(sender);
            default -> sender.sendMessage(messages.prefixed("usage"));
        }
        return true;
    }

    private void reload(CommandSender sender) {
        if (!hasPermission(sender, "novaguard.reload")) {
            noPermission(sender);
            return;
        }
        plugin.reloadNovaGuard();
        sender.sendMessage(messages.prefixed("reload"));
    }

    private void status(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "novaguard.status")) {
            noPermission(sender);
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.prefixed("commands.usage.status"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(messages.prefixed("not-found"));
            return;
        }
        PluginSettings settings = plugin.settings();
        String ip = plugin.ipFor(target.getUniqueId());
        if (ip == null && target.getAddress() != null) {
            ip = IpNormalizer.normalize(target.getAddress()).orElse(null);
        }
        IpSnapshot ipSnapshot = ip == null ? new IpSnapshot(0, 0) : plugin.ipLimitService().snapshot(ip);
        PacketSnapshot packet = plugin.packetRateLimiter().snapshot(target.getUniqueId(), System.nanoTime());
        Optional<Integer> override = ip == null ? Optional.empty() : stores.ipOverrides().get(ip);
        int limit = override.orElse(settings.maxAccountsPerIp());
        boolean permissionBypass = PermissionUtil.hasExactPermission(target, "novaguard.iplimit.bypass");
        knownPlayers.remember(target.getUniqueId(), target.getName());
        sender.sendMessage(messages.prefixed("commands.status.player", Map.of("player", target.getName())));
        sender.sendMessage(messages.prefixed("commands.status.uuid", Map.of("uuid", target.getUniqueId().toString())));
        sender.sendMessage(messages.prefixed("commands.status.normalized-ip", Map.of("ip", ip == null ? "unknown" : ip)));
        sender.sendMessage(messages.prefixed("commands.status.protection-enabled", Map.of("value", String.valueOf(settings.ipLimitEnabled()))));
        sender.sendMessage(messages.prefixed("commands.status.default-limit", Map.of("value", String.valueOf(settings.maxAccountsPerIp()))));
        sender.sendMessage(messages.prefixed("commands.status.effective-limit", Map.of("value", String.valueOf(limit))));
        sender.sendMessage(messages.prefixed("commands.status.active", Map.of("value", String.valueOf(ipSnapshot.active()))));
        sender.sendMessage(messages.prefixed("commands.status.reserved", Map.of("value", String.valueOf(ipSnapshot.reserved()))));
        sender.sendMessage(messages.prefixed("commands.status.whitelisted", Map.of("value", String.valueOf(stores.whitelist().contains(target.getUniqueId())))));
        sender.sendMessage(messages.prefixed("commands.status.permission-bypass", Map.of("value", String.valueOf(permissionBypass))));
        sender.sendMessage(messages.prefixed("commands.status.override", Map.of("value", override.map(String::valueOf).orElse("none"))));
        sender.sendMessage(messages.prefixed("commands.status.packet-rate", Map.of(
                "rate", String.valueOf(packet.ratePerSecond()),
                "tokens", String.format(Locale.ROOT, "%.1f", packet.tokens())
        )));
        sender.sendMessage(messages.prefixed("commands.status.violations", Map.of("value", String.valueOf(packet.violations()))));
        sender.sendMessage(messages.prefixed("commands.status.uuid-blacklist", Map.of("value", String.valueOf(stores.uuidBlacklist().contains(target.getUniqueId())))));
        sender.sendMessage(messages.prefixed("commands.status.ip-blacklist", Map.of("value", String.valueOf(ip != null && stores.ipBlacklist().contains(ip)))));
        sender.sendMessage(messages.prefixed("commands.status.packet-bypass", Map.of("value", String.valueOf(PermissionUtil.hasExactPermission(target, "novaguard.packet.bypass")))));
        sender.sendMessage(messages.prefixed("commands.status.packet-mode", Map.of("value", settings.packetMode().name())));
    }

    private void packetInfo(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "novaguard.packetinfo")) {
            noPermission(sender);
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.prefixed("commands.usage.packetinfo"));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            KnownPlayer known = knownPlayers.find(args[1]);
            sender.sendMessage(messages.prefixed("commands.packetinfo.no-live", Map.of(
                    "player", known == null || known.lastKnownName() == null ? args[1] : known.lastKnownName()
            )));
            return;
        }
        knownPlayers.remember(target.getUniqueId(), target.getName());
        PacketSnapshot packet = plugin.packetRateLimiter().snapshot(target.getUniqueId(), System.nanoTime());
        boolean bypass = PermissionUtil.hasExactPermission(target, "novaguard.packet.bypass");
        sender.sendMessage(messages.prefixed("commands.packetinfo.header", Map.of("player", target.getName())));
        sender.sendMessage(messages.component("commands.help.separator"));
        sender.sendMessage(messages.component("commands.packetinfo.uuid", Map.of("uuid", target.getUniqueId().toString())));
        sender.sendMessage(messages.component("commands.packetinfo.mode", Map.of("value", plugin.settings().packetMode().name())));
        sender.sendMessage(messages.component("commands.packetinfo.rate", Map.of("value", String.valueOf(packet.ratePerSecond()))));
        sender.sendMessage(messages.component("commands.packetinfo.burst", Map.of("value", String.format(Locale.ROOT, "%.1f", packet.tokens()))));
        sender.sendMessage(messages.component("commands.packetinfo.violations", Map.of("value", String.valueOf(packet.violations()))));
        sender.sendMessage(messages.component("commands.packetinfo.bypass", Map.of("value", yesNo(bypass))));
        sender.sendMessage(messages.component("commands.packetinfo.last-violation", Map.of("value", packet.violations() > 0 ? "aktuell" : "keiner")));
        sender.sendMessage(messages.component("commands.help.separator"));
    }

    private void perf(CommandSender sender) {
        if (!hasPermission(sender, "novaguard.performance.view")) {
            noPermission(sender);
            return;
        }
        sender.sendMessage(messages.prefixed("commands.perf.active-sessions", Map.of("value", String.valueOf(plugin.ipLimitService().activeSessionCount()))));
        sender.sendMessage(messages.prefixed("commands.perf.active-groups", Map.of("value", String.valueOf(plugin.ipLimitService().activeGroupCount()))));
        sender.sendMessage(messages.prefixed("commands.perf.reservations", Map.of("value", String.valueOf(plugin.ipLimitService().reservationCount()))));
        sender.sendMessage(messages.prefixed("commands.perf.rate-states", Map.of("value", String.valueOf(plugin.packetRateLimiter().stateCount()))));
        sender.sendMessage(messages.prefixed("commands.perf.storage-queue", Map.of("value", String.valueOf(plugin.stores().connectionHistory().queuedWrites()))));
        sender.sendMessage(messages.prefixed("commands.perf.history-entries", Map.of("value", String.valueOf(plugin.stores().connectionHistory().historyEntryCount()))));
        sender.sendMessage(messages.prefixed("commands.perf.known-players", Map.of("value", String.valueOf(knownPlayers.size()))));
        sender.sendMessage(messages.prefixed("commands.perf.debug", Map.of("value", String.valueOf(plugin.settings().debug()))));
    }

    private void uuidList(CommandSender sender, String[] args, UuidStore store, String permission, boolean reasonAllowed) {
        if (!hasPermission(sender, permission)) {
            noPermission(sender);
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.prefixed("commands.usage.uuid-list", Map.of("list", args[0])));
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("list")) {
            sender.sendMessage(messages.prefixed("commands.list.count", Map.of("list", args[0], "count", String.valueOf(store.list().size()))));
            int shown = 0;
            for (ListEntry entry : store.list()) {
                if (shown++ >= 25) {
                    break;
                }
                sender.sendMessage(messages.prefixed("commands.list.entry", Map.of("entry", entry.uuid() + nameSuffix(entry))));
            }
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(messages.prefixed("commands.usage.uuid-target", Map.of("list", args[0], "action", action)));
            return;
        }
        Optional<ResolvedUuid> resolved = resolveUuid(args[2], store);
        if (resolved.isEmpty()) {
            sender.sendMessage(messages.prefixed("not-found"));
            return;
        }
        UUID uuid = resolved.get().uuid();
        switch (action) {
            case "add" -> {
                String reason = reasonAllowed && args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : null;
                store.add(uuid, resolved.get().name(), reason, sender.getName());
                knownPlayers.remember(uuid, resolved.get().name());
                sender.sendMessage(messages.prefixed("changed"));
            }
            case "remove" -> {
                store.remove(uuid);
                sender.sendMessage(messages.prefixed("changed"));
            }
            case "check" -> sender.sendMessage(messages.prefixed("commands.list.check", Map.of("entry", uuid.toString(), "value", String.valueOf(store.contains(uuid)))));
            default -> sender.sendMessage(messages.prefixed("usage"));
        }
    }

    private void ipBlacklist(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "novaguard.ipblacklist.manage")) {
            noPermission(sender);
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.prefixed("commands.usage.ipblacklist"));
            return;
        }
        if (args[1].equalsIgnoreCase("list")) {
            sender.sendMessage(messages.prefixed("commands.list.count", Map.of("list", "ipblacklist", "count", String.valueOf(stores.ipBlacklist().list().size()))));
            int shown = 0;
            for (ListEntry entry : stores.ipBlacklist().list()) {
                if (shown++ >= 25) {
                    break;
                }
                sender.sendMessage(messages.prefixed("commands.list.entry", Map.of("entry", entry.key())));
            }
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(messages.prefixed("commands.usage.ipblacklist-target", Map.of("action", args[1])));
            return;
        }
        Optional<String> ip = IpNormalizer.normalize(args[2]);
        if (ip.isEmpty()) {
            sender.sendMessage(messages.prefixed("invalid-ip"));
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "add" -> {
                String reason = args.length > 3 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length)) : null;
                stores.ipBlacklist().add(ip.get(), reason, sender.getName());
                sender.sendMessage(messages.prefixed("changed"));
            }
            case "remove" -> {
                stores.ipBlacklist().remove(ip.get());
                sender.sendMessage(messages.prefixed("changed"));
            }
            case "check" -> sender.sendMessage(messages.prefixed("commands.list.check", Map.of("entry", ip.get(), "value", String.valueOf(stores.ipBlacklist().contains(ip.get())))));
            default -> sender.sendMessage(messages.prefixed("usage"));
        }
    }

    private void ipLimit(CommandSender sender, String[] args) {
        if (!hasPermission(sender, "novaguard.iplimit.manage")) {
            noPermission(sender);
            return;
        }
        if (args.length < 4 && !List.of("check", "reset").contains(args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "")) {
            sender.sendMessage(messages.prefixed("commands.usage.iplimit"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(messages.prefixed("commands.usage.iplimit"));
            return;
        }
        Optional<String> ip = IpNormalizer.normalize(args[2]);
        if (ip.isEmpty()) {
            sender.sendMessage(messages.prefixed("invalid-ip"));
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "set" -> {
                Optional<Integer> parsed = parseNonNegativeInt(args[3]);
                if (parsed.isEmpty()) {
                    sender.sendMessage(messages.prefixed("invalid-limit"));
                    return;
                }
                stores.ipOverrides().set(ip.get(), parsed.get());
                sender.sendMessage(messages.prefixed("changed"));
            }
            case "reset" -> {
                stores.ipOverrides().reset(ip.get());
                sender.sendMessage(messages.prefixed("changed"));
            }
            case "check" -> sender.sendMessage(messages.prefixed("commands.iplimit.check", Map.of(
                    "ip", ip.get(),
                    "value", stores.ipOverrides().get(ip.get()).map(String::valueOf).orElse("none")
            )));
            default -> sender.sendMessage(messages.prefixed("usage"));
        }
    }

    private Optional<ResolvedUuid> resolveUuid(String input, UuidStore store) {
        try {
            UUID uuid = UUID.fromString(input);
            KnownPlayer known = knownPlayers.find(input);
            return Optional.of(new ResolvedUuid(uuid, known == null ? null : known.lastKnownName()));
        } catch (IllegalArgumentException ignored) {
            Player online = Bukkit.getPlayerExact(input);
            if (online != null) {
                return Optional.of(new ResolvedUuid(online.getUniqueId(), online.getName()));
            }
            KnownPlayer known = knownPlayers.find(input);
            if (known != null) {
                return Optional.of(new ResolvedUuid(known.uuid(), known.lastKnownName()));
            }
            Optional<ListEntry> listed = store.findByName(input);
            return listed.map(entry -> new ResolvedUuid(entry.uuid(), entry.lastKnownName()));
        }
    }

    private void help(CommandSender sender, String commandName) {
        if (commandName != null) {
            CommandCatalog.CommandMeta command = CommandCatalog.find(commandName);
            if (command == null || !canUse(sender, command)) {
                sender.sendMessage(messages.prefixed("commands.help.unknown"));
                return;
            }
            sender.sendMessage(messages.prefixed("commands.help.detail-header", Map.of("command", command.name())));
            sender.sendMessage(messages.component("commands.help.separator"));
            sender.sendMessage(messages.component("commands.help.entry", Map.of("syntax", command.syntax())));
            sender.sendMessage(messages.component(command.descriptionKey()));
            sender.sendMessage(messages.component("commands.help.permission", Map.of("permission", command.permission())));
            sender.sendMessage(messages.component("commands.help.separator"));
            return;
        }
        sender.sendMessage(messages.prefixed("commands.help.header"));
        sender.sendMessage(messages.component("commands.help.separator"));
        CommandCatalog.Category lastCategory = null;
        int shown = 0;
        for (CommandCatalog.CommandMeta command : CommandCatalog.commands()) {
            if (!canUse(sender, command)) {
                continue;
            }
            if (lastCategory != command.category()) {
                lastCategory = command.category();
                sender.sendMessage(messages.component(lastCategory.messageKey()));
            }
            sender.sendMessage(messages.component("commands.help.entry", Map.of("syntax", command.syntax())));
            sender.sendMessage(messages.component(command.descriptionKey()));
            shown++;
        }
        if (shown == 0) {
            sender.sendMessage(messages.component("commands.help.empty"));
        }
        sender.sendMessage(messages.component("commands.help.separator"));
    }

    private void noPermission(CommandSender sender) {
        sender.sendMessage(messages.prefixed("no-permission"));
    }

    private Optional<Integer> parseNonNegativeInt(String raw) {
        try {
            int value = Integer.parseInt(raw);
            return value < 0 ? Optional.empty() : Optional.of(value);
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private boolean canUse(CommandSender sender, CommandCatalog.CommandMeta command) {
        return CommandCatalog.canUse(command, permission -> hasPermission(sender, permission));
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        return sender instanceof ConsoleCommandSender || sender.hasPermission(permission) || sender.hasPermission("novaguard.admin");
    }

    private String nameSuffix(ListEntry entry) {
        return entry.lastKnownName() == null ? "" : " (" + entry.lastKnownName() + ")";
    }

    private String yesNo(boolean value) {
        return value ? "Ja" : "Nein";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CommandCatalog.visibleRoots(permission -> hasPermission(sender, permission), args[0]);
        }
        String root = args[0].toLowerCase(Locale.ROOT);
        CommandCatalog.CommandMeta meta = CommandCatalog.find(root);
        if (meta == null || !canUse(sender, meta)) {
            return List.of();
        }
        if (args.length == 2) {
            return secondArgumentCompletions(sender, root, args[1]);
        }
        if (args.length == 3) {
            return thirdArgumentCompletions(root, args[1].toLowerCase(Locale.ROOT), args[2]);
        }
        if (args.length == 4 && root.equals("iplimit") && args[1].equalsIgnoreCase("set")) {
            return prefix(List.of(String.valueOf(plugin.settings().maxAccountsPerIp()), "2", "5", "10", "15"), args[3]);
        }
        return List.of();
    }

    private List<String> secondArgumentCompletions(CommandSender sender, String root, String token) {
        return switch (root) {
            case "help" -> CommandCatalog.visibleRoots(permission -> hasPermission(sender, permission), token);
            case "status", "packetinfo" -> knownPlayers.suggest(token);
            case "whitelist", "blacklist", "ipblacklist" -> prefix(LIST_ACTIONS, token);
            case "iplimit" -> prefix(IPLIMIT_ACTIONS, token);
            default -> List.of();
        };
    }

    private List<String> thirdArgumentCompletions(String root, String action, String token) {
        if (root.equals("whitelist")) {
            return action.equals("remove") || action.equals("check")
                    ? uuidStoreSuggestions(stores.whitelist(), token)
                    : knownPlayers.suggest(token);
        }
        if (root.equals("blacklist")) {
            return action.equals("remove") || action.equals("check")
                    ? uuidStoreSuggestions(stores.uuidBlacklist(), token)
                    : knownPlayers.suggest(token);
        }
        if (root.equals("ipblacklist") && (action.equals("remove") || action.equals("check"))) {
            return ipEntrySuggestions(token);
        }
        if (root.equals("iplimit") && (action.equals("reset") || action.equals("check"))) {
            return ipOverrideSuggestions(token);
        }
        return List.of();
    }

    private List<String> uuidStoreSuggestions(UuidStore store, String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        boolean uuidPrefix = lower.length() >= 4;
        List<String> result = new ArrayList<>();
        for (ListEntry entry : store.list()) {
            if (result.size() >= 40) {
                break;
            }
            String name = entry.lastKnownName();
            if (name != null && name.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(name);
                continue;
            }
            String uuid = entry.uuid().toString();
            if (uuidPrefix && uuid.startsWith(lower)) {
                result.add(uuid);
            }
        }
        return result;
    }

    private List<String> ipEntrySuggestions(String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (ListEntry entry : stores.ipBlacklist().list()) {
            if (entry.key().toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(entry.key());
                if (result.size() >= 40) {
                    break;
                }
            }
        }
        return result;
    }

    private List<String> ipOverrideSuggestions(String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        Set<String> keys = stores.ipOverrides().keys();
        List<String> result = new ArrayList<>();
        for (String key : keys) {
            if (key.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(key);
                if (result.size() >= 40) {
                    break;
                }
            }
        }
        return result;
    }

    private List<String> prefix(List<String> values, String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(value);
            }
        }
        return result;
    }

    private record ResolvedUuid(UUID uuid, String name) {
    }
}
