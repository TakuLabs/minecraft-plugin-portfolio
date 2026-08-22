package de.takulabs.smpshop;

import org.bukkit.Material;

import java.util.List;

record ShopCategory(String id, int slot, int size, Material material, CurrencyType currency, String name, String description, String title, List<ShopItem> items) {
}

