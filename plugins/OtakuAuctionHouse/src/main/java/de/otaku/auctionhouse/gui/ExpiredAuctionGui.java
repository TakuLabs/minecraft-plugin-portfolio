package de.otaku.auctionhouse.gui;

import de.otaku.auctionhouse.OtakuAuctionHousePlugin;
import de.otaku.auctionhouse.model.AuctionListing;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class ExpiredAuctionGui {
    private final OtakuAuctionHousePlugin plugin;

    public ExpiredAuctionGui(OtakuAuctionHousePlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        ExpiredAuctionHolder holder = new ExpiredAuctionHolder();
        Inventory inventory = Bukkit.createInventory(holder, 54, plugin.texts().parse("<bold><gradient:#6D28D9:#06B6D4>Abgelaufene Auktionen</gradient></bold>"));
        holder.inventory(inventory);

        List<AuctionListing> expired = plugin.listingStore().expiredListings(player.getUniqueId());
        for (int slot = 0; slot < Math.min(45, expired.size()); slot++) {
            AuctionListing listing = expired.get(slot);
            inventory.setItem(slot, displayItem(listing));
            holder.put(slot, listing);
        }

        for (int slot = 45; slot < 54; slot++) {
            inventory.setItem(slot, pane());
        }
        inventory.setItem(49, backButton());
        player.openInventory(inventory);
    }

    private ItemStack displayItem(AuctionListing listing) {
        ItemStack item = listing.item().clone();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        if (!lore.isEmpty()) {
            lore.add(Component.empty());
        }
        lore.add(plugin.texts().parse("<bold><gradient:#6D28D9:#06B6D4>Abgelaufen</gradient></bold>"));
        lore.add(plugin.texts().parse("<gray>Linksklick zum Zurückholen.</gray>"));
        meta.lore(lore);
        item.setItemMeta(meta);
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
}
