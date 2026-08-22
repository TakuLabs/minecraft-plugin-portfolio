package de.otaku.birthdayevent.reward;

import java.util.UUID;
import java.util.regex.Pattern;

public final class RewardCommandFormatter {
    private static final Pattern SAFE_PLAYER = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final Pattern SAFE_CRATE = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    private RewardCommandFormatter() {
    }

    public static String format(String template, String playerName, UUID uuid, RewardSelection selection) {
        if (!SAFE_PLAYER.matcher(playerName).matches()) {
            throw new IllegalArgumentException("Unsafe player name");
        }
        if (!SAFE_CRATE.matcher(selection.crate()).matches()) {
            throw new IllegalArgumentException("Unsafe crate name");
        }
        if (selection.amount() < 1) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        return template
                .replace("%player%", playerName)
                .replace("%uuid%", uuid.toString())
                .replace("%crate%", selection.crate())
                .replace("%amount%", String.valueOf(selection.amount()));
    }
}
