package de.otaku.auctionhouse.gui;

import de.otaku.auctionhouse.OtakuAuctionHousePlugin;
import de.otaku.auctionhouse.model.AuctionCategory;
import de.otaku.auctionhouse.model.AuctionListing;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class AuctionGuiListener implements Listener {
    private final OtakuAuctionHousePlugin plugin;

    public AuctionGuiListener(OtakuAuctionHousePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof CancelConfirmHolder holder) {
            handleCancelConfirmClick(event, holder);
            return;
        }
        if (event.getInventory().getHolder() instanceof ExpiredAuctionHolder holder) {
            handleExpiredClick(event, holder);
            return;
        }
        if (event.getInventory().getHolder() instanceof SellSetupHolder holder) {
            handleSellSetupClick(event, holder);
            return;
        }
        if (event.getInventory().getHolder() instanceof FilterHolder holder) {
            handleFilterClick(event, holder);
            return;
        }

        if (!(event.getInventory().getHolder() instanceof AuctionHolder holder)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player buyer)) {
            return;
        }

        int footerStart = event.getInventory().getSize() - 9;
        if (event.getRawSlot() >= footerStart) {
            handleFooterClick(buyer, event.getRawSlot() - footerStart, holder);
            return;
        }

        UUID listingId = holder.listingId(event.getRawSlot());
        if (listingId == null) {
            return;
        }

        plugin.listingStore().find(listingId).ifPresent(listing -> {
            if (event.isRightClick()) {
                if (buyer.getUniqueId().equals(listing.sellerId())) {
                    new CancelConfirmGui(plugin).open(buyer, listing);
                }
                return;
            }
            if (event.isLeftClick()) {
                buy(buyer, listing);
                return;
            }
        });
    }

    private void handleFooterClick(Player player, int footerSlot, AuctionHolder holder) {
        AuctionGui gui = new AuctionGui(plugin);
        if (footerSlot == 0) {
            gui.open(player, Math.max(0, holder.page() - 1), holder.category());
            return;
        }
        if (footerSlot == 2) {
            new FilterGui(plugin).open(player, holder.page(), holder.category());
            return;
        }
        if (footerSlot == 4) {
            gui.open(player, holder.page(), holder.category());
            return;
        }
        if (footerSlot == 6) {
            new ExpiredAuctionGui(plugin).open(player);
            return;
        }
        if (footerSlot == 8) {
            gui.open(player, holder.page() + 1, holder.category());
        }
    }

    private void handleFilterClick(InventoryClickEvent event, FilterHolder holder) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getRawSlot() == 31) {
            new AuctionGui(plugin).open(player, holder.previousPage(), holder.previousCategory());
            return;
        }

        AuctionCategory category = holder.category(event.getRawSlot());
        if (category == null) {
            return;
        }
        new AuctionGui(plugin).open(player, 0, category);
    }

    private void handleSellSetupClick(InventoryClickEvent event, SellSetupHolder holder) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        SellSetupGui gui = new SellSetupGui(plugin);
        double priceStep = plugin.getConfig().getDouble("settings.price-step", 100D);
        long durationStep = plugin.getConfig().getLong("settings.duration-step-hours", 24L);
        long defaultDuration = plugin.getConfig().getLong("settings.default-listing-duration-hours", 72L);
        long maxDuration = plugin.getConfig().getLong("settings.max-listing-duration-hours", 168L);

        if (event.getRawSlot() == 10) {
            player.closeInventory();
            return;
        }
        if (event.getRawSlot() == 11) {
            holder.price(Math.max(priceStep, holder.price() - priceStep));
            gui.open(player, holder);
            return;
        }
        if (event.getRawSlot() == 12) {
            holder.price(holder.price() + priceStep);
            gui.open(player, holder);
            return;
        }
        if (event.getRawSlot() == 14) {
            holder.durationHours(Math.max(defaultDuration, holder.durationHours() - durationStep));
            gui.open(player, holder);
            return;
        }
        if (event.getRawSlot() == 15) {
            holder.durationHours(Math.min(maxDuration, holder.durationHours() + durationStep));
            gui.open(player, holder);
            return;
        }
        if (event.getRawSlot() == 16) {
            confirmSell(player, holder);
        }
    }

    private void confirmSell(Player player, SellSetupHolder holder) {
        int active = plugin.listingStore().activeListings(player.getUniqueId());
        int max = de.otaku.auctionhouse.util.LimitResolver.maxListings(plugin, player);
        if (active >= max) {
            plugin.texts().send(player, "<red>Du hast dein Limit erreicht:</red> <gradient:#6D28D9:#06B6D4>" + active + "/" + max + "</gradient>");
            player.closeInventory();
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        ItemStack sellItem = holder.item();
        if (hand.getType().isAir() || !hand.isSimilar(sellItem) || hand.getAmount() < sellItem.getAmount()) {
            plugin.texts().send(player, "<red>Du musst das Verkaufsitem weiterhin in der Hand halten.</red>");
            player.closeInventory();
            return;
        }

        SellSetupGui gui = new SellSetupGui(plugin);
        double fee = gui.fee(holder.price(), holder.durationHours());
        if (fee > 0D && !plugin.economy().has(player, fee)) {
            plugin.texts().send(player, "<red>Du hast nicht genug Geld für die Einstellgebühr.</red>");
            return;
        }
        if (fee > 0D) {
            EconomyResponse feeResponse = plugin.economy().withdrawPlayer(player, fee);
            if (!feeResponse.transactionSuccess()) {
                plugin.texts().send(player, "<red>Die Einstellgebühr konnte nicht bezahlt werden.</red>");
                return;
            }
        }

        ItemStack remaining = hand.clone();
        remaining.setAmount(hand.getAmount() - sellItem.getAmount());
        player.getInventory().setItemInMainHand(remaining.getAmount() <= 0 ? null : remaining);
        plugin.listingStore().create(player, sellItem, holder.price(), holder.durationHours());
        plugin.texts().send(player, "<green>Dein Item wurde eingestellt.</green>");
        player.closeInventory();
    }

    private void handleExpiredClick(InventoryClickEvent event, ExpiredAuctionHolder holder) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getRawSlot() == 49) {
            new AuctionGui(plugin).open(player);
            return;
        }
        if (!event.isLeftClick()) {
            return;
        }

        UUID listingId = holder.listingId(event.getRawSlot());
        if (listingId == null) {
            return;
        }
        plugin.listingStore().findExpired(listingId).ifPresent(listing -> {
            if (!player.getUniqueId().equals(listing.sellerId())) {
                return;
            }
            if (!plugin.listingStore().removeExpired(listing.id())) {
                plugin.texts().send(player, "<red>Dieses abgelaufene Angebot ist nicht mehr verfügbar.</red>");
                new ExpiredAuctionGui(plugin).open(player);
                return;
            }
            HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(listing.item().clone());
            leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
            plugin.texts().send(player, "<green>Du hast dein abgelaufenes Angebot zurückgeholt.</green>");
            new ExpiredAuctionGui(plugin).open(player);
        });
    }

    private void handleCancelConfirmClick(InventoryClickEvent event, CancelConfirmHolder holder) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getRawSlot() == 15) {
            new AuctionGui(plugin).open(player);
            return;
        }
        if (event.getRawSlot() != 11) {
            return;
        }

        plugin.listingStore().find(holder.listingId()).ifPresentOrElse(listing -> {
            if (!player.getUniqueId().equals(listing.sellerId())) {
                player.closeInventory();
                return;
            }
            cancelListing(player, listing);
        }, () -> {
            plugin.texts().send(player, "<red>Dieses Angebot ist nicht mehr verfügbar.</red>");
            new AuctionGui(plugin).open(player);
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        List<String> messages = plugin.notificationStore().pop(player.getUniqueId());
        for (String message : messages) {
            plugin.texts().send(player, message);
        }
        if (!messages.isEmpty()) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.35F);
        }
    }

    private void cancelListing(Player seller, AuctionListing listing) {
        if (!plugin.listingStore().remove(listing.id())) {
            plugin.texts().send(seller, "<red>Dieses Angebot ist nicht mehr verfügbar.</red>");
            return;
        }
        HashMap<Integer, ItemStack> leftovers = seller.getInventory().addItem(listing.item().clone());
        leftovers.values().forEach(item -> seller.getWorld().dropItemNaturally(seller.getLocation(), item));
        plugin.texts().send(seller, "<green>Du hast dein Angebot zurückgenommen.</green>");
        new AuctionGui(plugin).open(seller);
    }

    private void buy(Player buyer, AuctionListing listing) {
        if (buyer.getUniqueId().equals(listing.sellerId())) {
            plugin.texts().send(buyer, "<red>Du kannst dein eigenes Angebot nicht kaufen.</red>");
            return;
        }

        if (!plugin.economy().has(buyer, listing.price())) {
            plugin.texts().send(buyer, "<red>Du hast nicht genug Geld für dieses Angebot.</red>");
            return;
        }

        if (!canFit(buyer, listing.item())) {
            plugin.texts().send(buyer, "<red>Du hast nicht genug Platz im Inventar.</red>");
            return;
        }

        EconomyResponse withdraw = plugin.economy().withdrawPlayer(buyer, listing.price());
        if (!withdraw.transactionSuccess()) {
            plugin.texts().send(buyer, "<red>Die Zahlung konnte nicht ausgeführt werden.</red>");
            return;
        }

        if (!plugin.listingStore().remove(listing.id())) {
            plugin.economy().depositPlayer(buyer, listing.price());
            plugin.texts().send(buyer, "<red>Dieses Angebot ist nicht mehr verfügbar.</red>");
            new AuctionGui(plugin).open(buyer);
            return;
        }

        plugin.economy().depositPlayer(plugin.getServer().getOfflinePlayer(listing.sellerId()), listing.price());
        HashMap<Integer, ItemStack> leftovers = buyer.getInventory().addItem(listing.item().clone());
        leftovers.values().forEach(item -> buyer.getWorld().dropItemNaturally(buyer.getLocation(), item));

        plugin.texts().send(buyer, "<green>Du hast das Angebot gekauft.</green>");
        notifySeller(listing);
        buyer.closeInventory();
    }

    private void notifySeller(AuctionListing listing) {
        DecimalFormat format = new DecimalFormat(plugin.getConfig().getString("settings.currency-format", "#,##0.00"));
        String itemName = itemName(listing.item());
        String message = "<green>Dein Angebot wurde verkauft:</green> "
                + plugin.texts().gradient(listing.item().getAmount() + "x " + itemName)
                + " <dark_gray>für</dark_gray> "
                + plugin.texts().gradient(format.format(listing.price()));

        Player seller = plugin.getServer().getPlayer(listing.sellerId());
        if (seller != null && seller.isOnline()) {
            plugin.texts().send(seller, message);
            seller.playSound(seller.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.35F);
            return;
        }
        plugin.notificationStore().add(listing.sellerId(), message);
    }

    private String itemName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return plugin.texts().plain(meta.displayName());
        }

        String key = item.getType().key().value().replace('_', ' ');
        String[] words = key.split(" ");
        StringBuilder name = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase());
        }
        return name.toString();
    }

    private boolean canFit(Player player, ItemStack item) {
        int remaining = item.getAmount();
        for (ItemStack content : player.getInventory().getStorageContents()) {
            if (content == null || content.getType().isAir()) {
                return true;
            }
            if (!content.isSimilar(item)) {
                continue;
            }
            remaining -= Math.max(0, content.getMaxStackSize() - content.getAmount());
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }
}
