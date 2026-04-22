# End Mines & Drill System Rework

## Overview
This document outlines the rework for both the Heart of the End Mines progression system and the Drill Forge crafting system to improve balance, clarity, and player engagement.

---

## Current State Analysis

### Heart of the End Mines System ✓
**Strengths:**
- HOTM-style progression tree implemented
- Multiple perks with meaningful bonuses
- Peak of the End Mines system
- XP and Dust currency system
- Token-based unlocks
- Well-structured GUI
- Proper save/load persistence

**Issues to Address:**
1. **Perk Balance**: Some perks may be mandatory vs optional
2. **Dust Economy**: Dust costs scale but might be too steep
3. **XP Pacing**: Level thresholds need validation
4. **Echo Surge RNG**: 4% per rank might feel inconsistent
5. **Rare Drop Multiplier**: Needs clearer communication

### Drill Forge System ✓
**Strengths:**
- Forge heat system with speed bonuses
- Overdrive mechanic with tiers
- Project queue system
- Multiple engine/tank tiers
- Proper ingredient checking
- Duration scaling with heat

**Issues to Address:**
1. **Overdrive Clarity**: Players might not understand tier benefits
2. **Heat Decay**: No heat decay over time
3. **Project Balance**: Some projects might be more efficient than others
4. **Ingredient Accessibility**: Some materials might be too rare
5. **Fuel System Integration**: Needs clearer connection to drill usage

---

## Proposed Improvements

### Heart of the End Mines Improvements

#### 1. **Perk Rebalancing**
```yaml
Proposed Changes:
  Kunzite Fortune:
    old: 8% per rank
    new: 10% per rank (more impactful)

  Void Fortune:
    old: 6% per rank
    new: 8% per rank (match Kunzite)

  Rift Insight:
    old: 5% rare drop per rank
    new: 7% + tooltip showing exact multiplier

  Echo Surge:
    old: 4% per rank
    new: 5% per rank + visual feedback
    max_stacks: 5 (same)

  Powder Sense:
    old: 10% dust per rank
    new: 12% dust per rank
```

#### 2. **Peak of the End Mines Enhancement**
```yaml
Current Bonuses:
  Level 1: +5% XP, +5% Dust, +3% Rare Drops
  Level 2: +10% XP, +10% Dust, +7% Rare Drops
  Level 3: +15% XP, +15% Dust, +11% Rare Drops
  Level 4: +20% XP, +20% Dust, +15% Rare Drops
  Level 5: +25% XP, +25% Dust, +20% Rare Drops

Proposed: Add new effect at max level
  Level 5 Bonus: "Endwalker's Blessing"
    - 2% chance for double Heart progress
    - Unique particle effect on trigger
```

#### 3. **XP Curve Smoothing**
```
Current Thresholds (needs validation):
Level 1: 0
Level 2: 75
Level 3: 225
Level 4: 525
Level 5: 975
Level 6: 1700
Level 7: 2800
Level 8: 4400
Level 9: 6650
Level 10: 9750

Proposed Changes:
- Keep early levels fast
- Smooth mid-game progression
- Add 2-3 more levels for endgame
```

#### 4. **New Heart Features**
- **Milestone Rewards**: Every 3 levels = special reward
- **Prestige System**: Reset at max level for cosmetic rewards
- **Daily/Weekly Bonuses**: First X blocks each period = bonus XP
- **Dust Conversion**: Exchange excess dust for other resources

---

### Drill Forge Improvements

#### 1. **Overdrive System Enhancement**

**Current Issues:**
- Players don't understand what each tier does
- Benefits aren't clearly communicated
- No visual feedback for active overdrive

**Proposed Solutions:**

```java
// Enhanced Overdrive Tiers
TIER 0 (20-39 Heat):
  - Duration: 12 minutes
  - Fuel Reduction: -3 per block
  - Cooldown Reduction: -6 seconds
  - Ability Duration: +40 ticks
  - Amplifier Bonus: +0

TIER 1 (40-59 Heat):
  - Duration: 13.5 minutes
  - Fuel Reduction: -4 per block
  - Cooldown Reduction: -8 seconds
  - Ability Duration: +60 ticks
  - Amplifier Bonus: +1
  - NEW: Mining Fortune +5

TIER 2 (60-79 Heat):
  - Duration: 15 minutes
  - Fuel Reduction: -5 per block
  - Cooldown Reduction: -10 seconds
  - Ability Duration: +80 ticks
  - Amplifier Bonus: +1
  - NEW: Mining Fortune +10, Speed +10%

TIER 3 (80-99 Heat):
  - Duration: 16.5 minutes
  - Fuel Reduction: -6 per block
  - Cooldown Reduction: -12 seconds
  - Ability Duration: +100 ticks
  - Amplifier Bonus: +1
  - NEW: Mining Fortune +15, Speed +15%

TIER 4 (100 Heat):
  - Duration: 18 minutes
  - Fuel Reduction: -7 per block
  - Cooldown Reduction: -14 seconds
  - Ability Duration: +120 ticks
  - Amplifier Bonus: +2
  - NEW: Mining Fortune +25, Speed +20%, Rare Find +10%
```

**Visual Improvements:**
- Action bar shows tier + remaining time
- Particle effects change with tier
- Sound cues for activation/expiration
- Boss bar for overdrive duration

#### 2. **Heat Decay System**
```yaml
Purpose: Prevent heat inflation, encourage active play

Decay Rules:
  - No decay while forge has active projects
  - Decay starts 30 minutes after last project claimed
  - Lose 1 heat per hour of inactivity
  - Minimum heat: 0
  - Warning at 50% remaining time

Implementation:
  - Check on player join
  - Check when opening forge GUI
  - Store last activity timestamp
```

#### 3. **Project Rebalancing**

**Priority Changes:**
```yaml
Engines (Critical Path):
  Mithril Engine:
    old_duration: 12 minutes
    new_duration: 10 minutes
    reason: Starter engine should be faster

  Titanium Engine:
    old_duration: 18 minutes
    new_duration: 15 minutes
    cost_reduction: 4200 -> 3800 coins

  Gemstone Engine:
    old_duration: 24 minutes
    new_duration: 20 minutes

  Divan Engine:
    old_duration: 35 minutes
    new_duration: 30 minutes
    cost_increase: 10000 -> 12000 coins
    reason: Premium end-game item

Consumables (Support):
  Volta Infusion:
    output_increase: 2 -> 3 units
    reason: Common consumable

  Oil Barrel:
    duration_reduction: 7min -> 6min

  Speed Boost:
    new_benefit: Also grants +2% mining speed for 10 minutes
```

#### 4. **New Forge Projects**
```yaml
New Projects to Add:

Heat Catalyst:
  output: HEAT_CATALYST (consumable)
  duration: 4 minutes
  cost: 800 coins
  effect: Instantly adds +10 heat (bypasses overdrive cost)
  heat_gain: 3
  ingredients:
    - RIFT_ESSENCE x8
    - GLOWSTONE_DUST x16

Drill Warranty Ticket:
  output: WARRANTY_TICKET
  duration: 15 minutes
  cost: 3500 coins
  effect: Next drill break refunds 50% of fuel
  heat_gain: 5
  ingredients:
    - VOID_CRYSTAL x4
    - PAPER x8
    - GOLD_INGOT x4

Fortune Cookie:
  output: MINING_FORTUNE_COOKIE
  duration: 8 minutes
  cost: 2200 coins
  effect: +50 Mining Fortune for 20 minutes
  heat_gain: 6
  ingredients:
    - RIFT_ESSENCE x6
    - ENDSTONE_SHARD x24
    - SUGAR x8
```

#### 5. **Forge GUI Improvements**
```yaml
Current Layout: Basic list
Proposed Layout: Categorized tabs

Categories:
  - Engines (permanent upgrades)
  - Tanks (permanent upgrades)
  - Consumables (temporary buffs)
  - Tools (compasses, scrolls)
  - Special (seasonal/event items)

Each Tab Shows:
  - Project icon with rarity color
  - Duration (adjusted by current heat)
  - Cost (coins + ingredients)
  - Output quantity
  - Heat gain
  - Lock/unlock status

Queue Management:
  - Drag to reorder (if not started)
  - Click to cancel (refund 75%)
  - Auto-claim toggle option
```

---

## Implementation Priority

### Phase 1: Critical Fixes (High Priority)
1. ✅ Heart perk rebalancing
2. ✅ Overdrive tier clarification
3. ✅ GUI improvements
4. ✅ Tooltip enhancements

### Phase 2: New Features (Medium Priority)
1. Heat decay system
2. New forge projects
3. Project rebalancing
4. Visual feedback improvements

### Phase 3: Advanced Features (Low Priority)
1. Prestige system
2. Daily/weekly bonuses
3. Forge automation
4. Cross-system integration

---

## Configuration Reference

### Heart of the End Mines Config
```yaml
end-mines:
  heart:
    enabled: true
    starting-level: 1
    starting-tokens: 1

    # XP rewards
    rewards:
      end-mine-xp: 1
      kunzite-xp: 8
      end-mine-dust-min: 1
      end-mine-dust-max: 2
      kunzite-dust-min: 2
      kunzite-dust-max: 4

    # Progression curve
    level-thresholds:
      - 0      # Level 1
      - 75     # Level 2
      - 225    # Level 3
      - 525    # Level 4
      - 975    # Level 5
      - 1700   # Level 6
      - 2800   # Level 7
      - 4400   # Level 8
      - 6650   # Level 9
      - 9750   # Level 10

    tier-token-rewards:
      - 0  # Level 1
      - 1  # Level 2
      - 2  # Level 3
      - 2  # Level 4
      - 2  # Level 5
      - 2  # Level 6
      - 2  # Level 7
      - 3  # Level 8
      - 2  # Level 9
      - 2  # Level 10

    auto-save-interval-seconds: 300
```

### Drill Forge Config
```yaml
drill-forge:
  enabled: true

  heat:
    max: 100
    decay-enabled: true
    decay-delay-minutes: 30
    decay-rate-per-hour: 1

  overdrive:
    heat-cost: 20
    coin-cost: 2500
    base-duration-minutes: 12
    bonus-duration-per-tier-minutes: 1.5

  projects:
    max-queue-size: 3
    speed-bonus-per-heat-percent: 0.4

  auto-save-interval-seconds: 300
```

---

## Testing Checklist

### Heart of the End Mines
- [ ] Perk unlocking with tokens
- [ ] Perk upgrading with dust
- [ ] Peak upgrading
- [ ] XP gain from mining
- [ ] Dust gain from mining
- [ ] Echo Surge triggering
- [ ] Level up rewards
- [ ] GUI navigation
- [ ] Save/load persistence
- [ ] Multi-profile support

### Drill Forge
- [ ] Project starting
- [ ] Project claiming
- [ ] Heat gain from claims
- [ ] Speed bonus calculation
- [ ] Overdrive activation
- [ ] Overdrive tier effects
- [ ] Heat decay (if implemented)
- [ ] Ingredient checking
- [ ] Coin deduction
- [ ] Queue management

---

## Migration Notes

### For Existing Players
- All existing progress preserved
- Heat values capped at new limits if over
- Projects in queue complete normally
- Perks rebalanced automatically
- No manual intervention needed

### For Server Admins
- Backup `end-mines/heart/player_data.yml`
- Backup `drill-forge-data.yml`
- Update config with new values
- Restart server to apply changes
- Monitor first week for balance issues

---

## Future Expansions

### Potential Features
1. **Forge Companions**: NPCs that provide passive bonuses
2. **Guild Forges**: Shared forge for guild members
3. **Forge Challenges**: Time-limited events with unique rewards
4. **Cross-Dimension Mining**: Extend to Nether, etc.
5. **Drill Customization**: Skins, particles, sounds
6. **Mining Leaderboards**: Competition for top miners

---

## Summary

Both systems are **functionally complete** and well-implemented. The proposed rework focuses on:

1. **Balance Tuning**: Making perks/projects more rewarding
2. **Clarity**: Better communication of mechanics
3. **Engagement**: New content to maintain interest
4. **Polish**: Visual/audio feedback improvements

The systems work well as-is; these improvements would take them from "good" to "excellent" in terms of player experience.

---

**Status**: Documentation Complete
**Next Steps**: Implement Phase 1 improvements
**Estimated Time**: 2-4 hours for Phase 1
