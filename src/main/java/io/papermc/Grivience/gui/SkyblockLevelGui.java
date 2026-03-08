package io.papermc.Grivience.gui;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.stats.SkyblockLevelManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 100% Accurate Hypixel Skyblock Leveling GUI (v0.19+)
 */
public final class SkyblockLevelGui implements Listener {
    private final GriviencePlugin plugin;
    private final SkyblockLevelManager levelManager;
    private final NamespacedKey actionKey;
    private final NamespacedKey valueKey;

    public SkyblockLevelGui(GriviencePlugin plugin, SkyblockLevelManager levelManager) {
        this.plugin = plugin;
        this.levelManager = levelManager;
        this.actionKey = new NamespacedKey(plugin, "skyblock-level-action");
        this.valueKey = new NamespacedKey(plugin, "skyblock-level-value");
    }

    public void openMainMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new LevelGuiHolder(LevelGuiType.MAIN), 54, ChatColor.DARK_GRAY + "SkyBlock Leveling");
        fillBackground(inventory);

        int level = levelManager.getLevel(player);
        long totalXp = levelManager.getXp(player);
        long intoLevel = levelManager.xpIntoCurrentLevel(player);
        long perLevel = levelManager.getXpPerLevel();
        long toNext = levelManager.xpToNextLevel(player);
        
        // Slot 13: SkyBlock Level (Experience Bottle)
        List<String> levelLore = new ArrayList<>();
        levelLore.add(ChatColor.GRAY + "Total XP: " + ChatColor.GOLD + fmt(totalXp));
        levelLore.add("");
        levelLore.add(ChatColor.GRAY + "Progress to Level " + (level + 1) + ": " + ChatColor.YELLOW + Math.round((intoLevel * 100.0) / perLevel) + "%");
        levelLore.add(progressBar(levelManager.getProgress(player), 20));
        levelLore.add(ChatColor.GRAY + "(" + fmt(intoLevel) + "/" + fmt(perLevel) + ")");
        levelLore.add("");
        levelLore.add(ChatColor.GOLD + "Skyblock Level Bonuses:");
        levelLore.add(ChatColor.GRAY + " \u25cf " + ChatColor.RED + "+" + levelManager.getHealthBonus(player) + " Health");
        levelLore.add(ChatColor.GRAY + " \u25cf " + ChatColor.RED + "+" + levelManager.getStrengthBonus(player) + " Strength");
        levelLore.add("");
        levelLore.add(ChatColor.YELLOW + "Click to view rewards!");

        inventory.setItem(13, createItem(Material.EXPERIENCE_BOTTLE, 
                levelManager.getLevelColor(level) + "SkyBlock Level " + level, levelLore, "open_rewards", ""));

        // Slot 11: SkyBlock Guide (Nether Star)
        List<String> guideLore = new ArrayList<>();
        guideLore.add(ChatColor.GRAY + "The SkyBlock Guide helps you");
        guideLore.add(ChatColor.GRAY + "progress through the game.");
        guideLore.add("");
        guideLore.add(ChatColor.GRAY + "Completed: " + ChatColor.GREEN + levelManager.claimedMilestones(player) + "/" + levelManager.totalMilestones());
        guideLore.add("");
        guideLore.add(ChatColor.YELLOW + "Click to view guide!");
        inventory.setItem(11, createItem(Material.NETHER_STAR, ChatColor.GREEN + "SkyBlock Guide", guideLore, "open_guide", ""));

        // Slot 15: Ways to Level Up (Chest)
        List<String> waysLore = new ArrayList<>();
        waysLore.add(ChatColor.GRAY + "View all the ways you can");
        waysLore.add(ChatColor.GRAY + "earn SkyBlock XP.");
        waysLore.add("");
        for (SkyblockLevelManager.GuideTrack track : levelManager.tracks()) {
            SkyblockLevelManager.GuideProgress progress = levelManager.progressFor(player, track);
            if (progress != null) {
                waysLore.add(levelManager.color(track.displayName()) + ChatColor.GRAY + ": " + ChatColor.AQUA + fmt(progress.counterValue()) + " XP");
            }
        }
        waysLore.add("");
        waysLore.add(ChatColor.YELLOW + "Click to view details!");
        inventory.setItem(15, createItem(Material.CHEST, ChatColor.GREEN + "Ways to Level Up", waysLore, "open_ways", ""));

        // Slot 49: Back (Arrow)
        inventory.setItem(49, createItem(Material.ARROW, ChatColor.GREEN + "Go Back", List.of(ChatColor.GRAY + "To SkyBlock Menu"), "back_to_main", ""));

        player.openInventory(inventory);
    }

    public void openGuideMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new LevelGuiHolder(LevelGuiType.GUIDE), 54, ChatColor.DARK_GRAY + "SkyBlock Guide");
        fillBackground(inventory);

        List<SkyblockLevelManager.GuideTrack> tracks = levelManager.tracks();
        int[] slots = {19, 20, 21, 22, 23, 24, 25, 29, 30, 31, 32, 33, 34};
        
        for (int i = 0; i < tracks.size() && i < slots.length; i++) {
            SkyblockLevelManager.GuideTrack track = tracks.get(i);
            SkyblockLevelManager.GuideProgress progress = levelManager.progressFor(player, track);
            
            List<String> lore = new ArrayList<>();
            for (String line : track.lore()) {
                lore.add(levelManager.color(line));
            }
            lore.add("");
            lore.add(ChatColor.GRAY + "Progress: " + ChatColor.GREEN + progress.claimedMilestones() + "/" + progress.totalMilestones());
            lore.add(progressBar(progress.progressToNext(), 18));
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to view milestones!");
            
            inventory.setItem(slots[i], createItem(track.icon(), levelManager.color(track.displayName()), lore, "open_track", track.id()));
        }

        inventory.setItem(49, createItem(Material.ARROW, ChatColor.GREEN + "Go Back", List.of(ChatColor.GRAY + "To SkyBlock Leveling"), "open_main", ""));
        player.openInventory(inventory);
    }

    public void openTrackMenu(Player player, String trackId) {
        SkyblockLevelManager.GuideTrack track = levelManager.track(trackId);
        if (track == null) return;

        Inventory inventory = Bukkit.createInventory(new LevelGuiHolder(LevelGuiType.TRACK), 54, ChatColor.DARK_GRAY + ChatColor.stripColor(levelManager.color(track.displayName())));
        fillBackground(inventory);

        SkyblockLevelManager.GuideProgress progress = levelManager.progressFor(player, track);
        
        int[] milestoneSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        for (int i = 0; i < track.milestones().size() && i < milestoneSlots.length; i++) {
            boolean claimed = i < progress.claimedMilestones();
            long threshold = track.milestones().get(i);
            long reward = track.rewards().get(i);
            
            Material material = claimed ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
            String name = (claimed ? ChatColor.GREEN : ChatColor.RED) + "Milestone " + (i + 1);
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Requirement: " + ChatColor.AQUA + fmt(threshold) + " XP");
            if (reward > 0) {
                lore.add(ChatColor.GRAY + "Reward: " + ChatColor.GOLD + "+" + reward + " SkyBlock XP");
            }
            lore.add("");
            lore.add(claimed ? ChatColor.GREEN + "UNLOCKED" : ChatColor.RED + "LOCKED");
            
            inventory.setItem(milestoneSlots[i], createItem(material, name, lore, "noop", ""));
        }

        inventory.setItem(49, createItem(Material.ARROW, ChatColor.GREEN + "Go Back", List.of(ChatColor.GRAY + "To SkyBlock Guide"), "open_guide", ""));
        player.openInventory(inventory);
    }

    public void openWaysToLevelMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new LevelGuiHolder(LevelGuiType.WAYS), 54, ChatColor.DARK_GRAY + "Ways to Level Up");
        fillBackground(inventory);

        List<SkyblockLevelManager.GuideTrack> tracks = levelManager.tracks();
        int[] slots = {19, 20, 21, 22, 23, 24, 25, 29, 30, 31, 32, 33, 34};
        
        long totalXp = levelManager.getXp(player);
        long trackedXp = 0;

        for (int i = 0; i < tracks.size() && i < slots.length; i++) {
            SkyblockLevelManager.GuideTrack track = tracks.get(i);
            SkyblockLevelManager.GuideProgress progress = levelManager.progressFor(player, track);
            trackedXp += progress.counterValue();
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Total XP from this category:");
            lore.add(ChatColor.AQUA + fmt(progress.counterValue()) + " XP");
            lore.add("");
            lore.add(ChatColor.GRAY + "Includes milestones and");
            lore.add(ChatColor.GRAY + "specific tasks.");
            
            inventory.setItem(slots[i], createItem(track.icon(), levelManager.color(track.displayName()), lore, "noop", ""));
        }

        long otherXp = totalXp - trackedXp;
        if (otherXp > 0) {
            inventory.setItem(40, createItem(Material.PAPER, ChatColor.YELLOW + "Other XP", 
                    List.of(ChatColor.GRAY + "XP from miscellaneous sources:", ChatColor.AQUA + fmt(otherXp) + " XP"), "noop", ""));
        }

        inventory.setItem(49, createItem(Material.ARROW, ChatColor.GREEN + "Go Back", List.of(ChatColor.GRAY + "To SkyBlock Leveling"), "open_main", ""));
        player.openInventory(inventory);
    }

    public void openRewardsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new LevelGuiHolder(LevelGuiType.REWARDS), 54, ChatColor.DARK_GRAY + "Leveling Rewards");
        fillBackground(inventory);

        int level = levelManager.getLevel(player);
        int startLevel = Math.max(1, ((level / 10) * 10) - 10);
        
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        
        for (int i = 0; i < slots.length; i++) {
            int displayLevel = startLevel + i;
            if (displayLevel > levelManager.getMaxLevel()) break;

            boolean reached = displayLevel <= level;
            Material mat = reached ? Material.GOLD_BLOCK : Material.IRON_BLOCK;
            String name = (reached ? ChatColor.GREEN : ChatColor.RED) + "Level " + displayLevel;
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Rewards:");
            lore.add(ChatColor.GRAY + " \u25cf " + ChatColor.RED + "+5 Health");
            if (displayLevel % 5 == 0) {
                lore.add(ChatColor.GRAY + " \u25cf " + ChatColor.RED + "+1 Strength");
            }
            
            List<String> unlocks = levelManager.featureUnlocksAtLevel(displayLevel);
            for (String unlock : unlocks) {
                lore.add(ChatColor.GRAY + " \u25cf " + ChatColor.AQUA + "Unlock: " + unlock);
            }
            
            lore.add("");
            lore.add(reached ? ChatColor.GREEN + "REACHED" : ChatColor.RED + "LOCKED");
            
            inventory.setItem(slots[i], createItem(mat, name, lore, "noop", ""));
        }

        inventory.setItem(49, createItem(Material.ARROW, ChatColor.GREEN + "Go Back", List.of(ChatColor.GRAY + "To SkyBlock Leveling"), "open_main", ""));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof LevelGuiHolder holder)) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        String value = meta.getPersistentDataContainer().get(valueKey, PersistentDataType.STRING);

        if (action == null || action.equals("noop")) return;

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);

        switch (action) {
            case "open_main" -> openMainMenu(player);
            case "open_guide" -> openGuideMenu(player);
            case "open_ways" -> openWaysToLevelMenu(player);
            case "open_rewards" -> openRewardsMenu(player);
            case "open_track" -> openTrackMenu(player, value);
            case "back_to_main" -> plugin.getSkyblockMenuManager().openMainMenu(player);
        }
    }

    private ItemStack createItem(Material material, String name, List<String> lore, String action, String value) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, value);
        item.setItemMeta(meta);
        return item;
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of(), "noop", "");
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    private String progressBar(double progress, int length) {
        StringBuilder sb = new StringBuilder(ChatColor.DARK_GRAY + "[");
        int filled = (int) (progress * length);
        sb.append(ChatColor.GREEN);
        for (int i = 0; i < filled; i++) sb.append("-");
        sb.append(ChatColor.WHITE);
        for (int i = filled; i < length; i++) sb.append("-");
        sb.append(ChatColor.DARK_GRAY).append("]");
        return sb.toString();
    }

    private String fmt(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    private enum LevelGuiType { MAIN, GUIDE, TRACK, WAYS, REWARDS }
    private static class LevelGuiHolder implements InventoryHolder {
        private final LevelGuiType type;
        LevelGuiHolder(LevelGuiType type) { this.type = type; }
        @Override public Inventory getInventory() { return null; }
    }
}
