package com.hcbattlegrounds.hytale.events;

import com.hcbattlegrounds.BattlegroundsManager;
import com.hcbattlegrounds.HC_BattlegroundsPlugin;
import com.hcbattlegrounds.hytale.notification.BattlegroundsMessages;
import com.hcbattlegrounds.models.Faction;
import com.hcbattlegrounds.models.FactionWar;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PlayerDeathSystem extends DeathSystems.OnDeathSystem {
    private static final int KILL_POINTS = 100;

    @Nonnull
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType());
    }

    public void onComponentAdded(@Nonnull Ref ref, @Nonnull DeathComponent component, @Nonnull Store store, @Nonnull CommandBuffer commandBuffer) {
        PlayerRef victim = (PlayerRef) store.getComponent(ref, PlayerRef.getComponentType());
        if (victim == null) {
            return;
        }
        PlayerRef killer = this.getKiller(component, store);
        if (killer == null) {
            return;
        }
        this.handleWarKill(killer.getUuid(), victim.getUuid());
    }

    @Nullable
    private PlayerRef getKiller(DeathComponent component, Store store) {
        Damage.Source source = component.getDeathInfo().getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return null;
        }
        Ref killerRef = entitySource.getRef();
        return (PlayerRef) store.getComponent(killerRef, PlayerRef.getComponentType());
    }

    private void handleWarKill(UUID killerId, UUID victimId) {
        BattlegroundsManager manager = HC_BattlegroundsPlugin.getInstance().getWarManager();
        FactionWar war = manager.getWarByPlayer(killerId);
        if (war == null || !war.isActive()) {
            return;
        }
        if (!war.isInWar(victimId)) {
            return;
        }
        if (war.isSameTeam(killerId, victimId)) {
            return;
        }
        war.addPoints(killerId, 100);
        war.recordKill(killerId);
        Faction team = war.getPlayerTeam(killerId);
        Universe universe = Universe.get();
        if (team != null && universe != null) {
            PlayerRef killer = universe.getPlayer(killerId);
            PlayerRef victim = universe.getPlayer(victimId);
            if (killer != null) {
                String victimName = victim != null ? victim.getUsername() : "an enemy";
                killer.sendMessage(BattlegroundsMessages.pointsEarnedKill(100, team.getPoints(), victimName));
            }
        }
    }
}
