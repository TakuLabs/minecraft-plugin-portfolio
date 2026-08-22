package de.otaku.auctionhouse.gui;

import de.otaku.auctionhouse.OtakuAuctionHousePlugin;
import de.otaku.auctionhouse.model.AuctionCategory;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public final class FilterGui {
    private static final int[] CATEGORY_SLOTS = {10, 11, 12, 13, 14, 15, 16, 22};

    private final OtakuAuctionHousePlugin plugin;

    public FilterGui(OtakuAuctionHousePlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int previousPage, AuctionCategory currentCategory) {
        FilterHolder holder = new FilterHolder(previousPage, currentCategory);
        Inventory inventory = Bukkit.createInventory(holder, 36, plugin.texts().parse("<bold><gradient:#6D28D9:#06B6D4>Filter auswählen</gradient></bold>"));
        holder.inventory(inventory);

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, pane());
        }

        AuctionCategory[] categories = AuctionCategory.values();
        for (int index = 0; index < Math.min(categories.length, CATEGORY_SLOTS.length); index++) {
            AuctionCategory category = categories[index];
            int slot = CATEGORY_SLOTS[index];
            inventory.setItem(slot, categoryItem(category, category == currentCategory));
            holder.put(slot, category);
        }

        inventory.setItem(31, backButton());
        player.openInventory(inventory);
    }

    private ItemStack categoryItem(AuctionCategory category, boolean selected) {
        ItemStack item = new ItemStack(material(category, selected));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.texts().parse("<bold><gradient:#6D28D9:#06B6D4>" + category.displayName() + "</gradient></bold>"));
            meta.lore(List.of(plugin.texts().parse(selected
                    ? "<green>Aktuell ausgewählt.</green>"
                    : "<gray>Klicke, um diesen Filter zu nutzen.</gray>")));
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material material(AuctionCategory category, boolean selected) {
        if (selected) {
            return Material.LIME_STAINED_GLASS_PANE;
        }
        return switch (category) {
            case NONE -> Material.BARRIER;
            case ARMOR -> Material.DIAMOND_CHESTPLATE;
            case WEAPONS -> Material.DIAMOND_SWORD;
            case TOOLS -> Material.DIAMOND_PICKAXE;
            case FOOD -> Material.GOLDEN_CARROT;
            case POTIONS -> Material.POTION;
            case BLOCKS -> Material.GRASS_BLOCK;
            case OTHER -> Material.CHEST;
        };
    }

    private ItemStack backButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.texts().parse("<bold><gradient:#6D28D9:#06B6D4>Zurück</gradient></bold>"));
            meta.lore(List.of(plugin.texts().parse("<gray>Zurück zum Auktionshaus.</gray>")));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack pane() {
        ItemStack item = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            item.setItemMeta(meta);
        }
        return item;
    }
}
