# Hide and Seek Minecraft Mod - AI Development Guide

## Project Architecture

This is a **NeoForge 21.1.209** Minecraft mod (1.21.1) implementing a multiplayer Hide and Seek game. The codebase follows NeoForge's modern deferred registration pattern and event-driven architecture.

### Core Structure
- **Main mod class**: `HideSeek.java` - Handles mod initialization, deferred registries, and server events
- **Client-side logic**: `HideSeekClient.java` - Handles client-specific setup and config screens  
- **Configuration**: `Config.java` - ModConfigSpec-based configuration with validation
- **Game specification**: `.github/instructions/documentacao.instructions.md` - Complete game design document

## Critical Development Context

### Game State Machine (from specs)
The mod must implement a state-driven game flow:
```
LOBBY → STARTING → HIDING → SEEKING → ENDING → LOBBY
```
Each state requires specific player management, teleportation, effects, and UI updates.

### Essential Commands to Implement
Priority commands from specifications:
- `/hns join/leave` - Player queue management
- `/hns set lobby/seekerspawn/mapboundary` - World setup with coordinate persistence
- `/hns randomize <min> <max>` - Team assignment logic
- `/hns start/stop` - Game lifecycle control
- `/hns set time <phase> <seconds>` - Timer configuration

### Key Technical Patterns

**Event Registration**: Use `@SubscribeEvent` on `NeoForge.EVENT_BUS` for game events. Client events go on mod event bus.

**Deferred Registration**: Follow existing pattern in `HideSeek.java`:
```java
public static final DeferredRegister<T> REGISTRY = DeferredRegister.create(Registry, MODID);
```

**Config Pattern**: Extend `Config.java` using `ModConfigSpec.Builder` with validation:
```java
public static final ModConfigSpec.IntValue HIDE_TIME = BUILDER
    .defineInRange("hideTime", 60, 10, 600);
```

**Client/Server Split**: Server logic in main classes, client rendering/UI in `HideSeekClient.java` with `@Mod(dist = Dist.CLIENT)`.

## Development Workflow

**Build**: `./gradlew build` - Compiles mod into `build/libs/`
**Run Client**: `./gradlew runClient` - Launches test client environment  
**Run Server**: `./gradlew runServer` - Launches test server with `--nogui`
**Data Gen**: `./gradlew runData` - Generates resources (recipes, tags, etc.)

**IDE Setup**: Project uses IntelliJ IDEA recommended settings with automatic source/javadoc downloads enabled.

## Implementation Priorities

1. **Command System**: Create command classes that integrate with NeoForge's command registration system
2. **Game State Manager**: Central class tracking current game state and coordinating transitions
3. **Player Data**: Persistent storage for spawn points, team assignments, and game statistics  
4. **Timer System**: BossBar integration for countdown displays as specified
5. **Effects Management**: Slowness/blindness application for Seekers during HIDING phase

## Critical Integration Points

**World Data Persistence**: Spawn coordinates must survive server restarts - use `SavedData` or config files.

**Scoreboard Integration**: Real-time team status and player counts via Minecraft's scoreboard system.

**Teleportation Safety**: Validate spawn points and handle cross-dimensional teleportation properly.

**Permission System**: Commands marked `OP` in specs require proper permission checking via `hasPermission()`.

## Resource Structure
- `assets/hideseek/lang/` - Localization (extend `en_us.json` for command feedback)
- `data/hideseek/` - Game data (loot tables, structures if needed)
- `META-INF/neoforge.mods.toml` - Mod metadata (auto-generated from `gradle.properties`)

## Testing Approach
Use `runClient` and `runServer` gradle tasks with multiple client instances for multiplayer testing. The mod requires at least 2 players for meaningful gameplay testing.