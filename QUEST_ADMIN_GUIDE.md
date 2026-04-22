# 📜 Grivience Quest Creation Guide

This guide explains how to create, edit, and manage quests in the Grivience Skyblock system. Quests can be created using the **In-Game Visual Editor** or by manually editing the `quests.yml` file.

---

## 🛠️ The Visual Editor (Recommended)
Admins with the `grivience.admin` permission can manage quests entirely in-game.

### 1. Opening the Editor
Type `/quest gui` or `/quest editor` to open the Admin Quest List.
- **Left-Click** any quest to edit it.
- **Emerald Icon**: Create a new quest.
- **Hopper Icon**: Reload all quest files from disk.

### 2. Editing a Quest
Once inside the Quest Editor, you can modify:
- **Display Name**: The name shown in the GUI and scoreboard (supports `&` colors).
- **Description**: The story or flavor text shown in the lore.
- **Objectives**: Add or remove requirements (Kills, Items, Levels, etc.).
- **Prerequisites**: Link quests together to form a "Questline".
- **Rewards**: Add console commands to run when the quest is finished (e.g., `eco give {player} 500`).
- **Toggles**: Enable/Disable quests or make them Repeatable.

---

## 📝 Manual Configuration (`quests.yml`)
For complex setups, editing `src/main/resources/quests.yml` is often faster.

### Quest Structure
```yaml
quests:
  my_custom_quest:              # Unique ID (No spaces)
    display-name: "&6Cool Quest" # Name in GUI
    description: "Do things!"    # Lore text
    prerequisites:               # MUST complete these first
      - village_greeting
    objectives:
      obj0:
        type: KILL_MOBS         # Objective Type
        target: ZOMBIE           # EntityType, Material, or SkillName
        amount: 10              # Required count
        description: "Slay 10 Zombies"
      obj1:
        type: COLLECT_ITEMS
        target: DIAMOND
        amount: 1
        description: "Find a Diamond"
    rewards:
      - "eco give {player} 1000" # Use {player} placeholder
    repeatable: false            # Can they do it again?
    enabled: true                # Is it active?
```

### Supported Objective Types
| Type | Target | Logic |
| :--- | :--- | :--- |
| `TALK_TO_NPC` | NPC ID | Complete by talking to a ZNPC with this ID. |
| `KILL_MOBS` | EntityType or ID | Supports vanilla (e.g., `ZOMBIE`) and custom mobs (e.g., `shadow_stalker`). |
| `COLLECT_ITEMS`| Material or ID | Supports vanilla (e.g., `IRON_INGOT`) and custom items (e.g., `SUMMONING_EYE`). |
| `REACH_LEVEL` | Skill Name | Complete by reaching a specific level in a Skill (e.g., `MINING`). |

---

## 🐲 Custom Mobs & Items
The quest system is fully integrated with Grivience's custom content. When setting a `target` for an objective:

### Custom Mobs
Use the internal ID of the mob. Examples:
- `shadow_stalker`
- `crimson_warden`
- `void_wraith`

### Custom Items
Use the internal ID of the custom item. Examples:
- `SUMMONING_EYE`
- `KUNZITE`
- `ENCHANTED_REDSTONE` (Used for Personal Compactors)
- `WARDENS_CLEAVER`

*Note: For vanilla items and mobs, use the standard Bukkit names (e.g., `COAL`, `DIAMOND`, `COW`).*

---

## 💎 Collection Rewards
Players can unlock special recipes by reaching milestones in their **Collections** (`/collection`).
- **Redstone Collection**: Unlocks recipes for **Personal Compactors** (Tiers 4000-7000).
- **Mining Collection**: Unlocks Drills and Engines.

---

## 🔗 Creating Questlines (Prerequisites)
To make a quest unlock only after another is finished, add the **ID** of the first quest to the `prerequisites` list of the second.

**Example: The Ironcrest Drill Line**
1. `ironcrest_part1_arrival`: Started automatically when entering the Minehub.
2. `ironcrest_part2_coal`: Requires Part 1.
3. `ironcrest_part3_iron`: Requires Part 2.
4. `ironcrest_part4_fragments`: Requires Part 3.
5. `ironcrest_part5_assembly`: Requires Part 4. Final reward: **Ironcrest Drill**.

---

## 🗺️ Area Discovery & Elevators
The progression system now tracks when players "discover" new mining layers or End Mines zones.

### Discovery Logic
- Discovery is **automatic**. Simply entering a layer (e.g., *Lapis Quarry*) or an End Mines zone (e.g., *Crystal Cavern*) for the first time will trigger a discovery notification.
- Discovery is stored per **Profile**.

### Elevator Integration
Admins can lock elevator floors based on these discoveries:
- **Command**: `/elevator addfloor <id> <name> <icon> [required_layer]`
- **Example**: `/elevator addfloor hub_elevator "Deep Mines" DIAMOND_PICKAXE Diamond Caverns`
- Players will see the floor as **Locked** until they physically travel to the *Diamond Caverns* at least once.
- To update an existing floor: `/elevator setrequirement <id> <index> <layer_name|none>`.

---

## ⚡ Automatic Triggers
Some quests are programmed to start automatically based on player actions:
- **World Change**: Quests can start when a player enters a specific world (e.g., entering the **Minehub** starts the Ironcrest Arrival quest).
- **Joining**: Quests can be checked and started when a player first joins the server.
- **Level Up**: Reaching certain skill levels (e.g., Mining 20) can trigger quest completion or starts.

---

## 🏆 Tips for Great Quests
1. **Color Codes**: Use `&a`, `&b`, etc., in `display-name` to make them pop in the `/quest` log.
2. **Scoreboard**: The first objective in the list is what usually shows up on the player's sidebar. Make it descriptive!
3. **Legacy NPCs**: If you use `target-npc` instead of the `objectives` system, the quest will behave as a simple "Talk to NPC" quest (Legacy Mode).
4. **Reloading**: Always run `/quest gui` and click the **Reload** button after editing the YAML file to apply changes without restarting the server.

---

## 🔑 Permissions
- `grivience.admin`: Access to `/quest gui` and all editor functions.
- (Default): Access to `/quest` (Player Menu).
