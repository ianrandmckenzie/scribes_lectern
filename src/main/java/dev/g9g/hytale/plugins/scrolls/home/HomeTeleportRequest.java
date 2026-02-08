package com.relentlesscurious.hytale.plugins.scrolls.home;

/**
 * Input payload for resolving a home teleport target.
 * Bed and spawn locations are nullable; resolution will prefer bed when
 * present.
 */
public record HomeTeleportRequest(
    String playerName,
    HomeLocation bedLocation,
    HomeLocation spawnLocation) {
}
