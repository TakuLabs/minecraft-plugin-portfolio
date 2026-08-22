package de.otaku.empirecrates;

import de.otaku.empirecrates.command.CrateCommand;
import de.otaku.empirecrates.gui.GuiManager;
import de.otaku.empirecrates.keyall.KeyallService;
import de.otaku.empirecrates.listener.CrateListener;
import de.otaku.empirecrates.storage.CrateStorage;
import de.otaku.empirecrates.storage.PlacedCrateStorage;
import de.otaku.empirecrates.storage.PlayerProgressStorage;
import de.otaku.empirecrates.util.MarkedItems;
import de.otaku.empirecrates.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;

public class EmpireCratesPlugin extends JavaPlugin {
    private CrateStorage crateStorage;
    private PlacedCrateStorage placedCrateStorage;
    private PlayerProgressStorage progressStorage;
    private MarkedItems markedItems;
    private GuiManager guiManager;
    private CrateListener crateListener;
    private KeyallService keyallService;
    private Object placeholderExpansion;
    private BukkitTask visualRestoreTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        installBundledCratesIfEmpty();
        this.markedItems = new MarkedItems(this);
        this.crateStorage = new CrateStorage(this);
        this.crateStorage.load();
        this.placedCrateStorage = new PlacedCrateStorage(this);
        this.placedCrateStorage.load();
        this.progressStorage = new PlayerProgressStorage(this);
        this.progressStorage.load();
        this.keyallService = new KeyallService(this);
        this.guiManager = new GuiManager(this);
        this.crateListener = new CrateListener(this);
        getServer().getPluginManager().registerEvents(guiManager, this);
        getServer().getPluginManager().registerEvents(crateListener, this);
        getCommand("empirecrates").setExecutor(new CrateCommand(this));
        getCommand("empirecrates").setTabCompleter(new CrateCommand(this));
        getCommand("keyall").setExecutor(new CrateCommand(this));
        getCommand("keyall").setTabCompleter(new CrateCommand(this));
        if (getServer().getPluginManager().isPluginEnabled("ItemsAdder")) {
            getLogger().info("ItemsAdder detected. Custom Items are preserved through ItemStack drag and drop.");
        }
        if (getServer().getPluginManager().isPluginEnabled("ModelEngine")) {
            getLogger().info("ModelEngine detected. Crates can use optional ModelEngine model IDs.");
        }
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            registerPlaceholders();
        }
        this.keyallService.start();
        scheduleVisualRestoreRetries();
    }

    public void scheduleVisualRestoreRetries() {
        if (crateListener == null) {
            return;
        }
        if (visualRestoreTask != null && !visualRestoreTask.isCancelled()) {
            return;
        }
        visualRestoreTask = new BukkitRunnable() {
            private int restoreAttempts;

            @Override
            public void run() {
                crateListener.restoreLoadedVisuals();
                restoreAttempts++;
                if (crateListener.loadedModelCratesReady() || restoreAttempts >= 90) {
                    cancel();
                    visualRestoreTask = null;
                }
            }
        }.runTaskTimer(this, 20L, 40L);
    }

    @Override
    public void onDisable() {
        if (crateListener != null) {
            crateListener.removeAllVisuals();
        }
        if (visualRestoreTask != null) {
            visualRestoreTask.cancel();
            visualRestoreTask = null;
        }
        if (keyallService != null) {
            keyallService.stop();
        }
        if (placeholderExpansion != null) {
            unregisterPlaceholders();
        }
        if (crateStorage != null) {
            crateStorage.save();
        }
        if (placedCrateStorage != null) {
            placedCrateStorage.save();
        }
    }

    public void reloadAll() {
        reloadConfig();
        if (crateListener != null) {
            crateListener.removeAllVisuals();
        }
        crateStorage.load();
        placedCrateStorage.load();
        progressStorage.load();
        keyallService.start();
        scheduleVisualRestoreRetries();
    }

    public Component msg(String path) {
        return msg(path, Map.of());
    }

    public Component msg(String path, Map<String, String> placeholders) {
        String message = getConfig().getString("theme.prefix", "") + getConfig().getString("messages." + path, path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return Text.chat(message);
    }

    public CrateStorage crates() {
        return crateStorage;
    }

    public PlacedCrateStorage placedCrates() {
        return placedCrateStorage;
    }

    public PlayerProgressStorage progress() {
        return progressStorage;
    }

    public MarkedItems markedItems() {
        return markedItems;
    }

    public GuiManager gui() {
        return guiManager;
    }

    public CrateListener crateListener() {
        return crateListener;
    }

    public KeyallService keyall() {
        return keyallService;
    }

    private void registerPlaceholders() {
        try {
            Class<?> expansionClass = Class.forName("de.otaku.empirecrates.placeholder.EmpireCratesPlaceholderExpansion");
            this.placeholderExpansion = expansionClass.getConstructor(EmpireCratesPlugin.class).newInstance(this);
            expansionClass.getMethod("register").invoke(this.placeholderExpansion);
            getLogger().info("PlaceholderAPI detected. Registered %empirecrates_keyall_time% placeholders.");
        } catch (ReflectiveOperationException | RuntimeException exception) {
            getLogger().warning("Could not register PlaceholderAPI expansion: " + exception.getMessage());
        }
    }

    private void unregisterPlaceholders() {
        try {
            placeholderExpansion.getClass().getMethod("unregister").invoke(placeholderExpansion);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        placeholderExpansion = null;
    }

    private void installBundledCratesIfEmpty() {
        File cratesFile = new File(getDataFolder(), "crates.yml");
        if (cratesFile.isFile() && cratesFile.length() > 16L) {
            return;
        }
        if (getResource("crates.yml") == null) {
            return;
        }
        if (cratesFile.isFile() && !cratesFile.delete()) {
            getLogger().warning("Could not replace empty crates.yml with bundled converted crates.");
            return;
        }
        saveResource("crates.yml", false);
        getLogger().info("Installed bundled crate data into crates.yml.");
    }
}


