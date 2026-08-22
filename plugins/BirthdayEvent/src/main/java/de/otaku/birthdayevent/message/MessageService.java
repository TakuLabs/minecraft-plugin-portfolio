package de.otaku.birthdayevent.message;

import de.otaku.birthdayevent.config.PluginSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.Map;

public final class MessageService {
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private YamlConfiguration messages;

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public void send(CommandSender sender, String key) {
        sender.sendMessage(component(key));
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(component(key, placeholders));
    }

    public Component component(String key) {
        return deserialize(messages.getString(key, key));
    }

    public Component component(String key, Map<String, String> placeholders) {
        return deserialize(messages.getString(key, key), placeholders);
    }

    public List<String> rawList(String key) {
        return messages.getStringList(key);
    }

    public Component deserialize(String raw) {
        return miniMessage.deserialize(raw.replace("%prefix%", messages.getString("prefix", "")));
    }

    public Component deserialize(String raw, Map<String, String> placeholders) {
        String resolved = raw.replace("%prefix%", messages.getString("prefix", ""));
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolved = resolved.replace("%" + entry.getKey() + "%", miniMessage.escapeTags(entry.getValue()));
        }
        return miniMessage.deserialize(resolved);
    }

    public void sendInvitation(Player player, PluginSettings settings) {
        Component button = component("button")
                .clickEvent(ClickEvent.runCommand("/birthdayevent gratulieren"))
                .hoverEvent(HoverEvent.showText(component("button-hover")));

        for (String line : rawList("invite-lines")) {
            if (line.equals("%button%")) {
                player.sendMessage(button);
            } else {
                player.sendMessage(deserialize(line, Map.of("age", String.valueOf(settings.event().age()))));
            }
        }
    }

    public void sendTestInvitation(Player player, PluginSettings settings) {
        Component button = component("button")
                .clickEvent(ClickEvent.runCommand("/birthdayevent testclick"))
                .hoverEvent(HoverEvent.showText(component("button-hover")));

        for (String line : rawList("invite-lines")) {
            if (line.equals("%button%")) {
                player.sendMessage(button);
            } else {
                player.sendMessage(deserialize(line, Map.of("age", String.valueOf(settings.event().age()))));
            }
        }
    }

    public static Map<String, String> placeholders(String player, String uuid, String crate, int amount) {
        String keyWord = amount == 1 ? "Key" : "Keys";
        return Map.of(
                "player", player,
                "uuid", uuid,
                "crate", crate,
                "amount", String.valueOf(amount),
                "key_word", keyWord
        );
    }
}
