package de.otaku.empirecrates.listener;

import de.otaku.empirecrates.EmpireCratesPlugin;
import de.otaku.empirecrates.model.CrateAnimation;
import de.otaku.empirecrates.model.CrateAura;
import de.otaku.empirecrates.model.CrateDefinition;
import de.otaku.empirecrates.model.CrateReward;
import de.otaku.empirecrates.model.RewardRarity;
import de.otaku.empirecrates.model.RewardType;
import de.otaku.empirecrates.util.ExcellentItemFactory;
import de.otaku.empirecrates.util.ItemBuilder;
import de.otaku.empirecrates.util.ModelEngineBridge;
import de.otaku.empirecrates.util.ModelEngineBridge.ModelHandle;
import de.otaku.empirecrates.util.RewardChanceCalculator;
import de.otaku.empirecrates.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.RayTraceResult;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class CrateListener implements Listener {
    private static final double OPEN_ANIMATION_BASE_Y = 0.85D;
    private static final double OPEN_ANIMATION_NAME_Y = 0.45D;

    private final EmpireCratesPlugin plugin;
    private final NamespacedKey placedCrateKey;
    private final NamespacedKey modelCarrierCrateKey;
    private final Map<String, Map<UUID, List<ArmorStand>>> holograms = new HashMap<>();
    private final Map<String, UUID> modelEntities = new HashMap<>();
    private final Map<String, ModelHandle> modelHandles = new HashMap<>();
    private final Map<String, String> modelEntityIds = new HashMap<>();
    private final Map<String, Long> modelOpenCooldowns = new HashMap<>();
    private final List<String> openingCrates = new ArrayList<>();
    private final ModelEngineBridge modelEngine;

    public CrateListener(EmpireCratesPlugin plugin) {
        this.plugin = plugin;
        this.placedCrateKey = new NamespacedKey(plugin, "placed_crate");
        this.modelCarrierCrateKey = new NamespacedKey(plugin, "model_carrier_crate");
        this.modelEngine = new ModelEngineBridge(plugin);
    }

    public boolean removeLookedAtCrate(Player player) {
        Entity entity = targetEntity(player);
        if (entity != null && crateId(entity) != null) {
            removeEntityCrate(entity);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 0.6F, 1.6F);
            player.sendMessage(Text.chat("<green>Entity-Crate wurde entfernt.</green>"));
            return true;
        }
        Block block = player.getTargetBlockExact(8);
        if (block == null || crateId(block) == null) {
            player.sendMessage(Text.chat("<red>Du schaust auf keine EmpireCrates-Crate.</red>"));
            return false;
        }
        removeHologram(block);
        plugin.placedCrates().remove(block);
        block.setType(Material.AIR);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 0.6F, 1.6F);
        player.sendMessage(Text.chat("<green>Crate wurde entfernt.</green>"));
        return true;
    }

    public int removePlacedCratesNear(Location center, double radius) {
        double radiusSquared = radius * radius;
        List<String> removedKeys = new ArrayList<>();
        plugin.placedCrates().all().forEach((key, crateId) -> plugin.placedCrates().location(key).ifPresent(location -> {
            if (!location.getWorld().equals(center.getWorld())) {
                return;
            }
            if (location.distanceSquared(center) > radiusSquared) {
                return;
            }
            removePlacedCrateBlock(location.getBlock());
            removedKeys.add(key);
        }));
        plugin.placedCrates().removeKeys(removedKeys);
        return removedKeys.size();
    }

    public int removeAllPlacedCrates() {
        List<String> removedKeys = new ArrayList<>();
        plugin.placedCrates().all().forEach((key, crateId) -> {
            plugin.placedCrates().location(key).ifPresent(location -> removePlacedCrateBlock(location.getBlock()));
            removedKeys.add(key);
        });
        plugin.placedCrates().removeKeys(removedKeys);
        return removedKeys.size();
    }

    private void removePlacedCrateBlock(Block block) {
        removeHologram(block);
        if (block.getType() != Material.AIR) {
            block.setType(Material.AIR, false);
        }
    }

    public boolean setLookedAtCrate(Player player, CrateDefinition crate) {
        Entity entity = targetEntity(player);
        if (entity != null && !crate.getModelEngineModelId().isBlank()) {
            setEntityCrate(player, entity, crate);
            return true;
        }
        Block block = player.getTargetBlockExact(8);
        if (block == null || block.getType() == Material.AIR) {
            player.sendMessage(Text.chat(crate.getModelEngineModelId().isBlank() ? "<red>Du schaust auf keinen Block.</red>" : "<red>Du schaust auf keinen Block oder nutzbares Entity.</red>"));
            return false;
        }
        if (block.getState() instanceof TileState state) {
            state.getPersistentDataContainer().set(placedCrateKey, PersistentDataType.STRING, crate.getId());
            state.update(true);
        }
        plugin.placedCrates().set(block, crate.getId(), player.getLocation().getYaw());
        spawnOrUpdateHologram(block, crate);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7F, 1.4F);
        player.sendMessage(Text.chat("<green>Block wurde als Crate gesetzt: </green>" + crate.getDisplayName()));
        return true;
    }

    private void setEntityCrate(Player player, Entity entity, CrateDefinition crate) {
        entity.getPersistentDataContainer().set(placedCrateKey, PersistentDataType.STRING, crate.getId());
        entity.setRotation(player.getLocation().getYaw(), entity.getLocation().getPitch());
        if (entity instanceof ArmorStand armorStand) {
            armorStand.setInvisible(true);
            armorStand.setGravity(false);
            armorStand.setInvulnerable(true);
        }
        removeHologram(entityKey(entity));
        spawnOrUpdateEntityModel(entity, crate);
        spawnOrUpdateEntityHologram(entity, crate);
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7F, 1.4F);
        player.sendMessage(Text.chat("<green>Entity wurde als ModelEngine-Crate gesetzt: </green>" + crate.getDisplayName()));
    }

    public void refreshLookedAtCrate(Player player) {
        Entity entity = targetEntity(player);
        if (entity != null) {
            String entityCrateId = crateId(entity);
            if (entityCrateId != null) {
                plugin.crates().find(entityCrateId).ifPresent(crate -> spawnOrUpdateEntityHologram(entity, crate));
                return;
            }
        }
        Block block = player.getTargetBlockExact(8);
        if (block == null) {
            return;
        }
        String crateId = crateId(block);
        if (crateId == null) {
            return;
        }
        plugin.crates().find(crateId).ifPresent(crate -> spawnOrUpdateHologram(block, crate));
    }

    public void refreshCrateVisuals(String crateId) {
        if (crateId == null || crateId.isBlank()) {
            return;
        }
        plugin.placedCrates().all().forEach((key, storedCrateId) -> {
            if (!crateId.equalsIgnoreCase(storedCrateId)) {
                return;
            }
            plugin.placedCrates().location(key).ifPresent(location -> {
                if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                    return;
                }
                plugin.crates().find(storedCrateId).ifPresent(crate -> {
                    Block block = location.getBlock();
                    removeModel(block);
                    spawnOrUpdateHologram(block, crate);
                });
            });
        });
        Bukkit.getWorlds().forEach(world -> {
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                for (Entity entity : chunk.getEntities()) {
                    String entityCrateId = crateId(entity);
                    if (entityCrateId == null || !crateId.equalsIgnoreCase(entityCrateId)) {
                        continue;
                    }
                    plugin.crates().find(entityCrateId).ifPresent(crate -> {
                        destroyModelHandle(entityKey(entity));
                        modelEntities.remove(entityKey(entity));
                        modelEntityIds.remove(entityKey(entity));
                        spawnOrUpdateEntityHologram(entity, crate);
                    });
                }
            }
        });
    }

    public void restoreLoadedVisuals() {
        plugin.placedCrates().all().forEach((key, crateId) -> plugin.placedCrates().location(key).ifPresent(location -> {
            if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                return;
            }
            plugin.crates().find(crateId).ifPresent(crate -> {
                Block block = location.getBlock();
                spawnOrUpdateHologram(block, crate);
            });
        }));
        Bukkit.getWorlds().forEach(world -> {
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                for (Entity entity : chunk.getEntities()) {
                    String crateId = crateId(entity);
                    if (crateId != null) {
                        plugin.crates().find(crateId).ifPresent(crate -> spawnOrUpdateEntityHologram(entity, crate));
                    }
                }
            }
        });
    }

    public boolean loadedModelCratesReady() {
        for (Map.Entry<String, String> entry : plugin.placedCrates().all().entrySet()) {
            Optional<Location> location = plugin.placedCrates().location(entry.getKey());
            if (location.isEmpty()
                    || !location.get().getWorld().isChunkLoaded(location.get().getBlockX() >> 4, location.get().getBlockZ() >> 4)) {
                continue;
            }
            Optional<CrateDefinition> crate = plugin.crates().find(entry.getValue());
            if (crate.isPresent()
                    && !crate.get().getModelEngineModelId().isBlank()
                    && !modelHandles.containsKey(blockKey(location.get().getBlock()))) {
                return false;
            }
        }
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
                for (Entity entity : chunk.getEntities()) {
                    String crateId = crateId(entity);
                    if (crateId == null) {
                        continue;
                    }
                    Optional<CrateDefinition> crate = plugin.crates().find(crateId);
                    if (crate.isPresent()
                            && !crate.get().getModelEngineModelId().isBlank()
                            && !modelHandles.containsKey(entityKey(entity))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        String crateId = crateIdFromItem(event.getItemInHand());
        if (crateId == null) {
            return;
        }
        Optional<CrateDefinition> optional = plugin.crates().find(crateId);
        if (optional.isEmpty()) {
            return;
        }
        CrateDefinition crate = optional.get();
        if (!crate.getModelEngineModelId().isBlank()) {
            event.setCancelled(true);
            Block block = event.getBlockPlaced();
            Bukkit.getScheduler().runTask(plugin, () -> placeHiddenModelCrate(event.getPlayer(), block, crate, event.getItemInHand()));
            return;
        }
        if (event.getBlockPlaced().getState() instanceof TileState state) {
            state.getPersistentDataContainer().set(placedCrateKey, PersistentDataType.STRING, crateId);
            state.update(true);
        }
        plugin.placedCrates().set(event.getBlockPlaced(), crateId, event.getPlayer().getLocation().getYaw());
        spawnOrUpdateHologram(event.getBlockPlaced(), crate);
    }

    private void placeHiddenModelCrate(Player player, Block block, CrateDefinition crate, ItemStack placedItem) {
        if (block.getType() != Material.AIR && !block.isReplaceable()) {
            player.sendMessage(Text.chat("<red>Hier kann keine ModelEngine-Crate gesetzt werden.</red>"));
            return;
        }
        plugin.placedCrates().set(block, crate.getId(), player.getLocation().getYaw());
        spawnOrUpdateHologram(block, crate);
        if (player.getGameMode() != GameMode.CREATIVE && placedItem != null && placedItem.getAmount() > 0) {
            placedItem.setAmount(placedItem.getAmount() - 1);
        }
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7F, 1.4F);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        String crateId = crateId(event.getBlock());
        if (crateId == null) {
            return;
        }
        if (!event.getPlayer().hasPermission("empirecrates.admin")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(plugin.msg("no-permission"));
            return;
        }
        event.setDropItems(false);
        removeHologram(event.getBlock());
        plugin.placedCrates().remove(event.getBlock());
        plugin.crates().find(crateId).ifPresent(crate -> event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), plugin.markedItems().markCrate(crate)));
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        for (BlockState state : event.getChunk().getTileEntities()) {
            Block block = state.getBlock();
            String crateId = crateId(block);
            if (crateId != null) {
                plugin.crates().find(crateId).ifPresent(crate -> spawnOrUpdateHologram(block, crate));
            }
        }
        for (Entity entity : event.getChunk().getEntities()) {
            String crateId = crateId(entity);
            if (crateId != null) {
                plugin.crates().find(crateId).ifPresent(crate -> spawnOrUpdateEntityHologram(entity, crate));
            }
        }
        plugin.placedCrates().all().forEach((key, crateId) -> plugin.placedCrates().location(key).ifPresent(location -> {
            if (!location.getWorld().equals(event.getWorld()) || location.getChunk().getX() != event.getChunk().getX() || location.getChunk().getZ() != event.getChunk().getZ()) {
                return;
            }
            Block block = location.getBlock();
            plugin.crates().find(crateId).ifPresent(crate -> {
                spawnOrUpdateHologram(block, crate);
            });
        }));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        for (Map<UUID, List<ArmorStand>> playerHolograms : holograms.values()) {
            for (List<ArmorStand> lines : playerHolograms.values()) {
                lines.forEach(line -> event.getPlayer().hideEntity(plugin, line));
            }
        }
        syncModelsForJoiningPlayer(event.getPlayer(), 20L);
        syncModelsForJoiningPlayer(event.getPlayer(), 60L);
        syncModelsForJoiningPlayer(event.getPlayer(), 120L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        openingCrates.removeIf(key -> key.startsWith(event.getPlayer().getUniqueId() + ":"));
        for (Map<UUID, List<ArmorStand>> playerHolograms : holograms.values()) {
            List<ArmorStand> lines = playerHolograms.remove(event.getPlayer().getUniqueId());
            if (lines != null) {
                lines.forEach(ArmorStand::remove);
            }
        }
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (!event.getPlugin().getName().equalsIgnoreCase("ModelEngine")) {
            return;
        }
        plugin.getLogger().info("ModelEngine became available. Refreshing crate models when its assets are ready.");
        plugin.scheduleVisualRestoreRetries();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (handleLootboxInteract(event)) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }
        String crateId = crateId(event.getClickedBlock());
        if (crateId == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        Optional<CrateDefinition> optional = plugin.crates().find(crateId);
        if (optional.isEmpty()) {
            player.sendMessage(Text.chat("<red>Diese Crate existiert nicht mehr.</red>"));
            return;
        }
        CrateDefinition crate = optional.get();
        spawnOrUpdateHologram(event.getClickedBlock(), crate);
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            plugin.gui().openPreview(player, crate);
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            openCrate(player, event.getClickedBlock(), crate);
        }
    }

    private boolean handleLootboxInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK
                && action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
            return false;
        }
        ItemStack handItem = event.getItem();
        String crateId = crateIdFromItem(handItem);
        if (crateId == null) {
            return false;
        }
        Optional<CrateDefinition> optional = plugin.crates().find(crateId);
        if (optional.isEmpty() || !optional.get().isLootboxEnabled()) {
            return false;
        }
        event.setCancelled(true);
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            plugin.gui().openPreview(event.getPlayer(), optional.get());
        } else {
            openLootboxCrate(event.getPlayer(), handItem, optional.get());
        }
        return true;
    }

    @EventHandler
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Entity entity = event.getRightClicked();
        String crateId = crateId(entity);
        if (crateId == null) {
            crateId = modelCarrierCrateId(entity);
            if (crateId != null) {
                event.setCancelled(true);
                String carrierCrateId = crateId;
                Block block = entity.getLocation().getBlock();
                plugin.crates().find(carrierCrateId).ifPresentOrElse(crate -> {
                    spawnOrUpdateHologram(block, crate);
                    openCrate(event.getPlayer(), block, crate);
                }, () -> event.getPlayer().sendMessage(Text.chat("<red>Diese Crate existiert nicht mehr.</red>")));
                return;
            }
        }
        if (crateId == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        plugin.crates().find(crateId).ifPresentOrElse(crate -> {
            spawnOrUpdateEntityModel(entity, crate);
            spawnOrUpdateEntityHologram(entity, crate);
            openEntityCrate(player, entity, crate);
        }, () -> player.sendMessage(Text.chat("<red>Diese Crate existiert nicht mehr.</red>")));
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        String crateId = crateId(event.getEntity());
        if (crateId == null) {
            crateId = modelCarrierCrateId(event.getEntity());
            if (crateId != null) {
                event.setCancelled(true);
                String carrierCrateId = crateId;
                Block block = event.getEntity().getLocation().getBlock();
                plugin.crates().find(carrierCrateId).ifPresentOrElse(crate -> {
                    spawnOrUpdateHologram(block, crate);
                    plugin.gui().openPreview(player, crate);
                }, () -> player.sendMessage(Text.chat("<red>Diese Crate existiert nicht mehr.</red>")));
                return;
            }
        }
        if (crateId == null) {
            return;
        }
        event.setCancelled(true);
        plugin.crates().find(crateId).ifPresentOrElse(crate -> {
            spawnOrUpdateEntityModel(event.getEntity(), crate);
            spawnOrUpdateEntityHologram(event.getEntity(), crate);
            plugin.gui().openPreview(player, crate);
        }, () -> player.sendMessage(Text.chat("<red>Diese Crate existiert nicht mehr.</red>")));
    }

    private void openCrate(Player player, Block block, CrateDefinition crate) {
        if (!player.hasPermission("empirecrates.open")) {
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        if (crate.getRewards().isEmpty()) {
            player.sendMessage(plugin.msg("no-rewards"));
            return;
        }
        String openingKey = openingKey(player, crate);
        if (openingCrates.contains(openingKey)) {
            player.sendMessage(plugin.msg("already-opening"));
            return;
        }
        String modelCooldownKey = modelCooldownKey(block);
        if (modelCooldownActive(modelCooldownKey, crate)) {
            player.sendMessage(plugin.msg("already-opening"));
            return;
        }
        CrateReward reward = roll(crate);
        if (!canReceiveReward(player, reward)) {
            player.sendMessage(plugin.msg("inventory-full"));
            return;
        }
        if (crate.isKeyRequired() && !consumeKey(player, crate)) {
            player.sendMessage(plugin.msg("need-key"));
            return;
        }
        int revealTicks = plugin.getConfig().getInt("animation.ticks-before-reveal", 64);
        int lockTicks = openingLockTicks(crate, revealTicks);
        openingCrates.add(openingKey);
        startModelCooldown(modelCooldownKey, crate, lockTicks);
        playModelOpening(block, crate, revealTicks);
        playAnimation(player, block, crate, reward);
        handleMilestone(player, crate);
        spawnOrUpdateHologram(block, crate);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                giveReward(player, crate, reward);
            } finally {
                releaseOpeningLater(openingKey, lockTicks - revealTicks);
            }
        }, revealTicks);
    }

    private void openEntityCrate(Player player, Entity entity, CrateDefinition crate) {
        if (!player.hasPermission("empirecrates.open")) {
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        if (crate.getRewards().isEmpty()) {
            player.sendMessage(plugin.msg("no-rewards"));
            return;
        }
        String openingKey = openingKey(player, crate);
        if (openingCrates.contains(openingKey)) {
            player.sendMessage(plugin.msg("already-opening"));
            return;
        }
        String modelCooldownKey = entityKey(entity);
        if (modelCooldownActive(modelCooldownKey, crate)) {
            player.sendMessage(plugin.msg("already-opening"));
            return;
        }
        CrateReward reward = roll(crate);
        if (!canReceiveReward(player, reward)) {
            player.sendMessage(plugin.msg("inventory-full"));
            return;
        }
        if (crate.isKeyRequired() && !consumeKey(player, crate)) {
            player.sendMessage(plugin.msg("need-key"));
            return;
        }
        int revealTicks = plugin.getConfig().getInt("animation.ticks-before-reveal", 64);
        int lockTicks = openingLockTicks(crate, revealTicks);
        openingCrates.add(openingKey);
        startModelCooldown(modelCooldownKey, crate, lockTicks);
        playEntityModelOpening(entity, crate, revealTicks);
        playAnimation(player, entity.getLocation().getBlock(), crate, reward);
        handleMilestone(player, crate);
        spawnOrUpdateEntityHologram(entity, crate);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                giveReward(player, crate, reward);
            } finally {
                releaseOpeningLater(openingKey, lockTicks - revealTicks);
            }
        }, revealTicks);
    }

    private void openLootboxCrate(Player player, ItemStack lootboxItem, CrateDefinition crate) {
        if (!player.hasPermission("empirecrates.open")) {
            player.sendMessage(plugin.msg("no-permission"));
            return;
        }
        if (crate.getRewards().isEmpty()) {
            player.sendMessage(plugin.msg("no-rewards"));
            return;
        }
        String openingKey = openingKey(player, crate);
        if (openingCrates.contains(openingKey)) {
            player.sendMessage(plugin.msg("already-opening"));
            return;
        }
        CrateReward reward = roll(crate);
        if (!canReceiveLootboxReward(player, reward, lootboxItem)) {
            player.sendMessage(plugin.msg("inventory-full"));
            return;
        }
        if (crate.isKeyRequired() && !consumeKey(player, crate)) {
            player.sendMessage(plugin.msg("need-key"));
            return;
        }
        if (player.getGameMode() != GameMode.CREATIVE && lootboxItem != null) {
            lootboxItem.setAmount(lootboxItem.getAmount() - 1);
        }
        int revealTicks = plugin.getConfig().getInt("animation.ticks-before-reveal", 64);
        openingCrates.add(openingKey);
        playAnimation(player, player.getLocation().getBlock(), crate, reward);
        handleMilestone(player, crate);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                giveReward(player, crate, reward);
            } finally {
                openingCrates.remove(openingKey);
            }
        }, revealTicks);
    }

    private boolean consumeKey(Player player, CrateDefinition crate) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }
            String keyId = plugin.markedItems().keyId(item);
            if (crate.getKeyId().equalsIgnoreCase(keyId)) {
                item.setAmount(item.getAmount() - 1);
                return true;
            }
        }
        return false;
    }

    private void handleMilestone(Player player, CrateDefinition crate) {
        if (!crate.isMilestoneEnabled()) {
            return;
        }
        int target = crate.getMilestoneTarget();
        int progress = plugin.progress().add(player, crate.getId(), 1);
        if (progress < target) {
            return;
        }
        int remaining = progress - target;
        plugin.progress().set(player, crate.getId(), remaining);
        plugin.crates().find(crate.getMilestoneRewardCrateId()).ifPresent(rewardKeyCrate -> {
            player.getInventory().addItem(plugin.markedItems().markKey(rewardKeyCrate));
            player.sendMessage(plugin.msg("milestone-key", Map.of("crate", rewardKeyCrate.getDisplayName())));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.9F, 1.2F);
        });
    }

    private CrateReward roll(CrateDefinition crate) {
        return RewardChanceCalculator.roll(crate.getRewards());
    }

    private void giveReward(Player player, CrateDefinition crate, CrateReward reward) {
        ItemStack rewardItem = rewardItem(reward);
        if (reward.getType() == RewardType.ITEM && rewardItem != null) {
            giveOrDrop(player, rewardItem);
        } else if (reward.getType() == RewardType.COMMAND && reward.getCommand() != null && !reward.getCommand().isBlank()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolveCommandPlaceholders(reward.getCommand(), player));
        }
        player.sendMessage(wonMessage(crate, reward));
        if (plugin.getConfig().getBoolean("broadcast.enabled", true) && reward.isBroadcast()) {
            try {
                Bukkit.broadcast(broadcastComponent(player, crate, reward));
            } catch (RuntimeException exception) {
                plugin.getLogger().warning("Could not render broadcast message, using fallback: " + exception.getMessage());
                Bukkit.broadcast(fallbackBroadcastComponent(player, crate, reward));
            }
        }
    }

    private String resolveCommandPlaceholders(String command, Player player) {
        return command
                .replace("%player%", player.getName())
                .replace("%spieler%", player.getName());
    }

    private Component wonMessage(CrateDefinition crate, CrateReward reward) {
        String prefix = plugin.getConfig().getString("theme.prefix", "");
        return Text.chat(prefix)
                .append(Text.chat("<gradient:#7CFFB2:#F5B642>Du hast </gradient>"))
                .append(Text.chat(reward.getDisplayName()))
                .append(Text.chat(" <gradient:#7CFFB2:#F5B642>aus </gradient>"))
                .append(Text.chat(crate.getDisplayName()))
                .append(Text.chat(" <gradient:#7CFFB2:#F5B642>gewonnen.</gradient>"));
    }

    private Component broadcastComponent(Player player, CrateDefinition crate, CrateReward reward) {
        String template = plugin.getConfig().getString("broadcast.message", "");
        String resolved = template.replace("%player%", player.getName())
                .replace("%crate%", crate.getDisplayName())
                .replace("%message%", randomBroadcastMessage())
                .replace("%rarity%", reward.getRarity().miniMessage())
                .replace("%chance%", String.valueOf(effectiveChance(crate, reward)));
        String[] parts = resolved.split("%reward%", -1);
        Component component = Component.empty();
        for (int index = 0; index < parts.length; index++) {
            component = component.append(Text.chat(parts[index]));
            if (index + 1 < parts.length) {
                Component rewardName = Text.chat(reward.getDisplayName());
                ItemStack rewardItem = rewardItem(reward);
                if (rewardItem != null) {
                    rewardName = rewardName.hoverEvent(rewardItem.asHoverEvent());
                }
                component = component.append(rewardName);
            }
        }
        return component;
    }

    private Component fallbackBroadcastComponent(Player player, CrateDefinition crate, CrateReward reward) {
        Component rewardName = Text.chat(reward.getDisplayName());
        ItemStack rewardItem = rewardItem(reward);
        if (rewardItem != null) {
            rewardName = rewardName.hoverEvent(rewardItem.asHoverEvent());
        }
        return Text.chat("<newline><dark_gray>----------------------------------------------------</dark_gray><newline>")
                .append(Text.chat("<bold><gradient:#F5B642:#9B4DFF>EMPIRECRATES DROP</gradient></bold><newline>"))
                .append(Text.chat("<newline><gradient:#FFFFFF:#F5B642>" + player.getName() + "</gradient> <gray>" + randomBroadcastMessage() + "</gray><newline><newline>"))
                .append(rewardName)
                .append(Text.chat("<newline><newline><gray>aus</gray> " + crate.getDisplayName() + " <dark_gray>|</dark_gray> " + reward.getRarity().miniMessage() + " <dark_gray>|</dark_gray> <gray>" + effectiveChance(crate, reward) + "% Chance</gray><newline>"))
                .append(Text.chat("<dark_gray>----------------------------------------------------</dark_gray><newline>"));
    }

    private double effectiveChance(CrateDefinition crate, CrateReward reward) {
        return RewardChanceCalculator.effectivePercent(crate.getRewards(), reward);
    }

    private String randomBroadcastMessage() {
        List<String> messages = plugin.getConfig().getStringList("broadcast.random-messages");
        if (messages.isEmpty()) {
            return "hat einen seltenen Gewinn gezogen";
        }
        return messages.get(ThreadLocalRandom.current().nextInt(messages.size()));
    }

    private void playAnimation(Player player, Block block, CrateDefinition crate, CrateReward reward) {
        if (crate.getAnimation().isGuiAnimation()) {
            playGuiAnimation(player, crate, reward);
            return;
        }
        Location center = block.getLocation().add(0.5D, OPEN_ANIMATION_BASE_Y, 0.5D);
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, 1.0F, 1.0F);
        block.getWorld().spawnParticle(Particle.PORTAL, center, 18, 0.25D, 0.35D, 0.25D, 0.01D);
        List<CrateReward> cycleRewards = cycleRewards(crate, reward);
        ItemDisplay itemDisplay = (ItemDisplay) block.getWorld().spawnEntity(center, EntityType.ITEM_DISPLAY);
        itemDisplay.setBillboard(Display.Billboard.CENTER);
        itemDisplay.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
        float itemScale = (float) plugin.getConfig().getDouble("animation.item-scale", 0.42D);
        itemDisplay.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(itemScale, itemScale, itemScale), new Quaternionf()));
        itemDisplay.setInterpolationDuration(2);
        itemDisplay.setTeleportDuration(2);
        TextDisplay nameDisplay = (TextDisplay) block.getWorld().spawnEntity(center.clone().add(0.0D, OPEN_ANIMATION_NAME_Y, 0.0D), EntityType.TEXT_DISPLAY);
        nameDisplay.setBillboard(Display.Billboard.CENTER);
        nameDisplay.setDefaultBackground(false);
        nameDisplay.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        nameDisplay.setTextOpacity((byte) 255);
        nameDisplay.setInterpolationDuration(2);
        nameDisplay.setTeleportDuration(2);
        nameDisplay.setSeeThrough(false);
        nameDisplay.setShadowed(true);
        applyRewardVisual(itemDisplay, nameDisplay, cycleRewards.get(0));
        int revealTicks = plugin.getConfig().getInt("animation.ticks-before-reveal", 64);
        int duration = plugin.getConfig().getInt("animation.hologram-duration-ticks", 70);
        double radius = plugin.getConfig().getDouble("animation.spin-radius", 1.2D);
        CrateAnimation animation = crate.getAnimation();
        AtomicInteger ticks = new AtomicInteger();
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            int tick = ticks.getAndIncrement() * 2;
            if (!itemDisplay.isValid() || !nameDisplay.isValid() || tick > duration) {
                itemDisplay.remove();
                nameDisplay.remove();
                task.cancel();
                return;
            }
            int cycleTicks = Math.max(6, plugin.getConfig().getInt("animation.reward-cycle-ticks", 8));
            if (tick < revealTicks && tick % cycleTicks == 0) {
                applyRewardVisual(itemDisplay, nameDisplay, cycleRewards.get((tick / cycleTicks) % cycleRewards.size()));
                block.getWorld().playSound(center, Sound.UI_BUTTON_CLICK, 0.22F, 1.55F);
            }
            Location next = animatedLocation(center, animation, tick, revealTicks, radius);
            itemDisplay.teleport(next);
            itemDisplay.setRotation(rotationYaw(animation, tick), rotationPitch(animation, tick));
            nameDisplay.teleport(next.clone().add(0.0D, OPEN_ANIMATION_NAME_Y, 0.0D));
            playAnimationParticles(block, animation, next, tick);
            if (tick == revealTicks) {
                applyRewardVisual(itemDisplay, nameDisplay, reward);
                block.getWorld().playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.35F);
                block.getWorld().spawnParticle(Particle.END_ROD, center, 18, 0.28D, 0.28D, 0.28D, 0.015D);
                block.getWorld().spawnParticle(Particle.GLOW, center, 12, 0.24D, 0.24D, 0.24D, 0.004D);
            }
        }, 0L, 2L);
    }

    private void playGuiAnimation(Player player, CrateDefinition crate, CrateReward reward) {
        Inventory inventory = Bukkit.createInventory(new de.otaku.empirecrates.gui.CrateGuiHolder(de.otaku.empirecrates.gui.GuiType.GUI_ANIMATION, crate.getId(), null), guiInventorySize(crate.getAnimation()), guiAnimationTitle(crate));
        ItemStack filler = guiFiller(Material.BLACK_STAINED_GLASS_PANE, "<dark_gray> </dark_gray>");
        fillGui(inventory, filler);
        player.openInventory(inventory);
        List<CrateReward> cycleRewards = cycleRewards(crate, reward);
        int revealTicks = plugin.getConfig().getInt("animation.ticks-before-reveal", 64);
        AtomicInteger ticks = new AtomicInteger();
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            int tick = ticks.getAndIncrement() * 2;
            if (!player.isOnline() || tick > revealTicks + 28) {
                closeGuiAnimation(player, inventory);
                task.cancel();
                return;
            }
            if (tick <= revealTicks) {
                renderGuiAnimationFrame(inventory, crate.getAnimation(), cycleRewards, reward, filler, tick, revealTicks);
                if (tick < revealTicks && tick % 8 == 0) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.18F, 1.45F);
                }
            }
            if (tick == revealTicks) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.35F);
            }
            if (tick >= revealTicks + 28) {
                closeGuiAnimation(player, inventory);
                task.cancel();
            }
        }, 0L, 2L);
    }

    private void renderGuiAnimationFrame(Inventory inventory, CrateAnimation animation, List<CrateReward> rewards, CrateReward finalReward, ItemStack filler, int tick, int revealTicks) {
        switch (animation) {
            case GUI_WHEEL -> renderGuiWheel(inventory, rewards, finalReward, filler, tick, revealTicks);
            case GUI_REVEAL -> renderGuiReveal(inventory, rewards, finalReward, filler, tick, revealTicks);
            default -> renderGuiRoulette(inventory, rewards, finalReward, filler, tick, revealTicks);
        }
    }

    private void renderGuiRoulette(Inventory inventory, List<CrateReward> rewards, CrateReward finalReward, ItemStack filler, int tick, int revealTicks) {
        int[] ring = guiRoulettePath();
        int prizeSlot = 23;
        fillGui(inventory, filler);
        renderRouletteFrame(inventory, tick);
        int finalIndex = Math.max(0, rewards.indexOf(finalReward));
        int spinStep = Math.max(1, tick / 2);
        int offset = tick >= revealTicks ? finalIndex : spinStep;
        for (int index = 0; index < ring.length; index++) {
            CrateReward shown = rewards.get(Math.floorMod(index + offset, rewards.size()));
            inventory.setItem(ring[index], guiRewardItem(shown, false));
        }
        renderRoulettePrizeHints(inventory);
        if (tick >= revealTicks) {
            renderRouletteFrame(inventory, revealTicks);
            for (int index = 0; index < ring.length; index++) {
                CrateReward shown = rewards.get(Math.floorMod(index + offset, rewards.size()));
                inventory.setItem(ring[index], guiRewardItem(shown, false));
            }
            renderRoulettePrizeHints(inventory);
            inventory.setItem(prizeSlot, guiRewardItem(finalReward, true));
        }
    }

    private void renderGuiWheel(Inventory inventory, List<CrateReward> rewards, CrateReward finalReward, ItemStack filler, int tick, int revealTicks) {
        int[] line = new int[]{10, 11, 12, 13, 14, 15, 16};
        int prizeSlot = 13;
        fillGui(inventory, filler);
        renderMovingBorder(inventory, tick);
        inventory.setItem(4, guiFiller(Material.LODESTONE, "<bold><gradient:#F5B642:#9B4DFF>Dein Preis</gradient></bold>"));
        int finalIndex = Math.max(0, rewards.indexOf(finalReward));
        int prizeIndex = indexOf(line, prizeSlot);
        int offset = tick >= revealTicks ? Math.floorMod(finalIndex - prizeIndex, rewards.size()) : tick / 2;
        for (int index = 0; index < line.length; index++) {
            CrateReward shown = rewards.get(Math.floorMod(index + offset, rewards.size()));
            inventory.setItem(line[index], guiRewardItem(shown, line[index] == prizeSlot));
        }
        if (tick >= revealTicks) {
            clearExcept(inventory, filler, prizeSlot, 4);
            renderMovingBorder(inventory, revealTicks);
            inventory.setItem(prizeSlot, guiRewardItem(finalReward, true));
        }
    }

    private void renderGuiReveal(Inventory inventory, List<CrateReward> rewards, CrateReward finalReward, ItemStack filler, int tick, int revealTicks) {
        int[][] phases = new int[][]{
                rectangleSlots(0, 5, 0, 8),
                rectangleSlots(1, 4, 1, 7),
                rectangleSlots(1, 4, 2, 6),
                rectangleSlots(2, 3, 2, 6),
                rectangleSlots(2, 3, 3, 5),
                {22}
        };
        fillGui(inventory, filler);
        int phase = tick >= revealTicks ? phases.length - 1 : Math.min(phases.length - 2, (int) ((long) tick * (phases.length - 1) / Math.max(1, revealTicks)));
        int rewardOffset = (tick / 3) + (phase * 11);
        for (int index = 0; index < phases[phase].length; index++) {
            CrateReward shown = rewards.get(Math.floorMod(index + rewardOffset, rewards.size()));
            inventory.setItem(phases[phase][index], guiRewardItem(shown, false));
        }
        if (tick >= revealTicks) {
            inventory.setItem(22, guiRewardItem(finalReward, true));
        }
    }

    private int[] rectangleSlots(int minRow, int maxRow, int minColumn, int maxColumn) {
        int[] slots = new int[(maxRow - minRow + 1) * (maxColumn - minColumn + 1)];
        int index = 0;
        for (int row = minRow; row <= maxRow; row++) {
            for (int column = minColumn; column <= maxColumn; column++) {
                slots[index++] = row * 9 + column;
            }
        }
        return slots;
    }

    private int guiInventorySize(CrateAnimation animation) {
        if (animation == CrateAnimation.GUI_WHEEL) {
            return 27;
        }
        if (animation == CrateAnimation.GUI_ROULETTE) {
            return 45;
        }
        return 54;
    }

    private void renderRouletteFrame(Inventory inventory, int tick) {
        int[] anchor = new int[]{13, 21, 31};
        for (int slot : anchor) {
            inventory.setItem(slot, guiFiller(Material.GRAY_STAINED_GLASS_PANE, "<dark_gray> </dark_gray>"));
        }
    }

    private void renderRoulettePrizeHints(Inventory inventory) {
        inventory.setItem(22, guiFiller(Material.LIME_STAINED_GLASS_PANE, "<bold><gradient:#F5B642:#9B4DFF>>> Reward hier</gradient></bold>"));
        inventory.setItem(24, guiFiller(Material.LIME_STAINED_GLASS_PANE, "<bold><gradient:#F5B642:#9B4DFF>Reward hier <<</gradient></bold>"));
    }

    private void renderMovingBorder(Inventory inventory, int tick) {
        int[] border = new int[]{0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 19, 20, 21, 23, 24, 25, 26};
        Material[] colors = new Material[]{Material.ORANGE_STAINED_GLASS_PANE, Material.PURPLE_STAINED_GLASS_PANE, Material.LIME_STAINED_GLASS_PANE, Material.CYAN_STAINED_GLASS_PANE, Material.MAGENTA_STAINED_GLASS_PANE};
        int offset = tick / 2;
        for (int index = 0; index < border.length; index++) {
            inventory.setItem(border[index], guiFiller(colors[Math.floorMod(index + offset, colors.length)], "<gradient:#F5B642:#9B4DFF>*</gradient>"));
        }
    }

    private void renderLargeMovingBorder(Inventory inventory, int tick) {
        int rows = inventory.getSize() / 9;
        Material[] colors = new Material[]{Material.ORANGE_STAINED_GLASS_PANE, Material.PURPLE_STAINED_GLASS_PANE, Material.LIME_STAINED_GLASS_PANE, Material.CYAN_STAINED_GLASS_PANE, Material.MAGENTA_STAINED_GLASS_PANE};
        int offset = tick / 2;
        int index = 0;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            int row = slot / 9;
            int col = slot % 9;
            if (row == 0 || row == rows - 1 || col == 0 || col == 8) {
                inventory.setItem(slot, guiFiller(colors[Math.floorMod(index + offset, colors.length)], "<gradient:#F5B642:#9B4DFF>*</gradient>"));
                index++;
            }
        }
    }

    private ItemStack guiRewardItem(CrateReward reward, boolean prize) {
        ItemStack item = animationItem(reward);
        return new ItemBuilder(item)
                .name(prize ? "<bold><gradient:#F5B642:#9B4DFF>Dein Preis</gradient></bold> <dark_gray>|</dark_gray> " + reward.getDisplayName() : reward.getDisplayName())
                .build();
    }

    private ItemStack guiFiller(Material material, String name) {
        return new ItemBuilder(material).name(name).build();
    }

    private void fillGui(Inventory inventory, ItemStack filler) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private void clearExcept(Inventory inventory, ItemStack filler, int... keepSlots) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            boolean keep = false;
            for (int keepSlot : keepSlots) {
                if (slot == keepSlot) {
                    keep = true;
                    break;
                }
            }
            if (!keep) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private void clearExcept(Inventory inventory, ItemStack filler, int keepSlot, int[] keepSlots) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            boolean keep = slot == keepSlot;
            for (int otherKeepSlot : keepSlots) {
                if (slot == otherKeepSlot) {
                    keep = true;
                    break;
                }
            }
            if (!keep) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private int indexOf(int[] slots, int target) {
        for (int index = 0; index < slots.length; index++) {
            if (slots[index] == target) {
                return index;
            }
        }
        return 0;
    }

    private void closeGuiAnimation(Player player, Inventory animationInventory) {
        if (player.getOpenInventory().getTopInventory() == animationInventory
                && player.getOpenInventory().getTopInventory().getHolder() instanceof de.otaku.empirecrates.gui.CrateGuiHolder holder
                && holder.type() == de.otaku.empirecrates.gui.GuiType.GUI_ANIMATION) {
            player.closeInventory();
        }
    }

    private int[] guiRoulettePath() {
        return new int[]{
                3, 4, 5,
                11, 15,
                19, 25,
                29, 33,
                39, 40, 41
        };
    }

    private Component guiAnimationTitle(CrateDefinition crate) {
        if (crate.getAnimation() != CrateAnimation.GUI_ROULETTE) {
            return Text.mm(crate.getDisplayName());
        }
        String plainName = PlainTextComponentSerializer.plainText().serialize(Text.mm(crate.getDisplayName())).trim();
        if (plainName.isBlank()) {
            plainName = crate.getId();
        }
        return Component.text("Opening " + plainName + "...");
    }

    private List<CrateReward> cycleRewards(CrateDefinition crate, CrateReward finalReward) {
        List<CrateReward> rewards = new ArrayList<>(crate.getRewards());
        rewards.removeIf(reward -> reward.getType() == RewardType.ITEM && reward.getItem() == null);
        if (rewards.isEmpty()) {
            rewards.add(finalReward);
        }
        if (!rewards.contains(finalReward)) {
            rewards.add(finalReward);
        }
        return rewards;
    }

    private void applyRewardVisual(ItemDisplay itemDisplay, TextDisplay nameDisplay, CrateReward reward) {
        itemDisplay.setItemStack(animationItem(reward));
        nameDisplay.text(Text.mm(reward.getDisplayName()));
    }

    private ItemStack animationItem(CrateReward reward) {
        ItemStack item = rewardItem(reward);
        if (item == null) {
            item = new ItemStack(Material.COMMAND_BLOCK);
        }
        item.setAmount(1);
        return item;
    }

    private Location animatedLocation(Location center, CrateAnimation animation, int tick, int revealTicks, double radius) {
        double progress = revealTicks <= 0 ? 1.0D : Math.min(1.0D, (double) tick / revealTicks);
        if (tick >= revealTicks) {
            return center.clone().add(0.0D, 1.1D, 0.0D);
        }
        return switch (animation) {
            case ORBIT -> {
                double angle = tick * 0.54D;
                double currentRadius = radius * (0.95D - progress * 0.35D);
                double secondWave = Math.sin(tick * 0.18D) * 0.22D;
                yield center.clone().add(Math.cos(angle) * currentRadius, 0.58D + Math.sin(angle * 1.65D) * 0.42D + secondWave + progress * 0.42D, Math.sin(angle) * currentRadius);
            }
            case FOUNTAIN -> {
                double bounce = Math.abs(Math.sin(progress * Math.PI * 4.0D));
                double swirl = tick * 0.32D;
                double spread = (1.0D - progress) * 0.42D;
                yield center.clone().add(Math.cos(swirl) * spread, 0.22D + bounce * 1.45D + progress * 0.42D, Math.sin(swirl) * spread);
            }
            case SPIRAL -> {
                double angle = tick * 0.5D;
                double currentRadius = radius * (1.0D - progress);
                yield center.clone().add(Math.cos(angle) * currentRadius, 0.15D + progress * 1.8D, Math.sin(angle) * currentRadius);
            }
            case PULSE -> {
                double pulse = 0.28D + Math.abs(Math.sin(tick * 0.22D)) * 0.95D;
                double angle = tick * 0.24D;
                yield center.clone().add(Math.cos(angle) * pulse * 0.34D, 0.72D + Math.sin(tick * 0.22D) * 0.16D + progress * 0.28D, Math.sin(angle) * pulse * 0.34D);
            }
            case COMET -> {
                double angle = progress * Math.PI * 1.35D - Math.PI * 0.18D;
                double sweep = Math.cos(angle) * radius * 1.15D;
                double depth = Math.sin(angle) * radius * 0.7D;
                yield center.clone().add(sweep, 0.44D + progress * 1.05D, depth);
            }
            case RAIN -> {
                double dropCycle = Math.floor(progress * 4.0D);
                double local = (progress * 4.0D) - dropCycle;
                double drop = 1.85D - (local * 1.35D);
                double sway = Math.sin(tick * 0.31D + dropCycle) * 0.42D;
                yield center.clone().add(sway, drop, Math.cos(tick * 0.23D + dropCycle) * 0.34D);
            }
            case GALAXY -> {
                double angle = tick * 0.34D;
                double wave = Math.sin(tick * 0.16D);
                double ellipse = radius * (0.72D + Math.sin(tick * 0.09D) * 0.18D);
                yield center.clone().add(Math.cos(angle) * ellipse, 0.92D + wave * 0.38D + progress * 0.25D, Math.sin(angle * 1.55D) * ellipse * 0.72D);
            }
            default -> {
                double angle = tick * 0.42D;
                double currentRadius = radius * (0.95D - progress * 0.48D);
                double lift = 0.48D + progress * 0.78D + Math.sin(tick * 0.2D) * 0.16D;
                yield center.clone().add(Math.cos(angle) * currentRadius, lift, Math.sin(angle) * currentRadius);
            }
        };
    }

    private float rotationYaw(CrateAnimation animation, int tick) {
        return switch (animation) {
            case ORBIT -> (tick * 18.0F) % 360.0F;
            case FOUNTAIN -> (tick * 7.0F) % 360.0F;
            case SPIRAL -> (tick * 20.0F) % 360.0F;
            case PULSE -> (float) (Math.sin(tick * 0.2D) * 35.0D);
            case COMET -> 90.0F;
            case RAIN -> (tick * 4.0F) % 360.0F;
            case GALAXY -> (tick * 15.0F) % 360.0F;
            default -> (tick * 12.0F) % 360.0F;
        };
    }

    private float rotationPitch(CrateAnimation animation, int tick) {
        return switch (animation) {
            case FOUNTAIN -> (float) Math.sin(tick * 0.25D) * 35.0F;
            case COMET -> 18.0F;
            case RAIN -> 35.0F;
            case GALAXY -> (float) Math.cos(tick * 0.18D) * 25.0F;
            default -> 0.0F;
        };
    }

    private void playAnimationParticles(Block block, CrateAnimation animation, Location location, int tick) {
        if (tick % 8 != 0) {
            return;
        }
        switch (animation) {
            case ORBIT -> block.getWorld().spawnParticle(Particle.ENCHANT, location, 2, 0.06D, 0.06D, 0.06D, 0.005D);
            case FOUNTAIN -> {
                block.getWorld().spawnParticle(Particle.FIREWORK, location, 3, 0.08D, 0.1D, 0.08D, 0.01D);
                block.getWorld().spawnParticle(Particle.END_ROD, location, 1, 0.04D, 0.04D, 0.04D, 0.004D);
            }
            case SPIRAL -> block.getWorld().spawnParticle(Particle.END_ROD, location, 2, 0.05D, 0.05D, 0.05D, 0.004D);
            case PULSE -> block.getWorld().spawnParticle(Particle.GLOW, location, 4, 0.12D, 0.12D, 0.12D, 0.002D);
            case COMET -> {
                block.getWorld().spawnParticle(Particle.FLAME, location, 3, 0.05D, 0.05D, 0.05D, 0.004D);
                block.getWorld().spawnParticle(Particle.SMOKE, location, 1, 0.03D, 0.03D, 0.03D, 0.002D);
            }
            case RAIN -> block.getWorld().spawnParticle(Particle.DRIPPING_WATER, location, 4, 0.08D, 0.12D, 0.08D, 0.0D);
            case GALAXY -> {
                block.getWorld().spawnParticle(Particle.PORTAL, location, 3, 0.06D, 0.06D, 0.06D, 0.01D);
                block.getWorld().spawnParticle(Particle.WITCH, location, 1, 0.03D, 0.03D, 0.03D, 0.0D);
            }
            default -> {
                block.getWorld().spawnParticle(Particle.CRIT, location, 2, 0.06D, 0.06D, 0.06D, 0.005D);
                block.getWorld().spawnParticle(Particle.END_ROD, location, 1, 0.03D, 0.03D, 0.03D, 0.003D);
            }
        }
    }

    private boolean canReceiveReward(Player player, CrateReward reward) {
        return canReceiveReward(player, reward, -1);
    }

    private boolean canReceiveLootboxReward(Player player, CrateReward reward, ItemStack lootboxItem) {
        if (lootboxItem == null || lootboxItem.getAmount() > 1) {
            return canReceiveReward(player, reward);
        }
        return canReceiveReward(player, reward, player.getInventory().getHeldItemSlot());
    }

    private boolean canReceiveReward(Player player, CrateReward reward, int forcedEmptySlot) {
        ItemStack rewardItem = rewardItem(reward);
        if (reward.getType() != RewardType.ITEM || rewardItem == null) {
            return true;
        }
        int remaining = rewardItem.getAmount();
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack item = contents[slot];
            if (slot == forcedEmptySlot) {
                return true;
            }
            if (item == null || item.getType() == Material.AIR) {
                return true;
            }
            if (item.isSimilar(rewardItem)) {
                remaining -= Math.max(0, item.getMaxStackSize() - item.getAmount());
                if (remaining <= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private void giveOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private ItemStack rewardItem(CrateReward reward) {
        if (reward == null) {
            return null;
        }
        ItemStack configured = reward.getItem();
        if (configured != null) {
            return configured;
        }
        if (!reward.getExcellentItemTag().isBlank()) {
            return ExcellentItemFactory.fromExcellentTag(reward.getExcellentItemTag(), null);
        }
        return null;
    }

    private String openingKey(Player player, CrateDefinition crate) {
        return player.getUniqueId() + ":" + crate.getId();
    }

    private String modelCooldownKey(Block block) {
        return "block:" + blockKey(block);
    }

    private boolean modelCooldownActive(String key, CrateDefinition crate) {
        if (crate.getModelEngineModelId().isBlank()) {
            return false;
        }
        long until = modelOpenCooldowns.getOrDefault(key, 0L);
        if (until <= System.currentTimeMillis()) {
            modelOpenCooldowns.remove(key);
            return false;
        }
        return true;
    }

    private void startModelCooldown(String key, CrateDefinition crate, int lockTicks) {
        if (crate.getModelEngineModelId().isBlank()) {
            return;
        }
        long lockMillis = Math.max(1L, lockTicks) * 50L;
        modelOpenCooldowns.put(key, System.currentTimeMillis() + lockMillis);
    }

    private int openingLockTicks(CrateDefinition crate, int revealTicks) {
        if (crate.getModelEngineModelId().isBlank()) {
            return revealTicks;
        }
        int extra = plugin.getConfig().getInt("animation.model-cooldown-after-reveal-ticks", 50);
        return revealTicks + Math.max(10, extra);
    }

    private void releaseOpeningLater(String openingKey, int delayTicks) {
        if (delayTicks <= 0) {
            openingCrates.remove(openingKey);
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> openingCrates.remove(openingKey), delayTicks);
    }

    private String crateIdFromItem(ItemStack item) {
        String crateId = plugin.markedItems().crateId(item);
        if (crateId != null) {
            return crateId;
        }
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        ItemStack placed = item.clone();
        placed.setAmount(1);
        for (CrateDefinition crate : plugin.crates().all()) {
            ItemStack configured = crate.getCrateItem();
            if (configured != null && configured.isSimilar(placed)) {
                return crate.getId();
            }
        }
        return null;
    }

    private String crateId(Block block) {
        if (!(block.getState() instanceof TileState state)) {
            return plugin.placedCrates().get(block);
        }
        String crateId = state.getPersistentDataContainer().get(placedCrateKey, PersistentDataType.STRING);
        return crateId == null ? plugin.placedCrates().get(block) : crateId;
    }

    private String crateId(Entity entity) {
        return entity.getPersistentDataContainer().get(placedCrateKey, PersistentDataType.STRING);
    }

    private String modelCarrierCrateId(Entity entity) {
        return entity.getPersistentDataContainer().get(modelCarrierCrateKey, PersistentDataType.STRING);
    }

    private Entity targetEntity(Player player) {
        RayTraceResult result = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getEyeLocation().getDirection(), 8.0D, 0.45D, entity -> entity.isValid() && !(entity instanceof Player));
        return result == null ? null : result.getHitEntity();
    }

    private String entityKey(Entity entity) {
        return "entity:" + entity.getUniqueId();
    }

    private void removeEntityCrate(Entity entity) {
        entity.getPersistentDataContainer().remove(placedCrateKey);
        removeHologram(entityKey(entity));
        destroyModelHandle(entityKey(entity));
        modelEntities.remove(entityKey(entity));
        modelEntityIds.remove(entityKey(entity));
    }

    private void spawnOrUpdateHologram(Block block, CrateDefinition crate) {
        spawnOrUpdateModel(block, crate);
        String key = blockKey(block);
        if (!holograms.containsKey(key)) {
            holograms.put(key, new HashMap<>());
            startBlockAuraTask(block, crate, key);
            long interval = plugin.getConfig().getLong("hologram.update-ticks", 40L);
            Bukkit.getScheduler().runTaskTimer(plugin, task -> {
                if (!holograms.containsKey(key) || block.getType() == Material.AIR || crateId(block) == null) {
                    removeHologram(block);
                    task.cancel();
                    return;
                }
                updateHologram(block, crate);
            }, 1L, Math.max(10L, interval));
        }
        updateHologram(block, crate);
    }

    private void spawnOrUpdateEntityHologram(Entity entity, CrateDefinition crate) {
        spawnOrUpdateEntityModel(entity, crate);
        String key = entityKey(entity);
        if (!holograms.containsKey(key)) {
            holograms.put(key, new HashMap<>());
            startEntityAuraTask(entity, crate, key);
            long interval = plugin.getConfig().getLong("hologram.update-ticks", 40L);
            Bukkit.getScheduler().runTaskTimer(plugin, task -> {
                if (!holograms.containsKey(key) || !entity.isValid() || crateId(entity) == null) {
                    removeHologram(key);
                    task.cancel();
                    return;
                }
                updateEntityHologram(entity, crate);
            }, 1L, Math.max(10L, interval));
        }
        updateEntityHologram(entity, crate);
    }

    private void startBlockAuraTask(Block block, CrateDefinition crate, String key) {
        AtomicInteger ticks = new AtomicInteger();
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!holograms.containsKey(key) || block.getType() == Material.AIR || crateId(block) == null) {
                task.cancel();
                return;
            }
            renderAura(block.getLocation().add(0.5D, 0.08D, 0.5D), crate, ticks.getAndIncrement());
        }, 1L, 4L);
    }

    private void startEntityAuraTask(Entity entity, CrateDefinition crate, String key) {
        AtomicInteger ticks = new AtomicInteger();
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!holograms.containsKey(key) || !entity.isValid() || crateId(entity) == null) {
                task.cancel();
                return;
            }
            renderAura(entity.getLocation().add(0.0D, 0.08D, 0.0D), crate, ticks.getAndIncrement());
        }, 1L, 4L);
    }

    private void renderAura(Location center, CrateDefinition crate, int tick) {
        CrateAura aura = crate.getAura();
        Particle particle = crate.getAuraParticle();
        if (aura == CrateAura.NONE || particle == null || center.getWorld() == null) {
            return;
        }
        switch (aura) {
            case RING -> renderFlatAuraRing(center, particle, tick);
            case HELIX -> renderTallAuraHelix(center, particle, tick);
            case PULSE -> renderPulseAura(center, particle, tick);
            case FLAME -> renderFlameAura(center, particle, tick);
            case GALAXY -> renderGalaxyAura(center, particle, tick);
            default -> {
            }
        }
    }

    private void renderFlatAuraRing(Location center, Particle particle, int tick) {
        for (int index = 0; index < 24; index++) {
            double angle = ((Math.PI * 2.0D) / 24.0D) * index;
            double radius = index % 2 == 0 ? 1.08D : 0.82D;
            Location point = center.clone().add(Math.cos(angle) * radius, 0.05D, Math.sin(angle) * radius);
            safeParticle(point, particle, 1, 0.01D, 0.005D, 0.01D, 0.0D);
        }
        double markerAngle = tick * 0.28D;
        for (int index = 0; index < 3; index++) {
            double angle = markerAngle + index * Math.PI * 2.0D / 3.0D;
            Location marker = center.clone().add(Math.cos(angle) * 1.22D, 0.09D, Math.sin(angle) * 1.22D);
            safeParticle(marker, particle, 2, 0.025D, 0.01D, 0.025D, 0.0D);
        }
    }

    private void renderTallAuraHelix(Location center, Particle particle, int tick) {
        for (int index = 0; index < 18; index++) {
            double progress = index / 17.0D;
            double y = 0.05D + progress * 2.2D;
            double radius = 0.42D + progress * 0.28D;
            double angle = tick * 0.24D + progress * Math.PI * 4.4D;
            Location first = center.clone().add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
            Location second = center.clone().add(Math.cos(angle + Math.PI) * radius, y, Math.sin(angle + Math.PI) * radius);
            safeParticle(first, particle, 1, 0.01D, 0.01D, 0.01D, 0.0D);
            safeParticle(second, particle, 1, 0.01D, 0.01D, 0.01D, 0.0D);
        }
        safeParticle(center.clone().add(0.0D, 2.34D, 0.0D), particle, 3, 0.12D, 0.04D, 0.12D, 0.0D);
    }

    private void renderPulseAura(Location center, Particle particle, int tick) {
        double phase = (tick % 18) / 18.0D;
        double radius = 0.35D + phase * 1.15D;
        double fadeHeight = 0.45D + Math.sin(phase * Math.PI) * 0.35D;
        for (int index = 0; index < 20; index++) {
            double angle = ((Math.PI * 2.0D) / 20.0D) * index;
            Location wave = center.clone().add(Math.cos(angle) * radius, fadeHeight, Math.sin(angle) * radius);
            safeParticle(wave, particle, 1, 0.018D, 0.018D, 0.018D, 0.0D);
        }
        double core = 0.18D + Math.sin(phase * Math.PI) * 0.18D;
        safeParticle(center.clone().add(0.0D, 0.72D, 0.0D), particle, 5, core, core, core, 0.0D);
    }

    private void renderFlameAura(Location center, Particle particle, int tick) {
        for (int column = 0; column < 6; column++) {
            double angle = column * Math.PI * 2.0D / 6.0D + Math.sin(tick * 0.12D) * 0.18D;
            double baseX = Math.cos(angle) * 0.52D;
            double baseZ = Math.sin(angle) * 0.52D;
            for (int step = 0; step < 4; step++) {
                double y = 0.12D + step * 0.34D + Math.floorMod(tick + column * 3, 8) * 0.035D;
                double curl = Math.sin(tick * 0.3D + step + column) * 0.08D;
                Location point = center.clone().add(baseX + curl, y, baseZ - curl);
                safeParticle(point, particle, 1, 0.035D, 0.07D, 0.035D, 0.004D);
            }
        }
        safeParticle(center.clone().add(0.0D, 1.72D, 0.0D), particle, 2, 0.18D, 0.1D, 0.18D, 0.002D);
    }

    private void renderGalaxyAura(Location center, Particle particle, int tick) {
        renderOrbitPlane(center.clone().add(0.0D, 0.75D, 0.0D), particle, tick, 1.2D, 0.0D, 18);
        renderOrbitPlane(center.clone().add(0.0D, 0.75D, 0.0D), particle, -tick, 0.95D, Math.PI / 3.0D, 14);
        renderOrbitPlane(center.clone().add(0.0D, 0.75D, 0.0D), particle, tick + 9, 0.7D, Math.PI * 0.66D, 10);
        safeParticle(center.clone().add(0.0D, 0.75D, 0.0D), particle, 2, 0.12D, 0.12D, 0.12D, 0.0D);
    }

    private void renderOrbitPlane(Location center, Particle particle, int tick, double radius, double tilt, int points) {
        for (int index = 0; index < points; index++) {
            double angle = ((Math.PI * 2.0D) / points) * index + tick * 0.18D;
            double x = Math.cos(angle) * radius;
            double flatZ = Math.sin(angle) * radius;
            double y = Math.sin(tilt) * flatZ * 0.72D;
            double z = Math.cos(tilt) * flatZ * 0.48D;
            Location point = center.clone().add(x, y, z);
            safeParticle(point, particle, 1, 0.01D, 0.01D, 0.01D, 0.0D);
        }
    }

    private void safeParticle(Location location, Particle particle, int count, double offsetX, double offsetY, double offsetZ, double extra) {
        try {
            location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
        } catch (IllegalArgumentException ignored) {
            // Some Minecraft particles require extra data. They stay selectable, but are skipped safely for passive auras.
        }
    }

    private ArmorStand hologramLine(Location location, Player viewer) {
        ArmorStand stand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        stand.setInvisible(true);
        stand.setMarker(true);
        stand.setGravity(false);
        stand.setCustomNameVisible(true);
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.hideEntity(plugin, stand);
        }
        viewer.showEntity(plugin, stand);
        return stand;
    }

    private void updateHologram(Block block, CrateDefinition crate) {
        Map<UUID, List<ArmorStand>> blockHolograms = holograms.get(blockKey(block));
        if (blockHolograms == null) {
            return;
        }
        double maxDistance = plugin.getConfig().getDouble("hologram.view-distance", 9.0D);
        double maxDistanceSquared = maxDistance * maxDistance;
        Location center = block.getLocation().add(0.5D, 0.5D, 0.5D);
        for (Player player : block.getWorld().getPlayers()) {
            boolean inRange = player.getLocation().distanceSquared(center) <= maxDistanceSquared;
            List<ArmorStand> lines = blockHolograms.get(player.getUniqueId());
            if (!inRange) {
                if (lines != null) {
                    lines.forEach(ArmorStand::remove);
                    blockHolograms.remove(player.getUniqueId());
                }
                continue;
            }
            if (lines == null || lines.size() < 3) {
                lines = createPlayerHologram(block, crate, player);
                blockHolograms.put(player.getUniqueId(), lines);
            }
            Location base = block.getLocation().add(0.5D, crate.getHologramHeight(), 0.5D);
            lines.get(0).teleport(base.clone().add(0.0D, 0.50D, 0.0D));
            lines.get(1).teleport(base.clone().add(0.0D, 0.25D, 0.0D));
            lines.get(2).teleport(base);
            int keys = countKeys(player, crate);
            lines.get(0).customName(Text.mm(crate.getDisplayName()));
            lines.get(1).customName(keyHologramText(keys));
            if (crate.isMilestoneEnabled()) {
                int progress = plugin.progress().get(player, crate.getId());
                lines.get(2).customName(milestoneHologramText(progress, crate.getMilestoneTarget()));
            } else {
                lines.get(2).customName(Component.empty());
            }
        }
    }

    private Component keyHologramText(int keys) {
        String keyColor = keys <= 0 ? "<red>" : "<green>";
        return Text.chat("<white>Deine Keys:</white> " + keyColor + keys + "</" + (keys <= 0 ? "red" : "green") + ">");
    }

    private Component milestoneHologramText(int progress, int target) {
        return Text.chat("<white>Dein Milestone:</white> <gradient:#FFFFFF:#F5B642>" + progress + "/" + target + "</gradient>");
    }

    private void updateEntityHologram(Entity entity, CrateDefinition crate) {
        Map<UUID, List<ArmorStand>> entityHolograms = holograms.get(entityKey(entity));
        if (entityHolograms == null) {
            return;
        }
        double maxDistance = plugin.getConfig().getDouble("hologram.view-distance", 9.0D);
        double maxDistanceSquared = maxDistance * maxDistance;
        Location center = entity.getLocation();
        for (Player player : entity.getWorld().getPlayers()) {
            boolean inRange = player.getLocation().distanceSquared(center) <= maxDistanceSquared;
            List<ArmorStand> lines = entityHolograms.get(player.getUniqueId());
            if (!inRange) {
                if (lines != null) {
                    lines.forEach(ArmorStand::remove);
                    entityHolograms.remove(player.getUniqueId());
                }
                continue;
            }
            if (lines == null || lines.size() < 3) {
                lines = createEntityHologram(entity, crate, player);
                entityHolograms.put(player.getUniqueId(), lines);
            }
            Location base = entityHologramBase(entity, crate);
            lines.get(0).teleport(base.clone().add(0.0D, 0.50D, 0.0D));
            lines.get(1).teleport(base.clone().add(0.0D, 0.25D, 0.0D));
            lines.get(2).teleport(base);
            int keys = countKeys(player, crate);
            lines.get(0).customName(Text.mm(crate.getDisplayName()));
            lines.get(1).customName(keyHologramText(keys));
            if (crate.isMilestoneEnabled()) {
                int progress = plugin.progress().get(player, crate.getId());
                lines.get(2).customName(milestoneHologramText(progress, crate.getMilestoneTarget()));
            } else {
                lines.get(2).customName(Component.empty());
            }
        }
    }

    private List<ArmorStand> createPlayerHologram(Block block, CrateDefinition crate, Player viewer) {
        Location base = block.getLocation().add(0.5D, crate.getHologramHeight(), 0.5D);
        return List.of(
                hologramLine(base.clone().add(0.0D, 0.50D, 0.0D), viewer),
                hologramLine(base.clone().add(0.0D, 0.25D, 0.0D), viewer),
                hologramLine(base, viewer)
        );
    }

    private List<ArmorStand> createEntityHologram(Entity entity, CrateDefinition crate, Player viewer) {
        Location base = entityHologramBase(entity, crate);
        return List.of(
                hologramLine(base.clone().add(0.0D, 0.50D, 0.0D), viewer),
                hologramLine(base.clone().add(0.0D, 0.25D, 0.0D), viewer),
                hologramLine(base, viewer)
        );
    }

    private Location entityHologramBase(Entity entity, CrateDefinition crate) {
        Location location = entity.getLocation();
        return new Location(location.getWorld(), location.getX(), location.getY() + crate.getHologramHeight(), location.getZ());
    }

    private int countKeys(Player player, CrateDefinition crate) {
        int amount = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && crate.getKeyId().equalsIgnoreCase(plugin.markedItems().keyId(item))) {
                amount += item.getAmount();
            }
        }
        return amount;
    }

    private void spawnOrUpdateModel(Block block, CrateDefinition crate) {
        String key = blockKey(block);
        String modelId = crate.getModelEngineModelId();
        if (modelId.isBlank()) {
            removeModel(block);
            return;
        }
        Entity existing = existingModelEntity(key);
        ModelHandle existingHandle = modelHandles.get(key);
        if (existing != null && modelId.equalsIgnoreCase(modelEntityIds.get(key)) && modelEngine.isModelUsable(existingHandle)) {
            existing.teleport(modelLocation(block));
            existing.setRotation(plugin.placedCrates().rotation(block), 0.0F);
            hideModelCrateBlock(block, crate);
            return;
        }
        removeModel(block);
        if (!modelEngine.isAvailable()) {
            showFallbackCrateBlock(block, crate);
            return;
        }
        ArmorStand base = (ArmorStand) block.getWorld().spawnEntity(modelLocation(block), EntityType.ARMOR_STAND);
        base.setRotation(plugin.placedCrates().rotation(block), 0.0F);
        base.setInvisible(true);
        base.setGravity(false);
        base.setInvulnerable(true);
        base.setSilent(true);
        base.setPersistent(false);
        base.setCustomNameVisible(false);
        base.getPersistentDataContainer().set(modelCarrierCrateKey, PersistentDataType.STRING, crate.getId());
        ModelHandle handle = modelEngine.spawnModel(base, modelId);
        if (handle == null) {
            base.remove();
            showFallbackCrateBlock(block, crate);
            return;
        }
        modelEntities.put(key, base.getUniqueId());
        modelHandles.put(key, handle);
        modelEntityIds.put(key, modelId);
        hideModelCrateBlock(block, crate);
        playModelIdle(handle, crate);
    }

    private void syncModelsForJoiningPlayer(Player player, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> syncModelsForPlayer(player), delayTicks);
    }

    private void syncModelsForPlayer(Player player) {
        if (!player.isOnline()) {
            return;
        }
        double rangeSquared = 160.0D * 160.0D;
        plugin.placedCrates().all().forEach((storedKey, crateId) -> plugin.placedCrates().location(storedKey).ifPresent(location -> {
            if (!location.getWorld().equals(player.getWorld())
                    || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)
                    || location.distanceSquared(player.getLocation()) > rangeSquared) {
                return;
            }
            plugin.crates().find(crateId).ifPresent(crate -> syncBlockModelForPlayer(player, location.getBlock(), crate));
        }));
        for (org.bukkit.Chunk chunk : player.getWorld().getLoadedChunks()) {
            for (Entity entity : chunk.getEntities()) {
                if (entity.getLocation().distanceSquared(player.getLocation()) > rangeSquared) {
                    continue;
                }
                String crateId = crateId(entity);
                if (crateId == null) {
                    continue;
                }
                plugin.crates().find(crateId).ifPresent(crate -> syncEntityModelForPlayer(player, entity, crate));
            }
        }
    }

    private void syncBlockModelForPlayer(Player player, Block block, CrateDefinition crate) {
        if (crate.getModelEngineModelId().isBlank()) {
            return;
        }
        String key = blockKey(block);
        ModelHandle handle = modelHandles.get(key);
        if (handle == null || !modelEngine.isModelUsable(handle) || existingModelEntity(key) == null) {
            removeModel(block);
            spawnOrUpdateHologram(block, crate);
            handle = modelHandles.get(key);
        }
        if (handle == null) {
            return;
        }
        modelEngine.syncViewer(handle, player);
        if (!modelCooldownActive(modelCooldownKey(block), crate)) {
            playModelIdle(handle, crate);
        }
    }

    private void syncEntityModelForPlayer(Player player, Entity entity, CrateDefinition crate) {
        if (crate.getModelEngineModelId().isBlank()) {
            return;
        }
        String key = entityKey(entity);
        ModelHandle handle = modelHandles.get(key);
        if (handle == null) {
            spawnOrUpdateEntityHologram(entity, crate);
            handle = modelHandles.get(key);
        }
        if (handle == null) {
            return;
        }
        modelEngine.syncViewer(handle, player);
        if (!modelCooldownActive(key, crate)) {
            playModelIdle(handle, crate);
        }
    }

    private void spawnOrUpdateEntityModel(Entity entity, CrateDefinition crate) {
        String key = entityKey(entity);
        String modelId = crate.getModelEngineModelId();
        if (modelId.isBlank()) {
            destroyModelHandle(key);
            modelEntities.remove(key);
            modelEntityIds.remove(key);
            return;
        }
        ModelHandle existingHandle = modelHandles.get(key);
        if (modelId.equalsIgnoreCase(modelEntityIds.get(key)) && modelEngine.isModelUsable(existingHandle)) {
            playModelIdle(existingHandle, crate);
            return;
        }
        if (modelEntityIds.containsKey(key) || modelHandles.containsKey(key)) {
            destroyModelHandle(key);
            modelEntities.remove(key);
            modelEntityIds.remove(key);
        }
        if (!modelEngine.isAvailable()) {
            return;
        }
        ModelHandle handle = modelEngine.spawnModel(entity, modelId);
        if (handle == null) {
            return;
        }
        modelEntities.put(key, entity.getUniqueId());
        modelHandles.put(key, handle);
        modelEntityIds.put(key, modelId);
        playModelIdle(handle, crate);
    }

    private void hideModelCrateBlock(Block block, CrateDefinition crate) {
        if (crate.getModelEngineModelId().isBlank() || block.getType() == Material.BARRIER) {
            return;
        }
        block.setType(Material.BARRIER, false);
    }

    private void showFallbackCrateBlock(Block block, CrateDefinition crate) {
        if (block.getType() != Material.BARRIER && block.getType() != Material.AIR) {
            return;
        }
        ItemStack crateItem = crate.getCrateItem();
        Material material = crateItem == null ? Material.SHULKER_BOX : crateItem.getType();
        if (!material.isBlock() || material == Material.AIR || material == Material.BARRIER) {
            material = Material.SHULKER_BOX;
        }
        block.setType(material, false);
    }

    private Entity existingModelEntity(String key) {
        UUID uuid = modelEntities.get(key);
        if (uuid == null) {
            return null;
        }
        Entity entity = Bukkit.getEntity(uuid);
        if (entity == null || entity.isDead()) {
            modelEntities.remove(key);
            modelHandles.remove(key);
            modelEntityIds.remove(key);
            return null;
        }
        return entity;
    }

    private void playModelOpening(Block block, CrateDefinition crate, int revealTicks) {
        if (crate.getModelEngineModelId().isBlank()) {
            return;
        }
        spawnOrUpdateModel(block, crate);
        ModelHandle handle = modelHandles.get(blockKey(block));
        if (handle == null) {
            return;
        }
        playModelOpen(handle, crate);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ModelHandle current = modelHandles.get(blockKey(block));
            if (current != null) {
                playModelClose(current, crate);
            }
        }, Math.max(1L, revealTicks + 20L));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ModelHandle current = modelHandles.get(blockKey(block));
            if (current != null) {
                playModelIdle(current, crate);
            }
        }, Math.max(1L, revealTicks + 42L));
    }

    private void playEntityModelOpening(Entity entity, CrateDefinition crate, int revealTicks) {
        if (crate.getModelEngineModelId().isBlank()) {
            return;
        }
        spawnOrUpdateEntityModel(entity, crate);
        String key = entityKey(entity);
        ModelHandle handle = modelHandles.get(key);
        if (handle == null) {
            return;
        }
        playModelOpen(handle, crate);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ModelHandle current = modelHandles.get(key);
            if (current != null) {
                playModelClose(current, crate);
            }
        }, Math.max(1L, revealTicks + 20L));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            ModelHandle current = modelHandles.get(key);
            if (current != null) {
                playModelIdle(current, crate);
            }
        }, Math.max(1L, revealTicks + 42L));
    }

    private Location modelLocation(Block block) {
        Location location = block.getLocation().add(0.5D, 0.0D, 0.5D);
        location.setYaw(plugin.placedCrates().rotation(block));
        location.setPitch(0.0F);
        return location;
    }

    private void removeModel(Block block) {
        String key = blockKey(block);
        Entity entity = existingModelEntity(key);
        destroyModelHandle(key);
        if (entity != null) {
            entity.remove();
        }
        modelEntities.remove(key);
        modelHandles.remove(key);
        modelEntityIds.remove(key);
    }

    private void destroyModelHandle(String key) {
        ModelHandle handle = modelHandles.remove(key);
        if (handle != null) {
            modelEngine.destroyModel(handle);
        }
    }

    private void playModelOpen(ModelHandle handle, CrateDefinition crate) {
        modelEngine.playAnimation(handle, modelAnimationCandidates(crate.getModelEngineOpenAnimation(), "opening", "opening_rare"), false);
    }

    private void playModelClose(ModelHandle handle, CrateDefinition crate) {
        modelEngine.playAnimation(handle, modelAnimationCandidates(crate.getModelEngineCloseAnimation(), "closing", "close"), false);
    }

    private void playModelIdle(ModelHandle handle, CrateDefinition crate) {
        modelEngine.playAnimation(handle, modelAnimationCandidates(crate.getModelEngineIdleAnimation(), "idle", "Idle", "idle_mouve"), true);
    }

    private List<String> modelAnimationCandidates(String configured, String... fallbacks) {
        List<String> candidates = new ArrayList<>();
        addAnimationCandidate(candidates, configured);
        for (String fallback : fallbacks) {
            addAnimationCandidate(candidates, fallback);
        }
        return candidates;
    }

    private void addAnimationCandidate(List<String> candidates, String animation) {
        if (animation == null || animation.isBlank() || animation.equalsIgnoreCase("none")) {
            return;
        }
        String trimmed = animation.trim();
        if (!candidates.contains(trimmed)) {
            candidates.add(trimmed);
        }
        String lower = trimmed.toLowerCase();
        if (!candidates.contains(lower)) {
            candidates.add(lower);
        }
        String title = Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
        if (!candidates.contains(title)) {
            candidates.add(title);
        }
    }

    private void removeHologram(Block block) {
        removeModel(block);
        removeHologram(blockKey(block));
    }

    private void removeHologram(String key) {
        Map<UUID, List<ArmorStand>> playerHolograms = holograms.remove(key);
        if (playerHolograms != null) {
            playerHolograms.values().forEach(lines -> lines.forEach(ArmorStand::remove));
        }
    }

    public void removeAllVisuals() {
        for (Map<UUID, List<ArmorStand>> playerHolograms : holograms.values()) {
            playerHolograms.values().forEach(lines -> lines.forEach(ArmorStand::remove));
        }
        holograms.clear();
        for (String key : new ArrayList<>(modelHandles.keySet())) {
            destroyModelHandle(key);
        }
        for (Map.Entry<String, UUID> entry : new ArrayList<>(modelEntities.entrySet())) {
            if (entry.getKey().startsWith("entity:")) {
                continue;
            }
            UUID uuid = entry.getValue();
            Entity entity = Bukkit.getEntity(uuid);
            if (entity != null) {
                entity.remove();
            }
        }
        modelEntities.clear();
        modelHandles.clear();
        modelEntityIds.clear();
    }

    private String blockKey(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }
}


