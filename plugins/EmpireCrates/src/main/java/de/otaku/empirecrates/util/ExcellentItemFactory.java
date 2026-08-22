package de.otaku.empirecrates.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExcellentItemFactory {
    private static final Pattern ID_PATTERN = Pattern.compile("id:\\\"([^\\\"]+)\\\"");
    private static final Pattern COUNT_PATTERN = Pattern.compile("count:(\\d+)");
    private static final Pattern ITEMSADDER_PATTERN = Pattern.compile("itemsadder:\\{id:\\\"([^\\\"]+)\\\",namespace:\\\"([^\\\"]+)\\\"}");

    private ExcellentItemFactory() {
    }

    public static ItemStack fromExcellentTag(String tag, ItemStack fallback) {
        if (tag == null || tag.isBlank()) {
            return fallback == null ? null : fallback.clone();
        }
        int amount = amount(tag);
        String materialKey = materialKey(tag);
        Material material = material(materialKey);
        if (material == null) {
            ItemStack itemsAdder = itemsAdderItem(tag, amount);
            if (valid(itemsAdder)) {
                return itemsAdder;
            }
            return fallback == null ? null : withAmount(fallback, amount);
        }
        ItemStack base = new ItemStack(material, amount);
        ItemStack parsed = parseWithPaper(materialKey, tag);
        if (valid(parsed)) {
            parsed.setAmount(amount);
            return parsed;
        }
        ItemStack itemsAdder = itemsAdderItem(tag, amount);
        if (valid(itemsAdder)) {
            return itemsAdder;
        }
        return base;
    }

    public static String materialKey(String tag) {
        Matcher matcher = ID_PATTERN.matcher(tag == null ? "" : tag);
        if (!matcher.find()) {
            return "minecraft:paper";
        }
        String key = matcher.group(1);
        return key == null || key.isBlank() || key.equalsIgnoreCase("minecraft:") ? "minecraft:paper" : key;
    }

    public static int amount(String tag) {
        Matcher matcher = COUNT_PATTERN.matcher(tag == null ? "" : tag);
        if (!matcher.find()) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(matcher.group(1)));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    public static ItemStack placeholder(String tag) {
        ItemStack item = itemsAdderItem(tag, 1);
        if (valid(item)) {
            item.setAmount(1);
            return item;
        }
        Material material = material(materialKey(tag));
        return new ItemStack(material == null ? Material.PAPER : material);
    }

    private static ItemStack parseWithPaper(String materialKey, String tag) {
        String components = components(tag);
        if (components.isBlank()) {
            return null;
        }
        try {
            return Bukkit.getItemFactory().createItemStack(materialKey + "[" + components + "]");
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String components(String tag) {
        int start = tag.indexOf("components:{");
        if (start < 0) {
            return "";
        }
        int open = tag.indexOf('{', start);
        int close = matchingBrace(tag, open);
        if (open < 0 || close <= open) {
            return "";
        }
        return tag.substring(open + 1, close);
    }

    private static int matchingBrace(String text, int open) {
        int depth = 0;
        char quote = 0;
        for (int index = open; index < text.length(); index++) {
            char current = text.charAt(index);
            if (quote != 0) {
                if (current == quote && (index == 0 || text.charAt(index - 1) != '\\')) {
                    quote = 0;
                }
                continue;
            }
            if (current == '\'' || current == '"') {
                quote = current;
                continue;
            }
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        return -1;
    }

    private static ItemStack itemsAdderItem(String tag, int amount) {
        Matcher matcher = ITEMSADDER_PATTERN.matcher(tag == null ? "" : tag);
        if (!matcher.find()) {
            return null;
        }
        String itemId = matcher.group(2) + ":" + matcher.group(1);
        try {
            Class<?> customStackClass = Class.forName("dev.lone.itemsadder.api.CustomStack");
            Object customStack = customStackClass.getMethod("getInstance", String.class).invoke(null, itemId);
            if (customStack == null) {
                return null;
            }
            Object result = customStackClass.getMethod("getItemStack").invoke(customStack);
            if (result instanceof ItemStack stack) {
                stack.setAmount(Math.max(1, amount));
                return stack;
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
        return null;
    }

    private static Material material(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        Material material = Material.matchMaterial(key);
        if (material != null) {
            return material;
        }
        String cleaned = key.toLowerCase(Locale.ROOT).startsWith("minecraft:") ? key.substring("minecraft:".length()) : key;
        return Material.matchMaterial(cleaned);
    }

    private static boolean valid(ItemStack item) {
        return item != null && item.getType() != Material.AIR;
    }

    private static ItemStack withAmount(ItemStack item, int amount) {
        ItemStack clone = item.clone();
        clone.setAmount(Math.max(1, amount));
        return clone;
    }
}
