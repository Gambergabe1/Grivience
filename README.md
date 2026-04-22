# Grivience Plugin

Japanese-themed dungeon + Skyblock plugin for Paper `1.21.x` with high-accuracy RPG mechanics.

## Current Feature Set

### Dungeon Core
- **Party-based floor runs** with config-driven floor data (`F1`-`F7`).
- **Expansive Procedural Generation**: Rooms are now 14 blocks high and corridors 11 blocks wide for high-fidelity combat.
- **Dungeon Classes**: Select your role via `/class`:
  - **Healer**: Massive self and team regeneration.
  - **Mage**: 2x Mana regeneration and intelligence scaling.
  - **Berserk**: Strength and healing on melee kills.
  - **Archer**: Ranged damage multipliers.
  - **Tank**: +50% Defense and teammate damage absorption.
- **Wither & Blood Keys**: Traditional dungeon progression mechanics.
- **Secrets & Blessings**: 1-3 hidden chests per room and powerful stat altars (Power, Life, Wisdom, Stone).
- **Hypixel-Style Bosses**: Bonzo, Scarf, Professor, Thorn, Livid, Sadan, and Necron archetypes with phased mechanics.

### Gear Progression
- **Dungeonization**: Non-dungeon gear must be "Dungeonized" via `/dungeon upgrade` (1,000,000 coins) to benefit from dungeon scaling.
- **Dungeon Stars**: Apply up to 5 stars to dungeonized gear. Each star grants **+10% stats** while inside a dungeon run.
- **Native Loot**: Gear found inside the dungeon is automatically dungeonized and ready for stars.

### Legacy 1.8 Combat
- **Instant Attacks**: Attack speed set to 1024.0, removing the 1.9+ cooldown for snappy, spammable combat.
- **Disabled Sweeps**: Area-of-effect sweep attacks are disabled for precise 1.8-style PvP/PvE.
- **Configurable Mechanics**: Toggle shields, attack speed, and sweep behavior in `config.yml`.

### Accessory System (Magical Power)
- **Magical Power (MP)** - Total power calculated from all unique accessories in your Accessory Bag.
- **Accessory Bag** - Dedicated storage for accessories with automatic slot expansion based on Skyblock Level.
- **Power Selection** - Choose various Powers (e.g., Commando, Bloody, Silky) that grant scaling stats based on your MP.
- **Stat Multiplier** - Every 10 MP grants a +0.5% bonus to all stats derived from accessories.

### Bazaar Shop
- **Order Matching Engine**: Price-time priority matching for buy/sell orders.
- **Shopping Bag System**: Claim filled orders and coin refunds in a dedicated GUI.
- **Professional Analytics**: Market depth and price history displays.

### Skyblock Core
- **Island Management**: Private island creation and upgrade paths.
- **Skyblock Leveling**: Comprehensive progression system with guide-driven milestones.
- **Pet System**: Equip companions with active and passive RPG perks.
- **Bank System**: Secure coin storage with interest and profile-specific purses.

## Commands

### Player Commands
- `/skyblock` (alias `/sb`) - Open the main Skyblock menu.
- `/island` (alias `/is`) - Island management.
- `/accessory` - Open Accessory Bag.
- `/class` - Select your Dungeon Class.
- `/dungeon upgrade` - Dungeonize and star your gear.
- `/bazaar` (alias `/bz`) - Open the Bazaar.
- `/bank` (alias `/bnk`) - Access the bank.

### Admin Commands
- `/grivience reload` - **Stable** system reload (millisecond precision).
- `/dungeon star <0-5>` - Manually set item star count.
- `/skyblockadmin` - Profile and island management.

## Configuration

Customizable via `plugins/Grivience/`:
- `config.yml`: Core settings & Legacy Combat toggles.
- `dungeons.yml`: Floor, Mob, and Boss configurations.
- `collections.yml`: Progression rewards.

## Build

Requires Java 21 and Paper API `1.21.x`.

```bash
./gradlew build
```
