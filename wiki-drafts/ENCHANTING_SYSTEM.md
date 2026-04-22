# Enchanting System

Last updated: 2026-03-13

## Overview

Grivience provides a Skyblock-style enchanting stack with table/anvil GUI workflows and registry-driven custom enchant logic.

Core mechanics:

- Enchantment table GUI (`/enchant`)
- Anvil combining GUI (`/anvil`)
- Custom enchant registry and compatibility checks
- Conflict enforcement and one-ultimate-per-item rule
- XP level cost scaling and anvil book discount logic

Primary sources:

- `src/main/java/io/papermc/Grivience/enchantment/EnchantmentRegistry.java`
- `src/main/java/io/papermc/Grivience/enchantment/EnchantmentManager.java`
- `src/main/java/io/papermc/Grivience/enchantment/EnchantmentTableGui.java`
- `src/main/java/io/papermc/Grivience/enchantment/SkyblockAnvilGui.java`
- `src/main/resources/config.yml` (`enchant-table.*`)
- `src/main/resources/plugin.yml` (`enchant`, `anvil`, `enchantinfo`, `enchantlist`)

## Player Flow

1. Hold target item and run `/enchant`.
2. Choose enchant and level from GUI.
3. Pay vanilla XP level cost.
4. Apply enchant; lore and PDC data are updated.
5. Use `/anvil` to combine items/books with conflict + ultimate checks.

## Commands

- `/enchant [page]` (aliases: `et`, `enchantmenttable`)
- `/anvil` (alias: `av`)
- `/enchantinfo <enchantment_id>` (alias: `ei`)
- `/enchantlist [page]` (alias: `el`)

Examples:

- Player: `/enchant`
- Player: `/anvil`
- Lookup: `/enchantinfo sharpness`

## Permissions

No dedicated enchant command permission nodes are declared in `plugin.yml`.
Access policy should be managed by server command/permission plugins if needed.

## Configuration Keys

Main section: `enchant-table`

Important keys:

- `enchant-table.enabled`
- `enchant-table.vanilla.base-cost`
- `enchant-table.vanilla.per-level-cost`
- `enchant-table.vanilla.max-cost`
- `enchant-table.cost-multipliers.*`
- `enchant-table.eco.command-template`
- `enchant-table.options.*`

## Data Files

Enchant state is item-bound, not file-bound:

- Item Persistent Data Container keys: `sb_enchant_*`
- Lore is rebuilt from stored enchant values

No standalone enchant runtime `.yml` database is required.

## Admin Setup

1. Verify commands in `plugin.yml`.
2. Tune `enchant-table.*` values in `config.yml`.
3. Reload: `/grivience reload`.
4. Validate:
   - `/enchantlist`
   - `/enchantinfo <id>`
   - table/anvil GUI apply flow

## Balancing Notes

Cost and rarity summary:

| Item | Rule |
| --- | --- |
| Table apply cost | `baseXpCost * selectedLevel` |
| Cost currency | Player vanilla XP levels |
| Anvil enchanted-book combine | 25% cost discount |
| Ultimate enchantments | Max one ultimate per item |
| Conflict handling | Registry-driven hard block |

Rarity tiers defined in `EnchantmentType`:

- `COMMON`
- `UNCOMMON`
- `RARE`
- `EPIC`
- `LEGENDARY`
- `MYTHIC`
- `SPECIAL`
- `VERY_SPECIAL`

## Troubleshooting

- "That enchantment cannot be applied":
  - Item category mismatch.
- "Conflicts with X":
  - Existing enchant conflicts in registry.
- "Only one ultimate enchant can be applied":
  - Item already has a different ultimate enchant.
- "Not enough levels":
  - Insufficient vanilla XP levels for computed cost.

### Common Mistakes

- Forgetting that costs use vanilla XP levels, not coins.
- Expecting conflicting enchants to combine via anvil.
- Treating ultimate enchants as stackable.

## Related Pages

- `Skyblock Leveling and Skills`
- `Dungeons and Parties`
- `Commands and Permissions`
