package io.papermc.Grivience.mines.end;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.collections.CollectionsManager;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import io.papermc.Grivience.util.DropDeliveryUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * End counterpart to a crystal-nucleus style run: bring a full set of End items to the altar,
 * cash them in for rewards, then wait out a profile-scoped cooldown.
 */
public final class EndMinesConvergenceManager implements Listener {
    private static final List<String> DEFAULT_REQUIRED_ITEM_IDS = List.of(
            "KUNZITE",
            "RIFT_ESSENCE",
            "VOID_CRYSTAL",
            "OBSIDIAN_CORE",
            "CHORUS_WEAVE"
    );
    private static final List<String> DEFAULT_REWARD_ENTRIES = List.of(
            "ORE_FRAGMENT:48:96:32",
            "ENDSTONE_SHARD:24:48:24",
            "KUNZITE:2:6:10",
            "RIFT_ESSENCE:2:5:8",
            "VOID_CRYSTAL:2:5:8",
            "OBSIDIAN_CORE:2:5:8",
            "CHORUS_WEAVE:2:5:8",
            "VOLTA:1:2:7",
            "OIL_BARREL:1:1:3",
            "MITHRIL_ENGINE:1:1:1",
            "MEDIUM_FUEL_TANK:1:1:1"
    );

    public record ConvergenceProfileRef(UUID profileId, String displayName) {
    }

    record RewardEntry(String itemId, int minAmount, int maxAmount, int weight) {
        RewardEntry {
            itemId = itemId == null ? "" : itemId.trim().toUpperCase(Locale.ROOT);
            minAmount = Math.max(1, minAmount);
            maxAmount = Math.max(minAmount, maxAmount);
            weight = Math.max(1, weight);
        }
    }

    private final GriviencePlugin plugin;
    private final EndMinesManager endMinesManager;
    private final CollectionsManager collectionsManager;
    private final CustomItemService itemService;
    private final File dataFile;
    private final Map<UUID, Long> lastCompletionByProfile = new ConcurrentHashMap<>();
    private final Map<Integer, UUID> hologramLineIds = new ConcurrentHashMap<>();
    private final NamespacedKey hologramKey;
    private final NamespacedKey hologramLineKey;
    private final Random random = new Random();
    private BukkitTask hologramTask;
    private BukkitTask visibilityTask;

    public EndMinesConvergenceManager(
            GriviencePlugin plugin,
            EndMinesManager endMinesManager,
            CollectionsManager collectionsManager,
            CustomItemService itemService
    ) {
        this.plugin = plugin;
        this.endMinesManager = endMinesManager;
        this.collectionsManager = collectionsManager;
        this.itemService = itemService;
        this.dataFile = new File(plugin.getDataFolder(), "end-convergence-data.yml");
        this.hologramKey = new NamespacedKey(plugin, "end_convergence_hologram");
        this.hologramLineKey = new NamespacedKey(plugin, "end_convergence_hologram_line");
        load();
        startHologramTask();
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("end-mines.convergence.enabled", true);
    }

    public void reload() {
        startHologramTask();
        refreshHologram();
    }

    public void shutdown() {
        stopHologramTask();
        clearHologram();
        save();
    }

    public void sendStatus(Player player) {
        if (player == null) {
            return;
        }
        if (!isEnabled()) {
            player.sendMessage(ChatColor.RED + "End Convergence is disabled.");
            return;
        }

        Location altar = getAltarLocation();
        long remaining = remainingCooldownMillis(player);
        List<String> required = requiredItemIds();

        player.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "=== End Convergence ===");
        if (altar != null) {
            player.sendMessage(ChatColor.GRAY + "Altar: " + ChatColor.AQUA + altar.getWorld().getName()
                    + ChatColor.GRAY + " (" + fmt(altar.getX()) + ", " + fmt(altar.getY()) + ", " + fmt(altar.getZ()) + ")");
            player.sendMessage(ChatColor.GRAY + "At Altar: " + (isNearAltar(player) ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
        } else {
            player.sendMessage(ChatColor.RED + "Altar location is not configured.");
        }
        player.sendMessage(ChatColor.GRAY + "Cooldown: " + (remaining <= 0L
                ? ChatColor.GREEN + "Ready"
                : ChatColor.YELLOW + formatDuration(remaining)));
        player.sendMessage(ChatColor.GRAY + "Required Items:");
        for (String itemId : required) {
            boolean has = countCustomItem(player, itemId) > 0;
            player.sendMessage((has ? ChatColor.GREEN : ChatColor.RED) + "- " + displayName(itemId));
        }
        player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/endmine convergence claim"
                + ChatColor.GRAY + " or right-click the altar to complete it.");
    }

    public boolean attempt(Player player) {
        if (player == null) {
            return false;
        }
        if (!isEnabled()) {
            player.sendMessage(ChatColor.RED + "End Convergence is disabled.");
            return false;
        }

        Location altar = getAltarLocation();
        if (altar == null) {
            player.sendMessage(ChatColor.RED + "End Convergence altar is not configured.");
            return false;
        }
        if (!isNearAltar(player)) {
            player.sendMessage(ChatColor.RED + "You must stand near the End Convergence altar to begin the run.");
            player.sendMessage(ChatColor.GRAY + "Altar: " + ChatColor.AQUA + altar.getWorld().getName()
                    + ChatColor.GRAY + " (" + fmt(altar.getX()) + ", " + fmt(altar.getY()) + ", " + fmt(altar.getZ()) + ")");
            return false;
        }

        long remaining = remainingCooldownMillis(player);
        if (remaining > 0L) {
            player.sendMessage(ChatColor.RED + "You have already completed an End Convergence recently.");
            player.sendMessage(ChatColor.GRAY + "Time remaining: " + ChatColor.YELLOW + formatDuration(remaining));
            return false;
        }

        List<String> required = requiredItemIds();
        List<String> missing = missingRequiredItemIds(player, required);
        if (!missing.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You are missing required End Convergence items.");
            for (String itemId : missing) {
                player.sendMessage(ChatColor.RED + "- " + displayName(itemId));
            }
            return false;
        }

        if (!consumeRequiredItems(player, required)) {
            player.sendMessage(ChatColor.RED + "Failed to consume the required items. Try again.");
            return false;
        }

        List<ItemStack> granted = grantRewards(player, altar);
        long xpReward = Math.max(0L, plugin.getConfig().getLong("end-mines.convergence.heart-xp-reward", 60L));
        long dustReward = Math.max(0L, plugin.getConfig().getLong("end-mines.convergence.heart-dust-reward", 25L));
        HeartOfTheEndMinesManager heartManager = plugin.getHeartOfTheEndMinesManager();
        if (heartManager != null && (xpReward > 0L || dustReward > 0L)) {
            heartManager.grantProgress(player, xpReward, dustReward, "End Convergence");
        }

        lastCompletionByProfile.put(resolveProfileId(player), System.currentTimeMillis());
        save();

        if (altar.getWorld() != null) {
            altar.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, altar.clone().add(0.5D, 1.2D, 0.5D), 30, 0.7D, 0.6D, 0.7D, 0.03D);
            altar.getWorld().playSound(altar, Sound.BLOCK_BEACON_ACTIVATE, 0.9F, 1.2F);
        }
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.3F);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "End Convergence complete!");
        player.sendMessage(ChatColor.GRAY + "You cannot complete it again for " + ChatColor.YELLOW + formatDuration(cooldownMillis()));
        if (!granted.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Rewards:");
            for (ItemStack stack : granted) {
                player.sendMessage(ChatColor.GREEN + "- " + stack.getAmount() + "x " + itemName(stack));
            }
        }
        return true;
    }

    public boolean resetCooldown(UUID profileId) {
        if (profileId == null) {
            return false;
        }
        Long removed = lastCompletionByProfile.remove(profileId);
        if (removed == null) {
            return false;
        }
        save();
        return true;
    }

    public ConvergenceProfileRef resolveProfileTarget(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        ProfileManager profileManager = plugin.getProfileManager();
        try {
            UUID uuid = UUID.fromString(input);
            if (profileManager != null) {
                SkyBlockProfile directProfile = profileManager.getProfile(uuid);
                if (directProfile != null && directProfile.getProfileId() != null) {
                    return new ConvergenceProfileRef(directProfile.getProfileId(), profileLabel(directProfile));
                }

                SkyBlockProfile selected = profileManager.getSelectedProfile(uuid);
                if (selected != null && selected.getProfileId() != null) {
                    return new ConvergenceProfileRef(selected.getProfileId(), profileLabel(selected));
                }
            }
            if (lastCompletionByProfile.containsKey(uuid)) {
                return new ConvergenceProfileRef(uuid, uuid.toString());
            }
        } catch (IllegalArgumentException ignored) {
        }

        Player online = Bukkit.getPlayerExact(input);
        if (online != null) {
            UUID profileId = resolveProfileId(online);
            return new ConvergenceProfileRef(profileId, online.getName() + " (" + selectedProfileName(online.getUniqueId()) + ")");
        }

        if (profileManager != null) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(input);
            if (offline != null && offline.getUniqueId() != null) {
                SkyBlockProfile selected = profileManager.getSelectedProfile(offline.getUniqueId());
                if (selected != null && selected.getProfileId() != null) {
                    String ownerName = offline.getName() != null ? offline.getName() : selected.getOwnerId().toString();
                    return new ConvergenceProfileRef(selected.getProfileId(), ownerName + " (" + selected.getProfileName() + ")");
                }
            }
        }

        return null;
    }

    public void setAltar(Location location) {
        clearHologram();
        if (location == null || location.getWorld() == null) {
            return;
        }
        String base = "end-mines.convergence.altar.";
        plugin.getConfig().set(base + "world", location.getWorld().getName());
        plugin.getConfig().set(base + "x", location.getX());
        plugin.getConfig().set(base + "y", location.getY());
        plugin.getConfig().set(base + "z", location.getZ());
        plugin.getConfig().set(base + "yaw", location.getYaw());
        plugin.getConfig().set(base + "pitch", location.getPitch());
        plugin.saveConfig();
        refreshHologram();
    }

    public Material styleAltarBlock(Block block) {
        if (block == null) {
            return Material.RESPAWN_ANCHOR;
        }
        Material material = configuredAltarMaterial();
        block.setType(material, false);
        if (material == Material.RESPAWN_ANCHOR) {
            BlockData data = Bukkit.createBlockData(material);
            if (data instanceof RespawnAnchor respawnAnchor) {
                respawnAnchor.setCharges(4);
                block.setBlockData(respawnAnchor, false);
            }
        }
        return material;
    }

    public Location getAltarLocation() {
        String base = "end-mines.convergence.altar.";
        String worldName = plugin.getConfig().getString(base + "world", "");
        if (worldName == null || worldName.isBlank()) {
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        double x = plugin.getConfig().getDouble(base + "x", 0.5D);
        double y = plugin.getConfig().getDouble(base + "y", 80.0D);
        double z = plugin.getConfig().getDouble(base + "z", 0.5D);
        float yaw = (float) plugin.getConfig().getDouble(base + "yaw", 0.0D);
        float pitch = (float) plugin.getConfig().getDouble(base + "pitch", 0.0D);
        return new Location(world, x, y, z, yaw, pitch);
    }

    public boolean isNearAltar(Player player) {
        if (player == null) {
            return false;
        }
        Location altar = getAltarLocation();
        if (altar == null || altar.getWorld() == null || !altar.getWorld().equals(player.getWorld())) {
            return false;
        }
        double radius = Math.max(1.5D, plugin.getConfig().getDouble("end-mines.convergence.altar-radius", 4.5D));
        return player.getLocation().distanceSquared(altar) <= radius * radius;
    }

    private void startHologramTask() {
        stopHologramTask();
        long refreshTicks = Math.max(20L, plugin.getConfig().getLong("end-mines.convergence.hologram.refresh-ticks", 40L));
        hologramTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshHologram, 20L, refreshTicks);

        // Visibility check every 5 ticks (0.25s) for responsiveness
        visibilityTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateHologramVisibility, 25L, 5L);
    }

    private void stopHologramTask() {
        if (hologramTask != null) {
            hologramTask.cancel();
            hologramTask = null;
        }
        if (visibilityTask != null) {
            visibilityTask.cancel();
            visibilityTask = null;
        }
    }

    private void refreshHologram() {
        if (!isEnabled() || !plugin.getConfig().getBoolean("end-mines.convergence.hologram.enabled", true)) {
            clearHologram();
            return;
        }

        Location altar = getAltarLocation();
        if (altar == null || altar.getWorld() == null) {
            clearHologram();
            return;
        }

        List<String> lines = buildHologramLines();
        if (lines.isEmpty()) {
            clearHologram();
            return;
        }

        String altarKey = altarKey(altar);
        cleanupNearbyHolograms(altar, altarKey, lines.size());
        clearExtraTrackedLines(lines.size());

        double baseHeight = plugin.getConfig().getDouble("end-mines.convergence.hologram.base-height", 2.25D);
        double lineSpacing = plugin.getConfig().getDouble("end-mines.convergence.hologram.line-spacing", 0.28D);
        Location base = altar.clone().add(0.0D, baseHeight, 0.0D);

        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            double yOffset = (lines.size() - 1 - lineIndex) * lineSpacing;
            Location lineLocation = base.clone().add(0.0D, yOffset, 0.0D);
            ArmorStand stand = ensureHologramLine(lineIndex, lineLocation, altarKey);
            if (stand == null) {
                continue;
            }
            styleHologramLine(stand, lineIndex, lines.get(lineIndex), altarKey);
        }

        // Trigger immediate visibility update after refresh
        updateHologramVisibility();
    }

    private void updateHologramVisibility() {
        if (!isEnabled() || !plugin.getConfig().getBoolean("end-mines.convergence.hologram.enabled", true)) {
            return;
        }

        Location altar = getAltarLocation();
        if (altar == null || altar.getWorld() == null) {
            return;
        }

        List<ArmorStand> stands = new ArrayList<>();
        for (UUID id : hologramLineIds.values()) {
            Entity e = Bukkit.getEntity(id);
            if (e instanceof ArmorStand stand && stand.isValid()) {
                stands.add(stand);
            }
        }

        if (stands.isEmpty()) {
            return;
        }

        for (Player player : altar.getWorld().getPlayers()) {
            boolean canSee = isLookingAtHologram(player, altar);
            for (ArmorStand stand : stands) {
                if (canSee) {
                    player.showEntity(plugin, stand);
                } else {
                    player.hideEntity(plugin, stand);
                }
            }
        }
    }

    private boolean isLookingAtHologram(Player player, Location altar) {
        if (player == null || player.getWorld() != altar.getWorld()) {
            return false;
        }

        // 1. Range check (e.g., 24 blocks)
        double distSq = player.getLocation().distanceSquared(altar);
        if (distSq > 24 * 24) {
            return false;
        }

        // 2. Angle check (Is the player looking towards the hologram area?)
        double baseHeight = plugin.getConfig().getDouble("end-mines.convergence.hologram.base-height", 2.25D);
        Location targetLoc = altar.clone().add(0.5D, baseHeight, 0.5D);
        
        Vector eye = player.getEyeLocation().toVector();
        Vector target = targetLoc.toVector();
        Vector toTarget = target.subtract(eye);
        Vector direction = player.getEyeLocation().getDirection();

        double dot = direction.dot(toTarget.normalize());
        // 0.85 is roughly 31 degrees off-center.
        if (dot < 0.85) {
            return false;
        }

        // 3. Line of sight check (Blocks)
        RayTraceResult result = player.getWorld().rayTraceBlocks(
                player.getEyeLocation(),
                toTarget.normalize(),
                toTarget.length(),
                FluidCollisionMode.NEVER,
                true
        );

        // If no solid block is hit before the hologram area, it's visible.
        return result == null || result.getHitBlock() == null;
    }

    private ArmorStand ensureHologramLine(int lineIndex, Location lineLocation, String altarKey) {
        UUID existingId = hologramLineIds.get(lineIndex);
        if (existingId != null) {
            Entity entity = Bukkit.getEntity(existingId);
            if (entity instanceof ArmorStand stand && entity.isValid() && matchesHologram(stand, altarKey, lineIndex)) {
                stand.teleport(lineLocation);
                return stand;
            }
        }

        ArmorStand stand = lineLocation.getWorld().spawn(lineLocation, ArmorStand.class, created -> {
            created.setVisible(false);
            created.setMarker(true);
            created.setGravity(false);
            created.setInvulnerable(true);
            created.setSilent(true);
            created.setPersistent(false);
            created.setCanPickupItems(false);
            created.setCollidable(false);
            created.setCustomNameVisible(true);
        });
        hologramLineIds.put(lineIndex, stand.getUniqueId());

        // Hide from all players initially to prevent flicker, 
        // unless they are looking at it (handled by next visibility update)
        for (Player p : stand.getWorld().getPlayers()) {
            p.hideEntity(plugin, stand);
        }

        return stand;
    }

    private void styleHologramLine(ArmorStand stand, int lineIndex, String line, String altarKey) {
        if (stand == null) {
            return;
        }
        stand.setVisible(false);
        stand.setMarker(true);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setSilent(true);
        stand.setPersistent(false);
        stand.setCanPickupItems(false);
        stand.setCollidable(false);
        stand.setCustomNameVisible(true);
        stand.setSmall(lineIndex != 0);
        stand.getPersistentDataContainer().set(hologramKey, PersistentDataType.STRING, altarKey);
        stand.getPersistentDataContainer().set(hologramLineKey, PersistentDataType.INTEGER, lineIndex);
        stand.setCustomName(line);
    }

    private void clearExtraTrackedLines(int lineCount) {
        List<Integer> staleLines = new ArrayList<>();
        for (Integer lineIndex : hologramLineIds.keySet()) {
            if (lineIndex != null && lineIndex >= lineCount) {
                staleLines.add(lineIndex);
            }
        }
        for (Integer lineIndex : staleLines) {
            UUID entityId = hologramLineIds.remove(lineIndex);
            if (entityId == null) {
                continue;
            }
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
    }

    private void clearHologram() {
        for (UUID entityId : hologramLineIds.values()) {
            if (entityId == null) {
                continue;
            }
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        hologramLineIds.clear();
    }

    private void cleanupNearbyHolograms(Location altar, String altarKey, int expectedLines) {
        if (altar == null || altar.getWorld() == null) {
            return;
        }
        List<ArmorStand> nearby = altar.getWorld().getNearbyEntities(
                        altar.clone().add(0.0D, 2.5D, 0.0D),
                        1.5D,
                        4.0D,
                        1.5D
                ).stream()
                .filter(ArmorStand.class::isInstance)
                .map(ArmorStand.class::cast)
                .filter(stand -> matchesHologram(stand, altarKey))
                .sorted(Comparator.comparingInt(this::hologramLineIndex))
                .toList();

        for (ArmorStand stand : nearby) {
            int lineIndex = hologramLineIndex(stand);
            if (lineIndex < 0 || lineIndex >= expectedLines) {
                stand.remove();
                hologramLineIds.values().removeIf(id -> id != null && id.equals(stand.getUniqueId()));
            } else {
                hologramLineIds.put(lineIndex, stand.getUniqueId());
            }
        }
    }

    private boolean matchesHologram(ArmorStand stand, String expectedAltarKey) {
        if (stand == null || !stand.isValid()) {
            return false;
        }
        String stored = stand.getPersistentDataContainer().get(hologramKey, PersistentDataType.STRING);
        return stored != null && stored.equals(expectedAltarKey);
    }

    private boolean matchesHologram(ArmorStand stand, String expectedAltarKey, int expectedLineIndex) {
        return matchesHologram(stand, expectedAltarKey) && hologramLineIndex(stand) == expectedLineIndex;
    }

    private int hologramLineIndex(ArmorStand stand) {
        if (stand == null) {
            return -1;
        }
        Integer lineIndex = stand.getPersistentDataContainer().get(hologramLineKey, PersistentDataType.INTEGER);
        return lineIndex == null ? -1 : lineIndex;
    }

    private String altarKey(Location altar) {
        if (altar == null || altar.getWorld() == null) {
            return "unset";
        }
        return altar.getWorld().getUID() + ":" + altar.getBlockX() + ":" + altar.getBlockY() + ":" + altar.getBlockZ();
    }

    private List<String> buildHologramLines() {
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "END CONVERGENCE");
        lines.add(ChatColor.LIGHT_PURPLE + "Awaken the altar with End relics");
        lines.add(ChatColor.GRAY + "Required Offerings:");

        List<String> itemLabels = requiredItemIds().stream()
                .map(this::hologramItemLabel)
                .toList();
        for (int index = 0; index < itemLabels.size(); index += 2) {
            String first = itemLabels.get(index);
            String second = index + 1 < itemLabels.size() ? itemLabels.get(index + 1) : null;
            if (second == null) {
                lines.add(first);
            } else {
                lines.add(first + ChatColor.DARK_GRAY + "  +  " + second);
            }
        }

        lines.add(ChatColor.GRAY + "Personal Cooldown: " + ChatColor.YELLOW + formatDuration(cooldownMillis()));
        lines.add(ChatColor.YELLOW + "Right-click the altar to begin");
        return lines;
    }

    private String hologramItemLabel(String itemId) {
        String plainName = ChatColor.stripColor(displayName(itemId));
        if (plainName == null || plainName.isBlank()) {
            plainName = friendlyName(itemId);
        }
        ChatColor color = switch (Objects.toString(itemId, "").trim().toUpperCase(Locale.ROOT)) {
            case "KUNZITE" -> ChatColor.LIGHT_PURPLE;
            case "RIFT_ESSENCE" -> ChatColor.DARK_PURPLE;
            case "VOID_CRYSTAL" -> ChatColor.AQUA;
            case "OBSIDIAN_CORE" -> ChatColor.GRAY;
            case "CHORUS_WEAVE" -> ChatColor.GOLD;
            default -> ChatColor.LIGHT_PURPLE;
        };
        return color + plainName;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!isEnabled() || event == null || event.getPlayer() == null) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        Location altar = getAltarLocation();
        if (altar == null || altar.getWorld() == null) {
            return;
        }
        if (!altar.getWorld().equals(event.getClickedBlock().getWorld())) {
            return;
        }
        if (event.getClickedBlock().getX() != altar.getBlockX()
                || event.getClickedBlock().getY() != altar.getBlockY()
                || event.getClickedBlock().getZ() != altar.getBlockZ()) {
            return;
        }

        event.setCancelled(true);
        attempt(event.getPlayer());
    }

    static List<String> normalizeRequiredItemIds(List<String> configured) {
        Collection<String> source = configured == null || configured.isEmpty() ? DEFAULT_REQUIRED_ITEM_IDS : configured;
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : source) {
            if (value == null || value.isBlank()) {
                continue;
            }
            normalized.add(value.trim().toUpperCase(Locale.ROOT));
        }
        if (normalized.isEmpty()) {
            normalized.addAll(DEFAULT_REQUIRED_ITEM_IDS);
        }
        return List.copyOf(normalized);
    }

    static List<RewardEntry> parseRewardEntries(List<String> configured) {
        Collection<String> source = configured == null || configured.isEmpty() ? DEFAULT_REWARD_ENTRIES : configured;
        List<RewardEntry> parsed = new ArrayList<>();
        for (String raw : source) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String[] pieces = raw.trim().split(":");
            if (pieces.length < 4) {
                continue;
            }
            try {
                String itemId = pieces[0].trim();
                int min = Integer.parseInt(pieces[1].trim());
                int max = Integer.parseInt(pieces[2].trim());
                int weight = Integer.parseInt(pieces[3].trim());
                if (itemId.isBlank() || min <= 0 || max <= 0 || weight <= 0) {
                    continue;
                }
                parsed.add(new RewardEntry(itemId, min, max, weight));
            } catch (NumberFormatException ignored) {
            }
        }
        if (parsed.isEmpty() && source != DEFAULT_REWARD_ENTRIES) {
            return parseRewardEntries(DEFAULT_REWARD_ENTRIES);
        }
        return List.copyOf(parsed);
    }

    private List<String> requiredItemIds() {
        return normalizeRequiredItemIds(plugin.getConfig().getStringList("end-mines.convergence.required-item-ids"));
    }

    private List<RewardEntry> rewardEntries() {
        return parseRewardEntries(plugin.getConfig().getStringList("end-mines.convergence.reward-pool"));
    }

    private List<String> missingRequiredItemIds(Player player, List<String> itemIds) {
        List<String> missing = new ArrayList<>();
        for (String itemId : itemIds) {
            if (countCustomItem(player, itemId) <= 0) {
                missing.add(itemId);
            }
        }
        return missing;
    }

    private boolean consumeRequiredItems(Player player, List<String> itemIds) {
        if (!missingRequiredItemIds(player, itemIds).isEmpty()) {
            return false;
        }
        for (String itemId : itemIds) {
            removeOneCustom(player, itemId);
        }
        return true;
    }

    private int countCustomItem(Player player, String itemId) {
        if (player == null || itemId == null || itemId.isBlank() || itemService == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            String current = itemService.itemId(stack);
            if (current != null && current.equalsIgnoreCase(itemId)) {
                total += Math.max(1, stack.getAmount());
            }
        }
        return total;
    }

    private void removeOneCustom(Player player, String itemId) {
        if (player == null || itemId == null || itemService == null) {
            return;
        }
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            String current = itemService.itemId(stack);
            if (current == null || !current.equalsIgnoreCase(itemId)) {
                continue;
            }
            int remaining = stack.getAmount() - 1;
            if (remaining <= 0) {
                player.getInventory().setItem(slot, null);
            } else {
                stack.setAmount(remaining);
            }
            return;
        }
    }

    private List<ItemStack> grantRewards(Player player, Location altar) {
        List<RewardEntry> entries = rewardEntries();
        int draws = Math.max(1, plugin.getConfig().getInt("end-mines.convergence.reward-draws", 3));
        List<ItemStack> granted = new ArrayList<>();
        if (itemService == null || entries.isEmpty()) {
            return granted;
        }

        for (int i = 0; i < draws; i++) {
            RewardEntry entry = drawReward(entries);
            if (entry == null) {
                continue;
            }
            ItemStack reward = itemService.createItemByKey(entry.itemId());
            if (reward == null || reward.getType().isAir()) {
                continue;
            }
            int amount = entry.minAmount() == entry.maxAmount()
                    ? entry.minAmount()
                    : ThreadLocalRandom.current().nextInt(entry.minAmount(), entry.maxAmount() + 1);
            distributeReward(player, altar, reward, amount, granted);
        }
        return granted;
    }

    private RewardEntry drawReward(List<RewardEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        int totalWeight = 0;
        for (RewardEntry entry : entries) {
            totalWeight += Math.max(1, entry.weight());
        }
        int roll = random.nextInt(Math.max(1, totalWeight));
        int cursor = 0;
        for (RewardEntry entry : entries) {
            cursor += Math.max(1, entry.weight());
            if (roll < cursor) {
                return entry;
            }
        }
        return entries.get(entries.size() - 1);
    }

    private void distributeReward(Player player, Location altar, ItemStack reward, int amount, List<ItemStack> granted) {
        if (player == null || reward == null || reward.getType().isAir() || amount <= 0) {
            return;
        }
        int maxStack = Math.max(1, reward.getMaxStackSize());
        int remaining = amount;
        while (remaining > 0) {
            int stackAmount = Math.min(maxStack, remaining);
            ItemStack stack = reward.clone();
            stack.setAmount(stackAmount);
            DropDeliveryUtil.giveToInventoryOrDrop(player, stack, altar.clone().add(0.5D, 1.0D, 0.5D));
            granted.add(stack);
            remaining -= stackAmount;
        }
    }

    private UUID resolveProfileId(Player player) {
        if (player == null) {
            return new UUID(0L, 0L);
        }
        if (collectionsManager != null) {
            UUID profileId = collectionsManager.getProfileId(player);
            if (profileId != null) {
                return profileId;
            }
        }
        return player.getUniqueId();
    }

    private String selectedProfileName(UUID ownerId) {
        ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager == null || ownerId == null) {
            return "Profile";
        }
        SkyBlockProfile selected = profileManager.getSelectedProfile(ownerId);
        if (selected == null || selected.getProfileName() == null || selected.getProfileName().isBlank()) {
            return "Profile";
        }
        return selected.getProfileName();
    }

    private String profileLabel(SkyBlockProfile profile) {
        if (profile == null) {
            return "Profile";
        }
        String profileName = profile.getProfileName();
        if (profileName == null || profileName.isBlank()) {
            profileName = "Profile";
        }
        UUID ownerId = profile.getOwnerId();
        if (ownerId == null) {
            return profileName;
        }
        OfflinePlayer owner = Bukkit.getOfflinePlayer(ownerId);
        String ownerName = owner != null && owner.getName() != null ? owner.getName() : ownerId.toString();
        return ownerName + " (" + profileName + ")";
    }

    private long cooldownMillis() {
        long seconds = Math.max(1L, plugin.getConfig().getLong("end-mines.convergence.cooldown-seconds", 3600L));
        return seconds * 1000L;
    }

    private long remainingCooldownMillis(Player player) {
        if (player == null) {
            return 0L;
        }
        long last = lastCompletionByProfile.getOrDefault(resolveProfileId(player), 0L);
        if (last <= 0L) {
            return 0L;
        }
        return Math.max(0L, (last + cooldownMillis()) - System.currentTimeMillis());
    }

    private void load() {
        if (!dataFile.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = config.getConfigurationSection("profiles");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                UUID profileId = UUID.fromString(key);
                long last = Math.max(0L, section.getLong(key, 0L));
                if (last > 0L) {
                    lastCompletionByProfile.put(profileId, last);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection section = config.createSection("profiles");
        for (Map.Entry<UUID, Long> entry : lastCompletionByProfile.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0L) {
                section.set(entry.getKey().toString(), entry.getValue());
            }
        }

        File parent = dataFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save End Convergence data: " + e.getMessage());
        }
    }

    private String displayName(String itemId) {
        if (itemService != null) {
            ItemStack stack = itemService.createItemByKey(itemId);
            if (stack != null && stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
                return stack.getItemMeta().getDisplayName();
            }
        }
        return friendlyName(itemId);
    }

    private String itemName(ItemStack stack) {
        if (stack == null) {
            return "Reward";
        }
        if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
            return stack.getItemMeta().getDisplayName();
        }
        return friendlyName(Objects.toString(itemService == null ? null : itemService.itemId(stack), stack.getType().name()));
    }

    private Material configuredAltarMaterial() {
        String configured = plugin.getConfig().getString("end-mines.convergence.altar.material", "RESPAWN_ANCHOR");
        Material material = configured == null ? null : Material.matchMaterial(configured.trim());
        if (material == null || !material.isBlock()) {
            return Material.RESPAWN_ANCHOR;
        }
        return material;
    }

    private String friendlyName(String id) {
        if (id == null || id.isBlank()) {
            return "Item";
        }
        String[] parts = id.toLowerCase(Locale.ROOT).replace('-', '_').split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.length() == 0 ? id : builder.toString();
    }

    private String formatDuration(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long leftover = seconds % 60L;
        if (hours > 0L) {
            return hours + "h " + minutes + "m " + leftover + "s";
        }
        if (minutes > 0L) {
            return minutes + "m " + leftover + "s";
        }
        return leftover + "s";
    }

    private String fmt(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
