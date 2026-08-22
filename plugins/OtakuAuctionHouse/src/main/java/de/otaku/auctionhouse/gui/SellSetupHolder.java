package de.otaku.auctionhouse.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class SellSetupHolder implements InventoryHolder {
    private final ItemStack item;
    private double price;
    private long durationHours;
    private Inventory inventory;

    public SellSetupHolder(ItemStack item, double price, long durationHours) {
        this.item = item.clone();
        this.price = price;
        this.durationHours = durationHours;
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

    public ItemStack item() {
        return item.clone();
    }

    public double price() {
        return price;
    }

    public void price(double price) {
        this.price = price;
    }

    public long durationHours() {
        return durationHours;
    }

    public void durationHours(long durationHours) {
        this.durationHours = durationHours;
    }
}
