package com.relentlesscurious.hytale.plugins.scrolls.data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerHomeData {
  private UUID uuid;
  private Map<String, StoredLocation> homes = new HashMap<>();

  public PlayerHomeData() {
  }

  public PlayerHomeData(UUID uuid) {
    this.uuid = uuid;
  }

  public UUID getUuid() {
    return uuid;
  }

  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  public void setHome(String name, StoredLocation location) {
    homes.put(name.toLowerCase(), location);
  }

  public StoredLocation getHome(String name) {
    return homes.get(name.toLowerCase());
  }

  public boolean hasHome(String name) {
    return homes.containsKey(name.toLowerCase());
  }

  public void removeHome(String name) {
    homes.remove(name.toLowerCase());
  }

  public Set<String> getHomeNames() {
    return homes.keySet();
  }

  public int getHomeCount() {
    return homes.size();
  }
}
