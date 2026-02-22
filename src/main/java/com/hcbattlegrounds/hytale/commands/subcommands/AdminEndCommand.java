package com.hcbattlegrounds.hytale.commands.subcommands;

import com.hcbattlegrounds.BattlegroundsManager;
import com.hcbattlegrounds.HC_BattlegroundsPlugin;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;

public final class AdminEndCommand extends AbstractPlayerCommand {
    private final RequiredArg<String> bgIdArg = this.withRequiredArg("bg_id", "Session ID (short/full UUID) or 'all'", ArgTypes.STRING);

    public AdminEndCommand() {
        super("end", "End a battleground session");
    }

    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef player, @Nonnull World world) {
        String bgId = this.bgIdArg.get(ctx);
        BattlegroundsManager manager = HC_BattlegroundsPlugin.getInstance().getWarManager();

        if ("all".equalsIgnoreCase(bgId)) {
            List<UUID> activeWarIds = new ArrayList<>(manager.getActiveWars().keySet());
            if (activeWarIds.isEmpty()) {
                player.sendMessage(Message.raw("No active battleground sessions to end.").color(Color.YELLOW));
                return;
            }
            for (UUID warId : activeWarIds) {
                manager.endWar(warId);
            }
            player.sendMessage(Message.raw("Ending " + activeWarIds.size() + " battleground session(s).").color(Color.YELLOW));
            return;
        }

        UUID warId = this.findWarId(manager, bgId);
        if (warId == null) {
            player.sendMessage(Message.raw("Battleground session '" + bgId + "' not found.").color(Color.RED));
            return;
        }

        manager.endWar(warId).thenRun(() -> {
            player.sendMessage(Message.raw("Ended battleground session " + warId.toString().substring(0, 8) + ".").color(Color.GREEN));
        }).exceptionally(ex -> {
            player.sendMessage(Message.raw("Failed to end battleground session: " + ex.getMessage()).color(Color.RED));
            return null;
        });
    }

    private UUID findWarId(BattlegroundsManager manager, String warIdStr) {
        Map<UUID, FactionWar> wars = manager.getActiveWars();
        for (UUID id : wars.keySet()) {
            if (id.toString().startsWith(warIdStr) || id.toString().contains(warIdStr)) {
                return id;
            }
        }
        try {
            UUID exact = UUID.fromString(warIdStr);
            if (wars.containsKey(exact)) {
                return exact;
            }
        } catch (IllegalArgumentException ignored) {
        }
        return null;
    }
}
