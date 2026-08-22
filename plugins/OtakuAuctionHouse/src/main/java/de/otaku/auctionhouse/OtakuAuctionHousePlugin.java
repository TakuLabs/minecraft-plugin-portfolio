package de.otaku.auctionhouse;

import de.otaku.auctionhouse.command.AuctionCommand;
import de.otaku.auctionhouse.gui.AuctionGuiListener;
import de.otaku.auctionhouse.storage.ListingStore;
import de.otaku.auctionhouse.storage.NotificationStore;
import de.otaku.auctionhouse.util.Texts;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class OtakuAuctionHousePlugin extends JavaPlugin {
    private Economy economy;
    private ListingStore listingStore;
    private NotificationStore notificationStore;
    private Texts texts;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        texts = new Texts(this);

        if (!setupEconomy()) {
            getLogger().severe("Vault oder ein Economy-Plugin wurde nicht gefunden. Plugin wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        listingStore = new ListingStore(this);
        listingStore.load();
        notificationStore = new NotificationStore(this);
        notificationStore.load();

        AuctionCommand command = new AuctionCommand(this);
        getCommand("ah").setExecutor(command);
        getCommand("ah").setTabCompleter(command);
        getServer().getPluginManager().registerEvents(new AuctionGuiListener(this), this);
        getServer().getScheduler().runTaskTimer(this, () -> listingStore.expireOldListings(), 20L * 60L, 20L * 60L);

        getLogger().info("OtakuAuctionHouse wurde aktiviert.");
    }

    @Override
    public void onDisable() {
        if (listingStore != null) {
            listingStore.save();
        }
        if (notificationStore != null) {
            notificationStore.save();
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        texts = new Texts(this);
        listingStore.load();
        notificationStore.load();
    }

    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> provider = getServer().getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            return false;
        }
        economy = provider.getProvider();
        return economy != null;
    }

    public Economy economy() {
        return economy;
    }

    public ListingStore listingStore() {
        return listingStore;
    }

    public NotificationStore notificationStore() {
        return notificationStore;
    }

    public Texts texts() {
        return texts;
    }
}
