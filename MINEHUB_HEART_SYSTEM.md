# Heart of the Minehub System

## Overview
A comprehensive HOTM (Heart of the Mountain) progression system specifically designed for the Minehub world. This system provides hours of engaging gameplay through perks, commissions, daily rewards, treasure hunting, and ultimate abilities.

---

## Table of Contents
1. [Core Features](#core-features)
2. [Progression System](#progression-system)
3. [Perk Tree](#perk-tree)
4. [Commission System](#commission-system)
5. [Treasure System](#treasure-system)
6. [Ultimate Ability](#ultimate-ability)
7. [Commands](#commands)
8. [Integration](#integration)
9. [Configuration](#configuration)

---

## Core Features

### 🎯 Multi-Layer Progression
- **15 Heart Levels**: Progressive XP thresholds with increasing rewards
- **Titanium Powder Currency**: Earned from mining, used to upgrade perks
- **Token System**: Unlock perks with tokens earned from leveling
- **Peak of the Minehub**: 7-level permanent core upgrade

### 💎 13 Unique Perks
Organized in 7 tiers from basic Fortune to ultimate abilities:
- **Tier 1**: Mining Fortune, Sapphire Fortune
- **Tier 2**: Mining Speed, Efficient Miner
- **Tier 3**: Powder Buff, Experience Wisdom
- **Tier 4**: Titanic Experience, Mining Madness
- **Tier 5**: Pristine, Daily Powder
- **Tier 6**: Lucky Minehub, Forge Master
- **Tier 7**: Minehub Heart Core (Ultimate Ability)

### 📋 Daily Commission System
- **4 Commissions Per Day**: Reset every 24 hours
- **15+ Commission Types**: From basic ore mining to complex variety challenges
- **Auto-Refresh**: New commissions generate as you complete them
- **Powder & XP Rewards**: Bonus progression through commissions

### 🎲 Random Events
- **Titanic Experience**: 4x drops, 3x powder, 2x XP
- **Pristine**: 5x drops from ore
- **Mining Madness**: Haste III for 10 seconds
- **Treasure Finds**: Random valuable items while mining

### 🏆 Ultimate Ability: Mining Frenzy
- **Duration**: 30 seconds of maximum mining power
- **Effects**: Haste V, Luck III, massive bonuses
- **Cooldown**: 10 minutes
- **Requirement**: Unlock Minehub Heart Core perk

---

## Progression System

### XP Thresholds
```
Level 1:  0       →  Level 9:  5,600
Level 2:  50      →  Level 10: 7,700
Level 3:  150     →  Level 11: 10,400
Level 4:  350     →  Level 12: 14,000
Level 5:  700     →  Level 13: 18,500
Level 6:  1,200   →  Level 14: 24,000
Level 7:  1,900   →  Level 15: MAX
Level 8:  2,800
```

### Token Rewards Per Level
```
Level 1: 2 tokens  →  Level 9:  5 tokens
Level 2: 2 tokens  →  Level 10: 5 tokens
Level 3: 3 tokens  →  Level 11: 5 tokens
Level 4: 3 tokens  →  Level 12: 6 tokens
Level 5: 3 tokens  →  Level 13: 6 tokens
Level 6: 4 tokens  →  Level 14: 7 tokens
Level 7: 4 tokens  →  Level 15: MAX
Level 8: 4 tokens
```

### Earning Progression

#### XP Sources
- **Common Ores** (Coal, Iron, Copper): 1-2 XP
- **Rare Ores** (Gold, Lapis, Redstone, Sapphire): 3-5 XP
- **Epic Ores** (Diamond, Emerald, Obsidian): 5-8 XP
- **Commissions**: 10-40 XP per completion
- **Multipliers**: Experience Wisdom perk (+10% per rank), Peak (+5% per level)

#### Titanium Powder Sources
- **Common Ores**: 1-2 powder
- **Rare Ores**: 2-4 powder
- **Epic Ores**: 3-5 powder
- **Commissions**: 20-80 powder per completion
- **Daily Reward**: 500 powder per rank of Daily Powder perk
- **Multipliers**: Powder Buff perk (+15% per rank), Peak (+4% per level)

### Peak of the Minehub

Peak Level Requirements:
```
Level 1: Heart Level 2  (+5% XP, +4% Powder, +2% Fortune)
Level 2: Heart Level 4  (+10% XP, +8% Powder, +4% Fortune)
Level 3: Heart Level 6  (+15% XP, +12% Powder, +6% Fortune)
Level 4: Heart Level 9  (+20% XP, +16% Powder, +8% Fortune)
Level 5: Heart Level 11 (+25% XP, +20% Powder, +10% Fortune)
Level 6: Heart Level 13 (+30% XP, +24% Powder, +12% Fortune)
Level 7: Heart Level 15 (+35% XP, +28% Powder, +14% Fortune)
        → MASTER OF THE MINEHUB
```

---

## Perk Tree

### Tier 1 Perks (Level 1 Required)

#### ⛏️ Mining Fortune
- **Icon**: Iron Pickaxe
- **Slot**: 10
- **Max Rank**: 10
- **Unlock Cost**: 1 Token
- **Upgrade Cost**: 20-138 powder (scaling)
- **Effect**: +5 Mining Fortune per rank
- **Max Effect**: +50 Mining Fortune at rank 10

#### 💎 Sapphire Fortune
- **Icon**: Blue Stained Glass
- **Slot**: 12
- **Max Rank**: 8
- **Unlock Cost**: 1 Token
- **Upgrade Cost**: 25-130 powder
- **Effect**: +10% Sapphire drop chance per rank
- **Max Effect**: +80% Sapphire drops at rank 8

### Tier 2 Perks (Level 2 Required)

#### ⚡ Mining Speed
- **Icon**: Golden Pickaxe
- **Slot**: 14
- **Max Rank**: 10
- **Unlock Cost**: 1 Token
- **Upgrade Cost**: 30-192 powder
- **Effect**: +10% mining speed per rank
- **Max Effect**: +100% mining speed at rank 10
- **Note**: Increases mining speed, stackable with Haste

#### 🌟 Efficient Miner
- **Icon**: Emerald
- **Slot**: 16
- **Max Rank**: 5
- **Unlock Cost**: 1 Token
- **Upgrade Cost**: 40-128 powder
- **Effect**: 5% chance per rank to mine instantly
- **Max Effect**: 25% instant mining at rank 5

### Tier 3 Perks (Level 3 Required)

#### ✨ Powder Buff
- **Icon**: Glowstone Dust
- **Slot**: 20
- **Max Rank**: 10
- **Unlock Cost**: 1 Token
- **Upgrade Cost**: 35-215 powder
- **Effect**: +15% Titanium Powder gains per rank
- **Max Effect**: +150% powder at rank 10
- **Synergy**: Stacks multiplicatively with Peak bonuses

#### 📚 Experience Wisdom
- **Icon**: Experience Bottle
- **Slot**: 24
- **Max Rank**: 8
- **Unlock Cost**: 1 Token
- **Upgrade Cost**: 45-220 powder
- **Effect**: +10% mining XP per rank
- **Max Effect**: +80% XP at rank 8
- **Synergy**: Stacks with Peak XP multiplier

### Tier 4 Perks (Level 4 Required)

#### ⚡ Titanic Experience
- **Icon**: Obsidian
- **Slot**: 29
- **Max Rank**: 5
- **Unlock Cost**: 2 Tokens
- **Upgrade Cost**: 60-200 powder
- **Effect**: 5% chance per rank to trigger Titanic Experience
- **Max Effect**: 25% chance at rank 5
- **Trigger**: 4x drops, 3x powder, 2x XP from that block
- **Visual**: Purple lightning message + Ender Dragon sound

#### 🔥 Mining Madness
- **Icon**: Redstone Block
- **Slot**: 31
- **Max Rank**: 3
- **Unlock Cost**: 2 Tokens
- **Upgrade Cost**: 80-180 powder
- **Effect**: 15% chance per rank to trigger Mining Madness
- **Max Effect**: 45% chance at rank 3
- **Trigger**: Haste III for 10 seconds
- **Duration**: Can overlap, essentially permanent at high ranks

### Tier 5 Perks (Level 5 Required)

#### ✦ Pristine
- **Icon**: Diamond Block
- **Slot**: 33
- **Max Rank**: 5
- **Unlock Cost**: 2 Tokens
- **Upgrade Cost**: 100-340 powder
- **Effect**: 4% chance per rank for Pristine
- **Max Effect**: 20% chance at rank 5
- **Trigger**: 5x drops from that ore
- **Visual**: White sparkle message + Amethyst chime sound
- **Note**: Does not stack with Titanic Experience

#### 🎁 Daily Powder
- **Icon**: Gold Block
- **Slot**: 38
- **Max Rank**: 1
- **Unlock Cost**: 2 Tokens
- **Effect**: Grants +500 Titanium Powder daily
- **Claim**: Click in Heart GUI to claim daily reward
- **Cooldown**: 24 hours between claims

### Tier 6 Perks (Level 6 Required)

#### 🍀 Lucky Minehub
- **Icon**: Enchanted Golden Apple
- **Slot**: 40
- **Max Rank**: 3
- **Unlock Cost**: 3 Tokens
- **Upgrade Cost**: 120-260 powder
- **Effect**: 2% chance per rank to find treasure
- **Max Effect**: 6% treasure chance at rank 3
- **Treasure Pool**:
  - Common Ores: Ore Fragments, Volta
  - Rare Ores: Enchanted Sapphire, Oil Barrel, Titanium Engine
  - Epic Ores: Titanium Engine, Gemstone Engine, Mining XP Scrolls

#### 🔨 Forge Master
- **Icon**: Anvil
- **Slot**: 42
- **Max Rank**: 5
- **Unlock Cost**: 2 Tokens
- **Upgrade Cost**: 140-420 powder
- **Effect**: Reduces Drill Forge times by 8% per rank
- **Max Effect**: -40% forge time at rank 5
- **Integration**: Directly reduces DrillForgeManager project durations

### Tier 7 Ultimate Perk (Level 7 Required)

#### 🌟 Minehub Heart Core
- **Icon**: Nether Star
- **Slot**: 44
- **Max Rank**: 1
- **Unlock Cost**: 5 Tokens
- **Upgrade Cost**: 300 powder
- **Effect**: Unlocks Mining Frenzy ability
- **Ability**: 30 seconds of maximum mining power
  - Haste V
  - Luck III
  - All perk effects amplified
  - Visual celebration on activation
- **Cooldown**: 10 minutes
- **Activation**: `/minehub frenzy` or keybind

---

## Commission System

### Overview
Daily renewable tasks that provide bonus Titanium Powder and XP. Players receive 4 commissions per day that refresh every 24 hours.

### Commission Types

#### Basic Ore Commissions (Common, 80% spawn rate)
1. **Mine Coal**: 100-150 Coal Ore → 20 powder
2. **Mine Iron**: 80-120 Iron Ore → 25 powder
3. **Mine Copper**: 75-110 Copper Ore → 22 powder
4. **Mine Gold**: 50-80 Gold Ore → 30 powder
5. **Mine Lapis**: 40-70 Lapis Ore → 35 powder
6. **Mine Redstone**: 60-90 Redstone Ore → 32 powder
7. **Mine Diamond**: 20-40 Diamond Ore → 50 powder
8. **Mine Emerald**: 15-30 Emerald Ore → 60 powder
9. **Mine Sapphire**: 50-85 Sapphire → 40 powder

#### Advanced Commissions (Common, 80% spawn rate)
10. **Deepslate Miner**: 120-180 Deepslate Ores → 35 powder
11. **Block Collector**: 15-30 Mineral Blocks → 55 powder
12. **Sapphire Hunter**: 80-130 Sapphire → 50 powder
13. **Obsidian Breaker**: 25-45 Obsidian → 65 powder

#### Special Commissions (Rare, 20% spawn rate)
14. **Ore Variety**: Mine 5 different ore types (10 each) → 45 powder
    - Tracks progress across Coal, Iron, Gold, Diamond, etc.
    - Must reach 10 of each type for 5 different ores
    - Visual progress indicator shows completed types

15. **Master Miner**: Mine 300 total blocks in Minehub → 80 powder
    - Any mineable block counts
    - Great for AFK mining sessions
    - Highest powder reward

### Commission Mechanics

#### Auto-Progress Tracking
- Commissions track progress automatically as you mine
- Progress persists across sessions
- Visual progress bars in commission GUI
- Sound notification when completed

#### Claiming Rewards
1. Complete commission (reach target)
2. Receive completion notification
3. Open commission GUI (`/minehub commissions`)
4. Click completed commission to claim
5. Instantly receive powder + XP
6. New commission generates immediately

#### Daily Reset
- Resets at 24 hours after first generation
- All incomplete commissions are cleared
- 4 new commissions generated
- Reset timer shown in GUI

#### Reward Calculation
- **Powder Reward**: Listed per commission (20-80 powder)
- **XP Reward**: 50% of powder value
- **Example**: 40 powder commission → 40 powder + 20 XP

---

## Treasure System

### Lucky Minehub Perk Integration
When you have ranks in Lucky Minehub, every block you mine has a chance to drop bonus treasure based on the ore tier.

### Treasure Tables

#### Common Ore Treasure (Coal, Iron, Copper)
- Ore Fragment Bundle (5x): Common
- Volta Fuel: Uncommon

#### Rare Ore Treasure (Gold, Lapis, Redstone, Sapphire)
- Enchanted Sapphire: Rare
- Oil Barrel: Uncommon
- Titanium Engine: Rare

#### Epic Ore Treasure (Diamond, Emerald, Obsidian)
- Titanium Engine: Rare
- Gemstone Engine: Epic
- Mining XP Scroll (2x): Uncommon

### Treasure Mechanics
- **Trigger Chance**: 2% per rank of Lucky Minehub (max 6%)
- **Ore Tier Detection**: Automatic based on block mined
- **Drop Method**: Added to inventory or dropped at location
- **Visual Feedback**: Purple "✦ TREASURE!" message + chest sound
- **Doesn't Replace**: Normal ore drops still occur

---

## Ultimate Ability: Mining Frenzy

### Unlocking
1. Reach Heart of the Minehub Level 7
2. Have 5 available tokens
3. Unlock Minehub Heart Core perk
4. Spend 300 Titanium Powder

### Activation
- **Command**: `/minehub frenzy`
- **Keybind**: Can be bound via Minecraft controls
- **Cooldown**: 10 minutes
- **Duration**: 30 seconds

### Effects During Frenzy
1. **Haste V**: Instant mining on most blocks
2. **Luck III**: Massively increased drop rates
3. **All Perk Bonuses**: Fortune, Speed, Pristine, etc. all active
4. **Visual Effects**:
   - Golden border message
   - Challenge complete sound
   - Particle effects (if implemented)
   - Action bar timer

### Strategic Use
- **Boss Events**: Use before fighting mining bosses
- **Commission Rush**: Complete multiple commissions quickly
- **Treasure Hunting**: Maximize Lucky Minehub proc rate
- **Powder Farming**: Combine with Powder Buff for massive gains

### Cooldown Management
- Cooldown persists across sessions
- GUI shows remaining cooldown time
- Cannot activate while on cooldown
- Logging out doesn't reset cooldown

---

## Commands

### Player Commands

#### `/minehub`
- **Permission**: `grivience.minehub.heart` (default: true)
- **Description**: Opens the Heart of the Minehub GUI
- **Aliases**: `/hotm`, `/heart`

#### `/minehub commissions`
- **Permission**: `grivience.minehub.commissions` (default: true)
- **Description**: Opens the commissions GUI
- **Aliases**: `/minehub comm`, `/minehub commission`

#### `/minehub frenzy`
- **Permission**: `grivience.minehub.frenzy` (default: true)
- **Description**: Activates Mining Frenzy (if unlocked)
- **Aliases**: `/minehub activate`
- **Requirement**: Minehub Heart Core perk unlocked

#### `/minehub help`
- **Permission**: None
- **Description**: Shows help information
- **Aliases**: `/minehub ?`

### Admin Commands (Future Enhancement)

#### `/minehub admin reset <player>`
- **Permission**: `grivience.admin`
- **Description**: Resets a player's Heart progress
- **Usage**: `/minehub admin reset PlayerName`

#### `/minehub admin setlevel <player> <level>`
- **Permission**: `grivience.admin`
- **Description**: Sets a player's Heart level
- **Usage**: `/minehub admin setlevel PlayerName 10`

#### `/minehub admin givepowder <player> <amount>`
- **Permission**: `grivience.admin`
- **Description**: Gives Titanium Powder to a player
- **Usage**: `/minehub admin givepowder PlayerName 1000`

#### `/minehub admin givetokens <player> <amount>`
- **Permission**: `grivience.admin`
- **Description**: Gives tokens to a player
- **Usage**: `/minehub admin givetokens PlayerName 5`

---

## Integration

### Required Integrations

#### 1. GriviencePlugin Registration
```java
// In GriviencePlugin.java onEnable():
MinehubHeartManager minehubHeartManager = new MinehubHeartManager(this, collectionsManager);
MinehubCommissionManager minehubCommissionManager = new MinehubCommissionManager(this, collectionsManager, minehubHeartManager);

// Register managers
this.minehubHeartManager = minehubHeartManager;
this.minehubCommissionManager = minehubCommissionManager;

// Register listeners
Bukkit.getPluginManager().registerEvents(minehubHeartManager, this);
Bukkit.getPluginManager().registerEvents(minehubCommissionManager, this);

// Register command
getCommand("minehub").setExecutor(new MinehubHeartCommand(this, minehubHeartManager, minehubCommissionManager));
```

#### 2. MinehubOreGenerationListener Integration
```java
// In MinehubOreGenerationListener.java, onBreakRegen() method after drop handling:

// Award Heart XP and Powder
if (plugin.getMinehubHeartManager() != null) {
    MinehubHeartManager.MiningBonus bonus = plugin.getMinehubHeartManager().recordMining(player, originalType);

    // Apply fortune bonus to drops
    if (bonus.fortuneMultiplier() > 1.0) {
        // Increase drop amount based on fortune
    }

    // Add bonus drops from Titanic/Pristine
    if (bonus.bonusDrops() > 0) {
        // Add extra drops
    }
}

// Award commission progress
if (plugin.getMinehubCommissionManager() != null) {
    plugin.getMinehubCommissionManager().recordMining(player, originalType);
}
```

#### 3. DrillForgeManager Integration
```java
// In DrillForgeManager.java, projectedDurationMillis() method:

public long projectedDurationMillis(Player player, ForgeProjectType type) {
    long baseDuration = type.baseDurationMillis();

    // Apply heat bonus
    baseDuration = adjustedDurationMillis(baseDuration, snapshot(player).heat());

    // Apply Forge Master perk reduction
    if (plugin.getMinehubHeartManager() != null) {
        int reductionPercent = plugin.getMinehubHeartManager().getForgeTimeReduction(player);
        baseDuration = (long) (baseDuration * (1.0 - (reductionPercent / 100.0)));
    }

    return baseDuration;
}
```

#### 4. MiningItemListener Integration (for Instant Mining)
```java
// In mining break event:
if (plugin.getMinehubHeartManager() != null) {
    double instantChance = plugin.getMinehubHeartManager().getInstantMineChance(player);
    if (ThreadLocalRandom.current().nextDouble() < instantChance) {
        event.setInstaBreak(true);
        player.sendMessage(ChatColor.GREEN + "⚡ Instant Mine!");
    }
}
```

### Accessor Methods Needed in GriviencePlugin

```java
public class GriviencePlugin extends JavaPlugin {
    private MinehubHeartManager minehubHeartManager;
    private MinehubCommissionManager minehubCommissionManager;

    public MinehubHeartManager getMinehubHeartManager() {
        return minehubHeartManager;
    }

    public MinehubCommissionManager getMinehubCommissionManager() {
        return minehubCommissionManager;
    }
}
```

---

## Configuration

### plugin.yml
```yaml
commands:
  minehub:
    description: Opens the Heart of the Minehub GUI
    usage: /minehub [commissions|frenzy|help]
    aliases: [hotm, heart]
    permission: grivience.minehub.heart

permissions:
  grivience.minehub.*:
    description: Grants all Minehub Heart permissions
    default: true
    children:
      grivience.minehub.heart: true
      grivience.minehub.commissions: true
      grivience.minehub.frenzy: true

  grivience.minehub.heart:
    description: Allows access to Heart of the Minehub GUI
    default: true

  grivience.minehub.commissions:
    description: Allows access to commissions system
    default: true

  grivience.minehub.frenzy:
    description: Allows activation of Mining Frenzy
    default: true
```

### config.yml
```yaml
minehub:
  heart:
    enabled: true
    # Enable/disable the Heart of the Minehub system

  commissions:
    enabled: true
    # Enable/disable daily commissions

    reset-hour: 0
    # Hour of day (0-23) when commissions reset
    # Default: 0 (midnight server time)
```

---

## Data Storage

### minehub-heart-data.yml Structure
```yaml
<profile-uuid>:
  level: 5
  xp: 1200
  powder: 450
  available-tokens: 3
  peak-level: 2
  last-daily-claim: 1234567890000
  mining-frenzy-unlocked: 1234567890000
  mining-frenzy-cooldown: 1234567890000
  perks:
    mining_fortune: 7
    sapphire_fortune: 5
    mining_speed: 3
    powder_buff: 4
```

### minehub-commissions.yml Structure
```yaml
<profile-uuid>:
  last-reset: 1234567890000
  completed-today: 2
  active:
    '0':
      type: MINE_DIAMOND
      target: 35
      progress: 12
      started: 1234567890000
    '1':
      type: MASTER_MINER
      target: 300
      progress: 148
      started: 1234567890000
```

---

## Gameplay Loop

### Early Game (Levels 1-5)
1. Start mining in Minehub to gain XP and powder
2. Unlock Mining Fortune and Sapphire Fortune (Tier 1)
3. Upgrade to rank 3-5 for noticeable bonuses
4. Complete easy commissions (Coal, Iron, Copper)
5. Upgrade Peak of the Minehub to level 1-2
6. Unlock Mining Speed or Efficient Miner (Tier 2)

**Time Investment**: 2-4 hours of active mining

### Mid Game (Levels 6-10)
1. All Tier 1-2 perks unlocked and partially upgraded
2. Unlock Powder Buff and Experience Wisdom (Tier 3)
3. Focus on rare ore commissions (Diamond, Emerald)
4. Upgrade Peak to level 3-4
5. Unlock Titanic Experience (Tier 4)
6. Start seeing rare event triggers (Titanic, Madness)
7. Save tokens for Tier 5 perks

**Time Investment**: 6-10 hours total

### Late Game (Levels 11-14)
1. Unlock Pristine and Daily Powder (Tier 5)
2. Complete variety and master miner commissions
3. Unlock Lucky Minehub for treasure hunting
4. Unlock Forge Master if using drills
5. Upgrade Peak to level 5-6
6. Max out favorite perks to rank 10
7. Farm powder through commissions and daily rewards

**Time Investment**: 15-25 hours total

### Endgame (Level 15)
1. Reach maximum Heart level
2. Unlock Minehub Heart Core (Tier 7)
3. Activate Mining Frenzy during important sessions
4. Max out Peak of the Minehub (level 7)
5. Achieve "Master of the Minehub" title
6. Max all perks to rank 10
7. Daily commission routine for powder maintenance

**Time Investment**: 30-50+ hours total

---

## Engagement Mechanics

### Daily Activities (15-30 minutes)
- Claim Daily Powder reward
- Complete 2-3 easy commissions
- Check for Mining Frenzy cooldown
- Quick mining session for event triggers

### Weekly Goals
- Complete all 28 commissions (7 days × 4)
- Reach next Heart level
- Unlock new perk tier
- Upgrade Peak of the Minehub

### Long-Term Goals
- Max out favorite perks
- Unlock Mining Frenzy
- Achieve Master of the Minehub
- Complete all commission types
- Find all treasure types

### Social Competition
- Compare Heart levels with friends
- Race to unlock Mining Frenzy first
- Commission completion leaderboards
- Peak level status symbol

---

## Balancing Notes

### Progression Curve
- **Levels 1-5**: Fast progression to hook players (2-4 hours)
- **Levels 6-10**: Moderate pace with noticeable power increases (4-6 hours)
- **Levels 11-14**: Slower grind rewarded with powerful perks (5-15 hours)
- **Level 15**: Endgame goal requiring dedication (10-30 hours)

### Power Scaling
- **Early perks**: Immediate impact (Fortune, Speed)
- **Mid perks**: Efficiency gains (Powder Buff, XP Wisdom)
- **Late perks**: Game-changing abilities (Pristine, Titanic)
- **Ultimate perk**: Temporary god mode (Mining Frenzy)

### Resource Economy
- **Tokens**: Limited resource, forces meaningful choices
- **Powder**: Farmable resource, rewards consistent play
- **Commissions**: Guaranteed daily powder income
- **Daily Reward**: Catch-up mechanism for casual players

---

## Future Enhancements

### Planned Features
1. **Prestige System**: Reset for permanent bonuses
2. **Mining Boss Events**: Special rare spawns with unique rewards
3. **Commission Leaderboards**: Track fastest completion times
4. **Perk Presets**: Save and swap perk configurations
5. **Guild Bonuses**: Additional perks for guild members
6. **Seasonal Events**: Limited-time special commissions

### Potential Perks
1. **Speed Mining**: Permanent Haste I-II
2. **Ore Sense**: Highlight valuable ores nearby
3. **Fortune Surge**: Temporary massive fortune boost
4. **Powder Magnet**: Auto-collect powder drops
5. **Commission Master**: Extra daily commission slot

---

## Technical Notes

### Performance Considerations
- Profile data cached in memory
- Auto-save every 5 minutes (dirty tracking)
- Commission progress tracked per mine event
- Event calculations optimized for minimal lag

### Thread Safety
- ConcurrentHashMap for profile storage
- Synchronous data access
- Bukkit task scheduling for async operations
- Profile locking for simultaneous edits

### Data Migration
- Automatic UUID-based profile system
- Backward compatible with old data format
- Graceful handling of missing perks
- Auto-cleanup of invalid data

---

## Credits

**System Design**: Custom HOTM-style progression for Minehub
**Inspiration**: Hypixel Skyblock Heart of the Mountain
**Implementation**: Fully integrated with existing Grivience systems
**Version**: 1.0.0

---

## Support

For bugs, suggestions, or questions:
- Check `/minehub help` in-game
- Review this documentation
- Contact server administrators
- Report issues on GitHub

---

**Last Updated**: 2026-04-18
**Minecraft Version**: 1.20+
**Plugin**: Grivience Skyblock
