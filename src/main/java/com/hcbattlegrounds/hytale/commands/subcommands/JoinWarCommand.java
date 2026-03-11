package com.hcbattlegrounds.hytale.commands.subcommands;

import com.hcbattlegrounds.BattlegroundsManager;
import com.hcbattlegrounds.HC_BattlegroundsPlugin;
import com.hcbattlegrounds.hytale.notification.BattlegroundsMessages;
import com.hcbattlegrounds.models.FactionRole;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.awt.Color;
import java.util.Locale;
import java.util.UUID;
import javax.annotation.Nonnull;

public final class JoinWarCommand extends AbstractPlayerCommand {
    public JoinWarCommand() {
        super("join", "Queue for battleground matchmaking");
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                           @Nonnull PlayerRef player, @Nonnull World world) {
        BattlegroundsManager manager = HC_BattlegroundsPlugin.getInstance().getWarManager();
        FactionRole faction = this.resolveFactionRole(player.getUuid());
        if (faction == null) {
            player.sendMessage(BattlegroundsMessages.missingFaction());
            return;
        }

        Transform currentTransform = player.getTransform();
        Transform safeReturnTransform = new Transform(
            new Vector3d(currentTransform.getPosition()),
            new Vector3f(currentTransform.getRotation())
        );

        BattlegroundsManager.QueueJoinResult joinResult = manager.queuePlayer(player, faction, world, safeReturnTransform);
        switch (joinResult.status()) {
            case JOINED_ACTIVE_WAR -> {
                UUID warId = joinResult.activeWarId();
                String shortId = warId != null ? warId.toString().substring(0, 8) : "unknown";
                player.sendMessage(BattlegroundsMessages.joinedActiveWar(
                    faction,
                    shortId,
                    joinResult.factionPlayersInWar(),
                    joinResult.totalPlayersInWar(),
                    manager.getMaxPlayersPerWar()
                ));
            }
            case QUEUED -> {
                BattlegroundsManager.QueueSnapshot snapshot = manager.getQueueSnapshot();
                player.sendMessage(BattlegroundsMessages.joinedQueue(
                    faction,
                    joinResult.queuePosition(),
                    joinResult.sameFactionQueued(),
                    joinResult.opposingFactionQueued(),
                    joinResult.minimumPerFaction()
                ));
                player.sendMessage(BattlegroundsMessages.queueStatus(
                    snapshot.redQueued(),
                    snapshot.blueQueued(),
                    joinResult.minimumPerFaction()
                ));
            }
            case ALREADY_QUEUED -> {
                FactionRole queuedFaction = manager.getQueuedFaction(player.getUuid());
                if (queuedFaction == null) {
                    queuedFaction = faction;
                }
                int queuePosition = manager.getQueuePosition(player.getUuid());
                player.sendMessage(BattlegroundsMessages.alreadyQueued(queuedFaction, Math.max(1, queuePosition)));
            }
            case ALREADY_IN_WAR -> player.sendMessage(BattlegroundsMessages.alreadyInWar());
            case MATCHMAKING -> player.sendMessage(BattlegroundsMessages.matchmakingInProgress());
            default -> player.sendMessage(Message.raw("Failed to join battleground queue.").color(Color.RED));
        }
    }

    private FactionRole resolveFactionRole(UUID playerUuid) {
        String factionId = this.getPlayerFactionId(playerUuid);
        if (factionId == null) {
            return null;
        }

        String normalized = factionId.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("horde") || normalized.equals("red")
            || normalized.contains("iron") || normalized.contains("legion")) {
            return FactionRole.RED;
        }
        if (normalized.equals("alliance") || normalized.equals("blue")
            || normalized.equals("valor") || normalized.contains("valor")) {
            return FactionRole.BLUE;
        }
        return null;
    }

    private String getPlayerFactionId(UUID playerUuid) {
        try {
            Class<?> pluginClass = Class.forName("com.hcfactions.HC_FactionsPlugin");
            Object plugin = pluginClass.getMethod("getInstance").invoke(null);
            if (plugin == null) {
                return null;
            }

            Object playerDataRepository = pluginClass.getMethod("getPlayerDataRepository").invoke(plugin);
            if (playerDataRepository == null) {
                return null;
            }

            Object playerData = playerDataRepository.getClass()
                .getMethod("getPlayerData", UUID.class)
                .invoke(playerDataRepository, playerUuid);
            if (playerData == null) {
                return null;
            }

            Object factionId = playerData.getClass().getMethod("getFactionId").invoke(playerData);
            if (factionId instanceof String value && !value.isBlank()) {
                return value;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
