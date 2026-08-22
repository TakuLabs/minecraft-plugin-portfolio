package de.otaku.auctionhouse.storage;

import de.otaku.auctionhouse.OtakuAuctionHousePlugin;
import de.otaku.auctionhouse.model.AuctionListing;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ListingStore {
    private final OtakuAuctionHousePlugin plugin;
    private final File file;
    private final List<AuctionListing> listings = new ArrayList<>();
    private final List<AuctionListing> expiredListings = new ArrayList<>();

    public ListingStore(OtakuAuctionHousePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "listings.yml");
    }

    public void load() {
        listings.clear();
        expiredListings.clear();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        loadSection(config, "listings", listings);
        loadSection(config, "expired", expiredListings);
        listings.sort(Comparator.comparingLong(AuctionListing::createdAt).reversed());
        expiredListings.sort(Comparator.comparingLong(AuctionListing::createdAt).reversed());
    }

    private void loadSection(YamlConfiguration config, String sectionName, List<AuctionListing> target) {
        ConfigurationSection section = config.getConfigurationSection(sectionName);
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                String path = sectionName + "." + key + ".";
                ItemStack item = ItemSerializer.fromBase64(config.getString(path + "item", ""));
                target.add(new AuctionListing(
                        UUID.fromString(key),
                        UUID.fromString(config.getString(path + "seller-id")),
                        config.getString(path + "seller-name", "Unbekannt"),
                        config.getDouble(path + "price"),
                        config.getLong(path + "created-at"),
                        config.getLong(path + "expires-at", config.getLong(path + "created-at") + defaultDurationMillis()),
                        item
                ));
            } catch (Exception exception) {
                plugin.getLogger().warning("Angebot " + key + " konnte nicht geladen werden: " + exception.getMessage());
            }
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        saveSection(config, "listings", listings);
        saveSection(config, "expired", expiredListings);

        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("listings.yml konnte nicht gespeichert werden: " + exception.getMessage());
        }
    }

    private void saveSection(YamlConfiguration config, String sectionName, List<AuctionListing> source) {
        for (AuctionListing listing : source) {
            try {
                String path = sectionName + "." + listing.id() + ".";
                config.set(path + "seller-id", listing.sellerId().toString());
                config.set(path + "seller-name", listing.sellerName());
                config.set(path + "price", listing.price());
                config.set(path + "created-at", listing.createdAt());
                config.set(path + "expires-at", listing.expiresAt());
                config.set(path + "item", ItemSerializer.toBase64(listing.item()));
            } catch (IOException exception) {
                plugin.getLogger().warning("Angebot " + listing.id() + " konnte nicht gespeichert werden: " + exception.getMessage());
            }
        }
    }

    public AuctionListing create(Player seller, ItemStack item, double price, long durationHours) {
        long createdAt = System.currentTimeMillis();
        AuctionListing listing = new AuctionListing(
                UUID.randomUUID(),
                seller.getUniqueId(),
                seller.getName(),
                price,
                createdAt,
                createdAt + durationHours * 60L * 60L * 1000L,
                item.clone()
        );
        listings.add(0, listing);
        save();
        return listing;
    }

    public boolean remove(UUID id) {
        boolean removed = listings.removeIf(listing -> listing.id().equals(id));
        if (removed) {
            save();
        }
        return removed;
    }

    public Optional<AuctionListing> find(UUID id) {
        return listings.stream().filter(listing -> listing.id().equals(id)).findFirst();
    }

    public boolean removeExpired(UUID id) {
        boolean removed = expiredListings.removeIf(listing -> listing.id().equals(id));
        if (removed) {
            save();
        }
        return removed;
    }

    public Optional<AuctionListing> findExpired(UUID id) {
        return expiredListings.stream().filter(listing -> listing.id().equals(id)).findFirst();
    }

    public List<AuctionListing> listings() {
        expireOldListings();
        return List.copyOf(listings);
    }

    public List<AuctionListing> expiredListings(UUID sellerId) {
        expireOldListings();
        return expiredListings.stream()
                .filter(listing -> listing.sellerId().equals(sellerId))
                .toList();
    }

    public int activeListings(UUID sellerId) {
        expireOldListings();
        int count = 0;
        for (AuctionListing listing : listings) {
            if (listing.sellerId().equals(sellerId)) {
                count++;
            }
        }
        return count;
    }

    public void expireOldListings() {
        long now = System.currentTimeMillis();
        List<AuctionListing> expired = listings.stream()
                .filter(listing -> now >= listing.expiresAt())
                .toList();
        if (expired.isEmpty()) {
            return;
        }
        listings.removeAll(expired);
        expiredListings.addAll(0, expired);
        save();
    }

    private long defaultDurationMillis() {
        return plugin.getConfig().getLong("settings.default-listing-duration-hours", 72L) * 60L * 60L * 1000L;
    }
}
