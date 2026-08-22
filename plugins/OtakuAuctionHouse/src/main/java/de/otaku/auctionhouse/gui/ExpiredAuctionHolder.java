package de.otaku.auctionhouse.gui;

import de.otaku.auctionhouse.model.AuctionListing;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ExpiredAuctionHolder implements InventoryHolder {
    private final Map<Integer, UUID> slotListings = new HashMap<>();
    private Inventory inventory;

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
}
