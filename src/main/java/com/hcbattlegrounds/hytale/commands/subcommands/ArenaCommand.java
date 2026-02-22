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

public final class ArenaCommand extends AbstractPlayerCommand {
    public ArenaCommand() {
        super("arena", "Arena configuration commands");
        this.addSubCommand(new ArenaListCommand());
        this.addSubCommand(new ArenaCreateCommand());
        this.addSubCommand(new ArenaInfoCommand());
        this.addSubCommand(new ArenaSetSpawnCommand());
        this.addSubCommand(new ArenaSetFlagCommand());
    }

    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        ctx.sendMessage(Message.raw("Arena Commands:").color("#FFD700")
            .insert("\n/battlegrounds arena list - List all arenas")
            .insert("\n/battlegrounds arena create <id> - Create a new arena")
            .insert("\n/battlegrounds arena info <id> - Show arena details")
            .insert("\n/battlegrounds arena setspawn <id> <red|blue> - Set team spawn")
            .insert("\n/battlegrounds arena setflag <id> <index> - Set flag position"));
    }
}
