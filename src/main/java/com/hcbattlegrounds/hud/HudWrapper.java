package com.hcbattlegrounds.hud;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * Reflection-based wrapper for HC_MultiHud integration.
 * Falls back to native HUD if HC_MultiHud is not available.
 */
public class HudWrapper {
    private static final HytaleLogger LOGGER = HytaleLogger.getLogger().getSubLogger("Battlegrounds-HudWrapper");
    private static final boolean MULTIPLE_HUD_AVAILABLE;
    private static volatile Object multipleHudInstance;
    private static volatile Method setCustomHudMethod;
    private static volatile Method hideCustomHudMethod;

    static {
        boolean available = false;
        try {
            Class<?> multipleHudClass = Class.forName("com.hcmultihud.HC_MultiHudPlugin");
            Method getInstanceMethod = multipleHudClass.getMethod("getInstance");
            multipleHudInstance = getInstanceMethod.invoke(null);
            setCustomHudMethod = multipleHudClass.getMethod("setCustomHud",
                Player.class, PlayerRef.class, String.class, CustomUIHud.class);
            hideCustomHudMethod = multipleHudClass.getMethod("hideCustomHud",
                Player.class, String.class);
            available = true;
            LOGGER.at(Level.INFO).log("HC_MultiHud detected, using multi-HUD support");
        } catch (ClassNotFoundException e) {
            LOGGER.at(Level.INFO).log("HC_MultiHud not found, using standard HUD mode");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error initializing HC_MultiHud support: " + e.getMessage());
        }
        MULTIPLE_HUD_AVAILABLE = available;
    }

    /**
     * @return true if accepted, false if rejected (safe mode).
     */
    public static boolean setCustomHud(Player player, PlayerRef playerRef, String hudId, CustomUIHud hud) {
        if (MULTIPLE_HUD_AVAILABLE) {
            try {
                Object result = setCustomHudMethod.invoke(multipleHudInstance, player, playerRef, hudId, hud);
                if (result instanceof Boolean && !(Boolean) result) {
                    return false;
                }
                return true;
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("Error setting HUD via HC_MultiHud: " + e.getMessage());
                player.getHudManager().setCustomHud(playerRef, hud);
                return true;
            }
        } else {
            player.getHudManager().setCustomHud(playerRef, hud);
            return true;
        }
    }

    public static void hideCustomHud(Player player, String hudId) {
        if (MULTIPLE_HUD_AVAILABLE) {
            try {
                hideCustomHudMethod.invoke(multipleHudInstance, player, hudId);
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("Error hiding HUD via HC_MultiHud: " + e.getMessage());
            }
        } else {
            player.getHudManager().setCustomHud(null, null);
        }
    }
}
