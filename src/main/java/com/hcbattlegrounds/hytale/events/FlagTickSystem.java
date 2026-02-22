package com.hcbattlegrounds.hytale.events;

import com.hcbattlegrounds.BattlegroundsManager;
import com.hcbattlegrounds.HC_BattlegroundsPlugin;
import com.hcbattlegrounds.hytale.notification.BattlegroundsMessages;
import com.hcbattlegrounds.models.CaptureFlag;
import com.hcbattlegrounds.models.FactionRole;
import com.hcbattlegrounds.models.FactionWar;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nonnull;

public class FlagTickSystem extends EntityTickingSystem<EntityStore> {
    // processFlagTick() is called once per second by BattlegroundsNotifier
    private static final int CAPTURE_UPDATE_INTERVAL_SECONDS = 1;
    private static final int POINT_AWARD_INTERVAL_SECONDS = 5;
    private int tickCounter = 0;
    private int pointTickCounter = 0;
    private final Map<UUID, Map<Integer, Set<UUID>>> warFlagPlayers = new HashMap<>();

    public void tick(float v, int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Player player = archetypeChunk.getComponent(i, Player.getComponentType());
        if (player == null) {
            return;
        }
        PlayerRef playerRef = store.getComponent(player.getReference(), PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        UUID playerId = playerRef.getUuid();
        BattlegroundsManager manager = HC_BattlegroundsPlugin.getInstance().getWarManager();
        FactionWar war = manager.getWarByPlayer(playerId);
        if (war == null || !war.isActive()) {
            return;
        }
        TransformComponent transform = store.getComponent(player.getReference(), EntityModule.get().getTransformComponentType());
        if (transform == null) {
            return;
        }
        Vector3d playerPos = transform.getPosition();
        this.trackPlayerNearFlags(war, playerId, playerPos);
    }

    private void trackPlayerNearFlags(FactionWar war, UUID playerId, Vector3d playerPos) {
        UUID warId = war.getWarId();
        this.warFlagPlayers.computeIfAbsent(warId, k -> new HashMap<>());
        Map<Integer, Set<UUID>> flagPlayers = this.warFlagPlayers.get(warId);
        CaptureFlag[] flags = war.getFlags();
        for (int i = 0; i < flags.length; ++i) {
            CaptureFlag flag = flags[i];
            flagPlayers.computeIfAbsent(i, k -> new HashSet<>());
            if (flag.isPlayerInRange(playerPos.getX(), playerPos.getY(), playerPos.getZ())) {
                flagPlayers.get(i).add(playerId);
            } else {
                flagPlayers.get(i).remove(playerId);
            }
        }
    }

    public void processFlagTick() {
        this.tickCounter++;
        this.pointTickCounter++;
        if (this.tickCounter < CAPTURE_UPDATE_INTERVAL_SECONDS) {
            return;
        }
        this.tickCounter = 0;
        boolean shouldAwardPoints = this.pointTickCounter >= POINT_AWARD_INTERVAL_SECONDS;
        if (shouldAwardPoints) {
            this.pointTickCounter = 0;
        }
        BattlegroundsManager manager = HC_BattlegroundsPlugin.getInstance().getWarManager();
        for (FactionWar war : manager.getActiveWars().values()) {
            if (!war.isActive()) continue;
            UUID warId = war.getWarId();
            Map<Integer, Set<UUID>> flagPlayers = this.warFlagPlayers.getOrDefault(warId, new HashMap<>());
            CaptureFlag[] flags = war.getFlags();
            for (int i = 0; i < flags.length; ++i) {
                CaptureFlag flag = flags[i];
                Set<UUID> nearbyPlayers = flagPlayers.getOrDefault(i, Collections.emptySet());
                int redCount = 0;
                int blueCount = 0;
                for (UUID playerId : nearbyPlayers) {
                    FactionRole role = war.getPlayerFaction(playerId);
                    if (role == FactionRole.RED) {
                        ++redCount;
                    } else if (role == FactionRole.BLUE) {
                        ++blueCount;
                    }
                }
                FactionRole previousController = flag.getControllingFaction();
                CaptureFlag.FlagState previousState = flag.getState();
                boolean controlChanged = flag.updateCapture(redCount, blueCount);
                if (controlChanged) {
                    this.notifyFlagCaptured(war, flag, previousController);
                }
                CaptureFlag.FlagState newState = flag.getState();
                if (previousState != newState) {
                    switch (newState) {
                        case CAPTURING:
                            this.notifyFlagCapturing(war, flag);
                            break;
                        case CONTESTED:
                            this.notifyFlagContested(war, flag);
                            break;
                        case CONTROLLED:
                            if (previousState == CaptureFlag.FlagState.CONTESTED) {
                                this.notifyFlagSecured(war, flag);
                            }
                            break;
                        case OVERTIME:
                            this.notifyFlagOvertime(war, flag);
                            break;
                    }
                }
                if (!shouldAwardPoints || !flag.isControlled() || flag.isContested()) continue;
                FactionRole controller = flag.getControllingFaction();
                int controllersNearby = controller == FactionRole.RED ? redCount : blueCount;
                int enemiesNearby = controller == FactionRole.RED ? blueCount : redCount;
                int points = flag.getPointsGenerated(controllersNearby, enemiesNearby);
                if (points <= 0) continue;
                war.addPointsToFaction(controller, points);
                this.notifyPointsEarned(war, flag, controller, points, nearbyPlayers);
            }
        }
    }

    private void notifyFlagCaptured(FactionWar war, CaptureFlag flag, FactionRole previousController) {
        FactionRole newController = flag.getControllingFaction();
        Universe universe = Universe.get();
        if (universe == null) {
            return;
        }
        for (UUID playerId : war.getAllPlayers()) {
            PlayerRef player = universe.getPlayer(playerId);
            if (player == null) continue;
            player.sendMessage(BattlegroundsMessages.flagCaptured(flag.getFlagIndex() + 1, newController));
        }
    }

    private void notifyFlagContested(FactionWar war, CaptureFlag flag) {
        Universe universe = Universe.get();
        if (universe == null) {
            return;
        }
        for (UUID playerId : war.getAllPlayers()) {
            PlayerRef player = universe.getPlayer(playerId);
            if (player == null) continue;
            player.sendMessage(BattlegroundsMessages.flagContested(flag.getFlagIndex() + 1));
        }
    }

    private void notifyFlagCapturing(FactionWar war, CaptureFlag flag) {
        FactionRole attacker = flag.getCapturingTeam();
        if (attacker == null) {
            return;
        }
        Universe universe = Universe.get();
        if (universe == null) {
            return;
        }
        for (UUID playerId : war.getAllPlayers()) {
            PlayerRef player = universe.getPlayer(playerId);
            if (player == null) continue;
            player.sendMessage(BattlegroundsMessages.flagCapturing(flag.getFlagIndex() + 1, attacker));
        }
    }

    private void notifyFlagSecured(FactionWar war, CaptureFlag flag) {
        FactionRole controller = flag.getControllingFaction();
        if (controller == null) {
            return;
        }
        Universe universe = Universe.get();
        if (universe == null) {
            return;
        }
        for (UUID playerId : war.getAllPlayers()) {
            PlayerRef player = universe.getPlayer(playerId);
            if (player == null) continue;
            player.sendMessage(BattlegroundsMessages.flagUncontested(flag.getFlagIndex() + 1, controller));
        }
    }

    private void notifyFlagOvertime(FactionWar war, CaptureFlag flag) {
        FactionRole attacker = flag.getCapturingTeam();
        if (attacker == null) {
            return;
        }
        Universe universe = Universe.get();
        if (universe == null) {
            return;
        }
        for (UUID playerId : war.getAllPlayers()) {
            PlayerRef player = universe.getPlayer(playerId);
            if (player == null) continue;
            player.sendMessage(BattlegroundsMessages.flagOvertime(flag.getFlagIndex() + 1, attacker));
        }
    }

    private void notifyPointsEarned(FactionWar war, CaptureFlag flag, FactionRole controller, int points, Set<UUID> nearbyPlayers) {
        Universe universe = Universe.get();
        if (universe == null) {
            return;
        }
        for (UUID playerId : nearbyPlayers) {
            if (war.getPlayerFaction(playerId) != controller) continue;
            PlayerRef player = universe.getPlayer(playerId);
            if (player == null) continue;
            player.sendMessage(BattlegroundsMessages.pointsEarnedFlag(points, flag.getFlagIndex() + 1, war.getFaction(controller).getPoints()));
        }
    }

    public void clearWarData(UUID warId) {
        this.warFlagPlayers.remove(warId);
    }

    @Nonnull
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType());
    }
}
