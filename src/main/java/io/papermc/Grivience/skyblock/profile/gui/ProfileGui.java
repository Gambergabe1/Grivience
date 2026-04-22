package io.papermc.Grivience.skyblock.profile.gui;

import io.papermc.Grivience.gui.SkyblockGui;
import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Skyblock-accurate Profile Selection GUI.
 */
public final class ProfileGui implements Listener {
    private static final String TITLE = SkyblockGui.title("Profile Management");
    private static final int[] PROFILE_SLOTS = {10, 12, 14, 16, 28};
    
    private final ProfileManager profileManager;

    public ProfileGui(ProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    /**
     * Open the profile management GUI.
     */
    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(new Holder(), 54, TITLE);
        
        // Fill background
        fillBackground(inventory);
        
        // Header
        setItem(inventory, 4, createHeaderItem(player));
        
        // Profile slots
        List<SkyBlockProfile> profiles = profileManager.getPlayerProfiles(player);
        SkyBlockProfile selected = profileManager.getSelectedProfile(player);
        
        for (int i = 0; i < Math.min(profiles.size(), PROFILE_SLOTS.length); i++) {
            SkyBlockProfile profile = profiles.get(i);
            setItem(inventory, PROFILE_SLOTS[i], createProfileItem(profile, profile.equals(selected)));
        }
        
        // Empty profile slots (create new)
        for (int i = profiles.size(); i < PROFILE_SLOTS.length; i++) {
            setItem(inventory, PROFILE_SLOTS[i], createEmptyProfileItem(i + 1));
        }
        
        // Action buttons (Skyblock-style bottom row)
        setItem(inventory, 48, createBackItem());
        setItem(inventory, 49, SkyblockGui.closeButton());
        setItem(inventory, 50, createCreateItem(player));
        setItem(inventory, 53, createInfoItem());
        
        player.openInventory(inventory);
    }

    private void fillBackground(Inventory inventory) {
        SkyblockGui.fillAll(inventory, SkyblockGui.filler(Material.BLACK_STAINED_GLASS_PANE));
        ItemStack border = SkyblockGui.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            boolean top = slot < 9;
            boolean bottom = slot >= inventory.getSize() - 9;
            boolean left = slot % 9 == 0;
            boolean right = slot % 9 == 8;
            if (top || bottom || left || right) {
                inventory.setItem(slot, border.clone());
            }
        }
    }

    private ItemStack createGlassItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createHeaderItem(Player player) {
        ItemStack item = new ItemStack(Material.BOOKSHELF);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Skyblock Profiles");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Manage your Skyblock profiles");
        lore.add(ChatColor.GRAY + "Each profile has separate:");
        lore.add(ChatColor.AQUA + "  • Island");
        lore.add(ChatColor.AQUA + "  • Inventory");
        lore.add(ChatColor.AQUA + "  • Collections");
        lore.add(ChatColor.AQUA + "  • Skills");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Max Profiles: " + profileManager.getMaxProfiles(player));
        
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createProfileItem(SkyBlockProfile profile, boolean selected) {
        Material icon = getProfileIcon(profile.getProfileIcon());
        ItemStack item = new ItemStack(icon);
        item.setAmount(1);
        
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((selected ? ChatColor.GREEN : ChatColor.GRAY) + 
            (selected ? "● " : "○ ") + ChatColor.BOLD + profile.getProfileName());
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Created: " + ChatColor.WHITE + profile.getFormattedCreationDate());
        lore.add(ChatColor.GRAY + "Playtime: " + ChatColor.YELLOW + profile.getFormattedPlaytime());
        lore.add("");
        lore.add(ChatColor.AQUA + "Average Skill Level: " + ChatColor.YELLOW + getAverageSkillLevel(profile));
        lore.add(ChatColor.GOLD + "Purse: " + ChatColor.YELLOW + String.format("%.1f", profile.getPurse()));
        lore.add("");
        
        if (selected) {
            lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "Currently Selected");
            lore.add(ChatColor.GREEN + "Click to view details");
        } else {
            lore.add(ChatColor.YELLOW + "Click to select");
            lore.add(ChatColor.RED + "Right-click to delete");
        }
        
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEmptyProfileItem(int slotNumber) {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Create Profile " + slotNumber);
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Click to create a new");
        lore.add(ChatColor.GRAY + "Skyblock profile.");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Each profile has:");
        lore.add(ChatColor.AQUA + "  • Separate island");
        lore.add(ChatColor.AQUA + "  • Separate inventory");
        lore.add(ChatColor.AQUA + "  • Separate progress");
        lore.add("");
        lore.add(ChatColor.GREEN + "Click to create!");
        
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCreateItem(Player player) {
        ItemStack item = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Create New Profile");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Create a new Skyblock profile");
        lore.add(ChatColor.GRAY + "with fresh progress.");
        lore.add("");
        lore.add(ChatColor.YELLOW + "You can have up to " + 
            profileManager.getMaxProfiles(player) + " profiles.");
        lore.add("");
        lore.add(ChatColor.GREEN + "Click to create!");
        
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfoItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Profile Information");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "View detailed information");
        lore.add(ChatColor.GRAY + "about your profiles.");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Includes:");
        lore.add(ChatColor.AQUA + "  • Statistics");
        lore.add(ChatColor.AQUA + "  • Skill levels");
        lore.add(ChatColor.AQUA + "  • Collection progress");
        lore.add("");
        lore.add(ChatColor.GREEN + "Click to view!");
        
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Back");
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Return to previous menu.");
        
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private Material getProfileIcon(String iconString) {
        try {
            return Material.valueOf(iconString);
        } catch (IllegalArgumentException e) {
            return Material.PLAYER_HEAD;
        }
    }

    private int getAverageSkillLevel(SkyBlockProfile profile) {
        return (int) profile.getSkillLevels().values().stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0.0);
    }

    private void setItem(Inventory inventory, int slot, ItemStack item) {
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder)) return;
        
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null) return;
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        if (!clicked.hasItemMeta()) return;
        
        String displayName = clicked.getItemMeta().getDisplayName();
        if (displayName == null) return;

        // Close button
        if (displayName.contains("Close")) {
            player.closeInventory();
            return;
        }
         
        // Handle profile selection
        if (displayName.contains("●") || displayName.contains("○")) {
            String profileName = ChatColor.stripColor(displayName).replace("● ", "").replace("○ ", "").trim();
            
            if (event.isLeftClick()) {
                profileManager.selectProfile(player, profileName);
                open(player); // Refresh GUI
            } else if (event.isRightClick()) {
                player.sendMessage(ChatColor.RED + "Profile deletion via GUI is disabled for safety.");
                player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.GREEN + "/profile delete " + profileName + 
                    ChatColor.GRAY + " instead.");
            }
            return;
        }
        
        // Handle create new profile
        if (displayName.contains("Create Profile")) {
            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + "Type the profile name in chat:");
            // In a full implementation, we'd use SignGUI or chat input
            player.sendMessage(ChatColor.GRAY + "Use: " + ChatColor.GREEN + "/profile create <name>");
            return;
        }
        
        // Handle create new profile button
        if (displayName.contains("Create New Profile")) {
            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + "Type the profile name in chat:");
            player.sendMessage(ChatColor.GRAY + "Use: " + ChatColor.GREEN + "/profile create <name>");
            return;
        }
        
        // Handle info button
        if (displayName.contains("Profile Information")) {
            player.closeInventory();
            player.performCommand("profile info");
            return;
        }
        
        // Handle back button
        if (displayName.contains("Back")) {
            player.closeInventory();
            player.performCommand("skyblock menu");
            return;
        }
    }

    private static final class Holder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}

