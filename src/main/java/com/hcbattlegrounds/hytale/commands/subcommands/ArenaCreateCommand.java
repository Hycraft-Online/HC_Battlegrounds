package com.hcbattlegrounds.hytale.commands.subcommands;

import com.hcbattlegrounds.HC_BattlegroundsPlugin;
import com.hcbattlegrounds.config.ArenaRegistry;
import com.hcbattlegrounds.world.ArenaConfig;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.awt.Color;
import java.io.IOException;
import javax.annotation.Nonnull;

public final class ArenaCreateCommand extends AbstractPlayerCommand {
    private final RequiredArg<String> arenaIdArg = this.withRequiredArg("arenaId", "Unique arena ID", ArgTypes.STRING);

    public ArenaCreateCommand() {
        super("create", "Create a new arena configuration");
    }

    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        String arenaId = this.arenaIdArg.get(ctx);
        ArenaRegistry registry = HC_BattlegroundsPlugin.getInstance().getArenaRegistry();
        if (registry.get(arenaId).isPresent()) {
            ctx.sendMessage(Message.raw("Arena '" + arenaId + "' already exists!").color(Color.RED));
            return;
        }
        ArenaConfig config = ArenaConfig.createDefault(arenaId);
        try {
            registry.save(config);
            ctx.sendMessage(Message.raw("Created arena: " + arenaId).color(Color.GREEN));
            ctx.sendMessage(Message.raw("Use /battlegrounds arena setspawn and /battlegrounds arena setflag to configure.").color(Color.GRAY));
        } catch (IOException e) {
            ctx.sendMessage(Message.raw("Failed to save arena: " + e.getMessage()).color(Color.RED));
        }
    }
}
