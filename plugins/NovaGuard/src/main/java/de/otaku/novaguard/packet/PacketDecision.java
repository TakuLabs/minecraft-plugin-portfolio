package de.otaku.novaguard.packet;

public record PacketDecision(
        boolean violation,
        boolean disconnect,
        double tokens,
        double ratePerSecond,
        int violations
) {
}
