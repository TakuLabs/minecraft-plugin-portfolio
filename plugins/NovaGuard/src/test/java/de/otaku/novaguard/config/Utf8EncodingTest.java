package de.otaku.novaguard.config;

import de.otaku.novaguard.util.YamlFiles;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Utf8EncodingTest {
    private static final String REQUIRED_UTF8 = "äöüÄÖÜß»✦━━━━━━━━";

    @TempDir
    Path tempDir;

    @Test
    void messagesYamlContainsRealUtf8Characters() throws Exception {
        String content = Files.readString(Path.of("src/main/resources/messages.yml"), StandardCharsets.UTF_8);
        assertTrue(content.contains("Über"));
        assertTrue(content.contains("Ungewöhnlicher Netzwerkverkehr"));
        assertTrue(content.contains("ExampleSMP</bold></dark_purple> <dark_gray>»</dark_gray>"));
        assertTrue(content.contains("✦ EXAMPLESMP ✦"));
        assertTrue(content.contains("━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        assertFalse(content.contains("\\u00"));
        assertFalse(content.contains("Ã"));
        assertFalse(content.contains("�"));
    }

    @Test
    void configYamlLoadsAsUtf8() throws Exception {
        YamlConfiguration config = YamlFiles.load(Path.of("src/main/resources/config.yml"));
        assertTrue(config.getBoolean("ip-limit.enabled"));
        assertEquals(15, config.getInt("ip-limit.max-accounts-per-ip"));
        assertEquals("MONITOR", config.getString("packet-protection.mode"));
    }

    @Test
    void kickScreenAndPrefixParseWithUtf8Characters() throws Exception {
        YamlConfiguration messages = YamlFiles.load(Path.of("src/main/resources/messages.yml"));
        MiniMessage miniMessage = MiniMessage.miniMessage();
        PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();

        Component prefix = miniMessage.deserialize(messages.getString("prefix"));
        assertEquals("ExampleSMP »", plain.serialize(prefix));

        Component kick = KickScreenRenderer.render(
                miniMessage,
                messages.getStringList("kick-screens.packet-protection"),
                Map.of("reason", "Ungewöhnlicher Netzwerkverkehr"),
                "fallback",
                ignored -> { }
        );
        String text = plain.serialize(kick);
        assertTrue(text.contains("✦ EXAMPLESMP ✦"));
        assertTrue(text.contains("Ungewöhnlicher Netzwerkverkehr"));
        assertTrue(text.contains("━━━━━━━━"));
    }

    @Test
    void utf8YamlRoundTripPreservesGermanCharacters() throws Exception {
        Path file = tempDir.resolve("utf8.yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("text", REQUIRED_UTF8);
        yaml.set("message", "Die Änderung wurde erfolgreich geprüft und zurück gespeichert.");

        YamlFiles.save(yaml, file);
        byte[] bytes = Files.readAllBytes(file);
        String raw = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(raw.contains(REQUIRED_UTF8));
        assertFalse(raw.contains("Ã"));
        assertFalse(raw.contains("?"));

        YamlConfiguration loaded = YamlFiles.load(file);
        assertEquals(REQUIRED_UTF8, loaded.getString("text"));
        assertEquals("Die Änderung wurde erfolgreich geprüft und zurück gespeichert.", loaded.getString("message"));
    }

    @Test
    void noGermanAsciiUmlautWorkaroundsRemainInVisibleResources() throws Exception {
        List<String> suspicious = List.of(
                "Ueber", "ueber", "fuer", "Fuer", "koennen", "Koennen",
                "moeglich", "Moeglich", "ungewoehnlich", "Ungewoehnlich",
                "hinzugefuegt", "ausgefuehrt", "geprueft", "zurueck",
                "waehrend", "geloescht", "Aenderung"
        );
        String messages = Files.readString(Path.of("src/main/resources/messages.yml"), StandardCharsets.UTF_8);
        for (String token : suspicious) {
            assertFalse(messages.contains(token), "messages.yml contains ASCII umlaut workaround: " + token);
        }
    }
}
