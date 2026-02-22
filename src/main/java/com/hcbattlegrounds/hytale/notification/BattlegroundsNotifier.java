package com.hcbattlegrounds.hytale.notification;

import com.hcbattlegrounds.BattlegroundsManager;
import com.hcbattlegrounds.hytale.events.FlagTickSystem;
import com.hcbattlegrounds.models.CaptureFlag;
import com.hcbattlegrounds.models.Faction;
import com.hcbattlegrounds.models.FactionRole;
import com.hcbattlegrounds.models.FactionWar;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class BattlegroundsNotifier {
    private static final long FIVE_MINUTES = 300000L;
    private static final long ONE_MINUTE = 60000L;
    private static final long THIRTY_SECONDS = 30000L;
    private static final long SCORE_UPDATE_INTERVAL = 30000L;
    private final BattlegroundsManager warManager;
    private final FlagTickSystem flagTickSystem;
    private final ScheduledExecutorService scheduler;
    private final Map<UUID, WarNotificationState> warStates = new ConcurrentHashMap<>();
    private ScheduledFuture<?> tickTask;
    private boolean running;

    public BattlegroundsNotifier(BattlegroundsManager warManager, FlagTickSystem flagTickSystem) {
        this.warManager = warManager;
        this.flagTickSystem = flagTickSystem;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void start() {
        if (this.running) {
            return;
        }
        this.running = true;
        this.tickTask = this.scheduler.scheduleAtFixedRate(this::tick, 0L, 1L, TimeUnit.SECONDS);
    }

    public void stop() {
        this.running = false;
        if (this.tickTask != null) {
            this.tickTask.cancel(false);
        }
        this.scheduler.shutdown();
    }

    private void tick() {
        if (!this.running) {
            return;
        }
        this.warManager.processMatchmaking();
        this.flagTickSystem.processFlagTick();
        this.warManager.refreshAllHuds();
        Map<UUID, FactionWar> activeWars = this.warManager.getActiveWars();
        this.warStates.keySet().removeIf(id -> !activeWars.containsKey(id));
        ArrayList<UUID> expiredWars = new ArrayList<>();
        for (Map.Entry<UUID, FactionWar> entry : activeWars.entrySet()) {
            UUID warId = entry.getKey();
            FactionWar war = entry.getValue();
            boolean isNew = !this.warStates.containsKey(warId);
            WarNotificationState state = this.warStates.computeIfAbsent(warId, k -> new WarNotificationState());
            if (isNew) {
                this.broadcastToWar(war, BattlegroundsMessages.warStarted());
                state.lastScoreUpdate = System.currentTimeMillis();
            }
            this.checkNotifications(war, state);
            if (!war.isExpired() && !this.warManager.hasReachedScoreLimit(war)) continue;
            this.handleWarEnd(war);
            expiredWars.add(warId);
        }
        for (UUID warId : expiredWars) {
            this.warManager.endWar(warId);
            this.flagTickSystem.clearWarData(warId);
            this.warStates.remove(warId);
        }
    }

    private void checkNotifications(FactionWar war, WarNotificationState state) {
        this.checkTimeWarnings(war, state);
        this.checkScoreUpdate(war, state);
        this.checkFlagStatusUpdate(war, state);
    }

    private void checkTimeWarnings(FactionWar war, WarNotificationState state) {
        long remaining = war.getRemainingTime();
        if (remaining <= 60000L && !state.warned1m) {
            state.warned1m = true;
            this.broadcastToWar(war, BattlegroundsMessages.warEnding("1 minute"));
        } else if (remaining <= 300000L && !state.warned5m) {
            state.warned5m = true;
            this.broadcastToWar(war, BattlegroundsMessages.warEnding("5 minutes"));
        }
    }

    private void checkScoreUpdate(FactionWar war, WarNotificationState state) {
        long now = System.currentTimeMillis();
        if (now - state.lastScoreUpdate >= 30000L) {
            state.lastScoreUpdate = now;
            int redPoints = war.getRedFaction().getPoints();
            int bluePoints = war.getBlueFaction().getPoints();
            int redFlags = war.getControlledFlagCount(FactionRole.RED);
            int blueFlags = war.getControlledFlagCount(FactionRole.BLUE);
            this.broadcastToWar(war, BattlegroundsMessages.scoreUpdate(redPoints, bluePoints, redFlags, blueFlags));
        }
    }

    private void checkFlagStatusUpdate(FactionWar war, WarNotificationState state) {
        int[] currentStatus = new int[4];
        CaptureFlag[] flags = war.getFlags();
        for (int i = 0; i < flags.length; ++i) {
            CaptureFlag flag = flags[i];
            FactionRole controller;
            currentStatus[i] = flag.isContested() ? 3 : ((controller = flag.getControllingFaction()) == FactionRole.RED ? 1 : (controller == FactionRole.BLUE ? 2 : 0));
        }
        if (!Arrays.equals(currentStatus, state.lastFlagStatus)) {
            state.lastFlagStatus = currentStatus.clone();
            this.broadcastToWar(war, BattlegroundsMessages.flagStatus(currentStatus));
        }
    }

    private void handleWarEnd(FactionWar war) {
        Faction winner = war.getWinner();
        int redPoints = war.getRedFaction().getPoints();
        int bluePoints = war.getBlueFaction().getPoints();
        int redFlags = war.getControlledFlagCount(FactionRole.RED);
        int blueFlags = war.getControlledFlagCount(FactionRole.BLUE);
        this.broadcastToWar(war, BattlegroundsMessages.scoreUpdate(redPoints, bluePoints, redFlags, blueFlags));
        if (winner == null) {
            this.broadcastToWar(war, BattlegroundsMessages.warEndDraw());
        } else {
            this.broadcastToWar(war, BattlegroundsMessages.warEndVictory(winner.getRole()));
        }
    }

    private void broadcastToWar(FactionWar war, Message message) {
        Universe universe = Universe.get();
        if (universe == null) {
            return;
        }
        for (UUID playerId : war.getAllPlayers()) {
            PlayerRef player = universe.getPlayer(playerId);
            if (player == null) continue;
            player.sendMessage(message);
        }
    }

    private static class WarNotificationState {
        boolean warned5m = false;
        boolean warned1m = false;
        long lastScoreUpdate = 0L;
        int[] lastFlagStatus = new int[4];
    }
}
