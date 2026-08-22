package de.otaku.empirecrates.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class CrateReward {
    private final String id;
    private String displayName;
    private RewardType type;
    private ItemStack item;
    private String command;
    private String excellentItemTag;
    private RewardRarity rarity;
    private double chancePercent;
    private boolean broadcast;

    public CrateReward(String id, String displayName, RewardType type) {
        this.id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        this.displayName = displayName;
        this.type = type;
        this.rarity = RewardRarity.GEWOEHNLICH;
        this.chancePercent = this.rarity.chancePercent();
        this.broadcast = false;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public RewardType getType() {
        return type;
    }

    public void setType(RewardType type) {
        this.type = type;
    }

    public ItemStack getItem() {
        return item == null ? null : item.clone();
    }

    public void setItem(ItemStack item) {
        this.item = item == null ? null : item.clone();
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getExcellentItemTag() {
        return excellentItemTag == null ? "" : excellentItemTag;
    }

    public void setExcellentItemTag(String excellentItemTag) {
        this.excellentItemTag = excellentItemTag == null ? "" : excellentItemTag;
    }

    public RewardRarity getRarity() {
        return rarity;
    }

    public void setRarity(RewardRarity rarity) {
        this.rarity = rarity == null ? RewardRarity.GEWOEHNLICH : rarity;
        this.chancePercent = this.rarity.chancePercent();
    }

    public double getChancePercent() {
        return chancePercent > 0.0D ? chancePercent : rarity.chancePercent();
    }

    public void setChancePercent(double chancePercent) {
        if (Double.isNaN(chancePercent) || Double.isInfinite(chancePercent) || chancePercent <= 0.0D) {
            this.chancePercent = rarity.chancePercent();
            return;
        }
        this.chancePercent = Math.max(0.0001D, Math.min(100.0D, chancePercent));
    }

    public boolean isBroadcast() {
        return broadcast;
    }

    public void setBroadcast(boolean broadcast) {
        this.broadcast = broadcast;
    }
}


