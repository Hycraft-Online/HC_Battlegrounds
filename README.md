# HC_Battlegrounds

Faction war battlegrounds with capture-the-flag gameplay for Hytale. Two factions (Red and Blue) compete in instanced arenas to capture and hold flag points, earning score based on control and player presence.

## Features

- Instanced battleground arenas with configurable flag positions and spawn points
- Capture-the-flag mechanics with capture progress, contestation, and overtime states
- Score-to-win system (default 2000 points) with time limit (default 15 minutes)
- Matchmaking queue with minimum player requirements per faction
- Player death tracking and respawn handling via ECS systems
- Real-time flag tick system that updates capture progress each tick
- Battleground HUD displaying scores, flag states, and timer
- Notification system for battleground events (captures, kills, war start/end)
- Arena creation and configuration commands for administrators
- Player commands for joining, leaving, and viewing war info
- Persistent arena configurations saved to disk
- Includes asset pack with battleground UI

## Dependencies

- Hytale:EntityModule
- Hytale:Instances

## Building

```bash
./gradlew build
```
