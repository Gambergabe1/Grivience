# Grivience Plugin

Japanese-themed dungeon + SkyBlock plugin for Paper `1.21.x`.

## Current Feature Set

### Dungeon Core
- Party-based floor runs with config-driven floor data (`F1`-`F4` by default).
- Procedural arena generation per run with connected rooms, corridors, and gate progression.
- Room types: Combat, Treasure, Puzzle Sequence (`SEQUENCE`), Puzzle Sync (`SYNC`), Puzzle Chime (`CHIME`), Puzzle Seal (`SEAL`).
- Temple Key progression and protections:
  - Room clears grant run-bound key access for the next gate.
  - Keys cannot be dropped, placed, or moved into containers.
  - Keys are removed outside active runs and on invalid pickup.
  - Key ownership is reassigned if the carrier disconnects.
- Puzzle guide book delivery and live objective/action-bar updates during puzzle flow.
- Folklore Yokai enemies plus floor-configured vanilla mob pools.
- Boss room system with archetype-based ability cycles, reinforcements, and enrage behavior.
- In-run death tracking, keep-inventory death handling, and room-based respawn location.
- Run scoring and grades (`S/A/B/C/D`) with reward-command placeholders (`{player}`, `{floor}`, `{grade}`, `{score}`).
- Rejoin support after disconnect.
- Friendly fire prevention for party/session teammates.
- In-run world protections (block place/break lock; creeper block damage blocked).
- Post-run arena cleanup.

### Party and Matchmaking
- `/party` and `/dungeon party` flows:
  - create / invite / accept / leave / kick / list
- Invite expiry window from config.
- Party Finder GUI with active party snapshots.
- "Request invite" flow from finder (leader gets prompt to `/party invite <name>`, with request cooldown).
- GUI-assisted player invites from online eligible player list.
- Party edits lock while in an active dungeon run.
- Party warp to leader island (`/island warp`).

### Dungeon GUI
- Dungeon Nexus main menu (`/dungeon menu`, `/dungeon gui`).
- Floor browser with floor cards (party-size range, room breakdown, boss info).
- Start-floor action directly from GUI floor cards.
- Party Finder and Invite Players menus with live eligibility checks.
- Adventurer profile card showing party/dungeon state.

### Custom Dungeon Items and Combat
- 16 custom dungeon weapons (added Hayabusa Katana, Raijin Shortbow).
- Config-driven shortbows fire instantly on left and right click.
- 12+ custom dungeon armor pieces across `Shogun`, `Shinobi`, `Onmyoji`, plus config armor sets.
- Passive custom-armor combat buffs and full-set effects for dungeon armor sets.
- Flying Raijin crafting recipe using boss materials:
  - `storm_sigil`
  - `thunder_essence`
  - `raijin_core`
- Config-driven dungeon drop rates for weapon, armor, reforge stone, and boss materials.
- Reforge system with 4 stones (`jagged`, `titan`, `arcane`, `tempest`) and custom stat bonuses.
- Hypixel-style reforge flow:
  - `/reforge` command (plus anvil-context opening).
  - Random blacksmith reforge pool when no stone is inserted.
  - Targeted stone-exclusive reforges when a stone is inserted.
  - Vault coin cost support (fallback to level cost if no economy provider).
  - Fixed per-rarity stat tables per reforge (Hypixel-like rarity breakpoints).
  - Reforge cost scaling by weapon rarity tier and name-prefix application.
- Configurable custom combat scaling:
  - Flat damage + strength scaling
  - Crit chance / crit damage scaling
  - Mana abilities (optional AuraSkills requirement)
  - Instant right-click shortbow firing for supported custom bows
- Arcane Enchanting GUI on enchanting table:
  - Auto-generated vanilla enchant options based on held weapon
  - Configured EcoEnchants options via command template
  - Config-driven cost scaling multipliers and paginated option view
- Anvil support for custom weapons with enchant lore sync.

### Custom Monsters (Overworld/Admin Spawn Points)
- Config-defined custom monster templates (`custom-monsters.monsters.*`) with stats and rewards.
- Supports standard material drops and optional custom-item drops per monster template.
- Persistent spawn points (`mob-spawns.yml`) with active/inactive toggles.
- Automatic spawn cycling with per-point nearby-entity caps.
- Admin spawn-point management command set (`/mobspawn ...`).

### Bazaar Shop
- `/bazaar` GUI with two catalogs:
  - Plugin items (custom weapons/armor/materials/reforge stones + config armor-set pieces)
  - Base server items (all vanilla materials where `Material#isItem` is true)
- Hypixel-style instant buy/sell + orders:
  - Instant trades use live best buy/sell orders when Vault is enabled; falls back to NPC pricing.
  - Middle-click = create buy order, Drop = create sell order (stack size).
- Economy handling:
  - Uses Vault economy when available (`bazaar.use-vault-economy`)
  - Optional XP-level fallback for instant trades when Vault is unavailable (`bazaar.allow-level-fallback`)
- Configurable default prices and per-item override maps under `bazaar.*` in `config.yml`.

### Config-Driven Custom Armor Sets
- Separate config-based armor-set system under `custom-armor.*` (enabled by default).
- Default config ships with multiple sets (`crimson`, `azure`, `onyx`, `storm`, `shogun`, `stormborne`, `blossom`) and set-bonus effects.
- Armor crafting supports custom materials via `custom:<id>` ingredients (e.g., dragon_scale, blossom_fiber).
- Auto-registered recipes for configured sets/pieces.
- Runtime set-bonus listeners and effect application for configured armor sets.

### SkyBlock Core
- Dedicated void-style SkyBlock world generation (`skyblock.world-name`).
- Per-player island creation and persistence (`plugins/Grivience/skyblock/islands/*.yml`).
- Configurable island sizes, upgrade tiers, and costs (Vault-aware if available).
- Island visit support (`/island go <player>`) with visit history/counter tracking.
- Coop islands with member lists; visitors blocked from breaking/containers/redstone, members allowed.
- Island metadata management:
  - Name (`/island setname`)
  - Description (`/island setdesc`)
  - Home/spawn (`/island sethome`)
- Command teleport guard to prevent command-teleporting outside your own island area in SkyBlock world.
- Island respawn routing to your island spawn location.
- Hub teleport (`/hub`, `/spawn`, `/lobby`) using configured hub world/spawn.
- Nether portal reroute to hub in configured SkyBlock flow.
- Optional navigation item (Compass in slot 9) that opens the SkyBlock menu.

### SkyBlock GUI Status
Implemented:
- Main/Island/Upgrades/Minions/Settings/Permissions menu navigation.
- Island expansion action through GUI (real upgrade call).
- Wardrobe tile opens wardrobe GUI; instant access via `/wardrobe`.
- Collection/Recipes tiles open AuroraCollections when installed.
- Fast travel tile dispatches `/hub`.
- Visitor protections: visitors cannot break blocks or interact with containers/redstone on islands.
- Admin island warp: `/skyblockadmin goto <player>`.
- Coop management via `/island coop <add|remove|list>`.

Placeholder / coming soon:
- Menu enchanting shortcut
- Biome change from GUI
- Set island spawn from GUI
- Invite/kick member from GUI
- Rename/description edits from GUI
- Banned-player actions
- Detailed rank-configuration actions
- `/island kick` command logic
- `/island leave` command logic

## Commands

### Dungeon and Party
- `/dungeon help`
- `/dungeon floors`
- `/dungeon start <floor>`
- `/dungeon abandon`
- `/dungeon menu` or `/dungeon gui`
- `/dungeon party <create|invite|accept|leave|kick|list|finder|gui>`
- `/party` (opens party finder)
- `/party <create|invite|accept|leave|kick|list|finder|gui>`
- `/reforge`
- `/bazaar [menu|custom|vanilla|help]`
- Aliases:
  - `/dg` for `/dungeon`
  - `/p` for `/party`
  - `/rf` for `/reforge`
  - `/bz` for `/bazaar`
- Admin:
  - `/dungeon reload`
  - `/dungeon give <player> <item_id> [amount]`

### SkyBlock
- `/skyblock` (opens main menu)
- `/skyblock <menu|main|island|upgrades|minions|settings|permissions|help>`
- Aliases: `/sb`, `/skyblockmenu`
- `/island <go|home|visit> [player]`
- `/island create`
- `/island <expand|upgrade> [level]`
- `/island info`
- `/island sethome`
- `/island setname <name>`
- `/island setdesc <description>`
- `/island warp` (party leader warp to leader island)
- `/island coop <add|remove|list> <player>` (coop members share protections)
- `/island kick` (placeholder)
- `/island leave` (placeholder)
- `/island help`
- Alias: `/is`
- `/hub`
- `/spawn`
- `/lobby`
- `/visit <player>` (alias `/v`, `/vi`) – visit any island
- `/wardrobe` (alias `/wd`) – open wardrobe GUI
- Admin:
  - `/skyblockadmin <sethub|reload|goto|help>` (goto teleports to a player's island)

### Monster Spawn Admin
- `/mobspawn create <monster_id>`
- `/mobspawn remove <spawn_point_id>`
- `/mobspawn list`
- `/mobspawn info <spawn_point_id>`
- `/mobspawn toggle <spawn_point_id>`
- `/mobspawn monsters`
- `/mobspawn help`

## Permissions

- `grivience.admin`
  - Dungeon reload/give
  - SkyBlock admin commands
  - Monster spawn admin commands

## Configuration Overview

Edit `plugins/Grivience/config.yml` on your server.
Template source in this repo: `src/main/resources/config.yml`.

Main sections:
- `dungeons`
- `floors`
- `navigation-item`
- `skyblock`
- `enchant-table`
- `custom-items`
- `custom-monsters`
- `custom-armor`

## Optional Integrations

- `Vault` for island-upgrade economy handling and reforge coin costs.
- `AuraSkills` for mana-backed abilities and stat scaling in custom combat.
- `EcoEnchants` for arcane eco-enchant options.

## Build

```bash
./gradlew build
```

Windows:

```powershell
.\gradlew.bat build
```

Requires Java 21 and Paper API compatibility with Minecraft `1.21.x`.
