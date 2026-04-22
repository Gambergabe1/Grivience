package io.papermc.Grivience.skyblock.listener;

import io.papermc.Grivience.skyblock.command.IslandBypassCommand;
import io.papermc.Grivience.skyblock.island.Island;
import io.papermc.Grivience.skyblock.island.IslandManager;
import io.papermc.Grivience.gui.SkyblockMenuManager;
import io.papermc.Grivience.trade.TradeGui;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.WorldBorder;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive island protection listener.
 * Prevents visitors from harming blocks, entities, or players on other players' islands.
 */
public final class IslandProtectionListener implements Listener {
    private static final long MESSAGE_THROTTLE_MS = 1200L;

    private final IslandManager islandManager;
    private IslandBypassCommand bypassCommand;
    private final Set<Material> protectedInteract;
    private final Set<EntityType> protectedEntities;
    private final Map<UUID, Long> lastWarnAtMs = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> lastIslandByPlayer = new ConcurrentHashMap<>();

    public IslandProtectionListener(IslandManager islandManager) {
        this.islandManager = islandManager;
        this.protectedInteract = buildProtectedInteract();
        this.protectedEntities = buildProtectedEntities();
    }

    public void setBypassCommand(IslandBypassCommand bypassCommand) {
        this.bypassCommand = bypassCommand;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        updateWorldBorder(event.getPlayer(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        updateWorldBorder(event.getPlayer(), event.getTo());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        updateWorldBorder(event.getPlayer(), event.getPlayer().getLocation());
    }

    private void updateWorldBorder(Player player, Location loc) {
        if (islandManager == null) return;
        String islandWorld = islandManager.getIslandWorldName();
        if (!loc.getWorld().getName().equalsIgnoreCase(islandWorld)) {
            if (lastIslandByPlayer.containsKey(player.getUniqueId())) {
                player.setWorldBorder(null); // Reset to world default
                lastIslandByPlayer.remove(player.getUniqueId());
            }
            return;
        }

        Island island = islandManager.getIslandAt(loc);
        UUID currentIslandId = island != null ? island.getId() : null;
        UUID lastIslandId = lastIslandByPlayer.get(player.getUniqueId());

        if (currentIslandId != null) {
            if (!currentIslandId.equals(lastIslandId)) {
                refreshWorldBorder(player, island);
                lastIslandByPlayer.put(player.getUniqueId(), currentIslandId);
            }
        } else if (lastIslandId != null) {
            player.setWorldBorder(null);
            lastIslandByPlayer.remove(player.getUniqueId());
        }
    }

    public void refreshWorldBorder(Player player, Island island) {
        if (island == null || player == null) return;
        
        WorldBorder border = org.bukkit.Bukkit.createWorldBorder();
        border.setCenter(island.getCenter());
        border.setSize(island.getSize());
        border.setWarningDistance(5);
        border.setDamageAmount(0.2);
        
        player.setWorldBorder(border);
    }

    /**
     * Check if a player can bypass island restrictions.
     */
    private boolean hasBypass(Player player) {
        if (player == null) {
            return false;
        }
        // Admins should always be able to assist players on their islands.
        if (player.hasPermission("grivience.admin")) {
            return true;
        }
        return bypassCommand != null && bypassCommand.hasBypass(player);
    }

    /**
     * Check if a player is allowed to interact on an island.
     */
    private boolean isAllowed(Player player, Island island) {
        if (island == null) return true; // Not on an island
        if (hasBypass(player)) return true; // Admin bypass
        
        UUID uuid = player.getUniqueId();
        
        // 1. Check if owner
        if (uuid.equals(island.getOwner())) {
            return true;
        }
        
        // 2. Check if co-op member
        if (island.isMember(uuid)) {
            return true;
        }
        
        return false;
    }

    /**
     * Send visitor restriction message.
     */
    private void sendVisitorMessage(Player player, String action) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (playerId != null) {
            long now = System.currentTimeMillis();
            Long last = lastWarnAtMs.get(playerId);
            if (last != null && (now - last) < MESSAGE_THROTTLE_MS) {
                return;
            }
            lastWarnAtMs.put(playerId, now);
        }
        player.sendMessage(ChatColor.RED + action);
        player.sendMessage(ChatColor.GRAY + "You cannot do this on someone else's island.");
        player.sendMessage(ChatColor.YELLOW + "Tip: " + ChatColor.GRAY + "Create your own island with /island create");
    }

    // ==================== BLOCK PROTECTION ====================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Island island = islandAt(event.getBlock().getLocation());
        
        if (island == null || isAllowed(player, island)) return;
        
        event.setCancelled(true);
        sendVisitorMessage(player, "Cannot break blocks here");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Island island = islandAt(event.getBlock().getLocation());
        
        if (island == null || isAllowed(player, island)) return;
        
        event.setCancelled(true);
        sendVisitorMessage(player, "Cannot place blocks here");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        Island island = islandAt(event.getBlock().getLocation());
        
        if (island == null || isAllowed(player, island)) return;
        
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        Island island = islandAt(event.getBlockClicked().getLocation());
        
        if (island == null || isAllowed(player, island)) return;
        
        event.setCancelled(true);
        sendVisitorMessage(player, "Cannot place liquids here");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        Island island = islandAt(event.getBlockClicked().getLocation());
        
        if (island == null || isAllowed(player, island)) return;
        
        event.setCancelled(true);
        sendVisitorMessage(player, "Cannot collect liquids here");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        Island island = islandAt(event.getBlock().getLocation());
        if (island == null) return;
        
        // Prevent explosions from damaging blocks on islands
        event.blockList().clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Island island = islandAt(event.getLocation());
        if (island == null) return;
        
        // Prevent explosions from damaging blocks on islands
        event.blockList().clear();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.PHYSICAL) {
            return;
        }
        
        Player player = event.getPlayer();
        Location loc = event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : player.getLocation();
        Island island = islandAt(loc);
        
        if (island == null || isAllowed(player, island)) return;

        // Prevent placing non-block entities (armor stands, boats, minecarts, spawn eggs, etc.) while visiting.
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && isVisitorPlaceItem(item.getType())) {
                event.setCancelled(true);
                sendVisitorMessage(player, "Cannot place " + formatMaterialName(item.getType()) + " here");
                return;
            }
        }
        
        Material type = event.getClickedBlock() != null ? event.getClickedBlock().getType() : Material.AIR;
        
        if (event.getAction() == Action.PHYSICAL
                || protectedInteract.contains(type)
                || isInteractableForVisitors(type)) {
            event.setCancelled(true);
            sendVisitorMessage(player, "Cannot interact with " + formatMaterialName(type) + " here");
        }
    }

    // ==================== ENTITY PROTECTION ====================

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();
        
        // Get the player responsible (direct or via projectile)
        Player player = null;
        if (damager instanceof Player) {
            player = (Player) damager;
        } else if (damager instanceof org.bukkit.entity.Projectile projectile) {
            if (projectile.getShooter() instanceof Player shooter) {
                player = shooter;
            }
        }
        
        if (player == null) return;
        
        Island island = islandAt(victim.getLocation());
        if (island == null) return;
        if (hasBypass(player)) return;

        // PVP is disabled on islands by default (Hypixel-style).
        if (victim instanceof Player) {
            event.setCancelled(true);
            if (!isAllowed(player, island)) {
                sendVisitorMessage(player, "Cannot attack players here");
            }
            return;
        }

        if (isAllowed(player, island)) return;
        
        // Prevent damaging entities on other islands
        event.setCancelled(true);
        
        sendVisitorMessage(player, "Cannot attack " + formatEntityName(victim.getType()) + " here");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        Island island = islandAt(entity.getLocation());
        
        if (island == null || isAllowed(player, island)) return;
        
        event.setCancelled(true);
        sendVisitorMessage(player, "Cannot interact with entities here");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        Island island = islandAt(entity.getLocation());
        
        if (island == null || isAllowed(player, island)) return;
        
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        Entity projectile = event.getEntity();
        if (!(projectile instanceof org.bukkit.entity.Projectile proj)) return;
        if (!(proj.getShooter() instanceof Player shooter)) return;
        
        Location hitLoc = event.getHitEntity() != null ? 
            event.getHitEntity().getLocation() : 
            (event.getHitBlock() != null ? event.getHitBlock().getLocation() : null);
        
        if (hitLoc == null) return;
        
        Island island = islandAt(hitLoc);
        if (island == null || isAllowed(shooter, island)) return;
        
        // Prevent projectiles from hitting things on other islands
        projectile.remove();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        Player player = event.getPlayer();
        Island island = islandAt(event.getBlock().getLocation());
        
        if (island == null || isAllowed(player, island)) return;
        
        event.setCancelled(true);
        sendVisitorMessage(player, "Cannot place " + formatEntityName(event.getEntity().getType()) + " here");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        Island island = islandAt(event.getEntity().getLocation());
        if (island == null) return;

        // If broken by anything else (explosions, etc. on an island), we protect the island.
        // If broken by an entity (usually a player), onHangingBreakByEntity handles the specific permission check.
        if (event.getCause() != HangingBreakEvent.RemoveCause.ENTITY) {
            event.setCancelled(true);
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        Entity remover = event.getRemover();
        Player player = null;
        
        if (remover instanceof Player) {
            player = (Player) remover;
        }
        
        if (player == null) return;
        
        Island island = islandAt(event.getEntity().getLocation());
        if (island == null || isAllowed(player, island)) return;
        
        event.setCancelled(true);
        sendVisitorMessage(player, "Cannot break " + formatEntityName(event.getEntity().getType()) + " here");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleDamage(VehicleDamageEvent event) {
        if (!(event.getAttacker() instanceof Player player)) return;
        
        Island island = islandAt(event.getVehicle().getLocation());
        if (island == null || isAllowed(player, island)) return;
        
        event.setCancelled(true);
        sendVisitorMessage(player, "Cannot damage vehicles here");
    }

    // ==================== GUI PROTECTION ====================

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        // Always allow normal inventory management.
        if (event.getInventory().getType() == InventoryType.CRAFTING || event.getInventory().getType() == InventoryType.CREATIVE) {
            return;
        }

        Island island = islandAt(player.getLocation());
        if (island == null || isAllowed(player, island)) {
            return;
        }
        if (isVisitorAllowedGui(event.getInventory())) {
            return;
        }

        // Visiting another player's island: no GUI/menu access (Hypixel-style).
        event.setCancelled(true);
        sendVisitorMessage(player, "Cannot open menus here");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClickWhileVisiting(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        InventoryType topType = event.getView().getTopInventory().getType();
        if (topType == InventoryType.CRAFTING || topType == InventoryType.CREATIVE) {
            return; // allow normal inventory management
        }

        Island island = islandAt(player.getLocation());
        if (island == null || isAllowed(player, island)) {
            return;
        }
        if (isVisitorAllowedGui(event.getView().getTopInventory())) {
            return;
        }

        event.setCancelled(true);
        player.closeInventory();
        sendVisitorMessage(player, "Cannot use menus here");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDragWhileVisiting(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        InventoryType topType = event.getView().getTopInventory().getType();
        if (topType == InventoryType.CRAFTING || topType == InventoryType.CREATIVE) {
            return;
        }

        Island island = islandAt(player.getLocation());
        if (island == null || isAllowed(player, island)) {
            return;
        }
        if (isVisitorAllowedGui(event.getView().getTopInventory())) {
            return;
        }

        event.setCancelled(true);
        player.closeInventory();
        sendVisitorMessage(player, "Cannot use menus here");
    }

    // ==================== UTILITIES ====================

    private Island islandAt(Location loc) {
        return islandManager != null ? islandManager.getIslandAt(loc) : null;
    }

    private boolean isVisitorAllowedGui(Inventory inventory) {
        // Visitors should still be able to use their personal menus (Skyblock Menu and its sub-menus) and trade.
        // We key off InventoryHolder because most plugin GUIs use private holder classes.
        if (inventory == null) {
            return false;
        }

        // Skyblock Menu opens a virtual crafting grid via openWorkbench(..., true).
        if (inventory.getType() == InventoryType.WORKBENCH) {
            return true;
        }

        if (SkyblockMenuManager.isSkyblockMenuInventory(inventory) || TradeGui.isTradeInventory(inventory)) {
            return true;
        }

        InventoryHolder holder = inventory.getHolder();
        if (holder == null) {
            return false;
        }

        // Allow all Grivience GUI inventories while visiting (Pets, Wardrobe, Storage, Profile, etc.).
        return holder.getClass().getName().startsWith("io.papermc.Grivience.");
    }

    private boolean isVisitorPlaceItem(Material material) {
        if (material == null) {
            return false;
        }
        String name = material.name();
        // Spawning/placing entities should be blocked for visitors (Hypixel-style island protection).
        if (name.endsWith("_SPAWN_EGG")) {
            return true;
        }
        if (material == Material.ARMOR_STAND || material == Material.END_CRYSTAL) {
            return true;
        }
        if (material == Material.MINECART || name.endsWith("_MINECART")) {
            return true;
        }
        // Includes both normal and chest boats; also includes bamboo rafts.
        if (name.endsWith("_BOAT") || name.endsWith("_RAFT")) {
            return true;
        }
        return false;
    }

    private boolean isInteractableForVisitors(Material material) {
        if (material == null) {
            return false;
        }
        if (!material.isInteractable()) {
            return false;
        }
        String name = material.name();
        // These report as interactable in Bukkit, but don't actually open GUIs or toggle states.
        if (name.contains("STAIRS") || name.contains("WALL")) {
            return false;
        }
        // Fence gates are real interactables; plain fences are not.
        if (name.contains("FENCE") && !name.contains("FENCE_GATE")) {
            return false;
        }
        return true;
    }

    private Set<Material> buildProtectedInteract() {
        Set<Material> set = EnumSet.of(
                // Containers
                Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL,
                Material.ENDER_CHEST, Material.SHULKER_BOX,
                Material.WHITE_SHULKER_BOX, Material.ORANGE_SHULKER_BOX,
                Material.MAGENTA_SHULKER_BOX, Material.LIGHT_BLUE_SHULKER_BOX,
                Material.YELLOW_SHULKER_BOX, Material.LIME_SHULKER_BOX,
                Material.PINK_SHULKER_BOX, Material.GRAY_SHULKER_BOX,
                Material.LIGHT_GRAY_SHULKER_BOX, Material.CYAN_SHULKER_BOX,
                Material.PURPLE_SHULKER_BOX, Material.BLUE_SHULKER_BOX,
                Material.BROWN_SHULKER_BOX, Material.GREEN_SHULKER_BOX,
                Material.RED_SHULKER_BOX, Material.BLACK_SHULKER_BOX,
                
                // Redstone components
                Material.LEVER,
                Material.STONE_BUTTON, Material.OAK_BUTTON, Material.SPRUCE_BUTTON,
                Material.BIRCH_BUTTON, Material.JUNGLE_BUTTON, Material.ACACIA_BUTTON,
                Material.DARK_OAK_BUTTON, Material.CRIMSON_BUTTON, Material.WARPED_BUTTON,
                Material.MANGROVE_BUTTON, Material.BAMBOO_BUTTON, Material.CHERRY_BUTTON,
                Material.POLISHED_BLACKSTONE_BUTTON,
                Material.STONE_PRESSURE_PLATE, Material.HEAVY_WEIGHTED_PRESSURE_PLATE,
                Material.LIGHT_WEIGHTED_PRESSURE_PLATE, Material.OAK_PRESSURE_PLATE,
                Material.SPRUCE_PRESSURE_PLATE, Material.BIRCH_PRESSURE_PLATE,
                Material.JUNGLE_PRESSURE_PLATE, Material.ACACIA_PRESSURE_PLATE,
                Material.DARK_OAK_PRESSURE_PLATE, Material.CRIMSON_PRESSURE_PLATE,
                Material.WARPED_PRESSURE_PLATE, Material.MANGROVE_PRESSURE_PLATE,
                Material.BAMBOO_PRESSURE_PLATE, Material.CHERRY_PRESSURE_PLATE,
                Material.POLISHED_BLACKSTONE_PRESSURE_PLATE,
                Material.REPEATER, Material.COMPARATOR, Material.REDSTONE_WIRE,
                Material.REDSTONE_TORCH, Material.REDSTONE_WALL_TORCH,
                
                // Utilities
                Material.OBSERVER, Material.DISPENSER, Material.DROPPER,
                Material.PISTON, Material.STICKY_PISTON,
                Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER,
                Material.CRAFTING_TABLE, Material.SMITHING_TABLE,
                Material.ENCHANTING_TABLE, Material.ANVIL,
                Material.BREWING_STAND, Material.CAULDRON,
                
                // Doors and gates
                Material.OAK_DOOR, Material.SPRUCE_DOOR, Material.BIRCH_DOOR,
                Material.JUNGLE_DOOR, Material.ACACIA_DOOR, Material.DARK_OAK_DOOR,
                Material.IRON_DOOR, Material.OAK_TRAPDOOR, Material.SPRUCE_TRAPDOOR,
                Material.BIRCH_TRAPDOOR, Material.JUNGLE_TRAPDOOR, Material.ACACIA_TRAPDOOR,
                Material.DARK_OAK_TRAPDOOR, Material.IRON_TRAPDOOR,
                Material.OAK_FENCE_GATE, Material.SPRUCE_FENCE_GATE,
                Material.BIRCH_FENCE_GATE, Material.JUNGLE_FENCE_GATE,
                Material.ACACIA_FENCE_GATE, Material.DARK_OAK_FENCE_GATE,
                
                // Crops and plants
                Material.WHEAT, Material.CARROTS, Material.POTATOES,
                Material.BEETROOTS, Material.MELON_STEM, Material.PUMPKIN_STEM,
                Material.COCOA, Material.NETHER_WART,
                
                // Other interactables
                Material.RED_BED, Material.BLUE_BED, Material.GREEN_BED,
                Material.BROWN_BED, Material.BLACK_BED, Material.GRAY_BED,
                Material.LIGHT_GRAY_BED, Material.WHITE_BED, Material.ORANGE_BED,
                Material.MAGENTA_BED, Material.LIGHT_BLUE_BED, Material.YELLOW_BED,
                Material.LIME_BED, Material.PINK_BED, Material.CYAN_BED, Material.PURPLE_BED,
                Material.CAKE, Material.SWEET_BERRY_BUSH, Material.BAMBOO
        );
        return set;
    }

    private Set<EntityType> buildProtectedEntities() {
        Set<EntityType> set = EnumSet.of(
                // Animals
                EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.CHICKEN,
                EntityType.HORSE, EntityType.DONKEY, EntityType.MULE,
                EntityType.LLAMA, EntityType.AXOLOTL, EntityType.GOAT,
                
                // Passive mobs
                EntityType.VILLAGER, EntityType.WANDERING_TRADER,
                
                // Utility entities
                EntityType.ARMOR_STAND, EntityType.ITEM_FRAME, EntityType.GLOW_ITEM_FRAME,
                EntityType.PAINTING, EntityType.LEASH_KNOT
        );
        return set;
    }

    private String formatMaterialName(Material material) {
        String name = material.name();
        StringBuilder result = new StringBuilder();
        String[] parts = name.split("_");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) result.append(" ");
            result.append(Character.toUpperCase(parts[i].charAt(0)));
            result.append(parts[i].substring(1).toLowerCase());
        }
        return result.toString();
    }

    private String formatEntityName(EntityType type) {
        String name = type.name();
        StringBuilder result = new StringBuilder();
        String[] parts = name.split("_");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) result.append(" ");
            result.append(Character.toUpperCase(parts[i].charAt(0)));
            result.append(parts[i].substring(1).toLowerCase());
        }
        return result.toString();
    }
}
