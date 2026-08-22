package de.otaku.birthdayevent.reward;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public final class RewardSelector {
    private final List<RewardCrate> crates;
    private final int totalWeight;

    public RewardSelector(List<RewardCrate> crates) {
        if (crates == null || crates.isEmpty()) {
            throw new IllegalArgumentException("At least one crate must be enabled");
        }
        this.crates = List.copyOf(crates);
        this.totalWeight = this.crates.stream().mapToInt(RewardCrate::weight).sum();
    }

    public RewardSelection select() {
        RewardCrate crate = selectCrate(ThreadLocalRandom.current().nextInt(totalWeight));
        int amount = crate.minKeys() == crate.maxKeys()
                ? crate.minKeys()
                : ThreadLocalRandom.current().nextInt(crate.minKeys(), crate.maxKeys() + 1);
        return new RewardSelection(crate.name(), amount);
    }

    public RewardSelection selectFixedCrate(String crateName) {
        RewardCrate crate = crates.stream()
                .filter(candidate -> candidate.name().equalsIgnoreCase(crateName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown crate: " + crateName));
        int amount = crate.minKeys() == crate.maxKeys()
                ? crate.minKeys()
                : ThreadLocalRandom.current().nextInt(crate.minKeys(), crate.maxKeys() + 1);
        return new RewardSelection(crate.name(), amount);
    }

    private RewardCrate selectCrate(int roll) {
        int cursor = 0;
        for (RewardCrate crate : crates) {
            cursor += crate.weight();
            if (roll < cursor) {
                return crate;
            }
        }
        return crates.get(crates.size() - 1);
    }

    public List<RewardCrate> crates() {
        return crates;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof RewardSelector selector && Objects.equals(crates, selector.crates);
    }

    @Override
    public int hashCode() {
        return Objects.hash(crates);
    }
}
