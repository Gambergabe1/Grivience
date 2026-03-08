package io.papermc.Grivience.welcome;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.gui.SkyblockGui;
import io.papermc.Grivience.skyblock.economy.ProfileEconomyService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Manages the Hard Hat Harry NPC and welcome event system.
 * Optional ZNPCS integration for NPC functionality.
 */
public class WelcomeEventManager {
    private final GriviencePlugin plugin;
    private final XPBoostManager xpBoostManager;
    private final ProfileEconomyService profileEconomy;
    private Object hardHatHarry; // ZNPCS NPC (optional)
    private Location spawnLocation;
    private boolean enabled;
    private double starterMoney;
    private final File welcomeDataFile;
    private FileConfiguration welcomeDataConfig;
    private final Set<UUID> claimedRewards = new HashSet<>();
    private final boolean znpcsAvailable;

    public WelcomeEventManager(GriviencePlugin plugin, XPBoostManager xpBoostManager) {
        this.plugin = plugin;
        this.xpBoostManager = xpBoostManager;
        this.profileEconomy = new ProfileEconomyService(plugin);
        this.welcomeDataFile = new File(plugin.getDataFolder(), "welcome-claimed.yml");
        this.znpcsAvailable = Bukkit.getPluginManager().isPluginEnabled("ZNPCs");
        loadConfig();
        loadClaimedRewards();
        createNPC();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean("welcome-event.enabled", true);
        starterMoney = config.getDouble("welcome-event.starter-money", 500.0);

        String worldName = config.getString("welcome-event.spawn-world", "world");
        double x = config.getDouble("welcome-event.spawn-x", 0.5);
        double y = config.getDouble("welcome-event.spawn-y", 100.0);
        double z = config.getDouble("welcome-event.spawn-z", 0.5);
        float yaw = (float) config.getDouble("welcome-event.spawn-yaw", 0.0);
        float pitch = (float) config.getDouble("welcome-event.spawn-pitch", 0.0);

        World world = Bukkit.getWorld(worldName);
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
            plugin.getLogger().warning("welcome-event.spawn-world '" + worldName + "' not found; using " + world.getName() + " instead.");
        }

        spawnLocation = world == null ? null : new Location(world, x, y, z, yaw, pitch);
    }

    public void loadClaimedRewards() {
        if (!welcomeDataFile.exists()) {
            File parent = welcomeDataFile.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            try {
                welcomeDataFile.createNewFile();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create welcome data file: " + e.getMessage());
            }
        }

        welcomeDataConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(welcomeDataFile);

        List<String> claimedList = welcomeDataConfig.getStringList("claimed-rewards");
        for (String uuidStr : claimedList) {
            try {
                claimedRewards.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void saveClaimedRewards() {
        if (welcomeDataConfig == null) {
            return;
        }

        List<String> claimedList = new ArrayList<>();
        for (UUID uuid : claimedRewards) {
            claimedList.add(uuid.toString());
        }
        welcomeDataConfig.set("claimed-rewards", claimedList);

        try {
            welcomeDataConfig.save(welcomeDataFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save welcome data: " + e.getMessage());
        }
    }

    private void createNPC() {
        if (!enabled) {
            return;
        }
        if (spawnLocation == null || spawnLocation.getWorld() == null) {
            plugin.getLogger().warning("Welcome NPC spawn location is invalid; Hard Hat Harry will not be created.");
            return;
        }

        if (!znpcsAvailable) {
            plugin.getLogger().info("ZNPCS plugin not found. Hard Hat Harry NPC will not be created.");
            plugin.getLogger().info("Players can still use /welcome command to claim rewards.");
            return;
        }

        try {
            if (hardHatHarry != null) {
                Class<?> npcClass = hardHatHarry.getClass();
                java.lang.reflect.Method removeMethod = npcClass.getMethod("remove");
                removeMethod.invoke(hardHatHarry);
            }

            Class<?> npcManagerClass = Class.forName("me.pikamug.npcs.NPCManager");
            java.lang.reflect.Method getManager = npcManagerClass.getMethod("getManager");
            Object npcManager = getManager.invoke(null);

            Class<?> npcTypeClass = Class.forName("me.pikamug.npcs.NPCType");
            java.lang.reflect.Field playerField = npcTypeClass.getField("PLAYER");
            Object playerType = playerField.get(null);

            java.lang.reflect.Method createNPC = npcManagerClass.getMethod("createNPC", playerType.getClass(), Location.class, String.class, int.class);
            int npcId = new Random().nextInt(10000) + 1;
            hardHatHarry = createNPC.invoke(npcManager, playerType, spawnLocation, ChatColor.GOLD + "" + ChatColor.BOLD + "Hard Hat Harry", npcId);

            if (hardHatHarry != null) {
                Class<?> npcClass = hardHatHarry.getClass();

                java.lang.reflect.Method setClickable = npcClass.getMethod("setClickable", boolean.class);
                setClickable.invoke(hardHatHarry, true);

                java.lang.reflect.Method spawn = npcClass.getMethod("spawn");
                spawn.invoke(hardHatHarry);

                plugin.getLogger().info("Hard Hat Harry NPC created at spawn using ZNPCS!");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create Hard Hat Harry NPC with ZNPCS: " + e.getMessage());
            plugin.getLogger().warning("Players can still use /welcome command to claim rewards.");
            plugin.getLogger().warning("Make sure ZNPCS is installed and up to date.");
        }
    }

    /**
     * Check if a player has already claimed welcome rewards.
     */
    public boolean hasClaimedRewards(UUID playerId) {
        return claimedRewards.contains(playerId);
    }

    /**
     * Claim welcome rewards for a player.
     * One-time only per player account (UUID-based).
     */
    public void claimRewards(Player player) {
        if (!enabled) {
            player.sendMessage(ChatColor.RED + "Welcome event is currently disabled.");
            return;
        }

        UUID playerId = player.getUniqueId();
        if (hasClaimedRewards(playerId)) {
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Already Claimed!");
            player.sendMessage(ChatColor.GRAY + "You have already claimed your welcome rewards.");
            player.sendMessage(ChatColor.GRAY + "This reward is one-time only per account.");
            return;
        }

        if (profileEconomy.requireSelectedProfile(player) == null) {
            player.sendMessage(ChatColor.RED + "Select a Skyblock profile before claiming welcome rewards.");
            return;
        }

        // Mark as claimed first so retries cannot duplicate rewards.
        claimedRewards.add(playerId);
        saveClaimedRewards();

        player.closeInventory();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);

        player.sendMessage("");
        player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Welcome to Skyblock");
        player.sendMessage("");

        giveStarterMoney(player);
        applyStarterBoosts(player);
        giveStarterArmor(player);
        giveStarterTools(player);
        sendGuideMessage(player);

        player.sendMessage("");
        player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Rewards Claimed Successfully!");
        player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/welcome status" + ChatColor.GRAY + " to review your starter perks.");
        player.sendMessage(ChatColor.GRAY + "Rewards are " + ChatColor.RED + "one-time only" + ChatColor.GRAY + " per account.");
        player.sendMessage("");
    }

    private void giveStarterMoney(Player player) {
        if (starterMoney <= 0) {
            return;
        }

        if (profileEconomy.requireSelectedProfile(player) == null) {
            return;
        }
        if (!profileEconomy.deposit(player, starterMoney)) {
            player.sendMessage(ChatColor.RED + "Failed to give starter coins. Try again after selecting a profile.");
            return;
        }
        player.sendMessage(ChatColor.GREEN + "+" + ChatColor.GOLD + formatCoins(starterMoney) + " coins " + ChatColor.GRAY + "(Starter)");
    }

    private void applyStarterBoosts(Player player) {
        int duration = xpBoostManager.getDefaultDurationMinutes();
        xpBoostManager.applyMiningBoost(player, duration);
        xpBoostManager.applyFarmingBoost(player, duration);
    }

    private void giveStarterArmor(Player player) {
        int equipped = 0;

        if (equipOrAdd(player, ArmorSlot.HELMET, starterGear(Material.LEATHER_HELMET, "Starter Hard Hat", "Starter Armor", "+1 Armor"))) {
            equipped++;
        }
        if (equipOrAdd(player, ArmorSlot.CHESTPLATE, starterGear(Material.LEATHER_CHESTPLATE, "Starter Work Vest", "Starter Armor", "+3 Armor"))) {
            equipped++;
        }
        if (equipOrAdd(player, ArmorSlot.LEGGINGS, starterGear(Material.LEATHER_LEGGINGS, "Starter Work Pants", "Starter Armor", "+2 Armor"))) {
            equipped++;
        }
        if (equipOrAdd(player, ArmorSlot.BOOTS, starterGear(Material.LEATHER_BOOTS, "Starter Work Boots", "Starter Armor", "+1 Armor"))) {
            equipped++;
        }

        player.sendMessage(ChatColor.GREEN + "+ " + ChatColor.YELLOW + "Starter Armor Set " + ChatColor.GRAY + "(Non-Tradeable)");
        if (equipped < 4) {
            player.sendMessage(ChatColor.GRAY + "Existing armor was preserved. Remaining pieces were added to your inventory.");
        }
    }

    private void giveStarterTools(Player player) {
        addItemOrDrop(player, starterGear(Material.WOODEN_PICKAXE, "Starter Pickaxe", "Starter Tool", "For mining stone and ores"));
        addItemOrDrop(player, starterGear(Material.WOODEN_AXE, "Starter Axe", "Starter Tool", "For chopping wood"));
        addItemOrDrop(player, starterGear(Material.WOODEN_HOE, "Starter Hoe", "Starter Tool", "For farming crops"));
        addItemOrDrop(player, starterItem(Material.WHEAT_SEEDS, 16, "Starter Seeds", "Starter Item", "Plant and grow wheat", false));

        player.sendMessage(ChatColor.GREEN + "+ " + ChatColor.YELLOW + "Starter Tool Set " + ChatColor.GRAY + "(Non-Tradeable)");
    }

    private void sendGuideMessage(Player player) {
        player.sendMessage("");
        player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "Getting Started:");
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "1. " + ChatColor.YELLOW + "Mine Resources" + ChatColor.GRAY + " - Start by mining stone and coal");
        player.sendMessage(ChatColor.GRAY + "2. " + ChatColor.YELLOW + "Gather Wood" + ChatColor.GRAY + " - Chop trees for building materials");
        player.sendMessage(ChatColor.GRAY + "3. " + ChatColor.YELLOW + "Start Farming" + ChatColor.GRAY + " - Plant seeds and grow food");
        player.sendMessage(ChatColor.GRAY + "4. " + ChatColor.YELLOW + "Level Up Skills" + ChatColor.GRAY + " - Gain XP to unlock new content");
        player.sendMessage(ChatColor.GRAY + "5. " + ChatColor.YELLOW + "Join a Party" + ChatColor.GRAY + " - Team up with friends for dungeons");
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Useful Commands:");
        player.sendMessage(ChatColor.YELLOW + "/skyblock" + ChatColor.GRAY + " - Open Skyblock menu");
        player.sendMessage(ChatColor.YELLOW + "/craft" + ChatColor.GRAY + " - Open crafting menu");
        player.sendMessage(ChatColor.YELLOW + "/bazaar" + ChatColor.GRAY + " - Trade with other players");
        player.sendMessage(ChatColor.YELLOW + "/party" + ChatColor.GRAY + " - Manage your party");
        player.sendMessage(ChatColor.YELLOW + "/island" + ChatColor.GRAY + " - Manage your island");
        player.sendMessage(ChatColor.YELLOW + "/welcome status" + ChatColor.GRAY + " - Review your starter status");
        player.sendMessage("");
        player.sendMessage(ChatColor.GREEN + "Your XP Boosts:");
        player.sendMessage(ChatColor.GRAY + "- Mining: +" + formatPercent(xpBoostManager.getMiningBoostPercent())
                + "% for " + xpBoostManager.getDefaultDurationMinutes() + " minutes");
        player.sendMessage(ChatColor.GRAY + "- Farming: +" + formatPercent(xpBoostManager.getFarmingBoostPercent())
                + "% for " + xpBoostManager.getDefaultDurationMinutes() + " minutes");
    }

    /**
     * Open the welcome GUI for a player.
     */
    public void openWelcomeGUI(Player player) {
        if (!enabled) {
            player.sendMessage(ChatColor.RED + "Welcome event is currently disabled.");
            return;
        }

        WelcomeHolder holder = new WelcomeHolder();
        org.bukkit.inventory.Inventory inv = Bukkit.createInventory(holder, 27, SkyblockGui.title("Welcome to Skyblock"));
        holder.inventory = inv;

        ItemStack fill = SkyblockGui.filler(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack border = SkyblockGui.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, fill.clone());
        }
        for (int slot = 0; slot < 9; slot++) {
            inv.setItem(slot, border.clone());
        }
        for (int slot = 18; slot < 27; slot++) {
            inv.setItem(slot, border.clone());
        }
        inv.setItem(9, border.clone());
        inv.setItem(17, border.clone());

        boolean alreadyClaimed = hasClaimedRewards(player.getUniqueId());

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        ItemMeta bookMeta = book.getItemMeta();
        if (bookMeta != null) {
            bookMeta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Welcome Guide");
            List<String> bookLore = new ArrayList<>();
            bookLore.add("");
            bookLore.add(ChatColor.GRAY + "Click to view starter rewards");
            bookLore.add(ChatColor.GRAY + "and claim if available.");
            bookLore.add("");
            if (alreadyClaimed) {
                bookLore.add(ChatColor.RED + "Already Claimed");
            } else {
                bookLore.add(ChatColor.GREEN + "Click to Claim!");
            }
            bookMeta.setLore(bookLore);
            book.setItemMeta(bookMeta);
        }
        inv.setItem(13, book);

        ItemStack status = new ItemStack(alreadyClaimed ? Material.LIME_STAINED_GLASS_PANE : Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta statusMeta = status.getItemMeta();
        if (statusMeta != null) {
            statusMeta.setDisplayName(alreadyClaimed ? ChatColor.GREEN + "Rewards Claimed" : ChatColor.YELLOW + "Rewards Available");
            List<String> statusLore = new ArrayList<>();
            statusLore.add("");
            if (alreadyClaimed) {
                statusLore.add(ChatColor.GRAY + "You have already claimed");
                statusLore.add(ChatColor.GRAY + "your welcome rewards.");
                statusLore.add("");
                statusLore.add(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/welcome status" + ChatColor.GRAY + " anytime.");
            } else {
                statusLore.add(ChatColor.GRAY + "Claim your starter rewards:");
                statusLore.add(ChatColor.GRAY + "- " + ChatColor.GOLD + formatCoins(starterMoney) + ChatColor.GRAY + " coins");
                statusLore.add(ChatColor.GRAY + "- Mining/Farming XP boosts (" + xpBoostManager.getDefaultDurationMinutes() + " min)");
                statusLore.add(ChatColor.GRAY + "- Starter armor set");
                statusLore.add(ChatColor.GRAY + "- Starter tools + seeds");
            }
            statusMeta.setLore(statusLore);
            status.setItemMeta(statusMeta);
        }
        inv.setItem(4, status);

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.0F);
    }

    public void sendStatusMessage(Player player) {
        boolean claimed = hasClaimedRewards(player.getUniqueId());

        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Welcome Event Status");
        player.sendMessage(ChatColor.GRAY + "Claimed: " + (claimed ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));

        if (!claimed) {
            player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/welcome" + ChatColor.GRAY + " to open the claim menu.");
            player.sendMessage(ChatColor.GRAY + "Starter coins: " + ChatColor.GOLD + formatCoins(starterMoney));
        }

        if (xpBoostManager.hasMiningBoost(player)) {
            player.sendMessage(ChatColor.GRAY + "Mining boost remaining: " + ChatColor.YELLOW + xpBoostManager.getMiningBoostRemaining(player) + "m");
        }
        if (xpBoostManager.hasFarmingBoost(player)) {
            player.sendMessage(ChatColor.GRAY + "Farming boost remaining: " + ChatColor.YELLOW + xpBoostManager.getFarmingBoostRemaining(player) + "m");
        }
        player.sendMessage("");
    }

    public void reload() {
        loadConfig();
        if (hardHatHarry != null) {
            try {
                Class<?> npcClass = hardHatHarry.getClass();
                java.lang.reflect.Method removeMethod = npcClass.getMethod("remove");
                removeMethod.invoke(hardHatHarry);
            } catch (Exception ignored) {
            }
        }
        createNPC();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Location getSpawnLocation() {
        return spawnLocation;
    }

    public Object getHardHatHarry() {
        return hardHatHarry;
    }

    private ItemStack starterGear(Material material, String name, String category, String summary) {
        return starterItem(material, 1, name, category, summary, true);
    }

    private ItemStack starterItem(Material material, int amount, String name, String category, String summary, boolean unbreakable) {
        ItemStack item = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName(ChatColor.WHITE + name);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Welcome to Skyblock!");
        lore.add("");
        lore.add(ChatColor.YELLOW + category);
        lore.add(ChatColor.GRAY + "- " + summary);
        lore.add("");
        lore.add(ChatColor.RED + "" + ChatColor.ITALIC + "Non-Tradeable");
        meta.setLore(lore);
        if (unbreakable) {
            meta.setUnbreakable(true);
        }
        item.setItemMeta(meta);
        return item;
    }

    private boolean equipOrAdd(Player player, ArmorSlot slot, ItemStack item) {
        PlayerInventory inventory = player.getInventory();
        ItemStack current = switch (slot) {
            case HELMET -> inventory.getHelmet();
            case CHESTPLATE -> inventory.getChestplate();
            case LEGGINGS -> inventory.getLeggings();
            case BOOTS -> inventory.getBoots();
        };

        if (current == null || current.getType().isAir()) {
            switch (slot) {
                case HELMET -> inventory.setHelmet(item);
                case CHESTPLATE -> inventory.setChestplate(item);
                case LEGGINGS -> inventory.setLeggings(item);
                case BOOTS -> inventory.setBoots(item);
            }
            return true;
        }

        addItemOrDrop(player, item);
        return false;
    }

    private void addItemOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        for (ItemStack leftover : leftovers.values()) {
            if (leftover == null || leftover.getType().isAir()) {
                continue;
            }
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private String formatCoins(double amount) {
        return String.format(Locale.ROOT, "%,.0f", Math.max(0.0, amount));
    }

    private String formatPercent(double value) {
        if (Math.rint(value) == value) {
            return String.format(Locale.ROOT, "%.0f", value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private enum ArmorSlot {
        HELMET,
        CHESTPLATE,
        LEGGINGS,
        BOOTS
    }

    static final class WelcomeHolder implements org.bukkit.inventory.InventoryHolder {
        private org.bukkit.inventory.Inventory inventory;

        @Override
        public org.bukkit.inventory.Inventory getInventory() {
            return inventory;
        }
    }
}
