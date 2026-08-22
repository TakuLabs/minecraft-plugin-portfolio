package de.takulabs.smpshop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

final class ShopHolder implements InventoryHolder {
    private final MenuType type;
    private final String categoryId;
    private final int page;
    private final String itemId;
    private final int quantity;

    ShopHolder(MenuType type, String categoryId, int page) {
        this(type, categoryId, page, "", 1);
    }

    ShopHolder(MenuType type, String categoryId, int page, String itemId, int quantity) {
        this.type = type;
        this.categoryId = categoryId;
        this.page = page;
        this.itemId = itemId;
        this.quantity = quantity;
    }

    MenuType type() {
        return type;
    }

    String categoryId() {
        return categoryId;
    }

    int page() {
        return page;
    }

    String itemId() {
        return itemId;
    }

    int quantity() {
        return quantity;
    }

    @Override
    public @NotNull Inventory getInventory() {
        throw new UnsupportedOperationException("SMPShop holder is a marker only.");
    }
}

