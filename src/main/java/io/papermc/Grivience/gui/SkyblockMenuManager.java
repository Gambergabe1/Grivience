package io.papermc.Grivience.gui;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.island.Island;
import io.papermc.Grivience.skyblock.island.IslandManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkyblockMenuManager implements Listener {
    private static final String TITLE_MAIN = ChatColor.DARK_PURPLE + "SkyBlock Menu";
    private static final String TITLE_ISLAND = ChatColor.DARK_GREEN + "Island Management";
    private static final String TITLE_UPGRADES = ChatColor.GOLD + "Island Upgrades";
    private static final String TITLE_MINIONS = ChatColor.DARK_AQUA + "Minion Management";
    private static final String TITLE_SETTINGS = ChatColor.RED + "Island Settings";
    private static final String TITLE_PERMISSIONS = ChatColor.DARK_RED + "Island Permissions";

    private final GriviencePlugin plugin;
    private IslandManager islandManager;
    private final NamespacedKey actionKey;
    private final NamespacedKey valueKey;
    private final Map<String, Long> actionCooldowns = new ConcurrentHashMap<>();
    private final DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");

    public SkyblockMenuManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "skyblock-action");
        this.valueKey = new NamespacedKey(plugin, "skyblock-value");
    }

    public void setIslandManager(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    public void openMainMenu(Player player) {
        MenuHolder holder = new MenuHolder(MenuType.MAIN);
        Inventory inventory = Bukkit.createInventory(holder, 54, TITLE_MAIN);
        holder.inventory = inventory;

        fillBackground(inventory);

        inventory.setItem(10, createMenuItem(
                Material.GRASS_BLOCK,
                ChatColor.GREEN + "Island Management",
                List.of(
                        ChatColor.GRAY + "Manage your island settings",
                        ChatColor.GRAY + "and configurations.",
                        "",
                        ChatColor.YELLOW + "Click to open"
                ),
                "open_island",
                "",
                true
        ));

        inventory.setItem(12, createMenuItem(
                Material.DIAMOND,
                ChatColor.GOLD + "Island Upgrades",
                List.of(
                        ChatColor.GRAY + "Upgrade your island with",
                        ChatColor.GRAY + "powerful enhancements.",
                        "",
                        ChatColor.YELLOW + "Click to browse upgrades"
                ),
                "open_upgrades",
                "",
                true
        ));

        inventory.setItem(14, createMenuItem(
                Material.ZOMBIE_SPAWN_EGG,
                ChatColor.DARK_AQUA + "Minions",
                List.of(
                        ChatColor.GRAY + "Manage your minions and",
                        ChatColor.GRAY + "automate your island.",
                        "",
                        ChatColor.YELLOW + "Click to manage minions"
                ),
                "open_minions",
                "",
                true
        ));

        inventory.setItem(16, createMenuItem(
                Material.OAK_DOOR,
                ChatColor.RED + "Island Settings",
                List.of(
                        ChatColor.GRAY + "Configure island permissions",
                        ChatColor.GRAY + "and visitor settings.",
                        "",
                        ChatColor.YELLOW + "Click to open settings"
                ),
                "open_settings",
                "",
                true
        ));

        Island island = islandManager != null ? islandManager.getIsland(player.getUniqueId()) : null;
        int islandLevel = island != null ? island.getUpgradeLevel() : 0;
        int islandSize = island != null ? island.getSize() : 0;
        String islandName = island != null ? island.getName() : "No Island";
        int totalIslands = islandManager != null ? islandManager.getTotalIslands() : 0;

        inventory.setItem(22, createMenuItem(
                Material.NETHER_STAR,
                ChatColor.LIGHT_PURPLE + "Island Overview",
                List.of(
                        ChatColor.GRAY + "Island: " + ChatColor.AQUA + islandName,
                        ChatColor.GRAY + "Island Level: " + ChatColor.GREEN + "" + islandLevel,
                        ChatColor.GRAY + "Island Size: " + ChatColor.GREEN + "" + islandSize + "x" + islandSize,
                        ChatColor.GRAY + "Total Islands: " + ChatColor.YELLOW + "" + totalIslands,
                        "",
                        ChatColor.DARK_GRAY + "Your island statistics"
                ),
                "noop",
                "",
                true
        ));

        inventory.setItem(4, createMenuItem(
                Material.COMPASS,
                ChatColor.AQUA + "Fast Travel",
                List.of(
                        ChatColor.GRAY + "Travel to different locations",
                        ChatColor.GRAY + "on your island.",
                        "",
                        ChatColor.YELLOW + "Click to open fast travel"
                ),
                "open_fast_travel",
                "",
                true
        ));

        inventory.setItem(29, createMenuItem(
                Material.BOOK,
                ChatColor.BLUE + "Collection",
                List.of(
                        ChatColor.GRAY + "View your item collections",
                        ChatColor.GRAY + "and unlock rewards.",
                        "",
                        ChatColor.YELLOW + "Click to view collections"
                ),
                "open_collection",
                "",
                true
        ));

        inventory.setItem(31, createMenuItem(
                Material.CRAFTING_TABLE,
                ChatColor.DARK_PURPLE + "Recipes",
                List.of(
                        ChatColor.GRAY + "Browse unlocked recipes",
                        ChatColor.GRAY + "and crafting guides.",
                        "",
                        ChatColor.YELLOW + "Click to view recipes"
                ),
                "open_recipes",
                "",
                true
        ));

        inventory.setItem(33, createMenuItem(
                Material.ENCHANTING_TABLE,
                ChatColor.DARK_BLUE + "Enchanting",
                List.of(
                        ChatColor.GRAY + "Access your enchanting table",
                        ChatColor.GRAY + "and upgrade items.",
                        "",
                        ChatColor.YELLOW + "Click to open enchanting"
                ),
                "open_enchanting",
                "",
                true
        ));

        inventory.setItem(49, createMenuItem(
                Material.BARRIER,
                ChatColor.RED + "Close",
                List.of(ChatColor.GRAY + "Close this menu."),
                "close_menu",
                ""
        ));

        player.openInventory(inventory);
    }

    public void openIslandMenu(Player player) {
        MenuHolder holder = new MenuHolder(MenuType.ISLAND);
        Inventory inventory = Bukkit.createInventory(holder, 54, TITLE_ISLAND);
        holder.inventory = inventory;

        fillBackground(inventory);

        Island island = islandManager != null ? islandManager.getIsland(player.getUniqueId()) : null;
        int currentSize = island != null ? island.getSize() : 32;
        int currentLevel = island != null ? island.getUpgradeLevel() : 1;
        int nextLevel = islandManager != null ? islandManager.getNextUpgradeLevel(island != null ? island : createPlaceholderIsland(player)) : 2;
        int nextSize = islandManager != null ? islandManager.getNextUpgradeSize(island != null ? island : createPlaceholderIsland(player)) : 48;
        double upgradeCost = islandManager != null ? islandManager.getUpgradeCost(nextLevel) : 5000;

        inventory.setItem(10, createMenuItem(
                Material.GOLDEN_AXE,
                ChatColor.GOLD + "Expand Island",
                List.of(
                        ChatColor.GRAY + "Increase your island size",
                        ChatColor.GRAY + "Current: " + ChatColor.AQUA + currentSize + "x" + currentSize,
                        ChatColor.GRAY + "Next: " + ChatColor.YELLOW + nextSize + "x" + nextSize + " (Level " + nextLevel + ")",
                        ChatColor.GRAY + "Cost: " + ChatColor.RED + "$" + decimalFormat.format(upgradeCost),
                        "",
                        ChatColor.YELLOW + "Click to expand"
                ),
                "expand_island",
                String.valueOf(nextLevel),
                true
        ));

        inventory.setItem(13, createMenuItem(
                Material.GRASS_BLOCK,
                ChatColor.GREEN + "Change Biome",
                List.of(
                        ChatColor.GRAY + "Change your island biome",
                        ChatColor.GRAY + "Current: " + ChatColor.AQUA + "Plains",
                        "",
                        ChatColor.YELLOW + "Click to select biome"
                ),
                "change_biome",
                "",
                true
        ));

        inventory.setItem(16, createMenuItem(
                Material.BEACON,
                ChatColor.AQUA + "Set Spawn Point",
                List.of(
                        ChatColor.GRAY + "Set your island spawn point",
                        ChatColor.GRAY + "for visitors and members.",
                        "",
                        ChatColor.YELLOW + "Click to set spawn"
                ),
                "set_spawn",
                "",
                true
        ));

        inventory.setItem(22, createMenuItem(
                Material.FILLED_MAP,
                ChatColor.DARK_PURPLE + "Island Info",
                List.of(
                        ChatColor.GRAY + "Island Name: " + ChatColor.WHITE + (island != null ? island.getName() : "None"),
                        ChatColor.GRAY + "Island Level: " + ChatColor.AQUA + "" + currentLevel,
                        ChatColor.GRAY + "Island Size: " + ChatColor.AQUA + currentSize + "x" + currentSize,
                        ChatColor.GRAY + "Members: " + ChatColor.GREEN + "1",
                        ChatColor.GRAY + "Total Visits: " + ChatColor.YELLOW + (island != null ? island.getTotalVisits() : 0),
                        "",
                        ChatColor.DARK_GRAY + "Your island overview"
                ),
                "noop",
                "",
                true
        ));

        inventory.setItem(28, createMenuItem(
                Material.EMERALD,
                ChatColor.GREEN + "Invite Member",
                List.of(
                        ChatColor.GRAY + "Invite a player to join",
                        ChatColor.GRAY + "your island as a member.",
                        "",
                        ChatColor.YELLOW + "Click to invite"
                ),
                "invite_member",
                "",
                false
        ));

        inventory.setItem(30, createMenuItem(
                Material.REDSTONE_TORCH,
                ChatColor.RED + "Kick Member",
                List.of(
                        ChatColor.GRAY + "Remove a member from",
                        ChatColor.GRAY + "your island.",
                        "",
                        ChatColor.YELLOW + "Click to manage"
                ),
                "kick_member",
                "",
                false
        ));

        inventory.setItem(34, createMenuItem(
                Material.OAK_SIGN,
                ChatColor.DARK_BLUE + "Rename Island",
                List.of(
                        ChatColor.GRAY + "Change your island's name",
                        ChatColor.GRAY + "to something unique.",
                        "",
                        ChatColor.YELLOW + "Click to rename"
                ),
                "rename_island",
                "",
                false
        ));

        inventory.setItem(45, createMenuItem(
                Material.ARROW,
                ChatColor.YELLOW + "Back",
                List.of(ChatColor.GRAY + "Return to main menu"),
                "open_main",
                ""
        ));

        inventory.setItem(49, createMenuItem(
                Material.BARRIER,
                ChatColor.RED + "Close",
                List.of(ChatColor.GRAY + "Close this menu."),
                "close_menu",
                ""
        ));

        player.openInventory(inventory);
    }

    public void openUpgradesMenu(Player player) {
        MenuHolder holder = new MenuHolder(MenuType.UPGRADES);
        Inventory inventory = Bukkit.createInventory(holder, 54, TITLE_UPGRADES);
        holder.inventory = inventory;

        fillBackground(inventory);

        inventory.setItem(10, createMenuItem(
                Material.DIAMOND_PICKAXE,
                ChatColor.AQUA + "Minion Slot Upgrade",
                List.of(
                        ChatColor.GRAY + "Add +1 minion slot",
                        ChatColor.GRAY + "Current: " + ChatColor.GREEN + "3",
                        ChatColor.GRAY + "Next: " + ChatColor.YELLOW + "4",
                        ChatColor.GRAY + "Cost: " + ChatColor.RED + "$5,000",
                        "",
                        ChatColor.GREEN + "Click to upgrade"
                ),
                "buy_upgrade",
                "minion_slot",
                true
        ));

        inventory.setItem(12, createMenuItem(
                Material.GOLD_BLOCK,
                ChatColor.GOLD + "Money Generator",
                List.of(
                        ChatColor.GRAY + "Generate money over time",
                        ChatColor.GRAY + "Level: " + ChatColor.GREEN + "1",
                        ChatColor.GRAY + "Income: " + ChatColor.YELLOW + "$100/min",
                        ChatColor.GRAY + "Upgrade Cost: " + ChatColor.RED + "$2,500",
                        "",
                        ChatColor.GREEN + "Click to upgrade"
                ),
                "buy_upgrade",
                "money_generator",
                true
        ));

        inventory.setItem(14, createMenuItem(
                Material.EXPERIENCE_BOTTLE,
                ChatColor.LIGHT_PURPLE + "XP Booster",
                List.of(
                        ChatColor.GRAY + "Boost XP gain on island",
                        ChatColor.GRAY + "Level: " + ChatColor.GREEN + "0",
                        ChatColor.GRAY + "Bonus: " + ChatColor.YELLOW + "+0%",
                        ChatColor.GRAY + "Upgrade Cost: " + ChatColor.RED + "$3,000",
                        "",
                        ChatColor.GREEN + "Click to upgrade"
                ),
                "buy_upgrade",
                "xp_booster",
                true
        ));

        inventory.setItem(16, createMenuItem(
                Material.SUGAR_CANE,
                ChatColor.GREEN + "Crop Growth Boost",
                List.of(
                        ChatColor.GRAY + "Increase crop growth speed",
                        ChatColor.GRAY + "Level: " + ChatColor.GREEN + "0",
                        ChatColor.GRAY + "Speed: " + ChatColor.YELLOW + "+0%",
                        ChatColor.GRAY + "Upgrade Cost: " + ChatColor.RED + "$4,000",
                        "",
                        ChatColor.GREEN + "Click to upgrade"
                ),
                "buy_upgrade",
                "crop_boost",
                true
        ));

        inventory.setItem(19, createMenuItem(
                Material.IRON_INGOT,
                ChatColor.WHITE + "Minion Speed Boost",
                List.of(
                        ChatColor.GRAY + "Make minions work faster",
                        ChatColor.GRAY + "Level: " + ChatColor.GREEN + "0",
                        ChatColor.GRAY + "Speed: " + ChatColor.YELLOW + "+0%",
                        ChatColor.GRAY + "Upgrade Cost: " + ChatColor.RED + "$6,000",
                        "",
                        ChatColor.GREEN + "Click to upgrade"
                ),
                "buy_upgrade",
                "minion_speed",
                true
        ));

        inventory.setItem(21, createMenuItem(
                Material.CHEST,
                ChatColor.DARK_PURPLE + "Storage Capacity",
                List.of(
                        ChatColor.GRAY + "Increase virtual storage",
                        ChatColor.GRAY + "Slots: " + ChatColor.GREEN + "27",
                        ChatColor.GRAY + "Next: " + ChatColor.YELLOW + "54",
                        ChatColor.GRAY + "Upgrade Cost: " + ChatColor.RED + "$7,500",
                        "",
                        ChatColor.GREEN + "Click to upgrade"
                ),
                "buy_upgrade",
                "storage",
                true
        ));

        inventory.setItem(23, createMenuItem(
                Material.TNT,
                ChatColor.RED + "Explosive Minion",
                List.of(
                        ChatColor.GRAY + "Minions clear area faster",
                        ChatColor.GRAY + "Level: " + ChatColor.GREEN + "0",
                        ChatColor.GRAY + "Effect: " + ChatColor.YELLOW + "Disabled",
                        ChatColor.GRAY + "Upgrade Cost: " + ChatColor.RED + "$10,000",
                        "",
                        ChatColor.GREEN + "Click to upgrade"
                ),
                "buy_upgrade",
                "explosive_minion",
                true
        ));

        inventory.setItem(25, createMenuItem(
                Material.ENDER_PEARL,
                ChatColor.DARK_PURPLE + "Teleport Pad",
                List.of(
                        ChatColor.GRAY + "Teleport between island points",
                        ChatColor.GRAY + "Pads: " + ChatColor.GREEN + "0",
                        ChatColor.GRAY + "Next: " + ChatColor.YELLOW + "1",
                        ChatColor.GRAY + "Upgrade Cost: " + ChatColor.RED + "$8,000",
                        "",
                        ChatColor.GREEN + "Click to upgrade"
                ),
                "buy_upgrade",
                "teleport_pad",
                true
        ));

        inventory.setItem(45, createMenuItem(
                Material.ARROW,
                ChatColor.YELLOW + "Back",
                List.of(ChatColor.GRAY + "Return to main menu"),
                "open_main",
                ""
        ));

        inventory.setItem(49, createMenuItem(
                Material.BARRIER,
                ChatColor.RED + "Close",
                List.of(ChatColor.GRAY + "Close this menu."),
                "close_menu",
                ""
        ));

        player.openInventory(inventory);
    }

    public void openMinionsMenu(Player player) {
        MenuHolder holder = new MenuHolder(MenuType.MINIONS);
        Inventory inventory = Bukkit.createInventory(holder, 54, TITLE_MINIONS);
        holder.inventory = inventory;

        fillBackground(inventory);

        inventory.setItem(4, createMenuItem(
                Material.ZOMBIE_SPAWN_EGG,
                ChatColor.DARK_AQUA + "Minion Overview",
                List.of(
                        ChatColor.GRAY + "Active Minions: " + ChatColor.GREEN + "0/3",
                        ChatColor.GRAY + "Total Generated: " + ChatColor.GOLD + "$0",
                        ChatColor.GRAY + "Items Collected: " + ChatColor.AQUA + "0",
                        "",
                        ChatColor.YELLOW + "Manage your minions here"
                ),
                "noop",
                "",
                true
        ));

        inventory.setItem(10, createMinionItem(player, "MINION_COBBLESTONE", Material.COBBLESTONE, "Cobblestone Minion", 1, 10));
        inventory.setItem(11, createMinionItem(player, "MINION_WOOD", Material.OAK_LOG, "Oak Minion", 1, 15));
        inventory.setItem(12, createMinionItem(player, "MINION_WHEAT", Material.WHEAT, "Wheat Minion", 1, 20));
        inventory.setItem(13, createMinionItem(player, "MINION_CARROT", Material.CARROT, "Carrot Minion", 1, 25));
        inventory.setItem(14, createMinionItem(player, "MINION_POTATO", Material.POTATO, "Potato Minion", 1, 25));
        inventory.setItem(15, createMinionItem(player, "MINION_PUMPKIN", Material.PUMPKIN, "Pumpkin Minion", 1, 30));
        inventory.setItem(16, createMinionItem(player, "MINION_MELON", Material.MELON, "Melon Minion", 1, 30));

        inventory.setItem(19, createMinionItem(player, "MINION_COAL", Material.COAL, "Coal Minion", 2, 50));
        inventory.setItem(20, createMinionItem(player, "MINION_IRON", Material.IRON_INGOT, "Iron Minion", 2, 100));
        inventory.setItem(21, createMinionItem(player, "MINION_GOLD", Material.GOLD_INGOT, "Gold Minion", 2, 150));
        inventory.setItem(22, createMinionItem(player, "MINION_DIAMOND", Material.DIAMOND, "Diamond Minion", 3, 500));
        inventory.setItem(23, createMinionItem(player, "MINION_EMERALD", Material.EMERALD, "Emerald Minion", 3, 750));
        inventory.setItem(24, createMinionItem(player, "MINION_LAPIS", Material.LAPIS_LAZULI, "Lapis Minion", 2, 75));

        inventory.setItem(28, createMinionItem(player, "MINION_SKELETON", Material.BONE, "Skeleton Minion", 3, 200));
        inventory.setItem(29, createMinionItem(player, "MINION_ZOMBIE", Material.ROTTEN_FLESH, "Zombie Minion", 3, 200));
        inventory.setItem(30, createMinionItem(player, "MINION_CREEPER", Material.GUNPOWDER, "Creeper Minion", 3, 250));
        inventory.setItem(31, createMinionItem(player, "MINION_SPIDER", Material.STRING, "Spider Minion", 3, 200));
        inventory.setItem(32, createMinionItem(player, "MINION_ENDERMAN", Material.ENDER_PEARL, "Enderman Minion", 4, 1000));

        inventory.setItem(45, createMenuItem(
                Material.ARROW,
                ChatColor.YELLOW + "Back",
                List.of(ChatColor.GRAY + "Return to main menu"),
                "open_main",
                ""
        ));

        inventory.setItem(49, createMenuItem(
                Material.BARRIER,
                ChatColor.RED + "Close",
                List.of(ChatColor.GRAY + "Close this menu."),
                "close_menu",
                ""
        ));

        player.openInventory(inventory);
    }

    private ItemStack createMinionItem(Player player, String minionId, Material material, String name, int tier, int cost) {
        return createMenuItem(
                material,
                ChatColor.AQUA + name + " " + ChatColor.GOLD + "Tier " + tier,
                List.of(
                        ChatColor.GRAY + "Generates resources automatically",
                        ChatColor.GRAY + "Tier: " + ChatColor.YELLOW + tier,
                        ChatColor.GRAY + "Action Time: " + ChatColor.GREEN + (20 - tier * 2) + "s",
                        ChatColor.GRAY + "Storage: " + ChatColor.BLUE + (tier * 10) + " slots",
                        ChatColor.GRAY + "Cost: " + ChatColor.RED + "$" + cost,
                        "",
                        ChatColor.GREEN + "Click to purchase"
                ),
                "buy_minion",
                minionId,
                true
        );
    }

    public void openSettingsMenu(Player player) {
        MenuHolder holder = new MenuHolder(MenuType.SETTINGS);
        Inventory inventory = Bukkit.createInventory(holder, 54, TITLE_SETTINGS);
        holder.inventory = inventory;

        fillBackground(inventory);

        inventory.setItem(10, createSettingsItem(
                Material.OAK_DOOR,
                ChatColor.GREEN + "Allow Visitors",
                ChatColor.GRAY + "Status: " + ChatColor.AQUA + "Enabled",
                "toggle_setting",
                "visitors",
                true
        ));

        inventory.setItem(12, createSettingsItem(
                Material.CHEST,
                ChatColor.BLUE + "Allow Chest Access",
                ChatColor.GRAY + "Status: " + ChatColor.RED + "Disabled",
                "toggle_setting",
                "chest_access",
                false
        ));

        inventory.setItem(14, createSettingsItem(
                Material.DIAMOND_PICKAXE,
                ChatColor.AQUA + "Allow Block Break",
                ChatColor.GRAY + "Status: " + ChatColor.RED + "Disabled",
                "toggle_setting",
                "block_break",
                false
        ));

        inventory.setItem(16, createSettingsItem(
                Material.GRASS_BLOCK,
                ChatColor.GREEN + "Allow Block Place",
                ChatColor.GRAY + "Status: " + ChatColor.RED + "Disabled",
                "toggle_setting",
                "block_place",
                false
        ));

        inventory.setItem(19, createSettingsItem(
                Material.FLINT_AND_STEEL,
                ChatColor.RED + "Allow Fire Damage",
                ChatColor.GRAY + "Status: " + ChatColor.RED + "Disabled",
                "toggle_setting",
                "fire_damage",
                false
        ));

        inventory.setItem(21, createSettingsItem(
                Material.BUCKET,
                ChatColor.DARK_AQUA + "Allow Bucket Use",
                ChatColor.GRAY + "Status: " + ChatColor.RED + "Disabled",
                "toggle_setting",
                "bucket_use",
                false
        ));

        inventory.setItem(23, createSettingsItem(
                Material.LEAD,
                ChatColor.GOLD + "Allow Animal Leash",
                ChatColor.GRAY + "Status: " + ChatColor.RED + "Disabled",
                "toggle_setting",
                "animal_leash",
                false
        ));

        inventory.setItem(25, createSettingsItem(
                Material.ZOMBIE_SPAWN_EGG,
                ChatColor.LIGHT_PURPLE + "Allow Mob Spawning",
                ChatColor.GRAY + "Status: " + ChatColor.AQUA + "Enabled",
                "toggle_setting",
                "mob_spawning",
                true
        ));

        inventory.setItem(31, createMenuItem(
                Material.NAME_TAG,
                ChatColor.DARK_PURPLE + "Island Description",
                List.of(
                        ChatColor.GRAY + "Set a description for",
                        ChatColor.GRAY + "your island.",
                        "",
                        ChatColor.YELLOW + "Click to edit"
                ),
                "set_description",
                "",
                false
        ));

        inventory.setItem(45, createMenuItem(
                Material.ARROW,
                ChatColor.YELLOW + "Back",
                List.of(ChatColor.GRAY + "Return to main menu"),
                "open_main",
                ""
        ));

        inventory.setItem(48, createMenuItem(
                Material.BOOK,
                ChatColor.DARK_RED + "Permissions",
                List.of(ChatColor.GRAY + "Manage member permissions"),
                "open_permissions",
                ""
        ));

        inventory.setItem(49, createMenuItem(
                Material.BARRIER,
                ChatColor.RED + "Close",
                List.of(ChatColor.GRAY + "Close this menu."),
                "close_menu",
                ""
        ));

        inventory.setItem(50, createMenuItem(
                Material.WRITABLE_BOOK,
                ChatColor.RED + "Banned Players",
                List.of(ChatColor.GRAY + "Manage banned players"),
                "open_banned",
                ""
        ));

        player.openInventory(inventory);
    }

    public void openPermissionsMenu(Player player) {
        MenuHolder holder = new MenuHolder(MenuType.PERMISSIONS);
        Inventory inventory = Bukkit.createInventory(holder, 54, TITLE_PERMISSIONS);
        holder.inventory = inventory;

        fillBackground(inventory);

        inventory.setItem(4, createMenuItem(
                Material.BOOK,
                ChatColor.GOLD + "Permission Levels",
                List.of(
                    ChatColor.GRAY + "Configure permissions for",
                    ChatColor.GRAY + "each member rank.",
                    "",
                    ChatColor.YELLOW + "Select a rank to configure"
                ),
                "noop",
                "",
                true
        ));

        inventory.setItem(11, createPermissionRankItem(
                Material.DIAMOND,
                ChatColor.AQUA + "Officer",
                ChatColor.GRAY + "Second-in-command permissions",
                "configure_rank",
                "officer"
        ));

        inventory.setItem(13, createPermissionRankItem(
                Material.IRON_INGOT,
                ChatColor.WHITE + "Member",
                ChatColor.GRAY + "Standard member permissions",
                "configure_rank",
                "member"
        ));

        inventory.setItem(15, createPermissionRankItem(
                Material.COBBLESTONE,
                ChatColor.GRAY + "Guest",
                ChatColor.GRAY + "Limited visitor permissions",
                "configure_rank",
                "guest"
        ));

        inventory.setItem(22, createMenuItem(
                Material.PAPER,
                ChatColor.YELLOW + "Permission Guide",
                List.of(
                        ChatColor.GRAY + "Click a rank above to customize",
                        ChatColor.GRAY + "what actions they can perform",
                        ChatColor.GRAY + "on your island.",
                        "",
                        ChatColor.DARK_GRAY + "Ranks: Officer > Member > Guest"
                ),
                "noop",
                "",
                true
        ));

        inventory.setItem(45, createMenuItem(
                Material.ARROW,
                ChatColor.YELLOW + "Back",
                List.of(ChatColor.GRAY + "Return to settings menu"),
                "open_settings",
                ""
        ));

        inventory.setItem(49, createMenuItem(
                Material.BARRIER,
                ChatColor.RED + "Close",
                List.of(ChatColor.GRAY + "Close this menu."),
                "close_menu",
                ""
        ));

        player.openInventory(inventory);
    }

    private ItemStack createMenuItem(Material type, String name, List<String> lore, String action, String value) {
        return createMenuItem(type, name, lore, action, value, false);
    }

    private ItemStack createMenuItem(Material type, String name, List<String> lore, String action, String value, boolean glow) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        if (glow) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, value);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSettingsItem(Material type, String name, String statusLine, String action, String value, boolean enabled) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(statusLine, "", ChatColor.YELLOW + "Click to toggle"));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        if (enabled) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, value);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPermissionRankItem(Material type, String name, String description, String action, String value) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of(description, "", ChatColor.GREEN + "Click to configure"));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, value);
        item.setItemMeta(meta);
        return item;
    }

    private void fillBackground(Inventory inventory) {
        ItemStack border = createMenuItem(
                Material.BLACK_STAINED_GLASS_PANE,
                ChatColor.BLACK + " ",
                List.of(),
                "noop",
                ""
        );
        for (int slot : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 46, 47, 48, 50, 51, 52, 53}) {
            if (slot < inventory.getSize()) {
                inventory.setItem(slot, border.clone());
            }
        }

        ItemStack accent = createMenuItem(
                Material.PURPLE_STAINED_GLASS_PANE,
                ChatColor.DARK_PURPLE + " ",
                List.of(),
                "noop",
                ""
        );
        for (int slot : new int[]{45, 53}) {
            if (slot < inventory.getSize()) {
                inventory.setItem(slot, accent.clone());
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory().getType() == InventoryType.PLAYER) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) {
            return;
        }
        ItemMeta meta = clicked.getItemMeta();
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action == null) {
            return;
        }
        String value = meta.getPersistentDataContainer().getOrDefault(valueKey, PersistentDataType.STRING, "");

        if (action.equals("noop")) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5F, 0.8F);
            return;
        }

        long now = System.currentTimeMillis();
        String cooldownKey = player.getUniqueId() + ":" + action;
        Long cooldownEnd = actionCooldowns.get(cooldownKey);
        if (cooldownEnd != null && cooldownEnd > now) {
            return;
        }
        actionCooldowns.put(cooldownKey, now + 500);

        switch (action) {
            case "open_main" -> {
                playUiClick(player);
                openMainMenu(player);
            }
            case "open_island" -> {
                playUiClick(player);
                openIslandMenu(player);
            }
            case "open_upgrades" -> {
                playUiClick(player);
                openUpgradesMenu(player);
            }
            case "open_minions" -> {
                playUiClick(player);
                openMinionsMenu(player);
            }
            case "open_settings" -> {
                playUiClick(player);
                openSettingsMenu(player);
            }
            case "open_permissions" -> {
                playUiClick(player);
                openPermissionsMenu(player);
            }
            case "toggle_setting" -> {
                playUiClick(player);
                player.sendMessage(ChatColor.GREEN + "Setting toggled: " + value);
                openSettingsMenu(player);
            }
            case "buy_upgrade" -> {
                playUiClick(player);
                player.sendMessage(ChatColor.YELLOW + "Upgrade purchase: " + value);
                openUpgradesMenu(player);
            }
            case "buy_minion" -> {
                playUiClick(player);
                player.sendMessage(ChatColor.YELLOW + "Minion purchase: " + value);
                openMinionsMenu(player);
            }
            case "configure_rank" -> {
                playUiClick(player);
                player.sendMessage(ChatColor.YELLOW + "Configuring rank: " + value);
            }
            case "close_menu" -> {
                playUiClick(player);
                player.closeInventory();
            }
            case "expand_island" -> {
                playUiClick(player);
                if (islandManager == null) {
                    player.sendMessage(ChatColor.RED + "Island system not available.");
                    return;
                }
                try {
                    int level = Integer.parseInt(value);
                    if (islandManager.expandIsland(player, level)) {
                        playUiSuccess(player);
                        openIslandMenu(player);
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid upgrade level.");
                }
            }
            case "change_biome", "set_spawn", "invite_member",
                 "kick_member", "rename_island", "set_description", "open_fast_travel",
                 "open_collection", "open_recipes", "open_enchanting", "open_banned" -> {
                playUiClick(player);
                player.sendMessage(ChatColor.YELLOW + "Feature coming soon: " + action);
            }
            default -> {
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    private void playUiClick(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
    }

    private void playUiSuccess(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.1F);
    }

    private void playUiError(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7F, 0.8F);
    }

    private Island createPlaceholderIsland(Player player) {
        if (islandManager == null || islandManager.getIslandWorld() == null) {
            return null;
        }
        return new Island(player.getUniqueId(), player.getName(), new org.bukkit.Location(islandManager.getIslandWorld(), 0, 80, 0));
    }

    private enum MenuType {
        MAIN,
        ISLAND,
        UPGRADES,
        MINIONS,
        SETTINGS,
        PERMISSIONS
    }

    private static final class MenuHolder implements InventoryHolder {
        private final MenuType type;
        private Inventory inventory;

        private MenuHolder(MenuType type) {
            this.type = type;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public MenuType type() {
            return type;
        }
    }
}
