package de.otaku.auctionhouse.gui;

import de.otaku.auctionhouse.OtakuAuctionHousePlugin;
import de.otaku.auctionhouse.model.AuctionCategory;
import de.otaku.auctionhouse.model.AuctionListing;
import de.otaku.auctionhouse.util.LimitResolver;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class AuctionGui {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final OtakuAuctionHousePlugin plugin;

    public AuctionGui(OtakuAuctionHousePlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        open(player, 0, AuctionCategory.NONE);
    }

    public void open(Player player, int page, AuctionCategory category) {
        int size = Math.min(54, Math.max(9, plugin.getConfig().getInt("settings.gui-size", 54)));
        size = (size / 9) * 9;
        AuctionHolder holder = new AuctionHolder(Math.max(0, page), category);
        Inventory inventory = Bukkit.createInventory(holder, size, plugin.texts().title());
        holder.inventory(inventory);

        List<AuctionListing> listings = plugin.listingStore().listings().stream()
                .filter(listing -> category.matches(listing.item()))
                .toList();
        int contentSlots = size - 9;
        int maxPage = Math.max(0, (int) Math.ceil(listings.size() / (double) contentSlots) - 1);
        int currentPage = Math.min(Math.max(0, page), maxPage);
        holder.page(currentPage);
        int startIndex = currentPage * contentSlots;
        for (int slot = 0; slot < contentSlots && startIndex + slot < listings.size(); slot++) {
            AuctionListing listing = listings.get(startIndex + slot);
            inventory.setItem(slot, displayItem(player, listing));
            holder.put(slot, listing);
        }

        fillFooter(inventory, player, currentPage, maxPage, category);
        player.openInventory(inventory);
    }

    private ItemStack displayItem(Player viewer, AuctionListing listing) {
        ItemStack item = listing.item().clone();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        if (!lore.isEmpty()) {
            lore.add(Component.empty());
        }

        DecimalFormat format = new DecimalFormat(plugin.getConfig().getString("settings.currency-format", "#,##0.00"));
        lore.add(plugin.texts().parse("<bold><gradient:#6D28D9:#06B6D4>Auktionsinfo</gradient></bold>"));
        lore.add(plugin.texts().parse("<dark_gray>Verkäufer:</dark_gray> <gradient:#6D28D9:#06B6D4>" + listing.sellerName() + "</gradient>"));
        lore.add(plugin.texts().parse("<dark_gray>Preis:</dark_gray> <gradient:#6D28D9:#06B6D4>" + format.format(listing.price()) + "</gradient>"));
        lore.add(plugin.texts().parse("<dark_gray>Menge:</dark_gray> <gradient:#6D28D9:#06B6D4>" + listing.item().getAmount() + "</gradient>"));
        lore.add(plugin.texts().parse("<gray>Eingestellt:</gray> <white>" + TIME_FORMAT.format(Instant.ofEpochMilli(listing.createdAt())) + "</white>"));
        lore.add(plugin.texts().parse("<gray>Läuft ab:</gray> <white>" + TIME_FORMAT.format(Instant.ofEpochMilli(listing.expiresAt())) + "</white>"));
        lore.add(Component.empty());
        if (viewer.getUniqueId().equals(listing.sellerId())) {
            lore.add(plugin.texts().parse("<gradient:#6D28D9:#06B6D4>Rechtsklick zum Zurücknehmen</gradient>"));
        } else {
            lore.add(plugin.texts().parse("<gradient:#6D28D9:#06B6D4>Linksklick zum Kaufen</gradient>"));
        }

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        item.setItemMeta(meta);
        return item;
    }

    private void fillFooter(Inventory inventory, Player player, int page, int maxPage, AuctionCategory category) {
        int start = inventory.getSize() - 9;
        for (int slot = start; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, pane(Material.BLUE_STAINED_GLASS_PANE, " "));
        }

        inventory.setItem(start, button(Material.ARROW,
                "<bold><gradient:#6D28D9:#06B6D4>Vorherige Seite</gradient></bold>",
                "<gray>Seite:</gray> <white>" + (page + 1) + "/" + (maxPage + 1) + "</white>"));
        inventory.setItem(start + 2, button(Material.HOPPER,
                "<bold><gradient:#6D28D9:#06B6D4>Filter: " + category.displayName() + "</gradient></bold>",
                "<gray>Klicke, um den Filter auszuwählen.</gray>"));
        inventory.setItem(start + 6, button(Material.CHEST,
                "<bold><gradient:#6D28D9:#06B6D4>Abgelaufene Auktionen</gradient></bold>",
                "<gray>Hole abgelaufene Angebote zurück.</gray>"));
        inventory.setItem(start + 8, button(Material.ARROW,
                "<bold><gradient:#6D28D9:#06B6D4>Nächste Seite</gradient></bold>",
                "<gray>Seite:</gray> <white>" + (page + 1) + "/" + (maxPage + 1) + "</white>"));
        inventory.setItem(start + 4, button(Material.EMERALD,
                "<bold><gradient:#6D28D9:#06B6D4>Aktualisieren</gradient></bold>",
                "<gray>Lädt diese Ansicht neu.</gray>"));

        int active = plugin.listingStore().activeListings(player.getUniqueId());
        int max = LimitResolver.maxListings(plugin, player);
        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.texts().parse("<bold><gradient:#6D28D9:#06B6D4>Deine Verkaufsplätze</gradient></bold>"));
            meta.lore(List.of(
                    plugin.texts().parse("<dark_gray>Aktiv:</dark_gray> <gradient:#6D28D9:#06B6D4>" + active + "/" + max + "</gradient>"),
                    plugin.texts().parse("<gray>Verkaufe mit:</gray> <white>/ah sell</white>")
            ));
            info.setItemMeta(meta);
        }
        inventory.setItem(start + 7, info);
    }

    private ItemStack pane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name));
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
