package com.relentlesscurious.hytale.plugins.scrolls.teleport;

import com.hypixel.hytale.server.core.entity.entities.Player;
import java.lang.reflect.Method;

public class ReflectPlayer {
    /**
     * Attempts to resolve a human-readable name for a player using reflection.
     * Falls back to a sanitized version of toString() or "Unknown" if all else fails.
     */
    public static String resolveName(Player player) {
        if (player == null) return "Unknown";

        try {
            // Priority 1: SDK getName() method
            Method getName = player.getClass().getMethod("getName");
            Object name = getName.invoke(player);
            if (name != null) {
                String nameStr = name.toString();
                if (!nameStr.isEmpty() && !nameStr.equalsIgnoreCase("null")) {
                    return nameStr;
                }
            }
        } catch (Exception ignored) {}

        try {
            // Priority 2: Sanitize toString() if it looks like "Player{uuid=...}"
            String raw = player.toString();
            if (raw.contains("uuid=")) {
                // If it's the raw object string, try to extract just the short part or keep it as is
                // but usually we want to avoid this in chat.
                // However, if we can't find a name, showing nothing is worse.
                return "a friend"; // Better than object data in chat
            }
            return raw;
        } catch (Exception e) {
            return "Player";
        }
    }
}
