package com.relentlesscurious.hytale.plugins.scrolls.home;

/**
 * Baseline home location model for the Scroll of Home Teleport.
 * This is intentionally API-agnostic and will be mapped to Hytale location
 * types later.
 */
public record HomeLocation(String worldId, double x, double y, double z) {
}
