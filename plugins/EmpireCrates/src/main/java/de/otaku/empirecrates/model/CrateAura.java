package de.otaku.empirecrates.model;

import org.bukkit.Material;
import org.bukkit.Particle;

public enum CrateAura {
    NONE("Keine Aura", "Aura deaktiviert.", Material.BARRIER, null),
    RING("Ring", "Ruhiger Partikelring um die Crate.", Material.AMETHYST_SHARD, Particle.END_ROD),
    HELIX("Helix", "Aufsteigende Doppelhelix um die Crate.", Material.BREEZE_ROD, Particle.ENCHANT),
    PULSE("Pulse", "Atmende Kreis-Aura mit Glow-Partikeln.", Material.ECHO_SHARD, Particle.GLOW),
    FLAME("Flame", "Warme Flammen-Aura mit kleiner Krone.", Material.BLAZE_POWDER, Particle.FLAME),
    GALAXY("Galaxy", "Breite Portal-Aura mit Sternenpunkten.", Material.END_CRYSTAL, Particle.PORTAL);

    private final String displayName;
    private final String description;
    private final Material icon;
    private final Particle particle;

    CrateAura(String displayName, String description, Material icon, Particle particle) {
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.particle = particle;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public Material icon() {
        return icon;
    }

    public Particle particle() {
        return particle;
    }

    public CrateAura next() {
        CrateAura[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static CrateAura parse(String input) {
        if (input == null || input.isBlank()) {
            return NONE;
        }
        for (CrateAura aura : values()) {
            if (aura.name().equalsIgnoreCase(input) || aura.displayName.equalsIgnoreCase(input)) {
                return aura;
            }
        }
        return NONE;
    }
}
