package com.hcbattlegrounds.models;

import com.hcbattlegrounds.utils.TimeProvider;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FactionWar {
    public static final int FLAG_COUNT = 4;
    private final UUID warId;
    private final Faction redFaction;
    private final Faction blueFaction;
    private final CaptureFlag[] flags;
    private final TimeProvider timeProvider;
    private final long startTime;
    private final long warDuration;
    private final World arenaWorld;
    private final Vector3i redSpawn;
    private final Vector3i blueSpawn;
    private boolean active;
    private final Map<UUID, Integer> playerKills = new ConcurrentHashMap<>();

    public FactionWar(UUID warId, TimeProvider timeProvider, long warDuration, World arenaWorld, Vector3i redSpawn, Vector3i blueSpawn, List<Vector3i> flagPositions) {
        this.warId = warId;
        this.redFaction = new Faction(FactionRole.RED);
        this.blueFaction = new Faction(FactionRole.BLUE);
        this.timeProvider = timeProvider;
        this.startTime = timeProvider.now();
        this.warDuration = warDuration;
        this.arenaWorld = arenaWorld;
        this.redSpawn = redSpawn;
        this.blueSpawn = blueSpawn;
        this.active = true;
        this.flags = new CaptureFlag[4];
        for (int i = 0; i < 4; ++i) {
            Vector3i pos = i < flagPositions.size() ? flagPositions.get(i) : new Vector3i(0, 60, 0);
            this.flags[i] = new CaptureFlag(i, pos);
        }
    }

    public UUID getWarId() {
        return this.warId;
    }

    public World getArenaWorld() {
        return this.arenaWorld;
    }

    public String getWorldName() {
        return this.arenaWorld != null ? this.arenaWorld.getName() : "unknown";
    }

    public Vector3i getRedSpawn() {
        return this.redSpawn;
    }

    public Vector3i getBlueSpawn() {
        return this.blueSpawn;
    }

    public Vector3i getSpawnForFaction(FactionRole role) {
        return role == FactionRole.RED ? this.redSpawn : this.blueSpawn;
    }

    public boolean isActive() {
        return this.active && !this.isExpired();
    }

    public boolean isExpired() {
        return this.timeProvider.now() - this.startTime >= this.warDuration;
    }

    public long getRemainingTime() {
        long remaining = this.warDuration - (this.timeProvider.now() - this.startTime);
        return Math.max(0L, remaining);
    }

    public void end() {
        this.active = false;
    }

    public Faction getRedFaction() {
        return this.redFaction;
    }

    public Faction getBlueFaction() {
        return this.blueFaction;
    }

    public Faction getFaction(FactionRole role) {
        return role == FactionRole.RED ? this.redFaction : this.blueFaction;
    }

    public boolean addPlayer(UUID playerId, FactionRole role) {
        return this.getFaction(role).addPlayer(playerId);
    }

    public boolean removePlayer(UUID playerId) {
        return this.redFaction.removePlayer(playerId) || this.blueFaction.removePlayer(playerId);
    }

    public boolean isInWar(UUID playerId) {
        return this.redFaction.hasPlayer(playerId) || this.blueFaction.hasPlayer(playerId);
    }

    public FactionRole getPlayerFaction(UUID playerId) {
        if (this.redFaction.hasPlayer(playerId)) {
            return FactionRole.RED;
        }
        if (this.blueFaction.hasPlayer(playerId)) {
            return FactionRole.BLUE;
        }
        return null;
    }

    public Faction getPlayerTeam(UUID playerId) {
        if (this.redFaction.hasPlayer(playerId)) {
            return this.redFaction;
        }
        if (this.blueFaction.hasPlayer(playerId)) {
            return this.blueFaction;
        }
        return null;
    }

    public boolean isSameTeam(UUID playerA, UUID playerB) {
        FactionRole roleA = this.getPlayerFaction(playerA);
        FactionRole roleB = this.getPlayerFaction(playerB);
        return roleA != null && roleA == roleB;
    }

    public void addPoints(UUID playerId, int amount) {
        Faction faction = this.getPlayerTeam(playerId);
        if (faction != null) {
            faction.addPoints(amount);
        }
    }

    public void addPointsToFaction(FactionRole role, int amount) {
        this.getFaction(role).addPoints(amount);
    }

    public CaptureFlag[] getFlags() {
        return this.flags;
    }

    public CaptureFlag getFlag(int index) {
        if (index < 0 || index >= 4) {
            return null;
        }
        return this.flags[index];
    }

    public int getControlledFlagCount(FactionRole role) {
        int count = 0;
        for (CaptureFlag flag : this.flags) {
            if (!flag.isControlledBy(role)) continue;
            ++count;
        }
        return count;
    }

    public List<CaptureFlag> getControlledFlags(FactionRole role) {
        ArrayList<CaptureFlag> controlled = new ArrayList<>();
        for (CaptureFlag flag : this.flags) {
            if (!flag.isControlledBy(role)) continue;
            controlled.add(flag);
        }
        return controlled;
    }

    public Faction getWinner() {
        int redPoints = this.redFaction.getPoints();
        int bluePoints = this.blueFaction.getPoints();
        if (redPoints > bluePoints) {
            return this.redFaction;
        }
        if (bluePoints > redPoints) {
            return this.blueFaction;
        }
        return null;
    }

    public FactionRole getWinningRole() {
        Faction winner = this.getWinner();
        return winner != null ? winner.getRole() : null;
    }

    public FactionRole getLeadingFaction() {
        int redPoints = this.redFaction.getPoints();
        int bluePoints = this.blueFaction.getPoints();
        if (redPoints > bluePoints) {
            return FactionRole.RED;
        }
        if (bluePoints > redPoints) {
            return FactionRole.BLUE;
        }
        return null;
    }

    public Set<UUID> getAllPlayers() {
        HashSet<UUID> all = new HashSet<>();
        all.addAll(this.redFaction.getPlayers());
        all.addAll(this.blueFaction.getPlayers());
        return all;
    }

    public int getTotalPlayerCount() {
        return this.redFaction.getPlayerCount() + this.blueFaction.getPlayerCount();
    }

    public void recordKill(UUID playerId) {
        this.playerKills.merge(playerId, 1, Integer::sum);
    }

    public int getPlayerKills(UUID playerId) {
        return this.playerKills.getOrDefault(playerId, 0);
    }
}
