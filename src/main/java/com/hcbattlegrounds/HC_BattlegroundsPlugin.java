package com.hcbattlegrounds;

import com.hcbattlegrounds.config.ArenaRegistry;
import com.hcbattlegrounds.hytale.commands.BattlegroundsCommand;
import com.hcbattlegrounds.hytale.events.FlagTickSystem;
import com.hcbattlegrounds.hytale.events.PlayerDeathSystem;
import com.hcbattlegrounds.hytale.notification.BattlegroundsNotifier;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Level;

public class HC_BattlegroundsPlugin extends JavaPlugin {
    private static final Path CONFIG_DIR = Path.of("mods", ".hc_config", "HC_Battlegrounds");
    private static volatile HC_BattlegroundsPlugin instance;
    private static final long WAR_DURATION = 900000L;
    private static final int MIN_PLAYERS_PER_FACTION = 3;
    private static final int SCORE_TO_WIN = 2000;
    private static final int MAX_PLAYERS_PER_BATTLEGROUND = 20;
    private static final String QUEUE_ARENA_ID = "default";
    private ArenaRegistry arenaRegistry;
    private BattlegroundsManager warManager;
    private BattlegroundsNotifier warNotifier;
    private FlagTickSystem flagTickSystem;

    public HC_BattlegroundsPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    protected void setup() {
        super.setup();
        this.getLogger().at(Level.INFO).log("Initializing Battlegrounds plugin");
        this.arenaRegistry = new ArenaRegistry(CONFIG_DIR);
        this.arenaRegistry.loadAll();
        this.flagTickSystem = new FlagTickSystem();
        PlayerDeathSystem deathSystem = new PlayerDeathSystem();
        this.getEntityStoreRegistry().registerSystem(this.flagTickSystem);
        this.getEntityStoreRegistry().registerSystem(deathSystem);
        this.warManager = new BattlegroundsManager(
            WAR_DURATION,
            MIN_PLAYERS_PER_FACTION,
            SCORE_TO_WIN,
            QUEUE_ARENA_ID,
            MAX_PLAYERS_PER_BATTLEGROUND,
            this.arenaRegistry
        );
        this.warNotifier = new BattlegroundsNotifier(this.warManager, this.flagTickSystem);
        this.warNotifier.start();
        this.getCommandRegistry().registerCommand(new BattlegroundsCommand());
        this.getLogger().at(Level.INFO).log("Battlegrounds plugin initialized successfully");
        this.getLogger().at(Level.INFO).log("Loaded %d arena configs", this.arenaRegistry.count());
        this.getLogger().at(Level.INFO).log(
            "Matchmaking enabled: min %dv%d per side, score-to-win %d, arena '%s'",
            MIN_PLAYERS_PER_FACTION,
            MIN_PLAYERS_PER_FACTION,
            SCORE_TO_WIN,
            QUEUE_ARENA_ID
        );
        this.getLogger().at(Level.INFO).log("Battleground capacity: %d players (%d per faction)",
            MAX_PLAYERS_PER_BATTLEGROUND, MAX_PLAYERS_PER_BATTLEGROUND / 2);
    }

    protected void shutdown() {
        this.getLogger().at(Level.INFO).log("Shutting down Battlegrounds plugin");
        if (this.warNotifier != null) {
            this.warNotifier.stop();
        }
        if (this.warManager != null) {
            for (UUID warId : this.warManager.getActiveWars().keySet()) {
                this.warManager.endWar(warId);
            }
        }
        super.shutdown();
    }

    public static HC_BattlegroundsPlugin getInstance() {
        return instance;
    }

    public BattlegroundsManager getWarManager() {
        return this.warManager;
    }

    public ArenaRegistry getArenaRegistry() {
        return this.arenaRegistry;
    }

    public FlagTickSystem getFlagTickSystem() {
        return this.flagTickSystem;
    }
}
