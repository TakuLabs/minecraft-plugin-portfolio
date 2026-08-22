package de.otaku.empirecrates.keyall;

import de.otaku.empirecrates.EmpireCratesPlugin;
import de.otaku.empirecrates.model.CrateDefinition;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;

public class KeyallService {
    private final EmpireCratesPlugin plugin;
    private BukkitTask task;
    private long nextRunMillis;

    public KeyallService(EmpireCratesPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        resetCountdown();
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!isEnabled()) {
                resetCountdown();
                return;
            }
            if (System.currentTimeMillis() < nextRunMillis) {
                return;
            }
            runConfiguredKeyall();
            resetCountdown();
        }, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("keyall-timer.enabled", false);
    }

    public String configuredCrateId() {
        return plugin.getConfig().getString("keyall-timer.crate-id", "");
    }

    public int configuredAmount() {
        return Math.max(1, Math.min(64, plugin.getConfig().getInt("keyall-timer.amount", 1)));
    }

    public int intervalMinutes() {
        return Math.max(1, plugin.getConfig().getInt("keyall-timer.interval-minutes", 60));
    }

    public long remainingSeconds() {
        if (!isEnabled()) {
            return 0L;
        }
        return Math.max(0L, (nextRunMillis - System.currentTimeMillis() + 999L) / 1000L);
    }

    public String remainingFormatted() {
        if (!isEnabled()) {
            return "deaktiviert";
        }
        long seconds = remainingSeconds();
        long minutes = seconds / 60L;
        long restSeconds = seconds % 60L;
        return minutes + "m " + String.format("%02d", restSeconds) + "s";
    }

    public void setEnabled(boolean enabled) {
        plugin.getConfig().set("keyall-timer.enabled", enabled);
        plugin.saveConfig();
        resetCountdown();
    }

    public void setCrateId(String crateId) {
        plugin.getConfig().set("keyall-timer.crate-id", crateId == null ? "" : crateId.toLowerCase());
        plugin.saveConfig();
        resetCountdown();
    }

    public void setAmount(int amount) {
        plugin.getConfig().set("keyall-timer.amount", Math.max(1, Math.min(64, amount)));
        plugin.saveConfig();
    }

    public int runConfiguredKeyall() {
        CrateDefinition crate = plugin.crates().find(configuredCrateId()).orElse(null);
        if (crate == null) {
            return 0;
        }
        return giveKeyall(crate, configuredAmount(), true);
    }

    public int giveKeyall(CrateDefinition crate, int amount, boolean announce) {
        int given = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            var item = plugin.markedItems().markKey(crate);
            item.setAmount(Math.max(1, Math.min(64, amount)));
            target.getInventory().addItem(item).values().forEach(leftover -> target.getWorld().dropItemNaturally(target.getLocation(), leftover));
            target.sendMessage(plugin.msg("keyall-received", Map.of(
                    "crate", crate.getDisplayName(),
                    "amount", String.valueOf(item.getAmount())
            )));
            target.playSound(target.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.55F, 1.35F);
            given++;
        }
        if (announce) {
            Bukkit.broadcast(plugin.msg("keyall", Map.of("crate", crate.getDisplayName())));
        }
        return given;
    }

    public void resetCountdown() {
        nextRunMillis = System.currentTimeMillis() + intervalMinutes() * 60_000L;
    }
}
