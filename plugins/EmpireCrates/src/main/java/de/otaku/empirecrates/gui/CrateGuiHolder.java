package de.otaku.empirecrates.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class CrateGuiHolder implements InventoryHolder {
    private final GuiType type;
    private final String crateId;
    private final String rewardId;
    private final String action;
    private final int page;

    public CrateGuiHolder(GuiType type, String crateId, String rewardId) {
        this(type, crateId, rewardId, null);
    }

    public CrateGuiHolder(GuiType type, String crateId, String rewardId, String action) {
        this(type, crateId, rewardId, action, 0);
    }

    public CrateGuiHolder(GuiType type, String crateId, String rewardId, String action, int page) {
        this.type = type;
        this.crateId = crateId;
        this.rewardId = rewardId;
        this.action = action;
        this.page = Math.max(0, page);
    }

    public GuiType type() {
        return type;
    }

    public String crateId() {
        return crateId;
    }

    public String rewardId() {
        return rewardId;
    }

    public String action() {
        return action;
    }

    public int page() {
        return page;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}


