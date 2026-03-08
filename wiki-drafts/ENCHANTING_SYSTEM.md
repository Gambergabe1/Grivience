# Enchanting System

Last updated: 2026-03-08

## Overview

Grivience provides a Skyblock-style enchanting stack with:

- Enchantment Table GUI (`/enchant`, `/et`)
- Anvil Combining GUI (`/anvil`, `/av`)
- Registry-driven custom enchant definitions
- Conflict handling
- Ultimate enchant limit (one ultimate per item)

Primary sources:

- `src/main/java/io/papermc/Grivience/enchantment/EnchantmentManager.java`
- `src/main/java/io/papermc/Grivience/enchantment/EnchantmentTableGui.java`
- `src/main/java/io/papermc/Grivience/enchantment/SkyblockAnvilGui.java`
- `src/main/java/io/papermc/Grivience/enchantment/EnchantmentRegistry.java`
- `src/main/java/io/papermc/Grivience/enchantment/SkyblockEnchantStorage.java`

## Player Flow

### Enchantment Table

1. Hold exactly one item in main hand.
2. Run `/enchant` (or use an enchanting table interaction flow).
3. Pick enchantment, then pick level.
4. Pay XP levels.
5. Item is updated with enchant data and lore.

### Anvil

1. Run `/anvil`.
2. Put target item + ingredient (book/item).
3. Preview result and XP level cost.
4. Click output to apply.

## Rules and Mechanics

### Applicability

Enchantments are category-checked (`SkyblockEnchantment.canEnchantItem`), including:

- Weapon classes (sword/axe/bow)
- Tool classes
- Armor pieces
- Universal/equipment categories

### Conflict Rules

- Conflicting enchantments cannot coexist.
- Conflict check is performed before apply in table/anvil flows.

### Ultimate Enchants

- Only one ultimate enchant is allowed per item.
- If an item already has a different ultimate, apply is blocked.

### XP Cost Model

- Enchantment table apply cost: `baseXpCost * selectedLevel` (from enchant definition).
- Cost is paid from **player vanilla XP levels**.
- Applying enchants records enchanting progression via `SkyblockLevelManager.recordEnchanting(...)`.

### Anvil Discount

- Combining with an enchanted book applies a **25% cost discount**.

## Rarity / Type Tiers

Defined in `EnchantmentType`:

- Common
- Uncommon
- Rare
- Epic
- Legendary
- Mythic
- Special
- Very Special

## Commands

From `plugin.yml`:

- `/enchant [page]` (aliases: `et`, `enchantmenttable`)
- `/anvil` (alias: `av`)
- `/enchantinfo <enchantment_id>` (alias: `ei`)
- `/enchantlist [page]` (alias: `el`)

## Permissions

No separate custom permission node is declared for these player commands in `plugin.yml`.
Use your server permission plugin policy as needed.

## Configuration Keys

Config section: `enchant-table` in `src/main/resources/config.yml`:

- `enchant-table.enabled`
- `enchant-table.vanilla.base-cost`
- `enchant-table.vanilla.per-level-cost`
- `enchant-table.vanilla.max-cost`
- `enchant-table.cost-multipliers.*`
- `enchant-table.eco.command-template`
- `enchant-table.options.*`

Implementation note:

- The active Skyblock enchant UI is primarily registry-driven from `EnchantmentRegistry`.
- The `enchant-table` config still controls legacy/auxiliary option behavior via listener layer.

## Data Storage

Enchants are stored on items:

- Vanilla enchant tags (when mapped)
- Persistent Data Container keys with prefix: `sb_enchant_`
- Lore lines are rebuilt from stored enchant levels

No standalone enchant database file is required for normal operation.

## Admin Setup

1. Ensure commands exist in `plugin.yml`.
2. Tune `enchant-table.*` config if using configured option paths.
3. Reload with `/grivience reload`.
4. Validate with:
   - `/enchantlist`
   - `/enchantinfo <id>`
   - Table/anvil GUI interaction tests

## Troubleshooting

- "That enchantment cannot be applied":
  - Item category mismatch.
- "Conflicts with X":
  - Existing enchant conflict in registry rules.
- "Only one ultimate enchant can be applied":
  - Item already has a different ultimate enchant.
- "Not enough levels":
  - Player level is lower than computed XP cost.
- Enchant lore not refreshing:
  - Reapply/refresh item through enchant system; storage rewrites lore from PDC.

## Related Pages

- `Skyblock Leveling and Skills`
- `Custom Items, Armor, Monsters`
- `Dungeons and Parties`
