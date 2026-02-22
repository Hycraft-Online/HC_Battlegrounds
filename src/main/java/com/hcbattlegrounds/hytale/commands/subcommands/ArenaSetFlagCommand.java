package com.hcbattlegrounds.hytale.commands.subcommands;

import com.hcbattlegrounds.HC_BattlegroundsPlugin;
import com.hcbattlegrounds.config.ArenaRegistry;
import com.hcbattlegrounds.models.Vec3i;
import com.hcbattlegrounds.world.ArenaConfig;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
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

public final class ArenaSetFlagCommand extends AbstractPlayerCommand {
    private final RequiredArg<String> arenaIdArg = this.withRequiredArg("arenaId", "Arena ID", ArgTypes.STRING);
    private final RequiredArg<Integer> flagIndexArg = this.withRequiredArg("flagIndex", "Flag index (0-3)", ArgTypes.INTEGER);

    public ArenaSetFlagCommand() {
        super("setflag", "Set flag position (uses your current position)");
    }

    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        String arenaId = this.arenaIdArg.get(ctx);
        int flagIndex = this.flagIndexArg.get(ctx);
        if (flagIndex < 0 || flagIndex > 3) {
            ctx.sendMessage(Message.raw("Flag index must be between 0 and 3.").color(Color.RED));
            return;
        }
        ArenaRegistry registry = HC_BattlegroundsPlugin.getInstance().getArenaRegistry();
        ArenaConfig config = registry.get(arenaId).orElse(null);
        if (config == null) {
            ctx.sendMessage(Message.raw("Arena '" + arenaId + "' not found!").color(Color.RED));
            return;
        }
        Vector3d position = player.getTransform().getPosition();
        Vec3i pos = new Vec3i((int) position.getX(), (int) position.getY(), (int) position.getZ());
        ArenaConfig updated = config.withFlagPosition(flagIndex, pos);
        try {
            registry.save(updated);
            ctx.sendMessage(Message.raw("Set flag " + flagIndex + " to " + pos.x() + ", " + pos.y() + ", " + pos.z()).color(Color.GREEN));
        } catch (IOException e) {
            ctx.sendMessage(Message.raw("Failed to save arena: " + e.getMessage()).color(Color.RED));
        }
    }
}
