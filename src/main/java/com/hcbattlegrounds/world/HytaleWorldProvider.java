package com.hcbattlegrounds.world;

import com.hypixel.hytale.builtin.instances.InstancesPlugin;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.math.vector.Transform;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HytaleWorldProvider implements WorldProvider {

    private static final Logger LOGGER = Logger.getLogger(HytaleWorldProvider.class.getName());
    private static final String INSTANCE_TEMPLATE_NAME = "Battlegrounds";
    private static final String WORLD_PREFIX = "battlegrounds-";

    private final boolean instanceTemplateExists;

    public HytaleWorldProvider() {
        this.instanceTemplateExists = InstancesPlugin.doesInstanceAssetExist(INSTANCE_TEMPLATE_NAME);
        if (this.instanceTemplateExists) {
            LOGGER.log(Level.INFO, "Using instance-based arena from template: " + INSTANCE_TEMPLATE_NAME);
        } else {
            LOGGER.log(Level.WARNING, "Instance template '" + INSTANCE_TEMPLATE_NAME + "' not found! "
                + "Ensure HC_BattlegroundsInstance mod is installed in server-files/mods/");
        }
    }

    @Override
    public CompletableFuture<World> createArenaWorld(UUID warId, World currentWorld, Transform returnTransform) {
        if (!instanceTemplateExists) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Instance template '" + INSTANCE_TEMPLATE_NAME + "' not found"));
        }

        String worldName = WORLD_PREFIX + warId.toString().substring(0, 8);
        LOGGER.log(Level.INFO, "Spawning arena from instance template: " + worldName);

        return InstancesPlugin.get().spawnInstance(
            INSTANCE_TEMPLATE_NAME,
            worldName,
            currentWorld,
            returnTransform
        );
    }

    @Override
    public CompletableFuture<Void> destroyArenaWorld(World world) {
        if (world == null) {
            return CompletableFuture.completedFuture(null);
        }
        try {
            InstancesPlugin.safeRemoveInstance(world);
            LOGGER.log(Level.INFO, "Marked instance arena for removal: " + world.getName());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to remove arena world: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }
}
