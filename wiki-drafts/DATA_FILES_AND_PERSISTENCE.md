# Data Files and Persistence

Last updated: 2026-03-13

## Overview

This page documents runtime data files generated under `plugins/Grivience/`.
Use this as the restore/backup checklist for launch and incident recovery.

Primary sources:

- `src/main/java/io/papermc/Grivience/**` (file path usage in managers)
- `WIKI_AUTHORING_README.md` required runtime file list

## Core Runtime Files (Required)

| File path | Purpose | Written by |
| --- | --- | --- |
| `plugins/Grivience/levels.yml` | Skyblock level, counters, progression state | `SkyblockLevelManager` |
| `plugins/Grivience/jumppads.yml` | Jump pad definitions and links | `JumpPadManager` |
| `plugins/Grivience/zones.yml` | Zone editor bounds/names/priority | `ZoneManager` |
| `plugins/Grivience/fasttravel.yml` | Fast-travel destination definitions plus player unlock state | `FastTravelManager` |
| `plugins/Grivience/collections/player_data.yml` | Per-player collection progress | `CollectionsManager` |
| `plugins/Grivience/collections/placed_blocks.yml` | Block placement tracking for anti-exploit collection logic | `CollectionListener` |
| `plugins/Grivience/bazaar_orders.yml` | Bazaar active orders/order book state | `BazaarOrderBook` / `BazaarOrderStore` |
| `plugins/Grivience/bazaar_history.yml` | Bazaar price history snapshots | `BazaarPriceHistory` |
| `plugins/Grivience/bazaar_bags.yml` | Bazaar claimable bag items | `BazaarShoppingBag` |
| `plugins/Grivience/bazaar_upgrades.yml` | Bazaar account upgrades | `BazaarAccountUpgrades` |
| `plugins/Grivience/storage_data.yml` | Storage inventory contents and metadata | `StorageManager` |
| `plugins/Grivience/pet-data.yml` | Legacy pet data / migration support | `PetManager` |
| `plugins/Grivience/welcome-claimed.yml` | Welcome reward claim flags | `WelcomeEventManager` |
| `plugins/Grivience/xp-boosts.yml` | Active/recorded XP boosts | `XPBoostManager` |
| `plugins/Grivience/mob-spawns.yml` | Custom and vanilla mobspawn point definitions | `CustomMonsterManager` |
| `plugins/Grivience/skyblock/minions.yml` | Minion placement and state | `MinionManager` |

## Additional Runtime Files (Also Generated)

| File path | Purpose | Written by |
| --- | --- | --- |
| `plugins/Grivience/loot.yml` | Optional override for dungeon reward chest pools | `RewardChestManager` |
| `plugins/Grivience/quest-progress.yml` | Generic quest progress tracking | `QuestManager` |
| `plugins/Grivience/welcome-quest-progress.yml` | Welcome questline progression | `QuestProgressManager` |
| `plugins/Grivience/wizard-tower-data.yml` | Wizard tower purchased tier state | `WizardTowerManager` |
| `plugins/Grivience/end-mines-mob-spawns.yml` | End Mines spawn point definitions | `EndMinesMobManager` |
| `plugins/Grivience/skyblock/profiles/<owner>/<profile>.yml` | Profile metadata and progression state | `ProfileManager` / island services |
| `plugins/Grivience/skyblock/islands/<island>.yml` | Island data per island ID | `IslandManager` |
| `plugins/Grivience/bits/<uuid>.yml` | Bits balance and stats per player | `BitsManager` |
| `plugins/Grivience/wardrobe/<uuid>.yml` | Wardrobe snapshots/loadouts | `WardrobeManager` |

## Files That Are Config, Not Runtime State

These are copied defaults and should be treated as configuration inputs:

- `plugins/Grivience/config.yml`
- `plugins/Grivience/collections.yml`
- `plugins/Grivience/pets.yml`
- `plugins/Grivience/quests.yml`
- `plugins/Grivience/storage_upgrades.yml`
- `plugins/Grivience/bazaar.yml`
- `plugins/Grivience/bazaar_products.yml`

## Backup and Restore Standard

Before updates:

1. Stop the server cleanly.
2. Back up the full `plugins/Grivience/` directory.
3. Keep at least one off-host backup copy.

For emergency restore:

1. Stop server.
2. Restore the latest known-good `plugins/Grivience/` backup.
3. Start server and verify:
   - `/grivience reload` (no exceptions in console)
   - `/sbadmin check <player>`
   - `/bazaar`
   - `/mobspawn list`
   - `/zone list`

## Data Integrity Notes

- Do not hand-edit runtime files while server is running.
- YAML key renames across releases should be migrated with a maintenance script, not manual search/replace.
- Dungeon sessions, active party mappings, and temporary encounter state are in-memory and reset on restart by design.

## Related Pages

- `Configuration Reference`
- `Commands and Permissions`
- `Dungeons and Parties`
