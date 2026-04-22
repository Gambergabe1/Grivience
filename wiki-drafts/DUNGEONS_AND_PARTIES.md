# Dungeons and Parties

Last updated: 2026-03-13

## Overview

Grivience dungeons run as party-based, instanced arenas inspired by Skyblock floor progression while keeping core dungeon concepts:

- Floor-based scaling (`F1` to `F5`)
- Party size requirements and invite flow
- Encounter mix (combat, puzzle, treasure, boss)
- Score and grade model (`S` to `D`)
- Reward commands by floor and grade
- Reward chest GUI pools with weighted drops

Primary sources:

- `src/main/java/io/papermc/Grivience/command/DungeonCommand.java`
- `src/main/java/io/papermc/Grivience/command/DungeonHubCommand.java`
- `src/main/java/io/papermc/Grivience/dungeon/DungeonManager.java`
- `src/main/java/io/papermc/Grivience/dungeon/DungeonSession.java`
- `src/main/java/io/papermc/Grivience/dungeon/FloorConfig.java`
- `src/main/resources/config.yml` (`dungeons.*`, `floors.*`)
- `src/main/resources/plugin.yml` (`dungeon`, `party`, `dungeonhub`)

## Player Flow

1. Create or join a party (`/party create`, `/party invite`, `/party accept`).
2. Leader checks floor options (`/dungeon floors`) and starts run (`/dungeon start <floor>`).
3. Party is teleported into an instanced arena generated from floor config.
4. Team clears room sequence:
   - Combat rooms
   - Puzzle rooms (sequence/sync/chime/seal)
   - Treasure rooms
   - Boss room
5. Room clears grant a Temple Key for gate unlock progression.
6. Run ends with score + grade; members receive configured command rewards and chest options.
7. Players are returned using configured dungeon exit behavior.

## Commands

From `plugin.yml` and command implementations:

- `/dungeon help`
- `/dungeon floors`
- `/dungeon menu`
- `/dungeon start <floor>`
- `/dungeon abandon`
- `/dungeon party <create|invite|accept|leave|kick|list|finder|gui>`
- `/dungeon reload` (admin-gated in code)
- `/dungeon give <player> <item_id> [amount]` (admin-gated in code)
- `/party <create|invite|accept|leave|kick|list|finder>`
- `/dungeonhub`
- `/dungeonhub set <world> <x> <y> <z> [yaw] [pitch]`

Example flow:

1. `/party create`
2. `/party invite Kazutos`
3. `/dungeon floors`
4. `/dungeon start F3`

## Permissions

`plugin.yml` does not assign dedicated player permission nodes for `dungeon`, `party`, or `dungeonhub`.

Code-level enforced admin checks:

- `grivience.admin` required for:
  - `/dungeon reload`
  - `/dungeon give ...`
  - `/dungeonhub set ...`

Party and run protection rules:

- Non-leaders cannot invite/kick/start.
- Party editing is blocked while party is inside an active run.
- Join/invite checks prevent invalid state transitions (already in party, full party, expired invite).

## Configuration Keys

`config.yml` keys:

- `dungeons.world`
- `dungeons.exit-world`
- `dungeons.exit-command`
- `dungeons.origin.*`
- `dungeons.arena-spacing`
- `dungeons.countdown-seconds`
- `dungeons.max-party-size`
- `dungeons.invite-timeout-seconds`
- `dungeons.mob-damage-multiplier`
- `dungeons.reward-chest.options-per-run`
- `dungeons.reward-chest.pools.*`
- `dungeons.hub.*`
- `floors.F1.*` ... `floors.F5.*`

Floor model highlights:

| Floor | Min Party | Max Party | Combat | Puzzle | Treasure |
| --- | ---: | ---: | ---: | ---: | ---: |
| `F1` | 1 | 5 | 4 | 1 | 1 |
| `F2` | 2 | 5 | 5 | 2 | 1 |
| `F3` | 3 | 5 | 6 | 2 | 2 |
| `F4` | 4 | 5 | 7 | 3 | 2 |
| `F5` | 4 | 5 | 8 | 3 | 2 |

## Data Files

Dungeon runtime sessions are in-memory and are not persisted as active run files.

Persistent related files:

- `plugins/Grivience/config.yml` (dungeon/floor config values)
- `plugins/Grivience/loot.yml` (optional reward chest override for `dungeons.reward-chest`)

## Admin Setup

1. Ensure dungeon world exists (`dungeons.world`).
2. Set dungeon hub:
   - `/dungeonhub set <world> <x> <y> <z> [yaw] [pitch]`
3. Tune party and invite limits:
   - `dungeons.max-party-size`
   - `dungeons.invite-timeout-seconds`
4. Validate floor blocks:
   - `floors.<id>.mob-pool`
   - `floors.<id>.boss-type`
   - `floors.<id>.floor-material`
   - `floors.<id>.wall-material`
5. Test full run:
   - `/party create`
   - `/dungeon start F1`
   - confirm score/grade/rewards on completion.

## Balancing Notes

Score formula from `DungeonSession`:

- Base: `300`
- Time penalty: `- elapsedSeconds / 6`
- Death penalty: `- 20 per death`
- Room bonuses:
  - `+12` per combat room
  - `+15` per puzzle room
  - `+8` per treasure room
- `+20` flat completion bonus
- `+20` additional speed bonus when `elapsedSeconds <= 900`
- Final score clamped to `0..300`

Grade thresholds:

| Grade | Score |
| --- | --- |
| `S` | `>= 270` |
| `A` | `220-269` |
| `B` | `170-219` |
| `C` | `120-169` |
| `D` | `< 120` |

Launch balancing checklist:

- Keep `dungeons.mob-damage-multiplier` conservative for first day.
- Verify reward command economy impact per floor and grade.
- Confirm `floors.F5.mob-health-multiplier` and `boss-health-multiplier` against target gear power.

## Troubleshooting

- "You must be in a party to start a dungeon":
  - Create a party first (`/party create`).
- "Only the party leader can start a dungeon":
  - Transfer workflow to leader.
- "Configured dungeon world ... does not exist":
  - Fix `dungeons.world` in config.
- "Need at least X online players":
  - Floor minimum party size not met.
- Party commands blocked in run:
  - Expected behavior while party has active dungeon session.
- No reward options:
  - Verify `dungeons.reward-chest.pools.*` or `loot.yml` validity.

## Related Pages

- `Skyblock Leveling and Skills`
- `Commands and Permissions`
- `Configuration Reference`
- `Data Files and Persistence`
