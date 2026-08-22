package de.otaku.auctionhouse.util;

import de.otaku.auctionhouse.OtakuAuctionHousePlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class LimitResolver {
    private LimitResolver() {
    }

    public static int maxListings(OtakuAuctionHousePlugin plugin, Player player) {
        int max = plugin.getConfig().getInt("limits.default-active-listings", 3);
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("limits.permission-limits");
        if (section == null) {
            return max;
        }

        for (String permission : section.getKeys(false)) {
            if (player.hasPermission(permission)) {
                max = Math.max(max, section.getInt(permission));
            }
        }
        return max;
    }
}
