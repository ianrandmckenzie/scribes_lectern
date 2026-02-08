package com.relentlesscurious.hytale.plugins.scrolls.home;

/**
 * Baseline resolver for Scroll of Home Teleport target selection.
 * Prefers bed location when available, otherwise falls back to spawn.
 */
public class HomeTeleportService {

  public HomeTeleportResult resolveTarget(HomeTeleportRequest request) {
    if (request == null) {
      return new HomeTeleportResult(HomeTeleportTargetType.NONE, null);
    }

    if (request.bedLocation() != null) {
      return new HomeTeleportResult(HomeTeleportTargetType.BED, request.bedLocation());
    }

    if (request.spawnLocation() != null) {
      return new HomeTeleportResult(HomeTeleportTargetType.SPAWN, request.spawnLocation());
    }

    return new HomeTeleportResult(HomeTeleportTargetType.NONE, null);
  }
}
