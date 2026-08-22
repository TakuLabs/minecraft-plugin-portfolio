package de.otaku.empirecrates.storage;

import de.otaku.empirecrates.EmpireCratesPlugin;
import de.otaku.empirecrates.model.CrateAnimation;
import de.otaku.empirecrates.model.CrateAura;
import de.otaku.empirecrates.model.CrateDefinition;
import de.otaku.empirecrates.model.CrateReward;
import de.otaku.empirecrates.model.RewardRarity;
import de.otaku.empirecrates.model.RewardType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class CrateStorage {
    private final EmpireCratesPlugin plugin;
    private final File file;
    private final Map<String, CrateDefinition> crates = new LinkedHashMap<>();

    public CrateStorage(EmpireCratesPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "crates.yml");
    }

    public void load() {
        crates.clear();
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        Set<RewardRarity> defaultBroadcasts = plugin.getConfig().getStringList("broadcast.default-enabled-rarities").stream()
                .map(RewardRarity::parse)
                .collect(Collectors.toSet());
        ConfigurationSection root = config.getConfigurationSection("crates");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            CrateDefinition crate = new CrateDefinition(id);
            crate.setDisplayName(section.getString("display-name", crate.getDisplayName()));
            crate.setLootboxEnabled(section.getBoolean("lootbox-enabled", crate.isLootboxEnabled()));
            crate.setKeyRequired(section.getBoolean("key-required", false));
            crate.setKeyId(section.getString("key-id", crate.getKeyId()));
            crate.setMilestoneEnabled(section.getBoolean("milestone.enabled", crate.isMilestoneEnabled()));
            crate.setMilestoneTarget(section.getInt("milestone.target", crate.getMilestoneTarget()));
            crate.setMilestoneRewardCrateId(section.getString("milestone.reward-crate-id", crate.getMilestoneRewardCrateId()));
            crate.setModelEngineModelId(section.getString("modelengine.model-id", crate.getModelEngineModelId()));
            crate.setModelEngineIdleAnimation(section.getString("modelengine.idle-animation", crate.getModelEngineIdleAnimation()));
            crate.setModelEngineOpenAnimation(section.getString("modelengine.open-animation", crate.getModelEngineOpenAnimation()));
            crate.setModelEngineCloseAnimation(section.getString("modelengine.close-animation", crate.getModelEngineCloseAnimation()));
            crate.setHologramHeight(section.getDouble("hologram-height", crate.getHologramHeight()));
            crate.setAnimation(CrateAnimation.parse(section.getString("animation", crate.getAnimation().name())));
            crate.setAura(CrateAura.parse(section.getString("aura", crate.getAura().name())));
            try {
                crate.setAuraParticle(section.getString("aura-particle", "standard"));
            } catch (IllegalArgumentException ignored) {
                crate.setAuraParticle("standard");
            }
            ItemStack crateItem = section.getItemStack("crate-item");
            ItemStack keyItem = section.getItemStack("key-item");
            if (crateItem != null) {
                crate.setCrateItem(crateItem);
            }
            if (keyItem != null) {
                crate.setKeyItem(keyItem);
            }
            ConfigurationSection rewards = section.getConfigurationSection("rewards");
            if (rewards != null) {
                for (String rewardId : rewards.getKeys(false)) {
                    ConfigurationSection rewardSection = rewards.getConfigurationSection(rewardId);
                    if (rewardSection == null) {
                        continue;
                    }
                    RewardType type = RewardType.valueOf(rewardSection.getString("type", "ITEM"));
                    CrateReward reward = new CrateReward(rewardId, rewardSection.getString("display-name", rewardId), type);
                    reward.setRarity(RewardRarity.parse(rewardSection.getString("rarity", "GEWOEHNLICH")));
                    if (!rewardSection.contains("rarity") && rewardSection.contains("weight")) {
                        reward.setRarity(fromLegacyWeight(rewardSection.getDouble("weight", 1.0D)));
                    }
                    double configuredChance = rewardSection.getDouble("chance-percent", reward.getRarity().chancePercent());
                    reward.setChancePercent(configuredChance > 0.0D ? configuredChance : reward.getRarity().chancePercent());
                    reward.setBroadcast(rewardSection.getBoolean("broadcast", defaultBroadcasts.contains(reward.getRarity()) || reward.getRarity().isBroadcastDefault()));
                    reward.setCommand(rewardSection.getString("command", ""));
                    reward.setItem(rewardSection.getItemStack("item"));
                    reward.setExcellentItemTag(rewardSection.getString("excellent-item-tag", ""));
                    crate.getRewards().add(reward);
                }
            }
            crates.put(crate.getId(), crate);
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (CrateDefinition crate : crates.values()) {
            String path = "crates." + crate.getId() + ".";
            config.set(path + "display-name", crate.getDisplayName());
            config.set(path + "lootbox-enabled", crate.isLootboxEnabled());
            config.set(path + "key-required", crate.isKeyRequired());
            config.set(path + "key-id", crate.getKeyId());
            config.set(path + "milestone.enabled", crate.isMilestoneEnabled());
            config.set(path + "milestone.target", crate.getMilestoneTarget());
            config.set(path + "milestone.reward-crate-id", crate.getMilestoneRewardCrateId());
            config.set(path + "modelengine.model-id", crate.getModelEngineModelId());
            config.set(path + "modelengine.idle-animation", crate.getModelEngineIdleAnimation());
            config.set(path + "modelengine.open-animation", crate.getModelEngineOpenAnimation());
            config.set(path + "modelengine.close-animation", crate.getModelEngineCloseAnimation());
            config.set(path + "hologram-height", crate.getHologramHeight());
            config.set(path + "animation", crate.getAnimation().name());
            config.set(path + "aura", crate.getAura().name());
            config.set(path + "aura-particle", crate.getAuraParticleName().equalsIgnoreCase("Standard") ? null : crate.getAuraParticleName());
            config.set(path + "crate-item", crate.getCrateItem());
            config.set(path + "key-item", crate.getKeyItem());
            for (CrateReward reward : crate.getRewards()) {
                String rewardPath = path + "rewards." + reward.getId() + ".";
                config.set(rewardPath + "display-name", reward.getDisplayName());
                config.set(rewardPath + "type", reward.getType().name());
                config.set(rewardPath + "rarity", reward.getRarity().name());
                config.set(rewardPath + "chance-percent", reward.getChancePercent());
                config.set(rewardPath + "broadcast", reward.isBroadcast());
                config.set(rewardPath + "item", reward.getItem());
                config.set(rewardPath + "command", reward.getCommand());
                if (!reward.getExcellentItemTag().isBlank()) {
                    config.set(rewardPath + "excellent-item-tag", reward.getExcellentItemTag());
                }
            }
        }
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().severe("Could not save crates.yml: " + exception.getMessage());
        }
    }

    public Optional<CrateDefinition> find(String id) {
        return Optional.ofNullable(crates.get(id == null ? "" : id.toLowerCase()));
    }

    public Collection<CrateDefinition> all() {
        return crates.values();
    }

    public CrateDefinition create(String id) {
        CrateDefinition crate = new CrateDefinition(id);
        crates.put(crate.getId(), crate);
        save();
        return crate;
    }

    public void remove(String id) {
        crates.remove(id.toLowerCase());
        save();
    }

    private RewardRarity fromLegacyWeight(double weight) {
        if (weight <= 0.25D) {
            return RewardRarity.JACKPOT;
        }
        if (weight <= 0.8D) {
            return RewardRarity.MYSTISCH;
        }
        if (weight <= 2.0D) {
            return RewardRarity.LEGENDAER;
        }
        if (weight <= 5.0D) {
            return RewardRarity.EPISCH;
        }
        if (weight <= 12.0D) {
            return RewardRarity.SELTEN;
        }
        if (weight <= 25.0D) {
            return RewardRarity.UNGEWOEHNLICH;
        }
        return RewardRarity.GEWOEHNLICH;
    }
}


