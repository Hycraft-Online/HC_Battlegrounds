package com.hcbattlegrounds.hytale.commands.subcommands;

import com.hcbattlegrounds.HC_BattlegroundsPlugin;
import com.hcbattlegrounds.config.ArenaRegistry;
import com.hcbattlegrounds.models.Vec3i;
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
import javax.annotation.Nonnull;

public final class ArenaInfoCommand extends AbstractPlayerCommand {
    private final RequiredArg<String> arenaIdArg = this.withRequiredArg("arenaId", "Arena ID", ArgTypes.STRING);

    public ArenaInfoCommand() {
        super("info", "Show arena configuration details");
    }

    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        String arenaId = this.arenaIdArg.get(ctx);
        ArenaRegistry registry = HC_BattlegroundsPlugin.getInstance().getArenaRegistry();
        ArenaConfig config = registry.get(arenaId).orElse(null);
        if (config == null) {
            ctx.sendMessage(Message.raw("Arena '" + arenaId + "' not found!").color(Color.RED));
            return;
        }
        ctx.sendMessage(Message.raw("=== Arena: " + config.id() + " ===").color(Color.CYAN));
        ctx.sendMessage(Message.raw("Prefab: " + config.prefabName()).color(Color.WHITE));
        Vec3i red = config.redSpawn();
        ctx.sendMessage(Message.raw("Red Spawn: " + red.x() + ", " + red.y() + ", " + red.z()).color(Color.RED));
        Vec3i blue = config.blueSpawn();
        ctx.sendMessage(Message.raw("Blue Spawn: " + blue.x() + ", " + blue.y() + ", " + blue.z()).color(Color.BLUE));
        ctx.sendMessage(Message.raw("Flags (" + config.getFlagCount() + "):").color(Color.YELLOW));
        int i = 0;
        for (Vec3i flag : config.flagPositions()) {
            ctx.sendMessage(Message.raw("  [" + i + "] " + flag.x() + ", " + flag.y() + ", " + flag.z()).color(Color.GRAY));
            ++i;
        }
    }
}
