package de.otaku.novaguard.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KickScreenRendererTest {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();

    @Test
    void replacesPercentPlaceholdersInIpLimitKickScreen() {
        Component component = KickScreenRenderer.render(
                miniMessage,
                List.of("<red>Zu viele Accounts</red>", "Maximal erlaubt: <dark_purple>%max_accounts%</dark_purple>"),
                Map.of("max_accounts", "2"),
                "fallback",
                ignored -> { }
        );

        String text = plain.serialize(component);
        assertTrue(text.contains("Maximal erlaubt: 2"));
        assertFalse(text.contains("%max_accounts%"));
    }

    @Test
    void ipLimitKickScreenDoesNotContainReasonField() {
        Component component = KickScreenRenderer.render(
                miniMessage,
                List.of("Zu viele Accounts", "Maximal erlaubt: %max_accounts%"),
                Map.of("max_accounts", "2"),
                "fallback",
                ignored -> { }
        );

        assertFalse(plain.serialize(component).contains("Grund:"));
    }

    @Test
    void blacklistWithReasonShowsReason() {
        Component component = KickScreenRenderer.render(
                miniMessage,
                List.of("Zugriff verweigert", "Grund: <white>%reason%</white>"),
                Map.of("reason", "Ban-Umgehung"),
                "fallback",
                ignored -> { }
        );

        assertTrue(plain.serialize(component).contains("Grund: Ban-Umgehung"));
    }

    @Test
    void blacklistWithoutReasonHidesReasonLine() {
        Component component = KickScreenRenderer.render(
                miniMessage,
                List.of("Zugriff verweigert", "Grund: <white>%reason%</white>", "Ende"),
                Map.of(),
                "fallback",
                ignored -> { }
        );

        String text = plain.serialize(component);
        assertFalse(text.contains("Grund:"));
        assertTrue(text.contains("Zugriff verweigert"));
        assertTrue(text.contains("Ende"));
    }

    @Test
    void utf8TextIsPreserved() {
        Component component = KickScreenRenderer.render(
                miniMessage,
                List.of("<white>Über deine IP-Adresse</white>"),
                Map.of(),
                "fallback",
                ignored -> { }
        );

        assertEquals("Über deine IP-Adresse", plain.serialize(component));
    }

    @Test
    void numericPlaceholderInsideMiniMessageTagIsRendered() {
        Component component = KickScreenRenderer.render(
                miniMessage,
                List.of("<gray>Maximal erlaubt: <dark_purple>%max_accounts%</dark_purple></gray>"),
                Map.of("max_accounts", "2"),
                "fallback",
                ignored -> { }
        );

        assertEquals("Maximal erlaubt: 2", plain.serialize(component));
    }

    @Test
    void placeholderValuesCannotInjectMiniMessageTags() {
        Component component = KickScreenRenderer.render(
                miniMessage,
                List.of("Grund: <white>%reason%</white>"),
                Map.of("reason", "<red>Test"),
                "fallback",
                ignored -> { }
        );

        assertEquals("Grund: <red>Test", plain.serialize(component));
    }

    @Test
    void playerNameIsEscaped() {
        Component component = KickScreenRenderer.render(
                miniMessage,
                List.of("Spieler: <white>%player%</white>"),
                Map.of("player", "<click:run_command:/op Test>Player"),
                "fallback",
                ignored -> { }
        );

        assertEquals("Spieler: <click:run_command:/op Test>Player", plain.serialize(component));
    }

    @Test
    void allProvidedNovaGuardPlaceholdersAreReplaced() {
        Component component = KickScreenRenderer.render(
                miniMessage,
                List.of("%player% %uuid% %reason% %max_accounts% %current_accounts% %violations%"),
                Map.of(
                        "player", "TestPlayer",
                        "uuid", "00000000-0000-0000-0000-000000000000",
                        "reason", "Ban-Umgehung",
                        "max_accounts", "2",
                        "current_accounts", "3",
                        "violations", "4"
                ),
                "fallback",
                ignored -> { }
        );

        String text = plain.serialize(component);
        assertFalse(text.contains("%player%"));
        assertFalse(text.contains("%reason%"));
        assertFalse(text.contains("%max_accounts%"));
        assertTrue(text.contains("TestPlayer"));
        assertTrue(text.contains("4"));
    }

    @Test
    void validMiniMessageTagsStillWork() {
        Component component = KickScreenRenderer.render(
                miniMessage,
                List.of("<red><bold>Zu viele Accounts</bold></red>"),
                Map.of(),
                "fallback",
                ignored -> { }
        );

        assertEquals("Zu viele Accounts", plain.serialize(component));
    }

    @Test
    void invalidMiniMessageUsesSafeFallback() {
        List<String> warnings = new ArrayList<>();
        Component component = KickScreenRenderer.render(
                miniMessage,
                List.of("<red"),
                Map.of(),
                "ExampleSMP » Deine Verbindung wurde abgelehnt.",
                warnings::add
        );

        assertEquals("ExampleSMP » Deine Verbindung wurde abgelehnt.", plain.serialize(component));
        assertFalse(warnings.isEmpty());
    }
}
