# Storage System README

## Overview

The **Storage System** is a comprehensive Skyblock-inspired storage management system for the Grivience plugin. It provides multiple storage types with upgradeable capacity, persistent data storage, and a user-friendly GUI interface.

## Features

### Storage Types

The system includes **7 unique storage types**, each with distinct characteristics:

1. **Personal Storage** - Basic backpack storage
   - Starting capacity: 27 slots
   - Maximum capacity: 54 slots
   - Permission: `storage.personal`

2. **Vault Storage** - Secure bank-like storage accessible from vault terminals
   - Starting capacity: 27 slots
   - Maximum capacity: 162 slots (6 pages)
   - Permission: `storage.vault`

3. **Ender Storage** - Portable storage linked to player's ender chest
   - Starting capacity: 27 slots
   - Maximum capacity: 54 slots
   - Permission: `storage.ender`

4. **Backpack Storage** - Portable shulker-style storage
   - Starting capacity: 9 slots
   - Maximum capacity: 45 slots
   - Permission: `storage.backpack`

5. **Warehouse Storage** - Large-scale bulk item storage
   - Starting capacity: 54 slots
   - Maximum capacity: 540 slots (20 pages)
   - Permission: `storage.warehouse`

6. **Accessory Bag** - Specialized storage for accessories
   - Starting capacity: 9 slots
   - Maximum capacity: 72 slots
   - Permission: `storage.accessory`

7. **Potion Bag** - Specialized storage for potions
   - Starting capacity: 18 slots
   - Maximum capacity: 54 slots
   - Permission: `storage.potion`

### Core Features

- **Upgrade System** - Expand storage capacity with tiered upgrades
- **Persistent Storage** - Items are saved automatically and persist across server restarts
- **Storage Locking** - Lock storage to prevent access
- **Custom Naming** - Rename storage for easy identification
- **Auto-Save** - Automatic data persistence every 30 seconds
- **Leaderboards** - Track top players by items stored
- **Usage Statistics** - Monitor storage capacity and usage percentage
- **Permission-Based Access** - Control which storage types players can access
- **Skyblock Menu Integration** - Accessible from the Skyblock Menu (Storage/Bags) from anywhere, including while visiting other islands (personal storage; island chests/containers remain protected)

## Commands

### Player Commands

| Command | Description | Aliases |
|---------|-------------|---------|
| `/storage` | Open the main storage menu | `/storages` |
| `/storage open <type>` | Open a specific storage type | - |
| `/storage upgrade <type>` | Upgrade storage capacity | - |
| `/storage status` | View storage status summary | - |
| `/storage rename <type> <name>` | Rename a storage type | - |
| `/storage lock <type>` | Lock a storage type | - |
| `/storage unlock <type>` | Unlock a storage type | - |
| `/storage top` | View storage leaderboard | - |
| `/storage rank` | View your storage rank | - |
| `/storage help` | Show help information | - |

### Storage Type Identifiers

When using commands, use these identifiers:
- `personal` - Personal Storage
- `vault` - Vault Storage
- `ender` - Ender Storage
- `backpack` - Backpack Storage
- `warehouse` - Warehouse Storage
- `accessory_bag` - Accessory Bag
- `potion_bag` - Potion Bag

## GUI Interface

### Main Storage Menu

Accessed via `/storage`, the main menu displays:
- **Storage Type Icons** - Click to open specific storage
- **Shift+Click** - Open upgrade menu for that storage type
- **Status Summary** - Shows total items, capacity, and rank
- **Close Button** - Exit the menu

### Storage Inventory

When opening a specific storage:
- Displays the storage's current capacity
- Items are automatically saved when closed
- Supports all Minecraft items and custom items
- Respects storage capacity limits

### Upgrade Menu

Shows:
- Current tier and slot count
- Next upgrade details
- Upgrade cost
- Upgrade button (if requirements are met)

## Configuration

### Main Config (`config.yml`)

```yaml
storage:
  # Enable the storage system
  enabled: true
  
  # Auto-save settings
  auto-save:
    enabled: true
    interval-ticks: 600  # 30 seconds
  
  # Storage locking feature
  allow-locking: true
  
  # Custom naming of storage
  allow-naming: true
  
  # Usage warning threshold (percentage)
  usage-warning-threshold: 90
  
  # Enable storage leaderboard
  enable-leaderboard: true
  
  # Enable upgrade notifications
  enable-upgrade-notifications: true
  
  # Permission settings
  permissions:
    default:
      - storage.personal
      - storage.ender
    vip:
      - storage.vault
      - storage.backpack
    mvp:
      - storage.warehouse
```

### Upgrade Configuration (`storage_upgrades.yml`)

Each storage type has configurable upgrade tiers:

```yaml
upgrades:
  personal:
    enabled: true
    tiers:
      tier-1:
        slots: 27
        cost: 0
        name: "&7Base Storage"
        commands: []
      tier-2:
        slots: 36
        cost: 5000
        name: "&aExpanded Storage I"
        commands:
          - "eco give {player} 1000"
```

**Upgrade Fields:**
- `slots` - Number of slots at this tier
- `cost` - Cost to upgrade (requires Vault economy)
- `name` - Display name for the tier
- `commands` - Commands to execute on upgrade (supports `{player}` placeholder)

## Data Persistence

### Storage Data

- **File**: `plugins/Grivience/storage_data.yml`
- **Format**: YAML
- **Auto-Save**: Every 30 seconds (configurable)
- **On Disconnect**: Player data is saved immediately

### Data Structure

Each player's storage data includes:
- Storage type
- Current slot count
- Upgrade tier
- Custom name
- Locked state
- Last accessed timestamp
- Item contents

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `storage.personal` | Access Personal Storage | All players |
| `storage.vault` | Access Vault Storage | VIP+ |
| `storage.ender` | Access Ender Storage | All players |
| `storage.backpack` | Access Backpack Storage | VIP+ |
| `storage.warehouse` | Access Warehouse Storage | MVP+ |
| `storage.accessory` | Access Accessory Bag | All players |
| `storage.potion` | Access Potion Bag | All players |
| `storage.personal.upgrade` | Upgrade Personal Storage | All players |
| `storage.vault.upgrade` | Upgrade Vault Storage | VIP+ |
| `storage.ender.upgrade` | Upgrade Ender Storage | All players |
| `storage.backpack.upgrade` | Upgrade Backpack Storage | VIP+ |
| `storage.warehouse.upgrade` | Upgrade Warehouse Storage | MVP+ |
| `storage.accessory.upgrade` | Upgrade Accessory Bag | All players |
| `storage.potion.upgrade` | Upgrade Potion Bag | All players |
| `grivience.storage.admin` | Admin storage commands | OP |
| `grivience.storage.admin.other` | Manage other players' storage | OP |

## Economy Integration

The storage system integrates with **Vault** for upgrade costs:

- Requires Vault and an economy plugin (e.g., EssentialsX Economy)
- Upgrade costs are deducted automatically
- Free upgrades are supported (cost: 0)
- Balance checks prevent upgrades without sufficient funds

## Developer API

### Accessing StorageManager

```java
StorageManager storageManager = GriviencePlugin.getInstance().getStorageManager();
```

### Getting Player Storage

```java
StorageProfile profile = storageManager.getStorage(player, StorageType.PERSONAL);
```

### Upgrading Storage

```java
boolean success = storageManager.upgradeStorage(player, StorageType.VAULT);
```

### Storage Profile Methods

```java
// Get current slots
int slots = profile.getCurrentSlots();

// Get upgrade tier
int tier = profile.getUpgradeTier();

// Get total items
int items = profile.getTotalItems();

// Check if locked
boolean locked = profile.isLocked();

// Set custom name
profile.setCustomName("My Storage");
```

## Performance Considerations

- **Auto-Save Interval**: Default 30 seconds (configurable)
- **Data Format**: Efficient YAML storage
- **Memory Usage**: Minimal - data loaded per-player on demand
- **Thread Safety**: Concurrent storage access supported

## Troubleshooting

### Storage not opening
- Check permissions for the storage type
- Ensure the storage system is enabled in config
- Verify no console errors on plugin load

### Upgrades not working
- Ensure Vault economy is set up correctly
- Check player has sufficient funds
- Verify upgrade permissions
- Check upgrade configuration in `storage_upgrades.yml`

### Items not saving
- Ensure auto-save is enabled
- Check file permissions for `storage_data.yml`
- Verify no errors in console during save operations

### Economy not working
- Ensure Vault is installed
- Check economy plugin is linked to Vault
- Verify economy provider is available

## Future Enhancements

Potential features for future versions:
- Storage sharing between party members
- Storage access logs
- Item search within storage
- Storage presets/loadouts
- Remote storage access items
- Storage rental system
- Auction house integration

## Support

For issues or suggestions:
1. Check the configuration files
2. Review console logs for errors
3. Verify permissions are set correctly
4. Contact server administrators

---

**Version**: 1.0.0  
**Author**: Grivience Development Team  
**License**: Proprietary

