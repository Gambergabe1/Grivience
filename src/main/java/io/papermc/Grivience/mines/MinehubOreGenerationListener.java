package io.papermc.Grivience.mines;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.util.RegenPlaceholderUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Protects the Mine Hub mining area and regenerates ore nodes from configurable Y-level bands.
 */
public final class MinehubOreGenerationListener implements Listener {
    private static final long WARNING_THROTTLE_MS = 1200L;

    private static final Set<Material> DEFAULT_BREAKABLE_BLOCKS = EnumSet.of(
            Material.COAL_ORE,
            Material.IRON_ORE,
            Material.COPPER_ORE,
            Material.GOLD_ORE,
            Material.LAPIS_ORE,
            Material.LAPIS_BLOCK,
            Material.REDSTONE_ORE,
            Material.REDSTONE_BLOCK,
            Material.DIAMOND_ORE,
            Material.DIAMOND_BLOCK,
            Material.EMERALD_ORE,
            Material.EMERALD_BLOCK,
            Material.DEEPSLATE_COAL_ORE,
            Material.DEEPSLATE_IRON_ORE,
            Material.DEEPSLATE_COPPER_ORE,
            Material.DEEPSLATE_GOLD_ORE,
            Material.DEEPSLATE_LAPIS_ORE,
            Material.DEEPSLATE_REDSTONE_ORE,
            Material.DEEPSLATE_DIAMOND_ORE,
            Material.DEEPSLATE_EMERALD_ORE,
            Material.OBSIDIAN,
            Material.BLUE_STAINED_GLASS,
            Material.BLUE_STAINED_GLASS_PANE,
            Material.BLUE_CONCRETE_POWDER,
            Material.LIGHT_GRAY_STAINED_GLASS,
            Material.LIGHT_GRAY_WOOL,
            Material.GRAY_CONCRETE
    );

    private final GriviencePlugin plugin;
    private final Map<UUID, Long> lastProtectionWarnAtMs = new ConcurrentHashMap<>();

    private volatile int cachedBreakableHash;
    private volatile Set<Material> cachedBreakableBlocks = DEFAULT_BREAKABLE_BLOCKS;

    private volatile int cachedLayerHash;
    private volatile List<OreLayer> cachedLayers = List.of();

    public MinehubOreGenerationListener(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isStagedOre(Material material) {
        if (material == null) return false;
        return switch (material) {
            case IRON_ORE, DEEPSLATE_IRON_ORE, IRON_BLOCK,
                 GOLD_ORE, DEEPSLATE_GOLD_ORE, GOLD_BLOCK,
                 COAL_ORE, DEEPSLATE_COAL_ORE,
                 LAPIS_ORE, DEEPSLATE_LAPIS_ORE, LAPIS_BLOCK,
                 REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE, REDSTONE_BLOCK,
                 EMERALD_ORE, DEEPSLATE_EMERALD_ORE, EMERALD_BLOCK,
                 DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE, DIAMOND_BLOCK,
                 OBSIDIAN, CRYING_OBSIDIAN,
                 BLUE_STAINED_GLASS, BLUE_STAINED_GLASS_PANE, BLUE_CONCRETE_POWDER -> true;
            default -> false;
        };
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (event == null || event.getBlockPlaced() == null) {
            return;
        }
        if (!isEnabled()) {
            return;
        }
        
        Location loc = event.getBlockPlaced().getLocation();
        if (!isMiningArea(loc)) {
            return;
        }
        
        if (canBuild(event.getPlayer())) {
            return;
        }

        event.setCancelled(true);
        warnProtection(event.getPlayer(), "This mining area is protected. Building is disabled here.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreakProtection(BlockBreakEvent event) {
        if (event == null || event.getBlock() == null) {
            return;
        }
        if (!isEnabled()) {
            return;
        }
        
        Location loc = event.getBlock().getLocation();
        if (!isMiningArea(loc)) {
            return;
        }
        
        if (canBuild(event.getPlayer())) {
            return;
        }

        Material type = event.getBlock().getType();

        // ALLOW CROP AND LOG BREAKING in Hub worlds so they can be handled by their listeners
        if (isCrop(type) || isLog(type)) {
            return; 
        }

        if (!getBreakableBlocks().contains(type)) {
            event.setCancelled(true);
            warnProtection(event.getPlayer(), "This mining area is protected. Only configured mining nodes can be broken.");
            return;
        }

        if (!hasConfiguredLayer(loc)) {
            event.setCancelled(true);
            warnProtection(event.getPlayer(), "Ore generation is not active at this height.");
            return;
        }

        // Check breaking power requirement
        if (!hasRequiredBreakingPower(event.getPlayer(), event.getBlock())) {
            event.setCancelled(true);
            warnProtection(event.getPlayer(), "You need a drill to mine here! Unlock your starter drill at Mining Level 20.");
            return;
        }
    }

    private boolean hasRequiredBreakingPower(Player player, org.bukkit.block.Block block) {
        if (player == null || block == null) {
            return false;
        }

        // Get required breaking power for this block
        int requiredPower = getRequiredBreakingPower(block.getType());
        if (requiredPower == 0) {
            return true; // No drill required for basic blocks
        }

        // Check if player has a drill equipped
        org.bukkit.inventory.ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand == null || mainHand.getType().isAir()) {
            return false;
        }

        // Check if it's a drill using CustomItemService
        if (plugin.getCustomItemService() == null) {
            return true; // If no custom item service, allow breaking (failsafe)
        }

        String itemId = plugin.getCustomItemService().itemId(mainHand);
        if (itemId == null || !io.papermc.Grivience.mines.DrillStatProfile.isDrillId(itemId)) {
            return false; // Not a drill
        }

        // Get drill's breaking power
        io.papermc.Grivience.mines.DrillStatProfile.Profile profile = io.papermc.Grivience.mines.DrillStatProfile.resolve(itemId, null, null);
        return profile.breakingPower() >= requiredPower;
    }

    private int getRequiredBreakingPower(Material material) {
        // Basic ores - no drill required (breaking power 0)
        if (material == Material.COAL_ORE || material == Material.DEEPSLATE_COAL_ORE
            || material == Material.IRON_ORE || material == Material.DEEPSLATE_IRON_ORE
            || material == Material.COPPER_ORE || material == Material.DEEPSLATE_COPPER_ORE
            || material == Material.IRON_BLOCK) {
            return 0;
        }

        // Uncommon ores - require basic drill (breaking power 5)
        if (material == Material.GOLD_ORE || material == Material.DEEPSLATE_GOLD_ORE
            || material == Material.LAPIS_ORE || material == Material.DEEPSLATE_LAPIS_ORE
            || material == Material.LAPIS_BLOCK
            || material == Material.REDSTONE_ORE || material == Material.DEEPSLATE_REDSTONE_ORE
            || material == Material.REDSTONE_BLOCK) {
            return 5;
        }

        // Sapphire - require basic drill (breaking power 5)
        if (material == Material.BLUE_STAINED_GLASS || material == Material.BLUE_STAINED_GLASS_PANE
            || material == Material.BLUE_CONCRETE_POWDER) {
            return 5;
        }

        // Diamond/Emerald - require Titanium drill (breaking power 7)
        if (material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE
            || material == Material.DIAMOND_BLOCK
            || material == Material.EMERALD_ORE || material == Material.DEEPSLATE_EMERALD_ORE
            || material == Material.EMERALD_BLOCK) {
            return 7;
        }

        // Titanium - require Titanium drill (breaking power 7)
        if (material == Material.LIGHT_GRAY_STAINED_GLASS || material == Material.LIGHT_GRAY_WOOL
            || material == Material.GRAY_CONCRETE) {
            return 7;
        }

        // Obsidian - require Gemstone drill (breaking power 8)
        if (material == Material.OBSIDIAN) {
            return 8;
        }

        return 5; // Default to basic drill requirement
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreakRegen(BlockBreakEvent event) {
        if (event == null || event.getBlock() == null) {
            return;
        }
        if (!isEnabled()) {
            return;
        }
        
        Location loc = event.getBlock().getLocation();
        if (!isMiningArea(loc)) {
            return;
        }
        
        // Don't process if player has build bypass (admins building the hub)
        if (canBuild(event.getPlayer())) {
            return;
        }

        Block block = event.getBlock();
        final Material originalType = block.getType();
        
        if (!getBreakableBlocks().contains(originalType)) {
            return;
        }

        if (!hasConfiguredLayer(loc)) {
            return;
        }

        // Handle Custom Drops (Sapphire, Titanium, etc.)
        handleCustomDrops(event, originalType, block);

        // 2. Handle Regeneration
        final Material replacement = originalType;
        
        if (replacement == null || replacement.isAir()) {
            return;
        }

        long delayTicks = Math.max(1L, plugin.getConfig().getLong("skyblock.minehub-ore-gen.delay-ticks", 60L));
        Location location = block.getLocation();
        Material placeholder = regenPlaceholder();
        boolean useStages = block.getY() < 117 && isStagedOre(originalType);

        // Schedule Bedrock placement
        plugin.getServer().getScheduler().runTask(plugin, () -> RegenPlaceholderUtil.placePlaceholder(location.getBlock(), placeholder));
        
        if (useStages) {
            long stage1Delay = Math.max(1L, delayTicks / 2);
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Block current = location.getBlock();
                if (!RegenPlaceholderUtil.canRestore(current, placeholder)) return;
                current.setType(Material.COBBLESTONE, false);
            }, stage1Delay);
            
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Block current = location.getBlock();
                if (!RegenPlaceholderUtil.canRestore(current, Material.COBBLESTONE)) return;
                current.setType(replacement, false);
            }, delayTicks);
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Block current = location.getBlock();
                if (!RegenPlaceholderUtil.canRestore(current, placeholder)) return;
                current.setType(replacement, false);
            }, delayTicks);
        }
    }

    private void handleCustomDrops(BlockBreakEvent event, Material originalType, Block block) {
        // Define Sapphire materials explicitly for robustness
        boolean isSapphire = (originalType == Material.BLUE_STAINED_GLASS ||
                             originalType == Material.BLUE_STAINED_GLASS_PANE ||
                             originalType == Material.BLUE_CONCRETE_POWDER ||
                             originalType == Material.LIGHT_BLUE_STAINED_GLASS ||
                             originalType == Material.LIGHT_BLUE_STAINED_GLASS_PANE ||
                             originalType == Material.LIGHT_BLUE_CONCRETE_POWDER);

        // Define Titanium materials
        boolean isTitanium = (originalType == Material.LIGHT_GRAY_STAINED_GLASS ||
                             originalType == Material.LIGHT_GRAY_WOOL ||
                             originalType == Material.GRAY_CONCRETE);

        if (isSapphire && plugin.getCustomItemService() != null) {
            event.setDropItems(false);
            int amount = originalType.name().contains("PANE") ? 1 : (originalType.name().contains("POWDER") ? 2 : 4);
            org.bukkit.inventory.ItemStack sapphire = plugin.getCustomItemService().createItemByKey("SAPPHIRE");
            if (sapphire != null) {
                sapphire.setAmount(amount);
                io.papermc.Grivience.util.DropDeliveryUtil.giveToInventoryOrDrop(event.getPlayer(), sapphire, block.getLocation().add(0.5, 0.5, 0.5));
            }
        } else if (isTitanium && plugin.getCustomItemService() != null) {
            event.setDropItems(false);
            int amount = originalType == Material.LIGHT_GRAY_WOOL ? 1 : (originalType == Material.GRAY_CONCRETE ? 2 : 4);
            org.bukkit.inventory.ItemStack titanium = plugin.getCustomItemService().createItemByKey("TITANIUM");
            if (titanium != null) {
                titanium.setAmount(amount);
                io.papermc.Grivience.util.DropDeliveryUtil.giveToInventoryOrDrop(event.getPlayer(), titanium, block.getLocation().add(0.5, 0.5, 0.5));
            }
        }
    }

    private boolean isMiningArea(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        String worldName = loc.getWorld().getName();
        
        // Check global Minehub world
        String minehubWorld = plugin.getConfig().getString("skyblock.minehub-world", "Minehub");
        if (worldName.equalsIgnoreCase(minehubWorld) || worldName.equalsIgnoreCase("Minehub")) {
            return true;
        }

        // Check global Hub world (requested to have same protections)
        String hubWorld = plugin.getConfig().getString("skyblock.hub-world", "world");
        if (worldName.equalsIgnoreCase(hubWorld)) {
            return true;
        }

        // Check Hub 2 explicitly (user world)
        if (worldName.equalsIgnoreCase("Hub 2")) {
            return true;
        }
        
        // Check specific mining areas
        ConfigurationSection areas = plugin.getConfig().getConfigurationSection("skyblock.minehub-ore-gen.areas");
        if (areas != null) {
            for (String key : areas.getKeys(false)) {
                ConfigurationSection area = areas.getConfigurationSection(key);
                if (area == null) continue;
                
                String areaWorld = area.getString("world");
                if (areaWorld != null && !worldName.equalsIgnoreCase(areaWorld)) continue;
                
                if (area.contains("min-x") && area.contains("max-x")) {
                    double x = loc.getX();
                    if (x < area.getDouble("min-x") || x > area.getDouble("max-x")) continue;
                }
                
                if (area.contains("min-z") && area.contains("max-z")) {
                    double z = loc.getZ();
                    if (z < area.getDouble("min-z") || z > area.getDouble("max-z")) continue;
                }
                
                return true;
            }
        }
        
        return false;
    }

    private boolean hasConfiguredLayer(Location loc) {
        int y = loc.getBlockY();
        
        // Check local area layers first
        ConfigurationSection areas = plugin.getConfig().getConfigurationSection("skyblock.minehub-ore-gen.areas");
        if (areas != null) {
            for (String key : areas.getKeys(false)) {
                ConfigurationSection area = areas.getConfigurationSection(key);
                if (area == null || !isLocationInAreaBounds(loc, area)) continue;
                
                ConfigurationSection layers = area.getConfigurationSection("layers");
                if (layers != null) {
                    for (String lKey : layers.getKeys(false)) {
                        ConfigurationSection layer = layers.getConfigurationSection(lKey);
                        if (layer != null && y >= layer.getInt("min-y") && y <= layer.getInt("max-y")) {
                            return true;
                        }
                    }
                }
            }
        }

        // Fallback to global layers
        for (OreLayer layer : getConfiguredLayers()) {
            if (layer.matches(y)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLocationInAreaBounds(Location loc, ConfigurationSection area) {
        String world = area.getString("world");
        if (world != null && !loc.getWorld().getName().equalsIgnoreCase(world)) return false;
        
        if (area.contains("min-x") && loc.getX() < area.getDouble("min-x")) return false;
        if (area.contains("max-x") && loc.getX() > area.getDouble("max-x")) return false;
        if (area.contains("min-z") && loc.getZ() < area.getDouble("min-z")) return false;
        if (area.contains("max-z") && loc.getZ() > area.getDouble("max-z")) return false;
        
        return true;
    }

    private boolean isMineralBlock(Material material) {
        if (material == null) return false;
        String name = material.name();
        return name.endsWith("_BLOCK") && !name.contains("RAW_");
    }

    private boolean isEnabled() {
        return plugin.getConfig().getBoolean("skyblock.minehub-ore-gen.enabled", true);
    }

    private Material regenPlaceholder() {
        return RegenPlaceholderUtil.resolvePlaceholder(plugin.getConfig(), "skyblock.minehub-ore-gen.placeholder-block", null);
    }

    private boolean isMinehubWorld(World world) {
        if (world == null) {
            return false;
        }
        String configured = plugin.getConfig().getString("skyblock.minehub-world", "Minehub");
        return world.getName().equalsIgnoreCase(configured)
                || world.getName().equalsIgnoreCase("Minehub");
    }

    private boolean canBuild(Player player) {
        return player != null
                && (player.hasPermission("grivience.admin") || player.hasPermission("grivience.minehub.build"));
    }

    private void warnProtection(Player player, String message) {
        if (player == null || message == null || message.isBlank()) {
            return;
        }

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastWarn = lastProtectionWarnAtMs.get(playerId);
        if (lastWarn != null && (now - lastWarn) < WARNING_THROTTLE_MS) {
            return;
        }

        lastProtectionWarnAtMs.put(playerId, now);
        player.sendMessage(ChatColor.RED + message);
    }

    private Set<Material> getBreakableBlocks() {
        Set<Material> materials = EnumSet.noneOf(Material.class);
        
        // 1. Always include materials defined in the Y-level generation bands
        materials.addAll(materialsFromLayers());
        
        // 2. Add materials from the explicit breakable-blocks list if configured
        List<String> configured = plugin.getConfig().getStringList("skyblock.minehub-ore-gen.breakable-blocks");
        if (configured != null && !configured.isEmpty()) {
            for (String value : configured) {
                if (value == null || value.isBlank()) continue;
                Material material = Material.matchMaterial(value.trim());
                if (material != null && material.isBlock()) {
                    materials.add(material);
                }
            }
        }
        
        // 3. Fallback to defaults if nothing is found (unlikely)
        if (materials.isEmpty()) {
            return DEFAULT_BREAKABLE_BLOCKS;
        }

        return materials;
    }

    private Set<Material> materialsFromLayers() {
        EnumSet<Material> materials = EnumSet.noneOf(Material.class);
        
        // 1. Ores from global Y-levels
        List<OreLayer> layers = getConfiguredLayers();
        for (OreLayer layer : layers) {
            for (WeightedOre ore : layer.ores()) {
                materials.add(ore.material());
            }
        }
        
        // 2. Ores from specific areas
        ConfigurationSection areas = plugin.getConfig().getConfigurationSection("skyblock.minehub-ore-gen.areas");
        if (areas != null) {
            for (String key : areas.getKeys(false)) {
                ConfigurationSection layersSection = areas.getConfigurationSection(key + ".layers");
                if (layersSection == null) continue;
                
                for (String lKey : layersSection.getKeys(false)) {
                    ConfigurationSection oresSection = layersSection.getConfigurationSection(lKey + ".ores");
                    if (oresSection == null) continue;
                    
                    for (String materialKey : oresSection.getKeys(false)) {
                        Material material = Material.matchMaterial(materialKey.trim());
                        if (material != null && material.isBlock()) {
                            materials.add(material);
                        }
                    }
                }
            }
        }
        
        return materials.isEmpty() ? DEFAULT_BREAKABLE_BLOCKS : materials;
    }

    private boolean hasConfiguredLayer(int y) {
        for (OreLayer layer : getConfiguredLayers()) {
            if (layer.matches(y)) {
                return true;
            }
        }
        return false;
    }

    private Material resolveReplacement(int y) {
        for (OreLayer layer : getConfiguredLayers()) {
            if (layer.matches(y)) {
                Material rolled = layer.roll();
                if (rolled != null) {
                    return rolled;
                }
            }
        }
        return null;
    }

    public String getLayerName(int y) {
        for (OreLayer layer : getConfiguredLayers()) {
            if (layer.matches(y)) {
                return layer.name();
            }
        }
        return null;
    }

    private List<OreLayer> getConfiguredLayers() {
        ConfigurationSection layersSection = plugin.getConfig().getConfigurationSection("skyblock.minehub-ore-gen.y-levels");
        if (layersSection == null) {
            return List.of();
        }

        Map<String, Object> rawValues = layersSection.getValues(true);
        int hash = rawValues.hashCode();
        if (hash == cachedLayerHash && cachedLayers != null) {
            return cachedLayers;
        }

        List<OreLayer> parsedLayers = new ArrayList<>();
        for (String layerKey : layersSection.getKeys(false)) {
            ConfigurationSection layerSection = layersSection.getConfigurationSection(layerKey);
            if (layerSection == null) {
                continue;
            }

            String name = layerSection.getString("name", capitalize(layerKey.replace('_', ' ')));
            int minY = layerSection.getInt("min-y", Integer.MIN_VALUE);
            int maxY = layerSection.getInt("max-y", Integer.MAX_VALUE);
            ConfigurationSection oresSection = layerSection.getConfigurationSection("ores");
            if (oresSection == null) {
                continue;
            }

            List<WeightedOre> ores = new ArrayList<>();
            for (String materialKey : oresSection.getKeys(false)) {
                if (materialKey == null || materialKey.isBlank()) {
                    continue;
                }
                Material material = Material.matchMaterial(materialKey.trim());
                int weight = oresSection.getInt(materialKey, 0);
                if (material != null && material.isBlock() && weight > 0) {
                    ores.add(new WeightedOre(material, weight));
                }
            }

            if (!ores.isEmpty()) {
                parsedLayers.add(new OreLayer(name, minY, maxY, List.copyOf(ores)));
            }
        }

        cachedLayerHash = hash;
        cachedLayers = List.copyOf(parsedLayers);
        return cachedLayers;
    }

    private String capitalize(String input) {
        if (input == null || input.isBlank()) return "";
        return input.substring(0, 1).toUpperCase(Locale.ROOT) + input.substring(1).toLowerCase(Locale.ROOT);
    }

    private boolean isLog(Material material) {
        if (material == null) return false;
        return org.bukkit.Tag.LOGS.isTagged(material);
    }

    private boolean isCrop(Material material) {
        if (material == null) return false;
        return switch (material) {
            case WHEAT, CARROTS, POTATOES, BEETROOTS, NETHER_WART, SUGAR_CANE, CACTUS, BAMBOO, COCOA -> true;
            default -> false;
        };
    }

    private record WeightedOre(Material material, int weight) {
    }

    private record OreLayer(String name, int minY, int maxY, List<WeightedOre> ores) {
        private boolean matches(int y) {
            return y >= minY && y <= maxY;
        }

        private Material roll() {
            if (ores == null || ores.isEmpty()) {
                return null;
            }

            int totalWeight = 0;
            for (WeightedOre ore : ores) {
                totalWeight += Math.max(0, ore.weight());
            }
            if (totalWeight <= 0) {
                return null;
            }

            int selection = ThreadLocalRandom.current().nextInt(totalWeight);
            int cursor = 0;
            for (WeightedOre ore : ores) {
                cursor += Math.max(0, ore.weight());
                if (selection < cursor) {
                    return ore.material();
                }
            }
            return ores.get(ores.size() - 1).material();
        }
    }
}
