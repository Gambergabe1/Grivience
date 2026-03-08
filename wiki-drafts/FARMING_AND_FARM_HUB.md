# Farming and Farm Hub

Last updated: 2026-03-08

## Overview

The Farm Hub system provides a configurable farming region with:

- Farm Hub teleport and spawn management
- Region-based crop regeneration
- Seedling-stage replanting
- Accelerated crop growth
- Forced farmland hydration
- Forced growth maintenance pass (independent of natural growth events)
- Scoreboard area labeling via zone integration

Primary sources:

- `src/main/java/io/papermc/Grivience/skyblock/command/FarmHubCommand.java`
- `src/main/java/io/papermc/Grivience/skyblock/listener/FarmHubCropRegenerationListener.java`
- `src/main/java/io/papermc/Grivience/listener/FarmingFortuneListener.java`
- `src/main/java/io/papermc/Grivience/listener/EnchantedFarmCraftListener.java`
- `src/main/java/io/papermc/Grivience/stats/SkyblockScoreboardManager.java`

## Player Flow

1. Admin defines Farm Hub spawn and crop area.
2. Players teleport with `/farmhub`.
3. Crop blocks in the configured area:
   - Regenerate after break
   - Replant as seedling/early stage
   - Grow faster than normal
4. Farmland in area remains hydrated.
5. Scoreboard shows the farm zone/world as `Farm Hub`.

## Admin Setup

### Commands

From `plugin.yml` and command implementation:

- `/farmhub`
  - Teleports player to Farm Hub.
- `/farmhub set`
  - Sets farm hub spawn.
- `/farmhub setpos1`
- `/farmhub setpos2`
- `/farmhub setarea`
  - Saves crop regeneration area.
- `/farmhub info`
  - Shows active area and tuning values.

### Setup Sequence

1. Stand at desired spawn and run `/farmhub set`.
2. Mark region corners:
   - `/farmhub setpos1`
   - `/farmhub setpos2`
3. Apply area with `/farmhub setarea`.
4. Confirm values with `/farmhub info`.
5. Reload if needed with `/grivience reload`.

## Crop Regeneration Mechanics

When a supported crop is broken in the configured area:

- Regeneration is scheduled after `skyblock.farmhub-crop-regen.delay-ticks` (default `60`).
- Replanting uses seedling-style state:
  - Ageable crops set to age `0`.
  - Bisected plants restore bottom half seedling and clear top half.
  - Vertical crops restore base and clear upper column.

Special seedling normalization:

- `ATTACHED_MELON_STEM` -> `MELON_STEM`
- `ATTACHED_PUMPKIN_STEM` -> `PUMPKIN_STEM`
- `PITCHER_PLANT` -> `PITCHER_CROP`
- `KELP_PLANT` -> `KELP`
- `CHORUS_PLANT` -> `CHORUS_FLOWER`

Current behavior note:

- Melon and pumpkin blocks keep their existing-style restoration behavior (not forced to stem conversion globally).

## Accelerated Growth Mechanics

Two growth paths are active:

- Event-time boost (`BlockGrowEvent`) based on multiplier
- Forced growth maintenance pass on interval

Key tuning:

- `skyblock.farmhub-crop-growth.multiplier: 4.0`
- `skyblock.farmhub-crop-growth.force-growth: true`
- `skyblock.farmhub-crop-growth.force-interval-ticks: 20`
- `skyblock.farmhub-crop-growth.max-scan-blocks: 300000`
- `skyblock.farmhub-maintenance.scan-blocks-per-run: 50000`

Because forced growth directly updates block data during maintenance scans, growth continues even when natural growth events are restricted by external region settings.

## Farmland Hydration Mechanics

Hydration enforcement in the area includes:

- Cancels moisture loss events.
- Cancels farmland fade-to-dirt events.
- Periodically scans and forces farmland to maximum moisture.

Key tuning:

- `skyblock.farmhub-farmland.force-hydrated: true`
- `skyblock.farmhub-farmland.hydrate-interval-ticks: 40`
- `skyblock.farmhub-farmland.max-scan-blocks: 350000`

## Supported Crop Types

Regeneration/growth list includes:

- Wheat, carrots, potatoes, beetroot, nether wart, cocoa
- Sugar cane, cactus, bamboo, kelp/kelp plant
- Sweet berry bush
- Melon, pumpkin, stems/attached stems
- Torchflower crop, pitcher crop/plant
- Chorus plant/flower

## Farming Fortune Integration

`FarmingFortuneListener` applies Skyblock-style extra drops to primary crop outputs:

- Extra drops scale with `farming_fortune / 100`.
- Fractional extra drops are chance-based.
- Only mature ageable crops are boosted.
- Uses combat engine/stat manager farming fortune source.

## Enchanted Farming Item Crafting

`EnchantedFarmCraftListener` supports compression-style crafting:

- 160 base material -> tier 1 enchanted farm item
- 160 tier 1 enchanted item -> tier 2 form
- Restricted to 3x3 crafting table workflow

See:

- `src/main/java/io/papermc/Grivience/item/EnchantedFarmItemType.java`

## Scoreboard Area Display

Area text resolves in this order:

1. Dungeon session context
2. Custom zone (ZoneManager)
3. Island context
4. World fallback (`hub`, `minehub`, `farmhub`, etc.)

Farm Hub zone sync:

- `/farmhub setarea` ensures/updates zone ID `farm_hub` with display name `Farm Hub`.

## Configuration Keys

In `config.yml` under `skyblock`:

- `farmhub-world`
- `farmhub-spawn.*`
- `farmhub-crop-area.*`
- `farmhub-crop-regen.*`
- `farmhub-crop-growth.*`
- `farmhub-farmland.*`
- `farmhub-maintenance.*`

Related:

- `scoreboard.custom.*`
- `zones.*`

## Data Files

- `plugins/Grivience/config.yml` (farmhub keys)
- `plugins/Grivience/zones.yml` (farm_hub zone bounds/display)

No standalone farm-regen database file is required; configuration + zone data are the authoritative state.

## Troubleshooting

- Crops not regenerating:
  - Verify `farmhub-crop-area.enabled` is true and world/pos values are correct.
- Farmland still drying:
  - Verify `farmhub-farmland.force-hydrated` is true and interval is running.
- Growth seems slow:
  - Check multiplier and force-growth interval settings.
- Scoreboard not showing Farm Hub:
  - Confirm `farm_hub` zone exists or `skyblock.farmhub-world` matches current world.
- Some crops unaffected:
  - Confirm crop type is in the regenerating set and region bounds include the block.

## Related Pages

- `Skyblock Leveling and Skills`
- `Collections`
- `Fast Travel and Hubs`
- `Zone Editor and Scoreboard Areas`
