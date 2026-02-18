package com.relentlesscurious.hytale.plugins.scrolls.util;

import com.hypixel.hytale.server.core.entity.entities.Player;
import java.lang.reflect.Method;

public class ReflectPlayer {
    /**
     * Attempts to resolve a human-readable name for a player using reflection.
     * Falls back to a sanitized version of toString() or "Unknown" if all else fails.
     */
    public static String resolveName(Player player) {
        if (player == null) return "Unknown";
        
        // Strategy 1: Direct Method Checks
        String[] methodNames = {"getName", "getDisplayName", "getUsername"};
        for (String methodName : methodNames) {
            String result = tryInvoke(player, methodName);
            if (isValidName(result)) return result;
        }

        // Strategy 2: Check Player Reference/Identity (common in SDK)
        try {
            Method getRef = player.getClass().getMethod("getReference");
            Object ref = getRef.invoke(player);
            if (ref != null) {
                for (String methodName : methodNames) {
                    String result = tryInvoke(ref, methodName);
                    if (isValidName(result)) return result;
                }
            }
        } catch (Exception ignored) {}

        // Strategy 3: Sanitized toString (last resort)
        try {
            String raw = player.toString();
            // If it's a raw Hytale object string like Player{uuid=...}, 
            // the name isn't here, so we return a generic but clean label.
            if (raw.contains("uuid=") || raw.contains("@")) {
                return "Player"; 
            }
            return raw;
        } catch (Exception e) {
            return "Player";
        }
    }

    private static String tryInvoke(Object obj, String methodName) {
        try {
            Method m = obj.getClass().getMethod(methodName);
            Object res = m.invoke(obj);
            return res != null ? res.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isValidName(String name) {
        return name != null && !name.isEmpty() && !name.equalsIgnoreCase("null") && !name.contains("{");
    }
}
