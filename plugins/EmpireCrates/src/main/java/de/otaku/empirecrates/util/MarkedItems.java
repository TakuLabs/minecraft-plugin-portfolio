package de.otaku.empirecrates.util;

import de.otaku.empirecrates.EmpireCratesPlugin;
import de.otaku.empirecrates.model.CrateDefinition;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class MarkedItems {
    private final NamespacedKey crateItemKey;
    private final NamespacedKey keyItemKey;

    public MarkedItems(EmpireCratesPlugin plugin) {
        this.crateItemKey = new NamespacedKey(plugin, "crate_id");
        this.keyItemKey = new NamespacedKey(plugin, "key_id");
    }

    public ItemStack markCrate(CrateDefinition crate) {
        ItemStack item = crate.getCrateItem();
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(crateItemKey, PersistentDataType.STRING, crate.getId());
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack markKey(CrateDefinition crate) {
        ItemStack item = crate.getKeyItem();
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(keyItemKey, PersistentDataType.STRING, crate.getKeyId());
        item.setItemMeta(meta);
        return item;
    }

    public String crateId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(crateItemKey, PersistentDataType.STRING);
    }

    public String keyId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(keyItemKey, PersistentDataType.STRING);
    }
}


