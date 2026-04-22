package io.papermc.Grivience.elevator;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.gui.SkyblockGui;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ElevatorGui implements Listener {
    private final GriviencePlugin plugin;
    private final ElevatorManager manager;

    public ElevatorGui(GriviencePlugin plugin, ElevatorManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void open(Player player, Elevator elevator) {
        Inventory inv = Bukkit.createInventory(new ElevatorHolder(elevator), 45, SkyblockGui.title(elevator.getDisplayName()));

        // Fill background
        SkyblockGui.fillAll(inv, SkyblockGui.filler(Material.BLACK_STAINED_GLASS_PANE));
        
        // Add border lines (top and bottom)
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, SkyblockGui.filler(Material.GRAY_STAINED_GLASS_PANE));
            inv.setItem(36 + i, SkyblockGui.filler(Material.GRAY_STAINED_GLASS_PANE));
        }

        List<ElevatorFloor> floors = elevator.getFloors();
        
        // Determine starting slot to center floors if possible
        // For now, let's just place them in rows 2 and 3 (slots 10-16, 19-25)
        int[] floorSlots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
        };

        for (int i = 0; i < floors.size(); i++) {
            if (i >= floorSlots.length) break;
            inv.setItem(floorSlots[i], buildFloorItem(player, floors.get(i)));
        }

        // Close button at the bottom center
        inv.setItem(40, SkyblockGui.closeButton());

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 0.5f, 1.2f);
    }

    private ItemStack buildFloorItem(Player player, ElevatorFloor floor) {
        ItemStack item = floor.icon().clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            boolean isCurrent = isCurrentFloor(player, floor);
            boolean isUnlocked = true;
            
            io.papermc.Grivience.skyblock.profile.SkyBlockProfile profile = plugin.getProfileManager() != null ? 
                    plugin.getProfileManager().getSelectedProfile(player) : null;
            
            if (profile != null && floor.requiredLayer() != null && !floor.requiredLayer().isBlank()) {
                isUnlocked = profile.hasDiscoveredLayer(floor.requiredLayer());
            }

            meta.setDisplayName((isUnlocked ? ChatColor.GOLD : ChatColor.RED) + floor.name());
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Quickly travel to this floor.");
            lore.add("");
            
            if (!isUnlocked) {
                lore.add(ChatColor.RED + "\u2716 Locked");
                lore.add(ChatColor.GRAY + "Requires discovering: " + ChatColor.AQUA + ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', floor.requiredLayer())));
                item.setType(Material.GRAY_STAINED_GLASS);
            } else if (isCurrent) {
                lore.add(ChatColor.GREEN + "\u25CF You are currently here");
                item.setType(Material.LIME_STAINED_GLASS); // Visual indicator for current floor
            } else {
                lore.add(ChatColor.YELLOW + "Click to teleport!");
            }
            
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isCurrentFloor(Player player, ElevatorFloor floor) {
        if (floor.location() == null || !player.getWorld().equals(floor.location().getWorld())) {
            return false;
        }
        // Check if player is within 3 blocks of the floor location
        return player.getLocation().distanceSquared(floor.location()) < 9.0;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();
        
        if (top != null && top.getHolder() instanceof ElevatorHolder holder) {
            event.setCancelled(true);
            if (event.getClickedInventory() == null || !event.getClickedInventory().equals(top)) return;

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            // Check for Close button
            if (clicked.getType() == Material.BARRIER) {
                player.closeInventory();
                player.playSound(player.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 0.5f, 1.2f);
                return;
            }

            // Find which floor was clicked
            int slot = event.getRawSlot();
            Elevator elevator = holder.getElevator();
            List<ElevatorFloor> floors = elevator.getFloors();
            
            // Map slot back to floor index
            int floorIndex = -1;
            int[] floorSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
            };
            
            for (int i = 0; i < floorSlots.length; i++) {
                if (floorSlots[i] == slot) {
                    floorIndex = i;
                    break;
                }
            }
            
            if (floorIndex >= 0 && floorIndex < floors.size()) {
                ElevatorFloor floor = floors.get(floorIndex);
                
                // Requirement Check
                io.papermc.Grivience.skyblock.profile.SkyBlockProfile profile = plugin.getProfileManager() != null ? 
                        plugin.getProfileManager().getSelectedProfile(player) : null;
                if (profile != null && floor.requiredLayer() != null && !floor.requiredLayer().isBlank()) {
                    if (!profile.hasDiscoveredLayer(floor.requiredLayer())) {
                        player.sendMessage(ChatColor.RED + "You must discover " + ChatColor.AQUA + 
                                ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', floor.requiredLayer())) + 
                                ChatColor.RED + " before you can travel here!");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 0.8f);
                        return;
                    }
                }

                if (isCurrentFloor(player, floor)) {
                    player.sendMessage(ChatColor.RED + "You are already on this floor!");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f);
                    return;
                }

                player.teleport(floor.location());
                player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                player.sendMessage(ChatColor.GREEN + "Elevator: You arrived at " + ChatColor.GOLD + floor.name() + ChatColor.GREEN + "!");
                player.closeInventory();
            }
        }
    }

    public static class ElevatorHolder implements InventoryHolder {
        private final Elevator elevator;
        public ElevatorHolder(Elevator elevator) { this.elevator = elevator; }
        public Elevator getElevator() { return elevator; }
        @Override public Inventory getInventory() { return null; }
    }
}
