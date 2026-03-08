package io.papermc.Grivience.skills;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.gui.SkyblockGui;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
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
 * Skyblock Skills GUI.
 * Matches the layout and style of the Skyblock menu.
 */
public final class SkillsGui implements Listener {
    private static final int[] SKILL_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22};
    private static final int[] REWARD_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24};
    private static final int[] REWARD_LEVELS = {1, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60};

    private final GriviencePlugin plugin;
    private final SkyblockSkillManager skillManager;
    private final NamespacedKey actionKey;
    private final NamespacedKey valueKey;

    public SkillsGui(GriviencePlugin plugin, SkyblockSkillManager skillManager) {
        this.plugin = plugin;
        this.skillManager = skillManager;
        this.actionKey = new NamespacedKey(plugin, "skills-action");
        this.valueKey = new NamespacedKey(plugin, "skills-value");
    }

    public void openMainMenu(Player player) {
        if (player == null) {
            return;
        }

        Inventory inv = Bukkit.createInventory(new SkillsHolder(MenuType.MAIN, null), 54, SkyblockGui.title("Your Skills"));
        fillBackground(inv);
        SkyblockSkill[] skills = SkyblockSkill.values();

        for (int i = 0; i < skills.length && i < SKILL_SLOTS.length; i++) {
            SkyblockSkill skill = skills[i];
            inv.setItem(SKILL_SLOTS[i], createSkillItem(player, skill, true));
        }

        inv.setItem(40, createSkillAverageItem(player));
        inv.setItem(48, createSimpleItem(Material.ARROW, ChatColor.YELLOW + "Back", List.of(ChatColor.GRAY + "Go back to Skyblock Menu"), "back_menu", ""));
        inv.setItem(49, createSimpleItem(Material.BARRIER, ChatColor.RED + "Close", List.of(ChatColor.GRAY + "Close this menu"), "close", ""));

        player.openInventory(inv);
    }

    public void openSkillDetails(Player player, SkyblockSkill skill) {
        if (player == null || skill == null) {
            return;
        }

        Inventory inv = Bukkit.createInventory(new SkillsHolder(MenuType.DETAIL, skill), 54, SkyblockGui.title(skill.getDisplayName() + " Skill"));
        fillBackground(inv);

        int level = skillManager.getLevel(player, skill);
        int maxLevel = skillManager.getMaxLevel(skill);
        inv.setItem(4, createSkillItem(player, skill, false));

        for (int i = 0; i < REWARD_SLOTS.length && i < REWARD_LEVELS.length; i++) {
            int rewardLevel = REWARD_LEVELS[i];
            if (rewardLevel <= maxLevel) {
                inv.setItem(REWARD_SLOTS[i], createRewardItem(skill, rewardLevel, level));
            }
        }

        inv.setItem(48, createSimpleItem(Material.ARROW, ChatColor.YELLOW + "Back", List.of(ChatColor.GRAY + "Return to all skills"), "back_skills", ""));
        inv.setItem(49, createSimpleItem(Material.BARRIER, ChatColor.RED + "Close", List.of(ChatColor.GRAY + "Close this menu"), "close", ""));
        inv.setItem(50, createSimpleItem(Material.COMPASS, ChatColor.GREEN + "Skyblock Menu", List.of(ChatColor.GRAY + "Return to Skyblock Menu"), "back_menu", ""));
        player.openInventory(inv);
    }

    private ItemStack createSkillItem(Player player, SkyblockSkill skill, boolean includeClickHint) {
        int maxLevel = skillManager.getMaxLevel(skill);
        int level = skillManager.getLevel(player, skill);
        double xp = skillManager.getXp(player, skill);
        double nextXp = skillManager.getXpForLevel(skill, Math.min(maxLevel, level + 1));
        double currentLevelXp = skillManager.getXpForLevel(skill, level);

        double progress = 1.0D;
        double gained = 0.0D;
        double required = 0.0D;
        if (level < maxLevel) {
            required = Math.max(1.0D, nextXp - currentLevelXp);
            gained = Math.max(0.0D, xp - currentLevelXp);
            progress = Math.max(0.0D, Math.min(1.0D, gained / required));
        }

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Increase your " + skill.getDisplayName() + " level to");
        lore.add(ChatColor.GRAY + "unlock unique rewards and stat bonuses!");
        lore.add("");
        lore.add(ChatColor.GRAY + "Level: " + ChatColor.GREEN + level);

        if (level < maxLevel) {
            lore.add(ChatColor.GRAY + "Progress to Level " + (level + 1) + ": " + ChatColor.YELLOW + Math.round(progress * 100) + "%");
            lore.add(progressBar(progress));
            lore.add(ChatColor.GRAY + "(" + format(gained) + "/" + format(required) + ")");
        } else {
            lore.add(ChatColor.GOLD + "MAX LEVEL REACHED");
        }

        lore.add("");
        if (hasPerk(skill)) {
            lore.add(ChatColor.GRAY + "Current Bonus:");
            lore.add(ChatColor.GRAY + " \u25cf " + skill.getPerkName() + ": " + ChatColor.GREEN + "+" + formatOneDecimal(skill.getPerkValue(level)));
        } else {
            lore.add(ChatColor.GRAY + "Perk: " + ChatColor.DARK_GRAY + "None");
        }

        if (level < maxLevel && hasPerk(skill)) {
            lore.add("");
            lore.add(ChatColor.GRAY + "Next Level Bonus:");
            lore.add(ChatColor.GRAY + " \u25cf " + skill.getPerkName() + ": " + ChatColor.GREEN + "+" + formatOneDecimal(skill.getPerkValue(level + 1)));
        }

        if (includeClickHint) {
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to view rewards!");
        }

        return createSimpleItem(skill.getIcon(), ChatColor.GREEN + skill.getDisplayName(), lore, includeClickHint ? "view_skill" : "noop", skill.name());
    }

    private ItemStack createRewardItem(SkyblockSkill skill, int rewardLevel, int currentLevel) {
        boolean unlocked = currentLevel >= rewardLevel;
        Material material = unlocked ? Material.LIME_DYE : Material.GRAY_DYE;
        String color = unlocked ? ChatColor.GREEN.toString() : ChatColor.GRAY.toString();

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Required Level: " + ChatColor.YELLOW + rewardLevel);
        lore.add(ChatColor.GRAY + "Status: " + (unlocked ? ChatColor.GREEN + "UNLOCKED" : ChatColor.RED + "LOCKED"));
        lore.add("");
        lore.add(ChatColor.GRAY + "Stat Bonus: " + ChatColor.AQUA + skill.getStatName());
        if (hasPerk(skill)) {
            lore.add(ChatColor.GRAY + "Perk Value: " + ChatColor.GREEN + "+" + formatOneDecimal(skill.getPerkValue(rewardLevel)));
        } else {
            lore.add(ChatColor.GRAY + "Perk Value: " + ChatColor.DARK_GRAY + "None");
        }

        return createSimpleItem(material, color + "Level " + rewardLevel, lore, "noop", skill.name() + ":" + rewardLevel);
    }

    private ItemStack createSkillAverageItem(Player player) {
        double average = skillManager.getSkillAverage(player);
        int total = skillManager.getTotalSkillLevels(player);
        int tracked = skillManager.getTrackedSkillCount();
        SkyblockSkill topSkill = skillManager.getHighestSkill(player);
        int topLevel = topSkill == null ? 0 : skillManager.getLevel(player, topSkill);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Your overall skill progression.");
        lore.add("");
        lore.add(ChatColor.GRAY + "Skill Average: " + ChatColor.GREEN + formatTwoDecimals(average));
        lore.add(ChatColor.GRAY + "Total Skill Levels: " + ChatColor.GREEN + total);
        lore.add(ChatColor.GRAY + "Tracked Skills: " + ChatColor.GREEN + tracked);
        if (topSkill != null) {
            lore.add(ChatColor.GRAY + "Highest Skill: " + ChatColor.YELLOW + topSkill.getDisplayName() + " " + topLevel);
        }

        return createSimpleItem(Material.NETHER_STAR, ChatColor.GOLD + "Skill Average", lore, "noop", "");
    }

    private String progressBar(double progress) {
        int length = 20;
        int filled = (int) (progress * length);
        StringBuilder bar = new StringBuilder(ChatColor.GREEN.toString());
        for (int i = 0; i < filled; i++) bar.append("-");
        bar.append(ChatColor.WHITE);
        for (int i = filled; i < length; i++) bar.append("-");
        return ChatColor.DARK_GRAY + "[" + bar + ChatColor.DARK_GRAY + "]";
    }

    private String format(double value) {
        if (value >= 1000000) return String.format("%.1fM", value / 1000000.0);
        if (value >= 1000) return String.format("%.1fK", value / 1000.0);
        return String.format("%.0f", value);
    }

    private String formatOneDecimal(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private String formatTwoDecimals(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private boolean hasPerk(SkyblockSkill skill) {
        return skill != null && skill.getPerkName() != null && !skill.getPerkName().equalsIgnoreCase("None");
    }

    private ItemStack createSimpleItem(Material type, String name, List<String> lore, String action, String value) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
            meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, value);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fillBackground(Inventory inv) {
        ItemStack filler = createSimpleItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of(), "noop", "");
        ItemStack border = createSimpleItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of(), "noop", "");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
        int[] slots = {0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,49,50,51,52,53};
        for (int s : slots) if (s < inv.getSize()) inv.setItem(s, border);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof SkillsHolder)) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String action = clicked.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action == null || action.equals("noop")) return;

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);

        switch (action) {
            case "back_menu" -> plugin.getSkyblockMenuManager().openMainMenu(player);
            case "back_skills" -> openMainMenu(player);
            case "close" -> player.closeInventory();
            case "view_skill" -> {
                String rawSkill = clicked.getItemMeta().getPersistentDataContainer().get(valueKey, PersistentDataType.STRING);
                SkyblockSkill skill = SkyblockSkill.parse(rawSkill);
                if (skill == null) {
                    player.sendMessage(ChatColor.RED + "Unknown skill.");
                    return;
                }
                openSkillDetails(player, skill);
            }
        }
    }

    private enum MenuType {
        MAIN,
        DETAIL
    }

    private static class SkillsHolder implements InventoryHolder {
        private final MenuType menuType;
        private final SkyblockSkill selectedSkill;

        private SkillsHolder(MenuType menuType, SkyblockSkill selectedSkill) {
            this.menuType = menuType;
            this.selectedSkill = selectedSkill;
        }

        public MenuType menuType() {
            return menuType;
        }

        public SkyblockSkill selectedSkill() {
            return selectedSkill;
        }

        @Override public Inventory getInventory() { return null; }
    }
}
