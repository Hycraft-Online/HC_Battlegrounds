package com.hcbattlegrounds.hytale.commands.subcommands;

import com.hcbattlegrounds.BattlegroundsManager;
import com.hcbattlegrounds.HC_BattlegroundsPlugin;
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

public final class AdminListCommand extends AbstractPlayerCommand {
    public AdminListCommand() {
        super("list", "List queued and active battleground sessions");
    }

    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef player, @Nonnull World world) {
        BattlegroundsManager manager = HC_BattlegroundsPlugin.getInstance().getWarManager();
        BattlegroundsManager.QueueSnapshot queue = manager.getQueueSnapshot();

        ctx.sendMessage(Message.raw("=== Battleground Sessions ===").color(Color.ORANGE).bold(true));
        ctx.sendMessage(Message.raw("Queue Target: " + manager.getQueueArenaId()).color(Color.CYAN));
        ctx.sendMessage(Message.raw("Queued Red: " + queue.redQueued() + " | Queued Blue: " + queue.blueQueued())
            .color(Color.WHITE));
        ctx.sendMessage(Message.raw("Queue Min: " + queue.minimumPerFaction() + " per side | Capacity: "
            + manager.getMaxPlayersPerWar() + " total").color(Color.GRAY));

        Map<UUID, FactionWar> activeWars = manager.getActiveWars();
        if (activeWars.isEmpty()) {
            ctx.sendMessage(Message.raw("Active Sessions: none").color(Color.YELLOW));
            return;
        }

        ctx.sendMessage(Message.raw("Active Sessions: " + activeWars.size()).color(Color.GREEN));
        for (FactionWar war : activeWars.values()) {
            String shortId = war.getWarId().toString().substring(0, 8);
            int redPlayers = war.getRedFaction().getPlayerCount();
            int bluePlayers = war.getBlueFaction().getPlayerCount();
            int totalPlayers = war.getTotalPlayerCount();
            String remaining = this.formatTime(war.getRemainingTime());

            ctx.sendMessage(Message.raw(" - " + shortId
                + " | World: " + war.getWorldName()
                + " | Players: " + redPlayers + "R/" + bluePlayers + "B (" + totalPlayers + ")"
                + " | Time Left: " + remaining
            ).color(Color.WHITE));
        }
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
