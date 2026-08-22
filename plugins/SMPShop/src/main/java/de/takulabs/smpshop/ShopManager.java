package de.takulabs.smpshop;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class ShopManager {
    private final SMPShopPlugin plugin;
    private final Map<String, ShopCategory> categories = new LinkedHashMap<>();
    private final ItemStack filler;

    ShopManager(SMPShopPlugin plugin) {
        this.plugin = plugin;
        this.filler = createFiller();
        loadCategories();
    }

    void openMain(Player player) {
        int size = normalizeSize(plugin.getConfig().getInt("settings.main-size", 27));
        Inventory inventory = Bukkit.createInventory(new ShopHolder(MenuType.MAIN, "", 0), size, Text.component(plugin, player, plugin.getConfig().getString("settings.main-title", "NOVA SHOP")));
        fill(inventory);
        for (ShopCategory category : categories.values()) {
            if (category.slot() >= 0 && category.slot() < inventory.getSize()) {
                inventory.setItem(category.slot(), categoryIcon(category, player));
            }
        }
        setConfiguredButtons(inventory, "main-buttons", player);
        player.openInventory(inventory);
    }

    void openCategory(Player player, String categoryId, int page) {
        ShopCategory category = categories.get(categoryId);
        if (category == null) {
            openMain(player);
            return;
        }
        int size = normalizeSize(category.size());
        int contentSlots = Math.max(9, size - 9);
        int maxPage = Math.max(0, (category.items().size() - 1) / contentSlots);
        int safePage = Math.max(0, Math.min(page, maxPage));
        String title = plugin.getConfig().getString("settings.category-title-prefix", "NOVA SHOP - ") + category.title();
        Inventory inventory = Bukkit.createInventory(new ShopHolder(MenuType.CATEGORY, categoryId, safePage), size, Text.component(plugin, player, title));
        fill(inventory);
        int offset = safePage * contentSlots;
        for (int index = offset; index < Math.min(offset + contentSlots, category.items().size()); index++) {
            inventory.setItem(index - offset, category.items().get(index).display(plugin, player));
        }
        setButton(inventory, "buttons.back", player, size - 9);
        setButton(inventory, "buttons.close", player, size - 1);
        if (safePage > 0) {
            setButton(inventory, "buttons.previous", player, size - 6);
        }
        if (safePage < maxPage) {
            setButton(inventory, "buttons.next", player, size - 4);
        }
        player.openInventory(inventory);
    }

    Optional<ShopCategory> categoryBySlot(int slot) {
        return categories.values().stream().filter(category -> category.slot() == slot).findFirst();
    }

    Optional<ShopItem> itemBySlot(String categoryId, int page, int slot) {
        ShopCategory category = categories.get(categoryId);
        if (category == null) {
            return Optional.empty();
        }
        int contentSlots = Math.max(9, normalizeSize(category.size()) - 9);
        int index = page * contentSlots + slot;
        if (index < 0 || index >= category.items().size()) {
            return Optional.empty();
        }
        return Optional.of(category.items().get(index));
    }

    Optional<ShopItem> itemById(String categoryId, String itemId) {
        ShopCategory category = categories.get(categoryId);
        if (category == null) {
            return Optional.empty();
        }
        return category.items().stream().filter(item -> item.id().equalsIgnoreCase(itemId)).findFirst();
    }

    void openQuantity(Player player, String categoryId, int page, ShopItem item, int quantity) {
        int safeQuantity = Math.max(1, Math.min(plugin.getConfig().getInt("settings.max-buy-quantity", 576), quantity));
        Inventory inventory = Bukkit.createInventory(new ShopHolder(MenuType.QUANTITY, categoryId, page, item.id(), safeQuantity), 27, Text.component(plugin, player, plugin.getConfig().getString("settings.quantity-title", "&8Menge auswählen")));
        fill(inventory);
        inventory.setItem(10, quantityButton(player, Material.RED_STAINED_GLASS_PANE, "&c-64", -64));
        inventory.setItem(11, quantityButton(player, Material.RED_STAINED_GLASS_PANE, "&c-32", -32));
        inventory.setItem(12, quantityButton(player, Material.RED_STAINED_GLASS_PANE, "&c-1", -1));
        inventory.setItem(13, item.quantityDisplay(plugin, player, safeQuantity));
        inventory.setItem(14, quantityButton(player, Material.LIME_STAINED_GLASS_PANE, "&a+1", 1));
        inventory.setItem(15, quantityButton(player, Material.LIME_STAINED_GLASS_PANE, "&a+32", 32));
        inventory.setItem(16, quantityButton(player, Material.LIME_STAINED_GLASS_PANE, "&a+64", 64));
        inventory.setItem(18, quantityButton(player, Material.ARROW, "&eZurück", 0));
        player.openInventory(inventory);
    }

    private void loadCategories() {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("categories");
        if (root == null) {
            return;
        }
        for (String categoryId : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(categoryId);
            if (section == null) {
                continue;
            }
            Material material = Material.matchMaterial(section.getString("material", "CHEST"));
            if (material == null) {
                material = Material.CHEST;
            }
            CurrencyType categoryCurrency = CurrencyType.from(section.getString("currency", "money"));
            List<ShopItem> items = new ArrayList<>();
            ConfigurationSection itemSection = section.getConfigurationSection("items");
            if (itemSection != null) {
                for (String itemId : itemSection.getKeys(false)) {
                    ConfigurationSection configuredItem = itemSection.getConfigurationSection(itemId);
                    if (configuredItem != null) {
                        ShopItem.fromConfig(itemId, configuredItem, categoryCurrency).ifPresent(items::add);
                    }
                }
            }
            categories.put(categoryId.toLowerCase(), new ShopCategory(
                    categoryId.toLowerCase(),
                    section.getInt("slot", 0),
                    normalizeSize(section.getInt("size", plugin.getConfig().getInt("settings.category-size", 27))),
                    material,
                    categoryCurrency,
                    section.getString("name", "&f" + ShopItem.prettify(categoryId)),
                    section.getString("description", ""),
                    section.getString("title", ShopItem.prettify(categoryId)),
                    items
            ));
        }
    }

    private ItemStack categoryIcon(ShopCategory category, Player player) {
        ItemStack itemStack = new ItemStack(category.material());
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            Map<String, String> replacements = Map.of(
                    "%category%", category.title(),
                    "%description%", category.description(),
                    "%items%", String.valueOf(category.items().size())
            );
            meta.displayName(Text.component(plugin, player, category.name()));
            meta.lore(Text.components(plugin, player, plugin.getConfig().getStringList("category-item-lore"), replacements));
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    private ItemStack quantityButton(Player player, Material material, String name, int change) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.component(plugin, player, name));
            if (change != 0) {
                meta.lore(Text.components(plugin, player, List.of("&7Menge ändern um &f" + change), Map.of()));
            }
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    private void setButton(Inventory inventory, String path, Player player) {
        setButton(inventory, path, player, plugin.getConfig().getInt(path + ".slot", -1));
    }

    private void setButton(Inventory inventory, String path, Player player, int slot) {
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        Material material = Material.matchMaterial(plugin.getConfig().getString(path + ".material", "BARRIER"));
        if (material == null) {
            material = Material.BARRIER;
        }
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.component(plugin, player, plugin.getConfig().getString(path + ".name", "")));
            meta.lore(Text.components(plugin, player, plugin.getConfig().getStringList(path + ".lore"), Map.of()));
            button.setItemMeta(meta);
        }
        inventory.setItem(slot, button);
    }

    private void setConfiguredButtons(Inventory inventory, String path, Player player) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            setButton(inventory, path + "." + id, player);
        }
    }

    private void fill(Inventory inventory) {
        if (filler == null) {
            return;
        }
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private ItemStack createFiller() {
        if (!plugin.getConfig().getBoolean("settings.filler.enabled", true)) {
            return null;
        }
        Material material = Material.matchMaterial(plugin.getConfig().getString("settings.filler.material", "BLACK_STAINED_GLASS_PANE"));
        if (material == null) {
            material = Material.BLACK_STAINED_GLASS_PANE;
        }
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    private int normalizeSize(int configuredSize) {
        int clamped = Math.max(9, Math.min(54, configuredSize));
        return ((clamped + 8) / 9) * 9;
    }
}

