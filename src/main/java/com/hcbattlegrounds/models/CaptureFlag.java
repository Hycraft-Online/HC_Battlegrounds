package com.hcbattlegrounds.models;

import com.hypixel.hytale.math.vector.Vector3i;

public class CaptureFlag {
    public static final int CAPTURE_RADIUS = 20;
    public static final int CAPTURE_RADIUS_SQ = 400;
    public static final int CAPTURE_THRESHOLD = 100;
    public static final int CAPTURE_RATE = 10;
    private final int flagIndex;
    private final Vector3i position;
    private FactionRole controllingFaction;
    private int redCaptureProgress;
    private int blueCaptureProgress;
    private FlagState currentState;
    private boolean wasContested;
    private int lastRedCount;
    private int lastBlueCount;

    public CaptureFlag(int flagIndex, Vector3i position) {
        this.flagIndex = flagIndex;
        this.position = position;
        this.controllingFaction = null;
        this.redCaptureProgress = 0;
        this.blueCaptureProgress = 0;
        this.currentState = FlagState.NEUTRAL;
        this.wasContested = false;
        this.lastRedCount = 0;
        this.lastBlueCount = 0;
    }

    public int getFlagIndex() {
        return this.flagIndex;
    }

    public Vector3i getPosition() {
        return this.position;
    }

    public synchronized FactionRole getControllingFaction() {
        return this.controllingFaction;
    }

    public synchronized boolean isControlled() {
        return this.controllingFaction != null;
    }

    public synchronized boolean isControlledBy(FactionRole role) {
        return this.controllingFaction == role;
    }

    public synchronized boolean updateCapture(int redPlayersNearby, int bluePlayersNearby) {
        int progressGain;
        FactionRole previousControl = this.controllingFaction;
        this.lastRedCount = redPlayersNearby;
        this.lastBlueCount = bluePlayersNearby;
        boolean bothPresent = redPlayersNearby > 0 && bluePlayersNearby > 0;
        boolean redOnly = redPlayersNearby > 0 && bluePlayersNearby == 0;
        boolean blueOnly = bluePlayersNearby > 0 && redPlayersNearby == 0;
        boolean nonePresent = redPlayersNearby == 0 && bluePlayersNearby == 0;
        if (bothPresent) {
            this.wasContested = true;
            this.currentState = this.controllingFaction != null ? FlagState.CONTESTED : FlagState.NEUTRAL;
            return false;
        }
        this.wasContested = false;
        if (nonePresent) {
            this.currentState = this.controllingFaction != null ? FlagState.CONTROLLED : FlagState.NEUTRAL;
            return false;
        }
        if (redOnly) {
            if (this.controllingFaction == FactionRole.RED) {
                this.currentState = FlagState.CONTROLLED;
            } else if (this.controllingFaction == FactionRole.BLUE) {
                this.currentState = FlagState.OVERTIME;
                progressGain = redPlayersNearby * 10;
                this.blueCaptureProgress = Math.max(0, this.blueCaptureProgress - progressGain);
                if (this.blueCaptureProgress == 0) {
                    this.redCaptureProgress = Math.min(100, this.redCaptureProgress + progressGain);
                    if (this.redCaptureProgress >= 100) {
                        this.controllingFaction = FactionRole.RED;
                        this.currentState = FlagState.CONTROLLED;
                    } else {
                        this.controllingFaction = null;
                        this.currentState = FlagState.CAPTURING;
                    }
                }
            } else {
                this.currentState = FlagState.CAPTURING;
                progressGain = redPlayersNearby * 10;
                this.redCaptureProgress = Math.min(100, this.redCaptureProgress + progressGain);
                if (this.redCaptureProgress >= 100) {
                    this.controllingFaction = FactionRole.RED;
                    this.currentState = FlagState.CONTROLLED;
                }
            }
        }
        if (blueOnly) {
            if (this.controllingFaction == FactionRole.BLUE) {
                this.currentState = FlagState.CONTROLLED;
            } else if (this.controllingFaction == FactionRole.RED) {
                this.currentState = FlagState.OVERTIME;
                progressGain = bluePlayersNearby * 10;
                this.redCaptureProgress = Math.max(0, this.redCaptureProgress - progressGain);
                if (this.redCaptureProgress == 0) {
                    this.blueCaptureProgress = Math.min(100, this.blueCaptureProgress + progressGain);
                    if (this.blueCaptureProgress >= 100) {
                        this.controllingFaction = FactionRole.BLUE;
                        this.currentState = FlagState.CONTROLLED;
                    } else {
                        this.controllingFaction = null;
                        this.currentState = FlagState.CAPTURING;
                    }
                }
            } else {
                this.currentState = FlagState.CAPTURING;
                progressGain = bluePlayersNearby * 10;
                this.blueCaptureProgress = Math.min(100, this.blueCaptureProgress + progressGain);
                if (this.blueCaptureProgress >= 100) {
                    this.controllingFaction = FactionRole.BLUE;
                    this.currentState = FlagState.CONTROLLED;
                }
            }
        }
        return previousControl != this.controllingFaction;
    }

    public synchronized int getPointsGenerated(int controllingPlayersNearby, int enemyPlayersNearby) {
        if (this.controllingFaction == null) {
            return 0;
        }
        if (enemyPlayersNearby > 0) {
            return 0;
        }
        if (controllingPlayersNearby == 0) {
            return 0;
        }
        return controllingPlayersNearby * (controllingPlayersNearby + 1) / 2;
    }

    public synchronized boolean isContested() {
        return this.currentState == FlagState.CONTESTED || this.wasContested;
    }

    public synchronized FlagState getState() {
        return this.currentState;
    }

    public synchronized FactionRole getCapturingTeam() {
        if (this.currentState == FlagState.CAPTURING || this.currentState == FlagState.OVERTIME) {
            if (this.lastRedCount > this.lastBlueCount) {
                return FactionRole.RED;
            }
            if (this.lastBlueCount > this.lastRedCount) {
                return FactionRole.BLUE;
            }
        }
        return null;
    }

    public synchronized int getLastRedCount() {
        return this.lastRedCount;
    }

    public synchronized int getLastBlueCount() {
        return this.lastBlueCount;
    }

    public synchronized int getRedCaptureProgress() {
        return this.redCaptureProgress;
    }

    public synchronized int getBlueCaptureProgress() {
        return this.blueCaptureProgress;
    }

    public synchronized double getCapturePercentage(FactionRole role) {
        int progress = role == FactionRole.RED ? this.redCaptureProgress : this.blueCaptureProgress;
        return (double) progress / 100.0 * 100.0;
    }

    public boolean isPlayerInRange(double playerX, double playerY, double playerZ) {
        double dx = playerX - (double) this.position.getX();
        double dy = playerY - (double) this.position.getY();
        double dz = playerZ - (double) this.position.getZ();
        return dx * dx + dy * dy + dz * dz <= 400.0;
    }

    public synchronized void reset() {
        this.controllingFaction = null;
        this.redCaptureProgress = 0;
        this.blueCaptureProgress = 0;
        this.currentState = FlagState.NEUTRAL;
        this.wasContested = false;
        this.lastRedCount = 0;
        this.lastBlueCount = 0;
    }

    public enum FlagState {
        NEUTRAL,
        CAPTURING,
        CONTROLLED,
        CONTESTED,
        OVERTIME
    }
}
