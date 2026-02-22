package com.hcbattlegrounds.hytale.notification;

import com.hcbattlegrounds.models.FactionRole;
import com.hypixel.hytale.server.core.Message;
import java.awt.Color;

public class BattlegroundsMessages {
    private static final Color GOLD = new Color(255, 170, 0);
    private static final Color RED = new Color(255, 85, 85);
    private static final Color BLUE = new Color(85, 85, 255);
    private static final Color GREEN = new Color(85, 255, 85);
    private static final Color AQUA = new Color(85, 255, 255);
    private static final Color YELLOW = new Color(255, 255, 85);
    private static final Color GRAY = new Color(170, 170, 170);
    private static final Color WHITE = Color.WHITE;

    public static Message warStarted() {
        return Message.join(prefix(), Message.raw("The ").color(WHITE), Message.raw("BATTLEGROUNDS").color(GOLD).bold(true), Message.raw(" has begun! Capture the flags!").color(WHITE));
    }

    public static Message warEnding(String time) {
        return Message.join(prefix(), Message.raw("War ends in ").color(WHITE), Message.raw(time).color(YELLOW).bold(true), Message.raw("!").color(WHITE));
    }

    public static Message warEndDraw() {
        return Message.join(prefix(), Message.raw("The war ended in a ").color(WHITE), Message.raw("DRAW").color(YELLOW).bold(true), Message.raw("!").color(WHITE));
    }

    public static Message warEndVictory(FactionRole winner) {
        Color winnerColor = winner == FactionRole.RED ? RED : BLUE;
        return Message.join(prefix(), Message.raw("VICTORY").color(winnerColor).bold(true), Message.raw("! ").color(WHITE), Message.raw(winner.getDisplayName()).color(winnerColor).bold(true), Message.raw(" team wins the war!").color(WHITE));
    }

    public static Message flagCaptured(int flagNumber, FactionRole faction) {
        Color factionColor = faction == FactionRole.RED ? RED : BLUE;
        return Message.join(prefix(), Message.raw("Flag " + flagNumber).color(YELLOW).bold(true), Message.raw(" captured by ").color(WHITE), Message.raw(faction.getDisplayName()).color(factionColor).bold(true), Message.raw("!").color(WHITE));
    }

    public static Message flagContested(int flagNumber) {
        return Message.join(prefix(), Message.raw("Flag " + flagNumber).color(YELLOW).bold(true), Message.raw(" is ").color(WHITE), Message.raw("CONTESTED").color(GOLD).bold(true), Message.raw("! Points paused!").color(WHITE));
    }

    public static Message flagUncontested(int flagNumber, FactionRole controller) {
        Color factionColor = controller == FactionRole.RED ? RED : BLUE;
        return Message.join(prefix(), Message.raw("Flag " + flagNumber).color(YELLOW), Message.raw(" is now ").color(WHITE), Message.raw("secured").color(factionColor), Message.raw(" by ").color(WHITE), Message.raw(controller.getDisplayName()).color(factionColor).bold(true), Message.raw("!").color(WHITE));
    }

    public static Message flagNeutral(int flagNumber) {
        return Message.join(prefix(), Message.raw("Flag " + flagNumber).color(YELLOW), Message.raw(" is now ").color(WHITE), Message.raw("neutral").color(GRAY), Message.raw(".").color(WHITE));
    }

    public static Message flagOvertime(int flagNumber, FactionRole attacker) {
        Color factionColor = attacker == FactionRole.RED ? RED : BLUE;
        return Message.join(prefix(), Message.raw("Flag " + flagNumber).color(YELLOW).bold(true), Message.raw(" is being taken by ").color(WHITE), Message.raw(attacker.getDisplayName()).color(factionColor).bold(true), Message.raw("!").color(WHITE));
    }

    public static Message flagCapturing(int flagNumber, FactionRole attacker) {
        Color factionColor = attacker == FactionRole.RED ? RED : BLUE;
        return Message.join(prefix(), Message.raw("Flag " + flagNumber).color(YELLOW).bold(true), Message.raw(" is being captured by ").color(WHITE), Message.raw(attacker.getDisplayName()).color(factionColor).bold(true), Message.raw(".").color(WHITE));
    }

    public static Message pointsEarnedFlag(int points, int flagNumber, int totalTeamPoints) {
        return Message.join(prefix(), Message.raw("+").color(GREEN), Message.raw(String.valueOf(points)).color(GREEN).bold(true), Message.raw(" points ").color(GREEN), Message.raw("(Flag " + flagNumber + ") ").color(GRAY), Message.raw("[Team: ").color(GRAY), Message.raw(String.valueOf(totalTeamPoints)).color(AQUA).bold(true), Message.raw("]").color(GRAY));
    }

    public static Message pointsEarnedKill(int points, int totalTeamPoints, String victimName) {
        return Message.join(prefix(), Message.raw("+").color(GREEN), Message.raw(String.valueOf(points)).color(GREEN).bold(true), Message.raw(" points ").color(GREEN), Message.raw("(Killed ").color(GRAY), Message.raw(victimName).color(RED), Message.raw(") ").color(GRAY), Message.raw("[Team: ").color(GRAY), Message.raw(String.valueOf(totalTeamPoints)).color(AQUA).bold(true), Message.raw("]").color(GRAY));
    }

    public static Message scoreUpdate(int redPoints, int bluePoints, int redFlags, int blueFlags) {
        return Message.join(prefix(), Message.raw("Red ").color(RED), Message.raw(String.valueOf(redPoints)).color(RED).bold(true), Message.raw(" [" + redFlags + " flags]").color(RED), Message.raw(" - ").color(GRAY), Message.raw("[" + blueFlags + " flags] ").color(BLUE), Message.raw(String.valueOf(bluePoints)).color(BLUE).bold(true), Message.raw(" Blue").color(BLUE));
    }

    public static Message flagStatus(int[] flagStates) {
        Message result = prefix();
        result = result.insert("Flags: ");
        for (int i = 0; i < flagStates.length; ++i) {
            Color color;
            String symbol = switch (flagStates[i]) {
                case 1 -> {
                    color = RED;
                    yield "R";
                }
                case 2 -> {
                    color = BLUE;
                    yield "B";
                }
                case 3 -> {
                    color = GOLD;
                    yield "!";
                }
                default -> {
                    color = GRAY;
                    yield "-";
                }
            };
            result = result.insert(Message.raw("[" + symbol + "]").color(color));
            if (i < flagStates.length - 1) {
                result = result.insert(" ");
            }
        }
        return result;
    }

    public static Message joinedWar(FactionRole faction) {
        Color factionColor = faction == FactionRole.RED ? RED : BLUE;
        return Message.join(prefix(), Message.raw("You joined the ").color(WHITE), Message.raw(faction.getDisplayName()).color(factionColor).bold(true), Message.raw(" team!").color(WHITE));
    }

    public static Message joinedQueue(FactionRole faction, int position, int teamQueued, int opposingQueued, int required) {
        Color factionColor = faction == FactionRole.RED ? RED : BLUE;
        return Message.join(
            prefix(),
            Message.raw("Queued for ").color(WHITE),
            Message.raw(faction.getDisplayName()).color(factionColor).bold(true),
            Message.raw(" [#").color(WHITE),
            Message.raw(String.valueOf(position)).color(YELLOW).bold(true),
            Message.raw("] ").color(WHITE),
            Message.raw("(Your side: ").color(GRAY),
            Message.raw(String.valueOf(teamQueued)).color(factionColor).bold(true),
            Message.raw(", Opposing: ").color(GRAY),
            Message.raw(String.valueOf(opposingQueued)).color(AQUA).bold(true),
            Message.raw(", Min: ").color(GRAY),
            Message.raw(String.valueOf(required)).color(YELLOW).bold(true),
            Message.raw(")").color(GRAY)
        );
    }

    public static Message joinedActiveWar(FactionRole faction, String warId, int factionCount, int totalPlayers, int maxPlayers) {
        Color factionColor = faction == FactionRole.RED ? RED : BLUE;
        return Message.join(
            prefix(),
            Message.raw("Joined active battleground ").color(WHITE),
            Message.raw(warId).color(AQUA).bold(true),
            Message.raw(" as ").color(WHITE),
            Message.raw(faction.getDisplayName()).color(factionColor).bold(true),
            Message.raw(" (").color(GRAY),
            Message.raw(String.valueOf(factionCount)).color(factionColor).bold(true),
            Message.raw(" on your team, ").color(GRAY),
            Message.raw(String.valueOf(totalPlayers)).color(YELLOW).bold(true),
            Message.raw("/" + maxPlayers + " total)").color(GRAY)
        );
    }

    public static Message alreadyQueued(FactionRole faction, int position) {
        Color factionColor = faction == FactionRole.RED ? RED : BLUE;
        return Message.join(
            prefix(),
            Message.raw("You are already queued for ").color(WHITE),
            Message.raw(faction.getDisplayName()).color(factionColor).bold(true),
            Message.raw(" (#").color(WHITE),
            Message.raw(String.valueOf(position)).color(YELLOW).bold(true),
            Message.raw(").").color(WHITE)
        );
    }

    public static Message matchmakingInProgress() {
        return Message.join(prefix(), Message.raw("A match is already being prepared for you.").color(YELLOW));
    }

    public static Message leftQueue() {
        return Message.join(prefix(), Message.raw("You left the battleground queue.").color(YELLOW));
    }

    public static Message leftWar() {
        return Message.join(prefix(), Message.raw("You left the war.").color(YELLOW));
    }

    public static Message alreadyInWar() {
        return Message.join(prefix(), Message.raw("You are already in a war!").color(RED));
    }

    public static Message missingFaction() {
        return Message.join(prefix(), Message.raw("Could not determine your faction. Choose a faction first.").color(RED));
    }

    public static Message notInWar() {
        return Message.join(prefix(), Message.raw("You are not in any war.").color(RED));
    }

    public static Message notInWarOrQueue() {
        return Message.join(prefix(), Message.raw("You are not in a battleground or queue.").color(RED));
    }

    public static Message queueStatus(int redQueued, int blueQueued, int required) {
        return Message.join(
            prefix(),
            Message.raw("Queue ").color(WHITE),
            Message.raw("Red ").color(RED),
            Message.raw(String.valueOf(redQueued)).color(RED).bold(true),
            Message.raw(" / ").color(GRAY),
            Message.raw("Blue ").color(BLUE),
            Message.raw(String.valueOf(blueQueued)).color(BLUE).bold(true),
            Message.raw(" (min ").color(GRAY),
            Message.raw(String.valueOf(required)).color(YELLOW).bold(true),
            Message.raw(" per side)").color(GRAY)
        );
    }

    public static Message matchFound(String warId, FactionRole faction, int teamSize) {
        Color factionColor = faction == FactionRole.RED ? RED : BLUE;
        return Message.join(
            prefix(),
            Message.raw("Match found! ").color(GREEN).bold(true),
            Message.raw("War ").color(WHITE),
            Message.raw(warId).color(AQUA).bold(true),
            Message.raw(" | Team ").color(WHITE),
            Message.raw(faction.getDisplayName()).color(factionColor).bold(true),
            Message.raw(" | ").color(WHITE),
            Message.raw(String.valueOf(teamSize)).color(YELLOW).bold(true),
            Message.raw("v").color(WHITE),
            Message.raw(String.valueOf(teamSize)).color(YELLOW).bold(true)
        );
    }

    public static Message returnedFromWar() {
        return Message.join(prefix(), Message.raw("Battleground finished. You were returned to your previous location.").color(YELLOW));
    }

    public static Message warNotFound() {
        return Message.join(prefix(), Message.raw("War not found!").color(RED));
    }

    public static Message warCreated(String warId) {
        return Message.join(prefix(), Message.raw("War created: ").color(GREEN), Message.raw(warId).color(AQUA));
    }

    public static Message warEnded() {
        return Message.join(prefix(), Message.raw("War has ended!").color(YELLOW));
    }

    private static Message prefix() {
        return Message.join(Message.raw("[").color(GRAY), Message.raw("Battlegrounds").color(GOLD).bold(true), Message.raw("] ").color(GRAY));
    }
}
