package com.hcbattlegrounds.hytale.commands.subcommands;

import com.hcbattlegrounds.HC_BattlegroundsPlugin;
import com.hcbattlegrounds.config.ArenaRegistry;
import com.hcbattlegrounds.world.ArenaConfig;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.awt.Color;
import javax.annotation.Nonnull;

public final class ArenaListCommand extends AbstractPlayerCommand {
    public ArenaListCommand() {
        super("list", "List all arena configurations");
    }

    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        ArenaRegistry registry = HC_BattlegroundsPlugin.getInstance().getArenaRegistry();
        if (registry.count() == 0) {
            ctx.sendMessage(Message.raw("No arena configurations found.").color(Color.YELLOW));
            return;
        }
        ctx.sendMessage(Message.raw("=== Arena Configurations ===").color(Color.CYAN));
        for (ArenaConfig config : registry.getAll()) {
            ctx.sendMessage(Message.raw("  - " + config.id() + " (" + config.getFlagCount() + " flags)").color(Color.WHITE));
        }
    }
}
