# Global Event System

The Global Event System in Grivience allows administrators to manage server-wide events and XP boosts.

## Commands

### /globalevent

The primary command for managing global events.

*   **Permission:** `grivience.admin`
*   **Aliases:** `/gevent`, `/global`

#### Subcommands

*   **`/globalevent startboost <multiplier> <duration>`**
    *   Starts a server-wide XP boost.
    *   `multiplier`: The multiplier to apply (e.g., `1.5` for +50% XP).
    *   `duration`: The duration of the boost in minutes.
    *   Example: `/globalevent startboost 2.0 60` (Double XP for 1 hour)

*   **`/globalevent stopboost`**
    *   Immediately stops any active global XP boost.

*   **`/globalevent status`**
    *   Displays all currently active global events, including Mining Events and Global XP Boosts.
    *   Shows the remaining time for the XP boost if active.

## Integrated Events

The Global Event System also tracks events from other managers:

### Mining Events
Managed via `/mineevent`, these events are also listed in the global `/globalevent status` command.
*   **King's Inspection:** Increased Mining XP and Rare Drop Chance.
    *   *New:* Admin can now set the King's Inspection NPC spawn location by standing at the desired spot and using `/mineevent setnpc`.
*   **Deep Core Breach:** Reinforced ores and stronger mobs.
*   **Grand Extraction:** Competitive mining leaderboard event.

## XP Multipliers

When a Global XP Boost is active, it applies to ALL XP gained through the Skyblock Leveling system, including:
*   Combat
*   Mining
*   Farming
*   Foraging
*   Fishing
*   Dungeons
*   Quests
*   Bestiary

This multiplier stacks with individual player boosts (like those from the Welcome Event) multiplicatively.
