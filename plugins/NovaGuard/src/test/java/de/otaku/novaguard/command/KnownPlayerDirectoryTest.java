package de.otaku.novaguard.command;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KnownPlayerDirectoryTest {
    @Test
    void suggestsKnownOfflinePlayersByPrefix() {
        KnownPlayerDirectory directory = new KnownPlayerDirectory();
        directory.remember(UUID.randomUUID(), "MoonBlushed", Instant.parse("2026-01-01T00:00:00Z"));
        directory.remember(UUID.randomUUID(), "TestPlayer", Instant.parse("2026-01-01T00:00:00Z"));

        assertEquals(List.of("MoonBlushed"), directory.suggest("Moo"));
    }

    @Test
    void matchingIsCaseInsensitive() {
        KnownPlayerDirectory directory = new KnownPlayerDirectory();
        directory.remember(UUID.randomUUID(), "MoonBlushed");

        assertEquals(List.of("MoonBlushed"), directory.suggest("moon"));
    }

    @Test
    void unknownPlayersAreNotInvented() {
        KnownPlayerDirectory directory = new KnownPlayerDirectory();

        assertEquals(List.of(), directory.suggest("Nobody"));
        assertNull(directory.find("Nobody"));
    }

    @Test
    void nameChangeKeepsUuidAndPrefersLatestName() {
        KnownPlayerDirectory directory = new KnownPlayerDirectory();
        UUID uuid = UUID.randomUUID();

        directory.remember(uuid, "OldName", Instant.parse("2026-01-01T00:00:00Z"));
        directory.remember(uuid, "NewName", Instant.parse("2026-01-02T00:00:00Z"));

        assertEquals(uuid, directory.find("NewName").uuid());
        assertNull(directory.find("OldName"));
        assertEquals(List.of("NewName"), directory.suggest("New"));
        assertNotEquals(List.of("OldName"), directory.suggest("Old"));
    }

    @Test
    void uuidPrefixMatchingIsOptionalAndBounded() {
        KnownPlayerDirectory directory = new KnownPlayerDirectory();
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        directory.remember(uuid, null);

        assertEquals(List.of(uuid.toString()), directory.suggest("550e"));
    }
}
