package com.relentlesscurious.hytale.plugins.scrolls.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class ConfigManager {
  private final JavaPlugin plugin;
  private final Gson gson;
  private MainConfig config;

  public ConfigManager(JavaPlugin plugin) {
    this.plugin = plugin;
    this.gson = new GsonBuilder().setPrettyPrinting().create();
  }

  public void loadConfig() {
    File configDir = new File("config");
    if (!configDir.exists()) {
      configDir.mkdirs();
    }
    File configFile = new File(configDir, "scribes_lectern.json");

    if (!configFile.exists()) {
      config = new MainConfig();
      // Set some defaults with recipes
      setupDefaultRecipes();
      saveConfig(configFile);
      plugin.getLogger().atInfo().log("Created default configuration at " + configFile.getAbsolutePath());
    } else {
      try (FileReader reader = new FileReader(configFile)) {
        config = gson.fromJson(reader, MainConfig.class);
        if (config == null) {
            plugin.getLogger().atWarning().log("Loaded config was null, using defaults.");
            config = new MainConfig();
        } else {
            plugin.getLogger().atInfo().log("Loaded scribes_lectern config successfully.");
        }
      } catch (Exception e) {
        plugin.getLogger().atSevere().withCause(e).log("Failed to load scribes_lectern.json - possibly invalid JSON syntax.");
        config = new MainConfig();
      }
    }
  }

  private void setupDefaultRecipes() {
    // Example: Home Scroll default recipe
    config.homeScroll.recipe = new ArrayList<>();
    RecipeIngredientConfig fibre = new RecipeIngredientConfig();
    fibre.itemId = "Ingredient_Fibre";
    fibre.quantity = 10;
    config.homeScroll.recipe.add(fibre);

    RecipeIngredientConfig sap = new RecipeIngredientConfig();
    sap.itemId = "Ingredient_Tree_Sap";
    sap.quantity = 5;
    config.homeScroll.recipe.add(sap);

    // Random Teleport default recipe
    config.randomTeleportScroll.recipe = new ArrayList<>();
    RecipeIngredientConfig silk = new RecipeIngredientConfig();
    silk.itemId = "Ingredient_Silk";
    silk.quantity = 4;
    config.randomTeleportScroll.recipe.add(silk);
  }

  public void saveConfig(File file) {
    try (FileWriter writer = new FileWriter(file)) {
      gson.toJson(config, writer);
    } catch (IOException e) {
      plugin.getLogger().atSevere().withCause(e).log("Failed to save scribes_lectern.json");
    }
  }

  public MainConfig getConfig() {
    return config;
  }
}
