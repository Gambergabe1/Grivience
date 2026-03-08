package io.papermc.Grivience.welcome;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.mines.MiningEventManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.Inventory;

/**
 * Listener for welcome event interactions.
 * Optional ZNPCS integration for NPC right-click.
 */
public class WelcomeListener implements Listener {
    private final GriviencePlugin plugin;
    private final WelcomeEventManager welcomeManager;

    public WelcomeListener(GriviencePlugin plugin, WelcomeEventManager welcomeManager) {
        this.plugin = plugin;
        this.welcomeManager = welcomeManager;
    }

    /**
     * Handle player join - direct them to Hard Hat Harry.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!welcomeManager.hasClaimedRewards(player.getUniqueId())) {
            Location npcLoc = welcomeManager.getSpawnLocation();
            player.sendMessage("");
            player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Welcome to Skyblock!");
            if (npcLoc != null && npcLoc.getWorld() != null) {
                player.sendMessage(ChatColor.GRAY + "Claim starter rewards from " + ChatColor.GOLD + "Hard Hat Harry"
                        + ChatColor.GRAY + " at "
                        + ChatColor.YELLOW + npcLoc.getWorld().getName()
                        + ChatColor.GRAY + " (" + (int) npcLoc.getX() + ", " + (int) npcLoc.getY() + ", " + (int) npcLoc.getZ() + ").");
            } else {
                player.sendMessage(ChatColor.GRAY + "Claim starter rewards with " + ChatColor.YELLOW + "/welcome" + ChatColor.GRAY + ".");
            }
            player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/welcome status" + ChatColor.GRAY + " to check claim status.");
            player.sendMessage("");

            if (player.isOnline()) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline() && !welcomeManager.hasClaimedRewards(player.getUniqueId())) {
                        player.sendMessage(ChatColor.GRAY + "Tip: " + ChatColor.YELLOW + "/welcome" + ChatColor.GRAY + " opens your starter claim menu.");
                    }
                }, 300L);
            }
        }
    }

    /**
     * Handle welcome GUI clicks.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        if (top == null || !(top.getHolder() instanceof WelcomeEventManager.WelcomeHolder)) {
            return;
        }

        event.setCancelled(true); // Prevent item movement

        if (event.getRawSlot() == 13) {
            if (welcomeManager.hasClaimedRewards(player.getUniqueId())) {
                player.sendMessage("");
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Already Claimed!");
                player.sendMessage(ChatColor.GRAY + "You have already claimed your welcome rewards.");
                player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/welcome status" + ChatColor.GRAY + " to check your boosts.");
                player.sendMessage("");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            } else {
                welcomeManager.claimRewards(player);
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    welcomeManager.openWelcomeGUI(player);
                }, 5L);
            }
        }
    }

    /**
     * Handle NPC right-click - open welcome GUI (ZNPCS).
     */
    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        // Check if ZNPCS is available
        if (!plugin.getServer().getPluginManager().isPluginEnabled("ZNPCs")) {
            return;
        }

        try {
            // Use reflection to check if the clicked entity is a ZNPC
            Object clickedEntity = event.getRightClicked();
            Class<?> entityClass = clickedEntity.getClass();

            // Check if entity has ZNPC metadata
            if (welcomeManager.getHardHatHarry() != null) {
                // Get NPC ID from Hard Hat Harry
                Class<?> npcClass = welcomeManager.getHardHatHarry().getClass();
                java.lang.reflect.Method getIdMethod = npcClass.getMethod("getId");
                Object harryId = getIdMethod.invoke(welcomeManager.getHardHatHarry());

                // Get clicked entity ID
                java.lang.reflect.Method getEntityIdMethod = entityClass.getMethod("getEntityId");
                Object clickedId = getEntityIdMethod.invoke(clickedEntity);

                // Check if clicked entity is Hard Hat Harry
                if (harryId != null && harryId.equals(clickedId)) {
                    event.setCancelled(true);
                    // Open welcome GUI
                    welcomeManager.openWelcomeGUI(player);
                    return;
                }
            }

            // Also check for King Ironcrest NPC from Mining Event
            if (plugin.getMiningEventManager() != null) {
                MiningEventManager miningManager = plugin.getMiningEventManager();
                Object kingNpc = miningManager.getKingsInspectionHook() != null ? miningManager.getKingsInspectionHook().getActiveNpc() : null;
                
                if (kingNpc != null) {
                    Class<?> npcClass = kingNpc.getClass();
                    java.lang.reflect.Method getIdMethod = npcClass.getMethod("getId");
                    Object kingId = getIdMethod.invoke(kingNpc);

                    java.lang.reflect.Method getEntityIdMethod = entityClass.getMethod("getEntityId");
                    Object clickedId = getEntityIdMethod.invoke(clickedEntity);

                    if (kingId != null && kingId.equals(clickedId)) {
                        event.setCancelled(true);
                        miningManager.getKingsInspectionHook().handleInteraction(player);
                    }
                }
            }
        } catch (Exception ignored) {
            // ZNPCS integration not available or failed
        }
    }
}
