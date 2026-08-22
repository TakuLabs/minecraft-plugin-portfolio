package de.otaku.novaguard.packet;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PacketTypeRegistryTest {
    @Test
    void supportedInboundPacketTypeIsRegistered() {
        TestPacket movement = client("POSITION");
        List<TestPacket> registered = PacketTypeRegistry.filterCandidates(List.of(movement), TestPacket::inbound, TestPacket::supported);
        assertEquals(List.of(movement), registered);
    }

    @Test
    void unsupportedOptionalPacketTypeIsSkippedWithoutDisablingOthers() {
        TestPacket supported = client("CHAT");
        TestPacket unsupported = new TestPacket("CHAT_PREVIEW", true, false);
        List<TestPacket> registered = PacketTypeRegistry.filterCandidates(List.of(supported, unsupported), TestPacket::inbound, TestPacket::supported);
        assertEquals(List.of(supported), registered);
    }

    @Test
    void onlyClientToServerPlayPacketsAreRegistered() {
        TestPacket inbound = client("USE_ITEM");
        TestPacket outbound = new TestPacket("CUSTOM_PAYLOAD", false, true);
        List<TestPacket> registered = PacketTypeRegistry.filterCandidates(List.of(inbound, outbound), TestPacket::inbound, TestPacket::supported);
        assertEquals(List.of(inbound), registered);
    }

    @Test
    void oldProblematicAliasesAreSkippedWhenUnsupported() {
        Set<String> unsupported = Set.of("TRANSACTION", "FLYING", "CHAT_PREVIEW");
        List<TestPacket> registered = PacketTypeRegistry.filterCandidates(
                List.of(client("TRANSACTION"), client("FLYING"), client("CHAT_PREVIEW"), client("CHAT")),
                TestPacket::inbound,
                type -> !unsupported.contains(type.name())
        );
        assertEquals(1, registered.size());
        assertEquals("CHAT", registered.getFirst().name());
    }

    @Test
    void movementPacketsClassifyWithoutLegacyFlyingAlias() {
        assertEquals(PacketCategory.MOVEMENT, PacketClassifier.categoryName("POSITION"));
        assertEquals(PacketCategory.MOVEMENT, PacketClassifier.categoryName("POSITION_LOOK"));
        assertEquals(PacketCategory.MOVEMENT, PacketClassifier.categoryName("LOOK"));
        assertEquals(PacketCategory.MOVEMENT, PacketClassifier.categoryName("STEER_VEHICLE"));
    }

    @Test
    void packetWeightsRemainMappedByCategory() {
        var weights = new de.otaku.novaguard.config.PacketWeights(1.0, 1.5, 1.5, 2.0, 1.5, 3.0, 1.0);
        assertEquals(1.0, PacketClassifier.weight(PacketCategory.MOVEMENT, weights));
        assertEquals(3.0, PacketClassifier.weight(PacketCategory.CUSTOM_PAYLOAD, weights));
    }

    private static TestPacket client(String name) {
        return new TestPacket(name, true, true);
    }

    private record TestPacket(String name, boolean inbound, boolean supported) {
    }
}
