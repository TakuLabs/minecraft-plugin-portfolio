package de.otaku.novaguard.util;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

public final class PermissionUtil {
    private PermissionUtil() {
    }

    public static boolean hasExactPermission(Player player, String permission) {
        for (PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            if (info.getPermission().equalsIgnoreCase(permission) && info.getValue()) {
                return true;
            }
        }
        return false;
    }
}
