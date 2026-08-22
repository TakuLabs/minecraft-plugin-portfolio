package de.otaku.novaguard.command;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandCatalogTest {
    @Test
    void helpShowsOnlyPermittedCommands() {
        Set<String> permissions = Set.of("novaguard.help", "novaguard.packetinfo");

        List<String> visible = CommandCatalog.visibleRoots(permissions::contains, "");

        assertEquals(List.of("help", "packetinfo"), visible);
    }

    @Test
    void packetInfoIsRegisteredInCatalog() {
        CommandCatalog.CommandMeta packetInfo = CommandCatalog.find("packetinfo");

        assertNotNull(packetInfo);
        assertEquals("novaguard.packetinfo", packetInfo.permission());
        assertEquals("/novaguard packetinfo <Spieler>", packetInfo.syntax());
    }

    @Test
    void noGhostCommandsInCatalog() {
        Set<String> handledCommands = Set.of("help", "reload", "status", "packetinfo", "whitelist",
                "blacklist", "ipblacklist", "iplimit", "perf");

        for (CommandCatalog.CommandMeta command : CommandCatalog.commands()) {
            assertTrue(handledCommands.contains(command.name()), command.name());
            assertNotNull(command.permission(), command.name());
            assertNotNull(command.descriptionKey(), command.name());
        }
    }

    @Test
    void rootCompletionUsesCaseInsensitivePrefixMatching() {
        List<String> visible = CommandCatalog.visibleRoots(permission -> true, "PaC");

        assertEquals(List.of("packetinfo"), visible);
    }
}
