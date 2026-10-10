# OtakuAuctionHouse

Player marketplace and item listings.

| Field | Value |
| --- | --- |
| Version | `1.1.5` |
| Build | Maven |
| Main class | `de.otaku.auctionhouse.OtakuAuctionHousePlugin` |
| Source files | 40 |
| Tests | 0 |
| Dependencies | Maven · paper-api, Vault |

## Technical Notes

- Category: Economy
- Focus: Player marketplace and item listings

## Code Snippets

### 1. src/main/java/de/otaku/auctionhouse/model/AuctionListing.java
```java
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
```

### 2. src/main/java/de/otaku/auctionhouse/storage/ListingStore.java
```java
    public ListingStore(OtakuAuctionHousePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "listings.yml");
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
