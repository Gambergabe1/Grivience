# Grivience Dungeons Plugin

Japanese-themed party dungeon plugin for Paper inspired by SkyBlock-style floor runs.

## Core Features
- Party flow: create, invite, accept, kick, leave, list.
- Floor-based runs with configurable room count and mob pools.
- Procedural room arena generation per run.
- Procedural layouts can turn in different cardinal directions between rooms.
- Japanese visual theming (torii-style lobby, shrine puzzles, temple boss sanctum).
- Mixed room types: combat, sequence puzzle, sync puzzle, and treasure.
- Linked room layout with physical corridors and sealed gates.
- Key progression: each cleared room grants a Temple Key to unlock the next gate.
- Custom folklore mobs (Yokai archetypes) with unique names, gear, and effects.
- Custom Hypixel-style weapons can drop from dungeon mobs.
- Multiple dungeon armor sets (`Shogun`, `Shinobi`, `Onmyoji`) with full-set bonuses.
- Reforge stones + reforge-anvil workflow for custom dungeon weapons.
- Custom Reforge Anvil UI (weapon slot, stone slot, preview, confirm).
- Bosses can drop Flying Raijin crafting materials (`Storm Sigil`, `Thunder Essence`, `Raijin Core`).
- Combat waves + boss encounter.
- Death tracking, score grading (`S/A/B/C/D`), and reward commands.
- Rejoin support after disconnect.

## Commands
- `/dungeon help`
- `/dungeon menu` (opens GUI)
- `/dungeon floors`
- `/dungeon start <floor>`
- `/dungeon abandon`
- `/dungeon party create`
- `/dungeon party invite <player>`
- `/dungeon party accept <leader>`
- `/dungeon party leave`
- `/dungeon party kick <player>`
- `/dungeon party list`
- `/party finder` or `/party` (opens party finder GUI)
- `/dungeon give <player> <item_id> [amount]` (admin)
- `/dungeon reload` (permission: `grivience.admin`)
- Default config currently ships with floors `F1` to `F4`.

## Configure Floors
Edit `src/main/resources/config.yml`:
- `dungeons.*` for world/origin/countdown/spacing.
- `floors.<ID>.*` for room mix, mob scaling, boss, room size, and rewards.
- Room mix keys:
  - `combat-rooms`
  - `puzzle-rooms`
  - `treasure-rooms`
  - `puzzle-types` (`SEQUENCE`, `SYNC`)
- Folklore mob keys:
  - `folklore-mobs.enabled` (`true`/`false`)
  - `folklore-mobs.pool` (e.g. `ONI_BRUTE`, `TENGU_SKIRMISHER`, `KAPPA_RAIDER`, `ONRYO_WRAITH`, `JOROGUMO_WEAVER`, `KITSUNE_TRICKSTER`, `GASHADOKURO_SENTINEL`)
- Custom item style + drop keys:
  - `custom-items.style` (`JAPANESE`, `SKYBLOCK`, `MINIMAL`)
  - `custom-items.combat.instant-bow-shots.enabled`
  - `custom-items.combat.instant-bow-shots.arrow-velocity`
  - `custom-items.combat.instant-bow-shots.cooldown-ticks`
  - `custom-items.drops.mob-weapon.base-chance`
  - `custom-items.drops.mob-weapon.yokai.<YOKAI_TYPE>`
  - `custom-items.drops.mob-armor.base-chance`
  - `custom-items.drops.mob-armor.yokai.<YOKAI_TYPE>`
  - `custom-items.drops.mob-reforge-stone.base-chance`
  - `custom-items.drops.boss-materials.storm-sigil`
  - `custom-items.drops.boss-materials.thunder-essence`
  - `custom-items.drops.boss-materials.raijin-core`
  - `custom-items.drops.boss-materials.flying-raijin`
  - `custom-items.drops.boss-materials.armor-piece`
  - `custom-items.drops.boss-materials.reforge-stone`
- Flying Raijin crafting recipe:
  - Top row: `Storm Sigil`, `Thunder Essence`, `Storm Sigil`
  - Middle row: `Raijin Core` in center
  - Bottom row: `Netherite Sword` in center
- Reward command placeholders:
  - `{player}`
  - `{floor}`
  - `{grade}`
  - `{score}`

## Reforging
- Right-click an anvil while holding a custom dungeon weapon, a reforge stone, or the `reforge_anvil` item.
- Use the custom Reforge UI to insert weapon + stone, preview the result, and confirm.
- Confirm consumes the stone and XP levels.
- Reforge stone item IDs for `/dungeon give`: `jagged_stone`, `titan_stone`, `arcane_stone`, `tempest_stone`.
- Utility item ID: `reforge_anvil`.

## Build
```bash
./gradlew build
```

Requires Java 21 and Paper API compatibility with Minecraft `1.21.x`.
