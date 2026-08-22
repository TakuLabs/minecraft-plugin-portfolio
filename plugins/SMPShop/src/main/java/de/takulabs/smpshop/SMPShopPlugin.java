package de.takulabs.smpshop;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SMPShopPlugin extends JavaPlugin {
    private EconomyHook economyHook;
    private PlayerPointsHook playerPointsHook;
    private PlaceholderHook placeholderHook;
    private ShopManager shopManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadComponents();

        PluginCommand command = getCommand("shop");
        if (command != null) {
            ShopCommand shopCommand = new ShopCommand(this);
            command.setExecutor(shopCommand);
            command.setTabCompleter(shopCommand);
        }

        Bukkit.getPluginManager().registerEvents(new ShopListener(this), this);
        getLogger().info("SMPShop enabled.");
    }

    void reloadComponents() {
        reloadConfig();
        this.economyHook = new EconomyHook(this);
        this.playerPointsHook = new PlayerPointsHook(this);
        this.placeholderHook = new PlaceholderHook(this);
        this.shopManager = new ShopManager(this);
    }

    EconomyHook economyHook() {
        return economyHook;
    }

    PlayerPointsHook playerPointsHook() {
        return playerPointsHook;
    }

    PlaceholderHook placeholderHook() {
        return placeholderHook;
    }

    ShopManager shopManager() {
        return shopManager;
    }
}
