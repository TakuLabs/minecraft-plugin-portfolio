package de.takulabs.smpshop;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

final class PlayerPointsHook {
    private final Object api;
    private final Method lookMethod;
    private final Method takeMethod;
    private final Method giveMethod;

    PlayerPointsHook(SMPShopPlugin plugin) {
        Object foundApi = null;
        Method foundLook = null;
        Method foundTake = null;
        Method foundGive = null;

        if (Bukkit.getPluginManager().isPluginEnabled("PlayerPoints")) {
            try {
                Class<?> playerPointsClass = Class.forName("org.black_ixx.playerpoints.PlayerPoints");
                Object instance = playerPointsClass.getMethod("getInstance").invoke(null);
                foundApi = playerPointsClass.getMethod("getAPI").invoke(instance);
                Class<?> apiClass = foundApi.getClass();
                foundLook = apiClass.getMethod("look", UUID.class);
                foundTake = apiClass.getMethod("take", UUID.class, int.class);
                foundGive = apiClass.getMethod("give", UUID.class, int.class);
            } catch (ReflectiveOperationException exception) {
                plugin.getLogger().warning("PlayerPoints hook failed: " + exception.getMessage());
            }
        }

        this.api = foundApi;
        this.lookMethod = foundLook;
        this.takeMethod = foundTake;
        this.giveMethod = foundGive;
    }

    boolean isReady() {
        return api != null;
    }

    boolean has(Player player, double amount) {
        if (!isReady()) {
            return false;
        }
        try {
            Object result = lookMethod.invoke(api, player.getUniqueId());
            return result instanceof Number number && number.intValue() >= Math.ceil(amount);
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    boolean take(Player player, double amount) {
        return transact(player, amount, takeMethod);
    }

    boolean give(Player player, double amount) {
        return transact(player, amount, giveMethod);
    }

    private boolean transact(Player player, double amount, Method method) {
        if (!isReady()) {
            return false;
        }
        try {
            Object result = method.invoke(api, player.getUniqueId(), (int) Math.ceil(amount));
            return !(result instanceof Boolean success) || success;
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }
}

