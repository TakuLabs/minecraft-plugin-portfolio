package de.otaku.empirecrates.model;

import org.bukkit.Material;

public enum CrateAnimation {
    ROULETTE("Roulette", "Offen", "Kreisender Reward-Wechsel über der Crate.", Material.CLOCK, false),
    ORBIT("Orbit", "Offen", "Vertikale Item-Helix mit enger Enthüllung.", Material.ENDER_EYE, false),
    FOUNTAIN("Fountain", "Offen", "Reward springt wie eine Fontäne aus der Crate.", Material.FIREWORK_ROCKET, false),
    SPIRAL("Spiral", "Offen", "Reward steigt in einer Spirale nach oben.", Material.BREEZE_ROD, false),
    PULSE("Pulse", "Offen", "Reward pulsiert ruhig über der Crate.", Material.ECHO_SHARD, false),
    COMET("Comet", "Offen", "Reward zieht als schneller Schweif vorbei.", Material.FIRE_CHARGE, false),
    RAIN("Rain", "Offen", "Rewards fallen wie ein kurzer Drop-Regen.", Material.WATER_BUCKET, false),
    GALAXY("Galaxy", "Offen", "Reward schwebt in einer breiten Galaxy-Bewegung.", Material.END_CRYSTAL, false),
    GUI_ROULETTE("GUI Roulette", "GUI", "Ein GUI öffnet sich und der Gewinn rotiert über feste Slots.", Material.CHEST_MINECART, true),
    GUI_WHEEL("GUI Wheel", "GUI", "Rewards laufen horizontal durch das GUI bis zum Reveal.", Material.COMPASS, true),
    GUI_REVEAL("GUI Reveal", "GUI", "Das GUI baut Spannung auf und enthüllt den Reward in der Mitte.", Material.RECOVERY_COMPASS, true);

    private final String displayName;
    private final String category;
    private final String description;
    private final Material icon;
    private final boolean guiAnimation;

    CrateAnimation(String displayName, String category, String description, Material icon, boolean guiAnimation) {
        this.displayName = displayName;
        this.category = category;
        this.description = description;
        this.icon = icon;
        this.guiAnimation = guiAnimation;
    }

    public String displayName() {
        return displayName;
    }

    public Material icon() {
        return icon;
    }

    public String category() {
        return category;
    }

    public String description() {
        return description;
    }

    public boolean isGuiAnimation() {
        return guiAnimation;
    }

    public CrateAnimation next() {
        CrateAnimation[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static CrateAnimation parse(String input) {
        if (input == null || input.isBlank()) {
            return ROULETTE;
        }
        for (CrateAnimation animation : values()) {
            if (animation.name().equalsIgnoreCase(input) || animation.displayName.equalsIgnoreCase(input)) {
                return animation;
            }
        }
        return ROULETTE;
    }
}