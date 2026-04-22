# Configuration Reference

Last updated: 2026-03-13

## Overview

This page is the canonical draft for all configuration keys used by Grivience.
Paths are based on `src/main/resources/config.yml` and auxiliary resource YAML files.

Primary sources:

- `src/main/resources/config.yml`
- `src/main/resources/collections.yml`
- `src/main/resources/fasttravel.yml`
- `src/main/resources/pets.yml`
- `src/main/resources/quests.yml`
- `src/main/resources/storage_upgrades.yml`

## Main `config.yml` Keys

| Top-level key | Purpose | Notable subkeys |
| --- | --- | --- |
| `dungeons` | Dungeon world, party, instance, and reward chest behavior | `world`, `exit-world`, `origin.*`, `arena-spacing`, `countdown-seconds`, `max-party-size`, `invite-timeout-seconds`, `mob-damage-multiplier`, `reward-chest.*`, `hub.*` |
| `navigation-item` | Enables/disables hub navigation item behavior | `enabled` |
| `security` | Security controls | `plugin-hider.*` |
| `crafting` | Crafting table override behavior | `override-crafting-table` |
| `skyblock` | Island/hub/farm hub settings and farm regeneration systems | `world-name`, `hub-world`, `minehub-world`, `farmhub-world`, `upgrade-*`, `hub-spawn.*`, `minehub-spawn.*`, `farmhub-spawn.*`, `farmhub-crop-area.*`, `farmhub-crop-regen.*`, `farmhub-crop-growth.*`, `farmhub-farmland.*`, `farmhub-maintenance.*`, `nether-portal-to-hub.*` |
| `end-mines` | End Mines access, generation, mining regen, mob systems, and protection | `enabled`, `required-level`, `access.*`, `spawn.*`, `generation.*`, `mining.regen.*`, `mobs.*`, `protection.*` |
| `scoreboard` | Sidebar and profile scoreboard rendering | `custom.*` |
| `skyblock-leveling` | Skyblock XP, skills, tracks, dungeon/bestiary/slayer progression | `max-level`, `xp-per-level`, `notify-xp-gain`, `skill-leveling.*`, `skill-level-xp-rewards.*`, `dungeons.*`, `bestiary.*`, `slayer.*`, `action-xp.*`, `skill-actions-per-level.*`, `level-rewards.*`, `tracks.*` |
| `enchant-table` | Enchant table/anvil option costs and eco command integration | `enabled`, `vanilla.*`, `cost-multipliers.*`, `eco.*`, `options.*` |
| `custom-items` | Reforge, combat scaling, mana abilities, and drop tuning | `style`, `reforge.*`, `combat.*`, `drops.*` |
| `bazaar` | Bazaar engine behavior, limits, pricing, GUI, and persistence mode | `enabled`, `max-orders-per-player`, `order-expiry-hours`, `stack-size-tiers`, `default-prices.*`, `material-price-overrides.*`, `custom-price-overrides.*`, `npc-shop.*`, `gui.*`, `notifications.*`, `database-type`, `mysql.*`, `advanced.*` |
| `zones` | Zone editor and scoreboard area priority | `enabled`, `default-zone-name`, `show-*`, `scoreboard-update-interval`, `priority-order` |
| `grappling-hook` | Grappling hook mechanics and item settings | `enabled`, `launch-velocity`, `max-distance`, `cooldown-ms`, `pull-force`, `cancel-*`, `particle-effects`, `sound-effects`, `item.*` |
| `staffs` | Staff ability balancing and ability definitions | `enabled`, `global-cooldown-ms`, `require-mana`, `show-cooldown-messages`, `intelligence-scaling`, `types.*` |
| `skyblock-profiles` | Profile-slot behavior and autosave | `enabled`, `max-profiles`, `allow-deletion`, `auto-save-interval-seconds` |
| `skyblock-guide` | First-join onboarding behavior | `first-join-auto-open`, `first-join-open-delay-ticks`, `first-join-chat-tip` |
| `mining-events` | Mining event NPC/event shared settings | `kings-inspection.*`, `settings.*` |
| `welcome-event` | Starter rewards and XP boost event behavior | `enabled`, `starter-money`, `mining-boost-percent`, `farming-boost-percent`, `boost-duration-minutes`, `spawn-*`, `give-starter-*` |
| `wizard-tower` | Wizard tower tiered permanent blessings | `enabled`, `znpc-id`, `interaction-prompt-cooldown-seconds`, `tiers.*` |
| `custom-monsters` | Custom mob stat blocks and drops | `enabled`, `damage-multiplier`, `monsters.*` |
| `custom-armor` | Custom armor sets and set bonus definitions | `enabled`, `sets.*` |
| `floors` | Dungeon floor definitions (`F1` to `F5`) | `display-name`, `min/max-party-size`, `combat/puzzle/treasure-rooms`, `puzzle-types`, `base-mob-count`, `mob-health-multiplier`, `mob-damage-tier`, `folklore-mobs.*`, `mob-pool`, `boss-*`, `rewards.*` |
| `resource-pack` | Resource-pack distribution and model map | `enabled`, `required`, `url`, `hash`, `prompt`, `local.*`, `models.*`, `name-map.*` |
| `jump-pads` | Jump pad movement and visuals | `horizontal-velocity`, `vertical-boost`, `arrival-teleport-delay-ticks`, `cooldown-ms`, `fall-cancel-window-ms`, `min-vertical-speed`, `max-vertical-speed`, `max-horizontal-speed`, plus effect toggles under `jump-pads.*` |
| `storage` | Storage system controls and permission bundles | `enabled`, `auto-save.*`, `allow-locking`, `allow-naming`, `usage-warning-threshold`, `enable-leaderboard`, `enable-upgrade-notifications`, `permissions.*` |

## Security Key (Plugin Hider)

`security.plugin-hider` controls command-level plugin hiding:

- `enabled`
- `bypass-permission`
- `block-message`
- `blocked-commands` (example: `pl`, `plugins`, `ver`, namespace variants)

## Auxiliary Config Files

### `collections.yml`

- Root: `collections`
- Contains per-collection definitions:
  - `name`, `description`, `icon`, `category`, `enabled`, `items`, `tiers.*`

### `fasttravel.yml`

- Root key currently used: `unlocks`
- Hub warps are synced from main config and can be extended with custom destinations.

### `pets.yml`

- Root: `pets`
- Per pet:
  - `name`, `icon`, `head-texture`, `rarity`, `skill-type`, `max-level`
  - `stats.*`
  - `abilities.*`

### `quests.yml`

- Root: `quests`
- Per quest:
  - `display-name`, `description`
  - `starter-npc`, `target-npc`
  - `rewards`
  - `repeatable`, `enabled`

### `storage_upgrades.yml`

- Roots:
  - `upgrades` (per storage type and tier)
  - `settings`
- Tier values:
  - `slots`, `cost`, `name`, `commands`

## Reload Behavior

- Safe hot-reload path: `/grivience reload`.
- For high-risk edits (major floor rewrites, world name changes), restart recommended after validation.
- Validate after config edits with:
  - `/dungeon floors`
  - `/farmhub info`
  - `/endmine status`
  - `/mobspawn list`

## Related Pages

- `Commands and Permissions`
- `Data Files and Persistence`
- `Dungeons and Parties`
