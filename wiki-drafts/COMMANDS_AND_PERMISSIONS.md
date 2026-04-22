Make sure the Regenerable Areas, Have a place Holder Block While They Wait to Respawn, Such as Bedrock.# Commands and Permissions

Last updated: 2026-03-13

## Overview

Canonical sources:

- `src/main/resources/plugin.yml` (`commands`, `permissions`)
- Command executors in `src/main/java/io/papermc/Grivience/**`

Error code legend used in tables:

- `E1`: Unknown subcommand or invalid option
- `E2`: Missing or invalid arguments
- `E3`: Missing permission
- `E4`: In-game only command used from console
- `E5`: Target player not found/offline
- `E6`: State restriction (party/run/profile/region/setup not valid yet)

## Command Reference

### Core Progression and Menus

| Command | Syntax | Aliases | Permission | Player example | Admin example | Errors |
| --- | --- | --- | --- | --- | --- | --- |
| `skills` | `/skills [skill_id]` | `skill` | none in `plugin.yml` | `/skills` | `/skills farming` | `E2` |
| `skyblock` | `/skyblock [menu|island|upgrades|minions|settings|leveling|permissions|help]` | `sb`, `skyblockmenu` | none in `plugin.yml` | `/skyblock menu` | `/skyblock help` | `E1`, `E2`, `E6` |
| `profile` | `/profile <create|list|switch|delete>` | `profiles` | none in `plugin.yml` | `/profile list` | `/profile create Ironman` | `E1`, `E2`, `E6` |
| `collection` | `/collection [search|top|admin]` | `collections` | none in `plugin.yml` | `/collection` | `/collection admin` | `E1`, `E2` |
| `quest` | `/quest <menu|list|progress|start|cancel|talk|gui|help>` | `-` | none in `plugin.yml` | `/quest list` | `/quest progress` | `E1`, `E2`, `E6` |
| `questline` | `/questline [progress|reset|help]` | `ql`, `quests`, `welcomequests` | none in `plugin.yml` | `/questline progress` | `/questline reset` | `E1`, `E2`, `E6` |
| `welcome` | `/welcome [claim|status|help]` | `starter`, `newplayer` | none in `plugin.yml` | `/welcome claim` | `/welcome status` | `E1`, `E2`, `E6` |
| `enchant` | `/enchant [page]` | `et`, `enchantmenttable` | none in `plugin.yml` | `/enchant` | `/enchant 2` | `E2`, `E4`, `E6` |
| `anvil` | `/anvil` | `av` | none in `plugin.yml` | `/anvil` | `/anvil` | `E4`, `E6` |
| `enchantinfo` | `/enchantinfo <enchantment_id>` | `ei` | none in `plugin.yml` | `/enchantinfo sharpness` | `/enchantinfo lifesteal` | `E2`, `E6` |
| `enchantlist` | `/enchantlist [page]` | `el` | none in `plugin.yml` | `/enchantlist` | `/enchantlist 3` | `E2` |
| `craft` | `/craft [recipe_name]` | `crafting`, `recipe` | none in `plugin.yml` | `/craft` | `/craft oni_cleaver` | `E2`, `E6` |
| `reforge` | `/reforge` | `rf` | none in `plugin.yml` | `/reforge` | `/reforge` | `E6` |
| `blacksmith` | `/blacksmith` | `-` | none in `plugin.yml` | `/blacksmith` | `/blacksmith` | `E6` |
| `pets` | `/pets` | `pet` | none in `plugin.yml` | `/pets` | `/pets` | `E6` |
| `wardrobe` | `/wardrobe` | `wd` | none in `plugin.yml` | `/wardrobe` | `/wardrobe` | `E6` |
| `storage` | `/storage [open|upgrade|status|rename|lock|unlock|top|rank|help]` | `storages` | none in `plugin.yml` (type access uses `storage.*`, admin clear uses `grivience.storage.admin`) | `/storage open` | `/storage rank` | `E1`, `E2`, `E3`, `E6` |
| `accessory` | `/accessory [open|upgrade|stats|help]` | `acc`, `accessories` | `storage.accessory` | `/accessory open` | `/accessory stats` | `E1`, `E2`, `E3`, `E6` |
| `compactor` | `/compactor [open|stats|clear|compact|help]` | `pc`, `personalcompactor` | `grivience.personalcompactor` | `/compactor open` | `/compactor stats` | `E1`, `E2`, `E3`, `E6` |
| `nick` | `/nick [reset]` | `-` | none in `plugin.yml` | `/nick` | `/nick reset` | `E2`, `E6` |
| `unnick` | `/unnick` | `-` | none in `plugin.yml` | `/unnick` | `/unnick` | `E6` |
| `wizardtower` | `/wizardtower [gui|buy <tier>|status|list|setznpc <npcId>|reload]` | `wt`, `wizard` | none in `plugin.yml` (`setznpc`/`reload` require `grivience.wizardtower.admin`) | `/wizardtower gui` | `/wizardtower setznpc 12` | `E1`, `E2`, `E3`, `E6` |

### Dungeons, Combat, and Encounters

| Command | Syntax | Aliases | Permission | Player example | Admin example | Errors |
| --- | --- | --- | --- | --- | --- | --- |
| `dungeon` | `/dungeon <help|floors|menu|start|abandon|party|reload|give>` | `dg` | none in `plugin.yml` (`reload`/`give` enforce `grivience.admin` in code) | `/dungeon start F2` | `/dungeon reload` | `E1`, `E2`, `E3`, `E4`, `E5`, `E6` |
| `party` | `/party <create|invite|accept|leave|kick|list|finder>` | `p` | none in `plugin.yml` | `/party invite Kazutos` | `/party list` | `E1`, `E2`, `E5`, `E6` |
| `dungeonhub` | `/dungeonhub [set <world> <x> <y> <z> [yaw] [pitch]]` | `dhub` | none in `plugin.yml` (`set` enforces `grivience.admin` in code) | `/dungeonhub` | `/dungeonhub set world 0 100 0` | `E2`, `E3`, `E4`, `E6` |
| `giveitem` | `/giveitem <item_id> [amount] [player]` | `gi` | `grivience.admin` | n/a | `/giveitem flying_raijin 1 Kazutos` | `E2`, `E3`, `E5`, `E6` |
| `mobspawn` | `/mobspawn <create|set|remove|list|info|toggle|spawn|monsters|help>` | `-` | `grivience.admin` | n/a | `/mobspawn spawn zombie 5` | `E1`, `E2`, `E3`, `E6` |
| `grisanity` | `/grisanity [all|recipes|mobs]` | `-` | `grivience.admin` | n/a | `/grisanity all` | `E1`, `E2`, `E3` |
| `mineevent` | `/mineevent <start|stop|status|shop|mechanic|npcshop>` | `-` | `grivience.admin.mineevent` | n/a | `/mineevent start` | `E1`, `E2`, `E3`, `E6` |
| `globalevent` | `/globalevent <startboost|stopboost|status>` | `gevent`, `global` | `grivience.admin.globalevent` | n/a | `/globalevent status` | `E1`, `E2`, `E3` |

### Travel and World

| Command | Syntax | Aliases | Permission | Player example | Admin example | Errors |
| --- | --- | --- | --- | --- | --- | --- |
| `hub` | `/hub [set]` | `spawn`, `lobby` | none in `plugin.yml` (`set` requires `grivience.admin` in code) | `/hub` | `/hub set` | `E2`, `E3`, `E4`, `E6` |
| `minehub` | `/minehub [set]` | `mh` | none in `plugin.yml` (`set` requires `grivience.admin` in code) | `/minehub` | `/minehub set` | `E2`, `E3`, `E4`, `E6` |
| `endmine` | `/endmine [help|status|set|generate|access|mobs|mineables]` | `endmines`, `em` | none in `plugin.yml` (`set/generate/access/mobs/mineables` are operator-only in code) | `/endmine` | `/endmine status` | `E1`, `E2`, `E3`, `E4`, `E6` |
| `farmhub` | `/farmhub [set|setpos1|setpos2|setarea|info]` | `fh` | none in `plugin.yml` (`set|setpos1|setpos2|setarea` require `grivience.admin` in code) | `/farmhub` | `/farmhub setarea` | `E1`, `E2`, `E3`, `E4`, `E6` |
| `fasttravel` | `/fasttravel [destination]` | `ft` | none in `plugin.yml` | `/fasttravel` | `/fasttravel dungeonhub` | `E2`, `E6` |
| `skyblockadmin` | `/skyblockadmin <sethub|reload|goto|help>` | `-` | `grivience.admin` | n/a | `/skyblockadmin reload` | `E1`, `E2`, `E3`, `E5`, `E6` |

### Islands, Social, and Trading

| Command | Syntax | Aliases | Permission | Player example | Admin example | Errors |
| --- | --- | --- | --- | --- | --- | --- |
| `island` | `/island [go|create|expand|info|sethome|setname|setdesc|leave|help]` | `is` | none in `plugin.yml` | `/island create` | `/island info` | `E1`, `E2`, `E6` |
| `islandbypass` | `/islandbypass [on|off|toggle|status|list]` | `ib` | `grivience.island.bypass` | n/a | `/islandbypass toggle` | `E1`, `E2`, `E3` |
| `visit` | `/visit <player>` | `vi`, `v` | none in `plugin.yml` | `/visit Kazutos` | `/visit Kazutos` | `E2`, `E5`, `E6` |
| `invite` | `/invite <player>` | `-` | none in `plugin.yml` | `/invite Kazutos` | `/invite Kazutos` | `E2`, `E5`, `E6` |
| `sbkick` | `/sbkick <player>` | `-` | none in `plugin.yml` | `/sbkick Kazutos` | `/sbkick Kazutos` | `E2`, `E5`, `E6` |
| `sbkickall` | `/sbkickall` | `-` | none in `plugin.yml` | `/sbkickall` | `/sbkickall` | `E6` |
| `setguestspawn` | `/setguestspawn` | `-` | none in `plugin.yml` | `/setguestspawn` | `/setguestspawn` | `E6` |
| `setspawn` | `/setspawn` | `-` | none in `plugin.yml` | `/setspawn` | `/setspawn` | `E6` |
| `minion` | `/minion [list|collectall|pickupall|give|givefuel|giveupgrade|givefragment|constellation|help]` | `minions` | none in `plugin.yml` (`give*` admin paths require `grivience.admin` in code) | `/minion list` | `/minion give iron 1 Kazutos` | `E1`, `E2`, `E3`, `E5`, `E6` |
| `trade` | `/trade <player> / /trade accept [player] / /trade decline [player] / /trade cancel` | `tr` | none in `plugin.yml` | `/trade Kazutos` | `/trade accept Kazutos` | `E2`, `E5`, `E6` |

### Economy and Admin Utility

| Command | Syntax | Aliases | Permission | Player example | Admin example | Errors |
| --- | --- | --- | --- | --- | --- | --- |
| `bazaar` | `/bazaar [menu|custom|vanilla|help]` | `bz` | none in `plugin.yml` | `/bazaar menu` | `/bazaar help` | `E1`, `E2`, `E6` |
| `npcshop` | `/npcshop` | `sellnpc`, `commodityshop` | none in `plugin.yml` | `/npcshop` | `/npcshop` | `E6` |
| `bank` | `/bank [balance|deposit|withdraw]` | `bnk` | none in `plugin.yml` | `/bank balance` | `/bank withdraw` | `E1`, `E2`, `E6` |
| `bitsadmin` | `/bitsadmin <set|give|take|check> <player> <amount>` | `-` | `grivience.admin` | n/a | `/bitsadmin give Kazutos 500` | `E1`, `E2`, `E3`, `E5` |
| `coinsadmin` | `/coinsadmin <balance|purse|bank|help> [args]` | `-` | `grivience.admin` | n/a | `/coinsadmin purse set Kazutos 100000` | `E1`, `E2`, `E3`, `E5` |
| `bossbar` | `/bossbar <announce|to|clear|reload|help> [args]` | `announce` | `grivience.announce` | n/a | `/bossbar announce Server restart in 5 minutes` | `E1`, `E2`, `E3`, `E5` |
| `grivience` | `/grivience reload` | `-` | `grivience.admin` | n/a | `/grivience reload` | `E1`, `E2`, `E3` |
| `admintp` | `/admintp <player> OR <x> <y> <z> [world]` | `-` | `grivience.admin` | n/a | `/admintp Kazutos` | `E2`, `E3`, `E5` |
| `jumppad` | `/jumppad <create|target|corner1|corner2|remove|list> [id]` | `-` | `grivience.admin` | n/a | `/jumppad create farm_pad` | `E1`, `E2`, `E3`, `E6` |
| `sbadmin` | `/sbadmin <set|setxp|give|take|check|reset> <player> [value]` | `-` | `grivience.admin` | n/a | `/sbadmin give Kazutos 25` | `E1`, `E2`, `E3`, `E5` |
| `skillxp` | `/skillxp <give|check> <player> [skill] [amount]` | `-` | `grivience.admin` | n/a | `/skillxp give Kazutos farming 5000` | `E1`, `E2`, `E3`, `E5` |
| `zone` | `/zone <create|delete|select|setpos1|setpos2|setname|setdisplayname|setcolor|setpriority|list|info|reload>` | `zoneeditor` | `grivience.admin` | n/a | `/zone create farm_hub` | `E1`, `E2`, `E3`, `E6` |

## Permission Nodes

| Permission node | Default | Purpose |
| --- | --- | --- |
| `grivience.admin` | `op` | Master admin node; includes most operational/admin child permissions |
| `grivience.admin.globalevent` | `op` | Manage global XP boost events |
| `grivience.admin.mineevent` | `op` | Manage mining events and related controls |
| `grivience.wizardtower.admin` | `op` | Manage wizard tower ZNPC binding/reloads |
| `grivience.island.bypass` | `op` | Bypass island build restrictions |
| `grivience.island.bypass.admin` | `op` | View/manage bypass-enabled players |
| `grivience.announce` | `op` | Use bossbar announcement command |
| `grivience.collections.admin` | `op` | Collection admin operations |
| `grivience.fasttravel.admin` | `op` | Edit/reload fast travel destinations |
| `grivience.storage.admin` | `op` | Storage administration |
| `grivience.storage.admin.other` | `op` | Storage admin actions on other players |
| `grivience.endmines.build` | `op` | End Mines build/interact bypass |
| `grivience.farmhub.build` | `op` | Farm Hub build/interact bypass |
| `grivience.minion.bypass` | `op` | Bypass minion placement/ownership limits |
| `grivience.visit.bypass` | `op` | Bypass visit restrictions |
| `grivience.pluginhider.bypass` | `false` | Bypass plugin hider command blocking |
| `grivience.visit.guestlimit.unlimited` | `false` | Unlimited island guest cap |
| `grivience.visit.guestlimit.vip` | `false` | VIP guest cap |
| `grivience.visit.guestlimit.mvp` | `false` | MVP guest cap |
| `grivience.visit.guestlimit.mvpplus` | `false` | MVP+ guest cap |
| `grivience.visit.guestlimit.youtuber` | `false` | YouTuber guest cap |
| `storage.personal` | `true` | Access personal storage |
| `storage.personal.upgrade` | `true` | Upgrade personal storage |
| `storage.vault` | `true` | Access vault storage |
| `storage.vault.upgrade` | `true` | Upgrade vault storage |
| `storage.ender` | `true` | Access ender storage |
| `storage.ender.upgrade` | `true` | Upgrade ender storage |
| `storage.backpack` | `true` | Access backpack storage |
| `storage.backpack.upgrade` | `true` | Upgrade backpack storage |
| `storage.warehouse` | `true` | Access warehouse storage |
| `storage.warehouse.upgrade` | `true` | Upgrade warehouse storage |
| `storage.accessory` | `true` | Access accessory bag |
| `storage.accessory.upgrade` | `true` | Upgrade accessory bag |
| `storage.potion` | `true` | Access potion bag |
| `storage.potion.upgrade` | `true` | Upgrade potion bag |
| `grivience.personalcompactor` | `true` | Use personal compactor and auto-compaction |

## Permission Notes

- `grivience.admin` includes children such as:
  - `grivience.admin.mineevent`
  - `grivience.admin.globalevent`
  - `grivience.pluginhider.bypass`
  - other system admin nodes declared under `children` in `plugin.yml`
- Some commands with no `plugin.yml` permission still enforce admin checks for specific subcommands in code (for example `/dungeon reload`, `/dungeon give`, `/dungeonhub set`).
- Plugin discovery commands are runtime-filtered by `security.plugin-hider.*` unless sender has `grivience.pluginhider.bypass`.

## Code-Enforced Subcommand Permissions

These are enforced in command executors even where `plugin.yml` command permission is unset:

- `grivience.admin`: `/hub set`, `/minehub set`, `/farmhub set|setpos1|setpos2|setarea`, `/dungeon reload`, `/dungeon give`, `/dungeonhub set`
- `grivience.wizardtower.admin`: `/wizardtower setznpc`, `/wizardtower reload`
- operator-only: `/endmine set|generate|access|mobs|mineables`
- storage and accessory nodes are checked at runtime for storage submenus (`storage.*` permissions), plus `grivience.storage.admin` for storage admin actions

## Related Pages

- `Configuration Reference`
- `Data Files and Persistence`
- `Dungeons and Parties`
