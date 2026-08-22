package de.otaku.novaguard.packet;

import com.comphenix.protocol.PacketType;
import de.otaku.novaguard.config.PluginSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class PacketTypeRegistry {
    private PacketTypeRegistry() {
    }

    public static List<PacketType> supportedInboundPlayTypes(JavaPlugin plugin, PluginSettings settings) {
        List<PacketType> registered = filter(PacketType.Play.Client.getInstance().values(), PacketTypeRegistry::isRegistrableInboundPlayType);
        PacketClassifier.prepare(registered);
        plugin.getLogger().info("[NovaGuard] Packet protection initialized.");
        plugin.getLogger().info("[NovaGuard] Registered inbound packet types: " + registered.size());
        plugin.getLogger().info("[NovaGuard] Packet protection mode: " + settings.packetMode());
        if (settings.debug()) {
            logDebugPacketAudit(plugin, registered);
        }
        return registered;
    }

    static List<PacketType> filter(Iterable<PacketType> candidates, Predicate<PacketType> supportedPredicate) {
        return filterCandidates(candidates, PacketTypeRegistry::isInboundPlayType, supportedPredicate);
    }

    static <T> List<T> filterCandidates(Iterable<T> candidates, Predicate<T> inboundPredicate, Predicate<T> supportedPredicate) {
        List<T> registered = new ArrayList<>();
        for (T candidate : candidates) {
            if (inboundPredicate.test(candidate) && supportedPredicate.test(candidate)) {
                registered.add(candidate);
            }
        }
        return registered;
    }

    static boolean isRegistrableInboundPlayType(PacketType type) {
        return isInboundPlayType(type) && type.isSupported();
    }

    static boolean isInboundPlayType(PacketType type) {
        return type != null
                && type.getProtocol() == PacketType.Protocol.PLAY
                && type.getSender() == PacketType.Sender.CLIENT
                && type.isClient();
    }

    private static void logDebugPacketAudit(JavaPlugin plugin, List<PacketType> registered) {
        for (PacketType type : PacketType.Play.Client.getInstance().values()) {
            if (registered.contains(type)) {
                plugin.getLogger().info("[NovaGuard DEBUG] Registered packet: " + type.name()
                        + " category=" + PacketClassifier.category(type)
                        + " supported=" + type.isSupported());
            } else if (isInboundPlayType(type)) {
                plugin.getLogger().info("[NovaGuard DEBUG] Skipped unsupported packet: " + type.name());
            }
        }
    }
}
