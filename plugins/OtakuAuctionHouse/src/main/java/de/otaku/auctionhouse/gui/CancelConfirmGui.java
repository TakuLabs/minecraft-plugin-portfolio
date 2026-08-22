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

import java.util.List;

public final class CancelConfirmGui {
    private final OtakuAuctionHousePlugin plugin;

    public CancelConfirmGui(OtakuAuctionHousePlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, AuctionListing listing) {
        CancelConfirmHolder holder = new CancelConfirmHolder(listing.id());
        Inventory inventory = Bukkit.createInventory(holder, 27, plugin.texts().parse("<bold><gradient:#6D28D9:#06B6D4>Angebot zurücknehmen?</gradient></bold>"));
        holder.inventory(inventory);

        inventory.setItem(11, button(Material.LIME_STAINED_GLASS_PANE,
                "<green><bold>Bestätigen</bold></green>",
                "<gray>Nimmt dein Angebot aus dem Auktionshaus.</gray>"));
        inventory.setItem(13, preview(listing.item()));
        inventory.setItem(15, button(Material.RED_STAINED_GLASS_PANE,
                "<red><bold>Abbrechen</bold></red>",
                "<gray>Zurück zum Auktionshaus.</gray>"));

        player.openInventory(inventory);
    }

    private ItemStack preview(ItemStack original) {
        ItemStack item = original.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<Component> lore = meta.lore();
            if (lore == null) {
                meta.lore(List.of(plugin.texts().parse("<dark_gray>Dieses Angebot zurücknehmen?</dark_gray>")));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack button(Material material, String name, String loreLine) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.texts().parse(name));
            meta.lore(List.of(plugin.texts().parse(loreLine)));
            item.setItemMeta(meta);
        }
        return item;
    }
}
