package de.otaku.birthdayevent.config;

import de.otaku.birthdayevent.event.EventSettings;
import de.otaku.birthdayevent.reward.RewardCrate;

import java.util.List;

public record PluginSettings(
        EventSettings event,
        boolean titleEnabled,
        int titleFadeInTicks,
        int titleStayTicks,
        int titleFadeOutTicks,
        boolean fireworkEnabled,
        String rewardCommand,
        List<RewardCrate> crates,
        boolean rewardLogging
) {
}
