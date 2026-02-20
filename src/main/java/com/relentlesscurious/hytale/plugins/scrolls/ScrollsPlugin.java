package com.relentlesscurious.hytale.plugins.scrolls;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import com.relentlesscurious.hytale.plugins.scrolls.config.MainConfig;
import com.relentlesscurious.hytale.plugins.scrolls.config.ConfigManager;
import com.relentlesscurious.hytale.plugins.scrolls.data.HomeStorage;
import com.relentlesscurious.hytale.plugins.scrolls.util.RecipeOverrideUtil;
import com.relentlesscurious.hytale.plugins.scrolls.random.RandomTeleportScrollListener;
import com.relentlesscurious.hytale.plugins.scrolls.teleport.FriendTeleportService;
import com.relentlesscurious.hytale.plugins.scrolls.teleport.FriendTeleportScrollListener;

import javax.annotation.Nonnull;
import java.nio.file.Path;

/**
 * ScrollsPlugin for Hytale.
 * Baseline plugin shell for Phase Two implementation.
 */
public class ScrollsPlugin extends JavaPlugin {

  private final com.relentlesscurious.hytale.plugins.scrolls.home.HomeTeleportService homeTeleportService = new com.relentlesscurious.hytale.plugins.scrolls.home.HomeTeleportService();
  private com.relentlesscurious.hytale.plugins.scrolls.home.SpawnScrollListener spawnScrollListener;
  private com.relentlesscurious.hytale.plugins.scrolls.home.HomeScrollListener homeScrollListener;
  private com.relentlesscurious.hytale.plugins.scrolls.home.HomeBindingScrollListener homeBindingScrollListener;
  private HomeStorage homeStorage;
  private ConfigManager configManager;
  private MainConfig config;
  private boolean recipesApplied = false;

  private RandomTeleportScrollListener randomTeleportScrollListener;
  private final FriendTeleportService friendTeleportService = new FriendTeleportService();
  private FriendTeleportScrollListener friendTeleportScrollListener;

  public ScrollsPlugin(@Nonnull JavaPluginInit init) {
    super(init);
  }

  @Override
  @SuppressWarnings("deprecation")
  protected void setup() {
    getLogger().atInfo().log("Scrolls setup() called.");

    // Initialize storage and config
    homeStorage = new HomeStorage(Path.of("data"));
    configManager = new ConfigManager(this);
    configManager.loadConfig();
    config = configManager.getConfig();

    // Register commands
    // SetHomeCommand replaced by Scroll of Home Binding
    getCommandRegistry()
        .registerCommand(new com.relentlesscurious.hytale.plugins.scrolls.commands.HomeCommand(homeStorage));

    // Initialize Listeners
    spawnScrollListener = new com.relentlesscurious.hytale.plugins.scrolls.home.SpawnScrollListener(
        homeTeleportService,
        config.spawnScroll,
        getLogger());

    homeScrollListener = new com.relentlesscurious.hytale.plugins.scrolls.home.HomeScrollListener(
        homeStorage,
        config.homeScroll,
        getLogger());

    homeBindingScrollListener = new com.relentlesscurious.hytale.plugins.scrolls.home.HomeBindingScrollListener(
        homeStorage,
        config.homeBindingScroll,
        getLogger());

    randomTeleportScrollListener = new RandomTeleportScrollListener(config.randomTeleportScroll, config.randomTeleportScroll, getLogger());

    friendTeleportScrollListener = new FriendTeleportScrollListener(friendTeleportService, config.friendTeleportScroll, getLogger());

    // Register Event Listeners
    getEventRegistry().registerGlobal(
        com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent.class,
        (java.util.function.Consumer<com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent>) event -> {
          spawnScrollListener.onPlayerMouseButton(event);
          homeScrollListener.onPlayerMouseButton(event);
          randomTeleportScrollListener.onPlayerMouseButton(event);
          friendTeleportScrollListener.onPlayerMouseButton(event);
        });

    getEventRegistry().registerGlobal(
        com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent.class,
        (java.util.function.Consumer<com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent>) event -> {
          spawnScrollListener.onPlayerInteract(event);
          homeScrollListener.onPlayerInteract(event);
          randomTeleportScrollListener.onPlayerInteract(event);
          friendTeleportScrollListener.onPlayerInteract(event);
        });

    getEventRegistry().registerGlobal(
        com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent.class,
        (java.util.function.Consumer<com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent>) event -> {
          if (!recipesApplied) {
            getLogger().atInfo().log("First player ready. Applying recipe overrides.");
            applyRecipeOverrides();
            recipesApplied = true;
          }
          getLogger().atInfo().log("PlayerReadyEvent received for %s", event.getPlayer());
          System.out.println("[Scrolls] PlayerReadyEvent received: " + event.getPlayer());
          friendTeleportScrollListener.onPlayerReady(event.getPlayer());
        });

    getEventRegistry().registerGlobal(
        com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent.class,
        (java.util.function.Consumer<com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent>) event -> {
          friendTeleportScrollListener.onPlayerLogout(event.getPlayerRef().getUuid());
        });

    // Register Interactions
    com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction
        .registerCustomPageSupplier(
            this,
            ScrollsPlugin.class,
            "Scroll_Spawn_Use",
            (com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction.CustomPageSupplier) (
                entityRef, accessor, playerRef, context) -> {
              spawnScrollListener.handleUse(playerRef, context.getCommandBuffer());
              return spawnScrollListener.buildNoopPage(playerRef);
            });

    com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction
        .registerCustomPageSupplier(
            this,
            ScrollsPlugin.class,
            "Scroll_Home_Use",
            (com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction.CustomPageSupplier) (
                entityRef, accessor, playerRef, context) -> {
              homeScrollListener.handleUse(playerRef, context.getCommandBuffer());
              return homeScrollListener.buildNoopPage(playerRef);
            });

    com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction
        .registerCustomPageSupplier(
            this,
            ScrollsPlugin.class,
            "Scroll_Home_Binding_Use",
            (com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction.CustomPageSupplier) (
                entityRef, accessor, playerRef, context) -> {
              homeBindingScrollListener.handleUse(entityRef, accessor, playerRef);
              return homeBindingScrollListener.buildNoopPage(playerRef);
            });

    com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction
        .registerCustomPageSupplier(
            this,
            ScrollsPlugin.class,
            "Scroll_Random_Teleport_Use",
            (com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction.CustomPageSupplier) (
                entityRef, accessor, playerRef, context) -> {
              randomTeleportScrollListener.handleUse(playerRef, context.getCommandBuffer());
              return randomTeleportScrollListener.buildNoopPage(playerRef);
            });

    com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction
        .registerCustomPageSupplier(
            this,
            ScrollsPlugin.class,
            "Scroll_Friend_Teleport_Use",
            (com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction.CustomPageSupplier) (
                entityRef, accessor, playerRef, context) -> {
              return friendTeleportScrollListener.buildFriendTeleportPage(playerRef);
            });

    getLogger().atInfo().log("Scroll spawn interaction hook registered.");
    getLogger().atInfo().log("Scroll home interaction hook registered.");
    getLogger().atInfo().log("Random teleport interaction hook registered.");
    System.out.println("[Scrolls] Hooks registered.");

    getLogger().atInfo().log("Scrolls event listeners registered.");
    System.out.println("[Scrolls] Event listeners registered.");
    getLogger().atInfo().log("Home teleport baseline initialized and listener registered.");

    getLogger().atInfo().log("Scrolls plugin setup complete.");
  }

  private void applyRecipeOverrides() {
    getLogger().atInfo().log("Starting applyRecipeOverrides()");
    applySingleRecipeOverride("scribes:Scroll_Spawn", config.spawnScroll);
    applySingleRecipeOverride("scribes:Scroll_Home", config.homeScroll);
    applySingleRecipeOverride("scribes:Scroll_Home_Binding", config.homeBindingScroll);
    applySingleRecipeOverride("scribes:Scroll_Random_Teleport", config.randomTeleportScroll);
    applySingleRecipeOverride("scribes:Scroll_Friend_Teleport", config.friendTeleportScroll);
  }

  private void applySingleRecipeOverride(String itemId, com.relentlesscurious.hytale.plugins.scrolls.config.BaseScrollConfig config) {
    getLogger().atInfo().log("Applying recipe override for: %s", itemId);
    if (!config.craftingEnabled) {
      RecipeOverrideUtil.disableCrafting(itemId, getLogger());
    } else {
      RecipeOverrideUtil.applyRecipe(itemId, config, getLogger());
    }
  }

  @Override
  protected void start() {
    getLogger().atInfo().log("Scrolls start() called.");
    // Delay applying recipes to ensure Assets are loaded
    java.util.concurrent.CompletableFuture.delayedExecutor(5, java.util.concurrent.TimeUnit.SECONDS).execute(() -> {
      if (!recipesApplied) {
        getLogger().atInfo().log("Applying recipe overrides via start-up delay.");
        applyRecipeOverrides();
        recipesApplied = true;
      }
    });
  }

  @Override
  protected void shutdown() {
    getLogger().atInfo().log("Scrolls plugin shutdown complete.");
  }
}
