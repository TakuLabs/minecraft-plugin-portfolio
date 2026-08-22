package de.otaku.empirecrates.util;

import de.otaku.empirecrates.model.CrateReward;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class RewardChanceCalculator {
    private RewardChanceCalculator() {
    }

    public static CrateReward roll(List<CrateReward> configuredRewards) {
        List<WeightedReward> rewards = weightedRewards(configuredRewards);
        if (rewards.isEmpty()) {
            return null;
        }
        double totalWeight = rewards.stream().mapToDouble(WeightedReward::weight).sum();
        double cursor = ThreadLocalRandom.current().nextDouble(totalWeight);
        double cumulative = 0.0D;
        for (WeightedReward reward : rewards) {
            cumulative += reward.weight();
            if (cursor < cumulative) {
                return reward.reward();
            }
        }
        return rewards.get(rewards.size() - 1).reward();
    }

    public static double effectivePercent(List<CrateReward> configuredRewards, CrateReward target) {
        if (target == null) {
            return 0.0D;
        }
        for (WeightedReward reward : weightedRewards(configuredRewards)) {
            if (reward.reward() == target || reward.reward().getId().equals(target.getId())) {
                return roundPercent(reward.percent());
            }
        }
        return 0.0D;
    }

    public static Map<CrateReward, Double> effectivePercents(List<CrateReward> configuredRewards) {
        Map<CrateReward, Double> result = new IdentityHashMap<>();
        for (WeightedReward reward : weightedRewards(configuredRewards)) {
            result.put(reward.reward(), roundPercent(reward.percent()));
        }
        return result;
    }

    private static List<WeightedReward> weightedRewards(List<CrateReward> configuredRewards) {
        if (configuredRewards == null || configuredRewards.isEmpty()) {
            return List.of();
        }
        List<WeightedReward> result = new ArrayList<>(configuredRewards.size());
        double totalWeight = 0.0D;
        for (CrateReward reward : configuredRewards) {
            if (reward == null) {
                continue;
            }
            double weight = reward.getChancePercent();
            if (Double.isNaN(weight) || Double.isInfinite(weight) || weight <= 0.0D) {
                continue;
            }
            result.add(new WeightedReward(reward, weight, 0.0D));
            totalWeight += weight;
        }
        if (!(totalWeight > 0.0D)) {
            return List.of();
        }
        double finalTotalWeight = totalWeight;
        return result.stream()
                .map(reward -> new WeightedReward(reward.reward(), reward.weight(), reward.weight() / finalTotalWeight * 100.0D))
                .toList();
    }

    private static double roundPercent(double percent) {
        return Math.round(percent * 10000.0D) / 10000.0D;
    }

    private record WeightedReward(CrateReward reward, double weight, double percent) {
    }
}
