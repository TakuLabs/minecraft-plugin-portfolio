package de.otaku.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class OtakuScoreboardPlugin extends JavaPlugin {
    private ScoreboardService scoreboardService;
    private TablistService tablistService;
    private ArmorStandNametagService armorStandNametagService;
    private BukkitTask updateTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        TextRenderer textRenderer = new TextRenderer(this);
        scoreboardService = new ScoreboardService(this, textRenderer);
        tablistService = new TablistService(this, textRenderer);
        armorStandNametagService = new ArmorStandNametagService(this, textRenderer);

        PluginCommand command = getCommand("otakuscoreboard");
        if (command != null) {
            OtakuScoreboardCommand executor = new OtakuScoreboardCommand(this, scoreboardService);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
        PluginCommand rainbowCommand = getCommand("rainbow");
        if (rainbowCommand != null) {
            rainbowCommand.setExecutor(new RainbowCommand(this, tablistService));
        }

        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(scoreboardService, armorStandNametagService), this);
        startTask();
    }

    @Override
    public void onDisable() {
        stopTask();
        if (scoreboardService != null) {
            scoreboardService.clearAll();
        }
        if (armorStandNametagService != null) {
            armorStandNametagService.removeAll();
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        scoreboardService.reload();
        tablistService.reload();
        armorStandNametagService.reload();
        startTask();
    }

    private void startTask() {
        stopTask();
        if (!getConfig().getBoolean("enabled", true)) {
            scoreboardService.clearAll();
            return;
        }

        long interval = Math.max(1L, getConfig().getLong("update-interval", 40L));
        updateTask = Bukkit.getScheduler().runTaskTimer(this, this::updateAll, 1L, interval);
    }

    private void stopTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    private void updateAll() {
        scoreboardService.updateAll();
        tablistService.updateAll();
        armorStandNametagService.updateAll();
    }
}
