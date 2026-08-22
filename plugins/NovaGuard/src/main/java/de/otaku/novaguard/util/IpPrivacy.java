package de.otaku.novaguard.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class IpPrivacy {
    private IpPrivacy() {
    }

    public static String display(String normalizedIp, boolean full) {
        if (normalizedIp == null) {
            return "unknown";
        }
        if (full) {
            return normalizedIp;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(normalizedIp.getBytes(StandardCharsets.UTF_8));
            return "ip#" + HexFormat.of().formatHex(hashed, 0, 6);
        } catch (NoSuchAlgorithmException ex) {
            return "ip#masked";
        }
    }
}
