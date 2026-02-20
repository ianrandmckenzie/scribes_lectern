package com.relentlesscurious.hytale.plugins.scrolls.home;

import com.relentlesscurious.hytale.plugins.scrolls.util.ReflectPlayer;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.MouseButtonEvent;
import com.hypixel.hytale.protocol.MouseButtonState;
import com.hypixel.hytale.protocol.MouseButtonType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.data.PlayerRespawnPointData;
import com.hypixel.hytale.server.core.entity.entities.player.data.PlayerWorldData;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.teleport.PendingTeleport;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;

import com.relentlesscurious.hytale.plugins.scrolls.config.SpawnScrollConfig;

public class SpawnScrollListener {

  private static final String SCROLL_SPAWN_ITEM_ID = "Scroll_Spawn";

  private final HomeTeleportService homeTeleportService;
  private final SpawnScrollConfig config;
  private final HytaleLogger logger;

  public SpawnScrollListener(HomeTeleportService homeTeleportService, SpawnScrollConfig config, HytaleLogger logger) {
    this.homeTeleportService = homeTeleportService;
    this.config = config;
    this.logger = logger;
  }

  public void onPlayerMouseButton(PlayerMouseButtonEvent event) {
    if (event == null || event.isCancelled()) {
      return;
    }

    System.out.println("[Scrolls] PlayerMouseButtonEvent received: " + event);

    logMouseButtonEvent(event);

    if (!isScrollInteraction(event)) {
      return;
    }

    Item itemInHand = event.getItemInHand();
    String itemId = resolveItemId(itemInHand);

    Player player = event.getPlayer();

    String playerName = resolvePlayerName(player);
    logger.atInfo().log(
        "Home scroll used by %s with itemId=%s mouseButton=%s",
        playerName,
        itemId,
        event.getMouseButton());

    handleUse(player);
    event.setCancelled(true);
  }

  @SuppressWarnings("deprecation")
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event == null || event.isCancelled()) {
      return;
    }

    System.out.println("[Scrolls] PlayerInteractEvent received: " + event);

    ItemStack itemInHand = event.getItemInHand();
    String itemId = itemInHand != null ? itemInHand.getItemId() : null;

    logger.atInfo().log(
        "PlayerInteractEvent: player=%s itemId=%s actionType=%s",
        resolvePlayerName(event.getPlayer()),
        itemId,
        event.getActionType());

    if (event.getActionType() != com.hypixel.hytale.protocol.InteractionType.Primary) {
      return;
    }

    if (!SCROLL_SPAWN_ITEM_ID.equalsIgnoreCase(itemId)) {
      return;
    }

    Player player = event.getPlayer();
    handleUse(player);
    event.setCancelled(true);
  }

  public void handleUse(PlayerRef playerRef) {
    if (playerRef == null) {
      logger.atWarning().log("Home scroll interaction received with null PlayerRef.");
      return;
    }

    Player player = resolvePlayerFromRef(playerRef);
    if (player == null) {
      logger.atWarning().log("Home scroll interaction could not resolve Player from PlayerRef.");
      return;
    }

    handleUse(player, null);
  }

  public void handleUse(PlayerRef playerRef, CommandBuffer<EntityStore> commandBuffer) {
    if (playerRef == null) {
      logger.atWarning().log("Home scroll interaction received with null PlayerRef.");
      return;
    }

    Player player = resolvePlayerFromRef(playerRef);
    if (player == null) {
      logger.atWarning().log("Home scroll interaction could not resolve Player from PlayerRef.");
      return;
    }

    handleUse(player, commandBuffer);
  }

  public CustomUIPage buildNoopPage(PlayerRef playerRef) {
    if (playerRef == null) {
      return null;
    }

    Player player = resolvePlayerFromRef(playerRef);
    if (player == null) {
      return null;
    }

    CustomUIPage page = player.getPageManager().getCustomPage();
    if (page != null) {
      page.setLifetime(CustomPageLifetime.CanDismissOrCloseThroughInteraction);
    }

    return page;
  }

  private void handleUse(Player player) {
    if (player == null) {
      logger.atWarning().log("Spawn scroll handleUse called with null player.");
      return;
    }

    String playerName = resolvePlayerName(player);
    World world = player.getWorld();
    if (world == null) {
      logger.atWarning().log("Spawn scroll used by %s but player has no world.", playerName);
      return;
    }

    logger.atInfo().log(
        "Spawn scroll use start: player=%s world=%s",
        playerName,
        world.getName());

    HomeLocation spawnLocation = resolveSpawnLocation(player, world);

    logger.atInfo().log(
        "Spawn scroll locations for %s: spawn=%s",
        playerName,
        spawnLocation);

    HomeTeleportResult result = homeTeleportService.resolveTarget(
        new HomeTeleportRequest(playerName, null, spawnLocation));

    if (result.targetType() == HomeTeleportTargetType.NONE || result.targetLocation() == null) {
      logger.atWarning().log("Home scroll could not resolve target for %s.", playerName);
      return;
    }

    logger.atInfo().log(
        "Home scroll target resolved for %s: %s -> %s",
        playerName,
        result.targetType(),
        result.targetLocation());

    playEffect(player);
    queueTeleport(player, world, result.targetLocation(), null);
  }

  private void playEffect(Player player) {
    if (player == null || player.getWorld() == null || player.getReference() == null) {
      return;
    }

    String particleId = "Potion_Stamina_Burst";
    logger.atInfo().log("Attempting to play effect '%s' for user %s", particleId, resolvePlayerName(player));

    World world = player.getWorld();
    Ref<EntityStore> ref = player.getReference();
    Store<EntityStore> store = (Store<EntityStore>) ref.getStore();
    if (store == null) {
      return;
    }

    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    if (transform == null) {
      return;
    }

    Vector3d pos = transform.getPosition();
    java.util.Collection<?> recipients = java.util.Collections.emptyList();

    com.hypixel.hytale.protocol.Color color = new com.hypixel.hytale.protocol.Color((byte) 255, (byte) 213, (byte) 64);

    try {
      try {
        java.lang.reflect.Method m = ParticleUtil.class.getMethod("spawnParticle", String.class, Vector3d.class, float.class, float.class, float.class, float.class, com.hypixel.hytale.protocol.Color.class, java.util.Collection.class, Store.class);
        m.invoke(null, particleId, pos, 0f, 0f, 0f, 1f, color, recipients, store);
        return;
      } catch (NoSuchMethodException ignored) {
      }

      try {
        java.lang.reflect.Method m = world.getClass().getMethod("broadcastParticle", String.class, Vector3d.class, int.class);
        m.invoke(world, particleId, pos, 1);
      } catch (NoSuchMethodException ignored) {
      }
    } catch (Exception e) {
      logger.atWarning().withCause(e).log("Error while trying to play effect through reflection");
    }
  }

  private void handleUse(Player player, CommandBuffer<EntityStore> commandBuffer) {
    if (player == null) {
      logger.atWarning().log("Spawn scroll handleUse called with null player.");
      return;
    }

    String playerName = resolvePlayerName(player);
    World world = player.getWorld();
    if (world == null) {
      logger.atWarning().log("Spawn scroll used by %s but player has no world.", playerName);
      return;
    }

    double chargingTime = config.chargingTime;
    if (chargingTime > 0) {
      player.sendMessage(Message.raw("Charging scroll... (" + chargingTime + "s)"));
      java.util.concurrent.CompletableFuture.runAsync(() -> {
        try {
          Thread.sleep((long) (chargingTime * 1000));
        } catch (InterruptedException ignored) {}
      }).thenRun(() -> {
        world.execute(() -> {
          HomeLocation spawnLocation = resolveSpawnLocation(player, world);
          HomeTeleportResult result = homeTeleportService.resolveTarget(
              new HomeTeleportRequest(playerName, null, spawnLocation));

          if (result.targetType() == HomeTeleportTargetType.NONE || result.targetLocation() == null) {
            logger.atWarning().log("Home scroll could not resolve target for %s.", playerName);
            return;
          }
          queueTeleport(player, world, result.targetLocation(), commandBuffer);
        });
      });
      return;
    }

    logger.atInfo().log(
        "Spawn scroll use start: player=%s world=%s",
        playerName,
        world.getName());

    HomeLocation spawnLocation = resolveSpawnLocation(player, world);

    logger.atInfo().log(
        "Spawn scroll locations for %s: spawn=%s",
        playerName,
        spawnLocation);

    HomeTeleportResult result = homeTeleportService.resolveTarget(
        new HomeTeleportRequest(playerName, null, spawnLocation));

    if (result.targetType() == HomeTeleportTargetType.NONE || result.targetLocation() == null) {
      logger.atWarning().log("Home scroll could not resolve target for %s.", playerName);
      return;
    }

    logger.atInfo().log(
        "Home scroll target resolved for %s: %s -> %s",
        playerName,
        result.targetType(),
        result.targetLocation());

    queueTeleport(player, world, result.targetLocation(), commandBuffer);
  }

  private boolean isScrollInteraction(PlayerMouseButtonEvent event) {
    if (!isLeftClick(event.getMouseButton())) {
      return false;
    }

    Item item = event.getItemInHand();
    if (item == null) {
      return false;
    }

    String itemId = resolveItemId(item);
    return SCROLL_SPAWN_ITEM_ID.equalsIgnoreCase(itemId);
  }

  private boolean isLeftClick(MouseButtonEvent mouseButtonEvent) {
    if (mouseButtonEvent == null) {
      return false;
    }

    MouseButtonType type = null;
    MouseButtonState state = null;

    try {
      type = (MouseButtonType) mouseButtonEvent.getClass().getMethod("getMouseButtonType")
          .invoke(mouseButtonEvent);
    } catch (Exception ignored) {
      // fallback below
    }

    try {
      state = (MouseButtonState) mouseButtonEvent.getClass().getMethod("getState")
          .invoke(mouseButtonEvent);
    } catch (Exception ignored) {
      // fallback below
    }

    if (type == null) {
      try {
        var field = mouseButtonEvent.getClass().getDeclaredField("mouseButtonType");
        field.setAccessible(true);
        type = (MouseButtonType) field.get(mouseButtonEvent);
      } catch (Exception ignored) {
        // fallback below
      }
    }

    if (state == null) {
      try {
        var field = mouseButtonEvent.getClass().getDeclaredField("state");
        field.setAccessible(true);
        state = (MouseButtonState) field.get(mouseButtonEvent);
      } catch (Exception ignored) {
        // fallback below
      }
    }

    return type == MouseButtonType.Left && state == MouseButtonState.Pressed;
  }

  private String resolveItemId(Item item) {
    if (item == null) {
      return null;
    }

    try {
      return item.getId();
    } catch (Exception ignored) {
      // fallback below
    }

    try {
      return String.valueOf(item.getClass().getMethod("getId").invoke(item));
    } catch (Exception ignored) {
      // fallback below
    }

    return String.valueOf(item);
  }

  private void logMouseButtonEvent(PlayerMouseButtonEvent event) {
    String playerName = resolvePlayerName(event.getPlayer());
    Item itemInHand = event.getItemInHand();
    String itemId = resolveItemId(itemInHand);

    MouseButtonEvent mouseButtonEvent = event.getMouseButton();
    MouseButtonType type = null;
    MouseButtonState state = null;

    if (mouseButtonEvent != null) {
      try {
        type = (MouseButtonType) mouseButtonEvent.getClass().getMethod("getMouseButtonType")
            .invoke(mouseButtonEvent);
      } catch (Exception ignored) {
        // fallback below
      }

      try {
        state = (MouseButtonState) mouseButtonEvent.getClass().getMethod("getState")
            .invoke(mouseButtonEvent);
      } catch (Exception ignored) {
        // fallback below
      }
    }

    logger.atInfo().log(
        "MouseButtonEvent: player=%s itemId=%s mouseButton=%s type=%s state=%s",
        playerName,
        itemId,
        mouseButtonEvent,
        type,
        state);
  }

  private HomeLocation resolveBedLocation(Player player, World world) {
    PlayerWorldData worldData = resolvePlayerWorldData(player, world);
    if (worldData == null) {
      logger.atInfo().log(
          "No per-world data for %s in world %s.",
          resolvePlayerName(player),
          world.getName());
      return null;
    }

    PlayerRespawnPointData[] respawnPoints = worldData.getRespawnPoints();
    if (respawnPoints == null || respawnPoints.length == 0) {
      logger.atInfo().log(
          "No respawn points for %s in world %s.",
          resolvePlayerName(player),
          world.getName());
      return null;
    }

    PlayerRespawnPointData respawnPoint = respawnPoints[0];
    if (respawnPoint == null || respawnPoint.getRespawnPosition() == null) {
      logger.atInfo().log(
          "Respawn point missing position for %s in world %s.",
          resolvePlayerName(player),
          world.getName());
      return null;
    }

    Vector3d bedPosition = respawnPoint.getRespawnPosition();
    return new HomeLocation(world.getName(), bedPosition.getX(), bedPosition.getY(), bedPosition.getZ());
  }

  private HomeLocation resolveSpawnLocation(Player player, World world) {
    ISpawnProvider spawnProvider = resolveSpawnProvider(world);
    if (spawnProvider == null) {
      logger.atWarning().log("Spawn provider not available for world %s.", world.getName());
      return null;
    }

    World safeWorld = java.util.Objects.requireNonNull(world);
    java.util.UUID safeUuid = java.util.Objects.requireNonNull(resolvePlayerUuid(player));
    Transform spawnPoint = spawnProvider.getSpawnPoint(safeWorld, safeUuid);
    if (spawnPoint == null || spawnPoint.getPosition() == null) {
      logger.atWarning().log(
          "Spawn provider returned null for %s in world %s.",
          resolvePlayerName(player),
          world.getName());
      return null;
    }

    Vector3d spawnPosition = spawnPoint.getPosition();
    return new HomeLocation(world.getName(), spawnPosition.getX(), spawnPosition.getY(), spawnPosition.getZ());
  }

  private void queueTeleport(Player player, World world, HomeLocation location,
      CommandBuffer<EntityStore> commandBuffer) {
    Vector3d position = new Vector3d(location.x(), location.y(), location.z());
    Vector3f rotation = new Vector3f(0f, 0f, 0f);
    Transform transform = new Transform(position, rotation);

    logger.atInfo().log(
        "Queueing teleport: player=%s world=%s pos=(%.2f, %.2f, %.2f)",
        resolvePlayerName(player),
        world.getName(),
        location.x(),
        location.y(),
        location.z());

    Teleport teleport = Teleport.createForPlayer(world, transform);

    var playerRef = java.util.Objects.requireNonNull(player.getReference());
    ComponentType<EntityStore, Teleport> teleportType = resolveTeleportComponentType();
    if (teleportType == null) {
      logger.atWarning().log(
          "Teleport component type unavailable; cannot teleport %s.",
          resolvePlayerName(player));
      return;
    }

    if (commandBuffer == null) {
      logger.atWarning().log(
          "CommandBuffer unavailable; teleport skipped for %s.",
          resolvePlayerName(player));
      return;
    }

    commandBuffer.putComponent(
        playerRef,
        teleportType,
        teleport);

    logger.atInfo().log(
        "Applied Teleport component for %s",
        resolvePlayerName(player));

    logger.atInfo().log(
        "Queued home teleport for %s to %.2f, %.2f, %.2f in %s",
        resolvePlayerName(player),
        location.x(),
        location.y(),
        location.z(),
        location.worldId());
  }

  private String resolvePlayerName(Player player) {
    return ReflectPlayer.resolveName(player);
  }

  private PlayerWorldData resolvePlayerWorldData(Player player, World world) {
    try {
      return (PlayerWorldData) player.getClass()
          .getMethod("getPerWorldData", String.class)
          .invoke(player, world.getName());
    } catch (Exception ex) {
      logger.atWarning().withCause(ex).log("Failed to resolve per-world data for player.");
      return null;
    }
  }

  private ISpawnProvider resolveSpawnProvider(World world) {
    try {
      return (ISpawnProvider) world.getClass().getMethod("getSpawnProvider").invoke(world);
    } catch (Exception ignored) {
      // fallback below
    }

    try {
      Object worldConfig = world.getClass().getMethod("getWorldConfig").invoke(world);
      if (worldConfig == null) {
        return null;
      }
      return (ISpawnProvider) worldConfig.getClass().getMethod("getSpawnProvider").invoke(worldConfig);
    } catch (Exception ex) {
      logger.atWarning().withCause(ex).log("Failed to resolve spawn provider for world %s.", world.getName());
      return null;
    }
  }

  private java.util.UUID resolvePlayerUuid(Player player) {
    try {
      return (java.util.UUID) player.getClass().getMethod("getUuid").invoke(player);
    } catch (Exception ignored) {
      // fallback below
    }

    try {
      Object ref = player.getReference();
      if (ref != null) {
        return (java.util.UUID) ref.getClass().getMethod("getUuid").invoke(ref);
      }
    } catch (Exception ex) {
      logger.atWarning().withCause(ex).log("Failed to resolve player UUID; using random UUID.");
    }

    return java.util.UUID.randomUUID();
  }

  private Player resolvePlayerFromRef(PlayerRef playerRef) {
    try {
      EntityModule module = resolveEntityModule();
      if (module == null) {
        return null;
      }

      var ref = playerRef.getReference();
      if (ref == null) {
        return null;
      }

      Store<EntityStore> store = (Store<EntityStore>) ref.getStore();
      ComponentType<EntityStore, Player> playerType = java.util.Objects
          .requireNonNull((ComponentType<EntityStore, Player>) module.getPlayerComponentType());

      return store.getComponent(ref, playerType);
    } catch (Exception ex) {
      logger.atWarning().withCause(ex).log("Failed to resolve Player from PlayerRef.");
      return null;
    }
  }

  private ComponentType<EntityStore, PendingTeleport> resolvePendingTeleportComponentType() {
    try {
      EntityModule module = resolveEntityModule();
      if (module == null) {
        return null;
      }

      return (ComponentType<EntityStore, PendingTeleport>) module.getPendingTeleportComponentType();
    } catch (Exception ex) {
      logger.atWarning().withCause(ex).log("Failed to resolve PendingTeleport component type.");
      return null;
    }
  }

  private ComponentType<EntityStore, Teleport> resolveTeleportComponentType() {
    try {
      EntityModule module = resolveEntityModule();
      if (module == null) {
        return null;
      }

      return (ComponentType<EntityStore, Teleport>) module.getTeleportComponentType();
    } catch (Exception ex) {
      logger.atWarning().withCause(ex).log("Failed to resolve Teleport component type.");
      return null;
    }
  }

  private EntityModule resolveEntityModule() {
    try {
      try {
        return (EntityModule) EntityModule.class.getMethod("getInstance").invoke(null);
      } catch (NoSuchMethodException ignored) {
        // fallback below
      }

      try {
        return (EntityModule) EntityModule.class.getMethod("instance").invoke(null);
      } catch (NoSuchMethodException ignored) {
        // fallback below
      }

      var field = EntityModule.class.getDeclaredField("instance");
      field.setAccessible(true);
      return (EntityModule) field.get(null);
    } catch (Exception ex) {
      logger.atWarning().withCause(ex).log("Failed to resolve EntityModule instance.");
      return null;
    }
  }
}
