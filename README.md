# Grivience Plugin

Japanese-themed dungeon + Skyblock plugin for Paper `1.21.x` with Skyblock-style accuracy.

## Current Feature Set

### Dungeon Core
- Party-based floor runs with config-driven floor data (`F1`-`F5` by default).
- Procedural arena generation per run with connected rooms, corridors, and gate progression.
- Room types: Combat, Treasure, Puzzle Sequence (`SEQUENCE`), Puzzle Sync (`SYNC`), Puzzle Chime (`CHIME`), Puzzle Seal (`SEAL`).
- Temple Key progression and protections:
  - Room clears grant run-bound key access for the next gate.
  - Keys cannot be dropped, placed, or moved into containers.
  - Keys are removed outside active runs and on invalid pickup.
  - Key ownership is reassigned if the carrier disconnects.
- Puzzle guide book delivery and live objective/action-bar updates during puzzle flow.
- Folklore Yokai enemies plus floor-configured vanilla mob pools.
- Boss room system with archetype-based ability cycles, reinforcements, and enrage behavior (F5 adds the Shadow Daimyo duel).
- Floor 4 boss now features phased mechanics (75%/50%/25% HP thresholds: lightning slow, gravity-well honor guard, final stand burst).
- In-run death tracking, keep-inventory death handling, room-based respawn location, and live mob health nameplates.
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
- Skyblock-style weapon reforges (Gentle, Odd, Fast, Fair, Epic, Sharp, Heroic, Spicy, Legendary) with per-rarity stat tables including attack speed and intelligence.
- Reforge stones aligned to those reforges (gentle/odd/fast/fair/epic/sharp/heroic/spicy/legendary) for targeted rolls; blacksmith rolls pull from the same pool.
- `/reforge` GUI supports Vault coin or XP level costs with rarity scaling.
- Reforge stats now influence combat: attack speed boosts DPS and crit flow; intelligence scales ability damage and mana costs.
- Built-in resource pack delivery: on join, players are prompted to download the pack so custom model data renders all Grivience weapons/bows (IDs 1001–1014 by default). Drop your pack as `plugins/Grivience/resource-pack.zip` or set `resource-pack.url` to a hosted zip; hashes auto-computed when locally hosted.
- Jump pads with area triggers:
  - Admin command `/jumppad <create|target|corner1|corner2|list|remove>` to set origin, destination, and cuboid trigger area.
  - Pads are stored in `plugins/Grivience/jump-pads.yml`; players entering the defined area (or the origin block if no area) get launched/teleported to the linked target.
- Blacksmith separated from stones:
  - `/blacksmith` opens a Skyblock-style random reforge menu (Vault coins, rarity-scaled cost).
  - `/reforge` now requires a reforge stone for targeted rolls; Skyblock menu reforge button routes to the blacksmith.
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

### Bazaar Shop (Skyblock-accurate)
- **Instant Buy/Sell** - Buy from lowest sell orders or sell to highest buy orders instantly
- **Buy/Sell Orders** - Place custom price orders with automatic matching
- **Order Matching Engine** - Price-time priority matching (best price first, then oldest)
- **Partial Order Fills** - Orders fill incrementally as matching orders arrive
- **Shopping Bag System** - Items and coins from filled orders go to shopping bag for claiming
- **6-Second Cancellation Window** - Cancel orders within 6 seconds for instant full refund
- **36-Hour Order Expiry** - Orders automatically expire after 36 hours (standard)
- **50 Orders Per Player** - Maximum active orders per player
- **5% Price Spread Rule** - Orders cannot deviate more than 5% from market price
- **Minimum Order Value** - 7 coins minimum order value
- **Stack Size Tiers** - Skyblock-style tiers: 64, 160, 256, 512, 1024, 1792
- **24-Hour Price History** - Track sales, volume, average price, and trends with visual indicators
- **Market Depth Display** - See total buy/sell order volume
- **GUI System** - Skyblock-accurate layouts for main, category, product, orders, and shopping bag menus
- **Product Catalog** - 60+ items across Farming, Mining, Combat, Foraging, Fishing, and Oddities categories
- **Commands**: `/bazaar`, `/bz`, `/bazaar buy`, `/bazaar sell`, `/bazaar place`, `/bazaar cancel`, `/bazaar orders`, `/bazaar bag`, `/bazaar search`

### Zone Editor System
- **Zone Creation & Management** - Create, edit, and remove zone areas with `/zone` commands
- **Cuboid Zone Bounds** - Define zones using two corner positions (pos1, pos2)
- **Scoreboard Integration** - Display current zone name on custom scoreboard
- **Priority System** - Higher priority zones override lower ones
- **Color-Coded Zones** - Each zone can have a custom color display
- **Zone Notifications** - Players notified when entering/leaving zones
- **YAML Persistence** - Zones saved to `plugins/Grivience/zones.yml`
- **World Support** - Zones can be defined in any world
- **Selection Wand** - Use wooden axe for easy position selection
- **Commands**: `/zone create`, `/zone delete`, `/zone select`, `/zone setpos1`, `/zone setpos2`, `/zone setdisplayname`, `/zone setcolor`, `/zone setpriority`, `/zone list`, `/zone info`, `/zone wand`

### Global Event System
- **Global XP Boosts** - Start/stop server-wide XP multipliers for all skills and leveling
- **Integrated Status** - Centralized `/globalevent status` command to track all active events including Mining Events
- **Broadcast Notifications** - Automatic server-wide announcements when events start or end
- **Multiplicative Stacking** - Global boosts stack with personal player boosts
- **Documentation**: [Global Event System Guide](GLOBAL_EVENT_SYSTEM_README.md)
- **Commands**: `/globalevent startboost`, `/globalevent stopboost`, `/globalevent status`

### Grappling Hook (Skyblock-accurate)
- **Right-Click to Launch** - FishHook projectile launches in arc
- **Right-Click to Cancel** - Cancel active hook by right-clicking again
- **Sneak to Cancel** - Hook retracts when player sneaks
- **2 Second Cooldown** - Accurate cooldown timing
- **50 Block Max Range** - Accurate distance limit
- **Block Impact Detection** - Hook pulls player on block hit
- **No Entity Hooking** - Grapple only works on blocks (not entities)
- **No Durability Consumption** - Fishing rod doesn't lose durability
- **Unbreakable Item** - Grappling hook is unbreakable
- **Visual Effects** - CRIT + CLOUD particle trails, impact burst, retract particles
- **Sound Effects** - Launch, impact, pull, and retract sounds (accurate)
- **Physics** - Launch velocity 2.0, pull force 0.8, upward velocity +0.3
- **Configuration** - Fully customizable in `config.yml`

### Staff System with Protections
- **Staff Types** - 6 staff types: Arcane, Frostbite, Inferno, Stormcaller, Voidwalker, Celestial
- **Block Placement Prevention** - Staffs CANNOT be placed as blocks (HIGHEST priority protection)
- **Crafting Prevention** - Staffs cannot be used in crafting recipes
- **Right-Click Abilities** - Staff abilities trigger on right-click with mana cost
- **Cooldown System** - Configurable cooldown per staff ability
- **Intelligence Scaling** - Ability damage scales with intelligence stats
- **Custom Model Data** - Each staff has unique model data (1020-1025) for texture packs
- **Unbreakable** - All staffs are unbreakable
- **Protection Messages** - Clear messages when placement is prevented
- **Configuration** - Fully customizable cooldowns, mana costs, and damage

### Config-Driven Custom Armor Sets
- Separate config-based armor-set system under `custom-armor.*` (enabled by default).
- Default config ships with multiple sets (`crimson`, `azure`, `onyx`, `storm`, `shogun`, `stormborne`, `blossom`) and set-bonus effects.
- Armor crafting supports custom materials via `custom:<id>` ingredients (e.g., dragon_scale, blossom_fiber).
- Auto-registered recipes for configured sets/pieces.
- Runtime set-bonus listeners and effect application for configured armor sets.

### Minion System and Constellation Synergy
- Skyblock-style minion placement, action ticks, storage caps, offline catch-up, collect all, and pickup all.
- Fuel subsystem with temporary and permanent fuels (including custom fuels), live fuel slot management, and duration handling.
- Upgrade subsystem with two upgrade slots and shipping slot behavior:
  - `auto_smelter`
  - `compactor`
  - `super_compactor_3000`
  - `diamond_spreading`
  - `corrupt_soil`
  - `budget_hopper`
  - `enchanted_hopper`
  - custom upgrades like `grivience_overclock_chip`, `grivience_astral_resonator`, and `grivience_quantum_hopper`
- Hopper-based overflow selling with stored hopper coins and in-GUI coin collection.
- Crafting recipes for each minion tier and utility items (fuel/upgrade items).
- Custom unique mechanic: **Constellation Synergy**.
  - Island tier is based on distinct placed minion types (or admin override).
  - Grants island-wide minion speed bonus and bonus `constellation_fragment` drops at higher tiers.
  - `constellation_fragment` is used to craft `grivience_astral_resonator` for extra minion speed and fragment chance.
  - Full admin control via `/minion constellation ...` commands.
- Documentation: [Minion System Guide](MINION_SYSTEM_README.md)

### Skyblock Core
- Dedicated void-style Skyblock world generation (`skyblock.world-name`).
- Per-player island creation and persistence (`plugins/Grivience/skyblock/islands/*.yml`).
- **Island Persistence Safeguards** - Islands NEVER reset on plugin updates/reloads:
  - Auto-save on server shutdown
  - Preservation during `/grivience reload`
  - Backup creation before updates
  - Console logging for all save operations
- Configurable island sizes, upgrade tiers, and costs (Vault-aware if available).
- Island visit support (`/island go <player>`) with visit history/counter tracking.
- Coop islands with member lists; visitors cannot place/break, open island containers, use redstone/doors, interact with entities, or attack players/mobs. Visitors can still use Skyblock Menu + personal sub-menus (Storage, Pets, Wardrobe, etc.) and trade.
- Island metadata management:
  - Name (`/island setname`)
  - Description (`/island setdesc`)
  - Home/spawn (`/island sethome`)
- Command teleport guard to prevent command-teleporting outside your own island area in Skyblock world.
- Island respawn routing to your island spawn location.
- Hub teleport (`/hub`, `/spawn`, `/lobby`) using configured hub world/spawn.
- Nether portal reroute to hub in configured Skyblock flow.
- Optional navigation item (Compass in slot 9) that opens the Skyblock menu.

### Skyblock GUI Status
Implemented:
- Main/Island/Upgrades/Minions/Settings/Permissions menu navigation.
- Skyblock Leveling menu (Skyblock-style guide/milestones) from the main Skyblock menu and `/skyblock leveling`.
- Skyblock XP tracking with persistent progression in `plugins/Grivience/levels.yml`.
- Leveling progression sources include combat, farming, mining, foraging, fishing, quest completion, dungeon completion, and island progression milestones.
- Island expansion action through GUI (real upgrade call).
- Wardrobe tile opens wardrobe GUI; instant access via `/wardrobe`.
- Collection/Recipes tiles open AuroraCollections when installed.
- Fast travel tile dispatches `/hub`.
- Bank system: `/bank` GUI to deposit/withdraw between purse and bank (profile-scoped / instanced balance).
- Visitor protections: visitors cannot place/break, use buckets, open island containers, use redstone/doors, interact with entities, or attack players/mobs on other islands. Visitors can still use Skyblock Menu + sub-menus (Storage/Pets/Wardrobe/Recipes/Profiles/etc.), the virtual crafting table, and /trade.
- Admin island warp: `/skyblockadmin goto <player>`.
- Coop management via `/island coop <add|remove|list>`.
- Farming Contest schedule tile in Skyblock menu shows the next rotating contest with three crops every 20 minutes (Skyblock-style).
- Profile tile opens profile manager commands to list/create/switch/delete profiles.
- **Close Buttons** - All GUI menus have close buttons (BARRIER in bottom-right corner)

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

### Trading
- Hypixel-style `/trade <player>` request flow with proximity (within 9 blocks, same world) and request timeout (60s).
- `/trade accept [player]`, `/trade decline [player]`, `/trade cancel` (alias: `/tr`).
- Crouch + right-click a player to send a trade request (same rules as `/trade <player>`).
- Trade both items and purse coins (coin offer slots in the trade GUI).
- Coin offers use the currently selected Skyblock profile purse (instanced balance per profile); profile switching is blocked while trading.
- Two-stage trade GUI (Offer -> Accept -> Confirm); items only exchange on mutual confirm.

### Bank
- Skyblock-style bank account stored on your currently selected Skyblock profile (instanced balance per profile).
- `/bank` opens the bank GUI.
- Deposit/withdraw quick amounts, deposit/withdraw all, or enter a custom amount in chat.

### Pets
- `/pets` GUI to equip companions with potion effects and attribute perks.
- Definitions live in `plugins/Grivience/pets.yml` (copied on first run).
- Each pet supports `name`, `icon`, `lore`, `effects.<potion>` (amplifier/duration ticks), `attributes.<AttributeName>`, and optional `crop-multiplier`.
- Optional `head-texture` (texture URL from textures.minecraft.net) to render the pet as a player head matching the creature.
- Equipped pet choice persists in `pet-data.yml`; barrier slot unequips.
- Attribute keys accept full enum names (e.g., `GENERIC_MAX_HEALTH`) or shorthand without the `GENERIC_` prefix.
- **Close Button** - Pet GUI has close button for easy exit

### Conversation Quests (ZNPCS)
- Persistent quest definitions in `plugins/Grivience/quests.yml`.
- Persistent per-player progress in `plugins/Grivience/quest-progress.yml`.
- Supports starter NPC and target NPC IDs for conversation flow.
- Works with ZNPCsPlus interaction events automatically when installed.
- Works with ZNPCS command actions using `/quest talk <npc_id>`.
- Player quest board GUI via `/quest` or `/quest menu`.
- Admin quest editor GUI via `/quest gui` (with `grivience.admin`).
- **Close Buttons** - Quest GUIs have close buttons

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

### Zone Editor (Admin)
- `/zone help` - Show help information
- `/zone create <id> [name] [displayName]` - Create a new zone
- `/zone delete <id>` - Delete a zone
- `/zone select <id>` - Select a zone for editing
- `/zone setpos1` - Set position 1 (current location)
- `/zone setpos2` - Set position 2 (current location)
- `/zone setname <name>` - Set zone internal name
- `/zone setdisplayname <displayName>` - Set display name
- `/zone setcolor <color>` - Set display color
- `/zone setpriority <number>` - Set priority
- `/zone setenabled <true|false>` - Enable/disable zone
- `/zone setdesc <description>` - Set description
- `/zone info [id]` - View zone info
- `/zone list` - List all zones
- `/zone reload` - Reload zones from config
- `/zone wand` - Get zone selection wand

### Grappling Hook
- Automatically works when holding a grappling hook item
- Right-click to launch
- Right-click again or sneak to cancel
- Cooldown displayed on use attempt

### Staff System
- Automatically works when holding a staff item
- Right-click to use ability
- Cannot be placed as blocks (protection enforced)
- Cooldown displayed on use attempt

### Quest
- `/quest` or `/quest menu` (open player quest board)
- `/quest list`
- `/quest progress`
- `/quest start <quest_id>`
- `/quest cancel <quest_id>`
- `/quest talk <znpcs_npc_id>` (for ZNPCS action commands)
- Admin:
  - `/quest gui` (open quest editor)
  - `/quest reload`
  - `/quest create <id> [displayName]`
  - `/quest delete <id>`
  - `/quest setname <id> <name...>`
  - `/quest setdescription <id> <description...>`
  - `/quest setstarter <id> <npc_id|none>`
  - `/quest settarget <id> <npc_id>`
  - `/quest setrepeatable <id> <true|false>`
  - `/quest setenabled <id> <true|false>`
  - `/quest addreward <id> <command...>`
  - `/quest removereward <id> <index>`
  - `/quest clearrewards <id>`
  - `/quest znpcshint <id>`

### Skyblock
- `/skyblock` (opens main menu)
- `/skyblock <menu|main|island|upgrades|minions|settings|leveling|permissions|help>`
- `/skyblock leveling` (open Skyblock XP / Guide menu)
- Aliases: `/sb`, `/skyblockmenu`
- `/minion` (open minion management GUI)
- `/minion <list|collectall|pickupall|help>`
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
- `/pets` – open pets GUI
- `/bank` (alias `/bnk`) – open bank GUI (deposit/withdraw purse <-> bank)
- Admin:
  - `/skyblockadmin <sethub|reload|goto|help>` (goto teleports to a player's island)
  - `/minion give <type> [tier] [player]`
  - `/minion givefuel <id> [amount] [player]`
  - `/minion giveupgrade <id> [amount] [player]`
  - `/minion givefragment [amount] [player]`
  - `/minion constellation status [player]`
  - `/minion constellation set <tier:0-3> [player]`
  - `/minion constellation clear [player]`

### Trading
- `/trade <player>` (alias `/tr`) - send a trade request
- `/trade accept [player]` - accept a trade request
- `/trade decline [player]` - decline a trade request
- `/trade cancel` - cancel an outgoing request or an active trade

### Bank
- `/bank` (alias `/bnk`) - open the bank GUI
- `/bank balance` - view purse and bank
- `/bank deposit <amount|all>` - deposit coins from purse to bank
- `/bank withdraw <amount|all>` - withdraw coins from bank to purse

### Monster Spawn Admin
- `/mobspawn create <monster_id>`
- `/mobspawn remove <spawn_point_id>`
- `/mobspawn list`
- `/mobspawn info <spawn_point_id>`
- `/mobspawn toggle <spawn_point_id>`
- `/mobspawn monsters`
- `/mobspawn help`

### Plugin Management
- `/grivience reload` - Reload plugin configuration (islands preserved)

## Permissions

- `grivience.admin`
  - Dungeon reload/give
  - Quest editor/admin commands
  - Skyblock admin commands
  - Monster spawn admin commands
  - Zone editor commands (`/zone ...`)

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
- `bazaar` (Skyblock-accurate settings)
- `zones` (zone editor configuration)
- `grappling-hook` (Skyblock-accurate settings)
- `staffs` (staff system with protections)
- `pets.yml` (separate file in the plugin data folder for custom pet definitions)
- `quests.yml` (quest definitions)
- `zones.yml` (zone definitions, auto-generated)

Example custom pet (`plugins/Grivience/pets.yml`):

```yaml
pets:
  rabbit:
    name: "&aRabbit"
    icon: PLAYER_HEAD
    head-texture: "http://textures.minecraft.net/texture/e087f28f68a799beb7f94f4d0723c1c5d19c73cd3a63675b8622c2c58eea8b2"
    lore:
      - "&7Haste I"
    effects:
      haste:
        amplifier: 0        # level 1
        duration: 12000     # ticks (10 minutes)
    attributes:
      ARMOR: 1.0
    crop-multiplier: 1.10   # value stored for crop bonuses

  farmer:
    name: "&6Farmer Buddy"
    icon: PLAYER_HEAD
    head-texture: "http://textures.minecraft.net/texture/2279da49c581694132ff7a73ae8623fa3dd8b1cb04f6bafc1f2de0bc1c8c0bd5"
    lore:
      - "&7Crops drop more"
      - "&7Gives extra health"
    effects:
      saturation:
        amplifier: 0
        duration: 12000
      haste:
        amplifier: 1
        duration: 12000
    attributes:
      MAX_HEALTH: 4.0
    crop-multiplier: 1.25
```

## Optional Integrations

- `Vault` for island-upgrade economy handling, reforge coin costs, and bazaar transactions.
- `AuraSkills` for mana-backed abilities and stat scaling in custom combat.
- `EcoEnchants` for arcane eco-enchant options.

## Data Persistence

### Islands
- Stored in: `plugins/Grivience/skyblock/islands/<uuid>.yml`
- **NEVER reset** on plugin updates or reloads
- Auto-saved on server shutdown
- Preserved during `/grivience reload`

### Zones
- Stored in: `plugins/Grivience/zones.yml`
- Auto-saved on creation/modification
- Reloadable via `/zone reload`

### Bazaar
- Orders stored in: `plugins/Grivience/bazaar_orders.yml`
- Shopping bags stored in: `plugins/Grivience/bazaar_bags.yml`
- Price history stored in: `plugins/Grivience/bazaar_history.yml`
- Auto-saved on all transactions

### Other Data
- Levels: `plugins/Grivience/levels.yml`
- Pets: `plugins/Grivience/pet-data.yml`
- Quests: `plugins/Grivience/quest-progress.yml`
- Jump Pads: `plugins/Grivience/jump-pads.yml`
- Minions: `plugins/Grivience/skyblock/minions.yml` (placed minions + constellation overrides)

## Build

```bash
./gradlew build
```

Windows:

```powershell
.\gradlew.bat build
```

Requires Java 21 and Paper API compatibility with Minecraft `1.21.x`.

## Server Branding

This plugin is configured for **Skyblock** server branding:
- Scoreboard title: `&e&lSkyblock`
- Scoreboard footer: `&eSkyblock`
- Navigation item: "Skyblock Menu"
- All GUI titles use "Skyblock" branding

To change branding, edit `config.yml`:
```yaml
scoreboard:
  custom:
    title: "&e&lYour Server"
    footer: "&ewww.yourserver.com"
```

