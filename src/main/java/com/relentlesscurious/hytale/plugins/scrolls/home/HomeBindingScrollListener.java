package com.relentlesscurious.hytale.plugins.scrolls.home;

import com.relentlesscurious.hytale.plugins.scrolls.util.ReflectPlayer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.relentlesscurious.hytale.plugins.scrolls.data.HomeStorage;
import com.relentlesscurious.hytale.plugins.scrolls.data.PlayerHomeData;
import com.relentlesscurious.hytale.plugins.scrolls.data.StoredLocation;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import java.util.UUID;

public class HomeBindingScrollListener {
  private final HomeStorage homeStorage;
  private final HytaleLogger logger;

  public HomeBindingScrollListener(HomeStorage homeStorage, HytaleLogger logger) {
    this.homeStorage = homeStorage;
    this.logger = logger;
  }

  public void handleUse(Ref<EntityStore> entityRef, ComponentAccessor<EntityStore> accessor, PlayerRef playerRef) {
    TransformComponent transform = accessor.getComponent(entityRef, TransformComponent.getComponentType());
    if (transform == null) {
      return;
    }

    HeadRotation headRotation = accessor.getComponent(entityRef, HeadRotation.getComponentType());
    Vector3f rotation = headRotation != null ? headRotation.getRotation() : new Vector3f(0, 0, 0);
    Vector3d position = transform.getPosition();

    Player player = resolvePlayerFromRef(playerRef);
    if (player == null)
      return;

    // Ensure world is loaded/available via Player wrapper
    if (player.getWorld() == null)
      return;
    String worldName = player.getWorld().getName();

    StoredLocation storedLocation = new StoredLocation(
        worldName,
        position.x,
        position.y,
        position.z,
        rotation.y,
        rotation.x);

    UUID playerUuid = resolvePlayerUuid(player);
    PlayerHomeData playerHomeData = homeStorage.getPlayerHomeData(playerUuid);
    playerHomeData.setHome("home", storedLocation);
    homeStorage.savePlayerHomeData(playerUuid);

    playEffect(player);
    player.sendMessage(Message.raw("Home bound to current location!"));
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

  private String resolvePlayerName(Player player) {
    return ReflectPlayer.resolveName(player);
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
      java.lang.reflect.Field field = EntityModule.class.getDeclaredField("instance");
      field.setAccessible(true);
      return (EntityModule) field.get(null);
    } catch (Exception e) {
      return null;
    }
  }
}
