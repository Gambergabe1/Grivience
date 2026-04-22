package io.papermc.Grivience.skyblock.profile.gui;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.gui.SkyblockGui;
import io.papermc.Grivience.skills.SkyblockSkill;
import io.papermc.Grivience.skills.SkyblockSkillManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import io.papermc.Grivience.stats.SkyblockLevelManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * GUI for viewing another player's Skyblock profile.
 */
public final class ProfileViewGui implements Listener {
    private final GriviencePlugin plugin;
    private final SkyblockLevelManager levelManager;
    private final SkyblockSkillManager skillManager;

    public ProfileViewGui(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.levelManager = plugin.getSkyblockLevelManager();
        this.skillManager = plugin.getSkyblockSkillManager();
    }

    public void open(Player viewer, OfflinePlayer target) {
        if (target == null) return;
        
        UUID profileId = levelManager.resolveOfflineProfileId(target);
        if (profileId == null) {
            viewer.sendMessage(ChatColor.RED + "This player doesn't have an active Skyblock profile!");
            return;
        }

        SkyBlockProfile profile = plugin.getProfileManager().getProfile(profileId);
        
        if (profile == null) {
            viewer.sendMessage(ChatColor.RED + "This player doesn't have an active Skyblock profile!");
            return;
        }

        String title = target.getName() + "'s Profile";
        Inventory inv = Bukkit.createInventory(new ViewHolder(target.getUniqueId()), 54, SkyblockGui.title(title));

        // Background
        SkyblockGui.fillAll(inv, SkyblockGui.filler(Material.BLACK_STAINED_GLASS_PANE));
        ItemStack border = SkyblockGui.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);
        for (int i = 0; i < 54; i += 9) inv.setItem(i, border);
        for (int i = 8; i < 54; i += 9) inv.setItem(i, border);

        // Player Head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        headMeta.setOwningPlayer(target);
        headMeta.displayName(Component.text(target.getName() + "'s Profile", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        List<Component> headLore = new ArrayList<>();
        headLore.add(Component.empty());
        headLore.add(Component.text("Skyblock Level: ", NamedTextColor.GRAY)
                .append(Component.text(levelManager.getLevel(profileId), TextColor.color(levelManager.getLevelColor(levelManager.getLevel(profileId)).asBungee().getColor().getRGB())))
                .decoration(TextDecoration.ITALIC, false));
        headLore.add(Component.text("Skill Average: ", NamedTextColor.GRAY)
                .append(Component.text(String.format(Locale.US, "%.1f", skillManager.getSkillAverage(profileId)), NamedTextColor.GOLD))
                .decoration(TextDecoration.ITALIC, false));
        headLore.add(Component.empty());
        headLore.add(Component.text("Purse: ", NamedTextColor.GRAY)
                .append(Component.text(String.format(Locale.US, "%,.1f", profile.getPurse()), NamedTextColor.GOLD))
                .decoration(TextDecoration.ITALIC, false));
        headLore.add(Component.empty());
        headLore.add(Component.text("View " + target.getName() + "'s full stats!", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        headMeta.lore(headLore);
        head.setItemMeta(headMeta);
        inv.setItem(13, head);

        // Skills
        int[] skillSlots = {19, 20, 21, 22, 23, 24, 25};
        SkyblockSkill[] displaySkills = {
            SkyblockSkill.FARMING, SkyblockSkill.MINING, SkyblockSkill.COMBAT,
            SkyblockSkill.FORAGING, SkyblockSkill.FISHING, SkyblockSkill.ENCHANTING,
            SkyblockSkill.ALCHEMY
        };

        for (int i = 0; i < Math.min(skillSlots.length, displaySkills.length); i++) {
            inv.setItem(skillSlots[i], createSkillItem(displaySkills[i], profile));
        }

        // Active Pet
        String equippedPet = profile.getEquippedPet();
        if (equippedPet != null) {
            ItemStack petItem = plugin.getPetManager().createPetItem(equippedPet, null, true);
            if (petItem != null) {
                ItemMeta petMeta = petItem.getItemMeta();
                List<Component> lore = petMeta.lore();
                if (lore == null) lore = new ArrayList<>();
                lore.add(0, Component.text("Equipped Pet:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                petMeta.lore(lore);
                petItem.setItemMeta(petMeta);
                inv.setItem(31, petItem);
            }
        } else {
            inv.setItem(31, createSimpleItem(Material.BONE, ChatColor.RED + "No Pet Equipped", List.of(Component.text("This player has no pet active.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))));
        }

        // Stats summary
        inv.setItem(40, createStatsItem(target, profile));

        // Close button
        inv.setItem(49, SkyblockGui.closeButton());

        viewer.openInventory(inv);
    }

    private ItemStack createSkillItem(SkyblockSkill skill, SkyBlockProfile profile) {
        int level = profile.getSkillLevel(skill.name());
        long xp = profile.getSkillXp(skill.name());
        
        ItemStack item = new ItemStack(skill.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(skill.getDisplayName() + " " + level, NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Level: ", NamedTextColor.GRAY).append(Component.text(level, NamedTextColor.AQUA)).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("XP: ", NamedTextColor.GRAY).append(Component.text(String.format(Locale.US, "%,d", xp), NamedTextColor.GOLD)).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Stat Bonus: ", NamedTextColor.GRAY)
                .append(Component.text("+" + String.format(Locale.US, "%.1f", (double)level * skill.getPerkValue(1)) + " " + skill.getStatName(), NamedTextColor.AQUA))
                .decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStatsItem(OfflinePlayer target, SkyBlockProfile profile) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("General Statistics", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Kills: ", NamedTextColor.GRAY).append(Component.text(String.format(Locale.US, "%,d", profile.getKills()), NamedTextColor.RED)).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Deaths: ", NamedTextColor.GRAY).append(Component.text(String.format(Locale.US, "%,d", profile.getDeaths()), NamedTextColor.RED)).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Coins Earned: ", NamedTextColor.GRAY).append(Component.text(String.format(Locale.US, "%,.0f", (double)profile.getCoinsEarned()), NamedTextColor.GOLD)).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Dungeons Completed: ", NamedTextColor.GRAY).append(Component.text(profile.getDungeonsCompleted(), NamedTextColor.LIGHT_PURPLE)).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Items Fished: ", NamedTextColor.GRAY).append(Component.text(profile.getItemsFished(), NamedTextColor.AQUA)).decoration(TextDecoration.ITALIC, false));
        
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSimpleItem(Material mat, String name, List<Component> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(ChatColor.stripColor(name), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof ViewHolder) {
            event.setCancelled(true);
            if (event.getSlot() == 49) {
                event.getWhoClicked().closeInventory();
            }
        }
    }

    private static final class ViewHolder implements InventoryHolder {
        private final UUID targetId;
        public ViewHolder(UUID targetId) { this.targetId = targetId; }
        @Override public Inventory getInventory() { return null; }
    }
}
