package de.otaku.auctionhouse.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class CancelConfirmHolder implements InventoryHolder {
    private final UUID listingId;
    private Inventory inventory;

    public CancelConfirmHolder(UUID listingId) {
        this.listingId = listingId;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("Inventory wurde noch nicht gesetzt.");
        }
        return inventory;
    }

    public UUID listingId() {
        return listingId;
    }

    public void inventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
