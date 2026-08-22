package de.otaku.scoreboard;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public final class ArmorStandNametagService {
    private final OtakuScoreboardPlugin plugin;
    private final TextRenderer textRenderer;
    private final Map<UUID, ArmorStand> nametags = new HashMap<>();

    private boolean enabled;
    private boolean placeholderApiAvailable;
    private String text;
    private boolean small;
    private boolean marker;

    public ArmorStandNametagService(OtakuScoreboardPlugin plugin, TextRenderer textRenderer) {
        this.plugin = plugin;
        this.textRenderer = textRenderer;
        reload();
    }

    public void reload() {
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean("scoreboard-teams.armorstand.enabled", false);
        text = config.getString("scoreboard-teams.armorstand.text", "%luckperms_prefix% %player%");
        small = config.getBoolean("scoreboard-teams.armorstand.small", true);
        marker = config.getBoolean("scoreboard-teams.armorstand.marker", true);
        placeholderApiAvailable = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");

        if (!enabled) {
            removeAll();
        }
        updateAll();
    }

    public void updateAll() {
        if (!enabled) {
            return;
        }

        Set<UUID> onlinePlayers = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            onlinePlayers.add(player.getUniqueId());
            update(player);
        }

        for (UUID uuid : Set.copyOf(nametags.keySet())) {
            if (!onlinePlayers.contains(uuid)) {
                remove(uuid);
            }
        }
    }

    public void update(Player player) {
        if (!enabled || !player.isOnline() || player.isDead()) {
            remove(player.getUniqueId());
            return;
        }

        ArmorStand armorStand = nametags.get(player.getUniqueId());
        if (armorStand == null || armorStand.isDead() || !armorStand.getWorld().equals(player.getWorld())) {
            remove(player.getUniqueId());
            armorStand = spawn(player);
            nametags.put(player.getUniqueId(), armorStand);
        }

        if (!player.getPassengers().contains(armorStand)) {
            player.addPassenger(armorStand);
        }
        armorStand.customName(textRenderer.render(player, text, placeholderApiAvailable));
        armorStand.setCustomNameVisible(true);
    }

    public void remove(UUID uuid) {
        ArmorStand armorStand = nametags.remove(uuid);
        if (armorStand != null && !armorStand.isDead()) {
            armorStand.remove();
        }
    }

    public void removeAll() {
        for (UUID uuid : Set.copyOf(nametags.keySet())) {
            remove(uuid);
        }
    }

    private ArmorStand spawn(Player player) {
        ArmorStand armorStand = (ArmorStand) player.getWorld().spawnEntity(player.getLocation(), EntityType.ARMOR_STAND);
        armorStand.setInvisible(true);
        armorStand.setInvulnerable(true);
        armorStand.setGravity(false);
        armorStand.setSmall(small);
        armorStand.setMarker(marker);
        armorStand.setBasePlate(false);
        armorStand.setArms(false);
        armorStand.setSilent(true);
        armorStand.setPersistent(false);
        armorStand.setRemoveWhenFarAway(false);
        armorStand.addScoreboardTag("otaku_armorstand_nametag");
        player.addPassenger(armorStand);
        return armorStand;
    }
}
