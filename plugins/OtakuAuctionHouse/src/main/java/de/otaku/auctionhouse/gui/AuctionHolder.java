package de.otaku.auctionhouse.gui;

import de.otaku.auctionhouse.model.AuctionListing;
import de.otaku.auctionhouse.model.AuctionCategory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AuctionHolder implements InventoryHolder {
    private final Map<Integer, UUID> slotListings = new HashMap<>();
    private int page;
    private final AuctionCategory category;
    private @Nullable Inventory inventory;

    public AuctionHolder(int page, AuctionCategory category) {
        this.page = page;
        this.category = category;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("Inventory wurde noch nicht gesetzt.");
        }
        return inventory;
    }

    public void inventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public void put(int slot, AuctionListing listing) {
        slotListings.put(slot, listing.id());
    }

    public UUID listingId(int slot) {
        return slotListings.get(slot);
    }

    public int page() {
        return page;
    }

    public void page(int page) {
        this.page = page;
    }

    public AuctionCategory category() {
        return category;
    }
}
