package de.otaku.empirecrates.model;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CrateDefinition {
    private final String id;
    private String displayName;
    private ItemStack crateItem;
    private boolean lootboxEnabled;
    private boolean keyRequired;
    private String keyId;
    private ItemStack keyItem;
    private boolean milestoneEnabled;
    private int milestoneTarget;
    private String milestoneRewardCrateId;
    private String modelEngineModelId;
    private String modelEngineIdleAnimation;
    private String modelEngineOpenAnimation;
    private String modelEngineCloseAnimation;
    private double hologramHeight;
    private CrateAnimation animation;
    private CrateAura aura;
    private Particle auraParticle;
    private final List<CrateReward> rewards = new ArrayList<>();

    public CrateDefinition(String id) {
        this.id = id.toLowerCase();
        this.displayName = "<gradient:#F5B642:#9B4DFF>" + id + "</gradient>";
        this.crateItem = new ItemStack(Material.SHULKER_BOX);
        this.lootboxEnabled = false;
        this.keyId = id + "_key";
        this.keyItem = new ItemStack(Material.TRIPWIRE_HOOK);
        this.milestoneEnabled = false;
        this.milestoneTarget = 50;
        this.milestoneRewardCrateId = id;
        this.modelEngineModelId = "";
        this.modelEngineIdleAnimation = "";
        this.modelEngineOpenAnimation = "";
        this.modelEngineCloseAnimation = "";
        this.hologramHeight = 2.25D;
        this.animation = CrateAnimation.ROULETTE;
        this.aura = CrateAura.NONE;
        this.auraParticle = null;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public ItemStack getCrateItem() {
        return crateItem == null ? null : crateItem.clone();
    }

    public void setCrateItem(ItemStack crateItem) {
        this.crateItem = crateItem == null ? new ItemStack(Material.SHULKER_BOX) : crateItem.clone();
    }

    public boolean isLootboxEnabled() {
        return lootboxEnabled;
    }

    public void setLootboxEnabled(boolean lootboxEnabled) {
        this.lootboxEnabled = lootboxEnabled;
    }

    public boolean isKeyRequired() {
        return keyRequired;
    }

    public void setKeyRequired(boolean keyRequired) {
        this.keyRequired = keyRequired;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId == null || keyId.isBlank() ? id + "_key" : keyId.toLowerCase();
    }

    public ItemStack getKeyItem() {
        return keyItem == null ? null : keyItem.clone();
    }

    public void setKeyItem(ItemStack keyItem) {
        this.keyItem = keyItem == null ? new ItemStack(Material.TRIPWIRE_HOOK) : keyItem.clone();
    }

    public List<CrateReward> getRewards() {
        return rewards;
    }

    public boolean isMilestoneEnabled() {
        return milestoneEnabled;
    }

    public void setMilestoneEnabled(boolean milestoneEnabled) {
        this.milestoneEnabled = milestoneEnabled;
    }

    public int getMilestoneTarget() {
        return milestoneTarget;
    }

    public void setMilestoneTarget(int milestoneTarget) {
        this.milestoneTarget = Math.max(1, milestoneTarget);
    }

    public String getMilestoneRewardCrateId() {
        return milestoneRewardCrateId;
    }

    public void setMilestoneRewardCrateId(String milestoneRewardCrateId) {
        this.milestoneRewardCrateId = milestoneRewardCrateId == null || milestoneRewardCrateId.isBlank() ? id : milestoneRewardCrateId.toLowerCase();
    }

    public String getModelEngineModelId() {
        return modelEngineModelId == null ? "" : modelEngineModelId;
    }

    public void setModelEngineModelId(String modelEngineModelId) {
        this.modelEngineModelId = modelEngineModelId == null || modelEngineModelId.equalsIgnoreCase("none") ? "" : modelEngineModelId.trim();
    }

    public String getModelEngineIdleAnimation() {
        return modelEngineIdleAnimation == null ? "" : modelEngineIdleAnimation;
    }

    public void setModelEngineIdleAnimation(String modelEngineIdleAnimation) {
        this.modelEngineIdleAnimation = cleanModelAnimation(modelEngineIdleAnimation);
    }

    public String getModelEngineOpenAnimation() {
        return modelEngineOpenAnimation == null ? "" : modelEngineOpenAnimation;
    }

    public void setModelEngineOpenAnimation(String modelEngineOpenAnimation) {
        this.modelEngineOpenAnimation = cleanModelAnimation(modelEngineOpenAnimation);
    }

    public String getModelEngineCloseAnimation() {
        return modelEngineCloseAnimation == null ? "" : modelEngineCloseAnimation;
    }

    public void setModelEngineCloseAnimation(String modelEngineCloseAnimation) {
        this.modelEngineCloseAnimation = cleanModelAnimation(modelEngineCloseAnimation);
    }

    public double getHologramHeight() {
        return hologramHeight;
    }

    public void setHologramHeight(double hologramHeight) {
        this.hologramHeight = Math.max(0.25D, Math.min(8.0D, hologramHeight));
    }

    private String cleanModelAnimation(String animation) {
        return animation == null || animation.isBlank() || animation.equalsIgnoreCase("none") ? "" : animation.trim();
    }

    public CrateAnimation getAnimation() {
        return animation;
    }

    public void setAnimation(CrateAnimation animation) {
        this.animation = animation == null ? CrateAnimation.ROULETTE : animation;
    }

    public CrateAura getAura() {
        return aura == null ? CrateAura.NONE : aura;
    }

    public void setAura(CrateAura aura) {
        this.aura = aura == null ? CrateAura.NONE : aura;
    }

    public Particle getAuraParticle() {
        if (auraParticle != null) {
            return auraParticle;
        }
        return getAura().particle();
    }

    public String getAuraParticleName() {
        return auraParticle == null ? "Standard" : auraParticle.name();
    }

    public void setAuraParticle(String particleName) {
        if (particleName == null || particleName.isBlank() || particleName.equalsIgnoreCase("default") || particleName.equalsIgnoreCase("standard") || particleName.equalsIgnoreCase("none")) {
            this.auraParticle = null;
            return;
        }
        this.auraParticle = Particle.valueOf(particleName.trim().toUpperCase());
    }
}


