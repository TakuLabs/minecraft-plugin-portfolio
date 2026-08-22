package de.otaku.novaguard.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class KickScreenRenderer {
    private static final Set<String> SUPPORTED_PLACEHOLDERS = Set.of(
            "player",
            "uuid",
            "ip",
            "reason",
            "max_accounts",
            "current_accounts",
            "timestamp",
            "limit",
            "pps",
            "violations",
            "value",
            "rate",
            "tokens",
            "list",
            "action",
            "entry",
            "count",
            "command",
            "syntax",
            "permission"
    );
    private static final Set<String> OPTIONAL_PLACEHOLDERS = Set.of("reason");

    private KickScreenRenderer() {
    }

    public static Component render(MiniMessage miniMessage, List<String> lines, Map<String, String> placeholders,
                                   String fallback, Consumer<String> warningLogger) {
        try {
            String joined = String.join("\n", lines);
            validateTemplate(joined, placeholders);
            String resolved = applyPlaceholders(filterOptionalLines(lines, placeholders), placeholders, miniMessage);
            return miniMessage.deserialize(resolved);
        } catch (RuntimeException ex) {
            warningLogger.accept("Invalid kick-screen MiniMessage, using fallback: " + ex.getMessage());
            return Component.text(fallback);
        }
    }

    public static String applyPlaceholders(String input, Map<String, String> placeholders) {
        return applyPlaceholders(input, placeholders, MiniMessage.miniMessage());
    }

    public static String applyPlaceholders(String input, Map<String, String> placeholders, MiniMessage miniMessage) {
        String result = input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", sanitize(entry.getValue(), miniMessage));
        }
        return result;
    }

    private static String sanitize(String value, MiniMessage miniMessage) {
        String cleaned = value == null ? "" : value.replace("\n", " ").replace("\r", " ");
        return miniMessage.escapeTags(cleaned);
    }

    private static void validateTemplate(String input, Map<String, String> placeholders) {
        if (input.lastIndexOf('<') > input.lastIndexOf('>')) {
            throw new IllegalArgumentException("Unclosed MiniMessage tag");
        }
        for (String placeholder : SUPPORTED_PLACEHOLDERS) {
            if (input.contains("%" + placeholder + "%")
                    && !OPTIONAL_PLACEHOLDERS.contains(placeholder)
                    && !placeholders.containsKey(placeholder)) {
                throw new IllegalArgumentException("Missing placeholder: " + placeholder);
            }
        }
    }

    private static String filterOptionalLines(List<String> lines, Map<String, String> placeholders) {
        List<String> kept = new ArrayList<>(lines.size());
        for (String line : lines) {
            if (isMissingOptionalLine(line, placeholders)) {
                continue;
            }
            kept.add(line);
        }
        return String.join("\n", kept);
    }

    private static boolean isMissingOptionalLine(String line, Map<String, String> placeholders) {
        for (String placeholder : OPTIONAL_PLACEHOLDERS) {
            if (line.contains("%" + placeholder + "%")) {
                String value = placeholders.get(placeholder);
                return value == null || value.isBlank();
            }
        }
        return false;
    }
}
