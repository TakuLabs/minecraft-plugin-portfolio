package de.otaku.novaguard.packet;

public record PacketSnapshot(double tokens, double ratePerSecond, int violations) {
}
