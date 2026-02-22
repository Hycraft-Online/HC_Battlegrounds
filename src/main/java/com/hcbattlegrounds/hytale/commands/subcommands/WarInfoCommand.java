package com.hcbattlegrounds.hytale.commands.subcommands;

import com.hcbattlegrounds.BattlegroundsManager;
import com.hcbattlegrounds.HC_BattlegroundsPlugin;
import com.hcbattlegrounds.hytale.notification.BattlegroundsMessages;
import com.hcbattlegrounds.models.CaptureFlag;
import com.hcbattlegrounds.models.FactionRole;
import com.hcbattlegrounds.models.FactionWar;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.awt.Color;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;

public final class WarInfoCommand extends AbstractPlayerCommand {
    private static final Color GOLD = new Color(255, 215, 0);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color BLUE = new Color(85, 85, 255);
    private static final Color GRAY = new Color(170, 170, 170);
    private static final Color WHITE = Color.WHITE;

    public WarInfoCommand() {
        super("info", "Show faction war information");
    }

    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        BattlegroundsManager manager = HC_BattlegroundsPlugin.getInstance().getWarManager();
        BattlegroundsManager.QueueSnapshot queueSnapshot = manager.getQueueSnapshot();
        ctx.sendMessage(BattlegroundsMessages.queueStatus(
            queueSnapshot.redQueued(),
            queueSnapshot.blueQueued(),
            queueSnapshot.minimumPerFaction()
        ));

        if (manager.isPlayerQueued(player.getUuid())) {
            FactionRole queuedFaction = manager.getQueuedFaction(player.getUuid());
            int queuePosition = manager.getQueuePosition(player.getUuid());
            if (queuedFaction != null && queuePosition > 0) {
                ctx.sendMessage(BattlegroundsMessages.alreadyQueued(queuedFaction, queuePosition));
            }
        } else if (manager.isPlayerPendingMatch(player.getUuid())) {
            ctx.sendMessage(BattlegroundsMessages.matchmakingInProgress());
        }

        Map<UUID, FactionWar> wars = manager.getActiveWars();
        FactionWar playerWar = manager.getWarByPlayer(player.getUuid());
        if (playerWar != null) {
            this.showWarInfo(ctx, playerWar);
            return;
        }

        if (wars.isEmpty()) {
            ctx.sendMessage(Message.raw("No active battleground matches.").color(Color.YELLOW));
            return;
        }

        ctx.sendMessage(Message.raw("Active Wars:").color(GOLD));
        for (Map.Entry<UUID, FactionWar> entry : wars.entrySet()) {
            this.showWarSummary(ctx, entry.getValue());
        }
    }

    private void showWarInfo(CommandContext ctx, FactionWar war) {
        String shortId = war.getWarId().toString().substring(0, 8);
        String timeLeft = this.formatTime(war.getRemainingTime());
        ctx.sendMessage(Message.raw("=== War: " + shortId + " ===").color(GOLD));
        ctx.sendMessage(Message.raw("Time Remaining: " + timeLeft).color(WHITE));
        ctx.sendMessage(Message.raw("").color(WHITE));
        int redPoints = war.getRedFaction().getPoints();
        int bluePoints = war.getBlueFaction().getPoints();
        int redPlayers = war.getRedFaction().getPlayerCount();
        int bluePlayers = war.getBlueFaction().getPlayerCount();
        ctx.sendMessage(Message.join(Message.raw("Red Team: ").color(RED), Message.raw(redPoints + " pts").color(RED).bold(true), Message.raw(" (" + redPlayers + " players)").color(GRAY)));
        ctx.sendMessage(Message.join(Message.raw("Blue Team: ").color(BLUE), Message.raw(bluePoints + " pts").color(BLUE).bold(true), Message.raw(" (" + bluePlayers + " players)").color(GRAY)));
        ctx.sendMessage(Message.raw("").color(WHITE));
        ctx.sendMessage(Message.raw("Flags:").color(GOLD));
        CaptureFlag[] flags = war.getFlags();
        for (int i = 0; i < flags.length; ++i) {
            Color statusColor;
            String status;
            CaptureFlag flag = flags[i];
            FactionRole controller = flag.getControllingFaction();
            if (controller == FactionRole.RED) {
                status = "Red";
                statusColor = RED;
            } else if (controller == FactionRole.BLUE) {
                status = "Blue";
                statusColor = BLUE;
            } else {
                status = "Neutral";
                statusColor = GRAY;
            }
            ctx.sendMessage(Message.join(Message.raw("  Flag " + (i + 1) + ": ").color(WHITE), Message.raw(status).color(statusColor)));
        }
    }

    private void showWarSummary(CommandContext ctx, FactionWar war) {
        String shortId = war.getWarId().toString().substring(0, 8);
        String timeLeft = this.formatTime(war.getRemainingTime());
        int totalPlayers = war.getTotalPlayerCount();
        ctx.sendMessage(Message.join(Message.raw("  " + shortId).color(WHITE), Message.raw(" - " + totalPlayers + " players").color(GRAY), Message.raw(" - " + timeLeft + " left").color(GRAY)));
    }

    private String formatTime(long ms) {
        long seconds = ms / 1000L;
        long minutes = seconds / 60L;
        long hours = minutes / 60L;
        if (hours > 0L) {
            return String.format("%dh %dm", hours, minutes % 60L);
        }
        if (minutes > 0L) {
            return String.format("%dm %ds", minutes, seconds % 60L);
        }
        return String.format("%ds", seconds);
    }
}
