package com.relentlesscurious.hytale.plugins.scrolls.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HomeStorage {
  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private final Path dataFolder;
  private final Map<UUID, PlayerHomeData> cache = new ConcurrentHashMap<>();

  public HomeStorage(Path dataFolder) {
    this.dataFolder = dataFolder;
    try {
      Files.createDirectories(dataFolder);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public PlayerHomeData getPlayerHomeData(UUID uuid) {
    if (cache.containsKey(uuid)) {
      return cache.get(uuid);
    }

    PlayerHomeData data = loadFromDisk(uuid);
    if (data == null) {
      data = new PlayerHomeData(uuid);
    }
    cache.put(uuid, data);
    return data;
  }

  private PlayerHomeData loadFromDisk(UUID uuid) {
    Path filePath = dataFolder.resolve(uuid.toString() + ".json");
    if (!Files.exists(filePath)) {
      return null;
    }

    try (Reader reader = Files.newBufferedReader(filePath)) {
      PlayerHomeData data = gson.fromJson(reader, PlayerHomeData.class);
      if (data.getUuid() == null) {
        data.setUuid(uuid);
      }
      return data;
    } catch (IOException e) {
      System.err.println("Failed to load player home data for " + uuid);
      e.printStackTrace();
      return null;
    }
  }

  public void savePlayerHomeData(UUID uuid) {
    PlayerHomeData data = cache.get(uuid);
    if (data == null)
      return;

    Path filePath = dataFolder.resolve(uuid.toString() + ".json");
    try (Writer writer = Files.newBufferedWriter(filePath)) {
      gson.toJson(data, writer);
    } catch (IOException e) {
      System.err.println("Failed to save player home data for " + uuid);
      e.printStackTrace();
    }
  }
}
