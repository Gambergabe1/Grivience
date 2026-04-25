package io.papermc.Grivience.skyblock.listener;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.command.IslandBypassCommand;
import io.papermc.Grivience.skyblock.island.Island;
import io.papermc.Grivience.skyblock.island.IslandManager;
import io.papermc.Grivience.gui.SkyblockMenuManager;
import io.papermc.Grivience.trade.TradeGui;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Sound;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive island protection listener.
 * Prevents visitors from harming blocks, entities, or players on other players' islands.
 * Ensures owners and members have full vanilla freedom.
 */
public final class IslandProtectionListener implements Listener {
    private static final long MESSAGE_THROTTLE_MS = 1200L;

    private final GriviencePlugin plugin;
    private final IslandManager islandManager;
    private IslandBypassCommand bypassCommand;
    private final Set<Material> protectedInteract;
    private final Set<EntityType> protectedEntities;
    private final Map<UUID, Long> lastWarnAtMs = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> lastIslandByPlayer = new ConcurrentHashMap<>();

    public IslandProtectionListener(GriviencePlugin plugin, IslandManager islandManager) {
        this.plugin = plugin;
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
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
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
        
        // ADMIN BYPASS: Admins don't get restricted by world borders
        if (hasBypass(player)) {
            if (lastIslandByPlayer.containsKey(player.getUniqueId())) {
                player.setWorldBorder(null);
                lastIslandByPlayer.remove(player.getUniqueId());
            }
            return;
        }

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
        
        // Final safety check for admins
        if (hasBypass(player)) {
            player.setWorldBorder(null);
            return;
        }

        WorldBorder border = org.bukkit.Bukkit.createWorldBorder();
        border.setCenter(island.getCenter());
        border.setSize(island.getSize());
        border.setWarningDistance(1000); // Always visible (red tint)
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

        // DEBUG: Only log if this is likely a bug (player is in the island world but rejected)
        if (player.getWorld().getName().equalsIgnoreCase(islandManager.getIslandWorldName())) {
            plugin.getLogger().info("[IslandDebug] Rejecting " + player.getName() + " (" + uuid + ") on island " + island.getId() + 
                ". Owner: " + island.getOwner() + ", Members: " + island.getMembers());
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
        if (hasBypass(player)) return;
        if (!isIslandWorld(event.getBlock().getWorld())) return;

        Island island = islandAt(event.getBlock().getLocation());
        if (island == null || isAllowed(player, island)) return;
        
        event.setCancelled(true);
        sendVisitorMessage(player, "Cannot break blocks here");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player)) return;
        if (!isIslandWorld(event.getBlock().getWorld())) return;

        Island island = islandAt(event.getBlock().getLocation());
        if (island == null || isAllowed(player, island)) return;
        
        event.setCancelled(true);
        sendVisitorMessage(player, "Cannot place blocks here");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player)) return;
        if (!isIslandWorld(event.getBlock().getWorld())) return;

        Island island = islandAt(event.getBlock().getLocation());
        if (island == null || isAllowed(player, island)) return;
        
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player)) return;
        if (!isIslandWorld(event.getBlockClicked().getWorld())) return;

        Island island = islandAt(event.getBlockClicked().getLocation());
        if (island == null || isAllowed(player, island)) return;
        
        event.setCancelled(true);
        sendVisitorMessage(player, "Cannot place liquids here");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player)) return;
        if (!isIslandWorld(event.getBlockClicked().getWorld())) return;

        Island island = islandAt(event.getBlockClicked().getLocation());
        if (island == null || isAllowed(player, island)) return;
        
        event.setCancelled(true);
        sendVisitorMessage(player, "Cannot collect liquids here");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!isIslandWorld(event.getBlock().getWorld())) return;

        Island island = islandAt(event.getBlock().getLocation());
        if (island == null) return;
        
        boolean visitorNearby = event.getBlock().getWorld().getNearbyEntities(event.getBlock().getLocation(), 10, 10, 10).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .anyMatch(p -> !isAllowed(p, island));

        if (visitorNearby) {
            event.blockList().clear();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!isIslandWorld(event.getLocation().getWorld())) return;

        Island island = islandAt(event.getLocation());
        if (island == null) return;

        Entity source = event.getEntity();
        if (source instanceof org.bukkit.entity.TNTPrimed tnt) {
            if (tnt.getSource() instanceof Player player) {
                if (isAllowed(player, island)) return;
            }
        }

        boolean visitorNearby = event.getLocation().getWorld().getNearbyEntities(event.getLocation(), 10, 10, 10).stream()
                .filter(e -> e instanceof Player)
                .map(e -> (Player) e)
                .anyMatch(p -> !isAllowed(p, island));

        if (visitorNearby) {
            event.blockList().clear();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player)) return;
        if (!isIslandWorld(player.getWorld())) return;

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.PHYSICAL) {
            return;
        }
        
        Location loc = event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : player.getLocation();
        Island island = islandAt(loc);
        
        if (island == null || isAllowed(player, island)) return;

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
        if (!isIslandWorld(event.getEntity().getWorld())) return;

        Entity damager = event.getDamager();
        Entity victim = event.getEntity();
        
        Player player = null;
        if (damager instanceof Player) {
            player = (Player) damager;
        } else if (damager instanceof org.bukkit.entity.Projectile projectile) {
            if (projectile.getShooter() instanceof Player shooter) {
                player = shooter;
            }
        }
        
        if (player == null) return;
        if (hasBypass(player)) return;
        
        Island island = islandAt(victim.getLocation());
        if (island == null || isAllowed(player, island)) {
            return;
        }

        event.setCancelled(true);
        if (victim instanceof Player) {
            sendVisitorMessage(player, "Cannot attack players here");
        } else {
            sendVisitorMessage(player, "Cannot attack " + formatEntityName(victim.getType()) + " here");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player)) return;
        if (!isIslandWorld(event.getRightClicked().getWorld())) return;

        Entity entity = event.getRightClicked();
        Island island = islandAt(entity.getLocation());
        
        if (island == null || isAllowed(player, island)) return;
        
        event.setCancelled(true);
        sendVisitorMessage(player, "Cannot interact with entities here");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player)) return;
        if (!isIslandWorld(event.getRightClicked().getWorld())) return;

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
        if (hasBypass(shooter)) return;
        
        Location hitLoc = event.getHitEntity() != null ? 
            event.getHitEntity().getLocation() : 
            (event.getHitBlock() != null ? event.getHitBlock().getLocation() : null);
        
        if (hitLoc == null) return;
        if (!isIslandWorld(hitLoc.getWorld())) return;
        
        Island island = islandAt(hitLoc);
        if (island == null || isAllowed(shooter, island)) return;
        
        projectile.remove();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        Player player = event.getPlayer();
        if (hasBypass(player)) return;
        if (!isIslandWorld(event.getBlock().getWorld())) return;

        Island island = islandAt(event.getBlock().getLocation());
        if (island == null || isAllowed(player, island)) return;
        
        event.setCancelled(true);
        sendVisitorMessage(player, "Cannot place " + formatEntityName(event.getEntity().getType()) + " here");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        // No global protection for hanging entities; allowed for environmental damage.
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        Entity remover = event.getRemover();
        Player player = (remover instanceof Player) ? (Player) remover : null;
        
        if (player == null) return;
        if (hasBypass(player)) return;
        if (!isIslandWorld(event.getEntity().getWorld())) return;
        
        Island island = islandAt(event.getEntity().getLocation());
        if (island == null || isAllowed(player, island)) return;
        
        event.setCancelled(true);
        sendVisitorMessage(player, "Cannot break " + formatEntityName(event.getEntity().getType()) + " here");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleDamage(VehicleDamageEvent event) {
        if (!(event.getAttacker() instanceof Player player)) return;
        if (hasBypass(player)) return;
        if (!isIslandWorld(event.getVehicle().getWorld())) return;
        
        Island island = islandAt(event.getVehicle().getLocation());
        if (island == null || isAllowed(player, island)) return;
        
        event.setCancelled(true);
        sendVisitorMessage(player, "Cannot damage vehicles here");
    }

    // ==================== GUI PROTECTION ====================

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (hasBypass(player)) return;
        if (!isIslandWorld(player.getWorld())) return;

        if (event.getInventory().getType() == InventoryType.CRAFTING || event.getInventory().getType() == InventoryType.CREATIVE) {
            return;
        }

        Island island = islandAt(player.getLocation());
        if (island == null || isAllowed(player, island)) return;
        if (isVisitorAllowedGui(event.getInventory())) return;

        event.setCancelled(true);
        sendVisitorMessage(player, "Cannot open menus here");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClickWhileVisiting(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (hasBypass(player)) return;
        if (!isIslandWorld(player.getWorld())) return;

        InventoryType topType = event.getView().getTopInventory().getType();
        if (topType == InventoryType.CRAFTING || topType == InventoryType.CREATIVE) return;

        Island island = islandAt(player.getLocation());
        if (island == null || isAllowed(player, island)) return;
        if (isVisitorAllowedGui(event.getView().getTopInventory())) return;

        event.setCancelled(true);
        player.closeInventory();
        sendVisitorMessage(player, "Cannot use menus here");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDragWhileVisiting(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (hasBypass(player)) return;
        if (!isIslandWorld(player.getWorld())) return;

        InventoryType topType = event.getView().getTopInventory().getType();
        if (topType == InventoryType.CRAFTING || topType == InventoryType.CREATIVE) return;

        Island island = islandAt(player.getLocation());
        if (island == null || isAllowed(player, island)) return;
        if (isVisitorAllowedGui(event.getView().getTopInventory())) return;

        event.setCancelled(true);
        player.closeInventory();
        sendVisitorMessage(player, "Cannot use menus here");
    }

    // ==================== UTILITIES ====================

    private Island islandAt(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        if (!isIslandWorld(loc.getWorld())) return null;
        return islandManager != null ? islandManager.getIslandAt(loc) : null;
    }

    private boolean isIslandWorld(World world) {
        return world != null && world.getName().equalsIgnoreCase(islandManager.getIslandWorldName());
    }

    private boolean isVisitorAllowedGui(Inventory inventory) {
        if (inventory == null) return false;
        if (inventory.getType() == InventoryType.WORKBENCH) return true;
        if (SkyblockMenuManager.isSkyblockMenuInventory(inventory) || TradeGui.isTradeInventory(inventory)) return true;

        InventoryHolder holder = inventory.getHolder();
        if (holder == null) return false;
        return holder.getClass().getName().startsWith("io.papermc.Grivience.");
    }

    private boolean isVisitorPlaceItem(Material material) {
        if (material == null) return false;
        String name = material.name();
        if (name.endsWith("_SPAWN_EGG")) return true;
        if (material == Material.ARMOR_STAND || material == Material.END_CRYSTAL) return true;
        if (material == Material.MINECART || name.endsWith("_MINECART")) return true;
        return name.endsWith("_BOAT") || name.endsWith("_RAFT");
    }

    private boolean isInteractableForVisitors(Material material) {
        if (material == null) return false;
        if (!material.isInteractable()) return false;
        String name = material.name();
        if (name.contains("STAIRS") || name.contains("WALL")) return false;
        return !name.contains("FENCE") || name.contains("FENCE_GATE");
    }

    private Set<Material> buildProtectedInteract() {
        return EnumSet.of(
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
                Material.LEVER, Material.STONE_BUTTON, Material.OAK_BUTTON,
                Material.REPEATER, Material.COMPARATOR, Material.REDSTONE_WIRE,
                Material.REDSTONE_TORCH, Material.REDSTONE_WALL_TORCH,
                Material.OBSERVER, Material.DISPENSER, Material.DROPPER,
                Material.PISTON, Material.STICKY_PISTON, Material.FURNACE,
                Material.CRAFTING_TABLE, Material.SMITHING_TABLE,
                Material.ENCHANTING_TABLE, Material.ANVIL, Material.BREWING_STAND,
                Material.OAK_DOOR, Material.IRON_DOOR, Material.OAK_TRAPDOOR,
                Material.IRON_TRAPDOOR, Material.OAK_FENCE_GATE,
                Material.WHEAT, Material.CARROTS, Material.POTATOES,
                Material.RED_BED, Material.CAKE, Material.BAMBOO
        );
    }

    private Set<EntityType> buildProtectedEntities() {
        return EnumSet.of(
                EntityType.COW, EntityType.PIG, EntityType.SHEEP, EntityType.CHICKEN,
                EntityType.VILLAGER, EntityType.WANDERING_TRADER,
                EntityType.ARMOR_STAND, EntityType.ITEM_FRAME, EntityType.GLOW_ITEM_FRAME,
                EntityType.PAINTING, EntityType.LEASH_KNOT
        );
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
