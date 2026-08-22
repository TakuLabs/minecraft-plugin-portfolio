package de.otaku.empirecrates.model;

import org.bukkit.Material;

import java.util.Arrays;

public enum RewardRarity {
    GEWOEHNLICH("Gew\u00f6hnlich", 55.0D, "#b7b7b7", "#ffffff", Material.IRON_NUGGET),
    UNGEWOEHNLICH("Ungew\u00f6hnlich", 25.0D, "#5cff87", "#d8ff6b", Material.EMERALD),
    SELTEN("Selten", 12.0D, "#45caff", "#2667ff", Material.LAPIS_LAZULI),
    EPISCH("Episch", 5.0D, "#d66bff", "#7b2cff", Material.AMETHYST_SHARD),
    LEGENDAER("Legend\u00e4r", 2.0D, "#ffb347", "#ff5f6d", Material.GOLD_INGOT),
    MYSTISCH("Mystisch", 0.8D, "#00f5ff", "#ff4ecd", Material.NETHER_STAR),
    JACKPOT("JACKPOT", 0.2D, "#fff176", "#ff1744", Material.TOTEM_OF_UNDYING);

    private final String displayName;
    private final double chancePercent;
    private final String firstHex;
    private final String secondHex;
    private final Material icon;

    RewardRarity(String displayName, double chancePercent, String firstHex, String secondHex, Material icon) {
        this.displayName = displayName;
        this.chancePercent = chancePercent;
        this.firstHex = firstHex;
        this.secondHex = secondHex;
        this.icon = icon;
    }

    public String displayName() {
        return displayName;
    }

    public double chancePercent() {
        return chancePercent;
    }

    public String miniMessage() {
        return "<bold><gradient:" + firstHex + ":" + secondHex + ">" + displayName + "</gradient></bold>";
    }

    public String legacyDisplay() {
        return net.md_5.bungee.api.ChatColor.of(firstHex) + displayName;
    }

    public Material icon() {
        return icon;
    }

    public RewardRarity next() {
        RewardRarity[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public boolean isBroadcastDefault() {
        return this == LEGENDAER || this == MYSTISCH || this == JACKPOT;
    }

    public static RewardRarity parse(String input) {
        if (input == null || input.isBlank()) {
            return GEWOEHNLICH;
        }
        String normalized = input.toUpperCase()
                .replace("\u00c4", "AE")
                .replace("\u00d6", "OE")
                .replace("\u00dc", "UE");
        return Arrays.stream(values())
                .filter(rarity -> rarity.name().equals(normalized) || rarity.displayName.toUpperCase().equals(normalized))
                .findFirst()
                .orElse(GEWOEHNLICH);
    }
}


