package com.hcbattlegrounds.hytale.commands.subcommands;

import com.hcbattlegrounds.BattlegroundsManager;
import com.hcbattlegrounds.HC_BattlegroundsPlugin;
import com.hcbattlegrounds.hytale.notification.BattlegroundsMessages;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public final class LeaveWarCommand extends AbstractPlayerCommand {
    public LeaveWarCommand() {
        super("leave", "Leave battleground queue or active battleground");
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        BattlegroundsManager manager = HC_BattlegroundsPlugin.getInstance().getWarManager();
        if (manager.unqueuePlayer(player.getUuid())) {
            player.sendMessage(BattlegroundsMessages.leftQueue());
            return;
        }
        if (manager.leaveWar(player.getUuid())) {
            player.sendMessage(BattlegroundsMessages.leftWar());
            return;
        }
        if (manager.isPlayerPendingMatch(player.getUuid())) {
            player.sendMessage(BattlegroundsMessages.matchmakingInProgress());
            return;
        }
        player.sendMessage(BattlegroundsMessages.notInWarOrQueue());
    }
}
