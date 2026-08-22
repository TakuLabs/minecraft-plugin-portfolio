package de.otaku.novaguard.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.otaku.novaguard.config.MessageService;
import de.otaku.novaguard.config.PluginSettings;
import de.otaku.novaguard.util.PermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Map;

public final class ProtocolPacketListener {
    private static final long VIOLATION_LOG_INTERVAL_NANOS = 5_000_000_000L;

    private final JavaPlugin plugin;
    private final AtomicReference<PluginSettings> settings;
    private final MessageService messages;
    private final PacketRateLimiter limiter;
    private final ConcurrentMap<UUID, PlayerCache> playerCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Long> lastViolationLog = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, AtomicInteger> suppressedViolationLogs = new ConcurrentHashMap<>();
    private PacketAdapter adapter;

    public ProtocolPacketListener(JavaPlugin plugin, AtomicReference<PluginSettings> settings,
                                  MessageService messages, PacketRateLimiter limiter) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.limiter = limiter;
    }

    public void register() {
        if (adapter != null) {
            return;
        }
        List<PacketType> clientTypes = PacketTypeRegistry.supportedInboundPlayTypes(plugin, settings.get());
        if (clientTypes.isEmpty()) {
            plugin.getLogger().warning("[NovaGuard] Packet protection registered no supported inbound packet types on this server.");
        }
        adapter = new PacketAdapter(plugin, ListenerPriority.NORMAL, clientTypes) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                handle(event);
            }
        };
        ProtocolLibrary.getProtocolManager().addPacketListener(adapter);
    }

    public void unregister() {
        if (adapter == null) {
            return;
        }
        ProtocolLibrary.getProtocolManager().removePacketListener(adapter);
        adapter = null;
        playerCache.clear();
        lastViolationLog.clear();
        suppressedViolationLogs.clear();
    }

    public void cachePlayer(Player player) {
        playerCache.put(player.getUniqueId(), new PlayerCache(player.getName(), PermissionUtil.hasExactPermission(player, "novaguard.packet.bypass")));
    }

    public void removePlayer(UUID uuid) {
        playerCache.remove(uuid);
        lastViolationLog.remove(uuid);
        suppressedViolationLogs.remove(uuid);
    }

    private void handle(PacketEvent event) {
        PluginSettings current = settings.get();
        if (!current.packetProtectionEnabled()) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        PlayerCache cached = playerCache.get(uuid);
        if (cached != null && cached.packetBypass()) {
            return;
        }
        PacketCategory category = PacketClassifier.category(event.getPacketType());
        if (category == PacketCategory.CUSTOM_PAYLOAD && !current.customPayloadEnabled()) {
            return;
        }
        double cost = PacketClassifier.weight(category, current.packetWeights());
        long now = System.nanoTime();
        int violationStatus = limiter.recordStatus(uuid, category, cost, now);
        if (violationStatus == 0) {
            return;
        }
        if (shouldLog(uuid, now)) {
            String name = cached == null ? "unknown" : cached.name();
            int suppressed = suppressedViolationLogs.computeIfAbsent(uuid, ignored -> new AtomicInteger()).getAndSet(0);
            plugin.getLogger().warning("Packet rate violation player=" + name
                    + " uuid=" + uuid + " type=" + event.getPacketType().name()
                    + " violations=" + Math.abs(violationStatus) + " mode=" + current.packetMode()
                    + (suppressed > 0 ? " suppressed=" + suppressed : ""));
        }
        if (violationStatus < 0) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player online = Bukkit.getPlayer(uuid);
                if (online != null && online.isOnline()) {
                    online.kick(messages.kickScreen("packet-protection", Map.of(
                            "player", online.getName(),
                            "uuid", uuid.toString(),
                            "reason", "Ungewöhnlicher Netzwerkverkehr"
                    )));
                }
            });
        }
    }

    private boolean shouldLog(UUID uuid, long nowNanos) {
        Long last = lastViolationLog.get(uuid);
        if (last != null && nowNanos - last < VIOLATION_LOG_INTERVAL_NANOS) {
            suppressedViolationLogs.computeIfAbsent(uuid, ignored -> new AtomicInteger()).incrementAndGet();
            return false;
        }
        lastViolationLog.put(uuid, nowNanos);
        return true;
    }

    private record PlayerCache(String name, boolean packetBypass) {
    }
}
