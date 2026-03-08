# Skyblock Leveling and Skills

Last updated: 2026-03-08

## Overview

Grivience uses a Skyblock-style progression model with:

- **Skyblock Level** (global profile progression)
- **Skill Levels** (Combat, Mining, Farming, etc.)
- **Category Tracks** (Core, Skill, Dungeon, Slaying, Story, etc.)
- **Objective and milestone XP** (one-time and repeatable progression sources)

Core manager: `src/main/java/io/papermc/Grivience/stats/SkyblockLevelManager.java`.

## Player Flow

1. Perform gameplay actions (farm, mine, fish, kill mobs, clear dungeons, craft, etc.).
2. Action counters update and can award:
   - Skill XP
   - Category XP
   - Objective XP
   - Milestone progress
3. Skyblock XP is applied to your active profile progress.
4. On level-up, players receive level-up messages, sounds, and configured unlocks.

## Progression Model

### Skyblock Level

- Default XP per level: `100` (`skyblock-leveling.xp-per-level`)
- Default max level: `521` (`skyblock-leveling.max-level`)
- Display/notification toggle: `skyblock-leveling.notify-xp-gain`

Formula:

- `level = floor(total_xp / xp_per_level)`

### Skills

Implemented skills are in `SkyblockSkill`:

- Combat
- Mining
- Farming
- Foraging
- Fishing
- Enchanting
- Alchemy
- Taming
- Hunting
- Dungeoneering
- Carpentry

Behavior notes:

- Most skills have max level 60.
- Dungeoneering max is 50.
- Skills track XP with counters like `skill_xp.farming`.

## XP Sources (Implemented)

From `SkyblockLevelListener` + `SkyblockLevelManager`:

- Mob kills (`recordCombatKill`)
- Mining block breaks (`recordMiningOre`)
- Foraging logs (`recordForagingLog`)
- Mature crop harvests (`recordFarmingHarvest`)
- Fishing catches / sea creature kills (`recordFishingCatch`)
- Enchanting level spend (`recordEnchanting`)
- Potion output interactions (`recordAlchemy`)
- Crafting actions (`recordCarpentry`)
- Dungeon completion/rank/score (`recordDungeonCompletion`)
- Quest completion (`recordQuestCompletion`)
- Island create/upgrade (`recordIslandCreated`, `recordIslandUpgrade`)

## Bestiary, Dungeon, and Track Systems

### Bestiary

- Uses `bestiary_kills.<mob>` counters.
- Awards slaying category XP and milestone XP from `skyblock-leveling.bestiary.*`.

### Dungeon Pseudo-Leveling

- Uses dungeon completion counters.
- Catacombs/class pseudo-level rewards come from:
  - `skyblock-leveling.dungeons.catacombs-level-xp-rewards.*`
  - `skyblock-leveling.dungeons.class-level-xp-reward`

### Guide Tracks

Configured under `skyblock-leveling.tracks.*` (Core/Event/Dungeon/Essence/Slaying/Skill/Misc/Story).
Each track has:

- `counter-key`
- `milestones`
- `rewards`
- Display metadata (name/icon/lore)

## Profile and Co-op Scope

Progress is **profile-scoped**, not just player-UUID scoped:

- Selected profile ID is used when available.
- Co-op profile ID is used for co-op members.
- Legacy owner-scoped data can migrate into profile-scoped storage.

Reference: `resolveProfileId(...)` in `SkyblockLevelManager`.

## Commands

From `plugin.yml`:

- `/skills [skill_id]`
  - Opens skills menu or specific skill details.
- `/skillxp <give|check> <player> [skill] [amount]`
  - Admin skill XP controls.
- `/sbadmin <set|setxp|give|take|check|reset> <player> [value]`
  - Admin Skyblock level/XP controls.

## Permissions

- `grivience.admin` (required for admin commands above)

## Configuration Keys

Primary section: `skyblock-leveling` in `src/main/resources/config.yml`.

Key groups:

- `xp-per-level`, `max-level`, `notify-xp-gain`
- `skill-leveling.*` (including level 1-60 XP table)
- `skill-level-xp-rewards.*`
- `dungeons.*` (catacombs/class rewards + floor rewards + score/rank objectives)
- `bestiary.*`
- `action-xp.*`
- `skill-actions-per-level.*`
- `level-rewards.*`
- `tracks.*`

Related display config:

- `scoreboard.custom.*`

## Data Files

- `plugins/Grivience/levels.yml`
  - Main persistent storage for:
    - total Skyblock XP
    - progression counters
    - objective states
    - milestone states

## Admin Setup

1. Tune `skyblock-leveling.*` in `config.yml`.
2. Reload plugin systems with `/grivience reload`.
3. Validate using:
   - `/sbadmin check <player>`
   - `/skillxp check <player>`

## Troubleshooting

- No progression from crop breaks:
  - Ensure crops are mature; immature ageable crops are ignored for farming XP.
- No progression in creative mode:
  - Creative/spectator are intentionally bypassed.
- Unexpected profile progression split:
  - Check selected profile/co-op assignments.
- Level cap hit:
  - Verify `skyblock-leveling.max-level` and available XP sources.

## Related Pages

- `Enchanting System`
- `Farming and Farm Hub`
- `Collections`
- `Dungeons and Parties`
