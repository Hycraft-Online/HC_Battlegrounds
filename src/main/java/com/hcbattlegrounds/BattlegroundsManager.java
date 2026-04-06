package com.hcbattlegrounds;

import com.hcbattlegrounds.config.ArenaRegistry;
import com.hcbattlegrounds.hud.BattlegroundsHud;
import com.hcbattlegrounds.hud.HudWrapper;
import com.hcbattlegrounds.hytale.notification.BattlegroundsMessages;
import com.hcbattlegrounds.models.FactionRole;
import com.hcbattlegrounds.models.FactionWar;
import com.hcbattlegrounds.models.Vec3i;
import com.hcbattlegrounds.utils.SystemTimeProvider;
import com.hcbattlegrounds.utils.TimeProvider;
import com.hcbattlegrounds.world.ArenaConfig;
import com.hcbattlegrounds.world.HytaleWorldProvider;
import com.hcbattlegrounds.world.WorldProvider;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.modules.interaction.InteractionModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.awt.Color;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BattlegroundsManager {
    private static final Logger LOGGER = Logger.getLogger(BattlegroundsManager.class.getName());
    private static final int DEFAULT_MIN_PLAYERS_PER_FACTION = 3;
    private static final int DEFAULT_SCORE_TO_WIN = 2000;
    private static final String DEFAULT_QUEUE_ARENA_ID = "default";
    private static final int DEFAULT_MAX_PLAYERS_PER_WAR = 20;
    private static final long INSTANCE_DESTROY_DELAY_MS = 2000L;

    private static final long HUD_SHOW_DELAY_MS = 2000L;

    private final Map<UUID, FactionWar> activeWars = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToWar = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerReturnState> playerReturnStates = new ConcurrentHashMap<>();
    private final Map<UUID, BattlegroundsHud> activeHuds = new ConcurrentHashMap<>();

    private final Object queueLock = new Object();
    private final Map<UUID, QueueEntry> queuedPlayers = new HashMap<>();
    private final EnumMap<FactionRole, ArrayDeque<UUID>> factionQueues = new EnumMap<>(FactionRole.class);
    private final Set<UUID> playersPendingMatch = new HashSet<>();

    private final WorldProvider worldProvider;
    private final TimeProvider timeProvider;
    private final long warDuration;
    private final int minPlayersPerFaction;
    private final int scoreToWin;
    private final int maxPlayersPerWar;
    private final int maxPlayersPerFaction;
    private final String queueArenaId;
    private final ArenaRegistry arenaRegistry;
    private volatile boolean matchmakingInProgress;

    public BattlegroundsManager(long warDuration, ArenaRegistry arenaRegistry) {
        this(warDuration, DEFAULT_MIN_PLAYERS_PER_FACTION, DEFAULT_SCORE_TO_WIN, DEFAULT_QUEUE_ARENA_ID, DEFAULT_MAX_PLAYERS_PER_WAR, arenaRegistry);
    }

    public BattlegroundsManager(long warDuration, int minPlayersPerFaction, int scoreToWin, String queueArenaId, ArenaRegistry arenaRegistry) {
        this(warDuration, minPlayersPerFaction, scoreToWin, queueArenaId, DEFAULT_MAX_PLAYERS_PER_WAR, arenaRegistry);
    }

    public BattlegroundsManager(long warDuration, int minPlayersPerFaction, int scoreToWin, String queueArenaId,
                                int maxPlayersPerWar, ArenaRegistry arenaRegistry) {
        this(new HytaleWorldProvider(), new SystemTimeProvider(),
            warDuration, minPlayersPerFaction, scoreToWin, queueArenaId, maxPlayersPerWar, arenaRegistry);
    }

    public BattlegroundsManager(WorldProvider worldProvider, TimeProvider timeProvider, long warDuration,
                                int minPlayersPerFaction, int scoreToWin, String queueArenaId,
                                int maxPlayersPerWar, ArenaRegistry arenaRegistry) {
        this.worldProvider = worldProvider;
        this.timeProvider = timeProvider;
        this.warDuration = warDuration;
        this.maxPlayersPerWar = Math.max(2, maxPlayersPerWar);
        this.maxPlayersPerFaction = Math.max(1, this.maxPlayersPerWar / 2);
        this.minPlayersPerFaction = Math.min(Math.max(1, minPlayersPerFaction), this.maxPlayersPerFaction);
        this.scoreToWin = Math.max(1, scoreToWin);
        this.queueArenaId = queueArenaId == null || queueArenaId.isBlank() ? DEFAULT_QUEUE_ARENA_ID : queueArenaId;
        this.arenaRegistry = arenaRegistry;
        this.factionQueues.put(FactionRole.RED, new ArrayDeque<>());
        this.factionQueues.put(FactionRole.BLUE, new ArrayDeque<>());
    }

    public CompletableFuture<FactionWar> createWar(String arenaId, World currentWorld, Transform returnTransform) {
        UUID warId = UUID.randomUUID();
        ArenaConfig config = this.arenaRegistry.getOrDefault(arenaId);
        ArrayList<Vector3i> flagPositions = new ArrayList<>();
        for (Vec3i pos : config.flagPositions()) {
            flagPositions.add(pos.toVector3i());
        }
        return this.worldProvider.createArenaWorld(warId, currentWorld, returnTransform).thenApply(world -> {
            FactionWar war = new FactionWar(warId, this.timeProvider, this.warDuration, world,
                config.redSpawn().toVector3i(), config.blueSpawn().toVector3i(), flagPositions);
            this.activeWars.put(warId, war);
            return war;
        });
    }

    public QueueJoinResult queuePlayer(PlayerRef playerRef, FactionRole faction, World currentWorld, Transform returnTransform) {
        if (playerRef == null || faction == null || currentWorld == null) {
            return new QueueJoinResult(QueueJoinStatus.INVALID_REQUEST, 0, 0, 0, this.minPlayersPerFaction, null, 0, 0);
        }

        UUID playerId = playerRef.getUuid();
        synchronized (this.queueLock) {
            if (this.playerToWar.containsKey(playerId)) {
                return new QueueJoinResult(QueueJoinStatus.ALREADY_IN_WAR, 0, 0, 0, this.minPlayersPerFaction, null, 0, 0);
            }
            if (this.queuedPlayers.containsKey(playerId)) {
                QueueEntry existing = this.queuedPlayers.get(playerId);
                int position = this.getQueuePositionLocked(playerId, existing.faction());
                return new QueueJoinResult(QueueJoinStatus.ALREADY_QUEUED, position,
                    this.factionQueues.get(existing.faction()).size(),
                    this.factionQueues.get(existing.faction().getOpposite()).size(),
                    this.minPlayersPerFaction,
                    null,
                    0,
                    0);
            }
            if (this.playersPendingMatch.contains(playerId)) {
                return new QueueJoinResult(QueueJoinStatus.MATCHMAKING, 0, 0, 0, this.minPlayersPerFaction, null, 0, 0);
            }

            Transform safeReturnTransform = this.copyTransform(returnTransform);
            QueueJoinResult joinedActiveWar = this.tryJoinActiveWarLocked(
                playerRef,
                faction,
                currentWorld.getWorldConfig().getUuid(),
                safeReturnTransform
            );
            if (joinedActiveWar != null) {
                return joinedActiveWar;
            }

            QueueEntry entry = new QueueEntry(
                playerId,
                faction,
                currentWorld.getWorldConfig().getUuid(),
                safeReturnTransform
            );
            this.queuedPlayers.put(playerId, entry);
            ArrayDeque<UUID> queue = this.factionQueues.get(faction);
            queue.addLast(playerId);

            return new QueueJoinResult(
                QueueJoinStatus.QUEUED,
                queue.size(),
                queue.size(),
                this.factionQueues.get(faction.getOpposite()).size(),
                this.minPlayersPerFaction,
                null,
                0,
                0
            );
        }
    }

    private QueueJoinResult tryJoinActiveWarLocked(PlayerRef playerRef, FactionRole faction, UUID returnWorldUuid, Transform returnTransform) {
        UUID playerId = playerRef.getUuid();
        for (FactionWar war : this.activeWars.values()) {
            if (!this.canJoinActiveWar(war, faction)) {
                continue;
            }
            if (!war.addPlayer(playerId, faction)) {
                continue;
            }

            this.playerToWar.put(playerId, war.getWarId());
            this.playerReturnStates.put(playerId, new PlayerReturnState(returnWorldUuid, returnTransform));

            Transform spawnTransform = this.createSpawnTransform(war.getSpawnForFaction(faction));
            this.teleportPlayer(playerRef, war.getArenaWorld(), spawnTransform);
            this.scheduleHudShow(playerId, war.getArenaWorld());

            return new QueueJoinResult(
                QueueJoinStatus.JOINED_ACTIVE_WAR,
                0,
                0,
                0,
                this.minPlayersPerFaction,
                war.getWarId(),
                war.getFaction(faction).getPlayerCount(),
                war.getTotalPlayerCount()
            );
        }
        return null;
    }

    private boolean canJoinActiveWar(FactionWar war, FactionRole faction) {
        if (war == null || faction == null || !war.isActive() || war.getArenaWorld() == null) {
            return false;
        }
        if (war.getTotalPlayerCount() >= this.maxPlayersPerWar) {
            return false;
        }
        return war.getFaction(faction).getPlayerCount() < this.maxPlayersPerFaction;
    }

    public void processMatchmaking() {
        this.processMatchmakingInternal(false);
    }

    public ForceStartResult forceStartFromQueue() {
        return this.processMatchmakingInternal(true);
    }

    public FactionWar createWarInWorld(World world, Vector3i redSpawn, Vector3i blueSpawn, List<Vector3i> flagPositions) {
        UUID warId = UUID.randomUUID();
        FactionWar war = new FactionWar(warId, this.timeProvider, this.warDuration, world, redSpawn, blueSpawn, flagPositions);
        this.activeWars.put(warId, war);
        return war;
    }

    public CompletableFuture<Void> endWar(UUID warId) {
        FactionWar war = this.activeWars.remove(warId);
        if (war == null) {
            return CompletableFuture.completedFuture(null);
        }
        war.end();
        for (UUID playerId : war.getAllPlayers()) {
            this.playerToWar.remove(playerId);
            this.hideHud(playerId);
            this.teleportPlayerToReturnState(playerId);
        }
        World world = war.getArenaWorld();
        if (world == null) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> destroyFuture = new CompletableFuture<>();
        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> this.worldProvider.destroyArenaWorld(world)
            .whenComplete((ignored, throwable) -> {
                if (throwable != null) {
                    destroyFuture.completeExceptionally(throwable);
                } else {
                    destroyFuture.complete(null);
                }
            }), INSTANCE_DESTROY_DELAY_MS, TimeUnit.MILLISECONDS);

        return destroyFuture;
    }

    public boolean joinWar(UUID playerId, UUID warId, FactionRole faction) {
        FactionWar war = this.activeWars.get(warId);
        if (war == null || !war.isActive()) {
            return false;
        }
        // Atomic check-and-put — only one thread can claim this player
        if (this.playerToWar.putIfAbsent(playerId, warId) != null) {
            return false;
        }
        if (war.addPlayer(playerId, faction)) {
            return true;
        }
        // war.addPlayer failed — rollback the playerToWar entry
        this.playerToWar.remove(playerId, warId);
        return false;
    }

    public boolean leaveWar(UUID playerId) {
        UUID warId = this.playerToWar.remove(playerId);
        if (warId == null) {
            return false;
        }
        this.hideHud(playerId);
        FactionWar war = this.activeWars.get(warId);
        if (war != null) {
            war.removePlayer(playerId);
            if (war.getTotalPlayerCount() == 0) {
                this.endWar(warId);
            }
        }
        this.teleportPlayerToReturnState(playerId);
        return true;
    }

    public boolean unqueuePlayer(UUID playerId) {
        synchronized (this.queueLock) {
            QueueEntry entry = this.queuedPlayers.remove(playerId);
            if (entry == null) {
                return false;
            }
            this.factionQueues.get(entry.faction()).remove(playerId);
            return true;
        }
    }

    public boolean isPlayerQueued(UUID playerId) {
        synchronized (this.queueLock) {
            return this.queuedPlayers.containsKey(playerId);
        }
    }

    public boolean isPlayerPendingMatch(UUID playerId) {
        synchronized (this.queueLock) {
            return this.playersPendingMatch.contains(playerId);
        }
    }

    public FactionRole getQueuedFaction(UUID playerId) {
        synchronized (this.queueLock) {
            QueueEntry entry = this.queuedPlayers.get(playerId);
            return entry != null ? entry.faction() : null;
        }
    }

    public int getQueuePosition(UUID playerId) {
        synchronized (this.queueLock) {
            QueueEntry entry = this.queuedPlayers.get(playerId);
            if (entry == null) {
                return -1;
            }
            return this.getQueuePositionLocked(playerId, entry.faction());
        }
    }

    public QueueSnapshot getQueueSnapshot() {
        synchronized (this.queueLock) {
            return new QueueSnapshot(
                this.factionQueues.get(FactionRole.RED).size(),
                this.factionQueues.get(FactionRole.BLUE).size(),
                this.minPlayersPerFaction
            );
        }
    }

    public FactionWar getWarByPlayer(UUID playerId) {
        UUID warId = this.playerToWar.get(playerId);
        return warId != null ? this.activeWars.get(warId) : null;
    }

    public boolean isPlayerInWar(UUID playerId) {
        return this.playerToWar.containsKey(playerId);
    }

    public FactionWar getWar(UUID warId) {
        return this.activeWars.get(warId);
    }

    public Map<UUID, FactionWar> getActiveWars() {
        return Collections.unmodifiableMap(this.activeWars);
    }

    public int getActiveWarCount() {
        return this.activeWars.size();
    }

    public ArenaRegistry getArenaRegistry() {
        return this.arenaRegistry;
    }

    public ArenaConfig getArenaConfig(String arenaId) {
        return this.arenaRegistry.getOrDefault(arenaId);
    }

    public long getWarDuration() {
        return this.warDuration;
    }

    public int getMinPlayersPerFaction() {
        return this.minPlayersPerFaction;
    }

    public int getScoreToWin() {
        return this.scoreToWin;
    }

    public int getMaxPlayersPerWar() {
        return this.maxPlayersPerWar;
    }

    public int getMaxPlayersPerFaction() {
        return this.maxPlayersPerFaction;
    }

    public String getQueueArenaId() {
        return this.queueArenaId;
    }

    public boolean hasReachedScoreLimit(FactionWar war) {
        if (war == null) {
            return false;
        }
        return war.getRedFaction().getPoints() >= this.scoreToWin || war.getBlueFaction().getPoints() >= this.scoreToWin;
    }

    public List<FactionWar> getExpiredWars() {
        ArrayList<FactionWar> expired = new ArrayList<>();
        for (FactionWar war : this.activeWars.values()) {
            if (!war.isExpired()) continue;
            expired.add(war);
        }
        return expired;
    }

    private void startQueuedMatch(List<QueueEntry> redEntries, List<QueueEntry> blueEntries, boolean forceStart) {
        List<QueueEntry> participants = new ArrayList<>(redEntries.size() + blueEntries.size());
        participants.addAll(redEntries);
        participants.addAll(blueEntries);

        List<QueueEntry> validRed = this.filterOnlineEntries(redEntries);
        List<QueueEntry> validBlue = this.filterOnlineEntries(blueEntries);
        if (!forceStart && (validRed.size() < this.minPlayersPerFaction || validBlue.size() < this.minPlayersPerFaction)) {
            LOGGER.log(Level.WARNING, "Matchmaking aborted: not enough valid players (red={0}, blue={1})", new Object[]{validRed.size(), validBlue.size()});
            this.notifyParticipants(participants, "Matchmaking failed: not enough online players.");
            this.completeMatchmaking(participants, true);
            return;
        }
        if (forceStart && validRed.isEmpty() && validBlue.isEmpty()) {
            LOGGER.log(Level.WARNING, "Force start aborted: no valid online players");
            this.notifyParticipants(participants, "Force start failed: no online players found in queue.");
            this.completeMatchmaking(participants, true);
            return;
        }

        World seedWorld = this.resolveSeedWorld(validRed, validBlue);
        if (seedWorld == null) {
            LOGGER.log(Level.WARNING, "Matchmaking aborted: could not resolve seed world");
            this.notifyParticipants(participants, "Matchmaking failed: could not find a valid world.");
            this.completeMatchmaking(participants, true);
            return;
        }
        Transform seedTransform = this.resolveSeedTransform(validRed, validBlue);

        LOGGER.log(Level.FINE, "Creating arena war (arena={0}, red={1}, blue={2})", new Object[]{this.queueArenaId, validRed.size(), validBlue.size()});
        this.createWar(this.queueArenaId, seedWorld, seedTransform).thenAccept(war -> {
            LOGGER.log(Level.FINE, "Arena world created: {0}, waiting for world to initialize...", war.getWarId());
            this.notifyParticipants(participants, "Arena created! Teleporting in 3 seconds...", Color.YELLOW);
            // Delay teleport to let the arena world fully load chunks
            HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
                LOGGER.log(Level.FINE, "Teleporting participants to war: {0}", war.getWarId());
                int redCount = this.addParticipantsToWar(war, validRed, FactionRole.RED);
                int blueCount = this.addParticipantsToWar(war, validBlue, FactionRole.BLUE);
                LOGGER.log(Level.FINE, "Added participants to war: red={0}, blue={1}", new Object[]{redCount, blueCount});
                if (!forceStart && (redCount == 0 || blueCount == 0)) {
                    LOGGER.log(Level.WARNING, "War cancelled: faction has no players (red={0}, blue={1})", new Object[]{redCount, blueCount});
                    this.endWar(war.getWarId());
                } else if (forceStart && redCount + blueCount == 0) {
                    LOGGER.log(Level.WARNING, "Force-started war cancelled: no players could be added");
                    this.notifyParticipants(participants, "Battleground failed: could not teleport any players.");
                    this.endWar(war.getWarId());
                }
                this.completeMatchmaking(participants, false);
                this.processMatchmaking();
            }, 3, TimeUnit.SECONDS);
        }).exceptionally(ex -> {
            LOGGER.log(Level.SEVERE, "Failed to create arena world", ex);
            this.notifyParticipants(participants, "Battleground failed: " + ex.getMessage());
            this.completeMatchmaking(participants, true);
            return null;
        });
    }

    private int addParticipantsToWar(FactionWar war, List<QueueEntry> participants, FactionRole faction) {
        int added = 0;
        String shortId = war.getWarId().toString().substring(0, 8);
        Transform spawnTransform = this.createSpawnTransform(war.getSpawnForFaction(faction));
        for (QueueEntry entry : participants) {
            PlayerRef playerRef = this.getOnlinePlayer(entry.playerId());
            if (playerRef == null) {
                continue;
            }
            if (!war.addPlayer(entry.playerId(), faction)) {
                continue;
            }

            this.playerToWar.put(entry.playerId(), war.getWarId());
            this.playerReturnStates.put(entry.playerId(), new PlayerReturnState(entry.returnWorldUuid(), entry.returnTransform()));
            playerRef.sendMessage(BattlegroundsMessages.matchFound(shortId, faction, participants.size()));
            this.teleportPlayer(playerRef, war.getArenaWorld(), spawnTransform);
            this.scheduleHudShow(entry.playerId(), war.getArenaWorld());
            added++;
        }
        return added;
    }

    private void completeMatchmaking(List<QueueEntry> participants, boolean requeueParticipants) {
        synchronized (this.queueLock) {
            for (QueueEntry entry : participants) {
                this.playersPendingMatch.remove(entry.playerId());
            }
            if (requeueParticipants) {
                this.requeueEntriesLocked(participants);
            }
            this.matchmakingInProgress = false;
        }
    }

    private void notifyParticipants(List<QueueEntry> participants, String message) {
        this.notifyParticipants(participants, message, Color.RED);
    }

    private void notifyParticipants(List<QueueEntry> participants, String message, Color color) {
        for (QueueEntry entry : participants) {
            PlayerRef playerRef = this.getOnlinePlayer(entry.playerId());
            if (playerRef != null) {
                playerRef.sendMessage(Message.raw(message).color(color));
            }
        }
    }

    private List<QueueEntry> filterOnlineEntries(List<QueueEntry> entries) {
        ArrayList<QueueEntry> filtered = new ArrayList<>(entries.size());
        for (QueueEntry entry : entries) {
            if (this.getOnlinePlayer(entry.playerId()) != null) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    private World resolveSeedWorld(List<QueueEntry> redEntries, List<QueueEntry> blueEntries) {
        Universe universe = Universe.get();
        if (universe == null) {
            return null;
        }

        for (QueueEntry entry : redEntries) {
            World world = universe.getWorld(entry.returnWorldUuid());
            if (world != null) {
                return world;
            }
        }
        for (QueueEntry entry : blueEntries) {
            World world = universe.getWorld(entry.returnWorldUuid());
            if (world != null) {
                return world;
            }
        }
        return universe.getDefaultWorld();
    }

    private Transform resolveSeedTransform(List<QueueEntry> redEntries, List<QueueEntry> blueEntries) {
        if (!redEntries.isEmpty()) {
            return redEntries.get(0).returnTransform();
        }
        if (!blueEntries.isEmpty()) {
            return blueEntries.get(0).returnTransform();
        }
        return this.createDefaultTransform();
    }

    private void cleanupStaleQueuedPlayersLocked() {
        Universe universe = Universe.get();
        if (universe == null) {
            return;
        }

        Iterator<Map.Entry<UUID, QueueEntry>> iterator = this.queuedPlayers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, QueueEntry> entry = iterator.next();
            if (universe.getPlayer(entry.getKey()) != null) {
                continue;
            }
            this.factionQueues.get(entry.getValue().faction()).remove(entry.getKey());
            iterator.remove();
        }
    }

    private List<QueueEntry> popQueuedEntriesLocked(FactionRole faction, int count) {
        ArrayList<QueueEntry> popped = new ArrayList<>(count);
        ArrayDeque<UUID> queue = this.factionQueues.get(faction);
        while (popped.size() < count && !queue.isEmpty()) {
            UUID playerId = queue.pollFirst();
            QueueEntry entry = this.queuedPlayers.remove(playerId);
            if (entry != null) {
                popped.add(entry);
            }
        }
        return popped;
    }

    private void requeueEntriesLocked(List<QueueEntry> entries) {
        Universe universe = Universe.get();
        for (QueueEntry entry : entries) {
            if (entry == null) {
                continue;
            }

            UUID playerId = entry.playerId();
            if (this.playerToWar.containsKey(playerId) || this.queuedPlayers.containsKey(playerId)) {
                continue;
            }
            if (universe != null && universe.getPlayer(playerId) == null) {
                continue;
            }

            this.queuedPlayers.put(playerId, entry);
            this.factionQueues.get(entry.faction()).addLast(playerId);
        }
    }

    private int getQueuePositionLocked(UUID playerId, FactionRole faction) {
        int position = 1;
        for (UUID queuedId : this.factionQueues.get(faction)) {
            if (queuedId.equals(playerId)) {
                return position;
            }
            position++;
        }
        return -1;
    }

    private void teleportPlayerToReturnState(UUID playerId) {
        PlayerReturnState returnState = this.playerReturnStates.remove(playerId);
        if (returnState == null) {
            return;
        }

        PlayerRef playerRef = this.getOnlinePlayer(playerId);
        if (playerRef == null) {
            return;
        }

        Universe universe = Universe.get();
        if (universe == null) {
            return;
        }

        World returnWorld = universe.getWorld(returnState.worldUuid());
        if (returnWorld == null) {
            returnWorld = universe.getDefaultWorld();
        }
        if (returnWorld == null) {
            return;
        }

        this.teleportPlayer(playerRef, returnWorld, returnState.returnTransform());
        playerRef.sendMessage(BattlegroundsMessages.returnedFromWar());
    }

    private void teleportPlayer(PlayerRef playerRef, World targetWorld, Transform targetTransform) {
        Ref<EntityStore> playerEntityRef = playerRef.getReference();
        if (playerEntityRef == null || !playerEntityRef.isValid()) {
            return;
        }

        Store<EntityStore> store = playerEntityRef.getStore();
        World currentWorld = store.getExternalData().getWorld();
        currentWorld.execute(() -> {
            Ref<EntityStore> freshRef = playerRef.getReference();
            if (freshRef == null || !freshRef.isValid()) {
                return;
            }

            Store<EntityStore> freshStore = freshRef.getStore();
            InteractionManager interactionManager = freshStore.getComponent(
                freshRef, InteractionModule.get().getInteractionManagerComponent());
            if (interactionManager != null) {
                interactionManager.clear();
            }

            freshStore.addComponent(freshRef, Teleport.getComponentType(),
                Teleport.createForPlayer(targetWorld, targetTransform.getPosition(), targetTransform.getRotation()));
        });
    }

    private PlayerRef getOnlinePlayer(UUID playerId) {
        Universe universe = Universe.get();
        if (universe == null) {
            return null;
        }
        PlayerRef playerRef = universe.getPlayer(playerId);
        if (playerRef == null) {
            return null;
        }
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return null;
        }
        return playerRef;
    }

    private Transform createSpawnTransform(Vector3i position) {
        return new Transform(
            new Vector3d(position.getX() + 0.5, position.getY(), position.getZ() + 0.5),
            new Vector3f(0, 0, 0)
        );
    }

    private Transform copyTransform(Transform transform) {
        if (transform == null) {
            return this.createDefaultTransform();
        }
        return new Transform(
            new Vector3d(transform.getPosition()),
            new Vector3f(transform.getRotation())
        );
    }

    private Transform createDefaultTransform() {
        return new Transform(new Vector3d(0, 100, 0), new Vector3f(0, 0, 0));
    }

    private ForceStartResult processMatchmakingInternal(boolean forceStart) {
        List<QueueEntry> redEntries;
        List<QueueEntry> blueEntries;
        int redToTake;
        int blueToTake;

        synchronized (this.queueLock) {
            if (this.matchmakingInProgress) {
                return ForceStartResult.MATCHMAKING_IN_PROGRESS;
            }

            this.cleanupStaleQueuedPlayersLocked();

            int redQueued = this.factionQueues.get(FactionRole.RED).size();
            int blueQueued = this.factionQueues.get(FactionRole.BLUE).size();

            if (forceStart) {
                if (redQueued + blueQueued < 1) {
                    return ForceStartResult.INSUFFICIENT_QUEUE;
                }
                redToTake = Math.min(redQueued, this.maxPlayersPerFaction);
                blueToTake = Math.min(blueQueued, this.maxPlayersPerFaction);
            } else {
                if (redQueued < this.minPlayersPerFaction || blueQueued < this.minPlayersPerFaction) {
                    return ForceStartResult.INSUFFICIENT_QUEUE;
                }
                int participantsPerFaction = Math.min(Math.min(redQueued, blueQueued), this.maxPlayersPerFaction);
                redToTake = participantsPerFaction;
                blueToTake = participantsPerFaction;
            }
            if (redToTake + blueToTake < 1) {
                return ForceStartResult.INSUFFICIENT_QUEUE;
            }

            redEntries = this.popQueuedEntriesLocked(FactionRole.RED, redToTake);
            blueEntries = this.popQueuedEntriesLocked(FactionRole.BLUE, blueToTake);
            if (redEntries.size() != redToTake || blueEntries.size() != blueToTake) {
                this.requeueEntriesLocked(redEntries);
                this.requeueEntriesLocked(blueEntries);
                return ForceStartResult.INSUFFICIENT_QUEUE;
            }

            for (QueueEntry entry : redEntries) {
                this.playersPendingMatch.add(entry.playerId());
            }
            for (QueueEntry entry : blueEntries) {
                this.playersPendingMatch.add(entry.playerId());
            }

            this.matchmakingInProgress = true;
        }

        this.startQueuedMatch(redEntries, blueEntries, forceStart);
        return forceStart ? ForceStartResult.STARTED : ForceStartResult.NOOP;
    }

    // ═══════════════════════════════════════════════════════
    // HUD MANAGEMENT
    // ═══════════════════════════════════════════════════════

    /**
     * Schedule showing the scoreboard HUD for a player after teleport delay.
     */
    public void scheduleHudShow(UUID playerId, World arenaWorld) {
        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> {
            if (arenaWorld == null) return;
            arenaWorld.execute(() -> {
                PlayerRef playerRef = this.getOnlinePlayer(playerId);
                if (playerRef == null) return;

                Ref<EntityStore> ref = playerRef.getReference();
                if (ref == null || !ref.isValid()) return;

                Player player = ref.getStore().getComponent(ref, Player.getComponentType());
                if (player == null) return;

                // Hide quest objective panel
                player.getHudManager().hideHudComponents(playerRef, HudComponent.ObjectivePanel);

                // Create and show scoreboard HUD
                BattlegroundsHud hud = new BattlegroundsHud(playerRef);
                activeHuds.put(playerId, hud);
                HudWrapper.setCustomHud(player, playerRef, BattlegroundsHud.getHudId(), hud);
            });
        }, HUD_SHOW_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Hide the scoreboard HUD for a player.
     */
    public void hideHud(UUID playerId) {
        BattlegroundsHud hud = activeHuds.remove(playerId);
        if (hud != null) {
            PlayerRef playerRef = this.getOnlinePlayer(playerId);
            if (playerRef != null) {
                Ref<EntityStore> ref = playerRef.getReference();
                if (ref != null && ref.isValid()) {
                    Player player = ref.getStore().getComponent(ref, Player.getComponentType());
                    if (player != null) {
                        HudWrapper.hideCustomHud(player, BattlegroundsHud.getHudId());
                        // Restore quest objective panel
                        player.getHudManager().showHudComponents(playerRef, HudComponent.ObjectivePanel);
                        return;
                    }
                }
            }
            // Fallback: visual hide only
            hud.hide();
        }
    }

    /**
     * Refresh all active scoreboard HUDs for a given war.
     */
    public void refreshWarHuds(FactionWar war) {
        if (war == null) return;
        for (UUID playerId : war.getAllPlayers()) {
            BattlegroundsHud hud = activeHuds.get(playerId);
            if (hud != null) {
                try {
                    hud.refresh(war, this.scoreToWin);
                } catch (Exception ignored) {
                    // Player may be transitioning worlds
                }
            }
        }
    }

    /**
     * Refresh all HUDs across all active wars.
     */
    public void refreshAllHuds() {
        for (FactionWar war : activeWars.values()) {
            refreshWarHuds(war);
        }
    }

    private record QueueEntry(UUID playerId, FactionRole faction, UUID returnWorldUuid, Transform returnTransform) {
    }

    private record PlayerReturnState(UUID worldUuid, Transform returnTransform) {
    }

    public enum QueueJoinStatus {
        JOINED_ACTIVE_WAR,
        QUEUED,
        ALREADY_QUEUED,
        ALREADY_IN_WAR,
        MATCHMAKING,
        INVALID_REQUEST
    }

    public record QueueJoinResult(
        QueueJoinStatus status,
        int queuePosition,
        int sameFactionQueued,
        int opposingFactionQueued,
        int minimumPerFaction,
        UUID activeWarId,
        int factionPlayersInWar,
        int totalPlayersInWar
    ) {
    }

    public record QueueSnapshot(int redQueued, int blueQueued, int minimumPerFaction) {
        public int queuedFor(FactionRole faction) {
            return faction == FactionRole.RED ? this.redQueued : this.blueQueued;
        }
    }

    public enum ForceStartResult {
        STARTED,
        INSUFFICIENT_QUEUE,
        MATCHMAKING_IN_PROGRESS,
        NOOP
    }
}
