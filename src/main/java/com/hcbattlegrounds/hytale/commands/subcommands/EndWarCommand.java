package com.hcbattlegrounds.hytale.commands.subcommands;

import com.hcbattlegrounds.BattlegroundsManager;
import com.hcbattlegrounds.HC_BattlegroundsPlugin;
import com.hcbattlegrounds.hytale.notification.BattlegroundsMessages;
import com.hcbattlegrounds.models.FactionWar;
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
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;

public final class EndWarCommand extends AbstractPlayerCommand {
    private final RequiredArg<String> warIdArg = this.withRequiredArg("warId", "War ID (or partial)", ArgTypes.STRING);

    public EndWarCommand() {
        super("end", "End a faction war (admin)");
        this.requirePermission("*");
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        String warIdStr = this.warIdArg.get(ctx);
        BattlegroundsManager manager = HC_BattlegroundsPlugin.getInstance().getWarManager();
        UUID warId = this.findWarId(manager, warIdStr);
        if (warId == null) {
            player.sendMessage(BattlegroundsMessages.warNotFound());
            return;
        }
        manager.endWar(warId).thenRun(() -> player.sendMessage(BattlegroundsMessages.warEnded())).exceptionally(ex -> {
            player.sendMessage(Message.raw("Failed to end war: " + ex.getMessage()).color(Color.RED));
            return null;
        });
    }

    private UUID findWarId(BattlegroundsManager manager, String warIdStr) {
        Map<UUID, FactionWar> wars = manager.getActiveWars();
        for (UUID id : wars.keySet()) {
            if (!id.toString().startsWith(warIdStr) && !id.toString().contains(warIdStr)) continue;
            return id;
        }
        try {
            UUID exact = UUID.fromString(warIdStr);
            if (wars.containsKey(exact)) {
                return exact;
            }
        } catch (IllegalArgumentException ignored) {
        }
        if (wars.size() == 1 && warIdStr.isEmpty()) {
            return wars.keySet().iterator().next();
        }
        return null;
    }
}
