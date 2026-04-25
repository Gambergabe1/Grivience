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
 * SkyBlock Leveling GUI.
 */
public final class SkyblockLevelGui implements Listener {
    private static final String TITLE = SkyblockGui.title("SkyBlock Leveling");

    private final GriviencePlugin plugin;
    private final SkyblockLevelManager levelManager;
    private final NamespacedKey actionKey;
    private final NamespacedKey valueKey;

    public enum LevelTab { MAIN, GUIDE, WAYS, REWARDS }

    public SkyblockLevelGui(GriviencePlugin plugin, SkyblockLevelManager levelManager) {
        this.plugin = plugin;
        this.levelManager = levelManager;
        this.actionKey = new NamespacedKey(plugin, "skyblock-level-action");
        this.valueKey = new NamespacedKey(plugin, "skyblock-level-value");
    }

    public void openMenu(Player player) {
        openMenu(player, LevelTab.MAIN, null);
    }

    public void openMenu(Player player, LevelTab tab, String selectedTrackId) {
        Inventory inventory = Bukkit.createInventory(new LevelGuiHolder(tab, selectedTrackId), 54, TITLE);
        fillBackground(inventory);

        // Top Row: Tabs
        inventory.setItem(1, createTabItem(LevelTab.MAIN, tab == LevelTab.MAIN));
        inventory.setItem(3, createTabItem(LevelTab.GUIDE, tab == LevelTab.GUIDE));
        inventory.setItem(5, createTabItem(LevelTab.WAYS, tab == LevelTab.WAYS));
        inventory.setItem(7, createTabItem(LevelTab.REWARDS, tab == LevelTab.REWARDS));

        switch (tab) {
            case MAIN -> renderMain(player, inventory);
            case GUIDE -> renderGuide(player, inventory, selectedTrackId);
            case WAYS -> renderWays(player, inventory);
            case REWARDS -> renderRewards(player, inventory);
        }

        inventory.setItem(49, createItem(Material.BARRIER, ChatColor.RED + "Close", List.of(), "close", ""));
        player.openInventory(inventory);
    }

    private ItemStack createTabItem(LevelTab tab, boolean active) {
        Material material = switch (tab) {
            case MAIN -> Material.EXPERIENCE_BOTTLE;
            case GUIDE -> Material.NETHER_STAR;
            case WAYS -> Material.CHEST;
            case REWARDS -> Material.BOOK;
        };
        String name = (active ? ChatColor.GREEN : ChatColor.GRAY) + switch (tab) {
            case MAIN -> "Overview";
            case GUIDE -> "SkyBlock Guide";
            case WAYS -> "Ways to Level";
            case REWARDS -> "Rewards";
        };
        List<String> lore = List.of(ChatColor.GRAY + "Click to view " + name.toLowerCase());
        ItemStack item = createItem(material, name, lore, "switch_tab", tab.name(), active);
        if (active) {
            ItemMeta meta = item.getItemMeta();
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void renderMain(Player player, Inventory inv) {
        int level = levelManager.getLevel(player);
        long totalXp = levelManager.getXp(player);
        double progress = clampProgress(levelManager.getProgress(player));
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Level: " + levelManager.getLevelColor(level) + level);
        lore.add(ChatColor.GRAY + "Total XP: " + ChatColor.GOLD + fmt(totalXp));
        lore.add(progressBar(progress, 20));
        lore.add("");
        lore.add(ChatColor.GRAY + "Next Level in: " + ChatColor.AQUA + fmt(levelManager.xpToNextLevel(player)) + " XP");
        
        inv.setItem(22, createItem(Material.EXPERIENCE_BOTTLE, ChatColor.GOLD + "SkyBlock Level " + level, lore, "noop", "", true));
    }

    private void renderGuide(Player player, Inventory inv, String selectedTrackId) {
        List<SkyblockLevelManager.GuideTrack> tracks = levelManager.tracks();
        String currentTrackId = selectedTrackId != null ? selectedTrackId : (tracks.isEmpty() ? null : tracks.get(0).id());
        
        // Render track selector on the left (column 0)
        int[] trackSlots = {9, 18, 27, 36, 45};
        for (int i = 0; i < tracks.size() && i < trackSlots.length; i++) {
            SkyblockLevelManager.GuideTrack track = tracks.get(i);
            boolean active = track.id().equals(currentTrackId);
            inv.setItem(trackSlots[i], createItem(track.icon(), (active ? ChatColor.GREEN : ChatColor.GRAY) + track.displayName(), 
                    List.of(ChatColor.GRAY + "Click to select category"), "select_track", track.id(), active));
        }

        // Render milestones for selected track in the center/right
        if (currentTrackId != null) {
            SkyblockLevelManager.GuideTrack track = levelManager.track(currentTrackId);
            if (track != null) {
                SkyblockLevelManager.GuideProgress progress = safeProgress(player, track);
                int[] milestoneSlots = {
                    11, 12, 13, 14, 15, 16,
                    20, 21, 22, 23, 24, 25,
                    29, 30, 31, 32, 33, 34,
                    38, 39, 40, 41, 42, 43
                };
                
                for (int i = 0; i < track.milestones().size() && i < milestoneSlots.length; i++) {
                    boolean claimed = i < progress.claimedMilestones();
                    boolean current = i == progress.claimedMilestones() && !progress.completed();
                    long threshold = track.milestones().get(i);
                    Material mat = claimed ? Material.LIME_STAINED_GLASS_PANE : (current ? Material.YELLOW_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE);
                    inv.setItem(milestoneSlots[i], createItem(mat, (claimed ? ChatColor.GREEN : (current ? ChatColor.YELLOW : ChatColor.RED)) + "Milestone " + (i+1),
                            List.of(ChatColor.GRAY + "Requires: " + ChatColor.AQUA + fmt(threshold) + " XP"), "noop", "", claimed));
                }
            }
        }
    }

    private void renderWays(Player player, Inventory inv) {
        List<SkyblockLevelManager.GuideTrack> tracks = levelManager.tracks();
        int[] slots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        for (int i = 0; i < tracks.size() && i < slots.length; i++) {
            SkyblockLevelManager.GuideTrack track = tracks.get(i);
            SkyblockLevelManager.GuideProgress prog = safeProgress(player, track);
            inv.setItem(slots[i], createItem(track.icon(), levelManager.color(track.displayName()), 
                    List.of(ChatColor.GRAY + "XP: " + ChatColor.AQUA + fmt(prog.counterValue())), "noop", ""));
        }
    }

    private void renderRewards(Player player, Inventory inv) {
        int level = levelManager.getLevel(player);
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        for (int i = 0; i < slots.length; i++) {
            int lv = i + 1;
            boolean reached = lv <= level;
            Material mat = reached ? Material.GOLD_BLOCK : (lv == level + 1 ? Material.YELLOW_CONCRETE : Material.IRON_BLOCK);
            inv.setItem(slots[i], createItem(mat, (reached ? ChatColor.GREEN : ChatColor.RED) + "Level " + lv, 
                    List.of(ChatColor.GRAY + "Reward: +5 Health"), "noop", "", reached));
        }
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
            case "switch_tab" -> openMenu(player, LevelTab.valueOf(value), null);
            case "select_track" -> openMenu(player, LevelTab.GUIDE, value);
            case "close" -> player.closeInventory();
        }
    }

    private ItemStack createItem(Material material, String name, List<String> lore, String action, String value) {
        return createItem(material, name, lore, action, value, false);
    }

    private ItemStack createItem(Material material, String name, List<String> lore, String action, String value, boolean glow) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        meta.setLore(lore);
        if (glow) meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, value);
        item.setItemMeta(meta);
        return item;
    }

    private void fillBackground(Inventory inventory) {
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of(), "noop", "");
        ItemStack fill = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of(), "noop", "");
        for (int i = 0; i < inventory.getSize(); i++) {
            int row = i / 9, col = i % 9;
            boolean isBorder = row == 0 || row == 5 || col == 0 || col == 8;
            inventory.setItem(i, (isBorder ? border : fill).clone());
        }
    }

    private String progressBar(double progress, int length) {
        int filled = (int) (progress * length);
        StringBuilder bar = new StringBuilder(ChatColor.DARK_GRAY + "[");
        for (int i = 0; i < length; i++) bar.append(i < filled ? ChatColor.GREEN : ChatColor.GRAY).append("|");
        return bar.append(ChatColor.DARK_GRAY).append("]").toString();
    }

    private double clampProgress(double p) { return Double.isFinite(p) ? Math.max(0.0, Math.min(1.0, p)) : 0.0; }
    private String fmt(long v) { return String.format(Locale.US, "%,d", v); }

    private SkyblockLevelManager.GuideProgress safeProgress(Player player, SkyblockLevelManager.GuideTrack track) {
        SkyblockLevelManager.GuideProgress progress = levelManager.progressFor(player, track);
        if (progress != null) return progress;
        int total = track.milestones().size();
        return new SkyblockLevelManager.GuideProgress(track, 0L, 0, total, 0L, total > 0 ? track.milestones().get(0) : 0L, 0L, 0.0D, false);
    }

    private static class LevelGuiHolder implements InventoryHolder {
        private final LevelTab tab;
        private final String selectedTrackId;
        LevelGuiHolder(LevelTab tab, String trackId) { this.tab = tab; this.selectedTrackId = trackId; }
        @Override public Inventory getInventory() { return null; }
    }
}
