package com.relentlesscurious.hytale.plugins.scrolls.teleport;

import com.relentlesscurious.hytale.plugins.scrolls.util.ReflectPlayer;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.MouseButtonEvent;
import com.hypixel.hytale.protocol.MouseButtonState;
import com.hypixel.hytale.protocol.MouseButtonType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import com.relentlesscurious.hytale.plugins.scrolls.config.FriendTeleportConfig;

public class FriendTeleportScrollListener {

  private static final String SCROLL_FRIEND_ITEM_ID = "Scroll_Friend_Teleport";

  private final FriendTeleportService friendTeleportService;
  private final FriendTeleportConfig config;
  private final HytaleLogger logger;
  private final Map<UUID, Player> onlinePlayersTracker = new ConcurrentHashMap<>();

  public FriendTeleportScrollListener(FriendTeleportService friendTeleportService, FriendTeleportConfig config, HytaleLogger logger) {
    this.friendTeleportService = friendTeleportService;
    this.config = config;
    this.logger = logger;
  }

  public void onPlayerReady(Player player) {
    if (player == null) return;
    UUID uuid = resolvePlayerUuid(player);
    onlinePlayersTracker.put(uuid, player);
  }

  public void onPlayerLogout(UUID uuid) {
    if (uuid == null) return;
    onlinePlayersTracker.remove(uuid);
  }

  public void onPlayerMouseButton(PlayerMouseButtonEvent event) {
    if (event == null || event.isCancelled()) {
      return;
    }
    if (!isScrollInteraction(event)) {
      return;
    }
    Player player = event.getPlayer();
    if (player == null) {
      return;
    }
    showFriendMenu(player);
    event.setCancelled(true);
  }

  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event == null || event.isCancelled()) {
      return;
    }
    if (event.getActionType() != InteractionType.Primary) {
      return;
    }
    ItemStack itemStack = event.getItemInHand();
    String itemId = itemStack != null ? itemStack.getItemId() : null;
    if (!SCROLL_FRIEND_ITEM_ID.equalsIgnoreCase(itemId)) {
      return;
    }
    Player player = event.getPlayer();
    if (player == null) {
      return;
    }
    showFriendMenu(player);
    event.setCancelled(true);
  }

  public void handleUse(PlayerRef playerRef, CommandBuffer commandBuffer) {
    // Kept for legacy event-path callers; the interaction supplier now uses buildFriendTeleportPage directly.
    logger.atInfo().log("handleUse called for friend teleport (legacy path).");
  }

  /**
   * Creates and returns a FriendTeleportPage for the given player.
   * Called by the OpenCustomUIInteraction supplier so the engine opens the page correctly.
   */
  public FriendTeleportPage buildFriendTeleportPage(PlayerRef playerRef) {
    if (playerRef == null) return null;
    logger.atInfo().log("Building FriendTeleportPage for %s", playerRef);
    return new FriendTeleportPage(playerRef, friendTeleportService, onlinePlayersTracker, this, config);
  }

  private void showFriendMenu(Player player) {
    if (player == null) return;
    PlayerRef playerRef = player.getPlayerRef();
    if (playerRef == null) return;

    logger.atInfo().log("Opening Friend Teleport menu for %s (event path)", resolvePlayerName(player));

    Ref<EntityStore> ref = (Ref<EntityStore>) player.getReference();
    if (ref == null) return;

    Store<EntityStore> store = (Store<EntityStore>) ref.getStore();
    if (store == null) return;

    FriendTeleportPage page = new FriendTeleportPage(playerRef, friendTeleportService, onlinePlayersTracker, this, config);
    try {
        player.getPageManager().openCustomPage(ref, store, page);
    } catch (Throwable t) {
        logger.atWarning().withCause(t).log("Failed to open custom page via getPageManager().");
    }
  }

  public void acceptRequest(Player receiver, UUID senderUuid) {
    Player sender = onlinePlayersTracker.get(senderUuid);

    if (sender == null) {
      receiver.sendMessage(Message.raw("Player is no longer online."));
      return;
    }

    World targetWorld = receiver.getWorld();
    if (targetWorld == null) {
        logger.atWarning().log("acceptRequest: receiver %s has no World.", resolvePlayerName(receiver));
        receiver.sendMessage(Message.raw("Could not determine your world. Please try again."));
        return;
    }

    var ref = receiver.getReference();
    if (ref == null) {
        logger.atWarning().log("acceptRequest: null ref for %s.", resolvePlayerName(receiver));
        receiver.sendMessage(Message.raw("Could not determine your location. Please try again."));
        return;
    }

    Store<EntityStore> store = (Store<EntityStore>) ref.getStore();
    TransformComponent transformComp = store.getComponent(ref, TransformComponent.getComponentType());

    if (transformComp == null) {
        logger.atWarning().log("acceptRequest: could not resolve TransformComponent for %s.", resolvePlayerName(receiver));
        receiver.sendMessage(Message.raw("Could not determine your location. Please try again."));
        return;
    }

    Vector3d targetPos = transformComp.getPosition();
    Vector3f targetRot = transformComp.getRotation();

    double chargingTime = config.chargingTime;
    if (chargingTime > 0) {
        sender.sendMessage(com.hypixel.hytale.server.core.Message.raw("Friend teleport charging... (" + chargingTime + "s)"));
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep((long) (chargingTime * 1000));
            } catch (InterruptedException ignored) {}
        }).thenRun(() -> {
            targetWorld.execute(() -> {
                long chunkIndex = com.hypixel.hytale.math.util.ChunkUtil.indexChunkFromBlock(targetPos.x, targetPos.z);
                targetWorld.getChunkAsync(chunkIndex).whenComplete((chunk, err) -> {
                    targetWorld.execute(() -> {
                        if (chunk == null || err != null) {
                            sender.sendMessage(com.hypixel.hytale.server.core.Message.raw("Failed to load destination chunk."));
                            return;
                        }
                        performTeleport(sender, targetWorld, targetPos, targetRot);
                        sender.sendMessage(com.hypixel.hytale.server.core.Message.raw("Teleporting to " + resolvePlayerName(receiver)));
                        receiver.sendMessage(com.hypixel.hytale.server.core.Message.raw("Teleporting " + resolvePlayerName(sender) + " to you."));
                        friendTeleportService.clearRequest(resolvePlayerUuid(receiver), senderUuid);
                    });
                });
            });
        });
        return;
    }

    long chunkIndex = com.hypixel.hytale.math.util.ChunkUtil.indexChunkFromBlock(targetPos.x, targetPos.z);
    targetWorld.getChunkAsync(chunkIndex).whenComplete((chunk, err) -> {
        targetWorld.execute(() -> {
            if (chunk == null || err != null) {
                receiver.sendMessage(com.hypixel.hytale.server.core.Message.raw("Failed to load destination chunk."));
                return;
            }
            performTeleport(sender, targetWorld, targetPos, targetRot);
            sender.sendMessage(com.hypixel.hytale.server.core.Message.raw("Teleporting to " + resolvePlayerName(receiver)));
            receiver.sendMessage(com.hypixel.hytale.server.core.Message.raw("Teleporting " + resolvePlayerName(sender) + " to you."));
            friendTeleportService.clearRequest(resolvePlayerUuid(receiver), senderUuid);
        });
    });
  }

  private void performTeleport(Player player, World world, Vector3d pos, Vector3f rot) {
    try {
      var ref = player.getReference();
      if (ref == null) {
        logger.atWarning().log("performTeleport: null ref for %s.", resolvePlayerName(player));
        return;
      }

      Store<EntityStore> store = (Store<EntityStore>) ref.getStore();
      if (store == null) {
        logger.atWarning().log("performTeleport: null store for %s.", resolvePlayerName(player));
        return;
      }

      EntityModule module = resolveEntityModule();
      if (module == null) {
        logger.atWarning().log("performTeleport: EntityModule unavailable.");
        return;
      }

      ComponentType<EntityStore, Teleport> teleportType = (ComponentType<EntityStore, Teleport>) module.getTeleportComponentType();
      if (teleportType == null) {
        logger.atWarning().log("performTeleport: teleportType unavailable.");
        return;
      }

      Transform transform = new Transform(pos, rot);
      Teleport teleport = Teleport.createForPlayer(world, transform);

      store.putComponent(ref, teleportType, teleport);
      logger.atInfo().log("Teleport component applied for %s to (%.1f, %.1f, %.1f).", resolvePlayerName(player), pos.x, pos.y, pos.z);
    } catch (Exception ex) {
      logger.atWarning().withCause(ex).log("Failed to perform teleport.");
    }
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

  private boolean isScrollInteraction(PlayerMouseButtonEvent event) {
    if (event == null || event.getMouseButton() == null) {
      return false;
    }
    if (!isLeftClick(event.getMouseButton())) {
      return false;
    }
    Item item = event.getItemInHand();
    String itemId = resolveItemId(item);
    return SCROLL_FRIEND_ITEM_ID.equalsIgnoreCase(itemId);
  }

  private boolean isLeftClick(MouseButtonEvent mouseButtonEvent) {
    try {
      MouseButtonType type = (MouseButtonType) mouseButtonEvent.getClass().getMethod("getMouseButtonType").invoke(mouseButtonEvent);
      MouseButtonState state = (MouseButtonState) mouseButtonEvent.getClass().getMethod("getState").invoke(mouseButtonEvent);
      return type == MouseButtonType.Left && state == MouseButtonState.Pressed;
    } catch (Exception e) {
      return false;
    }
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

  public String resolvePlayerName(Player player) {
    return ReflectPlayer.resolveName(player);
  }

  public UUID resolvePlayerUuid(Player player) {
    try {
      return (UUID) player.getClass().getMethod("getUuid").invoke(player);
    } catch (Exception ignored) {
    }
    try {
      Object ref = player.getReference();
      if (ref != null) {
        return (UUID) ref.getClass().getMethod("getUuid").invoke(ref);
      }
    } catch (Exception ignored) {
    }
    return UUID.randomUUID();
  }

  private Object resolveComponent(Player player, Class<?> componentClass) {
    try {
      var ref = player.getReference();
      if (ref == null) return null;

      EntityModule module = resolveEntityModule();
      if (module == null) return null;

      Store<EntityStore> store = (Store<EntityStore>) ref.getStore();
      if (store == null) return null;

      // Walk all registered component types and find the one whose value type matches componentClass
      for (java.lang.reflect.Method m : module.getClass().getMethods()) {
        if (!m.getName().startsWith("get") || m.getParameterCount() != 0) continue;
        Object candidate;
        try {
          candidate = m.invoke(module);
        } catch (Exception ignored) {
          continue;
        }
        if (!(candidate instanceof ComponentType)) continue;
        @SuppressWarnings("unchecked")
        ComponentType<EntityStore, ?> ct = (ComponentType<EntityStore, ?>) candidate;
        Object component;
        try {
          component = store.getComponent(ref, ct);
        } catch (Exception ignored) {
          continue;
        }
        if (component != null && componentClass.isAssignableFrom(component.getClass())) {
          return component;
        }
      }

      // Fallback: try store.getComponents() if available
      try {
        java.util.Collection<?> components = (java.util.Collection<?>) store.getClass()
            .getMethod("getComponents", ref.getClass()).invoke(store, ref);
        if (components != null) {
          for (Object c : components) {
            if (componentClass.isAssignableFrom(c.getClass())) return c;
          }
        }
      } catch (Exception ignored) {}

    } catch (Exception e) {
      logger.atWarning().withCause(e).log("resolveComponent failed for %s.", componentClass.getSimpleName());
    }
    return null;
  }

  private String resolveItemId(Item item) {
    if (item == null) {
      return null;
    }
    try {
      return item.getId();
    } catch (Exception ignored) {
    }
    try {
      return String.valueOf(item.getClass().getMethod("getId").invoke(item));
    } catch (Exception ignored) {
    }
    return item.toString();
  }

  private EntityModule resolveEntityModule() {
    try {
      try {
        return (EntityModule) EntityModule.class.getMethod("getInstance").invoke(null);
      } catch (NoSuchMethodException ignored) {
      }
      try {
        return (EntityModule) EntityModule.class.getMethod("instance").invoke(null);
      } catch (NoSuchMethodException ignored) {
      }
      try {
        var field = EntityModule.class.getDeclaredField("instance");
        field.setAccessible(true);
        return (EntityModule) field.get(null);
      } catch (NoSuchFieldException ignored) {
      }
      try {
        var field = EntityModule.class.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        return (EntityModule) field.get(null);
      } catch (NoSuchFieldException ignored) {
      }
    } catch (Exception ignored) {
    }
    return null;
  }
}
