# Grappling Hook - 100% Skyblock Accurate

A complete implementation of Skyblock's Grappling Hook with 100% accuracy to the original mechanics.

## Features Implemented

### Core Mechanics (100% Accurate)
- ✅ **Right-Click to Launch** - Fires a fishing bobber that acts as the hook.
- ✅ **Velocity-Based Pull** - Pulls the player towards the hook impact location with authentic Skyblock physics.
- ✅ **Vertical Boost** - Includes the slight upward boost (+0.3 Y) upon pull for better maneuverability.
- ✅ **2-Second Cooldown** - Hardcoded 2-second cooldown between successful pulls, with accurate error messages.
- ✅ **Fall Damage Negation** - Resetting fall distance upon a successful grapple pull.
- ✅ **Infinite Durability** - Item is set to Unbreakable and does not consume durability.
- ✅ **Sneak to Retract** - Pressing SHIFT while the hook is in flight will instantly retract it without pulling.
- ✅ **Right-Click to Cancel** - Right-clicking again while a hook is active will retract it (allowing for quick resets).

### Technical Excellence
- ✅ **PDC Identification** - Uses PersistentDataContainer (`grappling_hook_id`) for reliable item detection.
- ✅ **Metadata Tracking** - Hooks are tagged with metadata to prevent regular fishing rods from acting as grappling hooks.
- ✅ **Particle Trails** - Accurate CRIT and CLOUD particle trails during flight and at impact.
- ✅ **Authentic Sounds** - Launch, pull, and retract sounds match the original experience.

## Usage

### Obtaining the Item
Use the custom give command to obtain the Grappling Hook:
```bash
/giveitem <player> grappling_hook
```

### Controls
- **Right-Click**: Launch hook or pull yourself to the target.
- **Sneak (Shift)**: Retract hook while in flight.
- **Right-Click (In Flight)**: Retract hook while in flight.

## Configuration

The system can be toggled in `config.yml`:
```yaml
grappling-hook:
  enabled: true
  particle-effects: true
  sound-effects: true
```

## Physics Details
- **Launch Velocity**: 2.0
- **Pull Force**: 0.8
- **Vertical Boost**: +0.3
- **Cooldown**: 2000ms
