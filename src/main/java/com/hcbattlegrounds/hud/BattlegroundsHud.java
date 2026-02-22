package com.hcbattlegrounds.hud;

import com.hcbattlegrounds.models.FactionRole;
import com.hcbattlegrounds.models.FactionWar;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * HUD displaying the battleground scoreboard during a war.
 * Shows team scores, flag control, timer, and per-player kills.
 */
public class BattlegroundsHud extends CustomUIHud {

    private static final String HUD_PATH = "HUD/BattlegroundsScoreboard.ui";
    private static final String HUD_ID = "BattlegroundsScoreboard";
    private static final int MAX_PLAYERS_PER_TEAM = 5;

    private final PlayerRef playerRef;

    public BattlegroundsHud(@Nonnull PlayerRef playerRef) {
        super(playerRef);
        this.playerRef = playerRef;
    }

    public static String getHudId() {
        return HUD_ID;
    }

    @Override
    protected void build(@Nonnull UICommandBuilder cmd) {
        cmd.append(HUD_PATH);

        // Hide all player rows initially
        for (int i = 0; i < MAX_PLAYERS_PER_TEAM; i++) {
            cmd.set("#RedRow" + i + ".Visible", false);
            cmd.set("#BlueRow" + i + ".Visible", false);
        }
    }

    /**
     * Refresh the scoreboard with current war data.
     */
    public void refresh(FactionWar war, int scoreToWin) {
        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        UICommandBuilder cmd = new UICommandBuilder();

        // Team scores
        int redPts = war.getRedFaction().getPoints();
        int bluePts = war.getBlueFaction().getPoints();
        cmd.set("#RedScore.Text", "RED: " + redPts);
        cmd.set("#BlueScore.Text", "BLUE: " + bluePts);

        // Timer
        long remainingMs = war.getRemainingTime();
        long totalSec = remainingMs / 1000;
        long mins = totalSec / 60;
        long secs = totalSec % 60;
        cmd.set("#Timer.Text", String.format("%d:%02d", mins, secs));

        // Flags
        int redFlags = war.getControlledFlagCount(FactionRole.RED);
        int blueFlags = war.getControlledFlagCount(FactionRole.BLUE);
        cmd.set("#RedFlags.Text", "Flags: " + redFlags);
        cmd.set("#BlueFlags.Text", "Flags: " + blueFlags);

        // Score limit
        cmd.set("#ScoreLimit.Text", "Goal: " + scoreToWin);

        // Player lists sorted by kills
        populateTeamRows(cmd, war, FactionRole.RED, "Red");
        populateTeamRows(cmd, war, FactionRole.BLUE, "Blue");

        update(false, cmd);
    }

    /**
     * Hide the scoreboard.
     */
    public void hide() {
        Ref<EntityStore> entityRef = playerRef.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#BattlegroundsHud.Visible", false);
        update(false, cmd);
    }

    private void populateTeamRows(UICommandBuilder cmd, FactionWar war, FactionRole role, String prefix) {
        Set<UUID> teamPlayers = war.getFaction(role).getPlayers();
        Universe universe = Universe.get();

        // Build list of (uuid, kills) sorted by kills desc
        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>();
        for (UUID playerId : teamPlayers) {
            sorted.add(Map.entry(playerId, war.getPlayerKills(playerId)));
        }
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        for (int i = 0; i < MAX_PLAYERS_PER_TEAM; i++) {
            if (i < sorted.size()) {
                Map.Entry<UUID, Integer> entry = sorted.get(i);
                String name = "Unknown";
                if (universe != null) {
                    PlayerRef ref = universe.getPlayer(entry.getKey());
                    if (ref != null) {
                        name = ref.getUsername();
                    }
                }
                if (name.length() > 14) {
                    name = name.substring(0, 13) + "..";
                }

                cmd.set("#" + prefix + "Row" + i + ".Visible", true);
                cmd.set("#" + prefix + "Name" + i + ".Text", name);
                cmd.set("#" + prefix + "Kills" + i + ".Text", String.valueOf(entry.getValue()));
            } else {
                cmd.set("#" + prefix + "Row" + i + ".Visible", false);
            }
        }
    }
}
