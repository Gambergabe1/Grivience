# Grivience Plugin

Japanese-themed dungeon + Skyblock plugin for Paper `1.21.x` with high-accuracy RPG mechanics.

## 🌟 Current Feature Set

### 🗡️ Dungeon Core
- **Party-based floor runs** with config-driven floor data (`F1`-`F7`).
- **Expansive Procedural Generation**: Rooms are 14 blocks high and corridors 11 blocks wide for high-fidelity combat.
- **Dungeon Classes**: Select your role via `/class` (Healer, Mage, Berserk, Archer, Tank).
- **Secrets & Blessings**: 1-3 hidden chests per room and powerful stat altars.
- **Hypixel-Style Bosses**: Phased mechanics for Bonzo, Scarf, Professor, Thorn, Livid, Sadan, and Necron.

### 📜 Quest & Progression System
- **Multi-Objective Quests**: Progress through tasks like killing mobs, collecting items, reaching skill levels, or talking to NPCs.
- **Branching Questlines**: Advanced prerequisite system allows for deep, narrative-driven progression (e.g., the 5-part **Ironcrest Drill** line).
- **Tabbed Quest Log**: Integrated into the Skyblock Menu (`/skyblock`), allowing players to filter by Active, Available, and Completed quests.
- **Visual Admin Editor**: Admins can manage the entire quest system in-game via `/quest gui`.
- **Live Scoreboard Tracking**: Your current quest objective is automatically pinned to your sidebar for real-time tracking.

### ⛏️ Mining & Breaking Power
- **Breaking Power (BP) System**: Progression-based mining. Materials like **Titanium (BP 5)** and **Obsidian Cores (BP 8)** require advanced Drills.
- **Area Discovery**: Walk into new layers (e.g., *Diamond Caverns*, *Lapis Quarry*) or End Mine zones to "discover" them and unlock rewards.
- **Drill Forge**: Advanced drills and components (Engines, Tanks) must be forged over time using Minehub and End Mines materials.
- **Custom Visuals**: High-quality 3D block models for Sapphires and other rare gemstones.

### 🏝️ Skyblock & Island Border
- **Dynamic World Borders**: Your island's physical boundary is visually represented by a `WorldBorder` that expands instantly as you upgrade your island size.
- **Personalized Borders**: The border switches and centers automatically as you travel between your island and others.
- **Skyblock Leveling**: Comprehensive progression system with guide-driven milestones.
- **Accessory Bag**: Dedicated storage for unique accessories that grant **Magical Power (MP)** and scaling stat bonuses.

### 🎒 Specialized Gear & Forging
- **Forge Exclusivity**: Elite items like the **Guardian Armor** set must be forged using rare fragments (e.g., Guardian Fragments) rather than simple crafting.
- **Personal Compactors**: Auto-compact resources as you mine. Tiers 3000-7000 are unlocked via collections and require forging for higher tiers.
- **Drop Integrity**: Powerful gear (Shogun, Dreadlord, etc.) is strictly drop-only, preserving the incentive for combat.

### 📈 Bazaar & Economy
- **Order Matching Engine**: Price-time priority matching for buy/sell orders.
- **Shopping Bag System**: Claim filled orders and coin refunds in a dedicated GUI.
- **Bank System**: Secure coin storage with interest and profile-specific purses.

---

## ⌨️ Commands

### Player Commands
- `/skyblock` (alias `/sb`) - Open the main Skyblock menu (includes Quest Log).
- `/quest` - View your active objectives and available quests.
- `/island` (alias `/is`) - Island management and upgrades.
- `/elevator` - Quick travel between discovered mining layers.
- `/accessory` - Open Accessory Bag.
- `/class` - Select your Dungeon Class.
- `/bazaar` (alias `/bz`) - Open the Bazaar matching engine.

### Admin Commands
- `/quest gui` - Open the **Visual Quest Editor** and management suite.
- `/elevator addfloor <id> <name> <icon> [req_layer]` - Configure area-locked elevators.
- `/grivience reload` - Stable, sub-millisecond system reload.
- `/dungeon star <0-5>` - Manually set item star count.

---

## 🛠️ Configuration

All systems are configurable via `plugins/Grivience/`:
- `config.yml`: Core settings, Mining layers, and Combat toggles.
- `quests.yml`: Multi-objective quest definitions.
- `dungeons.yml`: Floor, Mob, and Boss configurations.
- `collections.yml`: Progression and recipe rewards.

---

## 📦 Build

Requires Java 21 and Paper API `1.21.x`.

```bash
./gradlew clean assemble
```
