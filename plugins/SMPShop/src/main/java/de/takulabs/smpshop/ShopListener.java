package de.takulabs.smpshop;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

final class ShopListener implements Listener {
    private final SMPShopPlugin plugin;

    ShopListener(SMPShopPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof ShopHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        int rawSlot = event.getRawSlot();
        if (holder.type() == MenuType.MAIN) {
            plugin.shopManager().categoryBySlot(rawSlot).ifPresent(category -> plugin.shopManager().openCategory(player, category.id(), 0));
            return;
        }

        if (holder.type() == MenuType.QUANTITY) {
            handleQuantityClick(player, holder, rawSlot);
            return;
        }

        int menuSize = event.getView().getTopInventory().getSize();
        int backSlot = menuSize - 9;
        int closeSlot = menuSize - 1;
        int previousSlot = menuSize - 6;
        int nextSlot = menuSize - 4;
        int contentSlots = Math.max(9, menuSize - 9);

        if (rawSlot == backSlot) {
            plugin.shopManager().openMain(player);
            return;
        }
        if (rawSlot == closeSlot) {
            plugin.shopManager().openMain(player);
            return;
        }
        if (rawSlot == previousSlot) {
            plugin.shopManager().openCategory(player, holder.categoryId(), holder.page() - 1);
            return;
        }
        if (rawSlot == nextSlot) {
            plugin.shopManager().openCategory(player, holder.categoryId(), holder.page() + 1);
            return;
        }
        if (rawSlot >= 0 && rawSlot < contentSlots) {
            plugin.shopManager().itemBySlot(holder.categoryId(), holder.page(), rawSlot).ifPresent(item -> {
                if (event.getClick().isLeftClick()) {
                    plugin.shopManager().openQuantity(player, holder.categoryId(), holder.page(), item, 1);
                } else if (event.getClick().isRightClick()) {
                    sell(player, item);
                }
            });
        }
    }

    private void handleQuantityClick(Player player, ShopHolder holder, int slot) {
        if (slot == 18) {
            plugin.shopManager().openCategory(player, holder.categoryId(), holder.page());
            return;
        }
        plugin.shopManager().itemById(holder.categoryId(), holder.itemId()).ifPresent(item -> {
            if (slot == 13) {
                buy(player, item, holder.quantity());
                return;
            }
            int change = switch (slot) {
                case 10 -> -64;
                case 11 -> -32;
                case 12 -> -1;
                case 14 -> 1;
                case 15 -> 32;
                case 16 -> 64;
                default -> 0;
            };
            if (change != 0) {
                plugin.shopManager().openQuantity(player, holder.categoryId(), holder.page(), item, holder.quantity() + change);
            }
        });
    }

    @EventHandler
    void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder(false) instanceof ShopHolder) {
            event.setCancelled(true);
        }
    }

    private void buy(Player player, ShopItem item) {
        buy(player, item, item.amount());
    }

    private void buy(Player player, ShopItem item, int quantity) {
        if (!item.canBuy()) {
            send(player, "messages.buy-disabled", item, item.formattedBuyPrice(plugin, quantity));
            return;
        }
        if (!currencyReady(item)) {
            send(player, "messages.vault-missing", item, item.formattedBuyPrice(plugin, quantity));
            return;
        }
        if (!hasSpace(player.getInventory(), item, quantity)) {
            send(player, "messages.inventory-full", item, item.formattedBuyPrice(plugin, quantity));
            return;
        }
        double totalPrice = item.buyPrice() * quantity;
        if (!has(player, item, totalPrice) || !withdraw(player, item, totalPrice)) {
            send(player, "messages.not-enough-money", item, item.formattedBuyPrice(plugin, quantity));
            return;
        }
        if (item.commands().isEmpty()) {
            for (ItemStack product : products(item, quantity)) {
                player.getInventory().addItem(product);
            }
        } else {
            for (int count = 0; count < quantity; count++) {
                for (String configuredCommand : item.commands()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Text.replace(configuredCommand, item.replacements(plugin, player, quantity)));
                }
            }
        }
        send(player, "messages.bought", item, item.formattedBuyPrice(plugin, quantity), quantity);
    }

    private void sell(Player player, ShopItem item) {
        if (!item.canSell()) {
            send(player, "messages.sell-disabled", item, item.formattedSellPrice(plugin));
            return;
        }
        if (!currencyReady(item)) {
            send(player, "messages.vault-missing", item, item.formattedSellPrice(plugin));
            return;
        }
        ItemStack product = item.product();
        if (!player.getInventory().containsAtLeast(product, product.getAmount())) {
            send(player, "messages.not-enough-items", item, item.formattedSellPrice(plugin));
            return;
        }
        player.getInventory().removeItem(product);
        if (!deposit(player, item)) {
            player.getInventory().addItem(product);
            send(player, "messages.vault-missing", item, item.formattedSellPrice(plugin));
            return;
        }
        send(player, "messages.sold", item, item.formattedSellPrice(plugin));
    }

    private boolean hasSpace(Inventory inventory, ItemStack product) {
        int remaining = product.getAmount();
        for (ItemStack content : inventory.getStorageContents()) {
            if (content == null || content.getType() == Material.AIR) {
                return true;
            }
            if (content.isSimilar(product)) {
                remaining -= Math.max(0, content.getMaxStackSize() - content.getAmount());
                if (remaining <= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasSpace(Inventory inventory, ShopItem item, int quantity) {
        int remaining = quantity;
        ItemStack single = item.product(1);
        for (ItemStack content : inventory.getStorageContents()) {
            if (content == null || content.getType() == Material.AIR) {
                remaining -= single.getMaxStackSize();
            } else if (content.isSimilar(single)) {
                remaining -= Math.max(0, content.getMaxStackSize() - content.getAmount());
            }
            if (remaining <= 0) {
                return true;
            }
        }
        return false;
    }

    private java.util.List<ItemStack> products(ShopItem item, int quantity) {
        java.util.List<ItemStack> products = new java.util.ArrayList<>();
        int remaining = quantity;
        ItemStack sample = item.product(1);
        int stackSize = Math.max(1, sample.getMaxStackSize());
        while (remaining > 0) {
            int amount = Math.min(stackSize, remaining);
            products.add(item.product(amount));
            remaining -= amount;
        }
        return products;
    }

    private boolean currencyReady(ShopItem item) {
        return item.currency() == CurrencyType.POINTS ? plugin.playerPointsHook().isReady() : plugin.economyHook().isReady();
    }

    private boolean has(Player player, ShopItem item) {
        return has(player, item, item.buyPrice());
    }

    private boolean has(Player player, ShopItem item, double price) {
        return item.currency() == CurrencyType.POINTS ? plugin.playerPointsHook().has(player, price) : plugin.economyHook().has(player, price);
    }

    private boolean withdraw(Player player, ShopItem item) {
        return withdraw(player, item, item.buyPrice());
    }

    private boolean withdraw(Player player, ShopItem item, double price) {
        return item.currency() == CurrencyType.POINTS ? plugin.playerPointsHook().take(player, price) : plugin.economyHook().withdraw(player, price);
    }

    private boolean deposit(Player player, ShopItem item) {
        return item.currency() == CurrencyType.POINTS ? plugin.playerPointsHook().give(player, item.sellPrice()) : plugin.economyHook().deposit(player, item.sellPrice());
    }

    private void send(Player player, String path, ShopItem item, String price) {
        send(player, path, item, price, item.amount());
    }

    private void send(Player player, String path, ShopItem item, String price, int quantity) {
        Map<String, String> replacements = new HashMap<>(item.replacements(plugin, player));
        replacements.put("%amount%", String.valueOf(quantity));
        replacements.put("%prefix%", plugin.getConfig().getString("messages.prefix", ""));
        replacements.put("%price%", price);
        player.sendMessage(Text.component(plugin, player, Text.replace(plugin.getConfig().getString(path, ""), replacements)));
    }
}

