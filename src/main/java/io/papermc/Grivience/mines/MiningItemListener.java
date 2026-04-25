package io.papermc.Grivience.mines;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.MiningItemType;
import io.papermc.Grivience.util.DropDeliveryUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MiningItemListener implements Listener {
    private final GriviencePlugin plugin;
    private final MiningSystemManager miningSystemManager;
    private final NamespacedKey customItemIdKey;
    private final Map<UUID, Long> drillCooldowns = new HashMap<>();
    private final Map<UUID, Long> drillWarningCooldowns = new HashMap<>();

    // Drill persistence keys and tuning
    private final NamespacedKey drillFuelKey;
    private final NamespacedKey drillFuelMaxKey;
    private static final int DRILL_FUEL_PER_BLOCK = 10;
    private static final int FUEL_FROM_COAL = 100;
    private static final int FUEL_FROM_COAL_BLOCK = 1000;
    private static final int FUEL_FROM_VOLTA = 5000;
    private static final int FUEL_FROM_OIL_BARREL = 10000;

    private final CustomItemService itemService;

    private record FuelLoad(String label, int itemsUsed, int fuelAdded) {
    }

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
        ItemStack reward = itemService.createEndMinesMaterial(io.papermc.Grivience.item.EndMinesMaterialType.ORE_FRAGMENT);
        if (reward != null && !reward.getType().isAir()) {
            reward.setAmount(Math.max(1, amount));
            DropDeliveryUtil.giveToInventoryOrDrop(player, reward, player.getLocation());
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(org.bukkit.event.block.BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;
        
        org.bukkit.block.Block block = event.getBlock();
        String worldName = block.getWorld().getName();
        
        // --- WORLD SCOPING ---
        // Breaking Power and Drill Fuel should only apply in Mining Hub and End Mines.
        String minehubWorld = plugin.getConfig().getString("skyblock.minehub-world", "Minehub");
        String endMinesWorld = plugin.getConfig().getString("end-mines.world-name", "skyblock_end_mines");
        
        boolean inMiningArea = worldName.equalsIgnoreCase(minehubWorld) || worldName.equalsIgnoreCase("Minehub") ||
                               worldName.equalsIgnoreCase(endMinesWorld) || 
                               (plugin.getEndMinesManager() != null && worldName.equalsIgnoreCase(plugin.getEndMinesManager().getWorldName()));
        
        if (!inMiningArea) {
            return; // Normal vanilla behavior outside mining areas
        }

        int requiredPower = getRequiredBreakingPower(block);
        
        ItemStack hand = player.getInventory().getItemInMainHand();
        int playerPower = getBreakingPower(player, hand);
        
        if (playerPower < requiredPower) {
            event.setCancelled(true);
            player.sendActionBar(ChatColor.RED + "Breaking Power " + requiredPower + " Required!");
            if (shouldSendDrillWarning(player, 1_500L)) {
                player.sendMessage(ChatColor.RED + "This material requires a stronger tool! " + 
                        ChatColor.GRAY + "(Required: " + requiredPower + ", You: " + playerPower + ")");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
            }
            return;
        }

        if (hand == null || !hand.hasItemMeta()) return;
        var meta = hand.getItemMeta();
        if (meta == null) return;
        String id = meta.getPersistentDataContainer().get(customItemIdKey, org.bukkit.persistence.PersistentDataType.STRING);
        if (!isDrillId(id)) return;

        DrillStatProfile.Profile profile = drillProfile(meta);
        if (profile == null) {
            return;
        }
        int fuel = meta.getPersistentDataContainer().getOrDefault(drillFuelKey, PersistentDataType.INTEGER, 0);
        int max = meta.getPersistentDataContainer().getOrDefault(drillFuelMaxKey, PersistentDataType.INTEGER, 20000);
        DrillMechanicGui drillForgeGui = plugin.getDrillMechanicGui();
        int baseFuelCost = profile.fuelCostPerBlock();
        int fuelCost = drillForgeGui == null ? baseFuelCost : drillForgeGui.adjustedFuelCostPerBlock(player, baseFuelCost);
        if (fuel < fuelCost) {
            event.setCancelled(true);
            player.sendActionBar(ChatColor.RED + "Drill Fuel Empty" + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "Sneak-right-click to refuel");
            if (shouldSendDrillWarning(player, 1_500L)) {
                player.sendMessage(ChatColor.RED + "Your drill is out of fuel. " + ChatColor.GRAY + "Sneak-right-click to auto-refuel with Coal, Volta, or Oil Barrels.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.5f);
            }
            return;
        }
        int newFuel = Math.max(0, fuel - fuelCost);
        var pdc = meta.getPersistentDataContainer();
        pdc.set(drillFuelKey, PersistentDataType.INTEGER, newFuel);
        
        // Dynamic stats update on use
        itemService.updateDrillLore(meta);
        hand.setItemMeta(meta);
        sendDrillTelemetry(player, hand, newFuel, max, fuelCost);
    }

    private int getBreakingPower(Player player, ItemStack item) {
        int base = 0;
        if (item != null && !item.getType().isAir()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String customId = meta.getPersistentDataContainer().get(customItemIdKey, PersistentDataType.STRING);
                if (customId != null) {
                    base = DrillStatProfile.breakingPowerFor(customId);
                } else {
                    base = getVanillaBreakingPower(item.getType());
                }
            } else {
                base = getVanillaBreakingPower(item.getType());
            }
        }
        
        // Add armor bonuses
        if (plugin.getCustomArmorManager() != null) {
            base += plugin.getCustomArmorManager().totalBreakingPowerBonus(player);
        }
        
        return base;
    }

    private int getVanillaBreakingPower(Material mat) {
        return switch (mat) {
            case NETHERITE_PICKAXE -> 4;
            case DIAMOND_PICKAXE -> 3;
            case IRON_PICKAXE -> 2;
            case GOLDEN_PICKAXE -> 2;
            case STONE_PICKAXE -> 1;
            case WOODEN_PICKAXE -> 0;
            default -> 0;
        };
    }

    private int getRequiredBreakingPower(org.bukkit.block.Block block) {
        Material type = block.getType();
        
        // Only apply Breaking Power to identified Mining Materials
        if (!isMiningMaterial(type)) {
            return 0;
        }

        // --- End Mines Materials ---
        if (type == Material.AMETHYST_CLUSTER) return 7; // Kunzite (Titanium Drill+)
        if (type == Material.REINFORCED_DEEPSLATE) return 8; // Obsidian Core (Gemstone Drill+)
        if (type == Material.END_STONE) return 4; // Endstone Shards (Netherite Pickaxe+)
        
        // --- Mining Hub Custom Ores (Represented by repurposed blocks) ---
        
        // Titanium (Requires Starter Drill - Ironcrest)
        if (type == Material.LIGHT_GRAY_STAINED_GLASS || type == Material.LIGHT_GRAY_WOOL || type == Material.GRAY_CONCRETE) {
            return 5;
        }
        
        // Sapphire
        if (type == Material.BLUE_STAINED_GLASS || type == Material.BLUE_STAINED_GLASS_PANE || type == Material.BLUE_CONCRETE_POWDER) {
            return 6;
        }

        // --- Mining Hub "Block" Nodes ---
        if (type == Material.LAPIS_BLOCK) return 3;
        if (type == Material.GOLD_BLOCK) return 4;
        if (type == Material.REDSTONE_BLOCK) return 4;
        if (type == Material.EMERALD_BLOCK) return 6;
        if (type == Material.DIAMOND_BLOCK) return 7;
        if (type == Material.OBSIDIAN) return 7;
        
        return 0; 
    }

    private boolean isMiningMaterial(Material mat) {
        String name = mat.name();
        // Vanilla Ores are EXEMPT (Breaking Power 0)
        if (name.contains("_ORE")) {
            return false;
        }

        // Custom "Ores" and Mining System Materials
        return name.contains("_BLOCK") || 
               mat == Material.OBSIDIAN || 
               mat == Material.AMETHYST_CLUSTER ||
               mat == Material.REINFORCED_DEEPSLATE ||
               mat == Material.END_STONE ||
               name.contains("STAINED_GLASS") || 
               name.contains("CONCRETE") ||
               name.contains("WOOL"); // For special Mining Hub fillers
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrillItemDamage(PlayerItemDamageEvent event) {
        if (event == null || event.getItem() == null || !event.getItem().hasItemMeta()) {
            return;
        }

        ItemStack item = event.getItem();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        String id = meta.getPersistentDataContainer().get(customItemIdKey, PersistentDataType.STRING);
        if (!isDrillId(id)) {
            return;
        }

        event.setCancelled(true);

        boolean changed = false;
        if (!meta.isUnbreakable()) {
            meta.setUnbreakable(true);
            changed = true;
        }
        if (meta instanceof Damageable damageable && damageable.getDamage() > 0) {
            damageable.setDamage(0);
            changed = true;
        }
        if (changed) {
            item.setItemMeta(meta);
        }
    }

    private void handleDrillAbility(Player player, ItemStack drill) {
        long now = System.currentTimeMillis();
        ItemMeta drillMeta = drill == null ? null : drill.getItemMeta();
        DrillStatProfile.Profile profile = drillProfile(drillMeta);
        if (profile == null) {
            return;
        }
        DrillMechanicGui drillForgeGui = plugin.getDrillMechanicGui();
        long baseCooldownMillis = profile.abilityCooldownMillis();
        long cooldownMillis = drillForgeGui == null ? baseCooldownMillis : drillForgeGui.adjustedAbilityCooldownMillis(player, baseCooldownMillis);
        long lastUsed = drillCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - lastUsed < cooldownMillis) {
            long remainingMillis = cooldownMillis - (now - lastUsed);
            player.sendActionBar(ChatColor.YELLOW + "Drill Burst" + ChatColor.DARK_GRAY + " | " + ChatColor.RED + "Ready in " + formatSeconds(remainingMillis));
            return;
        }

        drillCooldowns.put(player.getUniqueId(), now);
        int baseDurationTicks = profile.abilityDurationTicks();
        int baseAmplifier = profile.abilityAmplifier();
        int durationTicks = drillForgeGui == null ? baseDurationTicks : drillForgeGui.adjustedAbilityDurationTicks(player, baseDurationTicks);
        int amplifier = drillForgeGui == null ? baseAmplifier : drillForgeGui.adjustedAbilityAmplifier(player, baseAmplifier);
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, durationTicks, amplifier));
        String name = ChatColor.GOLD + "Drill";
        if (drill != null && drill.hasItemMeta()) {
            ItemMeta meta = drill.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                name = meta.getDisplayName();
            }
        }
        player.sendMessage(name + ChatColor.GRAY + ": Drill Burst active for " + ChatColor.AQUA + (durationTicks / 20) + "s"
                + ChatColor.GRAY + " (" + ChatColor.AQUA + "Haste " + toRoman(amplifier + 1) + ChatColor.GRAY + ")"
                + (drillForgeGui != null && drillForgeGui.isOverdriveActive(player) ? ChatColor.DARK_GRAY + " | " + ChatColor.AQUA + "Overdrive empowered" : ""));
        player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 1.5f);
        sendDrillTelemetry(player, drill, drillFuel(drill), drillFuelMax(drill), currentFuelCost(player, drill));
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
        List<FuelLoad> loads = new ArrayList<>();
        added += consumeFuel(player, Material.COAL, "Coal", FUEL_FROM_COAL, needed - added, loads);
        if (added < needed) {
            added += consumeFuel(player, Material.COAL_BLOCK, "Coal Block", FUEL_FROM_COAL_BLOCK, needed - added, loads);
        }
        if (added < needed) {
            added += consumeCustomFuel(player, "VOLTA", "Volta", FUEL_FROM_VOLTA, needed - added, loads);
        }
        if (added < needed) {
            added += consumeCustomFuel(player, "OIL_BARREL", "Oil Barrel", FUEL_FROM_OIL_BARREL, needed - added, loads);
        }
        if (added <= 0) {
            player.sendMessage(ChatColor.RED + "You do not have any drill fuel. " + ChatColor.GRAY + "Auto-refuel uses Coal, Coal Blocks, Volta, then Oil Barrels.");
            return;
        }
        int newFuel = Math.min(max, fuel + added);
        pdc.set(drillFuelKey, PersistentDataType.INTEGER, newFuel);
        itemService.updateDrillLore(meta);
        item.setItemMeta(meta);

        int gainedFuel = newFuel - fuel;
        player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 1.0f, 1.8f);
        player.sendMessage(ChatColor.YELLOW + "Refueled drill: " + ChatColor.GREEN + "+" + formatInt(gainedFuel)
                + ChatColor.GRAY + " (" + formatInt(newFuel) + "/" + formatInt(max) + ")");
        if (!loads.isEmpty()) {
            player.sendMessage(ChatColor.DARK_GRAY + "Loaded: " + formatFuelLoads(loads));
        }
        sendDrillTelemetry(player, item, newFuel, max, currentFuelCost(player, item));
    }

    private int consumeFuel(Player player, Material mat, String label, int fuelPerItem, int fuelNeeded, List<FuelLoad> loads) {
        if (fuelNeeded <= 0) return 0;
        var inv = player.getInventory();
        int toAdd = 0;
        int itemsUsed = 0;
        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack == null || stack.getType() != mat) continue;
            // Skip custom items if they are supposed to be regular materials
            if (itemService.itemId(stack) != null) continue;

            while (stack.getAmount() > 0 && toAdd < fuelNeeded) {
                stack.setAmount(stack.getAmount() - 1);
                toAdd += fuelPerItem;
                itemsUsed++;
            }
            if (stack.getAmount() <= 0) inv.setItem(slot, null);
            if (toAdd >= fuelNeeded) break;
        }
        if (itemsUsed > 0) {
            loads.add(new FuelLoad(label, itemsUsed, toAdd));
        }
        return toAdd;
    }

    private int consumeCustomFuel(Player player, String id, String label, int fuelPerItem, int fuelNeeded, List<FuelLoad> loads) {
        if (fuelNeeded <= 0) return 0;
        var inv = player.getInventory();
        int toAdd = 0;
        int itemsUsed = 0;
        for (int slot = 0; slot < inv.getSize(); slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack == null || !id.equals(itemService.itemId(stack))) continue;

            while (stack.getAmount() > 0 && toAdd < fuelNeeded) {
                stack.setAmount(stack.getAmount() - 1);
                toAdd += fuelPerItem;
                itemsUsed++;
            }
            if (stack.getAmount() <= 0) inv.setItem(slot, null);
            if (toAdd >= fuelNeeded) break;
        }
        if (itemsUsed > 0) {
            loads.add(new FuelLoad(label, itemsUsed, toAdd));
        }
        return toAdd;
    }

    private DrillStatProfile.Profile drillProfile(ItemMeta meta) {
        if (meta == null) {
            return null;
        }
        String drillId = meta.getPersistentDataContainer().get(customItemIdKey, PersistentDataType.STRING);
        if (!DrillStatProfile.isDrillId(drillId)) {
            return null;
        }
        String engineId = meta.getPersistentDataContainer().get(itemService.getDrillEngineKey(), PersistentDataType.STRING);
        String tankId = meta.getPersistentDataContainer().get(itemService.getDrillTankKey(), PersistentDataType.STRING);
        return DrillStatProfile.resolve(drillId, engineId, tankId);
    }

    private int currentFuelCost(Player player, ItemStack drill) {
        ItemMeta meta = drill == null ? null : drill.getItemMeta();
        DrillStatProfile.Profile profile = drillProfile(meta);
        if (profile == null) {
            return DRILL_FUEL_PER_BLOCK;
        }
        DrillMechanicGui drillForgeGui = plugin.getDrillMechanicGui();
        return drillForgeGui == null ? profile.fuelCostPerBlock() : drillForgeGui.adjustedFuelCostPerBlock(player, profile.fuelCostPerBlock());
    }

    private void sendDrillTelemetry(Player player, ItemStack drill, int fuel, int maxFuel, int fuelCost) {
        if (player == null || drill == null) {
            return;
        }
        DrillMechanicGui drillForgeGui = plugin.getDrillMechanicGui();
        long cooldownMillis = currentAbilityCooldown(player, drill);
        long remainingMillis = Math.max(0L, cooldownMillis - (System.currentTimeMillis() - drillCooldowns.getOrDefault(player.getUniqueId(), 0L)));
        String burstStatus = remainingMillis <= 0L ? ChatColor.GREEN + "Ready" : ChatColor.RED + formatSeconds(remainingMillis);
        ChatColor fuelColor = fuelPercentColor(fuel, maxFuel);
        player.sendActionBar(ChatColor.GRAY + "Fuel " + fuelColor + formatInt(Math.max(0, fuel)) + ChatColor.GRAY + "/" + ChatColor.YELLOW + formatInt(Math.max(0, maxFuel))
                + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "Burn " + ChatColor.YELLOW + fuelCost + "/blk"
                + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "Burst " + burstStatus
                + (drillForgeGui == null ? "" : drillForgeGui.overdriveActionBarSuffix(player)));
    }

    private long currentAbilityCooldown(Player player, ItemStack drill) {
        ItemMeta meta = drill == null ? null : drill.getItemMeta();
        DrillStatProfile.Profile profile = drillProfile(meta);
        if (profile == null) {
            return DrillStatProfile.BASE_ABILITY_COOLDOWN_MILLIS;
        }
        DrillMechanicGui drillForgeGui = plugin.getDrillMechanicGui();
        return drillForgeGui == null ? profile.abilityCooldownMillis() : drillForgeGui.adjustedAbilityCooldownMillis(player, profile.abilityCooldownMillis());
    }

    private int drillFuel(ItemStack drill) {
        if (drill == null || !drill.hasItemMeta()) {
            return 0;
        }
        ItemMeta meta = drill.getItemMeta();
        if (meta == null) {
            return 0;
        }
        return meta.getPersistentDataContainer().getOrDefault(drillFuelKey, PersistentDataType.INTEGER, 0);
    }

    private int drillFuelMax(ItemStack drill) {
        if (drill == null || !drill.hasItemMeta()) {
            return 20_000;
        }
        ItemMeta meta = drill.getItemMeta();
        if (meta == null) {
            return 20_000;
        }
        return meta.getPersistentDataContainer().getOrDefault(drillFuelMaxKey, PersistentDataType.INTEGER, 20_000);
    }

    private boolean shouldSendDrillWarning(Player player, long cooldownMillis) {
        if (player == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        long lastWarning = drillWarningCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - lastWarning < cooldownMillis) {
            return false;
        }
        drillWarningCooldowns.put(player.getUniqueId(), now);
        return true;
    }

    private ChatColor fuelPercentColor(int fuel, int maxFuel) {
        if (maxFuel <= 0) {
            return ChatColor.RED;
        }
        double ratio = fuel / (double) maxFuel;
        if (ratio >= 0.6D) {
            return ChatColor.GREEN;
        }
        if (ratio >= 0.25D) {
            return ChatColor.YELLOW;
        }
        return ChatColor.RED;
    }

    private String formatFuelLoads(List<FuelLoad> loads) {
        List<String> parts = new ArrayList<>();
        for (FuelLoad load : loads) {
            parts.add(ChatColor.GRAY + load.label() + ChatColor.DARK_GRAY + " x" + ChatColor.YELLOW + load.itemsUsed());
        }
        return String.join(ChatColor.DARK_GRAY + ", ", parts);
    }

    private String formatSeconds(long millis) {
        long seconds = Math.max(1L, (millis + 999L) / 1000L);
        return seconds + "s";
    }

    private String toRoman(int value) {
        return switch (Math.max(1, value)) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            default -> Integer.toString(value);
        };
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
