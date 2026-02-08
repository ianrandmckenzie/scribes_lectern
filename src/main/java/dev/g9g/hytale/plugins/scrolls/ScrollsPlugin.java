package com.relentlesscurious.hytale.plugins.scrolls;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import com.relentlesscurious.hytale.plugins.scrolls.config.RandomTeleportConfig;
import com.relentlesscurious.hytale.plugins.scrolls.data.HomeStorage;
import com.relentlesscurious.hytale.plugins.scrolls.random.RandomTeleportScrollListener;

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
  private final Config<RandomTeleportConfig> randomTeleportConfigStore = withConfig(RandomTeleportConfig.CODEC);
  private RandomTeleportConfig randomTeleportConfig;
  private RandomTeleportScrollListener randomTeleportScrollListener;

  public ScrollsPlugin(@Nonnull JavaPluginInit init) {
    super(init);
  }

  @Override
  @SuppressWarnings("deprecation")
  protected void setup() {
    getLogger().atInfo().log("Scrolls setup() called.");

    // Initialize storage and config
    homeStorage = new HomeStorage(Path.of("data"));
    randomTeleportConfig = randomTeleportConfigStore.get();
    randomTeleportConfigStore.save();

    // Register commands
    // SetHomeCommand replaced by Scroll of Home Binding
    getCommandRegistry()
        .registerCommand(new com.relentlesscurious.hytale.plugins.scrolls.commands.HomeCommand(homeStorage));

    // Initialize Listeners
    spawnScrollListener = new com.relentlesscurious.hytale.plugins.scrolls.home.SpawnScrollListener(
        homeTeleportService,
        getLogger());

    homeScrollListener = new com.relentlesscurious.hytale.plugins.scrolls.home.HomeScrollListener(
        homeStorage,
        getLogger());

    homeBindingScrollListener = new com.relentlesscurious.hytale.plugins.scrolls.home.HomeBindingScrollListener(
        homeStorage,
        getLogger());

    randomTeleportScrollListener = new RandomTeleportScrollListener(randomTeleportConfig, getLogger());

    // Register Event Listeners
    getEventRegistry().registerGlobal(
        com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent.class,
        (java.util.function.Consumer<com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent>) event -> {
          spawnScrollListener.onPlayerMouseButton(event);
          homeScrollListener.onPlayerMouseButton(event);
          randomTeleportScrollListener.onPlayerMouseButton(event);
        });

    getEventRegistry().registerGlobal(
        com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent.class,
        (java.util.function.Consumer<com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent>) event -> {
          spawnScrollListener.onPlayerInteract(event);
          homeScrollListener.onPlayerInteract(event);
          randomTeleportScrollListener.onPlayerInteract(event);
        });

    getEventRegistry().registerGlobal(
        com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent.class,
        (java.util.function.Consumer<com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent>) event -> {
          getLogger().atInfo().log("PlayerReadyEvent received for %s", event.getPlayer());
          System.out.println("[Scrolls] PlayerReadyEvent received: " + event.getPlayer());
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

    getLogger().atInfo().log("Scroll spawn interaction hook registered.");
    getLogger().atInfo().log("Scroll home interaction hook registered.");
    getLogger().atInfo().log("Random teleport interaction hook registered.");
    System.out.println("[Scrolls] Hooks registered.");

    getLogger().atInfo().log("Scrolls event listeners registered.");
    System.out.println("[Scrolls] Event listeners registered.");
    getLogger().atInfo().log("Home teleport baseline initialized and listener registered.");
    getLogger().atInfo().log("Scrolls plugin setup complete.");
  }

  @Override
  protected void start() {
    getLogger().atInfo().log("Scrolls start() called.");
  }

  @Override
  protected void shutdown() {
    getLogger().atInfo().log("Scrolls plugin shutdown complete.");
  }
}
