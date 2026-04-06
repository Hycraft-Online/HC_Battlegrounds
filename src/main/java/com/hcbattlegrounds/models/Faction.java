package com.hcbattlegrounds.models;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Faction {
    private final FactionRole role;
    private final Set<UUID> players;
    private final AtomicInteger points;

    public Faction(FactionRole role) {
        this.role = role;
        this.players = ConcurrentHashMap.newKeySet();
        this.points = new AtomicInteger(0);
    }

    public FactionRole getRole() {
        return this.role;
    }

    public boolean addPlayer(UUID playerId) {
        return this.players.add(playerId);
    }

    public boolean removePlayer(UUID playerId) {
        return this.players.remove(playerId);
    }

    public boolean hasPlayer(UUID playerId) {
        return this.players.contains(playerId);
    }

    public Set<UUID> getPlayers() {
        return Collections.unmodifiableSet(this.players);
    }

    public int getPlayerCount() {
        return this.players.size();
    }

    public int getPoints() {
        return this.points.get();
    }

    public void addPoints(int amount) {
        this.points.addAndGet(amount);
    }

    public void reset() {
        this.points.set(0);
        this.players.clear();
    }
}
