package com.relentlesscurious.hytale.plugins.scrolls.random;

import com.relentlesscurious.hytale.plugins.scrolls.util.ReflectPlayer;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.MouseButtonEvent;
import com.hypixel.hytale.protocol.MouseButtonState;
import com.hypixel.hytale.protocol.MouseButtonType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.relentlesscurious.hytale.plugins.scrolls.config.RandomTeleportConfig;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class RandomTeleportScrollListener {

  private static final String SCROLL_RANDOM_ITEM_ID = "Scroll_Random_Teleport";
  private static final String[] UNSAFE_KEYWORDS = { "campfire", "thorn" };

  private final RandomTeleportConfig config;
  private final HytaleLogger logger;
  private final Method fluidIdMethod;

  public RandomTeleportScrollListener(RandomTeleportConfig config, HytaleLogger logger) {
    this.config = config;
    this.logger = logger;
    this.fluidIdMethod = resolveFluidIdMethod();
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
    handleUse(player);
    event.setCancelled(true);
  }

  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event == null || event.isCancelled()) {
      return;
    }
    if (event.getActionType() != InteractionType.Primary) {
      return;
    }
    ItemStack itemInHand = event.getItemInHand();
    String itemId = itemInHand != null ? itemInHand.getItemId() : null;
    if (!SCROLL_RANDOM_ITEM_ID.equalsIgnoreCase(itemId)) {
      return;
    }
    Player player = event.getPlayer();
    if (player == null) {
      return;
    }
    handleUse(player);
    event.setCancelled(true);
  }

  public void handleUse(PlayerRef playerRef, CommandBuffer<EntityStore> commandBuffer) {
    if (playerRef == null) {
      logger.atWarning().log("Random teleport interaction received without player reference.");
      return;
    }
    Player player = resolvePlayerFromRef(playerRef);
    if (player == null) {
      logger.atWarning().log("Random teleport scroll could not resolve player from reference.");
      return;
    }
    handleUse(player);
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
      return;
    }
    World world = player.getWorld();
    if (world == null) {
      logger.atWarning().log("Random teleport scroll used by %s with no world.", resolvePlayerName(player));
      return;
    }
    Vector3d position = resolvePlayerPosition(player);
    if (position == null) {
      logger.atWarning().log("Could not determine position for %s before random teleport.", resolvePlayerName(player));
      return;
    }
    double centerX = position.getX();
    double centerZ = position.getZ();
    logger.atInfo().log("Random teleport requested for %s (shape=%s range=%.1f-%.1f)",
        resolvePlayerName(player), config.shape, config.minRange, config.maxRange);
    world.execute(() -> attemptTeleport(player, world, centerX, centerZ, 0));
  }

  private void attemptTeleport(Player player, World world, double centerX, double centerZ, int attempt) {
    int maxAttempts = Math.max(1, config.maxAttempts);
    if (attempt >= maxAttempts) {
      player.sendMessage(Message.raw("Could not locate a safe random location. Try again soon."));
      logger.atWarning().log("Random teleport search failed for %s after %d attempts.", resolvePlayerName(player),
          maxAttempts);
      return;
    }

    double[] target = computeTarget(centerX, centerZ);
    double targetX = target[0];
    double targetZ = target[1];
    long chunkIndex = ChunkUtil.indexChunkFromBlock(targetX, targetZ);

    WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
    if (chunk == null) {
      chunk = world.getChunkIfInMemory(chunkIndex);
    }

    if (chunk != null) {
      processChunk(player, world, centerX, centerZ, attempt, targetX, targetZ, chunk);
    } else {
      final int nextAttempt = attempt;
      final double finalTargetX = targetX;
      final double finalTargetZ = targetZ;
      world.getChunkAsync(chunkIndex).whenComplete((loadedChunk, error) -> {
        world.execute(() -> {
          if (loadedChunk == null || error != null) {
            attemptTeleport(player, world, centerX, centerZ, nextAttempt + 1);
          } else {
            processChunk(player, world, centerX, centerZ, nextAttempt, finalTargetX, finalTargetZ, loadedChunk);
          }
        });
      });
    }
  }

  private void processChunk(Player player, World world, double centerX, double centerZ, int attempt,
      double targetX, double targetZ, WorldChunk chunk) {
    int blockX = (int) Math.floor(targetX);
    int blockZ = (int) Math.floor(targetZ);
    Integer groundY = findHighestSolidBlock(chunk, blockX, blockZ, config.minSurfaceY);
    if (groundY == null) {
      attemptTeleport(player, world, centerX, centerZ, attempt + 1);
      return;
    }

    double teleportY = groundY + 1;
    if (!isSafeLocation(chunk, blockX, groundY, blockZ)) {
      attemptTeleport(player, world, centerX, centerZ, attempt + 1);
      return;
    }

    queueTeleport(player, world, targetX, teleportY, targetZ);
  }

  @Nullable
  private Vector3d resolvePlayerPosition(Player player) {
    if (player == null) {
      return null;
    }
    Ref<EntityStore> ref = player.getReference();
    if (ref == null) {
      return null;
    }
    Store<EntityStore> store = (Store<EntityStore>) ref.getStore();
    if (store == null) {
      return null;
    }
    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    if (transform == null) {
      return null;
    }
    return transform.getPosition();
  }

  private boolean isSafeLocation(WorldChunk chunk, int x, int groundY, int z) {
    BlockType ground = chunk.getBlockType(x, groundY, z);
    if (hasUnsafeTag(ground)) {
      return false;
    }

    BlockType above = chunk.getBlockType(x, groundY + 1, z);
    BlockType aboveHead = chunk.getBlockType(x, groundY + 2, z);
    if (isBlocking(above) || isBlocking(aboveHead)) {
      return false;
    }

    return !hasNearbyFluid(chunk, x, groundY, z);
  }

  private boolean hasNearbyFluid(WorldChunk chunk, int x, int y, int z) {
    if (fluidIdMethod == null) {
      return false;
    }
    try {
      for (int yOffset = -1; yOffset <= 2; yOffset++) {
        int checkY = y + yOffset;
        if (checkY < 0 || checkY >= 256) {
          continue;
        }
        int fluidId = (int) fluidIdMethod.invoke(chunk, x, checkY, z);
        if (fluidId == 6 || fluidId == 7) {
          return true;
        }
      }
      int[][] neighbors = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
      for (int[] neighbor : neighbors) {
        int checkX = x + neighbor[0];
        int checkZ = z + neighbor[1];
        int fluidId = (int) fluidIdMethod.invoke(chunk, checkX, y, checkZ);
        if (fluidId == 6 || fluidId == 7) {
          return true;
        }
      }
      return false;
    } catch (Exception ignored) {
      return false;
    }
  }

  private boolean hasUnsafeTag(BlockType block) {
    if (block == null) {
      return false;
    }
    String id = block.getId();
    if (id == null) {
      return false;
    }
    String lower = id.toLowerCase();
    for (String keyword : UNSAFE_KEYWORDS) {
      if (lower.contains(keyword)) {
        return true;
      }
    }
    return false;
  }

  private boolean isBlocking(BlockType block) {
    if (block == null) {
      return false;
    }
    try {
      return block.getMaterial() == BlockMaterial.Solid;
    } catch (Exception ignored) {
      return false;
    }
  }

  private Integer findHighestSolidBlock(WorldChunk chunk, int x, int z, int minY) {
    for (int y = 255; y >= minY; y--) {
      try {
        BlockType blockType = chunk.getBlockType(x, y, z);
        if (blockType != null && blockType.getMaterial() == BlockMaterial.Solid) {
          return y;
        }
      } catch (Exception ignored) {
        // Continue
      }
    }
    return null;
  }

  private double[] computeTarget(double centerX, double centerZ) {
    double min = Math.max(0, config.minRange);
    double max = Math.max(min, config.maxRange);
    String shape = config.shape != null ? config.shape.toLowerCase() : "circle";
    if ("square".equals(shape) && max > 0) {
      for (int attempt = 0; attempt < 5; attempt++) {
        double offsetX = (ThreadLocalRandom.current().nextDouble() * 2 - 1) * max;
        double offsetZ = (ThreadLocalRandom.current().nextDouble() * 2 - 1) * max;
        if (Math.max(Math.abs(offsetX), Math.abs(offsetZ)) >= min) {
          return new double[] { centerX + offsetX, centerZ + offsetZ };
        }
      }
      shape = "circle";
    }
    double inner = min;
    double outer = Math.max(inner, max);
    double radius = Math
        .sqrt(ThreadLocalRandom.current().nextDouble() * (outer * outer - inner * inner) + inner * inner);
    double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2;
    double offsetX = Math.cos(angle) * radius;
    double offsetZ = Math.sin(angle) * radius;
    return new double[] { centerX + offsetX, centerZ + offsetZ };
  }

  private Method resolveFluidIdMethod() {
    try {
      return WorldChunk.class.getMethod("getFluidId", int.class, int.class, int.class);
    } catch (Exception e) {
      logger.atWarning().withCause(e).log("Random teleport could not resolve fluid helper method.");
      return null;
    }
  }

  private void queueTeleport(Player player, World world, double x, double y, double z) {
    Ref<EntityStore> ref = player.getReference();
    if (ref == null) {
      logger.atWarning().log("Random teleport could not find entity reference for %s.", resolvePlayerName(player));
      return;
    }
    Store<EntityStore> store = (Store<EntityStore>) ref.getStore();
    if (store == null) {
      logger.atWarning().log("Entity store missing for %s during random teleport.", resolvePlayerName(player));
      return;
    }

    ComponentType<EntityStore, Teleport> teleportType = resolveTeleportComponentType();
    if (teleportType == null) {
      logger.atWarning().log("Teleport component is unavailable for %s.", resolvePlayerName(player));
      return;
    }

    Transform transform = new Transform(new Vector3d(x, y, z), new Vector3f(0f, 0f, 0f));
    Teleport teleport = Teleport.createForPlayer(world, transform);
    store.putComponent(ref, teleportType, teleport);
    player.sendMessage(Message.raw("Teleported to a random location."));
    logger.atInfo().log("Random teleport queued for %s to (%.1f, %.1f, %.1f).", resolvePlayerName(player), x, y, z);
  }

  private ComponentType<EntityStore, Teleport> resolveTeleportComponentType() {
    EntityModule module = resolveEntityModule();
    if (module == null) {
      return null;
    }
    try {
      return (ComponentType<EntityStore, Teleport>) module.getTeleportComponentType();
    } catch (Exception ex) {
      logger.atWarning().withCause(ex).log("Failed to resolve teleport component type.");
      return null;
    }
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
      Field field = EntityModule.class.getDeclaredField("instance");
      field.setAccessible(true);
      return (EntityModule) field.get(null);
    } catch (Exception e) {
      logger.atWarning().withCause(e).log("Failed to resolve EntityModule.");
      return null;
    }
  }

  private boolean isScrollInteraction(PlayerMouseButtonEvent event) {
    if (event == null || event.getMouseButton() == null) {
      return false;
    }
    if (!isLeftClick(event.getMouseButton())) {
      return false;
    }
    Item item = event.getItemInHand();
    if (item == null) {
      return false;
    }
    String itemId = resolveItemId(item);
    return SCROLL_RANDOM_ITEM_ID.equalsIgnoreCase(itemId);
  }

  private boolean isLeftClick(MouseButtonEvent mouseButtonEvent) {
    MouseButtonType type = null;
    MouseButtonState state = null;
    try {
      type = (MouseButtonType) mouseButtonEvent.getClass().getMethod("getMouseButtonType").invoke(mouseButtonEvent);
    } catch (Exception ignored) {
    }
    try {
      state = (MouseButtonState) mouseButtonEvent.getClass().getMethod("getState").invoke(mouseButtonEvent);
    } catch (Exception ignored) {
    }
    if (type == null) {
      try {
        var field = mouseButtonEvent.getClass().getDeclaredField("mouseButtonType");
        field.setAccessible(true);
        type = (MouseButtonType) field.get(mouseButtonEvent);
      } catch (Exception ignored) {
      }
    }
    if (state == null) {
      try {
        var field = mouseButtonEvent.getClass().getDeclaredField("state");
        field.setAccessible(true);
        state = (MouseButtonState) field.get(mouseButtonEvent);
      } catch (Exception ignored) {
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
    }
    try {
      return String.valueOf(item.getClass().getMethod("getId").invoke(item));
    } catch (Exception ignored) {
    }
    return item.toString();
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

  private UUID resolvePlayerUuid(Player player) {
    try {
      return (UUID) player.getClass().getMethod("getUuid").invoke(player);
    } catch (Exception ignored) {
      // fallback below
    }

    try {
      Object ref = player.getReference();
      if (ref != null) {
        return (UUID) ref.getClass().getMethod("getUuid").invoke(ref);
      }
    } catch (Exception ex) {
      logger.atWarning().withCause(ex).log("Failed to resolve player UUID; using random UUID.");
    }

    return UUID.randomUUID();
  }

  private String resolvePlayerName(Player player) {
    return ReflectPlayer.resolveName(player);
  }
}
