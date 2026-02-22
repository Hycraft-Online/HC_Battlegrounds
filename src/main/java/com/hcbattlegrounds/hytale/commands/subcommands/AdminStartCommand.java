package com.hcbattlegrounds.hytale.commands.subcommands;

import com.hcbattlegrounds.BattlegroundsManager;
import com.hcbattlegrounds.HC_BattlegroundsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.awt.Color;
import javax.annotation.Nonnull;

public final class AdminStartCommand extends AbstractPlayerCommand {
    private final OptionalArg<String> ignoredBgIdArg = this.withOptionalArg("bg_id", "Ignored - queue-based start", ArgTypes.STRING);

    public AdminStartCommand() {
        super("start", "Force start a battleground from the current queue");
    }

    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef player, @Nonnull World world) {
        String ignoredBgId = this.ignoredBgIdArg.get(ctx);
        BattlegroundsManager manager = HC_BattlegroundsPlugin.getInstance().getWarManager();
        BattlegroundsManager.ForceStartResult result = manager.forceStartFromQueue();
        switch (result) {
            case STARTED -> {
                if (ignoredBgId != null && !ignoredBgId.isBlank()) {
                    player.sendMessage(Message.raw("Ignoring bg_id '" + ignoredBgId + "': start now always uses live queue.").color(Color.YELLOW));
                }
                player.sendMessage(Message.raw("Forced battleground start from current queue.").color(Color.GREEN));
            }
            case MATCHMAKING_IN_PROGRESS -> player.sendMessage(Message.raw("Matchmaking is already in progress.").color(Color.YELLOW));
            case INSUFFICIENT_QUEUE -> player.sendMessage(Message.raw("Cannot start: queue is empty.").color(Color.RED));
            default -> player.sendMessage(Message.raw("No queued battleground session to start.").color(Color.YELLOW));
        }
    }
}
