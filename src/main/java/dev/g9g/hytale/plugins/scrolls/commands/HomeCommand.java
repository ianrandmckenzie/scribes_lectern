package com.relentlesscurious.hytale.plugins.scrolls.commands;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.relentlesscurious.hytale.plugins.scrolls.data.HomeStorage;
import com.relentlesscurious.hytale.plugins.scrolls.data.PlayerHomeData;
import com.relentlesscurious.hytale.plugins.scrolls.data.StoredLocation;

import javax.annotation.Nonnull;

public class HomeCommand extends AbstractPlayerCommand {
  private final HomeStorage homeStorage;

  public HomeCommand(HomeStorage homeStorage) {
    super("home", "Teleport to your home");
    this.homeStorage = homeStorage;
  }

  @Override
  protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
      @Nonnull PlayerRef player, @Nonnull World world) {
    String name = "home"; // Default, single home for now

    PlayerHomeData playerHomeData = homeStorage.getPlayerHomeData(player.getUuid());
    StoredLocation home = playerHomeData.getHome(name);

    if (home == null) {
      ctx.sendMessage(Message.raw("Home '" + name + "' not found."));
      return;
    }

    World targetWorld = Universe.get().getWorld(home.getWorldName());
    if (targetWorld == null) {
      ctx.sendMessage(Message.raw("World '" + home.getWorldName() + "' not found."));
      return;
    }

    Vector3d position = new Vector3d(home.getX(), home.getY(), home.getZ());
    Vector3f rotation = new Vector3f(0f, home.getYaw(), 0f);

    targetWorld.execute(() -> {
      Teleport teleport = new Teleport(targetWorld, position, rotation);

      try {
        EntityModule module = resolveEntityModule();
        if (module != null) {
          ComponentType<EntityStore, Teleport> teleportType = module.getTeleportComponentType();
          store.putComponent(ref, teleportType, teleport);
          ctx.sendMessage(Message.raw("Teleported to '" + name + "'."));
        } else {
          ctx.sendMessage(Message.raw("Teleport failed: EntityModule unavailable."));
        }
      } catch (Exception e) {
        ctx.sendMessage(Message.raw("Teleport failed: " + e.getMessage()));
        e.printStackTrace();
      }
    });
  }

  private EntityModule resolveEntityModule() {
    try {
      // Try method references via reflection to avoid static access errors during
      // compilation if ambiguous
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
