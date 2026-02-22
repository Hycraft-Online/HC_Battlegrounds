package com.hcbattlegrounds.hytale.commands.subcommands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public final class AdminCommand extends AbstractPlayerCommand {
    public AdminCommand() {
        super("admin", "Battleground admin commands");
        this.addSubCommand(new AdminStartCommand());
        this.addSubCommand(new AdminEndCommand());
        this.addSubCommand(new AdminListCommand());
    }

    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef player, @Nonnull World world) {
        ctx.sendMessage(Message.raw("Battleground Admin Commands:").color("#FFD700")
            .insert("\n/battlegrounds admin start - Force start from current queue")
            .insert("\n/battlegrounds admin end <bg_id> - End an active battleground session")
            .insert("\n/battlegrounds admin list - List queued + active battleground sessions"));
    }
}
