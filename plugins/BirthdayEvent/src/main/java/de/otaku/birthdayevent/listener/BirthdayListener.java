package de.otaku.birthdayevent.listener;

import de.otaku.birthdayevent.BirthdayEvent;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class BirthdayListener implements Listener {
    private final BirthdayEvent plugin;
    private final Set<UUID> sessionInvited = new HashSet<>();

    public BirthdayListener(BirthdayEvent plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.isEventActive() || plugin.congratulationService().hasCongratulated(event.getPlayer())) {
            return;
        }
        if (sessionInvited.add(event.getPlayer().getUniqueId())) {
            plugin.sendBirthdayExperience(event.getPlayer(), false);
        }
    }

    @EventHandler
    public void onFireworkDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Firework firework
                && firework.getPersistentDataContainer().has(plugin.congratulationService().fireworkKey(), PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }
}
