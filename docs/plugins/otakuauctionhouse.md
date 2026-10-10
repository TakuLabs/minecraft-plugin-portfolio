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
- Public page uses selected excerpts only; full private source is not copied.

## Code Snippets

### 1. src/main/java/de/otaku/auctionhouse/model/AuctionListing.java

Representative source excerpt.

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

Persistence or state handling from the plugin source.

```java
    public ListingStore(OtakuAuctionHousePlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "listings.yml");
    }
```

[← Back to Plugin Catalog](../plugin-catalog.md)
