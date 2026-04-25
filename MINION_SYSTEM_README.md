# Minion System Guide

This document covers the Grivience minion subsystem, including Hypixel-style fuel/upgrades plus the custom **Constellation Synergy** mechanic.

## Core Minion Features

- Placement-restricted island minions with island slot limits.
- Offline catch-up processing and action-tick simulation.
- Per-minion storage cap and collection actions.
- Minion GUI with:
  - Fuel slot
  - Upgrade Slot 1
  - Upgrade Slot 2
  - Shipping (hopper) slot
  - Stored item display
- Tier upgrades via recipe ingredient costs.

## Fuel System

Supported fuel behavior:
- Temporary fuels with duration timers.
- Permanent fuels (e.g., bucket-style) that can be removed.
- Speed and drop multipliers are applied during action processing.

## Upgrade System

Implemented upgrades:
- `auto_smelter`
- `compactor`
- `super_compactor_3000`
- `diamond_spreading`
- `corrupt_soil`
- `budget_hopper`
- `enchanted_hopper`
- `grivience_overclock_chip` (custom)
- `grivience_astral_resonator` (custom)
- `grivience_quantum_hopper` (custom)

Shipping upgrades auto-sell overflow and store coins on the minion for collection.

## Custom Mechanic: Constellation Synergy

Constellation Synergy is island-wide and based on **unique placed minion types**.

### Automatic Tiers

- Tier 0 (Dormant): `0+` unique minion types
  - Speed Multiplier: `1.00x`
  - Constellation Fragment Chance: `0%`
- Tier 1 (Resonant): `3+` unique minion types
  - Speed Multiplier: `1.10x`
  - Constellation Fragment Chance: `0%`
- Tier 2 (Harmonic): `5+` unique minion types
  - Speed Multiplier: `1.20x`
  - Constellation Fragment Chance: `5%` per generated item attempt
- Tier 3 (Astral): `8+` unique minion types
  - Speed Multiplier: `1.30x`
  - Constellation Fragment Chance: `10%` per generated item attempt

### Effects

- Speed multiplier is applied to minion action timing.
- At Tier 2+, minions can generate `constellation_fragment` as a bonus drop.
- `constellation_fragment` is consumed to craft `grivience_astral_resonator` (custom upgrade).
  - Effect: `+8%` minion speed.
  - Effect: `+5%` Constellation Fragment chance while Constellation is active.
- Minion display/GUI shows Constellation status.

### Admin Override

Admins can force a tier per island owner with `/minion constellation set`.
Override state persists in `plugins/Grivience/skyblock/minions.yml`.

## Commands

### Player

- `/minion`
- `/minion list`
- `/minion collectall`
- `/minion pickupall`

### Admin (`grivience.admin`)

- `/minion give <type> [tier] [player]`
- `/minion givefuel <id> [amount] [player]`
- `/minion giveupgrade <id> [amount] [player]`
- `/minion givefragment [amount] [player]`
- `/minion constellation status [player]`
- `/minion constellation set <tier:0-3> [player]`
- `/minion constellation clear [player]`

## Persistence

- File: `plugins/Grivience/skyblock/minions.yml`
- Stores:
  - All placed minions
  - Stored item maps
  - Fuel state
  - Upgrade slots
  - Hopper coins
  - Constellation tier overrides
