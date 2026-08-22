package de.otaku.auctionhouse.util;

import de.otaku.auctionhouse.OtakuAuctionHousePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;

public final class Texts {
    private final OtakuAuctionHousePlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public Texts(OtakuAuctionHousePlugin plugin) {
        this.plugin = plugin;
    }

    public Component title() {
        return parse(plugin.getConfig().getString("colors.title", "<bold><gradient:#6D28D9:#06B6D4>Mythic Auktionshaus</gradient></bold>"));
    }

    public Component parse(String text) {
        return miniMessage.deserialize(text);
    }

    public String escape(String text) {
        return miniMessage.escapeTags(text);
    }

    public String gradient(String text) {
        return "<gradient:#6D28D9:#06B6D4>" + escape(text) + "</gradient>";
    }

    public Component prefixed(String text) {
        String prefix = plugin.getConfig().getString("colors.prefix", "<dark_gray>[</dark_gray><bold><gradient:#6D28D9:#06B6D4>MythicAH</gradient></bold><dark_gray>]</dark_gray> ");
        return parse(prefix + text);
    }

    public void send(CommandSender sender, String text) {
        sender.sendMessage(prefixed(text));
    }

    public String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
