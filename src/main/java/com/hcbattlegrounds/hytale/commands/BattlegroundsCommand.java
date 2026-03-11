package com.hcbattlegrounds.hytale.commands;

import com.hcbattlegrounds.hytale.commands.subcommands.ArenaCommand;
import com.hcbattlegrounds.hytale.commands.subcommands.AdminCommand;
import com.hcbattlegrounds.hytale.commands.subcommands.EndWarCommand;
import com.hcbattlegrounds.hytale.commands.subcommands.JoinWarCommand;
import com.hcbattlegrounds.hytale.commands.subcommands.LeaveWarCommand;
import com.hcbattlegrounds.hytale.commands.subcommands.WarInfoCommand;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public final class BattlegroundsCommand extends AbstractPlayerCommand {
    public BattlegroundsCommand() {
        super("battlegrounds", "Battlegrounds minigame commands");
        this.addAliases("bg");
        this.addSubCommand(new JoinWarCommand());
        this.addSubCommand(new LeaveWarCommand());
        this.addSubCommand(new WarInfoCommand());
        this.addSubCommand(new EndWarCommand());
        this.addSubCommand(new ArenaCommand());
        this.addSubCommand(new AdminCommand());
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    protected void execute(@Nonnull CommandContext ctx, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef player, @Nonnull World world) {
        ctx.sendMessage(Message.raw("Battlegrounds Commands:").color("#FFD700")
            .insert("\n/battlegrounds join - Queue using your faction")
            .insert("\n/battlegrounds leave - Leave queue or active battleground")
            .insert("\n/battlegrounds info - Show queue + battleground status")
            .insert("\n/battlegrounds end <warId> - End a war (admin)")
            .insert("\n/battlegrounds admin start - Force start current queue (admin)")
            .insert("\n/battlegrounds admin end <bg_id> - End session (admin)")
            .insert("\n/battlegrounds admin list - List queued + active sessions (admin)")
            .insert("\n/battlegrounds arena - Arena configuration commands")
            .insert("\nAlias: /bg"));
    }
}
