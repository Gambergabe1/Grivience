# Zone Editor System

A comprehensive zone/area management system for the Grivience plugin that allows administrators to define custom areas and display them on the player scoreboard.

## Features

- ✅ **Zone Creation & Management** - Create, edit, and remove zone areas
- ✅ **Cuboid Zone Bounds** - Define zones using two corner positions (pos1, pos2)
- ✅ **Scoreboard Integration** - Display current zone name on custom scoreboard
- ✅ **Priority System** - Higher priority zones override lower ones
- ✅ **Color-Coded Zones** - Each zone can have a custom color display
- ✅ **Zone Notifications** - Players notified when entering/leaving zones
- ✅ **YAML Persistence** - Zones saved to `plugins/Grivience/zones.yml`
- ✅ **World Support** - Zones can be defined in any world
- ✅ **Selection Wand** - Use wooden axe for easy position selection

## Commands

All zone commands require the `grivience.admin` permission.

### Basic Commands

| Command | Description |
|---------|-------------|
| `/zone help` | Show help information |
| `/zone create <id> [name] [displayName]` | Create a new zone |
| `/zone delete <id>` | Delete a zone |
| `/zone select <id>` | Select a zone for editing |
| `/zone list` | List all configured zones |
| `/zone info [id]` | View zone information |
| `/zone reload` | Reload zones from config |
| `/zone wand` | Get zone selection wand (wooden axe) |

### Zone Editing Commands

| Command | Description |
|---------|-------------|
| `/zone setpos1` | Set position 1 (current location) |
| `/zone setpos2` | Set position 2 (current location) |
| `/zone setname <name>` | Set zone internal name |
| `/zone setdisplayname <displayName>` | Set display name (shown on scoreboard) |
| `/zone setcolor <color>` | Set display color |
| `/zone setpriority <number>` | Set priority (higher = override others) |
| `/zone setenabled <true|false>` | Enable/disable zone |
| `/zone setdesc <description>` | Set zone description |

## Usage Guide

### Creating a Zone

1. **Create the zone:**
   ```
   /zone create spawn SpawnArea Spawn
   ```

2. **Select the zone for editing:**
   ```
   /zone select spawn
   ```

3. **Set the bounds:**
   - Go to one corner of the area
   - `/zone setpos1`
   - Go to the opposite corner
   - `/zone setpos2`

4. **Customize the display:**
   ```
   /zone setdisplayname spawn §6§lSpawn Zone
   /zone setcolor spawn GOLD
   /zone setpriority spawn 10
   ```

### Using the Selection Wand

1. Get the wand:
   ```
   /zone wand
   ```

2. Select a zone first:
   ```
   /zone select <zone_id>
   ```

3. Left-click with wand to set pos1
4. Right-click with wand to set pos2

### Scoreboard Integration

Zones automatically appear on the custom scoreboard when:
- Player enters a zone area
- Zone has higher priority than other location types

**Priority Order:**
1. Dungeon sessions (highest)
2. Custom zones
3. Player islands
4. World names (default)

## Configuration

Edit `plugins/Grivience/config.yml`:

```yaml
zones:
  # Enable/disable zone system
  enabled: true
  
  # Default zone name when not in any zone
  default-zone-name: "Overworld"
  
  # Show zone name in scoreboard
  show-in-scoreboard: true
  
  # Show zone change message on join
  show-on-join: true
  
  # Update zone display when moving between zones
  update-on-change: true
  
  # Scoreboard update interval in ticks (20 = 1 second)
  scoreboard-update-interval: 20
  
  # Priority order for scoreboard display
  priority-order:
    - dungeon
    - zone
    - island
    - world
```

## Zone Properties

### Colors
Available colors for zone display:
- `BLACK`, `DARK_BLUE`, `DARK_GREEN`, `DARK_AQUA`
- `DARK_RED`, `DARK_PURPLE`, `GOLD`, `GRAY`
- `DARK_GRAY`, `BLUE`, `GREEN`, `AQUA`
- `RED`, `LIGHT_PURPLE`, `YELLOW`, `WHITE`

### Priority
- Higher numbers override lower numbers
- Default priority: 0
- Example: Priority 10 zone overrides priority 5 zone in overlapping areas

### Display Name Formatting
- Supports color codes (`&` prefix)
- Supports formatting codes (`&l` bold, `&o` italic, etc.)
- Example: `&6&lSpawn Zone` = Gold Bold "Spawn Zone"

## Data Storage

Zones are stored in `plugins/Grivience/zones.yml`:

```yaml
zones:
  spawn:
    name: SpawnArea
    world: world
    display-name: Spawn
    color: GOLD
    priority: 10
    enabled: true
    description: "Main spawn area"
    pos1:
      x: 0.0
      y: 64.0
      z: 0.0
    pos2:
      x: 100.0
      y: 100.0
      z: 100.0
```

## Examples

### Example 1: Hub Zone
```bash
# Create hub zone
/zone create hub Hub §b§lHub World
/zone select hub
/zone setpos1  # Stand at corner 1
/zone setpos2  # Stand at corner 2
/zone setcolor AQUA
/zone setpriority 50
```

### Example 2: Arena Zone
```bash
# Create arena zone
/zone create arena Arena §c§lBattle Arena
/zone select arena
/zone setpos1
/zone setpos2
/zone setcolor RED
/zone setpriority 20
/zone setdesc "PvP combat zone"
```

### Example 3: Farming District
```bash
# Create farming zone
/zone create farm Farming District §a§lFarm Zone
/zone select farm
/zone setpos1
/zone setpos2
/zone setcolor GREEN
/zone setpriority 10
```

## API Usage

For developers wanting to integrate zone system:

```java
// Get ZoneManager
ZoneManager zoneManager = plugin.getZoneManager();

// Get zone at location
Zone zone = zoneManager.getZoneAt(player.getLocation());

// Get zone name for display
String zoneName = zoneManager.getZoneName(player);

// Get all zones
Collection<Zone> zones = zoneManager.getAllZones();

// Create zone programmatically
Zone newZone = zoneManager.createZone("id", "name", "displayName");
newZone.setPos1(location1);
newZone.setPos2(location2);
newZone.setColor(ChatColor.BLUE);
newZone.setPriority(100);
```

## Troubleshooting

### Zone not showing on scoreboard
- Check if zone is enabled: `/zone info <id>`
- Verify bounds are set correctly
- Check priority (may be overridden by higher priority zone)
- Ensure zone system is enabled in config

### Commands not working
- Verify you have `grivience.admin` permission
- Check command spelling (use `/zone help` for list)

### Zone bounds incorrect
- Use `/zone info` to check current positions
- Re-set positions with `/zone setpos1` and `/zone setpos2`
- Ensure both positions are in the same world

## Integration

The zone system integrates with:
- **SkyblockScoreboardManager** - Displays zone name in sidebar
- **DungeonManager** - Dungeons override zone display
- **IslandManager** - Islands show as "Your Island" or "Private Island"

## Future Enhancements

Planned features:
- [ ] Zone GUI editor
- [ ] Zone teleportation commands
- [ ] Zone-specific permissions
- [ ] Zone entry/exit events API
- [ ] Zone-based restrictions (PvP, building, etc.)
- [ ] Mini-map zone overlay
- [ ] Zone warping system

## Credits

Zone Editor system developed for Grivience plugin.
Inspired by WorldGuard regions and Skyblock zone display.

