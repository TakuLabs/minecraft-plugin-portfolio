package de.otaku.core;

import net.md_5.bungee.api.ChatColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

import java.util.List;

@SuppressWarnings({"deprecation", "removal"})
public final class OtakuMessages {
    public static final String MINI_PREFIX = "<dark_purple><bold>ExampleSMP</bold></dark_purple> <dark_gray>|</dark_gray> <light_purple>";
    public static final String LEGACY_PREFIX = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "ExampleSMP " + ChatColor.DARK_GRAY + "| " + ChatColor.LIGHT_PURPLE;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private OtakuMessages() {
    }

    public static net.kyori.adventure.text.Component miniPrefix() {
        return MINI_MESSAGE.deserialize(MINI_PREFIX);
    }

    public static void sendHelp(CommandSender sender, String title, List<String> entries) {
        sender.sendMessage(MINI_MESSAGE.deserialize(MINI_PREFIX
                + "<gradient:#25F4D0:#9B5CFF><bold>" + MINI_MESSAGE.escapeTags(title) + "</bold></gradient>"));
        for (String entry : entries) {
            String[] parts = entry.split("\\|", 2);
            String command = parts[0];
            String description = parts.length > 1 ? parts[1] : "";
            sender.sendMessage(MINI_MESSAGE.deserialize(
                    "<dark_gray>•</dark_gray> <#D8B4FE>" + MINI_MESSAGE.escapeTags(command)
                            + "</#D8B4FE> <dark_gray>-</dark_gray> <gradient:#C084FC:#F0ABFC>"
                            + MINI_MESSAGE.escapeTags(description) + "</gradient>"));
        }
    }

    public static String legacyGradient(String text, String startHex, String middleHex, String endHex) {
        int[] start = rgb(startHex);
        int[] middle = rgb(middleHex);
        int[] end = rgb(endHex);
        StringBuilder builder = new StringBuilder();
        int steps = Math.max(1, text.length() - 1);
        for (int index = 0; index < text.length(); index++) {
            double progress = (double) index / steps;
            int[] from = progress <= 0.5D ? start : middle;
            int[] to = progress <= 0.5D ? middle : end;
            double local = progress <= 0.5D ? progress / 0.5D : (progress - 0.5D) / 0.5D;
            int red = interpolate(from[0], to[0], local);
            int green = interpolate(from[1], to[1], local);
            int blue = interpolate(from[2], to[2], local);
            builder.append(ChatColor.of(String.format("#%02X%02X%02X", red, green, blue)))
                    .append(ChatColor.BOLD)
                    .append(text.charAt(index));
        }
        return builder.toString();
    }

    private static int[] rgb(String hex) {
        return new int[]{
                Integer.parseInt(hex.substring(1, 3), 16),
                Integer.parseInt(hex.substring(3, 5), 16),
                Integer.parseInt(hex.substring(5, 7), 16)
        };
    }

    private static int interpolate(int start, int end, double progress) {
        return (int) Math.round(start + (end - start) * progress);
    }
}

