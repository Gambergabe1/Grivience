package io.papermc.Grivience.welcome.quest;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Enhanced GUI for viewing and tracking welcome quest progress.
 * Features a beautiful, Skyblock-style layout with visual progress indicators.
 */
public class QuestGuiManager implements Listener {
    private static final String TITLE = ChatColor.translateAlternateColorCodes('&', "&5&lWelcome Quest Line");
    private final GriviencePlugin plugin;
    private final QuestProgressManager progressManager;

    public QuestGuiManager(GriviencePlugin plugin, QuestProgressManager progressManager) {
        this.plugin = plugin;
        this.progressManager = progressManager;
    }

    /**
     * Open the enhanced quest book GUI for a player.
     */
    public void openQuestBook(Player player) {
        QuestBookHolder holder = new QuestBookHolder();
        Inventory inventory = Bukkit.createInventory(holder, 54, TITLE);
        holder.inventory = inventory;

        // Create decorative background
        createDecorativeBackground(inventory);

        // Header section (top row)
        createHeaderSection(inventory, player);

        // Progress bar (row 2)
        createProgressBar(inventory, player);

        // Quest grid (rows 3-6)
        createQuestGrid(inventory, player);

        // Footer section (bottom row)
        createFooterSection(inventory, player);

        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.2F);
    }

    /**
     * Create a decorative background with stained glass.
     */
    private void createDecorativeBackground(Inventory inventory) {
        // Top border - purple glass
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, createDecorativeItem(Material.PURPLE_STAINED_GLASS_PANE, " "));
        }

        // Bottom border - purple glass
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, createDecorativeItem(Material.PURPLE_STAINED_GLASS_PANE, " "));
        }

        // Side borders - purple glass
        for (int i = 0; i < 6; i++) {
            inventory.setItem(i * 9, createDecorativeItem(Material.PURPLE_STAINED_GLASS_PANE, " "));
            inventory.setItem(i * 9 + 8, createDecorativeItem(Material.PURPLE_STAINED_GLASS_PANE, " "));
        }

        // Inner background - gray glass
        for (int row = 1; row < 5; row++) {
            for (int col = 1; col < 7; col++) {
                int slot = row * 9 + col;
                if (inventory.getItem(slot) == null) {
                    inventory.setItem(slot, createDecorativeItem(Material.GRAY_STAINED_GLASS_PANE, " "));
                }
            }
        }
    }

    /**
     * Create the header section with quest line info.
     */
    private void createHeaderSection(Inventory inventory, Player player) {
        // Title item (slot 4)
        inventory.setItem(4, createTitleItem(player));

        // Completed quests counter (slot 2)
        inventory.setItem(2, createCounterItem(
            Material.LIME_STAINED_GLASS_PANE,
            "&a&lCompleted",
            "&7Quests Finished",
            "&e" + progressManager.getTotalCompleted(player),
            "&8/&7 " + WelcomeQuestRegistry.count()
        ));

        // Available quests counter (slot 6)
        int available = progressManager.getAvailableQuests(player).size();
        inventory.setItem(6, createCounterItem(
            Material.LIGHT_BLUE_STAINED_GLASS_PANE,
            "&b&lAvailable",
            "&7Quests Ready",
            "&e" + available,
            "&8quests"
        ));
    }

    /**
     * Create a progress bar showing overall completion.
     */
    private void createProgressBar(Inventory inventory, Player player) {
        int completed = progressManager.getTotalCompleted(player);
        int total = WelcomeQuestRegistry.count();
        double percentage = (double) completed / total;

        // Progress bar center (slot 13)
        inventory.setItem(13, createProgressItem(completed, total, percentage));

        // Left progress indicator (slot 11)
        inventory.setItem(11, createProgressIndicator(percentage, true));

        // Right progress indicator (slot 15)
        inventory.setItem(15, createProgressIndicator(percentage, false));
    }

    /**
     * Create the quest grid showing all available quests.
     */
    private void createQuestGrid(Inventory inventory, Player player) {
        Set<WelcomeQuest> available = progressManager.getAvailableQuests(player);
        Set<String> completed = progressManager.getCompletedQuests(player);

        // Quest slots in a nice grid pattern
        int[] questSlots = {
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        };

        int slotIndex = 0;
        for (WelcomeQuest quest : WelcomeQuestRegistry.getAll()) {
            if (slotIndex >= questSlots.length) break;

            int slot = questSlots[slotIndex];
            boolean isCompleted = completed.contains(quest.getId());
            boolean isAvailable = available.contains(quest);

            inventory.setItem(slot, createQuestItem(quest, isCompleted, isAvailable, player));
            slotIndex++;
        }

        // Fill empty quest slots with placeholder
        for (int i = slotIndex; i < questSlots.length; i++) {
            inventory.setItem(questSlots[i], createPlaceholderItem());
        }
    }

    /**
     * Create the footer section with navigation and info.
     */
    private void createFooterSection(Inventory inventory, Player player) {
        // Quest book info (slot 48)
        inventory.setItem(48, createInfoItem());

        // Close button (slot 49)
        inventory.setItem(49, createCloseButton());

        // Refresh button (slot 50)
        inventory.setItem(50, createRefreshButton(player));
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof QuestBookHolder)) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 0.8F);
            return;
        }

        if (clicked.getType() == Material.CLOCK) {
            // Refresh
            openQuestBook(player);
            player.sendMessage(ChatColor.GREEN + "Quest progress refreshed!");
            return;
        }

        if (clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
            String name = clicked.getItemMeta().getDisplayName();
            if (name.contains("Quest:")) {
                // Show quest details in chat
                showQuestDetails(player, clicked);
            }
        }
    }

    private void showQuestDetails(Player player, ItemStack questItem) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.0F);
        player.sendMessage("");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Quest Details:");
        player.sendMessage("");
        for (String line : questItem.getItemMeta().getLore()) {
            player.sendMessage(line);
        }
        player.sendMessage("");
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage("");
    }

    // ==================== ITEM CREATORS ====================

    private ItemStack createTitleItem(Player player) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6&l✨ &eWelcome Quest Line &6&l✨"));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Embark on your journey!"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Complete quests to earn rewards."));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&a&lYour Progress:"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7  • Completed: &e" + progressManager.getTotalCompleted(player) + "&8/&e " + WelcomeQuestRegistry.count()));
        double percentage = (double) progressManager.getTotalCompleted(player) / WelcomeQuestRegistry.count() * 100;
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7  • Completion: &e" + String.format("%.1f", percentage) + "%"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&e&lClick a quest &7to view details!"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8&l➤ &7Rewards include:"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8  • &6Money"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8  • &bExperience"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8  • &aPride & Satisfaction"));

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCounterItem(Material material, String displayName, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));

        List<String> lore = new ArrayList<>();
        for (String line : loreLines) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createProgressItem(int completed, int total, double percentage) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&lQuest Progress"));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Completed: &a" + completed + " &8/ &e" + total));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Remaining: &e" + (total - completed)));
        lore.add("");

        // Visual progress bar
        lore.add(ChatColor.translateAlternateColorCodes('&', createVisualBar(percentage)));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&e" + String.format("%.1f", percentage) + "% Complete"));
        lore.add("");

        if (percentage >= 100.0) {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&a&l🏆 All Quests Complete!"));
        } else {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Keep going!"));
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createProgressIndicator(double percentage, boolean left) {
        Material material = percentage >= 0.5 ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (left) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&a◄ Progress"));
        } else {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&eProgress ►"));
        }

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7" + String.format("%.1f", percentage * 100) + "%"));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createQuestItem(WelcomeQuest quest, boolean completed, boolean available, Player player) {
        ItemStack item = quest.getIcon();
        ItemMeta meta = item.getItemMeta();

        // Color based on status
        String statusColor = completed ? "&a" : (available ? "&e" : "&7");
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', statusColor + quest.getDisplayName()));

        int progress = progressManager.getQuestProgress(player, quest.getId());
        List<String> lore = quest.getFormattedLore(progress);

        // Add status-specific lore
        lore.add("");
        if (completed) {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&a&lCOMPLETED"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7You have mastered this task!"));
        } else if (available) {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&e&lAVAILABLE"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Click to view quest details!"));
        } else {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&c&lLOCKED"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Complete the previous quest first!"));
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        // Add enchant glow to available quests
        if (available && !completed) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPlaceholderItem() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', " "));
        meta.setLore(new ArrayList<>());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfoItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6&lℹ Quest Info"));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Welcome to the Quest Line!"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&eHow to Play:"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8  • &7Quests unlock in order"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8  • &7Complete objectives automatically"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8  • &7Earn money & XP rewards"));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&eTips:"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8  • &7Start with mining & woodcutting"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8  • &7Try fishing for easy progress"));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&8  • &7Join parties for dungeon quests"));

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCloseButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&lClose"));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Click to close the quest book."));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&eYour progress is saved automatically!"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRefreshButton(Player player) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&b&l⟳ Refresh"));

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Update quest progress."));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&eClick to refresh!"));

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDecorativeItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create a visual progress bar string.
     */
    private String createVisualBar(double percentage) {
        StringBuilder bar = new StringBuilder();
        bar.append("&8[");

        int filled = (int) (percentage * 20);
        for (int i = 0; i < 20; i++) {
            if (i < filled) {
                bar.append("&a■");
            } else {
                bar.append("&8□");
            }
        }

        bar.append("&8]");
        return ChatColor.translateAlternateColorCodes('&', bar.toString());
    }

    private static class QuestBookHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}

