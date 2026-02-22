package com.hcbattlegrounds.models;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Faction {
    private final FactionRole role;
    private final Set<UUID> players;
    private int points;

    public Faction(FactionRole role) {
        this.role = role;
        this.players = new HashSet<>();
        this.points = 0;
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
        return this.points;
    }

    public void addPoints(int amount) {
        this.points += amount;
    }

    public void reset() {
        this.points = 0;
        this.players.clear();
    }
}
