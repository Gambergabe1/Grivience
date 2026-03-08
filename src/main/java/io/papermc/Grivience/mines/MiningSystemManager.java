package io.papermc.Grivience.mines;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class MiningSystemManager {
    private final GriviencePlugin plugin;
    
    // Ore Rarity Cycle
    private Material currentRichVein = null;
    private final List<Material> orePool = Arrays.asList(
            Material.COAL_ORE, Material.IRON_ORE, Material.GOLD_ORE, 
            Material.LAPIS_ORE, Material.REDSTONE_ORE, Material.DIAMOND_ORE, Material.EMERALD_ORE
    );
    private long lastCycleTime = 0;
    private static final long CYCLE_INTERVAL = 20 * 60 * 1000; // 20 minutes
    private static final long RICH_DURATION = 5 * 60 * 1000; // 5 minutes

    // Ore Streak
    private final Map<UUID, Integer> playerStreaks = new HashMap<>();
    
    // Stability Anchor
    private final Map<UUID, Long> stabilityAnchors = new HashMap<>();

    public MiningSystemManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        startCycleTask();
    }

    private void startCycleTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            if (now - lastCycleTime >= CYCLE_INTERVAL) {
                lastCycleTime = now;
                currentRichVein = orePool.get(ThreadLocalRandom.current().nextInt(orePool.size()));
                announceOreCycleTitle(currentRichVein);
            }
        }, 0L, 20 * 60L); // Check every minute
    }

    public Material getCurrentRichVein() {
        long now = System.currentTimeMillis();
        if (now - lastCycleTime <= RICH_DURATION) {
            return currentRichVein;
        }
        return null;
    }

    public void incrementStreak(Player player) {
        UUID uuid = player.getUniqueId();
        int streak = playerStreaks.getOrDefault(uuid, 0) + 1;
        playerStreaks.put(uuid, streak);

        if (streak % 50 == 0) {
            player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "ORE STREAK! " + ChatColor.YELLOW + streak + " ores broken in this zone.");
            player.sendMessage(ChatColor.AQUA + "Bonus Mining XP granted!");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            // In a real implementation, we would call an XP service here.
        }
    }

    public void resetStreak(UUID uuid) {
        playerStreaks.remove(uuid);
    }

    public double getDeepPressurePenalty(Player player) {
        if (isStabilityAnchorActive(player)) return 0.0;

        int y = player.getLocation().getBlockY();
        if (y >= 64) return 0.0;
        
        // Deep Pressure System: Deeper layers provide increased XP but reduced speed.
        // Penalty scaling: 0.1 reduction for every 16 blocks below 64.
        double depth = 64 - y;
        return Math.min(0.5, (depth / 128.0)); // Max 50% reduction
    }

    public void activateStabilityAnchor(Player player) {
        stabilityAnchors.put(player.getUniqueId(), System.currentTimeMillis() + 60000); // 1 minute
        player.sendMessage(ChatColor.AQUA + "Stability Anchor activated! Deep pressure effects neutralized for 60 seconds.");
    }

    public boolean isStabilityAnchorActive(Player player) {
        Long expiry = stabilityAnchors.get(player.getUniqueId());
        return expiry != null && expiry > System.currentTimeMillis();
    }

    private String formatMaterial(Material m) {
        if (m == null) return "None";
        String name = m.name().toLowerCase().replace("_", " ");
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private void announceOreCycleTitle(Material vein) {
        if (vein == null) {
            return;
        }
        String title = ChatColor.GOLD + "" + ChatColor.BOLD + "ORE CYCLE!";
        String subtitle = ChatColor.YELLOW + "Rich Vein: " + ChatColor.GOLD + formatMaterial(vein);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == null || player.getWorld() == null) {
                continue;
            }
            String worldName = player.getWorld().getName();
            if (!isMiningAnnouncementWorld(worldName)) {
                continue;
            }
            player.sendTitle(title, subtitle, 10, 50, 15);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.3f);
        }
    }

    private boolean isMiningAnnouncementWorld(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return false;
        }
        String minehubWorld = plugin.getConfig().getString("skyblock.minehub-world", "Minehub");
        String endMinesWorld = plugin.getConfig().getString("end-mines.world-name", "skyblock_end_mines");
        return worldName.equalsIgnoreCase(minehubWorld)
                || worldName.equalsIgnoreCase("Minehub")
                || worldName.equalsIgnoreCase(endMinesWorld);
    }
}
