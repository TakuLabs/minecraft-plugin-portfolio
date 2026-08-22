package de.otaku.novaguard.util;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public final class IpNormalizer {
    private IpNormalizer() {
    }

    public static Optional<String> normalize(InetSocketAddress address) {
        if (address == null || address.getAddress() == null) {
            return Optional.empty();
        }
        return Optional.of(normalize(address.getAddress()));
    }

    public static Optional<String> normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String value = raw.trim();
        if (value.startsWith("[") && value.contains("]")) {
            value = value.substring(1, value.indexOf(']'));
        } else if (value.chars().filter(c -> c == ':').count() == 1 && value.contains(".")) {
            value = value.substring(0, value.indexOf(':'));
        }
        int zone = value.indexOf('%');
        if (zone >= 0) {
            value = value.substring(0, zone);
        }
        if (!looksLikeLiteralAddress(value)) {
            return Optional.empty();
        }
        try {
            return Optional.of(normalize(InetAddress.getByName(value)));
        } catch (UnknownHostException ex) {
            return Optional.empty();
        }
    }

    public static String normalize(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (address instanceof Inet6Address && isIpv4Mapped(bytes)) {
            byte[] mapped = Arrays.copyOfRange(bytes, 12, 16);
            try {
                return InetAddress.getByAddress(mapped).getHostAddress();
            } catch (UnknownHostException ignored) {
                return address.getHostAddress();
            }
        }
        if (address instanceof Inet4Address) {
            return address.getHostAddress();
        }
        return address.getHostAddress().toLowerCase(Locale.ROOT);
    }

    private static boolean isIpv4Mapped(byte[] bytes) {
        if (bytes.length != 16) {
            return false;
        }
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return bytes[10] == (byte) 0xff && bytes[11] == (byte) 0xff;
    }

    private static boolean looksLikeLiteralAddress(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean allowed = (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F')
                    || c == '.'
                    || c == ':';
            if (!allowed) {
                return false;
            }
        }
        return value.contains(".") || value.contains(":");
    }
}
