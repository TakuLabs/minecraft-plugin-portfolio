package de.otaku.novaguard.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public final class CommandCatalog {
    private static final List<CommandMeta> COMMANDS = List.of(
            new CommandMeta("help", "novaguard.help", Category.GENERAL, "/novaguard help", "commands.help.descriptions.help"),
            new CommandMeta("status", "novaguard.status", Category.PLAYER_INFO, "/novaguard status <Spieler>", "commands.help.descriptions.status"),
            new CommandMeta("packetinfo", "novaguard.packetinfo", Category.PACKET_PROTECTION, "/novaguard packetinfo <Spieler>", "commands.help.descriptions.packetinfo"),
            new CommandMeta("iplimit", "novaguard.iplimit.manage", Category.IP_PROTECTION, "/novaguard iplimit <set|reset|check> <IP> [Limit]", "commands.help.descriptions.iplimit"),
            new CommandMeta("ipblacklist", "novaguard.ipblacklist.manage", Category.IP_PROTECTION, "/novaguard ipblacklist <add|remove|list|check> <IP>", "commands.help.descriptions.ipblacklist"),
            new CommandMeta("whitelist", "novaguard.whitelist.manage", Category.PLAYER_MANAGEMENT, "/novaguard whitelist <add|remove|list|check> <Spieler|UUID>", "commands.help.descriptions.whitelist"),
            new CommandMeta("blacklist", "novaguard.blacklist.manage", Category.PLAYER_MANAGEMENT, "/novaguard blacklist <add|remove|list|check> <Spieler|UUID> [Grund]", "commands.help.descriptions.blacklist"),
            new CommandMeta("perf", "novaguard.performance.view", Category.ADMINISTRATION, "/novaguard perf", "commands.help.descriptions.perf"),
            new CommandMeta("reload", "novaguard.reload", Category.ADMINISTRATION, "/novaguard reload", "commands.help.descriptions.reload")
    );

    private CommandCatalog() {
    }

    public static List<CommandMeta> commands() {
        return COMMANDS;
    }

    public static CommandMeta find(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (CommandMeta command : COMMANDS) {
            if (command.name().equals(lower)) {
                return command;
            }
        }
        return null;
    }

    public static boolean canUse(CommandMeta command, Predicate<String> permissionCheck) {
        return command.permission() == null || permissionCheck.test(command.permission());
    }

    public static List<String> visibleRoots(Predicate<String> permissionCheck, String token) {
        String prefix = token.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (CommandMeta command : COMMANDS) {
            if (canUse(command, permissionCheck) && command.name().startsWith(prefix)) {
                result.add(command.name());
            }
        }
        return result;
    }

    public enum Category {
        GENERAL("commands.help.categories.general"),
        PLAYER_INFO("commands.help.categories.player-info"),
        IP_PROTECTION("commands.help.categories.ip-protection"),
        PLAYER_MANAGEMENT("commands.help.categories.player-management"),
        PACKET_PROTECTION("commands.help.categories.packet-protection"),
        ADMINISTRATION("commands.help.categories.administration");

        private final String messageKey;

        Category(String messageKey) {
            this.messageKey = messageKey;
        }

        public String messageKey() {
            return messageKey;
        }
    }

    public record CommandMeta(String name, String permission, Category category, String syntax, String descriptionKey) {
    }
}
