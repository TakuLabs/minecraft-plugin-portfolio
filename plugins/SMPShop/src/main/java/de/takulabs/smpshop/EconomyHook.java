package de.takulabs.smpshop;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;

final class EconomyHook {
    private final Object economy;
    private final Method hasMethod;
    private final Method withdrawMethod;
    private final Method depositMethod;

    EconomyHook(SMPShopPlugin plugin) {
        Object foundEconomy = null;
        Method foundHas = null;
        Method foundWithdraw = null;
        Method foundDeposit = null;

        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            try {
                Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
                RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(economyClass);
                if (registration != null) {
                    foundEconomy = registration.getProvider();
                    foundHas = economyClass.getMethod("has", OfflinePlayer.class, double.class);
                    foundWithdraw = economyClass.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
                    foundDeposit = economyClass.getMethod("depositPlayer", OfflinePlayer.class, double.class);
                }
            } catch (ReflectiveOperationException exception) {
                plugin.getLogger().warning("Vault hook failed: " + exception.getMessage());
            }
        }

        this.economy = foundEconomy;
        this.hasMethod = foundHas;
        this.withdrawMethod = foundWithdraw;
        this.depositMethod = foundDeposit;
    }

    boolean isReady() {
        return economy != null;
    }

    boolean has(Player player, double amount) {
        if (!isReady()) {
            return false;
        }
        try {
            return (boolean) hasMethod.invoke(economy, player, amount);
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }

    boolean withdraw(Player player, double amount) {
        return transact(player, amount, withdrawMethod);
    }

    boolean deposit(Player player, double amount) {
        return transact(player, amount, depositMethod);
    }

    boolean deposit(OfflinePlayer player, double amount) {
        return transact(player, amount, depositMethod);
    }

    private boolean transact(OfflinePlayer player, double amount, Method method) {
        if (!isReady()) {
            return false;
        }
        try {
            Object response = method.invoke(economy, player, amount);
            Method success = response.getClass().getMethod("transactionSuccess");
            return (boolean) success.invoke(response);
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }
}

