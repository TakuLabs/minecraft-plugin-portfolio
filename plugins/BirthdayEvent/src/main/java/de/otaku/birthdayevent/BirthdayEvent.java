package de.otaku.birthdayevent;

import de.otaku.birthdayevent.command.BirthdayEventCommand;
import de.otaku.birthdayevent.config.PluginSettings;
import de.otaku.birthdayevent.config.SettingsLoader;
import de.otaku.birthdayevent.event.BirthdayEventWindow;
import de.otaku.birthdayevent.event.EventSchedulerMode;
import de.otaku.birthdayevent.listener.BirthdayListener;
import de.otaku.birthdayevent.message.MessageService;
import de.otaku.birthdayevent.reward.CongratulationService;
import de.otaku.birthdayevent.storage.CongratulationStore;
import de.otaku.birthdayevent.storage.EventStateStore;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.ZonedDateTime;

public final class BirthdayEvent extends JavaPlugin {
    private PluginSettings settings;
    private BirthdayEventWindow eventWindow;
    private MessageService messages;
    private EventStateStore eventStateStore;
    private CongratulationStore congratulationStore;
    private CongratulationService congratulationService;
    private BukkitTask eventStartTask;
    private BukkitTask safetyTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("messages.yml");
        messages = new MessageService(this);
        eventStateStore = new EventStateStore(this);
        congratulationStore = new CongratulationStore(this);
        reloadRuntimeSettings();
        congratulationService = new CongratulationService(this, congratulationStore, messages, settings);

        BirthdayEventCommand command = new BirthdayEventCommand(this);
        PluginCommand pluginCommand = getCommand("birthdayevent");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        Bukkit.getPluginManager().registerEvents(new BirthdayListener(this), this);
        scheduleEventStart();
        checkEventStart();
        getLogger().info("BirthdayEvent 1.0.0 aktiviert. Event-Zeitzone: " + settings.event().zone());
    }

    @Override
    public void onDisable() {
        cancelTasks();
        if (congratulationService != null) {
            congratulationService.saveAll();
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        messages.reload();
        reloadRuntimeSettings();
        congratulationService.updateSettings(settings);
        cancelTasks();
        scheduleEventStart();
        checkEventStart();
    }

    private void reloadRuntimeSettings() {
        settings = SettingsLoader.load(getConfig(), warning -> getLogger().warning("[Config] " + warning));
        eventWindow = new BirthdayEventWindow(settings.event());
    }

    private void scheduleEventStart() {
        ZonedDateTime now = eventWindow.now();
        EventSchedulerMode mode = EventSchedulerMode.forState(eventWindow, eventStateStore.isEventTriggered(), now);
        if (mode == EventSchedulerMode.NONE) {
            return;
        }
        if (mode == EventSchedulerMode.DELAYED_START) {
            Duration untilStart = eventWindow.untilStart(now);
            long delayTicks = Math.max(1L, untilStart.toMillis() / 50L);
            eventStartTask = Bukkit.getScheduler().runTaskLater(this, this::checkEventStart, delayTicks);
            scheduleSafetyCheck();
        } else if (mode == EventSchedulerMode.SAFETY_ONLY) {
            scheduleSafetyCheck();
        }
    }

    private void scheduleSafetyCheck() {
        safetyTask = Bukkit.getScheduler().runTaskTimer(this, this::checkEventStart, 20L * 60L, 20L * 60L);
    }

    private void cancelTasks() {
        if (eventStartTask != null) {
            eventStartTask.cancel();
            eventStartTask = null;
        }
        if (safetyTask != null) {
            safetyTask.cancel();
            safetyTask = null;
        }
    }

    public void checkEventStart() {
        if (eventStateStore.isEventTriggered()) {
            return;
        }
        ZonedDateTime now = eventWindow.now();
        if (eventWindow.hasStarted(now) && now.isBefore(eventWindow.end())) {
            eventStateStore.markEventTriggered(now, eventWindow.start());
            for (Player player : Bukkit.getOnlinePlayers()) {
                sendBirthdayExperience(player, false);
            }
            getLogger().info("Einmaliger Geburtstagsevent-Start wurde ausgelöst.");
        }
    }

    public void sendBirthdayExperience(Player player, boolean testMode) {
        if (settings.titleEnabled()) {
            Title title = Title.title(
                    messages.component("title"),
                    messages.component("subtitle"),
                    Title.Times.times(
                            Duration.ofMillis(settings.titleFadeInTicks() * 50L),
                            Duration.ofMillis(settings.titleStayTicks() * 50L),
                            Duration.ofMillis(settings.titleFadeOutTicks() * 50L)
                    )
            );
            player.showTitle(title);
        }
        if (testMode) {
            messages.sendTestInvitation(player, settings);
        } else {
            messages.sendInvitation(player, settings);
        }
    }

    public boolean isEventActive() {
        return eventWindow.isActive(eventWindow.now());
    }

    public PluginSettings settings() {
        return settings;
    }

    public BirthdayEventWindow eventWindow() {
        return eventWindow;
    }

    public MessageService messages() {
        return messages;
    }

    public CongratulationService congratulationService() {
        return congratulationService;
    }

    public EventStateStore eventStateStore() {
        return eventStateStore;
    }

    private void saveResourceIfMissing(String resource) {
        if (!new java.io.File(getDataFolder(), resource).exists()) {
            saveResource(resource, false);
        }
    }
}
