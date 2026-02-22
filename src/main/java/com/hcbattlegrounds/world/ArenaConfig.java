package com.hcbattlegrounds.world;

import com.hcbattlegrounds.models.Vec3i;
import java.util.ArrayList;
import java.util.List;

public record ArenaConfig(String id, String prefabName, Vec3i redSpawn, Vec3i blueSpawn, List<Vec3i> flagPositions) {
    public ArenaConfig {
        flagPositions = List.copyOf(flagPositions);
    }

    public static ArenaConfig createDefault(String id) {
        return new ArenaConfig(id, "FactionWar",
            new Vec3i(-50, 60, 0), new Vec3i(50, 60, 0),
            List.of(new Vec3i(-30, 60, -30), new Vec3i(30, 60, -30), new Vec3i(-30, 60, 30), new Vec3i(30, 60, 30)));
    }

    public ArenaConfig withRedSpawn(Vec3i spawn) {
        return new ArenaConfig(this.id, this.prefabName, spawn, this.blueSpawn, this.flagPositions);
    }

    public ArenaConfig withBlueSpawn(Vec3i spawn) {
        return new ArenaConfig(this.id, this.prefabName, this.redSpawn, spawn, this.flagPositions);
    }

    public ArenaConfig withFlagPosition(int index, Vec3i position) {
        ArrayList<Vec3i> newFlags = new ArrayList<>(this.flagPositions);
        if (index >= 0 && index < newFlags.size()) {
            newFlags.set(index, position);
        } else if (index == newFlags.size()) {
            newFlags.add(position);
        }
        return new ArenaConfig(this.id, this.prefabName, this.redSpawn, this.blueSpawn, newFlags);
    }

    public ArenaConfig withPrefabName(String prefab) {
        return new ArenaConfig(this.id, prefab, this.redSpawn, this.blueSpawn, this.flagPositions);
    }

    public int getFlagCount() {
        return this.flagPositions.size();
    }
}
