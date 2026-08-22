package de.otaku.auctionhouse.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

public enum AuctionCategory {
    NONE("Nichts"),
    ARMOR("Rüstung"),
    WEAPONS("Waffen"),
    TOOLS("Werkzeuge"),
    FOOD("Essen"),
    POTIONS("Tränke"),
    BLOCKS("Blöcke"),
    OTHER("Andere");

    private final String displayName;

    AuctionCategory(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public AuctionCategory next() {
        AuctionCategory[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public boolean matches(ItemStack item) {
        if (this == NONE) {
            return true;
        }
        return categoryOf(item) == this;
    }

    public static AuctionCategory categoryOf(ItemStack item) {
        Material material = item.getType();
        String name = material.name();

        if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")
                || name.equals("SHIELD") || name.equals("ELYTRA") || name.equals("TURTLE_HELMET")) {
            return ARMOR;
        }
        if (name.endsWith("_SWORD") || name.endsWith("_AXE") || name.equals("BOW") || name.equals("CROSSBOW")
                || name.equals("TRIDENT") || name.equals("MACE")) {
            return WEAPONS;
        }
        if (name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE")
                || name.equals("FISHING_ROD") || name.equals("SHEARS") || name.equals("FLINT_AND_STEEL")
                || name.equals("BRUSH") || name.equals("COMPASS") || name.equals("CLOCK")) {
            return TOOLS;
        }
        if (material.isEdible()) {
            return FOOD;
        }
        if (item.getItemMeta() instanceof PotionMeta || name.contains("POTION") || name.equals("TIPPED_ARROW")) {
            return POTIONS;
        }
        if (material.isBlock()) {
            return BLOCKS;
        }
        return OTHER;
    }
}
