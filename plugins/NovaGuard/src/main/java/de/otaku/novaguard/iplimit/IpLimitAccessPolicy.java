package de.otaku.novaguard.iplimit;

public final class IpLimitAccessPolicy {
    private IpLimitAccessPolicy() {
    }

    public static boolean bypassesIpLimit(boolean uuidWhitelisted, boolean exactPermissionBypass) {
        return uuidWhitelisted || exactPermissionBypass;
    }
}
