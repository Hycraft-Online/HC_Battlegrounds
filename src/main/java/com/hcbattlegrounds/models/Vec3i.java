package com.hcbattlegrounds.models;

import com.hypixel.hytale.math.vector.Vector3i;

public record Vec3i(int x, int y, int z) {
    public Vector3i toVector3i() {
        return new Vector3i(this.x, this.y, this.z);
    }

    public static Vec3i from(Vector3i v) {
        return new Vec3i(v.getX(), v.getY(), v.getZ());
    }
}
