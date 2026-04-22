# Skyblock Leveling and Skills

Last updated: 2026-03-13

## Overview

Grivience uses profile-scoped Skyblock progression with:

- Global Skyblock XP and level
- Skill XP and skill levels
- Category tracks (Core/Event/Dungeon/Essence/Slaying/Skill/Misc/Story)
- Action, objective, milestone, and dungeon-derived progression sources

Primary sources:

- `src/main/java/io/papermc/Grivience/stats/SkyblockLevelManager.java`
- `src/main/java/io/papermc/Grivience/stats/SkyblockLevelListener.java`
- `src/main/java/io/papermc/Grivience/skills/SkyblockSkillManager.java`
- `src/main/java/io/papermc/Grivience/skills/SkillsCommand.java`
- `src/main/java/io/papermc/Grivience/skills/SkillXpAdminCommand.java`
- `src/main/resources/config.yml` (`skyblock-leveling.*`)
- `src/main/resources/plugin.yml` (`skills`, `skillxp`, `sbadmin`)

## Player Flow

1. Player performs gameplay actions (combat, mining, farming, fishing, dungeons, crafting, questing).
2. Listener/manager updates counters and applies:
   - skill XP
   - objective XP
   - category track XP
   - milestone progress
3. Total XP maps into Skyblock level progression.
4. Level milestones unlock configured features and rewards.

## Commands

- `/skills [skill_id]` (alias: `skill`)
- `/skillxp <give|check> <player> [skill] [amount]`
- `/sbadmin <set|setxp|give|take|check|reset> <player> [value]`

Examples:

- Player: `/skills`
- Admin: `/skillxp give Kazutos farming 5000`
- Admin: `/sbadmin check Kazutos`

## Permissions

- `skills` command: no explicit permission node in `plugin.yml`
- `skillxp`: `grivience.admin`
- `sbadmin`: `grivience.admin`

## Configuration Keys

Main section: `skyblock-leveling`

Important groups:

- `max-level`
- `xp-per-level`
- `notify-xp-gain`
- `skill-leveling.*` (skill XP requirements)
- `skill-level-xp-rewards.*`
- `dungeons.*`
- `bestiary.*`
- `slayer.*`
- `action-xp.*`
- `skill-actions-per-level.*`
- `level-rewards.*`
- `tracks.*`

Related display:

- `scoreboard.custom.*`

## Data Files

- `plugins/Grivience/levels.yml`
  - Skyblock XP totals
  - progression counters
  - milestone states
  - objective states

## Admin Setup

1. Tune `skyblock-leveling.*` in `config.yml`.
2. Reload systems: `/grivience reload`.
3. Validate with:
   - `/sbadmin check <player>`
   - `/skillxp check <player>`
4. Verify scoreboard and progression feedback in live gameplay.

## Balancing Notes

Core progression defaults:

| System | Default value | Config path |
| --- | --- | --- |
| Skyblock max level | `521` | `skyblock-leveling.max-level` |
| Skyblock XP per level | `100` | `skyblock-leveling.xp-per-level` |
| Skill max level | `60` (most skills) | `skyblock-leveling.skill-leveling.max-level` |
| Catacombs level cap | `50` | `skyblock-leveling.dungeons.catacombs-level-cap` |
| Class level cap | `50` | `skyblock-leveling.dungeons.class-level-cap` |

Dungeon + track integration notes:

- Dungeon completion routes through `recordDungeonCompletion(...)`.
- Bestiary uses `bestiary_kills.*` counters.
- Category tracks use `tracks.*.counter-key` and milestone arrays.

## Troubleshooting

- No skill gain on farm/mining/combat:
  - verify listeners are active and action criteria are met.
- Level appears split unexpectedly:
  - progression is profile-scoped; confirm active profile/co-op mapping.
- Admin command works for one command but not another:
  - ensure sender has `grivience.admin`.
- Scoreboard not reflecting progression:
  - verify `scoreboard.custom.enabled` and update interval settings.

### Common Mistakes

- Editing `levels.yml` while the server is running.
- Setting impossible XP requirements and forgetting to tune action rewards.
- Testing progression in creative/spectator and expecting normal gain behavior.

## Related Pages

- `Enchanting System`
- `Farming and Farm Hub`
- `Dungeons and Parties`
- `Commands and Permissions`
