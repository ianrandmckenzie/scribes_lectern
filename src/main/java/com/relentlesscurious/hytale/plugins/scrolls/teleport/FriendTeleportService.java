package com.relentlesscurious.hytale.plugins.scrolls.teleport;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manages teleport requests between friends.
 * Requests expire after 60 seconds.
 */
public class FriendTeleportService {

  private static final long EXPIRATION_SECONDS = 60;
  private final Map<UUID, List<TeleportRequest>> pendingRequests = new HashMap<>();

  public void addRequest(UUID to, UUID from, String senderName) {
    pendingRequests.computeIfAbsent(to, k -> new ArrayList<>())
        .add(new TeleportRequest(from, senderName, Instant.now()));
  }

  public List<TeleportRequest> getRequestsFor(UUID to) {
    List<TeleportRequest> requests = pendingRequests.get(to);
    if (requests == null) return new ArrayList<>();

    // Remove expired and return remaining requests
    requests.removeIf(TeleportRequest::isExpired);
    return new ArrayList<>(requests);
  }

  public Optional<UUID> getPendingRequest(UUID to) {
    List<TeleportRequest> requests = getRequestsFor(to);
    return requests.isEmpty() ? Optional.empty() : Optional.of(requests.get(requests.size() - 1).from());
  }

  public void clearRequest(UUID to, UUID from) {
    List<TeleportRequest> requests = pendingRequests.get(to);
    if (requests != null) {
      requests.removeIf(req -> req.from().equals(from));
    }
  }

  public void clearAllRequests(UUID to) {
    pendingRequests.remove(to);
  }

  public record TeleportRequest(UUID from, String senderName, Instant timestamp) {
    public boolean isExpired() {
      return Instant.now().isAfter(timestamp.plusSeconds(EXPIRATION_SECONDS));
    }

    public UUID senderUuid() { return from; }
  }
}
