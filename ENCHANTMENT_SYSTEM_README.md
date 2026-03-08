# Skyblock Enchantment System

A **100% Skyblock-accurate** enchantment system for the Grivience plugin, featuring the exact GUI layouts, mechanics, and enchantments from Skyblock.

---

## Features

### ✨ Enchantment Table GUI (`/et`, `/enchant`)
- **Skyblock-accurate layout** with purple stained glass theme
- **Item preview** showing held weapon/tool
- **Categorized enchantments** displayed in a grid format
- **Page navigation** for browsing all available enchantments
- **Level selection** view with XP cost display
- **Real-time level checking** showing current vs target enchant levels
- **Conflict detection** warning for incompatible enchantments

### 🔨 Anvil GUI (`/av`, `/anvil`)
- **Skyblock-accurate combining interface**
- **Enchanted book combining** with 25% cost discount
- **Item-to-item combining** for merging enchantments
- **Conflict prevention** - incompatible enchantments won't combine
- **Level-based cost system** matching Skyblock mechanics
- **Visual cost display** showing required XP levels
- **Result preview** before committing

### 📚 100+ Enchantments
All enchantments from Skyblock, organized by rarity:

#### **Common Enchantments**
- Sharpness, Protection, Power, Efficiency, Unbreaking, and more

#### **Uncommon Enchantments**
- Smite, Bane of Arthropods, Fire Protection, Luck of the Sea

#### **Rare Enchantments**
- Cleave, First Strike, Execute, Lifesteal, Thunderbolt, Fortune

#### **Epic Enchantments**
- Triple Strike, Prosecute, Titan Killer, Drain, Thunderlord

#### **Legendary Enchantments**
- Vampirism, Mending, Ultimate Wise, Soul Eater, Fatal Tempo

#### **Mythic Enchantments**
- Infinite Quiver, Curse of Vanishing

#### **Ultimate Enchantments** (One per item)
- Ultimate Wise, Soul Eater, One For All, Fatal Tempo, Inferno, Rend, Swarm, Duplex, Combo, Chimera, Last Stand, Legion, Refrigerate, Mana Vampire, Hardened Mana, Strong Mana, Ferocious Mana

#### **Dungeon Enchantments** (Dungeon-only)
- Lethality, Overload, Hecatomb, Rejuvenate, Feather Falling (VI-X), Infinite Quiver (VII-X)

---

## Commands

| Command | Aliases | Description |
|---------|---------|-------------|
| `/enchant` | `/et`, `/enchantmenttable` | Open the enchantment table GUI |
| `/anvil` | `/av` | Open the anvil combining GUI |
| `/enchantinfo <id>` | `/ei` | View detailed information about an enchantment |
| `/enchantlist [page]` | `/el` | List all available enchantments |

---

## Enchantment Types

### Normal Enchantments
- Obtainable through the Enchantment Table
- Can be applied using XP levels
- Multiple can be applied to a single item

### Ultimate Enchantments
- **Only 1 per item** (applying another overwrites)
- More powerful than normal enchantments
- Applied through the Anvil using books
- Examples: Ultimate Wise, Soul Eater, One For All

### Dungeon Enchantments
- Only obtainable from **Dungeon Loot Chests**
- Some have higher max levels than normal versions
- Examples: Lethality VI, Overload V, Rejuvenate V

---

## Enchantment Levels

Different enchantments have different maximum levels:

| Max Level | Examples |
|-----------|----------|
| **I** | Silk Touch, Smelting Touch, Aqua Affinity, Delicate |
| **II-III** | Tabasco, Vicious, Cubism |
| **IV-V** | Most enchantments (Sharpness, Protection, Power) |
| **VI-VII** | Execute, Thunderlord, Growth, Protection (upgraded) |
| **X** | Champion, Compact, Cultivating, Feather Falling (dungeon) |

---

## XP Cost System

### Enchantment Table Costs
- Each enchantment has a **base XP cost**
- Cost scales with level: `baseCost × level`
- Typical costs range from **10-50 levels** per enchantment

### Anvil Combining Costs
- **Enchanted Books**: 25% discount on total cost
- **Item-to-Item**: Full cost with no discount
- Cost = Sum of all enchantment XP costs

---

## Enchantment Conflicts

Certain enchantments **cannot coexist** on the same item:

### Weapon Conflicts
- **Sharpness** ↔ Smite ↔ Bane of Arthropods ↔ Cleave
- **First Strike** ↔ Triple-Strike
- **Thunderbolt** ↔ Thunderlord
- **Giant Killer** ↔ Titan Killer
- **Execute** ↔ Prosecute
- **Lifesteal** ↔ Drain ↔ Mana Steal ↔ Vampirism

### Armor Conflicts
- **Protection** ↔ Blast Protection ↔ Fire Protection ↔ Projectile Protection
- **Thorns** ↔ Reflection (chestplate only)
- **Rejuvenate** ↔ Respite
- **Big Brain** ↔ Small Brain (helmet only)
- **Mana Vampire** ↔ Hardened Mana ↔ Strong Mana ↔ Ferocious Mana

### Tool Conflicts
- **Fortune** ↔ Silk Touch ↔ Smelting Touch

### Bow Conflicts
- **Power** ↔ Snipe
- **Riptide** ↔ Loyalty (trident)

---

## GUI Layouts

### Enchantment Table (54 slots)
```
Slot 10: Item to enchant (left side)
Slots 14-47: Enchantment options (grid)
Slot 4: Player info / enchant guide
Slot 13: Enchanting guide book
Slot 48: Previous page arrow
Slot 49: Page info display
Slot 50: Next page arrow
Slot 52: Close button (barrier)
```

### Anvil (54 slots)
```
Slot 10: Target item (left)
Slot 14: Ingredient/book (right)
Slot 16: Result output
Slot 4: Anvil guide info
Slot 49: Cost display
Slot 50: Close button
Slot 52: Take result button
```

---

## Integration

### With Existing Systems
- **Custom Weapons**: All custom dungeon weapons support enchantments
- **Reforge System**: Enchantments work alongside reforges
- **Stats System**: Enchantments integrate with strength/crit/mana stats
- **Collections**: Enchantment books can be collection rewards
- **Bazaar**: Enchantment books tradable on bazaar

### With Commands
- Works with `/reforge` for complete gear customization
- Compatible with `/blacksmith` for additional upgrades
- Integrates with `/wardrobe` for armor sets

---

## Configuration

The enchantment system is **fully code-based** for maximum accuracy, but integrates with existing config systems:

### Plugin.yml Commands
```yaml
enchant:
  description: Open the Skyblock enchantment table menu
  aliases: [et, enchantmenttable]
anvil:
  description: Open the Skyblock anvil combining menu
  aliases: [av]
enchantinfo:
  description: View information about a specific enchantment
  aliases: [ei]
enchantlist:
  description: List all available enchantments
  aliases: [el]
```

---

## API Usage

### Get Enchantment by ID
```java
SkyblockEnchantment enchantment = EnchantmentRegistry.get("sharpness");
```

### Get All Enchantments for Item
```java
List<SkyblockEnchantment> available = EnchantmentRegistry.getForItem(itemStack);
```

### Get Ultimate Enchantments
```java
List<SkyblockEnchantment> ultimates = EnchantmentRegistry.getUltimateEnchantments();
```

### Get Dungeon Enchantments
```java
List<SkyblockEnchantment> dungeon = EnchantmentRegistry.getDungeonEnchantments();
```

### Check Conflicts
```java
boolean conflicts = enchantment1.conflictsWith(enchantment2);
```

### Get XP Cost
```java
int cost = enchantment.getXpCost(level);
```

---

## Enchantment Categories

Enchantments are categorized by what items they can be applied to:

- **SWORD** - Swords only
- **BOW** - Bows and crossbows
- **AXE** - Axes
- **PICKAXE** - Pickaxes
- **DRILL** - Custom drills
- **HOE** - Hoes
- **SHEARS** - Shears
- **SHOVEL** - Shovels
- **FISHING_ROD** - Fishing rods
- **ARMOR** - Any armor piece
- **HELMET** - Helmets only
- **CHESTPLATE** - Chestplates only
- **LEGGINGS** - Leggings only
- **BOOTS** - Boots only
- **TOOL** - Any tool
- **WEAPON** - Any weapon
- **UNIVERSAL** - Any item

---

## Rarity System

Each enchantment has a rarity that affects its display color:

| Rarity | Color | Examples |
|--------|-------|----------|
| **Common** | White | Sharpness, Protection, Efficiency |
| **Uncommon** | Green | Smite, Fire Protection, Lure |
| **Rare** | Blue | Cleave, First Strike, Fortune |
| **Epic** | Dark Purple | Triple Strike, Drain, Thunderlord |
| **Legendary** | Gold | Vampirism, Mending, Ultimate Wise |
| **Mythic** | Light Purple | Infinite Quiver, Curse of Vanishing |

---

## Technical Details

### Files Created
- `EnchantmentType.java` - Rarity/type enum
- `EnchantmentCategory.java` - Item category enum
- `SkyblockEnchantment.java` - Main enchantment data class
- `EnchantmentRegistry.java` - All enchantment definitions
- `EnchantmentTableGui.java` - Enchantment table GUI
- `SkyblockAnvilGui.java` - Anvil combining GUI
- `EnchantmentCommand.java` - Command handlers
- `EnchantmentManager.java` - System manager

### Dependencies
- Paper API 1.21.x
- No external libraries required

### Performance
- Lazy initialization of enchantment registry
- Efficient lookup by ID
- Minimal memory footprint
- No tick-based updates

---

## Future Enhancements

Potential additions for even more accuracy:

- [ ] **Enchanting Skill** - Level-based unlock requirements
- [ ] **Bookshelf Power** - Surrounding bookshelves affect available levels
- [ ] **Experiments Table** - Daily experiments for rare books
- [ ] **Dark Auction** - NPC auction for high-tier books
- [ ] **Enchantment Upgrading** - Champion/Compact style progression
- [ ] **Reforges** - Skyblock-style random stat rerolling
- [ ] **Gemstones** - Socket-based enchantment upgrades
- [ ] **Essence** - Dungeon currency for enchantment upgrades

---

## Credits

- **Skyblock** - Original enchantment system design
- **Grivience Plugin** - Integration and customization
- **Enchantment Data** - Based on Skyblock Wiki

---

## Support

For issues or suggestions, please contact the development team or submit a bug report.

**Version**: 1.0 (Skyblock Accurate)
**Last Updated**: 2026-02-24

