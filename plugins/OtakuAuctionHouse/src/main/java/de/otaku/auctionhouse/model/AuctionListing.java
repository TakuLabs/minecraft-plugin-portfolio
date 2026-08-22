package de.otaku.auctionhouse.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public record AuctionListing(
        UUID id,
        UUID sellerId,
        String sellerName,
        double price,
        long createdAt,
        long expiresAt,
        ItemStack item
) {
}
