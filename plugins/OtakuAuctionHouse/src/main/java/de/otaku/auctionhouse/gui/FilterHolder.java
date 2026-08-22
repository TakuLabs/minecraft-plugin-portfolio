package de.otaku.auctionhouse.gui;

import de.otaku.auctionhouse.model.AuctionCategory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class FilterHolder implements InventoryHolder {
    private final Map<Integer, AuctionCategory> categories = new HashMap<>();
    private final int previousPage;
    private final AuctionCategory previousCategory;
    private Inventory inventory;

    public FilterHolder(int previousPage, AuctionCategory previousCategory) {
        this.previousPage = previousPage;
        this.previousCategory = previousCategory;
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

    public void put(int slot, AuctionCategory category) {
        categories.put(slot, category);
    }

    public AuctionCategory category(int slot) {
        return categories.get(slot);
    }

    public int previousPage() {
        return previousPage;
    }

    public AuctionCategory previousCategory() {
        return previousCategory;
    }
}
