package de.otaku.novaguard.storage;

import de.otaku.novaguard.util.ResourceUtil;
import de.otaku.novaguard.util.YamlFiles;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class ConnectionHistoryStore {
    private static final int MAX_QUEUED_WRITES = 1024;

    private final JavaPlugin plugin;
    private final ConcurrentMap<String, ConcurrentMap<UUID, HistoryEntry>> networks = new ConcurrentHashMap<>();
    private final ExecutorService storageExecutor;
    private final AtomicBoolean saveQueued = new AtomicBoolean();
    private final AtomicInteger queuedWrites = new AtomicInteger();
    private File file;

    public ConnectionHistoryStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.storageExecutor = Executors.newSingleThreadExecutor(new HistoryThreadFactory());
    }

    public synchronized void load() {
        file = ResourceUtil.ensureResource(plugin, "connection-history.yml");
        YamlConfiguration yaml;
        try {
            yaml = YamlFiles.load(file.toPath());
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not load connection-history.yml as UTF-8: " + ex.getMessage());
            yaml = new YamlConfiguration();
        }
        networks.clear();
        ConfigurationSection root = yaml.getConfigurationSection("networks");
        if (root == null) {
            return;
        }
        for (String network : root.getKeys(false)) {
            ConfigurationSection entries = root.getConfigurationSection(network + ".accounts");
            if (entries == null) {
                continue;
            }
            ConcurrentMap<UUID, HistoryEntry> map = new ConcurrentHashMap<>();
            for (String uuidText : entries.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidText);
                    ConfigurationSection item = entries.getConfigurationSection(uuidText);
                    String name = item == null ? null : item.getString("last-known-name");
                    Instant firstSeen = parseInstant(item == null ? null : item.getString("first-seen"));
                    Instant lastSeen = parseInstant(item == null ? null : item.getString("last-seen"));
                    map.put(uuid, new HistoryEntry(name, firstSeen, lastSeen));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Ignoring invalid UUID in connection-history.yml: " + uuidText);
                }
            }
            networks.put(network, map);
        }
    }

    public void save() {
        saveNow();
    }

    private synchronized void saveNow() {
        YamlConfiguration yaml = new YamlConfiguration();
        networks.forEach((network, accounts) -> accounts.forEach((uuid, entry) -> {
            String path = "networks." + network + ".accounts." + uuid;
            yaml.set(path + ".last-known-name", entry.lastKnownName());
            yaml.set(path + ".first-seen", entry.firstSeen().toString());
            yaml.set(path + ".last-seen", entry.lastSeen().toString());
        }));
        try {
            YamlFiles.save(yaml, file.toPath());
        } catch (IOException ex) {
            plugin.getLogger().severe("Could not save connection-history.yml: " + ex.getMessage());
        }
    }

    public void record(String networkId, UUID uuid, String playerName) {
        networks.computeIfAbsent(networkId, ignored -> new ConcurrentHashMap<>())
                .compute(uuid, (ignored, previous) -> {
                    Instant now = Instant.now();
                    if (previous == null) {
                        return new HistoryEntry(playerName, now, now);
                    }
                    return new HistoryEntry(playerName, previous.firstSeen(), now);
                });
        enqueueSave();
    }

    public Set<UUID> accountsForNetwork(String networkId) {
        ConcurrentMap<UUID, HistoryEntry> accounts = networks.get(networkId);
        return accounts == null ? Set.of() : new HashSet<>(accounts.keySet());
    }

    public List<HistoricalPlayer> knownPlayers() {
        ConcurrentMap<UUID, HistoricalPlayer> latest = new ConcurrentHashMap<>();
        networks.values().forEach(accounts -> accounts.forEach((uuid, entry) ->
                latest.compute(uuid, (ignored, previous) -> {
                    if (previous == null || entry.lastSeen().isAfter(previous.lastSeen())) {
                        return new HistoricalPlayer(uuid, entry.lastKnownName(), entry.lastSeen());
                    }
                    return previous;
                })));
        return new ArrayList<>(latest.values());
    }

    public void cleanupExpired(int retentionDays) {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        networks.forEach((network, accounts) -> accounts.entrySet().removeIf(entry -> entry.getValue().lastSeen().isBefore(cutoff)));
        networks.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        enqueueSave();
    }

    public int networkCount() {
        return networks.size();
    }

    public int historyEntryCount() {
        int total = 0;
        for (ConcurrentMap<UUID, HistoryEntry> accounts : networks.values()) {
            total += accounts.size();
        }
        return total;
    }

    public int queuedWrites() {
        return queuedWrites.get();
    }

    public void flushAndShutdown() {
        flush();
        storageExecutor.shutdown();
        try {
            if (!storageExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                storageExecutor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            storageExecutor.shutdownNow();
        }
    }

    public void flush() {
        if (saveQueued.get()) {
            saveNow();
        }
    }

    private void enqueueSave() {
        if (queuedWrites.incrementAndGet() > MAX_QUEUED_WRITES) {
            queuedWrites.decrementAndGet();
            plugin.getLogger().warning("[NovaGuard] Connection history storage queue is full; coalescing writes.");
        }
        if (!saveQueued.compareAndSet(false, true)) {
            return;
        }
        storageExecutor.execute(() -> {
            try {
                saveNow();
            } finally {
                queuedWrites.set(0);
                saveQueued.set(false);
            }
        });
    }

    private static Instant parseInstant(String value) {
        if (value == null) {
            return Instant.now();
        }
        try {
            return Instant.parse(value);
        } catch (RuntimeException ex) {
            return Instant.now();
        }
    }

    public record HistoryEntry(String lastKnownName, Instant firstSeen, Instant lastSeen) {
    }

    public record HistoricalPlayer(UUID uuid, String lastKnownName, Instant lastSeen) {
    }

    private static final class HistoryThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "NovaGuard-Storage");
            thread.setDaemon(true);
            return thread;
        }
    }
}
