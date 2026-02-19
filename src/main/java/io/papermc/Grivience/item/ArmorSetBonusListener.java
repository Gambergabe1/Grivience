package io.papermc.Grivience.item;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ArmorSetBonusListener implements Listener {
    private final GriviencePlugin plugin;
    private final CustomArmorManager armorManager;
    private final Map<UUID, Map<String, Integer>> activeSetBonuses = new HashMap<>();
    private final Map<UUID, Long> lastEffectTick = new HashMap<>();

    public ArmorSetBonusListener(GriviencePlugin plugin, CustomArmorManager armorManager) {
        this.plugin = plugin;
        this.armorManager = armorManager;

        // Start effect ticker
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    updateArmorSetEffects(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                checkArmorSet(player);
            }
        }.runTaskLater(plugin, 10L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Map<String, Integer> sets = activeSetBonuses.get(player.getUniqueId());
        if (sets == null || sets.isEmpty()) {
            return;
        }

        // Check for specific set bonuses that trigger on damage
        for (Map.Entry<String, Integer> entry : sets.entrySet()) {
            String setId = entry.getKey();
            int pieces = entry.getValue();

            CustomArmorManager.CustomArmorSet armorSet = armorManager.getArmorSet(setId);
            if (armorSet == null) {
                continue;
            }

            // Apply set-specific damage bonuses
            if (setId.equalsIgnoreCase("shogun") && pieces >= 4) {
                // Shogun 4-piece: Reduce damage by 15%
                event.setDamage(event.getDamage() * 0.85);
                player.sendMessage(ChatColor.GOLD + "Shogun Set: Damage reduced!");
            }

            if (setId.equalsIgnoreCase("onyx") && pieces >= 2) {
                // Onyx 2-piece: Reflect 10% damage
                if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                    event.setDamage(event.getDamage() * 0.9);
                }
            }
        }
    }

    private void updateArmorSetEffects(Player player) {
        Map<String, Integer> sets = activeSetBonuses.get(player.getUniqueId());
        if (sets == null || sets.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        Long lastTick = lastEffectTick.get(player.getUniqueId());
        if (lastTick != null && now - lastTick < 1000) {
            return;
        }
        lastEffectTick.put(player.getUniqueId(), now);

        for (Map.Entry<String, Integer> entry : sets.entrySet()) {
            String setId = entry.getKey();
            int pieces = entry.getValue();

            CustomArmorManager.CustomArmorSet armorSet = armorManager.getArmorSet(setId);
            if (armorSet == null) {
                continue;
            }

            // Apply set-specific potion effects
            if (setId.equalsIgnoreCase("crimson")) {
                if (pieces >= 2) {
                    applyEffect(player, PotionEffectType.SPEED, 0);
                }
                if (pieces >= 4) {
                    applyEffect(player, PotionEffectType.STRENGTH, 0);
                }
            }

            if (setId.equalsIgnoreCase("azure")) {
                if (pieces >= 2) {
                    applyEffect(player, PotionEffectType.REGENERATION, 0);
                }
                if (pieces >= 4) {
                    applyEffect(player, PotionEffectType.WATER_BREATHING, 0);
                }
            }

            if (setId.equalsIgnoreCase("onyx")) {
                if (pieces >= 4) {
                    applyEffect(player, PotionEffectType.INVISIBILITY, 0, 100);
                }
            }

            if (setId.equalsIgnoreCase("storm")) {
                if (pieces >= 2) {
                    applyEffect(player, PotionEffectType.SPEED, 1);
                }
                if (pieces >= 4) {
                    applyEffect(player, PotionEffectType.JUMP_BOOST, 1);
                }
            }
        }
    }

    private void applyEffect(Player player, PotionEffectType type, int amplifier) {
        applyEffect(player, type, amplifier, Integer.MAX_VALUE);
    }

    private void applyEffect(Player player, PotionEffectType type, int amplifier, int duration) {
        if (player.hasPotionEffect(type)) {
            PotionEffect existing = player.getPotionEffect(type);
            if (existing != null && existing.getAmplifier() >= amplifier) {
                return;
            }
        }
        player.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false, true));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = (Player) event.getPlayer();
        checkArmorSet(player);
    }

    private void checkArmorSet(Player player) {
        Map<String, Integer> setCounts = new HashMap<>();

        ItemStack[] armor = player.getEquipment().getArmorContents();
        for (ItemStack piece : armor) {
            if (piece == null) {
                continue;
            }

            String setId = armorManager.getArmorSetId(piece);
            if (setId != null) {
                setCounts.put(setId, setCounts.getOrDefault(setId, 0) + 1);
            }
        }

        // Filter sets that meet the minimum piece requirement
        Map<String, Integer> activeSets = new HashMap<>();
        for (Map.Entry<String, Integer> entry : setCounts.entrySet()) {
            CustomArmorManager.CustomArmorSet armorSet = armorManager.getArmorSet(entry.getKey());
            if (armorSet != null && entry.getValue() >= armorSet.getPiecesRequired()) {
                activeSets.put(entry.getKey(), entry.getValue());
            }
        }

        // Check if bonuses changed
        Map<String, Integer> previous = activeSetBonuses.get(player.getUniqueId());
        if (!activeSets.equals(previous)) {
            activeSetBonuses.put(player.getUniqueId(), activeSets);

            if (!activeSets.isEmpty()) {
                player.sendMessage(ChatColor.GREEN + "Armor Set Bonus Active!");
                for (Map.Entry<String, Integer> entry : activeSets.entrySet()) {
                    CustomArmorManager.CustomArmorSet armorSet = armorManager.getArmorSet(entry.getKey());
                    if (armorSet != null) {
                        player.sendMessage(ChatColor.GOLD + armorSet.getDisplayName() +
                                ChatColor.GRAY + " (" + entry.getValue() + "/" + armorSet.getTotalPieces() + " pieces)");
                    }
                }
            }
        }
    }

    public Set<String> getActiveSetBonuses(Player player) {
        Map<String, Integer> sets = activeSetBonuses.get(player.getUniqueId());
        return sets != null ? sets.keySet() : Set.of();
    }

    public int getActivePieceCount(Player player, String setId) {
        Map<String, Integer> sets = activeSetBonuses.get(player.getUniqueId());
        if (sets == null) {
            return 0;
        }
        return sets.getOrDefault(setId, 0);
    }
}
