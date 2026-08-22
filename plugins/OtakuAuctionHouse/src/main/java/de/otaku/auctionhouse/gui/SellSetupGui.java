package de.otaku.auctionhouse.gui;

import de.otaku.auctionhouse.OtakuAuctionHousePlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public final class SellSetupGui {
    private final OtakuAuctionHousePlugin plugin;

    public SellSetupGui(OtakuAuctionHousePlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, ItemStack item) {
        double price = plugin.getConfig().getDouble("settings.default-sell-price", 100D);
        open(player, item, price);
    }

    public void open(Player player, ItemStack item, double price) {
        long durationHours = plugin.getConfig().getLong("settings.default-listing-duration-hours", 72L);
        open(player, new SellSetupHolder(item, price, durationHours));
    }

    public void open(Player player, SellSetupHolder holder) {
        Inventory inventory = Bukkit.createInventory(holder, 27, plugin.texts().parse("<bold><gradient:#6D28D9:#06B6D4>Auktion erstellen</gradient></bold>"));
        holder.inventory(inventory);

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, pane());
        }

        inventory.setItem(10, button(Material.RED_WOOL,
                "<red><bold>Abbrechen</bold></red>",
                "<gray>Schließt das Verkaufsmenü.</gray>"));
        inventory.setItem(11, button(Material.RED_CANDLE,
                "<red><bold>Preis senken</bold></red>",
                "<gray>Senkt den Preis um den eingestellten Schritt.</gray>"));
        inventory.setItem(12, button(Material.LIME_CANDLE,
                "<green><bold>Preis erhöhen</bold></green>",
                "<gray>Erhöht den Preis um den eingestellten Schritt.</gray>"));
        inventory.setItem(13, preview(holder));
        inventory.setItem(14, button(Material.CLOCK,
                "<red><bold>Dauer senken</bold></red>",
                "<gray>Senkt die Laufzeit um den eingestellten Schritt.</gray>"));
        inventory.setItem(15, button(Material.RECOVERY_COMPASS,
                "<green><bold>Dauer erhöhen</bold></green>",
                "<gray>Erhöht die Laufzeit um den eingestellten Schritt.</gray>"));
        inventory.setItem(16, button(Material.LIME_WOOL,
                "<green><bold>Bestätigen</bold></green>",
                "<gray>Stellt das Item ins Auktionshaus.</gray>"));

        player.openInventory(inventory);
    }

    private ItemStack preview(SellSetupHolder holder) {
        ItemStack item = holder.item();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        DecimalFormat format = new DecimalFormat(plugin.getConfig().getString("settings.currency-format", "#,##0.00"));
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        if (!lore.isEmpty()) {
            lore.add(Component.empty());
        }
        lore.add(plugin.texts().parse("<bold><gradient:#6D28D9:#06B6D4>Verkaufseinstellungen</gradient></bold>"));
        lore.add(plugin.texts().parse("<gray>Preis:</gray> <gradient:#6D28D9:#06B6D4>" + format.format(holder.price()) + "</gradient>"));
        lore.add(plugin.texts().parse("<gray>Dauer:</gray> <gradient:#6D28D9:#06B6D4>" + durationText(holder.durationHours()) + "</gradient>"));
        lore.add(plugin.texts().parse("<gray>Gebühr:</gray> <gradient:#6D28D9:#06B6D4>" + format.format(fee(holder.price(), holder.durationHours())) + "</gradient>"));
        meta.lore(lore);
        item.setItemMeta(meta);
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

    private ItemStack pane() {
        ItemStack item = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    public double fee(double price, long durationHours) {
        long defaultHours = plugin.getConfig().getLong("settings.default-listing-duration-hours", 72L);
        if (durationHours <= defaultHours) {
            return 0D;
        }
        double extraDays = Math.ceil((durationHours - defaultHours) / 24D);
        double percentPerDay = plugin.getConfig().getDouble("settings.fee-percent-per-extra-day", 2D);
        return price * (percentPerDay / 100D) * extraDays;
    }

    public String durationText(long durationHours) {
        long days = durationHours / 24L;
        long hours = durationHours % 24L;
        if (hours == 0L) {
            return days == 1L ? "1 Tag" : days + " Tage";
        }
        return days + " Tage " + hours + " Stunden";
    }
}
