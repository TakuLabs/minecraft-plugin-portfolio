package de.otaku.novaguard.config;

import de.otaku.novaguard.util.ResourceUtil;
import de.otaku.novaguard.util.YamlFiles;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

public final class MessageService {
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private YamlConfiguration messages;

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        try {
            messages = YamlFiles.load(ResourceUtil.ensureResource(plugin, "messages.yml").toPath());
        } catch (Exception ex) {
            plugin.getLogger().severe("Could not load messages.yml as UTF-8, using fallback messages: " + ex.getMessage());
            messages = new YamlConfiguration();
        }
    }

    public Component component(String key) {
        return miniMessage.deserialize(raw(key));
    }

    public Component component(String key, Map<String, String> placeholders) {
        try {
            return miniMessage.deserialize(KickScreenRenderer.applyPlaceholders(raw(key), placeholders, miniMessage));
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Invalid MiniMessage for message '" + key + "', using fallback: " + ex.getMessage());
            return Component.text("ExampleSMP » Deine Aktion konnte nicht verarbeitet werden.");
        }
    }

    public Component prefixed(String key) {
        return miniMessage.deserialize(raw("prefix") + " " + raw(key));
    }

    public Component prefixed(String key, Map<String, String> placeholders) {
        try {
            return miniMessage.deserialize(raw("prefix") + " " + KickScreenRenderer.applyPlaceholders(raw(key), placeholders, miniMessage));
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Invalid MiniMessage for message '" + key + "', using fallback: " + ex.getMessage());
            return Component.text("ExampleSMP » Deine Aktion konnte nicht verarbeitet werden.");
        }
    }

    public Component kickScreen(String key, Map<String, String> placeholders) {
        List<String> lines = messages.getStringList("kick-screens." + key);
        if (lines.isEmpty()) {
            plugin.getLogger().warning("Missing kick-screen '" + key + "', using fallback.");
            return Component.text("ExampleSMP » Deine Verbindung wurde abgelehnt.");
        }
        return KickScreenRenderer.render(
                miniMessage,
                lines,
                placeholders,
                "ExampleSMP » Deine Verbindung wurde abgelehnt.",
                plugin.getLogger()::warning
        );
    }

    public String raw(String key) {
        return messages.getString(key, fallback(key));
    }

    private String fallback(String key) {
        return switch (key) {
            case "prefix" -> "<dark_purple><bold>ExampleSMP</bold></dark_purple> <dark_gray>»</dark_gray>";
            case "ip-limit" -> "<red>Über deine IP-Adresse sind bereits zu viele Accounts gleichzeitig mit dem Server verbunden.</red>";
            case "uuid-blacklisted" -> "<red>Dein Account darf diesen Server nicht betreten.</red>";
            case "ip-blacklisted" -> "<red>Über diese Verbindung ist kein Zugriff auf den Server möglich.</red>";
            case "packet-kick" -> "<red>Deine Verbindung wurde aufgrund ungewöhnlich vieler Netzwerkpakete getrennt.</red>";
            case "no-permission" -> "<red>Dazu hast du keine Berechtigung.</red>";
            default -> "<red>NovaGuard message missing: " + key + "</red>";
        };
    }
}
