package io.papermc.Grivience.mines;

import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.MiningItemType;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Dynamic output for the drill-upgrade crafting recipe.
 *
 * The underlying Bukkit recipe uses {@code Material.IRON_PICKAXE} for the drill ingredient so it can match drills with
 * varying PDC (fuel/parts). This listener validates the input is a custom drill variant and returns the next tier while
 * preserving installed parts and fuel percentage.
 */
public final class DrillUpgradeCraftListener implements Listener {
    private final CustomItemService itemService;
    private final NamespacedKey recipeKey;
    private final NamespacedKey drillFuelKey;
    private final NamespacedKey drillFuelMaxKey;

    public DrillUpgradeCraftListener(JavaPlugin plugin, CustomItemService itemService) {
        this.itemService = itemService;
        this.recipeKey = itemService == null ? null : itemService.getRecipeDrillUpgradeKey();
        this.drillFuelKey = new NamespacedKey(plugin, "drill-fuel");
        this.drillFuelMaxKey = new NamespacedKey(plugin, "drill-fuel-max");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepare(PrepareItemCraftEvent event) {
        if (recipeKey == null || itemService == null) {
            return;
        }

        CraftingInventory inv = event.getInventory();
        if (inv.getType() != InventoryType.WORKBENCH) {
            return;
        }

        if (!(event.getRecipe() instanceof Keyed keyed) || keyed.getKey() == null) {
            return;
        }
        if (!recipeKey.equals(keyed.getKey())) {
            return;
        }

        ItemStack[] matrix = inv.getMatrix();
        if (matrix == null || matrix.length < 9) {
            inv.setResult(null);
            return;
        }

        // Recipe shape uses the center slot for the drill.
        ItemStack drill = matrix[4];
        if (drill == null || drill.getType().isAir()) {
            inv.setResult(null);
            return;
        }

        String drillId = itemService.itemId(drill);
        MiningItemType nextTier = nextTier(drillId);
        if (nextTier == null) {
            inv.setResult(null);
            return;
        }

        ItemStack result = itemService.createMiningItem(nextTier);
        if (result == null) {
            inv.setResult(null);
            return;
        }

        inv.setResult(transferPartsAndFuel(drill, result));
    }

    private MiningItemType nextTier(String drillId) {
        if (drillId == null) {
            return null;
        }
        return switch (drillId) {
            case "IRONCREST_DRILL" -> MiningItemType.MITHRIL_DRILL;
            case "MITHRIL_DRILL" -> MiningItemType.TITANIUM_DRILL;
            case "TITANIUM_DRILL" -> MiningItemType.GEMSTONE_DRILL;
            default -> null;
        };
    }

    private ItemStack transferPartsAndFuel(ItemStack fromDrill, ItemStack toDrill) {
        if (fromDrill == null || toDrill == null || !fromDrill.hasItemMeta() || !toDrill.hasItemMeta()) {
            return toDrill;
        }

        ItemMeta fromMeta = fromDrill.getItemMeta();
        ItemMeta toMeta = toDrill.getItemMeta();
        if (fromMeta == null || toMeta == null) {
            return toDrill;
        }

        var fromPdc = fromMeta.getPersistentDataContainer();
        var toPdc = toMeta.getPersistentDataContainer();

        // Preserve installed parts.
        String engine = fromPdc.get(itemService.getDrillEngineKey(), PersistentDataType.STRING);
        if (engine != null && !engine.isBlank()) {
            toPdc.set(itemService.getDrillEngineKey(), PersistentDataType.STRING, engine);
        }

        String tank = fromPdc.get(itemService.getDrillTankKey(), PersistentDataType.STRING);
        if (tank != null && !tank.isBlank()) {
            toPdc.set(itemService.getDrillTankKey(), PersistentDataType.STRING, tank);
        }

        // Preserve fuel percentage across tiers (so upgrades don't magically refuel).
        int fromFuel = fromPdc.getOrDefault(drillFuelKey, PersistentDataType.INTEGER, 0);
        int fromMax = fromPdc.getOrDefault(drillFuelMaxKey, PersistentDataType.INTEGER, 0);
        double ratio = fromMax <= 0 ? 0.0 : (double) fromFuel / (double) fromMax;
        ratio = Math.max(0.0, Math.min(1.0, ratio));

        // Let the item service recompute max fuel based on the transferred tank, then apply the scaled fuel value.
        itemService.updateDrillLore(toMeta);
        int toMax = toPdc.getOrDefault(drillFuelMaxKey, PersistentDataType.INTEGER, 20000);
        int toFuel = (int) Math.round(ratio * toMax);
        toFuel = Math.max(0, Math.min(toMax, toFuel));
        toPdc.set(drillFuelKey, PersistentDataType.INTEGER, toFuel);
        itemService.updateDrillLore(toMeta);

        toDrill.setItemMeta(toMeta);
        return toDrill;
    }
}

