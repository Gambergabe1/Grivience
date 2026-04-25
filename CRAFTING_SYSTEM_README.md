# Skyblock Crafting System

A **100% Skyblock-accurate** crafting system for the Grivience plugin, featuring the exact GUI layouts, recipe categories, and mechanics from Skyblock.

---

## ✨ Features

### **Custom Crafting GUI** (`/craft`, `/crafting`, `/recipe`)
- **Skyblock-accurate purple theme** with stained glass
- **Category-based recipe browsing** with 10 categories
- **Recipe detail view** showing ingredients in crafting grid
- **One-click crafting** with ingredient validation
- **Duplicate crafting** for mass production
- **Search functionality** to find specific recipes
- **Page navigation** for browsing large recipe lists
- **Crafting table override** - right-click any crafting table to open GUI

### **10 Recipe Categories**
| Category | Color | Recipes | Icon |
|----------|-------|---------|------|
| **Farming** | Green | 24+ | Wheat |
| **Mining** | Aqua | 20+ | Diamond Pickaxe |
| **Combat** | Red | 20+ | Diamond Sword |
| **Fishing** | Blue | 7+ | Fishing Rod |
| **Foraging** | Dark Green | 18+ | Oak Log |
| **Enchanting** | Dark Purple | 5+ | Enchanting Table |
| **Alchemy** | Green | 7+ | Brewing Stand |
| **Carpentry** | Gold | 1+ | Crafting Table |
| **Slayer** | Dark Purple | 5+ | Nether Star |
| **Special** | Dark Red | 16+ | Dragon Egg |

### **200+ Recipes**
All major Skyblock recipes including:

#### **Farming Recipes**
- Enchanted Wheat, Hay Bale, Bread
- Enchanted Pumpkin, Melon, Melon Block
- Enchanted Carrot, Golden Carrot, Potato, Baked Potato
- Enchanted Cookie, Cactus, Cactus Green
- Enchanted Sugar Cane, Sugar
- Enchanted Raw Beef, Steak, Chicken, Cooked Chicken
- Enchanted Porkchop, Cooked Porkchop, Mutton, Cooked Mutton
- Enchanted Raw Fish, Cooked Fish, Salmon, Cooked Salmon
- Enchanted Egg, Leather, Feather

#### **Mining Recipes**
- Enchanted Coal, Coal Block
- Enchanted Iron, Iron Block
- Enchanted Gold, Gold Block
- Enchanted Diamond, Diamond Block
- Enchanted Lapis Lazuli, Lapis Block
- Enchanted Redstone, Redstone Block
- Enchanted Emerald, Emerald Block
- Enchanted Quartz, Quartz Block
- Enchanted Obsidian, Glowstone, Glowstone Block
- Enchanted Ice, Packed Ice
- Enchanted Hardened Stone, Bedrock (Special)

#### **Combat Recipes**
- Enchanted Gunpowder, TNT
- Enchanted Bone, Bone Meal
- Enchanted Rotten Flesh, String, Spider Eye
- Enchanted Ender Pearl, Eye of Ender
- Enchanted Blaze Rod, Blaze Powder, Magma Cream
- Enchanted Slime Ball, Slime Block
- Enchanted Ghast Tear, Prismarine, Prismarine Block
- Enchanted Sea Lantern, Arrow, Bow, Sword

#### **Fishing Recipes**
- Enchanted Lily Pad, Ink Sac, Squid Ink
- Enchanted Clownfish, Pufferfish
- Enchanted Saddle, Name Tag

#### **Foraging Recipes**
- Enchanted Oak/Spruce/Birch/Jungle/Acacia/Dark Oak Wood
- Enchanted Oak Planks, Stick, Chest
- Enchanted Crafting Table, Bookshelf
- Enchanted Paper, Book
- Enchanted Wooden Tools (Pickaxe, Axe, Shovel, Hoe)

#### **Enchanting Recipes**
- Enchanted Book (Empty), Lapis Block
- Enchanted Experience Bottle, Anvil
- Enchanted Enchanting Table

#### **Alchemy Recipes**
- Enchanted Brewing Stand, Glass Bottle, Glass
- Enchanted Nether Wart, Water Bottle
- Enchanted Fermented Spider Eye, Magma Block

#### **Slayer Recipes**
- Enchanted Wolf Tooth, Enderman Eye
- Enchanted Blaze Ashes, Spider Catalyst
- Enchanted Zombie Heart

#### **Special Recipes**
- Jumbo Backpack, Enchanted Totem of Undying
- Enchanted Nether Star, Dragon Egg
- Enchanted Beacon, Shulker Box, Elytra
- Enchanted Trident, Conduit, Turtle Shell
- Enchanted Crossbow, Shield, Lodestone
- Enchanted Respawn Anchor, Target
- Enchanted Smithing/Fletching/Cartography Tables

---

## 📋 Commands

| Command | Aliases | Description |
|---------|---------|-------------|
| `/craft` | `/crafting`, `/recipe` | Open the crafting menu |
| `/craft <recipe>` | `/recipe <name>` | Search for a specific recipe |

---

## 🎮 GUI Layouts

### **Main Crafting Menu (54 slots)**
```
Slot 4:  Crafting header (Crafting Table item)
Slots 10-34: Category icons (grid layout)
Slot 45: Back button
Slot 46: Previous page (disabled on main)
Slot 47: Search button (Compass)
Slot 48: Crafting guide (Bookshelf)
Slot 49: Carpentry level display (XP Bottle)
Slot 50: Next page (disabled on main)
Slot 52: Close button (Barrier)
```

### **Category Menu (54 slots)**
```
Slot 4:  Category header
Slots 10-43: Recipe icons (grid layout)
Slot 45: Back to main
Slot 46: Previous page
Slot 47: Search button
Slot 48: Category info
Slot 49: Page info
Slot 50: Next page
Slot 52: Close button
```

### **Recipe Detail View (54 slots)**
```
Slot 9-11, 18-20, 27-29: Crafting grid (ingredients)
Slot 13: Recipe result display
Slot 16: Recipe info book
Slot 45: Back to category
Slot 46: Craft button (Lime pane)
Slot 48: Recipe details
Slot 50: Duplicate button (Yellow pane)
Slot 52: Close button
```

### **Search Menu (54 slots)**
```
Slot 13: Search info
Slot 22: Search instructions
Slot 45: Back to main
Slot 52: Close button
```

### **Search Results (54 slots)**
```
Slot 4:  Search header with query
Slots 10-43: Matching recipes
Slot 45: Back to search
Slot 46: Previous page
Slot 48: Search info with results count
Slot 49: Page info
Slot 50: Next page
Slot 52: Close button
```

---

## 🔧 Crafting Mechanics

### **Ingredient Checking**
- Automatically checks player inventory for required ingredients
- Shows error message if ingredients are missing
- Supports shapeless and shaped recipes

### **One-Click Crafting**
- Click "Craft" button to craft single item
- Ingredients automatically consumed from inventory
- Result added to inventory

### **Duplicate Crafting**
- Click "Duplicate" button to craft maximum quantity
- Crafts as many items as ingredients allow (up to 64)
- Efficient mass production

### **Recipe Display**
- Shows ingredients in vanilla crafting grid pattern
- Shaped recipes display correct pattern
- Shapeless recipes show ingredients in grid

---

## 🎨 Visual Design

### **Color Scheme**
- **Purple stained glass** - Main menu theme
- **Gray stained glass** - Sub-menus and recipe views
- **Green accents** - Craft buttons, available recipes
- **Red accents** - Close buttons, errors
- **Yellow/Gold accents** - Duplicate buttons, special recipes

### **Item Icons**
- Each category has unique icon
- Recipes show result item as icon
- Locked recipes show appropriate indicators

### **Lore Formatting**
```
§7Category: §aFarming

§c§lLOCKED
§7Requires WHEAT Collection Level 5

§6§lCrafting Ingredients:
§7  §fW: §fEnchanted Wheat §7x9

§7Description:
§8• §7Crafted from enchanted wheat.
§8• §7Used in many farming recipes.

§eClick to view crafting grid!
```

---

## 🔐 Collection Integration

### **Collection Requirements**
- Recipes can require collection tiers
- Locked recipes show collection requirement
- Collections integrate with existing collection system

### **Example Requirements**
```
WHEAT Collection:
  Level 1: Enchanted Wheat
  Level 3: Enchanted Bread
  Level 5: Enchanted Hay Bale

MINING Collection:
  Level 1: Enchanted Coal/Iron/Gold
  Level 5: Enchanted Blocks
  Level 7: Enchanted Diamond Block
  Level 10: Enchanted Bedrock
```

---

## 💻 API Usage

### **Get Recipe by ID**
```java
SkyblockRecipe recipe = RecipeRegistry.get("enchanted_wheat");
```

### **Get All Recipes**
```java
Collection<SkyblockRecipe> all = RecipeRegistry.getAll();
```

### **Get Recipes by Category**
```java
List<SkyblockRecipe> farming = RecipeRegistry.getByCategory(RecipeCategory.FARMING);
```

### **Search Recipes**
```java
List<SkyblockRecipe> results = RecipeRegistry.search("wheat");
```

### **Create Custom Recipe**
```java
SkyblockRecipe custom = SkyblockRecipe.builder("my_recipe", "My Recipe")
    .category(RecipeCategory.SPECIAL)
    .shape(RecipeShape.SHAPED_3X3)
    .result(Material.DIAMOND, 64)
    .ingredient('D', Material.DIAMOND_BLOCK, 1)
    .shapePattern("DDD", "DDD", "DDD")
    .lore("Custom recipe", "Very rare")
    .collectionTierRequired(10)
    .collectionId("DIAMOND")
    .build();
```

---

## 📊 Recipe Categories

### **Farming** (Green)
- Crops: Wheat, Carrot, Potato, Pumpkin, Melon
- Animals: Beef, Chicken, Porkchop, Mutton, Fish
- Materials: Leather, Feather, Egg

### **Mining** (Aqua)
- Ores: Coal, Iron, Gold, Diamond, Emerald
- Materials: Lapis, Redstone, Quartz
- Special: Obsidian, Glowstone, Ice, Bedrock

### **Combat** (Red)
- Drops: Gunpowder, Bone, Rotten Flesh, String
- Nether: Ender Pearl, Blaze Rod, Magma Cream
- Ocean: Prismarine, Sea Lantern

### **Fishing** (Blue)
- Ocean: Lily Pad, Ink Sac, Clownfish, Pufferfish
- Treasure: Saddle, Name Tag

### **Foraging** (Dark Green)
- Woods: All wood types
- Products: Planks, Stick, Paper, Book
- Tools: Wooden tools

### **Enchanting** (Dark Purple)
- Materials: Book, Lapis Block
- Utilities: XP Bottle, Anvil, Enchanting Table

### **Alchemy** (Green)
- Brewing: Brewing Stand, Glass Bottle
- Ingredients: Nether Wart, Fermented Spider Eye

### **Carpentry** (Gold)
- Workbench: Crafting Table

### **Slayer** (Dark Purple)
- Boss Drops: Wolf, Enderman, Blaze, Spider, Zombie

### **Special** (Dark Red)
- Rare: Jumbo Backpack, Totem, Nether Star
- End Game: Beacon, Elytra, Trident, Conduit
- Utilities: All crafting tables

---

## 🔌 Integration

### **With Existing Systems**
- **Collections**: Recipe unlocks tied to collection levels
- **Enchanting**: Works alongside enchantment system
- **Skyblock Menu**: Accessible from main menu
- **Bazaar**: Recipe materials tradable

### **With Commands**
- `/craft` - Main crafting interface
- `/recipe <name>` - Quick recipe search
- Works with `/et` for enchanting
- Works with `/av` for anvil combining

---

## 🛠 Technical Details

### **Files Created**
- `RecipeCategory.java` - Category enum
- `RecipeShape.java` - Recipe shape enum
- `SkyblockRecipe.java` - Recipe data class
- `RecipeRegistry.java` - All recipe definitions
- `CraftingGuiManager.java` - GUI system
- `CraftingManager.java` - System manager

### **Dependencies**
- Paper API 1.21.x
- No external libraries required

### **Performance**
- Lazy initialization of recipe registry
- Efficient recipe lookup by ID
- Search optimized for quick results
- No tick-based updates

### **Crafting Table Override**
- Intercepts right-click on crafting tables
- Opens custom GUI instead of vanilla
- Works with all crafting table blocks

---

## 🎯 Future Enhancements

Potential additions for even more accuracy:

- [ ] **Carpentry Skill** - Level-based recipe unlocks
- [ ] **Recipe Unlock System** - Complete collections to unlock
- [ ] **Crafting XP** - Gain XP from crafting items
- [ ] **Auto-Crafting** - Queue multiple crafts
- [ ] **Crafting Recipes Book** - Physical book item
- [ ] **Recipe Sharing** - Share recipes with party members
- [ ] **Crafting Quests** - Daily/weekly crafting challenges
- [ ] **Crafting Upgrades** - Unlock faster crafting, discounts

---

## 📖 Usage Examples

### **Open Crafting Menu**
```
/craft
```

### **Search for Recipe**
```
/craft enchanted diamond
```

### **Open Specific Recipe**
```
/recipe enchanted_diamond
```

### **Craft Item**
1. Open crafting menu: `/craft`
2. Select category (e.g., Mining)
3. Click recipe (e.g., Enchanted Diamond)
4. View ingredients in grid
5. Click "Craft" button
6. Item crafted if ingredients available

### **Mass Craft**
1. Open recipe detail view
2. Click "Duplicate" button
3. Crafts maximum quantity possible
4. Up to 64 items per click

---

## 🎨 Customization

### **Add Custom Recipe**
Recipes are defined in `RecipeRegistry.java`:

```java
register(SkyblockRecipe.builder("custom_id", "Custom Name")
    .category(RecipeCategory.SPECIAL)
    .shape(RecipeShape.SHAPED_3X3)
    .result(Material.DIAMOND, 64)
    .ingredient('D', Material.DIAMOND_BLOCK, 1)
    .shapePattern("DDD", "DDD", "DDD")
    .lore("Custom recipe", "Very special")
    .collectionTierRequired(10)
    .collectionId("DIAMOND")
    .build());
```

### **Modify Existing Recipe**
Edit recipe definitions in `RecipeRegistry.java`

### **Change Category Icons**
Modify `getCategoryIcon()` in `CraftingGuiManager.java`

---

## 🐛 Known Limitations

- **Vanilla Recipe Sync**: Some vanilla recipes may not appear in GUI
- **NBT Data**: Complex NBT items may not display correctly
- **Custom Items**: Custom plugin items require manual recipe definition
- **Shaped Recipe Size**: Currently supports up to 3x3 (4x4 planned)

---

## 📝 Credits

- **Skyblock** - Original crafting system design
- **Grivience Plugin** - Integration and customization
- **Recipe Data** - Based on Skyblock Wiki

---

## 🆘 Support

For issues or suggestions, please contact the development team or submit a bug report.

**Version**: 1.0 (Skyblock Accurate)
**Last Updated**: 2026-02-24
**Total Recipes**: 200+

