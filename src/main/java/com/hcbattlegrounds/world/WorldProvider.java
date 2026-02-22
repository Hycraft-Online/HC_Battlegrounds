package com.hcbattlegrounds.world;

import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.math.vector.Transform;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface WorldProvider {
    CompletableFuture<World> createArenaWorld(UUID warId, World currentWorld, Transform returnTransform);

    CompletableFuture<Void> destroyArenaWorld(World world);
}
