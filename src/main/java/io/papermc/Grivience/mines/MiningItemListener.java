package io.papermc.Grivience.mines;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.MiningItemType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MiningItemListener implements Listener {
    private final GriviencePlugin plugin;
    private final MiningSystemManager miningSystemManager;
    private final NamespacedKey customItemIdKey;
    private final Map<UUID, Long> drillCooldowns = new HashMap<>();

    // Drill persistence keys and tuning
    private final NamespacedKey drillFuelKey;
    private final NamespacedKey drillFuelMaxKey;
    private static final int DRILL_FUEL_PER_BLOCK = 10;
    private static final int FUEL_FROM_COAL = 100;
    private static final int FUEL_FROM_COAL_BLOCK = 1000;
    private static final int FUEL_FROM_VOLTA = 5000;
    private static final int FUEL_FROM_OIL_BARREL = 10000;

    private final CustomItemService itemService;

    public MiningItemListener(GriviencePlugin plugin, MiningSystemManager miningSystemManager, CustomItemService itemService) {
        this.plugin = plugin;
        this.miningSystemManager = miningSystemManager;
        this.itemService = itemService;
        this.customItemIdKey = new NamespacedKey(plugin, "custom-item-id");
        this.drillFuelKey = new NamespacedKey(plugin, "drill-fuel");
        this.drillFuelMaxKey = new NamespacedKey(plugin, "drill-fuel-max");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        String id = item.getItemMeta().getPersistentDataContainer().get(customItemIdKey, PersistentDataType.STRING);
        if (id == null) return;

        Player player = event.getPlayer();
        Action action = event.getAction();

        // Open Drill Mechanic GUI
        if (action == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
            if (player.isSneaking() && event.getClickedBlock().getType() == Material.ANVIL) {
                if (isDrillId(id)) {
                    event.setCancelled(true);
                    plugin.getDrillMechanicGui().open(player);
                    return;
                }
            }
        }

        if (isDrillId(id)) {
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                if (player.isSneaking()) {
                    handleDrillRefuel(player, item);
                } else {
                    handleDrillAbility(player, item);
                }
            }
        } else if (id.equals("PROSPECTOR_COMPASS")) {
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                handleCompass(player);
            }
        } else if (id.equals("STABILITY_ANCHOR")) {
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                handleStabilityAnchor(player, item);
            }
        } else if (id.equals("MINING_XP_SCROLL")) {
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                handleXpScroll(player, item);
            }
        } else if (id.equals("ORE_FRAGMENT_BUNDLE")) {
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                handleOreBundle(player, item);
            }
        } else if (id.equals("TEMP_MINING_SPEED_BOOST")) {
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                handleSpeedBoost(player, item);
            }
        }
    }

    private void handleOreBundle(Player player, ItemStack item) {
        item.setAmount(item.getAmount() - 1);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
        player.sendMessage(ChatColor.GREEN + "You opened an Ore Fragment Bundle!");
        int amount = 5 + new java.util.Random().nextInt(11); // 5 to 15
        for (int i = 0; i < amount; i++) {
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(itemService.createEndMinesMaterial(io.papermc.Grivience.item.EndMinesMaterialType.ORE_FRAGMENT));
            for (ItemStack leftover : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }
    }

    private void handleSpeedBoost(Player player, ItemStack item) {
        // Non-stackable effect check
        if (player.hasPotionEffect(PotionEffectType.HASTE)) {
            player.sendMessage(ChatColor.RED + "You already have a Mining Speed boost active!");
            return;
        }
        item.setAmount(item.getAmount() - 1);
        int minutes = 5 + new java.util.Random().nextInt(6); // 5 to 10
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 20 * 60 * minutes, 1)); // Haste II
        player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 1.0f);
        player.sendMessage(ChatColor.YELLOW + "Mining Speed Boost activated for " + minutes + " minutes!");
    }

    @org.bukkit.event.EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || !hand.hasItemMeta()) return;
        var meta = hand.getItemMeta();
        if (meta == null) return;
        String id = meta.getPersistentDataContainer().get(customItemIdKey, org.bukkit.persistence.PersistentDataType.STRING);
        if (!isDrillId(id)) return;

        int fuel = meta.getPersistentDataContainer().getOrDefault(drillFuelKey, PersistentDataType.INTEGER, 0);
        int max = meta.getPersistentDataContainer().getOrDefault(drillFuelMaxKey, PersistentDataType.INTEGER, 20000);
        if (fuel < DRILL_FUEL_PER_BLOCK) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Your drill has no fuel! " + ChatColor.GRAY + "Shift-right-click with Coal to refuel.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
            return;
        }
        int newFuel = Math.max(0, fuel - DRILL_FUEL_PER_BLOCK);
        var pdc = meta.getPersistentDataContainer();
        pdc.set(drillFuelKey, PersistentDataType.INTEGER, newFuel);
        
        // Dynamic stats update on use
        itemService.updateDrillLore(meta);
        
        hand.setItemMeta(meta);

        // Show fuel in action bar
        String fuelColor = newFuel > (max * 0.2) ? ChatColor.YELLOW.toString() : ChatColor.RED.toString();
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                new net.md_5.bungee.api.chat.TextComponent(ChatColor.GRAY + "Drill Fuel: " + fuelColor + formatInt(newFuel) + ChatColor.GRAY + "/" + ChatColor.YELLOW + formatInt(max)));
    }

    private void handleDrillAbility(Player player, ItemStack drill) {
        long now = System.currentTimeMillis();
        long lastUsed = drillCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - lastUsed < 30000) { // 30s cooldown
            player.sendMessage(ChatColor.RED + "Drill ability is on cooldown! (" + (30 - (now - lastUsed) / 1000) + "s)");
            return;
        }

        drillCooldowns.put(player.getUniqueId(), now);
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 100, 2)); // Haste III for 5s
        String name = ChatColor.GOLD + "Drill";
        if (drill != null && drill.hasItemMeta()) {
            ItemMeta meta = drill.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                name = meta.getDisplayName();
            }
        }
        player.sendMessage(name + ChatColor.GRAY + ": Mining speed boost activated!");
        player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 1.5f);
    }

    private boolean isDrillId(String id) {
        return id != null && id.endsWith("_DRILL");
    }

    private void handleDrillRefuel(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        var meta = item.getItemMeta();
        if (meta == null) return;
        var pdc = meta.getPersistentDataContainer();
        int max = pdc.getOrDefault(drillFuelMaxKey, PersistentDataType.INTEGER, 20000);
        int fuel = pdc.getOrDefault(drillFuelKey, PersistentDataType.INTEGER, 0);
        if (fuel >= max) {
            player.sendMessage(ChatColor.GRAY + "Your drill's tank is already full.");
            return;
        }
        int needed = max - fuel;
        int added = 0;
        // Consume coal blocks first
        added += consumeFuel(player, Material.COAL_BLOCK, FUEL_FROM_COAL_BLOCK, needed - added);
        if (added < needed) {
            added += consumeFuel(player, Material.COAL, FUEL_FROM_COAL, needed - added);
        }
        if (added < needed) {
            added += consumeCustomFuel(player, "VOLTA", FUEL_FROM_VOLTA, needed - added);
        }
        if (added < needed) {
            added += consumeCustomFuel(player, "OIL_BARREL", FUEL_FROM_OIL_BARREL, needed - added);
        }
        if (added <= 0) {
            player.sendMessage(ChatColor.RED + "You don't have any fuel (Coal) to refuel your drill.");
            return;
        }
        int newFuel = Math.min(max, fuel + added);
        pdc.set(drillFuelKey, PersistentDataType.INTEGER, newFuel);
        
        itemService.updateDrillLore(meta);
        item.setItemMeta(meta);
        
        player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 1.0f, 1.8f);
        player.sendMessage(ChatColor.YELLOW + "Refueled drill: " + ChatColor.GREEN + "+" + added + ChatColor.GRAY + " (" + newFuel + "/" + max + ")");
    }

    private int consumeFuel(Player player, Material mat, int fuelPerItem, int fuelNeeded) {
        if (fuelNeeded <= 0) return 0;
        var inv = player.getInventory();
        int toAdd = 0;
        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack == null || stack.getType() != mat) continue;
            // Skip custom items if they are supposed to be regular materials
            if (itemService.itemId(stack) != null) continue;

            while (stack.getAmount() > 0 && toAdd < fuelNeeded) {
                stack.setAmount(stack.getAmount() - 1);
                toAdd += fuelPerItem;
            }
            if (stack.getAmount() <= 0) inv.setItem(slot, null);
            if (toAdd >= fuelNeeded) break;
        }
        return toAdd;
    }

    private int consumeCustomFuel(Player player, String id, int fuelPerItem, int fuelNeeded) {
        if (fuelNeeded <= 0) return 0;
        var inv = player.getInventory();
        int toAdd = 0;
        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack == null || !id.equals(itemService.itemId(stack))) continue;

            while (stack.getAmount() > 0 && toAdd < fuelNeeded) {
                stack.setAmount(stack.getAmount() - 1);
                toAdd += fuelPerItem;
            }
            if (stack.getAmount() <= 0) inv.setItem(slot, null);
            if (toAdd >= fuelNeeded) break;
        }
        return toAdd;
    }

    private void updateDrillFuelLore(ItemStack item, int fuel, int max) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        itemService.updateDrillLore(meta);
        item.setItemMeta(meta);
    }

    private static String formatInt(int value) {
        return String.format(java.util.Locale.US, "%,d", value);
    }

    private void handleCompass(Player player) {
        Material richVein = miningSystemManager.getCurrentRichVein();
        if (richVein == null) {
            player.sendMessage(ChatColor.RED + "No Rich Vein active right now.");
        } else {
            String name = richVein.name().toLowerCase().replace("_", " ");
            player.sendMessage(ChatColor.AQUA + "Prospector's Compass: Current Rich Vein is " + ChatColor.GOLD + name + ChatColor.AQUA + "!");
        }
    }

    private void handleStabilityAnchor(Player player, ItemStack item) {
        miningSystemManager.activateStabilityAnchor(player);
        item.setAmount(item.getAmount() - 1);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
    }

    private void handleXpScroll(Player player, ItemStack item) {
        item.setAmount(item.getAmount() - 1);
        player.sendMessage(ChatColor.GREEN + "You used a Mining XP Scroll! +500 Mining XP.");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        if (plugin.getSkyblockLevelManager() != null) {
            plugin.getSkyblockLevelManager().awardCategoryXp(player, "mining", 500, "Mining XP Scroll", true);
        }
    }
}
