package de.otaku.novaguard.storage;

import org.bukkit.plugin.java.JavaPlugin;

public final class SecurityStores {
    private final UuidStore whitelist;
    private final UuidStore uuidBlacklist;
    private final IpStore ipBlacklist;
    private final IpOverrideStore ipOverrides;
    private final ConnectionHistoryStore connectionHistory;

    public SecurityStores(JavaPlugin plugin) {
        this.whitelist = new UuidStore(plugin, "ip-whitelist.yml");
        this.uuidBlacklist = new UuidStore(plugin, "uuid-blacklist.yml");
        this.ipBlacklist = new IpStore(plugin, "ip-blacklist.yml");
        this.ipOverrides = new IpOverrideStore(plugin);
        this.connectionHistory = new ConnectionHistoryStore(plugin);
    }

    public void load() {
        whitelist.load();
        uuidBlacklist.load();
        ipBlacklist.load();
        ipOverrides.load();
        connectionHistory.load();
    }

    public void save() {
        whitelist.save();
        uuidBlacklist.save();
        ipBlacklist.save();
        ipOverrides.save();
        connectionHistory.save();
    }

    public UuidStore whitelist() { return whitelist; }
    public UuidStore uuidBlacklist() { return uuidBlacklist; }
    public IpStore ipBlacklist() { return ipBlacklist; }
    public IpOverrideStore ipOverrides() { return ipOverrides; }
    public ConnectionHistoryStore connectionHistory() { return connectionHistory; }
}
