package de.otaku.empirecrates.util;

import org.bukkit.ChatColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;

public final class Text {
    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private Text() {
    }

    public static String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }

    public static List<String> color(List<String> input) {
        return input.stream().map(Text::color).toList();
    }

    public static String strip(String input) {
        return ChatColor.stripColor(color(input));
    }

    public static Component mm(String input) {
        if (input == null) {
            return Component.empty();
        }
        input = legacyToMini(input);
        if (input.contains("<") && input.contains(">")) {
            try {
                return MINI.deserialize(input);
            } catch (RuntimeException ignored) {
                return Component.text(stripMiniTags(input));
            }
        }
        return LEGACY.deserialize(input);
    }

    public static Component chat(String input) {
        return mm(smallCapsMini(legacyToMini(input)));
    }

    public static List<Component> mm(List<String> input) {
        return input.stream().map(Text::mm).toList();
    }

    public static String smallCapsMini(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(input.length());
        boolean inTag = false;
        for (char character : input.toCharArray()) {
            if (character == '<') {
                inTag = true;
                builder.append(character);
                continue;
            }
            if (character == '>') {
                inTag = false;
                builder.append(character);
                continue;
            }
            builder.append(inTag ? character : smallCaps(character));
        }
        return builder.toString();
    }

    private static String smallCaps(char character) {
        return switch (Character.toLowerCase(character)) {
            case 'a' -> "\u1d00";
            case 'b' -> "\u0299";
            case 'c' -> "\u1d04";
            case 'd' -> "\u1d05";
            case 'e' -> "\u1d07";
            case 'f' -> "\u0493";
            case 'g' -> "\u0262";
            case 'h' -> "\u029c";
            case 'i' -> "\u026a";
            case 'j' -> "\u1d0a";
            case 'k' -> "\u1d0b";
            case 'l' -> "\u029f";
            case 'm' -> "\u1d0d";
            case 'n' -> "\u0274";
            case 'o' -> "\u1d0f";
            case 'p' -> "\u1d18";
            case 'q' -> "\u01eb";
            case 'r' -> "\u0280";
            case 's', '\u00df' -> "\ua731";
            case 't' -> "\u1d1b";
            case 'u' -> "\u1d1c";
            case 'v' -> "\u1d20";
            case 'w' -> "\u1d21";
            case 'x' -> "x";
            case 'y' -> "\u028f";
            case 'z' -> "\u1d22";
            case '\u00e4' -> "\u00e4";
            case '\u00f6' -> "\u00f6";
            case '\u00fc' -> "\u00fc";
            default -> String.valueOf(character);
        };
    }

    private static String legacyToMini(String input) {
        String legacy = input.replace('\u00a7', '&');
        StringBuilder builder = new StringBuilder(legacy.length());
        for (int index = 0; index < legacy.length(); index++) {
            char character = legacy.charAt(index);
            if (character != '&' || index + 1 >= legacy.length()) {
                builder.append(character);
                continue;
            }
            char code = Character.toLowerCase(legacy.charAt(index + 1));
            if (code == 'x' && hasLegacyHex(legacy, index)) {
                builder.append("<#");
                for (int hexIndex = index + 3; hexIndex <= index + 13; hexIndex += 2) {
                    builder.append(legacy.charAt(hexIndex));
                }
                builder.append(">");
                index += 13;
                continue;
            }
            String tag = legacyTag(code);
            if (tag == null) {
                builder.append(character);
                continue;
            }
            builder.append(tag);
            index++;
        }
        return builder.toString();
    }

    private static boolean hasLegacyHex(String input, int start) {
        if (start + 13 >= input.length()) {
            return false;
        }
        for (int index = start + 2; index <= start + 12; index += 2) {
            if (input.charAt(index) != '&' || !isHex(input.charAt(index + 1))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHex(char character) {
        char lower = Character.toLowerCase(character);
        return (lower >= '0' && lower <= '9') || (lower >= 'a' && lower <= 'f');
    }

    private static String legacyTag(char code) {
        return switch (code) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'l' -> "<bold>";
            case 'o' -> "<italic>";
            case 'n' -> "<underlined>";
            case 'm' -> "<strikethrough>";
            case 'k' -> "<obfuscated>";
            case 'r' -> "<reset>";
            default -> null;
        };
    }

    private static String stripMiniTags(String input) {
        return PlainTextComponentSerializer.plainText().serialize(LEGACY.deserialize(input.replaceAll("<[^>]+>", "")));
    }
}


