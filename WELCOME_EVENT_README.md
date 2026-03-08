# Welcome Event System - Hard Hat Harry

## Overview

A structured welcome event system that introduces new players to core Skyblock mechanics through an NPC guide ("Hard Hat Harry") and provides balanced starter rewards.

**NPC Plugin Support**: ✅ **ZNPCS** (Optional - `/welcome` command works without)

---

## Features

### **Hard Hat Harry NPC**
- **Location**: Spawn (near island creation portal)
- **Role**: Starter Guide + Reward Distributor
- **Appearance**: Custom NPC with hard hat theme
- **Interaction**: Right-click to open welcome GUI
- **Plugin**: Uses **ZNPCS** (optional)

### **Welcome GUI**
- Beautiful purple-themed interface
- Shows reward status (claimed/available)
- One-click reward claiming
- Accessible via `/welcome` command

### **Starter Rewards**

#### **1. Starter Money**
- **Amount**: $500 (configurable)
- **Purpose**: Small starting balance to begin progression
- **Balance**: Enough to start, not skip progression
- **Distribution**: Via Vault economy or emerald representation

#### **2. Mining Boost**
- **Bonus**: +15% Mining XP
- **Duration**: 45 minutes
- **Purpose**: Early-game focused progression boost
- **Stacking**: Does not stack with high-tier multipliers

#### **3. Farming Boost**
- **Bonus**: +15% Farming XP
- **Duration**: 45 minutes
- **Purpose**: Starter-level farming acceleration
- **Stacking**: Does not stack with high-tier multipliers

#### **4. Starter Armor Set** (Optional)
- **Starter Hard Hat** (Leather Helmet)
  - +1 Armor
  - Unbreakable
  - Non-Tradeable
- **Starter Work Vest** (Leather Chestplate)
  - +3 Armor
  - Unbreakable
  - Non-Tradeable
- **Starter Work Pants** (Leather Leggings)
  - +2 Armor
  - Unbreakable
  - Non-Tradeable
- **Starter Work Boots** (Leather Boots)
  - +1 Armor
  - Unbreakable
  - Non-Tradeable

#### **5. Starter Tool Set** (Optional)
- **Starter Pickaxe** (Wooden)
  - Unbreakable
  - For mining stone and ores
- **Starter Axe** (Wooden)
  - Unbreakable
  - For chopping wood
- **Starter Hoe** (Wooden)
  - Unbreakable
  - For farming crops
- **Starter Seeds** (16x Wheat Seeds)
  - For initial farming

---

## Player Flow

### **1. Player Joins Server**
```
Welcome to Skyblock!

Find Hard Hat Harry at spawn to claim your
starter rewards!

Or type /welcome to claim now.
```

### **2. Directed to Hard Hat Harry**
- NPC located at spawn
- Right-click to interact
- Opens welcome GUI

### **3. Quick Intro to Skyblock**
GUI displays:
- Welcome message
- Starter objectives
- Reward preview
- Claim button

### **4. Player Claims Rewards**
- One-time per account
- Instant distribution
- Guide message with getting started tips

### **5. Getting Started Guide**
```
Getting Started:

1. Mine Resources - Start by mining stone and coal
2. Gather Wood - Chop trees for building materials
3. Start Farming - Plant seeds and grow food
4. Level Up Skills - Gain XP to unlock new content
5. Join a Party - Team up with friends for dungeons

Useful Commands:
/skyblock - Open Skyblock menu
/craft - Open crafting menu
/bazaar - Trade with other players
/party - Manage your party
/island - Manage your island

Your XP Boosts:
• Mining: +15% for 45 minutes
• Farming: +15% for 45 minutes
```

---

## Commands

| Command | Aliases | Description |
|---------|---------|-------------|
| `/welcome` | `/starter`, `/newplayer` | Open welcome GUI |
| `/welcome claim` | - | Claim starter rewards directly |
| `/welcome help` | - | Show help information |

---

## Configuration

```yaml
# ==================== WELCOME EVENT ====================
# Hard Hat Harry - Starter Guide & Rewards
welcome-event:
  # Enable welcome event system
  enabled: true

  # Starter money (balanced for economy)
  starter-money: 500.0

  # Mining XP boost percentage
  mining-boost-percent: 15.0

  # Farming XP boost percentage
  farming-boost-percent: 15.0

  # Boost duration in minutes
  boost-duration-minutes: 45

  # NPC spawn location (near island creation portal)
  spawn-world: world
  spawn-x: 0.5
  spawn-y: 100.0
  spawn-z: 0.5
  spawn-yaw: 0.0
  spawn-pitch: 0.0

  # Starter armor (optional - set to false to disable)
  give-starter-armor: true

  # Starter tools (optional - set to false to disable)
  give-starter-tools: true
```

---

## Balance Design

### **Economy Balance**
- **Starter Money**: $500 is enough to:
  - Buy basic tools from bazaar
  - Make small orders
  - NOT enough to skip early progression
- **Market Impact**: Minimal - too small to inflate early market

### **XP Boost Balance**
- **15% Bonus**: Significant but not game-breaking
- **45 Minutes**: Enough to reach ~level 10-15 in skills
- **No Stacking**: Does not stack with high-tier multipliers
- **Early-Game Only**: Most impactful at low levels

### **Item Balance**
- **Starter Armor**: 
  - Leather tier (lowest armor tier)
  - Unbreakable (no repair cost)
  - Non-tradeable (cannot be sold)
  - No auction value
- **Starter Tools**:
  - Wooden tier (lowest tool tier)
  - Unbreakable (no replacement needed)
  - Non-tradeable (cannot be sold)
  - No auction value

---

## Restrictions

### **One-Time Claim (STRICT)**
- ✅ **One claim per account** (UUID-based)
- ✅ **Saved immediately** to disk (prevents loss)
- ✅ **Checked before rewards** (prevents exploits)
- ✅ **Persists across sessions** (never resets)
- ✅ **Profile-specific** (if using profiles)
- ✅ **Tracked in** `welcome-claimed.yml`
- ✅ **Cannot be bypassed** via GUI or command
- ✅ **Clear messages** when already claimed

### **Non-Tradeable Items**
All starter items are:
- ❌ Cannot be traded
- ❌ Cannot be sold to NPCs
- ❌ Cannot be listed on bazaar
- ❌ Cannot be dropped (prevents alt abuse)
- ✅ Can be used by player
- ✅ Can be kept in inventory

### **One-Time Claim**
- ✅ One claim per account (UUID-based)
- ✅ Persists across sessions
- ✅ Profile-specific (if using profiles)
- ✅ Tracked in `welcome-claimed.yml`

### **Boost Limitations**
- ✅ Does not stack with other boosts
- ✅ Expires after duration
- ✅ Visible expiry notifications
- ✅ Tracked in `xp-boosts.yml`

### **Security Features**
- ✅ **UUID-based tracking** - Cannot bypass with name changes
- ✅ **Immediate save** - No data loss on crash
- ✅ **Pre-check validation** - Checked before any rewards given
- ✅ **GUI protection** - Cannot click through if already claimed
- ✅ **Command protection** - Command checks claim status
- ✅ **Clear feedback** - Players notified when already claimed

---

## Files Created

### **Data Files**
- `plugins/Grivience/welcome-claimed.yml` - Tracks claimed rewards
- `plugins/Grivience/xp-boosts.yml` - Tracks active XP boosts

### **Source Files**
- `StarterReward.java` - Reward data structure
- `XPBoostManager.java` - Manages XP boosts
- `WelcomeEventManager.java` - Main event manager
- `WelcomeCommand.java` - Command handler
- `WelcomeListener.java` - Event listener
- `WelcomeManager.java` - System manager

---

## Optional Integrations

### **ZNPCS NPC** (Optional)
If ZNPCS plugin is installed:
- ✅ Hard Hat Harry NPC created at spawn
- ✅ Right-click NPC to open GUI
- ✅ NPC persists across restarts
- ✅ NPC is clickable
- ✅ Uses reflection for compatibility

Without ZNPCS:
- ✅ `/welcome` command still works
- ✅ All functionality available via command
- ✅ No NPC, but same rewards

**Note**: The Welcome Event system uses reflection to integrate with ZNPCS, ensuring compatibility across different versions. If you encounter issues, please ensure you're using a recent version of ZNPCS.

### **Vault Economy** (Optional)
If Vault is installed:
- ✅ Starter money deposited via economy
- ✅ Proper currency handling

Without Vault:
- ✅ Emerald representation given
- ✅ 1 emerald = $100 value

---

## XP Boost Integration

### **With Skyblock Leveling**
The XP boost manager integrates with the existing leveling system:

```java
// Apply boost multiplier
double boostedXP = baseXP * xpBoostManager.getMiningBoostMultiplier(player);
```

### **Boost Tracking**
- Active boosts tracked per player
- Expiry checked every minute
- Notifications on expiry
- Persistent across restarts

---

## Getting Started Guide

### **For New Players**
1. **Join Server** - Receive welcome message
2. **Find Harry** - Locate Hard Hat Harry at spawn
3. **Claim Rewards** - Right-click or use `/welcome`
4. **Read Guide** - Review getting started tips
5. **Start Playing** - Begin your Skyblock journey!

### **Recommended First Steps**
1. Mine stone and coal with starter pickaxe
2. Chop trees with starter axe
3. Plant seeds with starter hoe
4. Level up mining/farming skills
5. Use starter money wisely on bazaar

---

## Admin Commands

### **Manage Welcome Event**
```bash
# Reload welcome event config
/grivience reload

# Check who has claimed
# (Check plugins/Grivience/welcome-claimed.yml)

# Reset player's claim (for testing)
# (Remove from welcome-claimed.yml)
```

---

## Troubleshooting

### **NPC Not Appearing**
- Check if ZNPCS plugin is installed
- Check spawn location in config
- Check console for errors
- Use `/welcome` command as alternative

### **Rewards Not Claiming**
- Check if already claimed (one-time only)
- Check console for errors
- Check if welcome-event is enabled in config
- Verify player has inventory space

### **Boosts Not Applying**
- Check if XPBoostManager is initialized
- Check boost duration hasn't expired
- Verify boost percentage in config
- Check console for errors

---

## Future Enhancements

Potential additions:
- [ ] Additional starter reward options
- [ ] Custom NPC skin for Hard Hat Harry
- [ ] Welcome quest line
- [ ] Tutorial island
- [ ] Video tutorial integration
- [ ] Discord welcome message
- [ ] Mentor system integration
- [ ] Achievement tracking

---

## Credits

Welcome Event System developed for Grivience plugin.
Inspired by Skyblock's starter guide systems.
NPC integration via ZNPCS (optional).

**Version**: 1.0  
**Last Updated**: 2026-02-24  
**NPC Support**: ZNPCS (optional)

