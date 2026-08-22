package de.otaku.headsystem.trade;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public final class TradeSession {
    private final UUID leftPlayer;
    private final UUID rightPlayer;
    private final Inventory inventory;
    private boolean leftReady;
    private boolean rightReady;
    private boolean completing;

    public TradeSession(Player leftPlayer, Player rightPlayer, Inventory inventory) {
        this.leftPlayer = leftPlayer.getUniqueId();
        this.rightPlayer = rightPlayer.getUniqueId();
        this.inventory = inventory;
    }

    public UUID leftPlayer() {
        return leftPlayer;
    }

    public UUID rightPlayer() {
        return rightPlayer;
    }

    public Inventory inventory() {
        return inventory;
    }

    public boolean isParticipant(UUID uuid) {
        return leftPlayer.equals(uuid) || rightPlayer.equals(uuid);
    }

    public boolean isLeft(UUID uuid) {
        return leftPlayer.equals(uuid);
    }

    public UUID other(UUID uuid) {
        return leftPlayer.equals(uuid) ? rightPlayer : leftPlayer;
    }

    public boolean isReady(UUID uuid) {
        return isLeft(uuid) ? leftReady : rightReady;
    }

    public void toggleReady(UUID uuid) {
        if (isLeft(uuid)) {
            leftReady = !leftReady;
        } else {
            rightReady = !rightReady;
        }
    }

    public void clearReady() {
        leftReady = false;
        rightReady = false;
    }

    public boolean bothReady() {
        return leftReady && rightReady;
    }

    public boolean completing() {
        return completing;
    }

    public void setCompleting(boolean completing) {
        this.completing = completing;
    }
}
