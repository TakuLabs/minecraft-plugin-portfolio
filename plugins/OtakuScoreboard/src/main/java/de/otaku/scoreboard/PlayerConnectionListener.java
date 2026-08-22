package de.otaku.scoreboard;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerConnectionListener implements Listener {
    private final ScoreboardService scoreboardService;
    private final ArmorStandNametagService armorStandNametagService;

    public PlayerConnectionListener(ScoreboardService scoreboardService, ArmorStandNametagService armorStandNametagService) {
        this.scoreboardService = scoreboardService;
        this.armorStandNametagService = armorStandNametagService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scoreboardService.update(event.getPlayer());
        armorStandNametagService.update(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        armorStandNametagService.remove(event.getPlayer().getUniqueId());
        scoreboardService.updateAll();
    }
}
