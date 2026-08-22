package de.takulabs.smpshop;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

record ShopItem(String id, Material material, CurrencyType currency, int amount, double buyPrice, double sellPrice, String name, List<String> lore, List<String> commands, Map<Enchantment, Integer> enchantments, int fireworkPower, PotionType potionType) {
    static Optional<ShopItem> fromConfig(String id, ConfigurationSection section) {
        return fromConfig(id, section, CurrencyType.MONEY);
    }

    static Optional<ShopItem> fromConfig(String id, ConfigurationSection section, CurrencyType defaultCurrency) {
        Material material = Material.matchMaterial(section.getString("material", "STONE"));
        if (material == null) {
            return Optional.empty();
        }
        int amount = Math.max(1, Math.min(64, section.getInt("amount", 1)));
        return Optional.of(new ShopItem(
                id,
                material,
                CurrencyType.from(section.getString("currency", defaultCurrency.name().toLowerCase(Locale.ROOT))),
                amount,
                section.getDouble("buy-price", -1.0D),
                section.getDouble("sell-price", -1.0D),
                section.getString("name", "&f" + prettify(id)),
                section.getStringList("lore"),
                section.getStringList("commands"),
                enchantments(section),
                section.getInt("firework-power", 0),
                potionType(section.getString("potion-type", ""))
        ));
    }

    ItemStack display(SMPShopPlugin plugin, Player player) {
        ItemStack itemStack = product();
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            Map<String, String> replacements = replacements(plugin, player);
            meta.displayName(Text.component(plugin, player, Text.replace(name, replacements)));
            List<String> lines = lore.isEmpty() ? plugin.getConfig().getStringList("shop-item-lore") : lore;
            meta.lore(Text.components(plugin, player, lines, replacements));
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    ItemStack quantityDisplay(SMPShopPlugin plugin, Player player, int quantity) {
        ItemStack itemStack = product(Math.max(1, Math.min(product().getMaxStackSize(), quantity)));
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            Map<String, String> replacements = replacements(plugin, player, quantity);
            meta.displayName(Text.component(plugin, player, Text.replace(name, replacements)));
            meta.lore(Text.components(plugin, player, plugin.getConfig().getStringList("quantity-item-lore"), replacements));
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }

    ItemStack product() {
        return product(amount);
    }

    ItemStack product(int quantity) {
        ItemStack itemStack = new ItemStack(material);
        itemStack.setAmount(Math.max(1, Math.min(itemStack.getMaxStackSize(), quantity)));
        if (itemStack.getItemMeta() instanceof FireworkMeta fireworkMeta && fireworkPower > 0) {
            fireworkMeta.setPower(Math.max(1, Math.min(3, fireworkPower)));
            itemStack.setItemMeta(fireworkMeta);
        }
        if (itemStack.getItemMeta() instanceof PotionMeta potionMeta && potionType != null) {
            potionMeta.setBasePotionType(potionType);
            itemStack.setItemMeta(potionMeta);
        }
        if (itemStack.getItemMeta() instanceof EnchantmentStorageMeta storageMeta) {
            enchantments.forEach((enchantment, level) -> storageMeta.addStoredEnchant(enchantment, Math.max(1, level), true));
            itemStack.setItemMeta(storageMeta);
        } else {
            enchantments.forEach((enchantment, level) -> itemStack.addUnsafeEnchantment(enchantment, Math.max(1, level)));
        }
        return itemStack;
    }

    boolean canBuy() {
        return buyPrice >= 0.0D;
    }

    boolean canSell() {
        return sellPrice >= 0.0D && commands.isEmpty();
    }

    String itemName() {
        return name == null || name.isBlank() ? prettify(id) : name.replace("&0", "").replace("&1", "").replace("&2", "").replace("&3", "").replace("&4", "").replace("&5", "").replace("&6", "").replace("&7", "").replace("&8", "").replace("&9", "").replace("&a", "").replace("&b", "").replace("&c", "").replace("&d", "").replace("&e", "").replace("&f", "").replace("&l", "").replace("&n", "").replace("&o", "").replace("&r", "");
    }

    Map<String, String> replacements(SMPShopPlugin plugin, Player player) {
        return replacements(plugin, player, amount);
    }

    Map<String, String> replacements(SMPShopPlugin plugin, Player player, int quantity) {
        return Map.of(
                "%player%", player.getName(),
                "%item%", itemName(),
                "%amount%", String.valueOf(quantity),
                "%buy_price%", format(plugin, buyPrice * quantity),
                "%sell_price%", sellPrice < 0.0D ? "-" : format(plugin, sellPrice * quantity),
                "%currency%", currencyName(plugin)
        );
    }

    String formattedBuyPrice(SMPShopPlugin plugin) {
        return format(plugin, buyPrice);
    }

    String formattedBuyPrice(SMPShopPlugin plugin, int quantity) {
        return format(plugin, buyPrice * quantity);
    }

    String formattedSellPrice(SMPShopPlugin plugin) {
        return format(plugin, sellPrice);
    }

    private String format(SMPShopPlugin plugin, double price) {
        if (price < 0.0D) {
            return "-";
        }
        if (currency == CurrencyType.POINTS) {
            String pattern = plugin.getConfig().getString("settings.points-format", "%,.0f Punkte");
            try {
                return String.format(Locale.GERMANY, pattern, price);
            } catch (IllegalArgumentException exception) {
                return Math.round(price) + " Punkte";
            }
        }
        String pattern = plugin.getConfig().getString("settings.money-format", plugin.getConfig().getString("settings.currency-format", "$%,.2f"));
        try {
            return String.format(Locale.US, pattern, price);
        } catch (IllegalArgumentException exception) {
            return NumberFormat.getCurrencyInstance(Locale.US).format(price);
        }
    }

    private String currencyName(SMPShopPlugin plugin) {
        return currency == CurrencyType.POINTS
                ? plugin.getConfig().getString("settings.points-name", "Punkte")
                : plugin.getConfig().getString("settings.money-name", "Coins");
    }

    static String prettify(String id) {
        String[] parts = id.toLowerCase(Locale.ROOT).replace('-', '_').split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }

    private static Map<Enchantment, Integer> enchantments(ConfigurationSection section) {
        Map<Enchantment, Integer> enchantments = new LinkedHashMap<>();
        ConfigurationSection enchantmentSection = section.getConfigurationSection("enchantments");
        if (enchantmentSection == null) {
            return enchantments;
        }
        for (String key : enchantmentSection.getKeys(false)) {
            Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(key.toLowerCase(Locale.ROOT)));
            if (enchantment == null) {
                enchantment = Enchantment.getByName(key.toUpperCase(Locale.ROOT));
            }
            if (enchantment != null) {
                enchantments.put(enchantment, enchantmentSection.getInt(key, 1));
            }
        }
        return enchantments;
    }

    private static PotionType potionType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return PotionType.valueOf(value.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}

