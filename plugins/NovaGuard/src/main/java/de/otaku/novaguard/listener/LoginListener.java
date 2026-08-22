package de.otaku.novaguard.listener;

import de.otaku.novaguard.config.MessageService;
import de.otaku.novaguard.command.KnownPlayerDirectory;
import de.otaku.novaguard.config.PluginSettings;
import de.otaku.novaguard.iplimit.IpLimitService;
import de.otaku.novaguard.iplimit.IpLimitAccessPolicy;
import de.otaku.novaguard.iplimit.ReservationResult;
import de.otaku.novaguard.packet.PacketRateLimiter;
import de.otaku.novaguard.packet.ProtocolPacketListener;
import de.otaku.novaguard.storage.SecurityStores;
import de.otaku.novaguard.util.IpNormalizer;
import de.otaku.novaguard.util.IpPrivacy;
import de.otaku.novaguard.util.PermissionUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class LoginListener implements Listener {
    private final JavaPlugin plugin;
    private final AtomicReference<PluginSettings> settings;
    private final MessageService messages;
    private final SecurityStores stores;
    private final IpLimitService ipLimitService;
    private final PacketRateLimiter packetRateLimiter;
    private final ProtocolPacketListener protocolPacketListener;
    private final KnownPlayerDirectory knownPlayerDirectory;

    public LoginListener(JavaPlugin plugin, AtomicReference<PluginSettings> settings, MessageService messages,
                         SecurityStores stores, IpLimitService ipLimitService, PacketRateLimiter packetRateLimiter,
                         ProtocolPacketListener protocolPacketListener, KnownPlayerDirectory knownPlayerDirectory) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.stores = stores;
        this.ipLimitService = ipLimitService;
        this.packetRateLimiter = packetRateLimiter;
        this.protocolPacketListener = protocolPacketListener;
        this.knownPlayerDirectory = knownPlayerDirectory;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        PluginSettings current = settings.get();
        UUID uuid = event.getUniqueId();
        knownPlayerDirectory.remember(uuid, event.getName());
        Optional<String> normalized = IpNormalizer.normalize(event.getAddress().getHostAddress());
        if (normalized.isEmpty()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, messages.prefixed("invalid-address"));
            return;
        }
        String ip = normalized.get();
        if (stores.uuidBlacklist().contains(uuid)) {
            String reason = stores.uuidBlacklist().find(uuid).map(entry -> entry.reason()).orElse(null);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    messages.kickScreen("uuid-blacklist", kickPlaceholders(event.getName(), uuid, reason)));
            plugin.getLogger().info("UUID blacklist denied " + event.getName() + " " + uuid);
            return;
        }
        if (stores.ipBlacklist().contains(ip)) {
            String reason = stores.ipBlacklist().find(ip).map(entry -> entry.reason()).orElse(null);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    messages.kickScreen("ip-blacklist", kickPlaceholders(event.getName(), uuid, reason)));
            plugin.getLogger().info("IP blacklist denied " + event.getName() + " at " + IpPrivacy.display(ip, current.logFullIps()));
            return;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(PlayerLoginEvent event) {
        PluginSettings current = settings.get();
        Player player = event.getPlayer();
        knownPlayerDirectory.remember(player.getUniqueId(), player.getName());
        Optional<String> normalized = IpNormalizer.normalize(event.getAddress().getHostAddress());
        if (normalized.isEmpty()) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, messages.prefixed("invalid-address"));
            return;
        }
        String ip = normalized.get();
        UUID uuid = player.getUniqueId();
        boolean whitelisted = stores.whitelist().contains(uuid);
        boolean permissionBypass = PermissionUtil.hasExactPermission(player, "novaguard.iplimit.bypass");
        Optional<Integer> override = stores.ipOverrides().get(ip);
        int limit = override.orElse(current.maxAccountsPerIp());
        var snapshot = ipLimitService.snapshot(ip);
        if (stores.uuidBlacklist().contains(uuid)) {
            String reason = stores.uuidBlacklist().find(uuid).map(entry -> entry.reason()).orElse(null);
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED,
                    messages.kickScreen("uuid-blacklist", kickPlaceholders(player.getName(), uuid, reason)));
            debugLogin(current, player.getName(), uuid, event.getAddress().getHostAddress(), ip, snapshot.active(),
                    snapshot.reserved(), limit, whitelisted, permissionBypass, override, "DENY_UUID_BLACKLIST");
            return;
        }
        if (stores.ipBlacklist().contains(ip)) {
            String reason = stores.ipBlacklist().find(ip).map(entry -> entry.reason()).orElse(null);
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED,
                    messages.kickScreen("ip-blacklist", kickPlaceholders(player.getName(), uuid, reason)));
            debugLogin(current, player.getName(), uuid, event.getAddress().getHostAddress(), ip, snapshot.active(),
                    snapshot.reserved(), limit, whitelisted, permissionBypass, override, "DENY_IP_BLACKLIST");
            return;
        }
        if (!current.ipLimitEnabled() || IpLimitAccessPolicy.bypassesIpLimit(whitelisted, permissionBypass)) {
            debugLogin(current, player.getName(), uuid, event.getAddress().getHostAddress(), ip, snapshot.active(),
                    snapshot.reserved(), limit, whitelisted, permissionBypass, override, "ALLOW_BYPASS_OR_DISABLED");
            return;
        }
        ReservationResult result = ipLimitService.reserve(uuid, ip, limit, current.reservationTimeoutMillis());
        if (!result.allowed()) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, messages.kickScreen("ip-limit", Map.of(
                    "player", player.getName(),
                    "uuid", uuid.toString(),
                    "max_accounts", String.valueOf(result.limit()),
                    "current_accounts", String.valueOf(result.active() + result.reserved())
            )));
            plugin.getLogger().info("IP limit denied " + player.getName() + " " + uuid
                    + " active=" + result.active() + " reserved=" + result.reserved()
                    + " limit=" + result.limit() + " ip=" + IpPrivacy.display(ip, current.logFullIps()));
        }
        debugLogin(current, player.getName(), uuid, event.getAddress().getHostAddress(), ip, result.active(),
                result.reserved(), result.limit(), whitelisted, permissionBypass, override, result.allowed() ? "ALLOW" : "DENY_IP_LIMIT");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLoginMonitor(PlayerLoginEvent event) {
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            ipLimitService.release(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ipLimitService.activate(player.getUniqueId());
        knownPlayerDirectory.remember(player.getUniqueId(), player.getName());
        if (player.getAddress() != null) {
            IpNormalizer.normalize(player.getAddress()).ifPresent(ip ->
                    stores.connectionHistory().record(ip, player.getUniqueId(), player.getName()));
        }
        protocolPacketListener.cachePlayer(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        ipLimitService.release(uuid);
        packetRateLimiter.remove(uuid);
        protocolPacketListener.removePlayer(uuid);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKick(PlayerKickEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        ipLimitService.release(uuid);
        packetRateLimiter.remove(uuid);
        protocolPacketListener.removePlayer(uuid);
    }

    public boolean hasIpBypass(Player player) {
        return PermissionUtil.hasExactPermission(player, "novaguard.iplimit.bypass") || stores.whitelist().contains(player.getUniqueId());
    }

    private Map<String, String> kickPlaceholders(String playerName, UUID uuid, String reason) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", playerName);
        placeholders.put("uuid", uuid.toString());
        if (reason != null && !reason.isBlank()) {
            placeholders.put("reason", reason);
        }
        return placeholders;
    }

    private void debugLogin(PluginSettings current, String name, UUID uuid, String rawAddress, String normalizedAddress,
                            int active, int reserved, int limit, boolean whitelisted, boolean permissionBypass,
                            Optional<Integer> override, String decision) {
        if (!current.debug()) {
            return;
        }
        plugin.getLogger().info("[NovaGuard DEBUG] Login attempt:");
        plugin.getLogger().info("[NovaGuard DEBUG] Player: " + name);
        plugin.getLogger().info("[NovaGuard DEBUG] UUID: " + uuid);
        plugin.getLogger().info("[NovaGuard DEBUG] Raw address: " + rawAddress);
        plugin.getLogger().info("[NovaGuard DEBUG] Normalized address: " + normalizedAddress);
        plugin.getLogger().info("[NovaGuard DEBUG] Active sessions: " + active);
        plugin.getLogger().info("[NovaGuard DEBUG] Reservations: " + reserved);
        plugin.getLogger().info("[NovaGuard DEBUG] Effective limit: " + limit);
        plugin.getLogger().info("[NovaGuard DEBUG] Whitelisted: " + whitelisted);
        plugin.getLogger().info("[NovaGuard DEBUG] Permission bypass: " + permissionBypass);
        plugin.getLogger().info("[NovaGuard DEBUG] IP override: " + override.map(String::valueOf).orElse("none"));
        plugin.getLogger().info("[NovaGuard DEBUG] Decision: " + decision);
    }
}
