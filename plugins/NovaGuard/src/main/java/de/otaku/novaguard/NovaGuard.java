package de.otaku.novaguard;

import de.otaku.novaguard.command.NovaGuardCommand;
import de.otaku.novaguard.command.KnownPlayerDirectory;
import de.otaku.novaguard.config.MessageService;
import de.otaku.novaguard.config.PluginSettings;
import de.otaku.novaguard.iplimit.IpLimitService;
import de.otaku.novaguard.listener.LoginListener;
import de.otaku.novaguard.packet.PacketRateLimiter;
import de.otaku.novaguard.packet.ProtocolPacketListener;
import de.otaku.novaguard.storage.SecurityStores;
import de.otaku.novaguard.iplimit.LimitReevaluationResult;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import de.otaku.novaguard.util.IpNormalizer;
import de.otaku.novaguard.util.IpPrivacy;
import de.otaku.novaguard.util.PermissionUtil;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public final class NovaGuard extends JavaPlugin {
    private final AtomicReference<PluginSettings> settings = new AtomicReference<>();
    private MessageService messages;
    private SecurityStores stores;
    private IpLimitService ipLimitService;
    private PacketRateLimiter packetRateLimiter;
    private KnownPlayerDirectory knownPlayerDirectory;
    private ProtocolPacketListener protocolPacketListener;
    private BukkitTask reservationCleanupTask;
    private BukkitTask historyCleanupTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        messages = new MessageService(this);
        messages.load();
        settings.set(PluginSettings.load(this));
        stores = new SecurityStores(this);
        stores.load();
        knownPlayerDirectory = new KnownPlayerDirectory();
        knownPlayerDirectory.rebuild(stores);
        ipLimitService = new IpLimitService();
        packetRateLimiter = new PacketRateLimiter(settings.get());
        protocolPacketListener = new ProtocolPacketListener(this, settings, messages, packetRateLimiter);
        getServer().getPluginManager().registerEvents(
                new LoginListener(this, settings, messages, stores, ipLimitService, packetRateLimiter, protocolPacketListener, knownPlayerDirectory),
                this
        );
        protocolPacketListener.register();
        rebuildOnlinePlayerState();
        registerCommand();
        startReservationCleanup();
        startHistoryCleanup();
        getLogger().info("NovaGuard enabled. Packet protection mode: " + settings.get().packetMode());
    }

    @Override
    public void onDisable() {
        if (protocolPacketListener != null) {
            protocolPacketListener.unregister();
        }
        HandlerList.unregisterAll(this);
        if (reservationCleanupTask != null) {
            reservationCleanupTask.cancel();
        }
        if (historyCleanupTask != null) {
            historyCleanupTask.cancel();
        }
        if (stores != null) {
            stores.save();
            stores.connectionHistory().flushAndShutdown();
        }
        if (ipLimitService != null) {
            ipLimitService.clear();
        }
        if (packetRateLimiter != null) {
            packetRateLimiter.clear();
        }
    }

    public void reloadNovaGuard() {
        PluginSettings previous = settings.get();
        reloadConfig();
        messages.load();
        stores.connectionHistory().flush();
        stores.load();
        knownPlayerDirectory.rebuild(stores);
        PluginSettings loaded = PluginSettings.load(this);
        settings.set(loaded);
        packetRateLimiter.updateSettings(loaded);
        rebuildOnlinePlayerState();
        reevaluateActiveIpLimits(previous, loaded);
        startReservationCleanup();
        startHistoryCleanup();
        if (previous != null && previous.maxAccountsPerIp() != loaded.maxAccountsPerIp()) {
            getLogger().info("[NovaGuard] IP limit changed: " + previous.maxAccountsPerIp() + " -> " + loaded.maxAccountsPerIp());
        }
        if (loaded.debug()) {
            getLogger().info("[NovaGuard] Reload complete");
            getLogger().info("[NovaGuard] IP limit enabled: " + loaded.ipLimitEnabled());
            getLogger().info("[NovaGuard] Max accounts per IP: " + loaded.maxAccountsPerIp());
            getLogger().info("[NovaGuard] Packet protection mode: " + loaded.packetMode());
        }
    }

    private void registerCommand() {
        PluginCommand command = getCommand("novaguard");
        if (command == null) {
            getLogger().severe("Command novaguard is missing from plugin.yml");
            return;
        }
        NovaGuardCommand executor = new NovaGuardCommand(this, messages, stores, knownPlayerDirectory);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void startReservationCleanup() {
        if (reservationCleanupTask != null) {
            reservationCleanupTask.cancel();
        }
        reservationCleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                () -> ipLimitService.cleanupExpired(settings.get().reservationTimeoutMillis()),
                20L * 10L,
                20L * 10L);
    }

    private void startHistoryCleanup() {
        if (historyCleanupTask != null) {
            historyCleanupTask.cancel();
        }
        long sixHours = 20L * 60L * 60L * 6L;
        historyCleanupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                () -> stores.connectionHistory().cleanupExpired(settings.get().historyRetentionDays()),
                sixHours,
                sixHours);
    }

    private void rebuildOnlinePlayerState() {
        Map<UUID, String> activeSessions = new HashMap<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            InetSocketAddress address = player.getAddress();
            if (address == null) {
                continue;
            }
            IpNormalizer.normalize(address).ifPresent(ip -> {
                stores.connectionHistory().record(ip, player.getUniqueId(), player.getName());
                knownPlayerDirectory.remember(player.getUniqueId(), player.getName());
                boolean exempt = PermissionUtil.hasExactPermission(player, "novaguard.iplimit.bypass")
                        || stores.whitelist().contains(player.getUniqueId());
                if (!exempt) {
                    activeSessions.put(player.getUniqueId(), ip);
                }
            });
            protocolPacketListener.cachePlayer(player);
        }
        ipLimitService.rebuildActiveSessions(activeSessions);
    }

    private void reevaluateActiveIpLimits(PluginSettings previous, PluginSettings loaded) {
        if (!loaded.ipLimitEnabled()) {
            return;
        }
        getLogger().info("[NovaGuard] Re-evaluating active IP groups...");
        LimitReevaluationResult result = ipLimitService.reevaluateLimits(ip -> stores.ipOverrides().get(ip).orElse(loaded.maxAccountsPerIp()));
        for (UUID uuid : result.disconnects()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                int limit = result.limitsByUuid().getOrDefault(uuid, loaded.maxAccountsPerIp());
                player.kick(messages.kickScreen("ip-limit-reduced", Map.of(
                        "player", player.getName(),
                        "uuid", uuid.toString(),
                        "max_accounts", String.valueOf(limit),
                        "current_accounts", String.valueOf(limit)
                )));
            }
        }
        getLogger().info("[NovaGuard] Re-evaluation complete.");
        getLogger().info("[NovaGuard] Affected groups: " + result.affectedGroups());
        getLogger().info("[NovaGuard] Disconnected players: " + result.disconnects().size());
        if (loaded.debug() && !result.disconnects().isEmpty()) {
            getLogger().info("[NovaGuard DEBUG] Re-evaluation disconnected UUIDs: " + result.disconnects());
        }
    }

    public PluginSettings settings() {
        return settings.get();
    }

    public IpLimitService ipLimitService() {
        return ipLimitService;
    }

    public PacketRateLimiter packetRateLimiter() {
        return packetRateLimiter;
    }

    public SecurityStores stores() {
        return stores;
    }

    public KnownPlayerDirectory knownPlayerDirectory() {
        return knownPlayerDirectory;
    }

    public String ipFor(UUID uuid) {
        return ipLimitService.ipFor(uuid);
    }
}
