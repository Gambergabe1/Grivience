# Grivience Wiki Authoring README

This document is the master checklist for building a full wiki for the Grivience plugin.
It covers the Levels system, Enchanting, Farming, and every other major feature that should be documented.

## 1) Source of Truth (Use This Order)

Use this priority so wiki pages stay accurate:

1. Source code in `src/main/java/io/papermc/Grivience/`
2. `src/main/resources/plugin.yml` (commands, aliases, permissions)
3. `src/main/resources/config.yml` and other resource YAML files
4. Existing system docs in root `*_README.md`

If a README conflicts with code/config, trust code/config.

## 2) Required Wiki Structure

Create these top-level wiki sections:

- Getting Started
- Core Progression
- Combat and Equipment
- Farming and Gathering
- Economy and Trading
- Travel and World Systems
- Quests, Events, and Social
- Admin and Configuration
- Developer and Data Files

## 3) Required Core Pages (Highest Priority)

These pages must exist first.

### 3.1 Levels and Skills

Page title suggestion: `Skyblock Leveling and Skills`

Must include:

- What Skyblock Level is and what gives Skyblock XP
- Level caps and XP-per-level model
- Skill leveling model and per-skill XP requirements
- Dungeon, Bestiary, Slayer, and Track XP sources
- Feature unlock milestones
- Commands and admin controls
- Data persistence files

Primary sources:

- `src/main/java/io/papermc/Grivience/stats/SkyblockLevelManager.java`
- `src/main/java/io/papermc/Grivience/stats/SkyblockLevelListener.java`
- `src/main/java/io/papermc/Grivience/skills/SkyblockSkillManager.java`
- `src/main/java/io/papermc/Grivience/skills/SkillsCommand.java`
- `src/main/java/io/papermc/Grivience/skills/SkillXpAdminCommand.java`
- `src/main/resources/config.yml` (`skyblock-leveling.*`, `scoreboard.custom.*`)
- `src/main/resources/plugin.yml` (`skills`, `skillxp`, `sbadmin`)

Data file:

- `plugins/Grivience/levels.yml`

### 3.2 Enchanting and Anvil

Page title suggestion: `Enchanting System`

Must include:

- Enchant table and anvil GUI flow
- Enchantment rarity tiers and categories
- Ultimate enchantment rule (one ultimate per item)
- Dungeon enchantments and how they differ
- Conflict rules and examples
- XP cost behavior
- Commands for browsing and info lookup

Primary sources:

- `src/main/java/io/papermc/Grivience/enchantment/EnchantmentRegistry.java`
- `src/main/java/io/papermc/Grivience/enchantment/EnchantmentManager.java`
- `src/main/java/io/papermc/Grivience/enchantment/EnchantmentCommand.java`
- `src/main/java/io/papermc/Grivience/enchantment/EnchantmentTableGui.java`
- `src/main/java/io/papermc/Grivience/enchantment/SkyblockAnvilGui.java`
- `src/main/resources/config.yml` (`enchant-table.*`)
- `src/main/resources/plugin.yml` (`enchant`, `anvil`, `enchantinfo`, `enchantlist`)
- `ENCHANTMENT_SYSTEM_README.md`

### 3.3 Farming and Farm Hub

Page title suggestion: `Farming and Farm Hub`

Must include:

- `/farmhub` travel and setup flow
- Admin area setup (`setpos1`, `setpos2`, `setarea`)
- Crop regeneration behavior
- Seedling-stage replant behavior
- Accelerated growth behavior
- Forced hydrated farmland behavior
- Forced growth behavior that runs independently of natural growth events
- Scoreboard zone display as `Farm Hub`
- Related farming fortune and farming items

Primary sources:

- `src/main/java/io/papermc/Grivience/skyblock/command/FarmHubCommand.java`
- `src/main/java/io/papermc/Grivience/skyblock/listener/FarmHubCropRegenerationListener.java`
- `src/main/java/io/papermc/Grivience/listener/FarmingFortuneListener.java`
- `src/main/java/io/papermc/Grivience/listener/EnchantedFarmCraftListener.java`
- `src/main/java/io/papermc/Grivience/stats/SkyblockScoreboardManager.java`
- `src/main/resources/config.yml` (`skyblock.farmhub-*`, `skyblock.farmhub-crop-*`)
- `src/main/resources/plugin.yml` (`farmhub`)
- `src/main/resources/collections.yml` (farming collections/rewards)

## 4) Additional Required Pages (Everything Else)

Create these after the 3 core pages.

### 4.1 Dungeons and Parties

- Sources:
  - `src/main/java/io/papermc/Grivience/dungeon/`
  - `src/main/resources/config.yml` (`dungeons.*`, `floors.*`)
  - `src/main/resources/plugin.yml` (`dungeon`, `party`, `dungeonhub`)

### 4.2 Crafting

- Sources:
  - `src/main/java/io/papermc/Grivience/crafting/`
  - `src/main/resources/config.yml` (`crafting.*`)
  - `CRAFTING_SYSTEM_README.md`
  - `src/main/resources/plugin.yml` (`craft`, `crafting`, `recipe`)

### 4.3 Collections

- Sources:
  - `src/main/java/io/papermc/Grivience/collections/`
  - `src/main/resources/collections.yml`
  - `src/main/resources/plugin.yml` (`collection`, `collections`)

### 4.4 Bazaar

- Sources:
  - `src/main/java/io/papermc/Grivience/bazaar/`
  - `src/main/resources/config.yml` (`bazaar.*`)
  - `BAZAAR_README.md`
  - `src/main/resources/plugin.yml` (`bazaar`, `npcshop`)

### 4.5 Minions

- Sources:
  - `src/main/java/io/papermc/Grivience/minion/`
  - `src/main/resources/config.yml` (`minions.*` if present, plus related systems)
  - `MINION_SYSTEM_README.md`
  - `src/main/resources/plugin.yml` (`minion`, `minions`)

### 4.6 Storage

- Sources:
  - `src/main/java/io/papermc/Grivience/storage/`
  - `src/main/resources/config.yml` (`storage.*`)
  - `src/main/resources/storage_upgrades.yml`
  - `STORAGE_SYSTEM_README.md`
  - `src/main/resources/plugin.yml` (`storage`, `storages`)

### 4.7 Bank and Trade

- Sources:
  - `src/main/java/io/papermc/Grivience/bank/`
  - `src/main/java/io/papermc/Grivience/trade/`
  - `BANK_SYSTEM_README.md`
  - `TRADE_SYSTEM_README.md`
  - `src/main/resources/plugin.yml` (`bank`, `trade`)

### 4.8 Pets

- Sources:
  - `src/main/java/io/papermc/Grivience/pet/`
  - `src/main/resources/pets.yml`
  - `src/main/resources/plugin.yml` (`pets`)

### 4.9 Fast Travel and Hubs

- Sources:
  - `src/main/java/io/papermc/Grivience/fasttravel/`
  - `src/main/java/io/papermc/Grivience/skyblock/command/HubCommand.java`
  - `src/main/java/io/papermc/Grivience/skyblock/command/MinehubCommand.java`
  - `src/main/java/io/papermc/Grivience/skyblock/command/FarmHubCommand.java`
  - `src/main/resources/fasttravel.yml`
  - `src/main/resources/plugin.yml` (`fasttravel`, `hub`, `minehub`, `farmhub`, `endmine`)

### 4.10 Jump Pads

- Sources:
  - `src/main/java/io/papermc/Grivience/jumppad/JumpPadListener.java`
  - `src/main/java/io/papermc/Grivience/jumppad/JumpPadManager.java`
  - `src/main/java/io/papermc/Grivience/command/JumpPadCommand.java`
  - `src/main/resources/config.yml` (`jump-pads.*`)
  - `src/main/resources/plugin.yml` (`jumppad`)

Important note for wiki:

- Jump pad definitions persist in `plugins/Grivience/jumppads.yml`.

### 4.11 Zone Editor and Scoreboard Areas

- Sources:
  - `src/main/java/io/papermc/Grivience/zone/`
  - `src/main/java/io/papermc/Grivience/stats/SkyblockScoreboardManager.java`
  - `src/main/resources/config.yml` (`zones.*`, `scoreboard.custom.*`)
  - `ZONE_EDITOR_README.md`

### 4.12 Grappling Hook and Staffs

- Sources:
  - `src/main/java/io/papermc/Grivience/item/GrapplingHook*`
  - `src/main/java/io/papermc/Grivience/item/Staff*`
  - `src/main/resources/config.yml` (`grappling-hook.*`, `staffs.*`)
  - `GRAPPLING_HOOK_README.md`

### 4.13 Mining Systems and End Mines

- Sources:
  - `src/main/java/io/papermc/Grivience/mines/`
  - `src/main/java/io/papermc/Grivience/mines/end/`
  - `src/main/resources/config.yml` (`mining-events.*`, `end-mines.*`)
  - `src/main/resources/plugin.yml` (`mineevent`, `globalevent`, `endmine`)
  - `GLOBAL_EVENT_SYSTEM_README.md`

### 4.14 Welcome Event and Quests

- Sources:
  - `src/main/java/io/papermc/Grivience/welcome/`
  - `src/main/java/io/papermc/Grivience/quest/`
  - `src/main/resources/config.yml` (`welcome-event.*`)
  - `src/main/resources/quests.yml`
  - `WELCOME_EVENT_README.md`
  - `src/main/resources/plugin.yml` (`welcome`, `quest`, `questline`)

### 4.15 Islands, Profiles, and Core Skyblock

- Sources:
  - `src/main/java/io/papermc/Grivience/skyblock/`
  - `src/main/resources/config.yml` (`skyblock.*`, `skyblock-profiles.*`)
  - `src/main/resources/plugin.yml` (`skyblock`, `island`, `profile`, `visit`)

### 4.16 Custom Items, Armor, Monsters

- Sources:
  - `src/main/java/io/papermc/Grivience/item/`
  - `src/main/java/io/papermc/Grivience/mob/`
  - `src/main/resources/config.yml` (`custom-items.*`, `custom-armor.*`, `custom-monsters.*`, `resource-pack.*`)
  - `src/main/resources/plugin.yml` (`giveitem`, `mobspawn`)

## 5) Command and Permission Reference Page (Required)

Create a dedicated wiki page called `Commands and Permissions`.

Use these as the canonical source:

- `src/main/resources/plugin.yml` `commands:` section
- `src/main/resources/plugin.yml` `permissions:` section

For each command include:

- Syntax
- Aliases
- Required permission
- Player/admin examples
- Error cases

## 6) Config Reference Page (Required)

Create `Configuration Reference` with one section per top-level key in `config.yml`:

- `dungeons`
- `navigation-item`
- `crafting`
- `skyblock`
- `end-mines`
- `scoreboard`
- `skyblock-leveling`
- `enchant-table`
- `custom-items`
- `bazaar`
- `zones`
- `grappling-hook`
- `staffs`
- `skyblock-profiles`
- `mining-events`
- `welcome-event`
- `custom-monsters`
- `custom-armor`
- `floors`
- `resource-pack`
- `jump-pads`
- `storage`

Also include these auxiliary config files:

- `src/main/resources/collections.yml`
- `src/main/resources/fasttravel.yml`
- `src/main/resources/pets.yml`
- `src/main/resources/quests.yml`
- `src/main/resources/storage_upgrades.yml`

## 7) Runtime Data Files Page (Required)

Create `Data Files and Persistence`.

Document where plugin runtime state is stored:

- `plugins/Grivience/levels.yml`
- `plugins/Grivience/jumppads.yml`
- `plugins/Grivience/zones.yml`
- `plugins/Grivience/fasttravel.yml`
- `plugins/Grivience/collections/player_data.yml`
- `plugins/Grivience/collections/placed_blocks.yml`
- `plugins/Grivience/bazaar_orders.yml`
- `plugins/Grivience/bazaar_history.yml`
- `plugins/Grivience/bazaar_bags.yml`
- `plugins/Grivience/bazaar_upgrades.yml`
- `plugins/Grivience/storage_data.yml`
- `plugins/Grivience/pet-data.yml`
- `plugins/Grivience/welcome-claimed.yml`
- `plugins/Grivience/xp-boosts.yml`
- `plugins/Grivience/mob-spawns.yml`
- `plugins/Grivience/skyblock/minions.yml`

## 8) Wiki Page Template (Use for Every System Page)

Copy this template:

```md
# <System Name>

## Overview
## Player Flow
## Commands
## Permissions
## Configuration Keys
## Data Files
## Admin Setup
## Balancing Notes
## Troubleshooting
## Related Pages
```

## 9) Required Visual/Media Checklist

For each player-facing system page include:

- At least 1 GUI screenshot
- At least 1 command example
- At least 1 config snippet
- At least 1 practical scenario ("how to use")

For progression pages (levels, skills, enchants, farming):

- Include at least one table (XP, costs, caps, or tiers)
- Include one "common mistakes" section

## 10) Quality Gate Before Publishing Any Wiki Page

Do not publish until all checks pass:

- Command syntax matches `plugin.yml`
- Permission nodes match `plugin.yml`
- Config paths match exact YAML keys
- Claimed mechanics are backed by class-level source
- Page links to related systems
- Last updated date is present

## 11) Fast Authoring Workflow

Use this order for fastest completion:

1. Publish `Commands and Permissions`
2. Publish `Configuration Reference`
3. Publish `Skyblock Leveling and Skills`
4. Publish `Enchanting System`
5. Publish `Farming and Farm Hub`
6. Publish remaining systems by section
7. Publish `Data Files and Persistence`

## 12) Existing Docs to Reuse

You already have high-value docs in repo root:

- `README.md`
- `ENCHANTMENT_SYSTEM_README.md`
- `CRAFTING_SYSTEM_README.md`
- `BAZAAR_README.md`
- `MINION_SYSTEM_README.md`
- `STORAGE_SYSTEM_README.md`
- `BANK_SYSTEM_README.md`
- `TRADE_SYSTEM_README.md`
- `ZONE_EDITOR_README.md`
- `GRAPPLING_HOOK_README.md`
- `GLOBAL_EVENT_SYSTEM_README.md`
- `WELCOME_EVENT_README.md`

Treat these as draft content, then verify against code/config before wiki publication.

