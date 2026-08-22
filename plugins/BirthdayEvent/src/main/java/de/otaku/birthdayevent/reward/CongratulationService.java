package de.otaku.birthdayevent.reward;

import de.otaku.birthdayevent.config.PluginSettings;
import de.otaku.birthdayevent.event.BirthdayEventWindow;
import de.otaku.birthdayevent.message.MessageService;
import de.otaku.birthdayevent.storage.CongratulationStore;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map;

public final class CongratulationService {
    private final JavaPlugin plugin;
    private final CongratulationStore store;
    private final CongratulationRegistry registry;
    private final MessageService messages;
    private final NamespacedKey fireworkKey;
    private PluginSettings settings;
    private RewardSelector selector;
    private BirthdayEventWindow window;

    public CongratulationService(JavaPlugin plugin, CongratulationStore store, MessageService messages, PluginSettings settings) {
        this.plugin = plugin;
        this.store = store;
        this.messages = messages;
        this.registry = new CongratulationRegistry(store.loadRecords());
        this.fireworkKey = new NamespacedKey(plugin, "decorative_birthday_firework");
        updateSettings(settings);
    }

    public void updateSettings(PluginSettings settings) {
        this.settings = settings;
        this.selector = new RewardSelector(settings.crates());
        this.window = new BirthdayEventWindow(settings.event());
    }

    public void handle(Player player) {
        ZonedDateTime now = window.now();
        if (now.isBefore(window.start())) {
            messages.send(player, "event-not-started");
            return;
        }
        if (!window.isActive(now)) {
            messages.send(player, "event-ended");
            return;
        }

        RewardSelection selection = selector.select();
        CongratulationClaim claim = registry.tryBegin(player.getUniqueId(), player.getName(), selection, Instant.now());
        if (!claim.accepted()) {
            if (claim.record() != null && claim.record().status() == CongratulationStatus.PENDING) {
                plugin.getLogger().warning("Pending reward remains locked for " + player.getName()
                        + " -> " + claim.record().amount() + "x " + claim.record().crate()
                        + ". No new reward was generated.");
                messages.send(player, "reward-pending");
            } else {
                messages.send(player, "already-congratulated");
            }
            return;
        }

        store.saveRecord(claim.record());
        String command;
        try {
            command = RewardCommandFormatter.format(settings.rewardCommand(), player.getName(), player.getUniqueId(), selection);
        } catch (IllegalArgumentException exception) {
            registry.rollbackPending(player.getUniqueId());
            store.remove(player.getUniqueId());
            plugin.getLogger().warning("Reward-Command konnte nicht gebaut werden: " + exception.getMessage());
            messages.send(player, "reward-failed");
            return;
        }

        boolean dispatched = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        if (!dispatched) {
            registry.rollbackPending(player.getUniqueId());
            store.remove(player.getUniqueId());
            plugin.getLogger().warning("Reward-Command wurde nicht verarbeitet: " + command);
            messages.send(player, "reward-failed");
            return;
        }

        CongratulationRecord completed = registry.complete(player.getUniqueId());
        store.saveRecord(completed);
        Bukkit.broadcast(messages.component(
                "public-congratulation",
                Map.of("player", player.getName())
        ));
        messages.send(player, "reward", MessageService.placeholders(
                player.getName(),
                player.getUniqueId().toString(),
                selection.crate(),
                selection.amount()
        ));
        spawnFirework(player);
        if (settings.rewardLogging()) {
            plugin.getLogger().info("Reward granted: " + player.getName() + " -> " + selection.amount() + "x " + selection.crate());
        }
    }

    public void handleTestClick(Player player) {
        messages.send(player, "test-click");
    }

    public boolean hasCongratulated(Player player) {
        return registry.hasCongratulated(player.getUniqueId());
    }

    public int count() {
        return registry.size();
    }

    public void saveAll() {
        store.saveAll(registry.snapshot());
    }

    public NamespacedKey fireworkKey() {
        return fireworkKey;
    }

    private void spawnFirework(Player player) {
        if (!settings.fireworkEnabled()) {
            return;
        }
        Firework firework = (Firework) player.getWorld().spawnEntity(player.getLocation(), EntityType.FIREWORK_ROCKET);
        firework.getPersistentDataContainer().set(fireworkKey, PersistentDataType.BYTE, (byte) 1);
        firework.setShotAtAngle(false);

        FireworkMeta meta = firework.getFireworkMeta();
        meta.setPower(0);
        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL)
                .withColor(Color.PURPLE, Color.FUCHSIA)
                .withFade(Color.WHITE)
                .trail(false)
                .flicker(true)
                .build());
        firework.setFireworkMeta(meta);
        Bukkit.getScheduler().runTaskLater(plugin, firework::detonate, 2L);
    }
}
