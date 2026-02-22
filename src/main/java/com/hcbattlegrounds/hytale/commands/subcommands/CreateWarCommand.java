package com.hcbattlegrounds.hytale.commands.subcommands;

import com.hcbattlegrounds.HC_BattlegroundsPlugin;
import com.hcbattlegrounds.hytale.notification.BattlegroundsMessages;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.vector.Transform;
import java.awt.Color;
import javax.annotation.Nonnull;

public final class CreateWarCommand extends AbstractPlayerCommand {
    private final OptionalArg<String> arenaIdArg = this.withOptionalArg("arena", "Arena config ID (default: 'default')", ArgTypes.STRING);

    public CreateWarCommand() {
        super("create", "Create a new faction war");
    }

    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        String arenaId = this.arenaIdArg.get(ctx);
        if (arenaId == null || arenaId.isEmpty()) {
            arenaId = "default";
        }

        TransformComponent transform = store.getComponent(ref, EntityModule.get().getTransformComponentType());
        Transform returnTransform;
        if (transform != null) {
            returnTransform = new Transform(transform.getPosition(), transform.getRotation());
        } else {
            returnTransform = new Transform(new Vector3d(0, 100, 0), new Vector3f(0, 0, 0));
        }

        ctx.sendMessage(Message.raw("Creating war with arena: " + arenaId + "...").color(Color.YELLOW));
        HC_BattlegroundsPlugin.getInstance().getWarManager().createWar(arenaId, world, returnTransform)
            .thenAccept(war -> {
                String shortId = war.getWarId().toString().substring(0, 8);
                player.sendMessage(BattlegroundsMessages.warCreated(shortId));
            })
            .exceptionally(ex -> {
                player.sendMessage(Message.raw("Failed to create war: " + ex.getMessage()).color(Color.RED));
                return null;
            });
    }
}
