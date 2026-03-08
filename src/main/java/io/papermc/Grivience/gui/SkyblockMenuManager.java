package io.papermc.Grivience.gui;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.collections.CollectionsManager;
import io.papermc.Grivience.collections.CollectionGUI;
import io.papermc.Grivience.skyblock.island.Island;
import io.papermc.Grivience.stats.SkyblockCombatEngine;
import io.papermc.Grivience.stats.SkyblockLevelManager;
import io.papermc.Grivience.stats.SkyblockManaManager;
import io.papermc.Grivience.stats.SkyblockPlayerStats;
import io.papermc.Grivience.stats.SkyblockStatsManager;
import io.papermc.Grivience.item.CustomArmorManager;
import io.papermc.Grivience.skyblock.island.IslandManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the main Skyblock Menu and its sub-menus.
 * Provides a central hub for player progression, island management, and system access.
 */
public final class SkyblockMenuManager implements Listener {
    private static final String TITLE_MAIN = SkyblockGui.title("Skyblock Menu");
    private static final String TITLE_ISLAND = SkyblockGui.title("Island Management");
    private static final String TITLE_UPGRADES = SkyblockGui.title("Island Upgrades");
    private static final String TITLE_MINIONS = SkyblockGui.title("Minion Management");
    private static final String TITLE_SETTINGS = SkyblockGui.title("Island Settings");
    private static final String TITLE_PERMISSIONS = SkyblockGui.title("Island Permissions");
    private static final String TITLE_LEVELING = SkyblockGui.title("Skyblock Leveling");
    private static final String TITLE_VISIT_SETTINGS = SkyblockGui.title("Visit Settings");

    private static final long TEXT_INPUT_TIMEOUT_MS = 60_000L;

    private final GriviencePlugin plugin;
    private IslandManager islandManager;
    private SkyblockLevelManager levelManager;
    private SkyblockStatsManager statsManager;
    private SkyblockManaManager manaManager;
    private SkyblockCombatEngine combatEngine;
    private CustomArmorManager armorManager;
    private io.papermc.Grivience.crafting.CraftingManager craftingManager;
    private io.papermc.Grivience.fasttravel.FastTravelGui fastTravelGui;
    private io.papermc.Grivience.minion.MinionGuiManager minionGuiManager;

    private io.papermc.Grivience.event.GlobalEventManager globalEventManager;

    private final NamespacedKey actionKey;
    private final NamespacedKey valueKey;
    private final Map<String, Long> actionCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, PendingTextInput> pendingTextInput = new ConcurrentHashMap<>();
    private final DecimalFormat wholeNumberFormat = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));
    private final DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");

    private static final long CONTEST_INTERVAL_MILLIS = 20 * 60 * 1000L;
    private static final String[] CONTEST_CROPS = {
            "Wheat", "Carrot", "Potato", "Sugar Cane", "Nether Wart", "Cactus"
    };

    public SkyblockMenuManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "skyblock-action");
        this.valueKey = new NamespacedKey(plugin, "skyblock-value");
    }

    public void setIslandManager(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    public void setCraftingManager(io.papermc.Grivience.crafting.CraftingManager craftingManager) {
        this.craftingManager = craftingManager;
    }

    public void setManagers(SkyblockLevelManager levelManager, SkyblockStatsManager statsManager, SkyblockManaManager manaManager, CustomArmorManager armorManager) {
        this.levelManager = levelManager;
        this.statsManager = statsManager;
        this.manaManager = manaManager;
        this.armorManager = armorManager;
    }

    public void setCombatEngine(SkyblockCombatEngine combatEngine) {
        this.combatEngine = combatEngine;
    }

    public void setFastTravelGui(io.papermc.Grivience.fasttravel.FastTravelGui fastTravelGui) {
        this.fastTravelGui = fastTravelGui;
    }

    public void setMinionGuiManager(io.papermc.Grivience.minion.MinionGuiManager minionGuiManager) {
        this.minionGuiManager = minionGuiManager;
    }

    public void setGlobalEventManager(io.papermc.Grivience.event.GlobalEventManager globalEventManager) {
        this.globalEventManager = globalEventManager;
    }

    public void openMainMenu(Player player) {
        MenuHolder holder = new MenuHolder(MenuType.MAIN);
        Inventory inventory = Bukkit.createInventory(holder, 54, TITLE_MAIN);
        holder.inventory = inventory;

        fillMainMenuBackground(inventory);

        Island island = islandManager != null ? islandManager.getIsland(player) : null;
        int islandLevel = island != null ? island.getUpgradeLevel() : 0;
        int islandSize = island != null ? island.getSize() : 0;
        String islandName = island != null ? island.getName() : "No Island";
        int totalIslands = islandManager != null ? islandManager.getTotalIslands() : 0;

        // Profile Head
        ItemStack profile = createMenuItem(
                Material.PLAYER_HEAD,
                ChatColor.GREEN + "Your Skyblock Profile",
                profileLore(player, islandName, islandLevel, islandSize, totalIslands),
                "open_island",
                "",
                false
        );
        if (profile.getItemMeta() instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(player);
            profile.setItemMeta(skullMeta);
        }
        inventory.setItem(13, profile);

        // Core Systems
        inventory.setItem(19, createMenuItem(Material.DIAMOND_SWORD, ChatColor.GREEN + "Your Skills", List.of(ChatColor.GRAY + "View your skill progression.", "", ChatColor.YELLOW + "Click to view!"), "open_skills", "", false));
        inventory.setItem(20, createMenuItem(Material.PAINTING, ChatColor.YELLOW + "Collections", List.of(ChatColor.GRAY + "View all discovered items.", "", ChatColor.YELLOW + "Click to view!"), "open_collection", "", false));
        inventory.setItem(21, createMenuItem(Material.BOOK, ChatColor.LIGHT_PURPLE + "Recipe Book", List.of(ChatColor.GRAY + "Browse unlocked recipes.", "", ChatColor.YELLOW + "Click to view!"), "open_recipes", "", false));
        inventory.setItem(22, createMenuItem(Material.EXPERIENCE_BOTTLE, ChatColor.AQUA + "Skyblock Leveling", List.of(ChatColor.GRAY + "Track your progression rewards.", "", ChatColor.YELLOW + "Click to view!"), "open_leveling", "", false));
        inventory.setItem(23, createMenuItem(Material.WRITABLE_BOOK, ChatColor.GOLD + "Quest Log", List.of(ChatColor.GRAY + "View active tasks.", "", ChatColor.YELLOW + "Click to view!"), "open_quests", "", false));
        inventory.setItem(24, createMenuItem(Material.CLOCK, ChatColor.YELLOW + "Calendar and Events", List.of(ChatColor.GRAY + "View upcoming events.", "", ChatColor.YELLOW + "Click to view!"), "open_contests", "", false));
        inventory.setItem(25, createMenuItem(Material.CHEST, ChatColor.GREEN + "Storage", List.of(ChatColor.GRAY + "Access your personal storage.", "", ChatColor.YELLOW + "Click to view!"), "open_storage", "", false));

        // Sub-Systems
        inventory.setItem(28, createMenuItem(Material.GOLD_INGOT, ChatColor.GOLD + "Bank", List.of(ChatColor.GRAY + "Store coins safely.", "", ChatColor.YELLOW + "Click to open!"), "open_bank", "", false));
        inventory.setItem(29, createMenuItem(Material.BUNDLE, ChatColor.GOLD + "Your Bags", List.of(ChatColor.GRAY + "Access your specialized bags.", "", ChatColor.YELLOW + "Click to view!"), "open_bags", "", false));
        inventory.setItem(30, createMenuItem(Material.BONE, ChatColor.GREEN + "Pets", List.of(ChatColor.GRAY + "Manage your pets.", "", ChatColor.YELLOW + "Click to view!"), "open_pets", "", false));
        inventory.setItem(31, createMenuItem(Material.CRAFTING_TABLE, ChatColor.GREEN + "Crafting Table", List.of(ChatColor.GRAY + "Open your crafting grid.", "", ChatColor.YELLOW + "Click to open!"), "open_crafting", "", false));
        inventory.setItem(32, createMenuItem(Material.LEATHER_CHESTPLATE, ChatColor.GREEN + "Wardrobe", List.of(ChatColor.GRAY + "Swap armor sets.", "", ChatColor.YELLOW + "Click to view!"), "open_wardrobe", "", false));

        // Bottom Row Actions
        inventory.setItem(48, createMenuItem(Material.GLOBE_BANNER_PATTERN, ChatColor.AQUA + "Fast Travel", List.of(ChatColor.GRAY + "Travel quickly across Hubs.", "", ChatColor.YELLOW + "Click to open!"), "open_fast_travel", "", false));
        inventory.setItem(49, createMenuItem(Material.NAME_TAG, ChatColor.LIGHT_PURPLE + "Profile Management", List.of(ChatColor.GRAY + "Switch between island profiles.", "", ChatColor.YELLOW + "Click to view!"), "open_profiles", "", false));
        inventory.setItem(50, createMenuItem(Material.BARRIER, ChatColor.RED + "Close", List.of(ChatColor.GRAY + "Close this menu"), "close_menu", ""));
        inventory.setItem(51, createMenuItem(
                Material.EMERALD,
                ChatColor.GREEN + "NPC Commodity Sell",
                List.of(
                        ChatColor.GRAY + "Sell Bazaar commodities to NPC buyback.",
                        ChatColor.DARK_GRAY + "AH-style gear is blocked.",
                        "",
                        ChatColor.YELLOW + "Click to open!"
                ),
                "open_npc_sell_shop",
                "",
                false
        ));
        inventory.setItem(52, createMenuItem(Material.REDSTONE_TORCH, ChatColor.RED + "Settings", List.of(ChatColor.GRAY + "Configure your settings.", "", ChatColor.YELLOW + "Click to view!"), "open_settings", "", false));

        player.openInventory(inventory);
    }

    private List<String> profileLore(Player player, String islandName, int islandLevel, int islandSize, int totalIslands) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "View your equipment, stats,");
        lore.add(ChatColor.GRAY + "skills, and more.");
        lore.add("");
        lore.add(ChatColor.GRAY + "Island: " + ChatColor.AQUA + islandName);
        lore.add(ChatColor.GRAY + "Island Tier: " + ChatColor.GREEN + islandLevel);
        lore.add(ChatColor.GRAY + "Island Size: " + ChatColor.GREEN + islandSize + "x" + islandSize);
        lore.add("");

        int sbLevel = levelManager != null ? levelManager.getLevel(player) : 0;
        lore.add(ChatColor.GOLD + "Skyblock Level: " + ChatColor.GREEN + sbLevel + ChatColor.AQUA + " \u272f");
        lore.add("");

        if (combatEngine != null) {
            SkyblockPlayerStats stats = combatEngine.stats(player);
            if (stats != null) {
                lore.add(ChatColor.RED + "\u2764 Health: " + ChatColor.GREEN + (int)stats.health());
                lore.add(ChatColor.GREEN + "\u2748 Defense: " + ChatColor.GREEN + (int)stats.defense());
                lore.add(ChatColor.RED + "\u2741 Strength: " + ChatColor.GREEN + (int)stats.strength());
                lore.add(ChatColor.AQUA + "\u270e Intelligence: " + ChatColor.AQUA + (int)stats.intelligence());
            }
        }
        
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to view!");
        return lore;
    }

    public void openIslandMenu(Player player) {
        MenuHolder holder = new MenuHolder(MenuType.ISLAND);
        Inventory inv = Bukkit.createInventory(holder, 54, TITLE_ISLAND);
        holder.inventory = inv;
        fillBackground(inv);

        inv.setItem(10, createMenuItem(Material.GRASS_BLOCK, ChatColor.GREEN + "Change Biome", List.of(ChatColor.GRAY + "Change your island biome.", "", ChatColor.YELLOW + "Click to select!"), "open_biomes", ""));
        inv.setItem(12, createMenuItem(Material.BEACON, ChatColor.AQUA + "Set Spawn Point", List.of(ChatColor.GRAY + "Set island spawn point.", "", ChatColor.YELLOW + "Click to set!"), "set_spawn", ""));
        inv.setItem(14, createMenuItem(Material.OAK_SAPLING, ChatColor.GREEN + "Island Upgrades", List.of(ChatColor.GRAY + "Expand your island size.", "", ChatColor.YELLOW + "Click to view!"), "open_upgrades", ""));
        inv.setItem(16, createMenuItem(Material.FURNACE, ChatColor.GOLD + "Minion Management", List.of(ChatColor.GRAY + "Manage and collect from your minions.", "", ChatColor.YELLOW + "Click to open!"), "open_minions", ""));
        
        inv.setItem(45, createMenuItem(Material.ARROW, ChatColor.YELLOW + "Back", List.of(ChatColor.GRAY + "Go back to Skyblock Menu"), "open_main", ""));
        inv.setItem(49, createMenuItem(Material.BARRIER, ChatColor.RED + "Close", List.of(ChatColor.GRAY + "Close this menu"), "close_menu", ""));

        player.openInventory(inv);
    }

    public void openBiomesMenu(Player player) {
        MenuHolder holder = new MenuHolder(MenuType.MAIN);
        Inventory inv = Bukkit.createInventory(holder, 45, SkyblockGui.title("Island Biomes"));
        holder.inventory = inv;
        fillBackground(inv);

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        
        org.bukkit.block.Biome[] biomes = {
            org.bukkit.block.Biome.PLAINS, org.bukkit.block.Biome.DESERT, org.bukkit.block.Biome.FOREST,
            org.bukkit.block.Biome.JUNGLE, org.bukkit.block.Biome.SWAMP, org.bukkit.block.Biome.SAVANNA,
            org.bukkit.block.Biome.DARK_FOREST, org.bukkit.block.Biome.MUSHROOM_FIELDS, org.bukkit.block.Biome.SNOWY_PLAINS
        };
        
        Material[] materials = {
            Material.GRASS_BLOCK, Material.SAND, Material.OAK_LOG,
            Material.JUNGLE_LOG, Material.VINE, Material.ACACIA_LOG,
            Material.DARK_OAK_LOG, Material.MYCELIUM, Material.SNOW_BLOCK
        };

        for (int i = 0; i < biomes.length; i++) {
            org.bukkit.block.Biome biome = biomes[i];
            String name = biome.getKey().getKey().replace("_", " ");
            name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
            
            inv.setItem(slots[i], createMenuItem(materials[i], ChatColor.GREEN + name, 
                    List.of(ChatColor.GRAY + "Click to change your", ChatColor.GRAY + "island biome to " + name + "."), 
                    "set_biome", biome.getKey().getKey().toUpperCase()));
        }

        inv.setItem(36, createMenuItem(Material.ARROW, ChatColor.YELLOW + "Back", List.of(ChatColor.GRAY + "Go back to Island Menu"), "open_island", ""));
        inv.setItem(40, createMenuItem(Material.BARRIER, ChatColor.RED + "Close", List.of(ChatColor.GRAY + "Close this menu"), "close_menu", ""));

        player.openInventory(inv);
    }

    public void openSettingsMenu(Player player) {
        MenuHolder holder = new MenuHolder(MenuType.SETTINGS);
        Inventory inv = Bukkit.createInventory(holder, 54, TITLE_SETTINGS);
        holder.inventory = inv;
        fillBackground(inv);

        Island island = islandManager != null ? islandManager.getIsland(player) : null;
        boolean isOwner = island != null && island.getOwner() != null && island.getOwner().equals(player.getUniqueId());

        if (isOwner && islandManager != null) {
            int computedLimit = islandManager.computeGuestLimit(player);
            if (computedLimit != island.getGuestLimit()) {
                island.setGuestLimit(computedLimit);
                islandManager.saveIsland(island);
            }
        }

        if (island == null) {
            inv.setItem(22, createMenuItem(
                    Material.BARRIER,
                    ChatColor.RED + "No Island",
                    List.of(
                            ChatColor.GRAY + "Create an island to configure",
                            ChatColor.GRAY + "Island Settings.",
                            "",
                            ChatColor.YELLOW + "Use /island create"
                    ),
                    "noop",
                    ""
            ));
        } else {
            inv.setItem(10, createMenuItem(
                    Material.NAME_TAG,
                    ChatColor.GREEN + "Island Name",
                    List.of(
                            ChatColor.GRAY + "Rename your island.",
                            "",
                            ChatColor.GRAY + "Current: " + ChatColor.AQUA + island.getName(),
                            "",
                            isOwner ? ChatColor.YELLOW + "Click to edit!" : ChatColor.RED + "Only the island owner can edit."
                    ),
                    "edit_island_name",
                    ""
            ));

            List<String> descLore = new ArrayList<>();
            descLore.add(ChatColor.GRAY + "Edit your island description.");
            descLore.add("");
            descLore.add(ChatColor.GRAY + "Current:");
            String desc = island.getDescription();
            if (desc == null || desc.isBlank()) {
                descLore.add(ChatColor.DARK_GRAY + "<none>");
            } else {
                for (String line : wrapPlainText(desc, 28, 3)) {
                    descLore.add(ChatColor.GRAY + line);
                }
            }
            descLore.add("");
            descLore.add(isOwner ? ChatColor.YELLOW + "Click to edit!" : ChatColor.RED + "Only the island owner can edit.");

            inv.setItem(12, createMenuItem(
                    Material.WRITABLE_BOOK,
                    ChatColor.LIGHT_PURPLE + "Island Description",
                    descLore,
                    "edit_island_desc",
                    ""
            ));

            Island.VisitPolicy policy = island.getVisitPolicy();
            String guestLimitDisplay = island.getGuestLimit() < 0 ? "UNLIMITED" : String.valueOf(Math.max(1, island.getGuestLimit()));
            inv.setItem(14, createMenuItem(
                    Material.OAK_DOOR,
                    ChatColor.AQUA + "Visit Settings",
                    List.of(
                            ChatColor.GRAY + "Control who can visit your island.",
                            "",
                            ChatColor.GRAY + "Policy: " + ChatColor.YELLOW + (policy != null ? policy.name() : Island.VisitPolicy.OFF.name()),
                            ChatColor.GRAY + "Guest Limit: " + ChatColor.AQUA + guestLimitDisplay,
                            "",
                            isOwner ? ChatColor.YELLOW + "Click to configure!" : ChatColor.RED + "Only the island owner can edit."
                    ),
                    "open_visit_settings",
                    ""
            ));

            Location safeGuestSpawn = islandManager != null ? islandManager.getSafeGuestSpawnLocation(island) : island.getGuestSpawnPoint();
            String guestSpawnDisplay = safeGuestSpawn != null
                    ? (safeGuestSpawn.getBlockX() + ", " + safeGuestSpawn.getBlockY() + ", " + safeGuestSpawn.getBlockZ())
                    : "Unknown";

            inv.setItem(16, createMenuItem(
                    Material.ENDER_PEARL,
                    ChatColor.GREEN + "Guest Spawn Point",
                    List.of(
                            ChatColor.GRAY + "Set where visitors spawn.",
                            "",
                            ChatColor.GRAY + "Current: " + ChatColor.AQUA + guestSpawnDisplay,
                            "",
                            ChatColor.DARK_GRAY + "Must be on your island.",
                            isOwner ? ChatColor.YELLOW + "Click: set to your location" : ChatColor.RED + "Only the island owner can edit.",
                            isOwner ? ChatColor.YELLOW + "Shift-Click: reset" : ChatColor.DARK_GRAY + ""
                    ),
                    "guest_spawn",
                    ""
            ));

            List<String> visitLore = new ArrayList<>();
            visitLore.add(ChatColor.GRAY + "Total Visits: " + ChatColor.GREEN + wholeNumberFormat.format(Math.max(0L, island.getTotalVisits())));
            visitLore.add("");
            Map<String, Island.VisitorStats> visits = island.getVisits();
            if (visits.isEmpty()) {
                visitLore.add(ChatColor.RED + "No visitors yet.");
            } else {
                visitLore.add(ChatColor.GRAY + "Recent Visitors:");
                for (String name : island.getRecentVisitors(5)) {
                    visitLore.add(ChatColor.GRAY + " \u25cf " + ChatColor.AQUA + name);
                }
            }
            inv.setItem(28, createMenuItem(Material.BOOK, ChatColor.YELLOW + "Visitor Log", visitLore, "noop", ""));

            List<String> coopLore = new ArrayList<>();
            coopLore.add(ChatColor.GRAY + "Your co-op members.");
            coopLore.add("");
            if (island.getMembers().isEmpty()) {
                coopLore.add(ChatColor.RED + "No members.");
            } else {
                coopLore.add(ChatColor.GRAY + "Members: " + ChatColor.AQUA + island.getMembers().size());
                int shown = 0;
                for (UUID memberId : island.getMembers()) {
                    if (shown++ >= 5) {
                        coopLore.add(ChatColor.DARK_GRAY + "...");
                        break;
                    }
                    OfflinePlayer offline = Bukkit.getOfflinePlayer(memberId);
                    String name = offline != null && offline.getName() != null ? offline.getName() : memberId.toString().substring(0, 8);
                    coopLore.add(ChatColor.GRAY + " \u25cf " + ChatColor.AQUA + name);
                }
            }
            coopLore.add("");
            coopLore.add(ChatColor.YELLOW + "Use /island coop to manage.");
            inv.setItem(30, createMenuItem(Material.PLAYER_HEAD, ChatColor.GOLD + "Co-op Members", coopLore, "noop", ""));

            inv.setItem(32, createMenuItem(
                    Material.STICK,
                    ChatColor.RED + "Kick All Visitors",
                    List.of(
                            ChatColor.GRAY + "Kick all visitors from",
                            ChatColor.GRAY + "your island.",
                            "",
                            ChatColor.YELLOW + "Click to kick all!"
                    ),
                    "kick_all_visitors",
                    ""
            ));

            inv.setItem(34, createMenuItem(
                    Material.REDSTONE_TORCH,
                    ChatColor.RED + "Permissions",
                    List.of(
                            ChatColor.GRAY + "View island protection rules",
                            ChatColor.GRAY + "and future permission toggles.",
                            "",
                            ChatColor.YELLOW + "Click to view!"
                    ),
                    "open_permissions",
                    ""
            ));
        }

        inv.setItem(45, createMenuItem(Material.ARROW, ChatColor.YELLOW + "Back", List.of(ChatColor.GRAY + "Go back to Skyblock Menu"), "open_main", ""));
        inv.setItem(49, createMenuItem(Material.BARRIER, ChatColor.RED + "Close", List.of(ChatColor.GRAY + "Close this menu"), "close_menu", ""));

        player.openInventory(inv);
    }

    public void openVisitSettingsMenu(Player player) {
        MenuHolder holder = new MenuHolder(MenuType.VISIT_SETTINGS);
        Inventory inv = Bukkit.createInventory(holder, 27, TITLE_VISIT_SETTINGS);
        holder.inventory = inv;
        fillBackground(inv);

        Island island = islandManager != null ? islandManager.getIsland(player) : null;
        boolean isOwner = island != null && island.getOwner() != null && island.getOwner().equals(player.getUniqueId());

        if (island == null) {
            inv.setItem(13, createMenuItem(
                    Material.BARRIER,
                    ChatColor.RED + "No Island",
                    List.of(ChatColor.GRAY + "Create an island to configure", ChatColor.GRAY + "visit settings."),
                    "noop",
                    ""
            ));
        } else {
            Island.VisitPolicy current = island.getVisitPolicy();
            inv.setItem(10, createMenuItem(
                    Material.BARRIER,
                    ChatColor.RED + "Closed",
                    List.of(
                            ChatColor.GRAY + "Nobody can visit your island.",
                            ChatColor.DARK_GRAY + "Invites still work.",
                            "",
                            isOwner ? ChatColor.YELLOW + "Click to select!" : ChatColor.RED + "Only the island owner can edit."
                    ),
                    "set_visit_policy",
                    Island.VisitPolicy.OFF.name(),
                    current == Island.VisitPolicy.OFF
            ));
            inv.setItem(12, createMenuItem(
                    Material.LIME_DYE,
                    ChatColor.GREEN + "Anyone",
                    List.of(
                            ChatColor.GRAY + "Anyone can visit your island.",
                            "",
                            isOwner ? ChatColor.YELLOW + "Click to select!" : ChatColor.RED + "Only the island owner can edit."
                    ),
                    "set_visit_policy",
                    Island.VisitPolicy.ANYONE.name(),
                    current == Island.VisitPolicy.ANYONE
            ));
            inv.setItem(14, createMenuItem(
                    Material.PLAYER_HEAD,
                    ChatColor.AQUA + "Friends",
                    List.of(
                            ChatColor.GRAY + "Only friends can visit.",
                            ChatColor.DARK_GRAY + "Friends = same party.",
                            "",
                            isOwner ? ChatColor.YELLOW + "Click to select!" : ChatColor.RED + "Only the island owner can edit."
                    ),
                    "set_visit_policy",
                    Island.VisitPolicy.FRIENDS.name(),
                    current == Island.VisitPolicy.FRIENDS
            ));
            inv.setItem(16, createMenuItem(
                    Material.NETHER_STAR,
                    ChatColor.LIGHT_PURPLE + "Guild",
                    List.of(
                            ChatColor.GRAY + "Only guild members can visit.",
                            ChatColor.DARK_GRAY + "Uses LuckPerms meta.",
                            "",
                            isOwner ? ChatColor.YELLOW + "Click to select!" : ChatColor.RED + "Only the island owner can edit."
                    ),
                    "set_visit_policy",
                    Island.VisitPolicy.GUILD.name(),
                    current == Island.VisitPolicy.GUILD
            ));
        }

        inv.setItem(18, createMenuItem(Material.ARROW, ChatColor.YELLOW + "Back", List.of(ChatColor.GRAY + "Go back to Island Settings"), "open_settings", ""));
        inv.setItem(26, createMenuItem(Material.BARRIER, ChatColor.RED + "Close", List.of(ChatColor.GRAY + "Close this menu"), "close_menu", ""));

        player.openInventory(inv);
    }

    public void openLevelingMenu(Player player) {
        if (plugin.getSkyblockLevelGui() != null) {
            plugin.getSkyblockLevelGui().openMainMenu(player);
        } else {
            player.sendMessage(ChatColor.RED + "Leveling GUI is currently unavailable.");
        }
    }

    public void openBagsMenu(Player player) {
        MenuHolder holder = new MenuHolder(MenuType.MAIN);
        Inventory inv = Bukkit.createInventory(holder, 54, SkyblockGui.title("Your Bags"));
        holder.inventory = inv;
        fillMainMenuBackground(inv);

        inv.setItem(20, createMenuItem(Material.REDSTONE, ChatColor.GOLD + "Accessory Bag", List.of(ChatColor.GRAY + "Stores accessories.", "", ChatColor.YELLOW + "Click to open!"), "open_bag", "ACCESSORY_BAG"));
        inv.setItem(22, createMenuItem(Material.POTION, ChatColor.LIGHT_PURPLE + "Potion Bag", List.of(ChatColor.GRAY + "Stores potions.", "", ChatColor.YELLOW + "Click to open!"), "open_bag", "POTION_BAG"));
        inv.setItem(48, createMenuItem(Material.ARROW, ChatColor.YELLOW + "Back", List.of(ChatColor.GRAY + "Go back to Skyblock Menu"), "open_main", ""));
        inv.setItem(50, createMenuItem(Material.BARRIER, ChatColor.RED + "Close", List.of(ChatColor.GRAY + "Close this menu"), "close_menu", ""));

        player.openInventory(inv);
    }

    public void openUpgradesMenu(Player player) {
        if (islandManager == null) {
            player.sendMessage(ChatColor.RED + "Island system unavailable.");
            return;
        }
        Island island = islandManager.getIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return;
        }

        MenuHolder holder = new MenuHolder(MenuType.MAIN);
        Inventory inv = Bukkit.createInventory(holder, 27, TITLE_UPGRADES);
        holder.inventory = inv;
        fillBackground(inv);

        int nextLevel = island.getUpgradeLevel() + 1;
        int nextSize = islandManager.getNextUpgradeSize(island);
        double cost = islandManager.getUpgradeCost(nextLevel);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current Level: " + ChatColor.GREEN + island.getUpgradeLevel());
        lore.add(ChatColor.GRAY + "Current Size: " + ChatColor.GREEN + island.getSize() + "x" + island.getSize());
        lore.add("");
        
        if (cost > 0) {
            lore.add(ChatColor.GRAY + "Next Size: " + ChatColor.AQUA + nextSize + "x" + nextSize);
            lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + "$" + wholeNumberFormat.format(cost));
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to upgrade!");
        } else {
            lore.add(ChatColor.GOLD + "MAX LEVEL REACHED");
        }

        inv.setItem(13, createMenuItem(Material.OAK_SAPLING, ChatColor.GREEN + "Expand Island", lore, "expand_island", String.valueOf(nextLevel)));
        inv.setItem(18, createMenuItem(Material.ARROW, ChatColor.YELLOW + "Back", List.of(ChatColor.GRAY + "Go back to Island Menu"), "open_island", ""));

        player.openInventory(inv);
    }

    public void openMinionsMenu(Player player) {
        if (minionGuiManager == null) {
            player.sendMessage(ChatColor.RED + "Minion system unavailable.");
            return;
        }
        minionGuiManager.openOverview(player);
    }

    public void openPermissionsMenu(Player player) {
        MenuHolder holder = new MenuHolder(MenuType.PERMISSIONS);
        Inventory inv = Bukkit.createInventory(holder, 27, TITLE_PERMISSIONS);
        holder.inventory = inv;
        fillBackground(inv);

        inv.setItem(13, createMenuItem(
                Material.SHIELD,
                ChatColor.GREEN + "Island Protections",
                List.of(
                        ChatColor.GRAY + "Visitors are blocked from:",
                        ChatColor.GRAY + "\u25cf Breaking/placing blocks",
                        ChatColor.GRAY + "\u25cf Opening containers",
                        ChatColor.GRAY + "\u25cf Using redstone/doors",
                        ChatColor.GRAY + "\u25cf Interacting with entities",
                        ChatColor.GRAY + "\u25cf Attacking players/mobs",
                        "",
                        ChatColor.DARK_GRAY + "Custom permission toggles",
                        ChatColor.DARK_GRAY + "coming soon."
                ),
                "noop",
                ""
        ));

        inv.setItem(18, createMenuItem(Material.ARROW, ChatColor.YELLOW + "Back", List.of(ChatColor.GRAY + "Go back to Island Settings"), "open_settings", ""));
        inv.setItem(26, createMenuItem(Material.BARRIER, ChatColor.RED + "Close", List.of(ChatColor.GRAY + "Close this menu"), "close_menu", ""));

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getInventory())) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action == null) return;

        String value = meta.getPersistentDataContainer().getOrDefault(valueKey, PersistentDataType.STRING, "");

        long now = System.currentTimeMillis();
        String cooldownKey = player.getUniqueId() + ":" + action;
        if (actionCooldowns.getOrDefault(cooldownKey, 0L) > now) return;
        actionCooldowns.put(cooldownKey, now + 200);

        playUiClick(player);

        switch (action) {
            case "open_main" -> openMainMenu(player);
            case "open_island" -> openIslandMenu(player);
            case "open_settings" -> openSettingsMenu(player);
            case "open_visit_settings" -> openVisitSettingsMenu(player);
            case "open_permissions" -> openPermissionsMenu(player);
            case "open_bags" -> openBagsMenu(player);
            case "open_bag" -> {
                // Requested server behavior: bag tiles route back to main Skyblock menu.
                openMainMenu(player);
            }
            case "open_skills" -> plugin.getServer().dispatchCommand(player, "skills");
            case "open_collection" -> {
                if (plugin.getCollectionGui() != null) plugin.getCollectionGui().openMainGui(player);
                else player.sendMessage(ChatColor.RED + "Collections unavailable.");
            }
            case "open_recipes" -> {
                if (craftingManager != null) craftingManager.openCraftingMenu(player);
                else player.sendMessage(ChatColor.RED + "Crafting unavailable.");
            }
            case "open_leveling" -> openLevelingMenu(player);
            case "open_quests" -> plugin.getServer().dispatchCommand(player, "quest progress");
            case "open_storage" -> {
                if (plugin.getStorageGui() != null) plugin.getStorageGui().openMainMenu(player);
                else player.sendMessage(ChatColor.RED + "Storage unavailable.");
            }
            case "open_crafting" -> {
                // Always open the vanilla crafting table UI; recipe browsing is available via /craft.
                player.openWorkbench(player.getLocation(), true);
            }
            case "open_bank" -> plugin.getServer().dispatchCommand(player, "bank");
            case "open_pets" -> plugin.getServer().dispatchCommand(player, "pets");
            case "open_wardrobe" -> plugin.getServer().dispatchCommand(player, "wardrobe");
            case "open_fast_travel" -> {
                if (fastTravelGui != null) fastTravelGui.open(player);
                else player.sendMessage(ChatColor.RED + "Fast travel unavailable.");
            }
            case "open_profiles" -> {
                if (plugin.getProfileGui() != null) plugin.getProfileGui().open(player);
                else player.sendMessage(ChatColor.RED + "Profile system unavailable.");
            }
            case "open_npc_sell_shop" -> {
                if (plugin.getNpcSellShopGui() != null) {
                    plugin.getNpcSellShopGui().open(player);
                } else {
                    player.sendMessage(ChatColor.RED + "NPC commodity shop is unavailable.");
                }
            }
            case "open_contests" -> openContestsMenu(player);
            case "open_upgrades" -> openUpgradesMenu(player);
            case "open_minions" -> openMinionsMenu(player);
            case "open_biomes" -> openBiomesMenu(player);
            case "set_biome" -> {
                if (islandManager == null) return;
                Island island = islandManager.getIsland(player);
                if (island == null || !island.getOwner().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "You must be on your own island to change the biome!");
                    return;
                }
                try {
                    org.bukkit.block.Biome biome = Registry.BIOME.get(org.bukkit.NamespacedKey.minecraft(value.toLowerCase()));
                    if (biome == null) {
                        player.sendMessage(ChatColor.RED + "Invalid biome.");
                        return;
                    }
                    islandManager.setIslandBiome(island, biome);
                    player.sendMessage(ChatColor.GREEN + "Island biome changed to " + value.replace("_", " ").toLowerCase() + "!");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    player.closeInventory();
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Error setting biome.");
                }
            }
            case "set_spawn" -> {
                if (islandManager == null) {
                    player.sendMessage(ChatColor.RED + "Island system unavailable.");
                    return;
                }
                Island island = islandManager.getIsland(player);
                if (island == null || !island.getOwner().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "You must be on your own island to set the spawn point!");
                    return;
                }
                if (!island.isWithinIsland(player.getLocation())) {
                    player.sendMessage(ChatColor.RED + "You must be within your island boundaries!");
                    return;
                }
                island.setSpawnPoint(player.getLocation());
                islandManager.saveIsland(island);
                player.sendMessage(ChatColor.GREEN + "Island spawn point set to your current location!");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            }
            case "expand_island" -> {
                if (islandManager == null) return;
                try {
                    int level = Integer.parseInt(value);
                    if (islandManager.expandIsland(player, level)) {
                        player.closeInventory();
                    } else {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    }
                } catch (Exception ignored) {}
            }
            case "set_visit_policy" -> {
                if (islandManager == null) {
                    player.sendMessage(ChatColor.RED + "Island system unavailable.");
                    return;
                }
                Island island = islandManager.getIsland(player);
                if (island == null || island.getOwner() == null || !island.getOwner().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Only the island owner can change visit settings!");
                    return;
                }
                Island.VisitPolicy policy;
                try {
                    policy = Island.VisitPolicy.valueOf(value.trim().toUpperCase(Locale.ROOT));
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "Invalid visit setting.");
                    return;
                }
                island.setVisitPolicy(policy);
                island.setGuestLimit(islandManager.computeGuestLimit(player));
                islandManager.saveIsland(island);
                player.sendMessage(ChatColor.GREEN + "Visit setting updated to " + ChatColor.YELLOW + policy.name() + ChatColor.GREEN + ".");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.1f);
                openVisitSettingsMenu(player);
            }
            case "guest_spawn" -> {
                if (islandManager == null) {
                    player.sendMessage(ChatColor.RED + "Island system unavailable.");
                    return;
                }
                Island island = islandManager.getIsland(player);
                if (island == null || island.getOwner() == null || !island.getOwner().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "Only the island owner can change the guest spawn point!");
                    return;
                }

                if (event.isShiftClick()) {
                    island.setGuestSpawnPoint(null);
                    islandManager.saveIsland(island);
                    player.sendMessage(ChatColor.YELLOW + "Guest spawn point reset.");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.1f);
                    openSettingsMenu(player);
                    return;
                }

                if (!island.isWithinIsland(player.getLocation())) {
                    player.sendMessage(ChatColor.RED + "You must be on your island to set the guest spawn point!");
                    return;
                }
                island.setGuestSpawnPoint(player.getLocation());
                islandManager.saveIsland(island);
                player.sendMessage(ChatColor.GREEN + "Guest spawn point set to your current location!");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.1f);
                openSettingsMenu(player);
            }
            case "kick_all_visitors" -> {
                plugin.getServer().dispatchCommand(player, "island kickall");
                player.closeInventory();
            }
            case "edit_island_name" -> beginTextInput(player, PendingTextInputType.ISLAND_NAME);
            case "edit_island_desc" -> beginTextInput(player, PendingTextInputType.ISLAND_DESCRIPTION);
            case "close_menu" -> player.closeInventory();
            case "coming_soon" -> player.sendMessage(ChatColor.YELLOW + "Feature coming soon!");
        }
    }

    public void openContestsMenu(Player player) {
        MenuHolder holder = new MenuHolder(MenuType.MAIN);
        Inventory inv = Bukkit.createInventory(holder, 54, SkyblockGui.title("Calendar and Events"));
        holder.inventory = inv;
        fillBackground(inv);

        // Active Events Slot 22
        List<String> activeLore = new ArrayList<>();
        activeLore.add(ChatColor.GRAY + "View all currently active");
        activeLore.add(ChatColor.GRAY + "server-wide events.");
        activeLore.add("");
        
        if (globalEventManager != null) {
            List<String> active = globalEventManager.getActiveEvents();
            if (active.isEmpty()) {
                activeLore.add(ChatColor.RED + "No active events.");
            } else {
                for (String event : active) {
                    activeLore.add(ChatColor.GRAY + " \u25cf " + event);
                }
            }
            
            long remaining = globalEventManager.getXpBoostRemainingMillis();
            if (remaining > 0) {
                activeLore.add("");
                activeLore.add(ChatColor.GRAY + "XP Boost ends in: " + ChatColor.YELLOW + (remaining / 60000) + "m");
            }
        } else {
            activeLore.add(ChatColor.RED + "Event system unavailable.");
        }
        
        inv.setItem(22, createMenuItem(Material.EMERALD, ChatColor.GREEN + "Active Events", activeLore, "noop", ""));

        // Farming Contest (Mockup based on README) Slot 24
        List<String> farmingLore = new ArrayList<>();
        farmingLore.add(ChatColor.GRAY + "Next contest starts in: " + ChatColor.YELLOW + "14m 20s");
        farmingLore.add("");
        farmingLore.add(ChatColor.GRAY + "Featured Crops:");
        farmingLore.add(ChatColor.GRAY + " \u25cf " + ChatColor.GOLD + "Wheat");
        farmingLore.add(ChatColor.GRAY + " \u25cf " + ChatColor.GOLD + "Carrot");
        farmingLore.add(ChatColor.GRAY + " \u25cf " + ChatColor.GOLD + "Potato");
        farmingLore.add("");
        farmingLore.add(ChatColor.YELLOW + "Click to view full schedule!");
        
        inv.setItem(24, createMenuItem(Material.GOLDEN_HOE, ChatColor.YELLOW + "Farming Contests", farmingLore, "coming_soon", "farming_contest"));

        inv.setItem(48, createMenuItem(Material.ARROW, ChatColor.YELLOW + "Back", List.of(ChatColor.GRAY + "Go back to Skyblock Menu"), "open_main", ""));
        inv.setItem(49, createMenuItem(Material.BARRIER, ChatColor.RED + "Close", List.of(ChatColor.GRAY + "Close this menu"), "close_menu", ""));

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        if (event == null || event.getPlayer() == null) {
            return;
        }
        pendingTextInput.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        UUID playerId = player.getUniqueId();
        PendingTextInput input = pendingTextInput.get(playerId);
        if (input == null) {
            return;
        }

        event.setCancelled(true);
        String message = event.getMessage();

        Bukkit.getScheduler().runTask(plugin, () -> handleTextInput(player, message));
    }

    private void beginTextInput(Player player, PendingTextInputType type) {
        if (player == null) {
            return;
        }
        if (islandManager == null) {
            player.sendMessage(ChatColor.RED + "Island system unavailable.");
            return;
        }
        Island island = islandManager.getIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return;
        }
        if (island.getOwner() == null || !island.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the island owner can edit island settings!");
            return;
        }

        PendingTextInputType resolvedType = type == null ? PendingTextInputType.ISLAND_NAME : type;
        pendingTextInput.put(player.getUniqueId(), new PendingTextInput(resolvedType, System.currentTimeMillis() + TEXT_INPUT_TIMEOUT_MS));

        player.closeInventory();
        if (resolvedType == PendingTextInputType.ISLAND_NAME) {
            player.sendMessage(ChatColor.GOLD + "Island Name");
            player.sendMessage(ChatColor.GRAY + "Type a new island name in chat (max 32 chars).");
        } else {
            player.sendMessage(ChatColor.GOLD + "Island Description");
            player.sendMessage(ChatColor.GRAY + "Type a new island description in chat (max 100 chars).");
            player.sendMessage(ChatColor.DARK_GRAY + "Type 'clear' to remove the description.");
        }
        player.sendMessage(ChatColor.DARK_GRAY + "Type 'cancel' to stop.");
    }

    private void handleTextInput(Player player, String message) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        PendingTextInput input = pendingTextInput.get(playerId);
        if (input == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now > input.expiresAtMs) {
            pendingTextInput.remove(playerId);
            player.sendMessage(ChatColor.RED + "Island setting entry timed out.");
            return;
        }

        String msg = message == null ? "" : message.trim();
        if (msg.equalsIgnoreCase("cancel")) {
            pendingTextInput.remove(playerId);
            player.sendMessage(ChatColor.YELLOW + "Cancelled.");
            return;
        }

        if (islandManager == null) {
            pendingTextInput.remove(playerId);
            player.sendMessage(ChatColor.RED + "Island system unavailable.");
            return;
        }

        Island island = islandManager.getIsland(player);
        if (island == null) {
            pendingTextInput.remove(playerId);
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return;
        }
        if (island.getOwner() == null || !island.getOwner().equals(playerId)) {
            pendingTextInput.remove(playerId);
            player.sendMessage(ChatColor.RED + "Only the island owner can edit island settings!");
            return;
        }

        if (input.type == PendingTextInputType.ISLAND_NAME) {
            if (msg.isBlank()) {
                player.sendMessage(ChatColor.RED + "Name cannot be empty. Type a new name, or 'cancel'.");
                return;
            }
            if (msg.length() > 32) {
                player.sendMessage(ChatColor.RED + "Name must be 32 characters or less. Type a new name, or 'cancel'.");
                return;
            }

            island.setName(msg);
            islandManager.saveIsland(island);
            pendingTextInput.remove(playerId);

            player.sendMessage(ChatColor.GREEN + "Island name set to: " + ChatColor.AQUA + msg);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.1f);
            openSettingsMenu(player);
            return;
        }

        // Description
        String newDesc;
        if (msg.equalsIgnoreCase("clear") || msg.equalsIgnoreCase("none")) {
            newDesc = "";
        } else {
            newDesc = msg;
        }
        if (newDesc.length() > 100) {
            player.sendMessage(ChatColor.RED + "Description must be 100 characters or less. Type a new description, or 'cancel'.");
            return;
        }

        island.setDescription(newDesc);
        islandManager.saveIsland(island);
        pendingTextInput.remove(playerId);

        player.sendMessage(ChatColor.GREEN + "Island description updated.");
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.1f);
        openSettingsMenu(player);
    }

    private static List<String> wrapPlainText(String text, int maxLen, int maxLines) {
        if (text == null) {
            return List.of();
        }
        int limit = Math.max(10, maxLen);
        int linesLimit = Math.max(1, maxLines);

        String cleaned = text.replace("\n", " ").replace("\r", " ").trim();
        if (cleaned.isEmpty()) {
            return List.of();
        }

        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : cleaned.split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }

            if (line.length() == 0) {
                line.append(word);
            } else if (line.length() + 1 + word.length() <= limit) {
                line.append(' ').append(word);
            } else {
                lines.add(line.toString());
                if (lines.size() >= linesLimit) {
                    return lines;
                }
                line.setLength(0);
                if (word.length() <= limit) {
                    line.append(word);
                } else {
                    // Hard-split a very long token.
                    line.append(word, 0, Math.min(limit, word.length()));
                }
            }
        }
        if (line.length() != 0 && lines.size() < linesLimit) {
            lines.add(line.toString());
        }
        return lines;
    }

    private ItemStack createMenuItem(Material type, String name, List<String> lore, String action, String value) {
        return createMenuItem(type, name, lore, action, value, false);
    }

    private ItemStack createMenuItem(Material type, String name, List<String> lore, String action, String value, boolean glow) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            if (glow) meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
            meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, value);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fillMainMenuBackground(Inventory inv) {
        ItemStack filler = createMenuItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of(), "noop", "");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
    }

    private void fillBackground(Inventory inv) {
        ItemStack filler = createMenuItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of(), "noop", "");
        ItemStack border = createMenuItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of(), "noop", "");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);
        int[] slots = {0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,49,50,51,52,53};
        for (int s : slots) if (s < inv.getSize()) inv.setItem(s, border);
    }

    private void playUiClick(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
    }

    /**
     * Returns true when the given inventory is one of this plugin's Skyblock menu inventories.
     * Used by island protection to allow the menu while visiting other islands.
     */
    public static boolean isSkyblockMenuInventory(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof MenuHolder;
    }

    private enum MenuType { MAIN, ISLAND, SETTINGS, VISIT_SETTINGS, PERMISSIONS }

    private enum PendingTextInputType { ISLAND_NAME, ISLAND_DESCRIPTION }

    private static final class PendingTextInput {
        final PendingTextInputType type;
        final long expiresAtMs;

        private PendingTextInput(PendingTextInputType type, long expiresAtMs) {
            this.type = type;
            this.expiresAtMs = expiresAtMs;
        }
    }

    private static final class MenuHolder implements InventoryHolder {
        private final MenuType type;
        private Inventory inventory;
        public MenuHolder(MenuType type) { this.type = type; }
        @Override public Inventory getInventory() { return inventory; }
    }
}
