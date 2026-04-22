package io.papermc.Grivience.slayer;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.attribute.Attribute;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.NamespacedKey;
import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public class SlayerListener implements Listener {
    private final GriviencePlugin plugin;
    private final SlayerManager slayerManager;
    private final NamespacedKey bossKey;

    public SlayerListener(GriviencePlugin plugin, SlayerManager slayerManager) {
        this.plugin = plugin;
        this.slayerManager = slayerManager;
        this.bossKey = new NamespacedKey(plugin, "slayer_boss");
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        
        // Handle boss death
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        if (pdc.has(bossKey, PersistentDataType.STRING)) {
            event.getDrops().clear();
            event.getDrops().add(new ItemStack(Material.DIAMOND, 5));
            Player killer = entity.getKiller();
            if (killer != null) {
                plugin.getSkyblockSkillManager().addXp(killer, io.papermc.Grivience.skills.SkyblockSkill.COMBAT, 500);
            }
            return;
        }

        // Handle regular mob death for XP
        Player killer = entity.getKiller();
        if (killer == null) return;

        SlayerManager.SlayerQuest quest = slayerManager.getActiveQuest(killer);
        if (quest == null) return;

        if (entity.getType() == quest.getType().getTargetType()) {
            double multiplier = 1.0;
            if (plugin.getMayorManager() != null && plugin.getMayorManager().getActiveMayor() != null) {
                if (plugin.getMayorManager().getActiveMayor().getName().equalsIgnoreCase("Aatrox")) {
                    multiplier = 1.2;
                }
            }
            int xpAmount = (int) (1 * multiplier);
            if (multiplier > 1.0 && xpAmount == 1 && Math.random() < 0.2) {
                xpAmount = 2;
            }

            quest.addXp(xpAmount);
            plugin.getSkyblockSkillManager().addXp(killer, io.papermc.Grivience.skills.SkyblockSkill.COMBAT, xpAmount);
            killer.sendMessage(ChatColor.GREEN + "Slayer XP: " + quest.getCurrentXp() + "/" + quest.getType().getRequiredXp());

            if (quest.getCurrentXp() >= quest.getType().getRequiredXp()) {
                spawnBoss(killer, quest.getType(), entity.getLocation());
                slayerManager.clearQuest(killer);
            }
        }
    }

    private void spawnBoss(Player player, SlayerType type, Location location) {
        player.sendMessage(ChatColor.RED + "The " + type.getBossName() + " has spawned!");
        Entity boss = location.getWorld().spawnEntity(location, type.getTargetType());
        if (boss instanceof LivingEntity livingBoss) {
            livingBoss.setCustomName(ChatColor.RED + type.getBossName());
            livingBoss.setCustomNameVisible(true);
            io.papermc.Grivience.util.SkyblockDamageScaleUtil.setHealthSafely(livingBoss, 1000.0);
            if (livingBoss.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
                livingBoss.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(20.0);
            }
            livingBoss.getPersistentDataContainer().set(bossKey, PersistentDataType.STRING, type.name());
        }
    }
}