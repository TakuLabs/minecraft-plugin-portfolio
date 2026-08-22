package de.otaku.birthdayevent.reward;

public record RewardCrate(String name, int minKeys, int maxKeys, int weight) {
    public RewardCrate {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Crate name must not be blank");
        }
        if (minKeys < 1) {
            throw new IllegalArgumentException("minKeys must be at least 1");
        }
        if (maxKeys < minKeys) {
            throw new IllegalArgumentException("maxKeys must be >= minKeys");
        }
        if (weight < 1) {
            throw new IllegalArgumentException("weight must be at least 1");
        }
    }
}
