package de.otaku.novaguard.packet;

import com.comphenix.protocol.PacketType;
import de.otaku.novaguard.config.PacketWeights;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class PacketClassifier {
    private static final ConcurrentMap<PacketType, PacketCategory> CACHE = new ConcurrentHashMap<>();

    private PacketClassifier() {
    }

    public static PacketCategory category(PacketType type) {
        PacketCategory cached = CACHE.get(type);
        if (cached != null) {
            return cached;
        }
        PacketCategory classified = classify(type);
        PacketCategory previous = CACHE.putIfAbsent(type, classified);
        return previous == null ? classified : previous;
    }

    public static void prepare(Iterable<PacketType> types) {
        for (PacketType type : types) {
            category(type);
        }
    }

    private static PacketCategory classify(PacketType type) {
        return categoryName(type.name());
    }

    static PacketCategory categoryName(String name) {
        if (name.contains("POSITION") || name.contains("LOOK") || name.contains("GROUND")
                || name.contains("MOVE") || name.contains("STEER") || name.contains("PLAYER_INPUT")) {
            return PacketCategory.MOVEMENT;
        }
        if (name.contains("WINDOW") || name.contains("CLICK") || name.contains("CREATIVE") || name.contains("HELD_ITEM")
                || name.contains("RECIPE") || name.contains("BEACON")) {
            return PacketCategory.INVENTORY;
        }
        if (name.contains("USE_ENTITY") || name.contains("USE_ITEM") || name.contains("ARM_ANIMATION")
                || name.contains("BLOCK_PLACE") || name.contains("BLOCK_DIG")) {
            return PacketCategory.INTERACTION;
        }
        if (name.contains("COMMAND") || name.contains("TAB_COMPLETE")) {
            return PacketCategory.COMMAND;
        }
        if (name.contains("CHAT")) {
            return PacketCategory.CHAT;
        }
        if (name.contains("CUSTOM_PAYLOAD")) {
            return PacketCategory.CUSTOM_PAYLOAD;
        }
        return PacketCategory.OTHER;
    }

    public static double weight(PacketCategory category, PacketWeights weights) {
        return switch (category) {
            case MOVEMENT -> weights.movement();
            case INVENTORY -> weights.inventory();
            case INTERACTION -> weights.interaction();
            case COMMAND -> weights.command();
            case CHAT -> weights.chat();
            case CUSTOM_PAYLOAD -> weights.customPayload();
            case OTHER -> weights.other();
        };
    }
}
