package com.hcbattlegrounds.models;

import java.awt.Color;

public enum FactionRole {
    RED("Red", new Color(255, 85, 85)),
    BLUE("Blue", new Color(85, 85, 255));

    private final String displayName;
    private final Color color;

    FactionRole(String displayName, Color color) {
        this.displayName = displayName;
        this.color = color;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public Color getColor() {
        return this.color;
    }

    public FactionRole getOpposite() {
        return this == RED ? BLUE : RED;
    }
}
