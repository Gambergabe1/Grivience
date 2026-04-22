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
    private static final String TITLE_MAIN = SkyblockGui.title("SkyBlock Leveling");
    private static final String TITLE_GUIDE = SkyblockGui.title("SkyBlock Guide");
    private static final String TITLE_WAYS = SkyblockGui.title("Ways to Level Up");
    private static final String TITLE_REWARDS = SkyblockGui.title("Leveling Rewards");

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
        Inventory inventory = Bukkit.createInventory(new LevelGuiHolder(LevelGuiType.MAIN), 54, TITLE_MAIN);
        fillBackground(inventory);

        int level = levelManager.getLevel(player);
        long totalXp = levelManager.getXp(player);
        long intoLevel = levelManager.xpIntoCurrentLevel(player);
        long perLevel = Math.max(1L, levelManager.getXpPerLevel());
        long toNext = levelManager.xpToNextLevel(player);
        double progress = clampProgress(levelManager.getProgress(player));
        int progressPercent = (int) Math.round(progress * 100.0D);
        boolean atMaxLevel = levelManager.isAtMaxLevel(player);

        List<String> levelLore = new ArrayList<>();
        levelLore.add(ChatColor.GRAY + "Track your SkyBlock level progression.");
        levelLore.add(divider());
        levelLore.add(ChatColor.GRAY + "Current Level: " + levelManager.getLevelColor(level) + level);
        levelLore.add(ChatColor.GRAY + "Total XP: " + ChatColor.GOLD + fmt(totalXp));
        levelLore.add("");
        if (atMaxLevel) {
            levelLore.add(ChatColor.GRAY + "Progress: " + ChatColor.GREEN + "MAXED");
            levelLore.add(progressBar(1.0D, 20));
            levelLore.add(ChatColor.GRAY + "Next Level In: " + ChatColor.GREEN + "MAX");
        } else {
            levelLore.add(ChatColor.GRAY + "Progress to " + levelManager.getLevelColor(level + 1) + "Level " + (level + 1)
                    + ChatColor.GRAY + ": " + ChatColor.YELLOW + progressPercent + "%");
            levelLore.add(progressBar(progress, 20));
            levelLore.add(ChatColor.DARK_GRAY + fmt(intoLevel) + "/" + fmt(perLevel) + " XP");
            levelLore.add(ChatColor.GRAY + "Next Level In: " + ChatColor.AQUA + fmt(toNext) + " XP");
        }
        levelLore.add("");

        SkyblockLevelManager.FeatureUnlock nextUnlock = levelManager.nextFeatureUnlock(player);
        if (nextUnlock != null) {
            levelLore.add(ChatColor.GRAY + "Next Unlock: " + ChatColor.GOLD + "Level " + nextUnlock.level());
            if (!nextUnlock.unlocks().isEmpty()) {
                levelLore.add(ChatColor.DARK_GRAY + "- " + levelManager.color(nextUnlock.unlocks().get(0)));
                if (nextUnlock.unlocks().size() > 1) {
                    levelLore.add(ChatColor.DARK_GRAY + "- " + ChatColor.GRAY + "+" + (nextUnlock.unlocks().size() - 1) + " more unlocks");
                }
            }
        } else {
            levelLore.add(ChatColor.GRAY + "Next Unlock: " + ChatColor.GREEN + "All rewards unlocked");
        }
        levelLore.add("");
        levelLore.add(ChatColor.GOLD + "SkyBlock Level Bonuses:");
        levelLore.add(ChatColor.GRAY + "- " + ChatColor.RED + "+" + levelManager.getHealthBonus(player) + ChatColor.GRAY + " Health");
        levelLore.add(ChatColor.GRAY + "- " + ChatColor.RED + "+" + levelManager.getStrengthBonus(player) + ChatColor.GRAY + " Strength");
        levelLore.add("");
        levelLore.add(ChatColor.YELLOW + "Click to view rewards!");

        inventory.setItem(13, createItem(
                Material.EXPERIENCE_BOTTLE,
                levelManager.getLevelColor(level) + "SkyBlock Level " + level,
                levelLore,
                "open_rewards",
                "",
                true
        ));

        int claimedMilestones = levelManager.claimedMilestones(player);
        int totalMilestones = Math.max(1, levelManager.totalMilestones());
        double guideCompletion = claimedMilestones / (double) totalMilestones;
        List<String> guideLore = new ArrayList<>();
        guideLore.add(ChatColor.GRAY + "Follow milestones for guided progression.");
        guideLore.add(divider());
        guideLore.add(ChatColor.GRAY + "Milestones: " + ChatColor.GREEN + claimedMilestones
                + ChatColor.DARK_GRAY + "/" + ChatColor.GREEN + totalMilestones);
        guideLore.add(progressBar(guideCompletion, 16));
        guideLore.add(ChatColor.GRAY + "Completion: " + ChatColor.YELLOW + Math.round(guideCompletion * 100.0D) + "%");
        guideLore.add("");
        guideLore.add(ChatColor.GRAY + "Browse each category and claim milestones.");
        guideLore.add("");
        guideLore.add(ChatColor.YELLOW + "Click to view guide!");
        inventory.setItem(11, createItem(Material.NETHER_STAR, ChatColor.GREEN + "SkyBlock Guide", guideLore, "open_guide", "", true));

        long trackedXp = 0L;
        for (SkyblockLevelManager.GuideTrack track : levelManager.tracks()) {
            trackedXp += safeProgress(player, track).counterValue();
        }
        long otherXp = Math.max(0L, totalXp - trackedXp);
        List<String> waysLore = new ArrayList<>();
        waysLore.add(ChatColor.GRAY + "Review where your SkyBlock XP is coming from.");
        waysLore.add(divider());
        waysLore.add(ChatColor.GRAY + "Tracked XP: " + ChatColor.AQUA + fmt(trackedXp));
        waysLore.add(ChatColor.GRAY + "Other XP: " + ChatColor.AQUA + fmt(otherXp));
        waysLore.add(ChatColor.GRAY + "Total XP: " + ChatColor.GOLD + fmt(totalXp));
        waysLore.add("");

        int previewCount = 0;
        for (SkyblockLevelManager.GuideTrack track : levelManager.tracks()) {
            if (previewCount >= 3) {
                break;
            }
            SkyblockLevelManager.GuideProgress trackProgress = safeProgress(player, track);
            waysLore.add(levelManager.color(track.displayName()) + ChatColor.GRAY + ": " + ChatColor.AQUA + fmt(trackProgress.counterValue()) + " XP");
            previewCount++;
        }
        if (levelManager.tracks().size() > previewCount) {
            waysLore.add(ChatColor.GRAY + "+" + (levelManager.tracks().size() - previewCount) + " more categories");
        }
        waysLore.add("");
        waysLore.add(ChatColor.YELLOW + "Click to view details!");
        inventory.setItem(15, createItem(Material.CHEST, ChatColor.GREEN + "Ways to Level Up", waysLore, "open_ways", ""));

        inventory.setItem(49, createItem(Material.ARROW, ChatColor.GREEN + "Go Back", List.of(ChatColor.GRAY + "To SkyBlock Menu"), "back_to_main", ""));
        player.openInventory(inventory);
    }

    public void openGuideMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new LevelGuiHolder(LevelGuiType.GUIDE), 54, TITLE_GUIDE);
        fillBackground(inventory);

        List<SkyblockLevelManager.GuideTrack> tracks = levelManager.tracks();
        int[] slots = {19, 20, 21, 22, 23, 24, 25, 29, 30, 31, 32, 33, 34};

        for (int i = 0; i < tracks.size() && i < slots.length; i++) {
            SkyblockLevelManager.GuideTrack track = tracks.get(i);
            SkyblockLevelManager.GuideProgress progress = safeProgress(player, track);

            List<String> lore = new ArrayList<>();
            for (String line : track.lore()) {
                lore.add(levelManager.color(line));
            }
            lore.add(divider());
            lore.add(ChatColor.GRAY + "Category XP: " + ChatColor.AQUA + fmt(progress.counterValue()));
            lore.add(ChatColor.GRAY + "Milestones: " + ChatColor.GREEN + progress.claimedMilestones()
                    + ChatColor.DARK_GRAY + "/" + ChatColor.GREEN + progress.totalMilestones());
            if (progress.completed()) {
                lore.add(ChatColor.GRAY + "Status: " + ChatColor.GREEN + "Completed");
            } else {
                int nextMilestone = progress.claimedMilestones() + 1;
                long remaining = Math.max(0L, progress.nextThreshold() - progress.counterValue());
                lore.add(ChatColor.GRAY + "Next Milestone: " + ChatColor.YELLOW + "#" + nextMilestone);
                lore.add(progressBar(progress.progressToNext(), 16));
                lore.add(ChatColor.DARK_GRAY + fmt(progress.counterValue()) + "/" + fmt(progress.nextThreshold()));
                lore.add(ChatColor.GRAY + "Remaining: " + ChatColor.AQUA + fmt(remaining));
                if (progress.nextRewardXp() > 0L) {
                    lore.add(ChatColor.GRAY + "Reward: " + ChatColor.GOLD + "+" + fmt(progress.nextRewardXp()) + " SkyBlock XP");
                }
            }
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to view milestones!");

            inventory.setItem(slots[i], createItem(
                    track.icon(),
                    levelManager.color(track.displayName()),
                    lore,
                    "open_track",
                    track.id(),
                    progress.completed()
            ));
        }

        inventory.setItem(49, createItem(Material.ARROW, ChatColor.GREEN + "Go Back", List.of(ChatColor.GRAY + "To SkyBlock Leveling"), "open_main", ""));
        player.openInventory(inventory);
    }

    public void openTrackMenu(Player player, String trackId) {
        SkyblockLevelManager.GuideTrack track = levelManager.track(trackId);
        if (track == null) {
            return;
        }

        String title = ChatColor.stripColor(levelManager.color(track.displayName()));
        Inventory inventory = Bukkit.createInventory(new LevelGuiHolder(LevelGuiType.TRACK), 54, SkyblockGui.title(title));
        fillBackground(inventory);

        SkyblockLevelManager.GuideProgress progress = safeProgress(player, track);

        List<String> summaryLore = new ArrayList<>();
        for (String line : track.lore()) {
            summaryLore.add(levelManager.color(line));
        }
        summaryLore.add(divider());
        summaryLore.add(ChatColor.GRAY + "Category XP: " + ChatColor.AQUA + fmt(progress.counterValue()));
        summaryLore.add(ChatColor.GRAY + "Milestones: " + ChatColor.GREEN + progress.claimedMilestones()
                + ChatColor.DARK_GRAY + "/" + ChatColor.GREEN + progress.totalMilestones());
        if (progress.completed()) {
            summaryLore.add(ChatColor.GREEN + "All milestones completed.");
            summaryLore.add(progressBar(1.0D, 16));
        } else {
            summaryLore.add(progressBar(progress.progressToNext(), 16));
            summaryLore.add(ChatColor.DARK_GRAY + fmt(progress.counterValue()) + "/" + fmt(progress.nextThreshold()));
        }
        inventory.setItem(4, createItem(track.icon(), levelManager.color(track.displayName()) + ChatColor.BOLD + " Progress", summaryLore, "noop", "", true));

        int[] milestoneSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        for (int i = 0; i < track.milestones().size() && i < milestoneSlots.length; i++) {
            boolean claimed = i < progress.claimedMilestones();
            boolean current = i == progress.claimedMilestones() && !progress.completed();
            long threshold = track.milestones().get(i);
            long reward = i < track.rewards().size() ? track.rewards().get(i) : 0L;
            long previousThreshold = i == 0 ? 0L : track.milestones().get(i - 1);
            double localProgress = threshold <= previousThreshold
                    ? 1.0D
                    : clampProgress((progress.counterValue() - previousThreshold) / (double) (threshold - previousThreshold));

            Material material = claimed ? Material.LIME_STAINED_GLASS_PANE
                    : current ? Material.YELLOW_STAINED_GLASS_PANE
                    : Material.GRAY_STAINED_GLASS_PANE;
            ChatColor nameColor = claimed ? ChatColor.GREEN : current ? ChatColor.YELLOW : ChatColor.RED;
            String name = nameColor + "Milestone " + (i + 1);

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Requirement: " + ChatColor.AQUA + fmt(threshold) + " Track XP");
            if (reward > 0L) {
                lore.add(ChatColor.GRAY + "Reward: " + ChatColor.GOLD + "+" + fmt(reward) + " SkyBlock XP");
            }
            lore.add("");
            if (claimed) {
                lore.add(ChatColor.GREEN + "Status: UNLOCKED");
            } else if (current) {
                long remaining = Math.max(0L, threshold - progress.counterValue());
                lore.add(progressBar(localProgress, 14));
                lore.add(ChatColor.DARK_GRAY + fmt(progress.counterValue()) + "/" + fmt(threshold));
                lore.add(ChatColor.GRAY + "Remaining: " + ChatColor.AQUA + fmt(remaining));
                lore.add(ChatColor.YELLOW + "Status: IN PROGRESS");
            } else {
                lore.add(ChatColor.RED + "Status: LOCKED");
            }

            inventory.setItem(milestoneSlots[i], createItem(material, name, lore, "noop", "", claimed));
        }

        inventory.setItem(45, createItem(
                Material.LIME_STAINED_GLASS_PANE,
                ChatColor.GREEN + "Unlocked",
                List.of(ChatColor.GRAY + "This milestone has been claimed."),
                "noop",
                ""
        ));
        inventory.setItem(47, createItem(
                Material.YELLOW_STAINED_GLASS_PANE,
                ChatColor.YELLOW + "In Progress",
                List.of(ChatColor.GRAY + "This is your next milestone."),
                "noop",
                ""
        ));
        inventory.setItem(53, createItem(
                Material.GRAY_STAINED_GLASS_PANE,
                ChatColor.RED + "Locked",
                List.of(ChatColor.GRAY + "Complete earlier milestones first."),
                "noop",
                ""
        ));
        inventory.setItem(49, createItem(Material.ARROW, ChatColor.GREEN + "Go Back", List.of(ChatColor.GRAY + "To SkyBlock Guide"), "open_guide", ""));
        player.openInventory(inventory);
    }

    public void openWaysToLevelMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new LevelGuiHolder(LevelGuiType.WAYS), 54, TITLE_WAYS);
        fillBackground(inventory);

        List<SkyblockLevelManager.GuideTrack> tracks = levelManager.tracks();
        int[] slots = {19, 20, 21, 22, 23, 24, 25, 29, 30, 31, 32, 33, 34};

        long totalXp = levelManager.getXp(player);
        long trackedXp = 0L;
        for (SkyblockLevelManager.GuideTrack track : tracks) {
            trackedXp += safeProgress(player, track).counterValue();
        }
        long otherXp = Math.max(0L, totalXp - trackedXp);

        List<String> summaryLore = new ArrayList<>();
        summaryLore.add(ChatColor.GRAY + "Your SkyBlock XP sources.");
        summaryLore.add(divider());
        summaryLore.add(ChatColor.GRAY + "Tracked XP: " + ChatColor.AQUA + fmt(trackedXp));
        summaryLore.add(ChatColor.GRAY + "Other XP: " + ChatColor.AQUA + fmt(otherXp));
        summaryLore.add(ChatColor.GRAY + "Total XP: " + ChatColor.GOLD + fmt(totalXp));
        inventory.setItem(13, createItem(Material.NETHER_STAR, ChatColor.GOLD + "XP Breakdown", summaryLore, "noop", "", true));

        for (int i = 0; i < tracks.size() && i < slots.length; i++) {
            SkyblockLevelManager.GuideTrack track = tracks.get(i);
            SkyblockLevelManager.GuideProgress progress = safeProgress(player, track);
            double contribution = totalXp <= 0L ? 0.0D : (progress.counterValue() * 100.0D) / totalXp;

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "XP from this category:");
            lore.add(ChatColor.AQUA + fmt(progress.counterValue()) + " XP");
            lore.add(ChatColor.GRAY + "Contribution: " + ChatColor.YELLOW + String.format(Locale.US, "%.1f%%", contribution));
            lore.add("");
            lore.add(ChatColor.GRAY + "Milestones: " + ChatColor.GREEN + progress.claimedMilestones()
                    + ChatColor.DARK_GRAY + "/" + ChatColor.GREEN + progress.totalMilestones());
            if (!progress.completed()) {
                long remaining = Math.max(0L, progress.nextThreshold() - progress.counterValue());
                lore.add(ChatColor.GRAY + "Next Milestone In: " + ChatColor.AQUA + fmt(remaining));
            } else {
                lore.add(ChatColor.GREEN + "All milestones completed.");
            }

            inventory.setItem(slots[i], createItem(track.icon(), levelManager.color(track.displayName()), lore, "noop", ""));
        }

        if (otherXp > 0L) {
            inventory.setItem(40, createItem(
                    Material.PAPER,
                    ChatColor.YELLOW + "Other XP",
                    List.of(
                            ChatColor.GRAY + "XP from uncategorized sources:",
                            ChatColor.AQUA + fmt(otherXp) + " XP"
                    ),
                    "noop",
                    ""
            ));
        }

        inventory.setItem(49, createItem(Material.ARROW, ChatColor.GREEN + "Go Back", List.of(ChatColor.GRAY + "To SkyBlock Leveling"), "open_main", ""));
        player.openInventory(inventory);
    }

    public void openRewardsMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new LevelGuiHolder(LevelGuiType.REWARDS), 54, TITLE_REWARDS);
        fillBackground(inventory);

        int level = levelManager.getLevel(player);
        int maxLevel = Math.max(1, levelManager.getMaxLevel());
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        int windowSize = slots.length;
        int startLevel = Math.max(1, level - (windowSize / 2));
        if (startLevel + windowSize - 1 > maxLevel) {
            startLevel = Math.max(1, maxLevel - windowSize + 1);
        }
        int endLevel = Math.min(maxLevel, startLevel + windowSize - 1);

        List<String> summaryLore = new ArrayList<>();
        summaryLore.add(ChatColor.GRAY + "Current Level: " + levelManager.getLevelColor(level) + level);
        summaryLore.add(ChatColor.GRAY + "Showing Levels: " + ChatColor.AQUA + startLevel + ChatColor.DARK_GRAY + " - " + ChatColor.AQUA + endLevel);
        summaryLore.add("");
        summaryLore.add(ChatColor.GRAY + "Reward pattern:");
        summaryLore.add(ChatColor.GRAY + "- " + ChatColor.RED + "+5 Health each level");
        summaryLore.add(ChatColor.GRAY + "- " + ChatColor.RED + "+1 Strength every 5 levels");
        inventory.setItem(4, createItem(Material.BOOK, ChatColor.GOLD + "Reward Overview", summaryLore, "noop", "", true));

        for (int i = 0; i < slots.length; i++) {
            int displayLevel = startLevel + i;
            if (displayLevel > maxLevel) {
                break;
            }

            boolean reached = displayLevel <= level;
            boolean next = !reached && displayLevel == (level + 1);
            Material material = reached ? Material.GOLD_BLOCK : next ? Material.YELLOW_CONCRETE : Material.IRON_BLOCK;
            ChatColor nameColor = reached ? ChatColor.GREEN : next ? ChatColor.YELLOW : ChatColor.RED;
            String name = nameColor + "Level " + displayLevel;

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Rewards:");
            lore.add(ChatColor.GRAY + "- " + ChatColor.RED + "+5 Health");
            if (displayLevel % 5 == 0) {
                lore.add(ChatColor.GRAY + "- " + ChatColor.RED + "+1 Strength");
            }

            List<String> unlocks = levelManager.featureUnlocksAtLevel(displayLevel);
            if (!unlocks.isEmpty()) {
                lore.add("");
                lore.add(ChatColor.AQUA + "Feature Unlocks:");
                for (String unlock : unlocks) {
                    lore.add(ChatColor.GRAY + "- " + levelManager.color(unlock));
                }
            }

            lore.add("");
            lore.add(reached ? ChatColor.GREEN + "REACHED" : next ? ChatColor.YELLOW + "NEXT" : ChatColor.RED + "LOCKED");

            inventory.setItem(slots[i], createItem(material, name, lore, "noop", "", reached));
        }

        inventory.setItem(45, createItem(Material.GOLD_BLOCK, ChatColor.GREEN + "Reached", List.of(ChatColor.GRAY + "You already reached this level."), "noop", ""));
        inventory.setItem(47, createItem(Material.YELLOW_CONCRETE, ChatColor.YELLOW + "Next", List.of(ChatColor.GRAY + "This is your next level reward."), "noop", ""));
        inventory.setItem(53, createItem(Material.IRON_BLOCK, ChatColor.RED + "Locked", List.of(ChatColor.GRAY + "Keep gaining SkyBlock XP."), "noop", ""));
        inventory.setItem(49, createItem(Material.ARROW, ChatColor.GREEN + "Go Back", List.of(ChatColor.GRAY + "To SkyBlock Leveling"), "open_main", ""));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof LevelGuiHolder)) {
            return;
        }

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        String value = meta.getPersistentDataContainer().get(valueKey, PersistentDataType.STRING);

        if (action == null || action.equals("noop")) {
            return;
        }

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);

        switch (action) {
            case "open_main" -> openMainMenu(player);
            case "open_guide" -> openGuideMenu(player);
            case "open_ways" -> openWaysToLevelMenu(player);
            case "open_rewards" -> openRewardsMenu(player);
            case "open_track" -> openTrackMenu(player, value);
            case "back_to_main" -> plugin.getSkyblockMenuManager().openMainMenu(player);
            default -> {
            }
        }
    }

    private ItemStack createItem(Material material, String name, List<String> lore, String action, String value) {
        return createItem(material, name, lore, action, value, false);
    }

    private ItemStack createItem(Material material, String name, List<String> lore, String action, String value, boolean glow) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(name == null ? " " : name);
        meta.setLore(lore == null ? List.of() : new ArrayList<>(lore));
        if (glow) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action == null ? "noop" : action);
        meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, value == null ? "" : value);
        item.setItemMeta(meta);
        return item;
    }

    private void fillBackground(Inventory inventory) {
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of(), "noop", "");
        ItemStack fill = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of(), "noop", "");
        int rows = inventory.getSize() / 9;
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            int row = slot / 9;
            int col = slot % 9;
            boolean isBorder = row == 0 || row == rows - 1 || col == 0 || col == 8;
            inventory.setItem(slot, (isBorder ? border : fill).clone());
        }
    }

    private String progressBar(double progress, int length) {
        int safeLength = Math.max(1, length);
        double clamped = clampProgress(progress);
        int filled = (int) Math.round(clamped * safeLength);
        if (clamped > 0.0D && filled == 0) {
            filled = 1;
        }

        StringBuilder bar = new StringBuilder(ChatColor.DARK_GRAY + "[");
        for (int i = 0; i < safeLength; i++) {
            bar.append(i < filled ? ChatColor.GREEN : ChatColor.GRAY).append("|");
        }
        bar.append(ChatColor.DARK_GRAY).append("]");
        return bar.toString();
    }

    private double clampProgress(double progress) {
        if (!Double.isFinite(progress)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(1.0D, progress));
    }

    private String divider() {
        return ChatColor.DARK_GRAY + "----------------------";
    }

    private SkyblockLevelManager.GuideProgress safeProgress(Player player, SkyblockLevelManager.GuideTrack track) {
        SkyblockLevelManager.GuideProgress progress = levelManager.progressFor(player, track);
        if (progress != null) {
            return progress;
        }

        int totalMilestones = track == null ? 0 : track.milestones().size();
        if (track == null || totalMilestones == 0) {
            return new SkyblockLevelManager.GuideProgress(track, 0L, 0, 0, 0L, 0L, 0L, 1.0D, true);
        }

        long nextThreshold = track.milestones().get(0);
        long nextReward = track.rewards().isEmpty() ? 0L : track.rewards().get(0);
        return new SkyblockLevelManager.GuideProgress(track, 0L, 0, totalMilestones, 0L, nextThreshold, nextReward, 0.0D, false);
    }

    private String fmt(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    private enum LevelGuiType {
        MAIN,
        GUIDE,
        TRACK,
        WAYS,
        REWARDS
    }

    private static class LevelGuiHolder implements InventoryHolder {
        private final LevelGuiType type;

        LevelGuiHolder(LevelGuiType type) {
            this.type = type;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
