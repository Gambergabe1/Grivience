# Farming and Farm Hub

Last updated: 2026-03-13

## Overview

Farm Hub is a configured region system for Skyblock-style farming quality-of-life:

- `/farmhub` travel and spawn management
- region-based crop regeneration
- seedling-state replanting
- accelerated growth and forced growth passes
- forced growth maintenance that runs independently of natural growth events
- forced hydrated farmland
- scoreboard zone display as `Farm Hub`

Primary sources:

- `src/main/java/io/papermc/Grivience/skyblock/command/FarmHubCommand.java`
- `src/main/java/io/papermc/Grivience/skyblock/listener/FarmHubCropRegenerationListener.java`
- `src/main/java/io/papermc/Grivience/listener/FarmingFortuneListener.java`
- `src/main/java/io/papermc/Grivience/listener/EnchantedFarmCraftListener.java`
- `src/main/java/io/papermc/Grivience/stats/SkyblockScoreboardManager.java`
- `src/main/resources/config.yml` (`skyblock.farmhub-*`)
- `src/main/resources/plugin.yml` (`farmhub`)

## Player Flow

1. Admin sets Farm Hub spawn and crop region.
2. Player teleports with `/farmhub`.
3. In configured area:
   - harvested crops regenerate
   - replanted blocks return as seedlings/early stage
   - forced growth and hydration keep farm active
4. Scoreboard area text resolves to `Farm Hub`.

## Commands

- `/farmhub`
- `/farmhub set`
- `/farmhub setpos1`
- `/farmhub setpos2`
- `/farmhub setarea`
- `/farmhub info`

Examples:

- Setup: `/farmhub set` -> `/farmhub setpos1` -> `/farmhub setpos2` -> `/farmhub setarea`
- Validation: `/farmhub info`

## Permissions

`plugin.yml` does not declare a dedicated `farmhub` command permission.

Related bypass node from permissions section:

- `grivience.farmhub.build` (defaults to `op`)
  - allows build/interaction bypass in protected Farm Hub areas

## Configuration Keys

Main section: `skyblock`

Farm hub key groups:

- `farmhub-world`
- `farmhub-spawn.*`
- `farmhub-crop-area.*`
- `farmhub-crop-regen.*`
- `farmhub-crop-growth.*`
- `farmhub-farmland.*`
- `farmhub-maintenance.*`

Related:

- `zones.*`
- `scoreboard.custom.*`

## Data Files

- `plugins/Grivience/config.yml` (Farm Hub values)
- `plugins/Grivience/zones.yml` (zone entry for `farm_hub`)

No dedicated Farm Hub runtime data file is required for normal operation.

## Admin Setup

1. Set travel point: `/farmhub set`.
2. Mark region corners:
   - `/farmhub setpos1`
   - `/farmhub setpos2`
3. Save region: `/farmhub setarea`.
4. Verify with `/farmhub info`.
5. Reload if needed: `/grivience reload`.

## Balancing Notes

Recommended launch tuning checkpoints:

| Behavior | Config key | Typical value |
| --- | --- | --- |
| Regen delay | `skyblock.farmhub-crop-regen.delay-ticks` | `60` |
| Growth multiplier | `skyblock.farmhub-crop-growth.multiplier` | `4.0` |
| Forced growth interval | `skyblock.farmhub-crop-growth.force-interval-ticks` | `20` |
| Hydration interval | `skyblock.farmhub-farmland.hydrate-interval-ticks` | `40` |
| Maintenance scan budget | `skyblock.farmhub-maintenance.scan-blocks-per-run` | `50000` |

Supported crop ecosystem includes standard crops plus advanced cases (melon/pumpkin stems, kelp variants, chorus, torchflower, pitcher crop).

## Troubleshooting

- Crops not regenerating:
  - check `farmhub-crop-area.enabled` and region/world coordinates.
- Farmland dries out:
  - confirm `farmhub-farmland.force-hydrated: true`.
- Growth feels too slow:
  - tune `farmhub-crop-growth.multiplier` and force interval.
- Scoreboard not showing `Farm Hub`:
  - verify zone registration and world mapping.

### Common Mistakes

- Forgetting `/farmhub setarea` after setting corners.
- Defining corners in wrong world.
- Setting scan budgets too low for very large farm regions.

## Related Pages

- `Skyblock Leveling and Skills`
- `Configuration Reference`
- `Commands and Permissions`
