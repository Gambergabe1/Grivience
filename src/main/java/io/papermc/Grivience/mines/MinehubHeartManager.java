package io.papermc.Grivience.mines;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.collections.CollectionsManager;
import io.papermc.Grivience.stats.SkyblockLevelManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Heart of the Minehub - A custom HOTM-style progression system for the Minehub world.
 * Features unique perks, commissions, events, and treasure hunting mechanics.
 */
public final class MinehubHeartManager implements Listener {
    private static final String GUI_TITLE = ChatColor.GOLD + "" + ChatColor.BOLD + "Heart of the Minehub";
    private static final String CURRENCY_NAME = "Titanium Powder";
    private static final int PEAK_SLOT = 22;
    private static final int PEAK_MAX_LEVEL = 7;

    // XP Thresholds for each level
    private static final List<Long> LEVEL_THRESHOLDS = List.of(
        0L, 50L, 150L, 350L, 700L, 1200L, 1900L, 2800L, 4000L, 5600L, 7700L, 10400L, 14000L, 18500L, 24000L
    );

    // Token rewards per level
    private static final List<Integer> TOKEN_REWARDS = List.of(
        0, 2, 2, 3, 3, 3, 4, 4, 4, 5, 5, 5, 6, 6, 7
    );

    private final GriviencePlugin plugin;
    private final CollectionsManager collectionsManager;
    private final File dataFile;
    private final Map<UUID, HeartProfileData> profileData = new HashMap<>();
    private final Set<UUID> dirtyProfiles = new java.util.HashSet<>();
    private BukkitTask autoSaveTask;

    /**
     * Mining bonus record returned when player mines
     */
    public record MiningBonus(
        int bonusDrops,
        int bonusPowder,
        double fortuneMultiplier,
        double experienceMultiplier,
        boolean titanicTriggered,
        boolean pristineTriggered,
        int xpAwarded,
        int powderAwarded
    ) {
        public static MiningBonus none() {
            return new MiningBonus(0, 0, 1.0D, 1.0D, false, false, 0, 0);
        }
    }

    public enum MinehubPerk {
        // Tier 1 - Basic Fortune perks
        MINING_FORTUNE(
            "mining_fortune",
            ChatColor.GOLD + "Mining Fortune",
            Material.IRON_PICKAXE,
            10,
            1,
            10,
            1,
            20,
            12,
            "Grants +5 Mining Fortune per rank."
        ),
        SAPPHIRE_FORTUNE(
            "sapphire_fortune",
            ChatColor.AQUA + "Sapphire Fortune",
            Material.BLUE_STAINED_GLASS,
            12,
            1,
            8,
            1,
            25,
            15,
            "Increases Sapphire drop chance by 10% per rank."
        ),

        // Tier 2 - Speed and efficiency
        MINING_SPEED(
            "mining_speed",
            ChatColor.YELLOW + "Mining Speed",
            Material.GOLDEN_PICKAXE,
            14,
            2,
            10,
            1,
            30,
            18,
            "Increases mining speed by 10% per rank."
        ),
        EFFICIENT_MINER(
            "efficient_miner",
            ChatColor.GREEN + "Efficient Miner",
            Material.EMERALD,
            16,
            2,
            5,
            1,
            40,
            22,
            "5% chance per rank to mine instantly."
        ),

        // Tier 3 - Powder and XP
        POWDER_BUFF(
            "powder_buff",
            ChatColor.LIGHT_PURPLE + "Powder Buff",
            Material.GLOWSTONE_DUST,
            20,
            3,
            10,
            1,
            35,
            20,
            "Increases Mithril Powder gains by 15% per rank."
        ),
        EXPERIENCE_WISDOM(
            "experience_wisdom",
            ChatColor.AQUA + "Experience Wisdom",
            Material.EXPERIENCE_BOTTLE,
            24,
            3,
            8,
            1,
            45,
            25,
            "Grants +10% mining XP per rank."
        ),

        // Tier 4 - Special abilities
        TITANICEXPERIENCE(
            "titanic_experience",
            ChatColor.DARK_PURPLE + "Titanic Experience",
            Material.OBSIDIAN,
            29,
            4,
            5,
            2,
            60,
            35,
            "5% chance per rank to trigger Titanic Experience (4x drops, 3x powder, 2x XP)."
        ),
        MINING_MADNESS(
            "mining_madness",
            ChatColor.RED + "Mining Madness",
            Material.REDSTONE_BLOCK,
            31,
            4,
            3,
            2,
            80,
            50,
            "Each rank gives +15% chance to trigger Mining Madness (Haste III for 10s)."
        ),

        // Tier 5 - Advanced perks
        PRISTINE(
            "pristine",
            ChatColor.WHITE + "Pristine",
            Material.DIAMOND_BLOCK,
            33,
            5,
            5,
            2,
            100,
            60,
            "4% chance per rank for Pristine (5x drops from that ore)."
        ),
        DAILY_POWDER(
            "daily_powder",
            ChatColor.GOLD + "Daily Powder",
            Material.GOLD_BLOCK,
            38,
            5,
            1,
            2,
            150,
            0,
            "Grants +500 Mithril Powder daily per rank (claim via GUI)."
        ),

        // Tier 6 - Endgame perks
        LUCKY_MINEHUB(
            "lucky_minehub",
            ChatColor.LIGHT_PURPLE + "Lucky Minehub",
            Material.ENCHANTED_GOLDEN_APPLE,
            40,
            6,
            3,
            3,
            120,
            70,
            "2% chance per rank to find treasure while mining."
        ),
        FORGE_MASTER(
            "forge_master",
            ChatColor.DARK_RED + "Forge Master",
            Material.ANVIL,
            42,
            6,
            5,
            2,
            140,
            80,
            "Reduces Drill Forge times by 8% per rank."
        ),

        // Tier 7 - Ultimate ability
        MINEHUB_HEART_CORE(
            "minehub_heart_core",
            ChatColor.GOLD + "" + ChatColor.BOLD + "Minehub Heart Core",
            Material.NETHER_STAR,
            44,
            7,
            1,
            5,
            300,
            0,
            "Unlocks the ultimate mining ability: Mining Frenzy (30s of maximum mining power, 10min cooldown)."
        );

        private final String id;
        private final String displayName;
        private final Material icon;
        private final int slot;
        private final int requiredHeartLevel;
        private final int maxLevel;
        private final int tokenUnlockCost;
        private final int basePowderCost;
        private final int powderCostStep;
        private final String description;

        MinehubPerk(
            String id,
            String displayName,
            Material icon,
            int slot,
            int requiredHeartLevel,
            int maxLevel,
            int tokenUnlockCost,
            int basePowderCost,
            int powderCostStep,
            String description
        ) {
            this.id = id;
            this.displayName = displayName;
            this.icon = icon;
            this.slot = slot;
            this.requiredHeartLevel = requiredHeartLevel;
            this.maxLevel = maxLevel;
            this.tokenUnlockCost = tokenUnlockCost;
            this.basePowderCost = basePowderCost;
            this.powderCostStep = powderCostStep;
            this.description = description;
        }

        public String id() { return id; }
        public String displayName() { return displayName; }
        public Material icon() { return icon; }
        public int slot() { return slot; }
        public int requiredHeartLevel() { return requiredHeartLevel; }
        public int maxLevel() { return maxLevel; }
        public int tokenUnlockCost() { return tokenUnlockCost; }

        public int powderCostForNextRank(int currentRank) {
            if (currentRank <= 0 || currentRank >= maxLevel) {
                return 0;
            }
            return basePowderCost + ((currentRank - 1) * powderCostStep);
        }

        public String description() { return description; }

        public static MinehubPerk fromSlot(int slot) {
            for (MinehubPerk perk : values()) {
                if (perk.slot == slot) {
                    return perk;
                }
            }
            return null;
        }
    }

    private enum PurchaseResult {
        UNLOCKED,
        UPGRADED,
        PEAK_UPGRADED,
        MAXED,
        NEEDS_LEVEL,
        NEEDS_TOKENS,
        NEEDS_POWDER,
        CLAIMED_DAILY
    }

    private static final class HeartProfileData {
        private int level;
        private long xp;
        private long powder;
        private int availableTokens;
        private int peakLevel;
        private long lastDailyClaimTime;
        private long miningFrenzyUnlockedAt;
        private long miningFrenzyCooldownUntil;
        private final EnumMap<MinehubPerk, Integer> perkRanks = new EnumMap<>(MinehubPerk.class);

        private HeartProfileData(int level, int availableTokens) {
            this.level = level;
            this.availableTokens = availableTokens;
        }

        private static HeartProfileData createDefault(int level, int tokens) {
            return new HeartProfileData(level, tokens);
        }

        private static HeartProfileData load(ConfigurationSection section, int defaultLevel, int defaultTokens) {
            HeartProfileData data = new HeartProfileData(
                Math.max(1, section == null ? defaultLevel : section.getInt("level", defaultLevel)),
                Math.max(0, section == null ? defaultTokens : section.getInt("available-tokens", defaultTokens))
            );
            if (section == null) {
                return data;
            }
            data.xp = Math.max(0L, section.getLong("xp", 0L));
            data.powder = Math.max(0L, section.getLong("powder", 0L));
            data.peakLevel = Math.max(0, Math.min(PEAK_MAX_LEVEL, section.getInt("peak-level", 0)));
            data.lastDailyClaimTime = section.getLong("last-daily-claim", 0L);
            data.miningFrenzyUnlockedAt = section.getLong("mining-frenzy-unlocked", 0L);
            data.miningFrenzyCooldownUntil = section.getLong("mining-frenzy-cooldown", 0L);

            ConfigurationSection perks = section.getConfigurationSection("perks");
            if (perks != null) {
                for (MinehubPerk perk : MinehubPerk.values()) {
                    int rank = Math.max(0, Math.min(perk.maxLevel(), perks.getInt(perk.id(), 0)));
                    if (rank > 0) {
                        data.perkRanks.put(perk, rank);
                    }
                }
            }
            return data;
        }

        private int perkRank(MinehubPerk perk) {
            return perkRanks.getOrDefault(perk, 0);
        }

        private void setPerkRank(MinehubPerk perk, int rank) {
            if (rank <= 0) {
                perkRanks.remove(perk);
                return;
            }
            perkRanks.put(perk, rank);
        }

        private void save(ConfigurationSection section) {
            section.set("level", level);
            section.set("xp", xp);
            section.set("powder", powder);
            section.set("available-tokens", availableTokens);
            section.set("peak-level", peakLevel);
            section.set("last-daily-claim", lastDailyClaimTime);
            section.set("mining-frenzy-unlocked", miningFrenzyUnlockedAt);
            section.set("mining-frenzy-cooldown", miningFrenzyCooldownUntil);

            ConfigurationSection perks = section.createSection("perks");
            for (Map.Entry<MinehubPerk, Integer> entry : perkRanks.entrySet()) {
                if (entry.getValue() != null && entry.getValue() > 0) {
                    perks.set(entry.getKey().id(), entry.getValue());
                }
            }
        }
    }

    public MinehubHeartManager(GriviencePlugin plugin, CollectionsManager collectionsManager) {
        this.plugin = plugin;
        this.collectionsManager = collectionsManager;
        this.dataFile = new File(plugin.getDataFolder(), "minehub-heart-data.yml");
        loadAll();
        startAutoSave();
    }

    public void shutdown() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
        saveAll();
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("minehub.heart.enabled", true);
    }

    // ===== GUI Methods =====

    public void openGui(Player player) {
        if (!isEnabled() || player == null) {
            return;
        }

        HeartProfileData data = getData(player);
        Inventory inventory = Bukkit.createInventory(player, 54, GUI_TITLE);
        fillBackground(inventory);

        inventory.setItem(0, createHelpItem());
        inventory.setItem(4, createSummaryItem(data));
        inventory.setItem(8, createCurrencyItem(data));
        inventory.setItem(13, createProgressItem(data));
        inventory.setItem(PEAK_SLOT, createPeakItem(data));
        inventory.setItem(45, createDailyRewardItem(data));
        inventory.setItem(49, namedItem(Material.BARRIER, ChatColor.RED + "Close", List.of()));

        for (MinehubPerk perk : MinehubPerk.values()) {
            inventory.setItem(perk.slot(), createPerkItem(data, perk));
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!GUI_TITLE.equals(event.getView().getTitle())) {
            return;
        }

        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        int slot = event.getRawSlot();

        if (slot == 49) {
            player.closeInventory();
            return;
        }

        if (slot == 45) {
            handleDailyRewardClaim(player);
            openGui(player);
            return;
        }

        if (slot == PEAK_SLOT) {
            handlePeakUpgrade(player);
            openGui(player);
            return;
        }

        MinehubPerk perk = MinehubPerk.fromSlot(slot);
        if (perk == null) {
            return;
        }

        PurchaseResult result = purchase(player, perk);
        switch (result) {
            case UNLOCKED -> player.sendMessage(ChatColor.GREEN + "Unlocked " + perk.displayName() + ChatColor.GREEN + "!");
            case UPGRADED -> player.sendMessage(ChatColor.GREEN + "Upgraded " + perk.displayName() + ChatColor.GREEN + "!");
            case MAXED -> player.sendMessage(ChatColor.YELLOW + "That perk is already maxed.");
            case NEEDS_LEVEL -> player.sendMessage(ChatColor.RED + "You need Heart of the Minehub level " + perk.requiredHeartLevel() + ".");
            case NEEDS_TOKENS -> player.sendMessage(ChatColor.RED + "You need " + perk.tokenUnlockCost() + " token(s).");
            case NEEDS_POWDER -> player.sendMessage(ChatColor.RED + "You need more " + CURRENCY_NAME + ".");
            default -> { return; }
        }

        openGui(player);
    }

    // ===== Mining Integration =====

    public MiningBonus recordMining(Player player, Material oreMined) {
        if (!isEnabled() || player == null) {
            return MiningBonus.none();
        }

        HeartProfileData data = getData(player);

        // Calculate base rewards
        int baseXp = calculateBaseXp(oreMined);
        int basePowder = calculateBasePowder(oreMined);

        // Apply perk bonuses
        int bonusDrops = 0;
        double fortuneMultiplier = 1.0 + (data.perkRank(MinehubPerk.MINING_FORTUNE) * 0.05);

        // Sapphire-specific fortune
        if (isSapphireMaterial(oreMined)) {
            fortuneMultiplier += (data.perkRank(MinehubPerk.SAPPHIRE_FORTUNE) * 0.10);
        }

        // Powder buff
        double powderMultiplier = 1.0 + (data.perkRank(MinehubPerk.POWDER_BUFF) * 0.15) + peakPowderMultiplier(data.peakLevel);

        // Experience wisdom
        double xpMultiplier = 1.0 + (data.perkRank(MinehubPerk.EXPERIENCE_WISDOM) * 0.10) + peakXpMultiplier(data.peakLevel);

        // Global Booster Integration
        if (plugin.getGlobalEventManager() != null) {
            double globalHotmPower = plugin.getGlobalEventManager().getMultiplier(io.papermc.Grivience.event.GlobalEventManager.BoosterType.MINEHUB_HEART);
            powderMultiplier *= globalHotmPower;
            xpMultiplier *= globalHotmPower;
        }

        // Check for special triggers
        boolean titanicTriggered = false;
        boolean pristineTriggered = false;

        // Titanic Experience check
        int titanicRank = data.perkRank(MinehubPerk.TITANICEXPERIENCE);
        if (titanicRank > 0 && ThreadLocalRandom.current().nextDouble() < titanicRank * 0.05) {
            titanicTriggered = true;
            bonusDrops += 3; // 4x drops (3 bonus)
            powderMultiplier *= 3.0;
            xpMultiplier *= 2.0;
            player.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "⚡ TITANIC EXPERIENCE! " + ChatColor.GRAY + "Massive rewards!");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6F, 1.5F);
        }

        // Pristine check
        int pristineRank = data.perkRank(MinehubPerk.PRISTINE);
        if (pristineRank > 0 && !titanicTriggered && ThreadLocalRandom.current().nextDouble() < pristineRank * 0.04) {
            pristineTriggered = true;
            bonusDrops += 4; // 5x drops
            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "✦ PRISTINE! " + ChatColor.GRAY + "Perfect ore extraction!");
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0F, 2.0F);
        }

        // Mining Madness check
        int madnessRank = data.perkRank(MinehubPerk.MINING_MADNESS);
        if (madnessRank > 0 && ThreadLocalRandom.current().nextDouble() < madnessRank * 0.15) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.HASTE, 200, 2, false, true, true
            ));
            player.sendMessage(ChatColor.RED + "Mining Madness activated!");
        }

        // Lucky Minehub treasure check
        int luckyRank = data.perkRank(MinehubPerk.LUCKY_MINEHUB);
        if (luckyRank > 0 && ThreadLocalRandom.current().nextDouble() < luckyRank * 0.02) {
            grantTreasure(player, oreMined);
        }

        // Calculate final amounts
        int finalXp = (int) Math.round(baseXp * xpMultiplier);
        int finalPowder = (int) Math.round(basePowder * powderMultiplier);

        // Apply progress
        applyProgress(player, data, finalXp, finalPowder);

        return new MiningBonus(bonusDrops, finalPowder, fortuneMultiplier, xpMultiplier,
                               titanicTriggered, pristineTriggered, finalXp, finalPowder);
    }

    public int getMiningSpeedBonus(Player player) {
        if (!isEnabled() || player == null) {
            return 0;
        }
        HeartProfileData data = getData(player);
        return data.perkRank(MinehubPerk.MINING_SPEED) * 10;
    }

    public double getInstantMineChance(Player player) {
        if (!isEnabled() || player == null) {
            return 0.0;
        }
        HeartProfileData data = getData(player);
        return data.perkRank(MinehubPerk.EFFICIENT_MINER) * 0.05;
    }

    public int getForgeTimeReduction(Player player) {
        if (!isEnabled() || player == null) {
            return 0;
        }
        HeartProfileData data = getData(player);
        return data.perkRank(MinehubPerk.FORGE_MASTER) * 8;
    }

    public boolean canActivateMiningFrenzy(Player player) {
        if (!isEnabled() || player == null) {
            return false;
        }
        HeartProfileData data = getData(player);
        return data.perkRank(MinehubPerk.MINEHUB_HEART_CORE) > 0
            && System.currentTimeMillis() >= data.miningFrenzyCooldownUntil;
    }

    public void activateMiningFrenzy(Player player) {
        if (!canActivateMiningFrenzy(player)) {
            player.sendMessage(ChatColor.RED + "Mining Frenzy is on cooldown!");
            return;
        }

        HeartProfileData data = getData(player);
        data.miningFrenzyCooldownUntil = System.currentTimeMillis() + (10L * 60L * 1000L); // 10min cooldown
        markDirty(player);

        // Apply massive buffs for 30 seconds
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.HASTE, 600, 4, false, true, true
        ));
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.LUCK, 600, 2, false, true, true
        ));

        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "═══════════════════════");
        player.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "    MINING FRENZY ACTIVATED!");
        player.sendMessage(ChatColor.GRAY + "Duration: " + ChatColor.YELLOW + "30 seconds");
        player.sendMessage(ChatColor.GRAY + "Haste V, Luck III, and massive bonuses!");
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "═══════════════════════");
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
    }

    // ===== Private Helper Methods =====

    private void handleDailyRewardClaim(Player player) {
        HeartProfileData data = getData(player);
        int dailyRank = data.perkRank(MinehubPerk.DAILY_POWDER);

        if (dailyRank <= 0) {
            player.sendMessage(ChatColor.RED + "You haven't unlocked Daily Powder yet!");
            return;
        }

        long now = System.currentTimeMillis();
        long dayInMillis = 24L * 60L * 60L * 1000L;

        if (data.lastDailyClaimTime > 0 && (now - data.lastDailyClaimTime) < dayInMillis) {
            long remaining = dayInMillis - (now - data.lastDailyClaimTime);
            long hoursLeft = remaining / (60L * 60L * 1000L);
            player.sendMessage(ChatColor.RED + "Daily reward available in " + hoursLeft + " hours.");
            return;
        }

        int reward = dailyRank * 500;
        data.powder += reward;
        data.lastDailyClaimTime = now;
        markDirty(player);

        player.sendMessage(ChatColor.GREEN + "Daily Reward Claimed! +" + reward + " " + CURRENCY_NAME);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7F, 1.5F);
    }

    private void handlePeakUpgrade(Player player) {
        HeartProfileData data = getData(player);
        int nextLevel = data.peakLevel + 1;

        if (data.peakLevel >= PEAK_MAX_LEVEL) {
            player.sendMessage(ChatColor.YELLOW + "Peak of the Minehub is already maxed!");
            return;
        }

        if (data.level < peakRequiredHeartLevel(nextLevel)) {
            player.sendMessage(ChatColor.RED + "You need Heart level " + peakRequiredHeartLevel(nextLevel) + ".");
            return;
        }

        if (data.availableTokens < 1) {
            player.sendMessage(ChatColor.RED + "You need 1 Token of the Minehub.");
            return;
        }

        data.availableTokens -= 1;
        data.peakLevel = nextLevel;
        markDirty(player);

        player.sendMessage(ChatColor.GREEN + "Upgraded Peak of the Minehub!");
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0F, 1.2F);
    }

    private PurchaseResult purchase(Player player, MinehubPerk perk) {
        HeartProfileData data = getData(player);
        int rank = data.perkRank(perk);

        if (rank <= 0) {
            if (data.level < perk.requiredHeartLevel()) {
                return PurchaseResult.NEEDS_LEVEL;
            }
            if (data.availableTokens < perk.tokenUnlockCost()) {
                return PurchaseResult.NEEDS_TOKENS;
            }
            data.availableTokens -= perk.tokenUnlockCost();
            data.setPerkRank(perk, 1);
            markDirty(player);
            return PurchaseResult.UNLOCKED;
        }

        if (rank >= perk.maxLevel()) {
            return PurchaseResult.MAXED;
        }

        int cost = perk.powderCostForNextRank(rank);
        if (data.powder < cost) {
            return PurchaseResult.NEEDS_POWDER;
        }

        data.powder -= cost;
        data.setPerkRank(perk, rank + 1);
        markDirty(player);
        return PurchaseResult.UPGRADED;
    }

    private void applyProgress(Player player, HeartProfileData data, int xpAwarded, int powderAwarded) {
        data.xp += Math.max(0, xpAwarded);
        data.powder += Math.max(0, powderAwarded);
        levelIfNeeded(player, data);
        markDirty(player);

        if (xpAwarded > 0 || powderAwarded > 0) {
            StringBuilder message = new StringBuilder();
            if (xpAwarded > 0) {
                message.append(ChatColor.AQUA).append("+").append(xpAwarded).append(" Heart XP");
            }
            if (powderAwarded > 0) {
                if (message.length() > 0) {
                    message.append(ChatColor.DARK_GRAY).append("  •  ");
                }
                message.append(ChatColor.LIGHT_PURPLE).append("+").append(powderAwarded).append(" ").append(CURRENCY_NAME);
            }
            player.sendActionBar(message.toString());
        }
    }

    private void levelIfNeeded(Player player, HeartProfileData data) {
        int maxLevel = LEVEL_THRESHOLDS.size();
        while (data.level < maxLevel && data.xp >= thresholdForLevel(data.level + 1)) {
            data.level++;
            int awardedTokens = tokensForLevel(data.level);
            data.availableTokens += awardedTokens;

            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "HEART OF THE MINEHUB LEVEL UP! "
                + ChatColor.YELLOW + "Level " + data.level);
            player.sendMessage(ChatColor.GRAY + "You gained " + ChatColor.GREEN + awardedTokens
                + ChatColor.GRAY + " Token(s) of the Minehub.");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.2F);

            SkyblockLevelManager levelManager = plugin.getSkyblockLevelManager();
            if (levelManager != null) {
                levelManager.awardObjectiveXp(
                    player,
                    "skill.minehub.heart.level_" + data.level,
                    "skill",
                    7L,
                    "Heart of the Minehub Level " + data.level,
                    true
                );
            }
        }
    }

    private void grantTreasure(Player player, Material oreMined) {
        // Treasure table based on ore type
        List<String> possibleTreasures = new ArrayList<>();

        if (isCommonOre(oreMined)) {
            possibleTreasures.add("ORE_FRAGMENT:5");
            possibleTreasures.add("VOLTA:1");
        } else if (isRareOre(oreMined)) {
            possibleTreasures.add("ENCHANTED_SAPPHIRE:1");
            possibleTreasures.add("OIL_BARREL:1");
            possibleTreasures.add("MITHRIL_ENGINE:1");
        } else if (isEpicOre(oreMined)) {
            possibleTreasures.add("TITANIUM_ENGINE:1");
            possibleTreasures.add("GEMSTONE_ENGINE:1");
            possibleTreasures.add("MINING_XP_SCROLL:2");
        }

        if (possibleTreasures.isEmpty()) {
            possibleTreasures.add("ORE_FRAGMENT:3");
        }

        String treasure = possibleTreasures.get(ThreadLocalRandom.current().nextInt(possibleTreasures.size()));
        String[] parts = treasure.split(":");
        String itemId = parts[0];
        int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;

        if (plugin.getCustomItemService() != null) {
            ItemStack item = plugin.getCustomItemService().createItemByKey(itemId);
            if (item != null) {
                item.setAmount(amount);
                io.papermc.Grivience.util.DropDeliveryUtil.giveToInventoryOrDrop(player, item, player.getLocation());
                player.sendMessage(ChatColor.LIGHT_PURPLE + "✦ TREASURE! " + ChatColor.GRAY + "You found " + item.getItemMeta().getDisplayName() + ChatColor.GRAY + "!");
                player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8F, 1.5F);
            }
        }
    }

    private boolean isSapphireMaterial(Material material) {
        return material == Material.BLUE_STAINED_GLASS
            || material == Material.BLUE_STAINED_GLASS_PANE
            || material == Material.BLUE_CONCRETE_POWDER;
    }

    private boolean isCommonOre(Material material) {
        return material == Material.COAL_ORE || material == Material.IRON_ORE
            || material == Material.COPPER_ORE || material == Material.DEEPSLATE_COAL_ORE
            || material == Material.DEEPSLATE_IRON_ORE || material == Material.DEEPSLATE_COPPER_ORE;
    }

    private boolean isRareOre(Material material) {
        return material == Material.GOLD_ORE || material == Material.LAPIS_ORE
            || material == Material.REDSTONE_ORE || material == Material.DEEPSLATE_GOLD_ORE
            || material == Material.DEEPSLATE_LAPIS_ORE || material == Material.DEEPSLATE_REDSTONE_ORE
            || isSapphireMaterial(material);
    }

    private boolean isEpicOre(Material material) {
        return material == Material.DIAMOND_ORE || material == Material.EMERALD_ORE
            || material == Material.DEEPSLATE_DIAMOND_ORE || material == Material.DEEPSLATE_EMERALD_ORE
            || material == Material.OBSIDIAN;
    }

    private int calculateBaseXp(Material material) {
        if (isEpicOre(material)) return 5;
        if (isRareOre(material)) return 3;
        return 1;
    }

    private int calculateBasePowder(Material material) {
        if (isEpicOre(material)) return ThreadLocalRandom.current().nextInt(3, 6);
        if (isRareOre(material)) return ThreadLocalRandom.current().nextInt(2, 4);
        return ThreadLocalRandom.current().nextInt(1, 3);
    }

    private long thresholdForLevel(int level) {
        int index = Math.max(0, Math.min(LEVEL_THRESHOLDS.size() - 1, level - 1));
        return LEVEL_THRESHOLDS.get(index);
    }

    private int tokensForLevel(int level) {
        if (level <= 0 || level >= TOKEN_REWARDS.size()) {
            return 2;
        }
        return TOKEN_REWARDS.get(level);
    }

    private int peakRequiredHeartLevel(int nextPeakLevel) {
        return switch (Math.max(1, Math.min(PEAK_MAX_LEVEL, nextPeakLevel))) {
            case 1 -> 2;
            case 2 -> 4;
            case 3 -> 6;
            case 4 -> 9;
            case 5 -> 11;
            case 6 -> 13;
            default -> 15;
        };
    }

    private double peakXpMultiplier(int peakLevel) {
        return 1.0 + (Math.max(0, peakLevel) * 0.05);
    }

    private double peakPowderMultiplier(int peakLevel) {
        return Math.max(0, peakLevel) * 0.04;
    }

    private double peakFortuneBonus(int peakLevel) {
        return Math.max(0, peakLevel) * 0.02;
    }

    private HeartProfileData getData(Player player) {
        UUID profileId = resolveProfileId(player);
        return profileData.computeIfAbsent(profileId, ignored ->
            HeartProfileData.createDefault(1, 2));
    }

    private UUID resolveProfileId(Player player) {
        if (player == null) {
            return new UUID(0L, 0L);
        }
        if (collectionsManager != null) {
            UUID profileId = collectionsManager.getProfileId(player);
            if (profileId != null) {
                return profileId;
            }
        }
        return player.getUniqueId();
    }

    private void markDirty(Player player) {
        UUID profileId = resolveProfileId(player);
        if (profileId != null) {
            dirtyProfiles.add(profileId);
        }
    }

    // ===== GUI Helper Methods =====

    private void fillBackground(Inventory inventory) {
        ItemStack filler = namedItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        ItemStack frame = namedItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int slot : new int[]{0, 1, 2, 3, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53}) {
            inventory.setItem(slot, frame);
        }

        ItemStack connector = namedItem(Material.YELLOW_STAINED_GLASS_PANE, ChatColor.GOLD + "Mining Path", List.of());
        for (int slot : new int[]{11, 15, 19, 21, 23, 25, 28, 30, 32, 34, 37, 41}) {
            inventory.setItem(slot, connector);
        }
    }

    private ItemStack createHelpItem() {
        return namedItem(
            Material.BOOK,
            ChatColor.AQUA + "How It Works",
            List.of(
                ChatColor.GRAY + "Mine in the Minehub to gain",
                ChatColor.AQUA + "Heart XP " + ChatColor.GRAY + "and " + ChatColor.LIGHT_PURPLE + CURRENCY_NAME + ChatColor.GRAY + ".",
                "",
                ChatColor.GRAY + "Level ups grant " + ChatColor.GREEN + "Tokens" + ChatColor.GRAY + ".",
                ChatColor.GRAY + "Use tokens to unlock perks,",
                ChatColor.GRAY + "then use powder to upgrade them.",
                "",
                ChatColor.GRAY + "Peak of the Minehub is your",
                ChatColor.GRAY + "permanent core upgrade."
            )
        );
    }

    private ItemStack createSummaryItem(HeartProfileData data) {
        long currentFloor = thresholdForLevel(data.level);
        long nextThreshold = thresholdForLevel(Math.min(LEVEL_THRESHOLDS.size(), data.level + 1));
        long progress = Math.max(0L, data.xp - currentFloor);
        long needed = Math.max(1L, nextThreshold - currentFloor);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Level: " + ChatColor.GOLD + data.level + ChatColor.GRAY + "/" + ChatColor.GOLD + LEVEL_THRESHOLDS.size());
        lore.add(ChatColor.GRAY + "XP: " + ChatColor.AQUA + String.format("%,d", data.xp));
        lore.add(ChatColor.GRAY + "Progress: " + ChatColor.AQUA + String.format("%,d", progress) + ChatColor.GRAY + "/" + ChatColor.AQUA + String.format("%,d", needed));
        lore.add(ChatColor.GRAY + progressBar(progress, needed));
        if (data.level < LEVEL_THRESHOLDS.size()) {
            lore.add(ChatColor.GRAY + "Next: " + ChatColor.GREEN + "+" + tokensForLevel(data.level + 1) + " Token(s)");
        }
        lore.add("");
        lore.add(ChatColor.GRAY + "Mine in the Minehub to progress!");
        return namedItem(Material.NETHER_STAR, ChatColor.GOLD + "" + ChatColor.BOLD + "Heart of the Minehub", lore);
    }

    private ItemStack createCurrencyItem(HeartProfileData data) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + CURRENCY_NAME + ": " + ChatColor.LIGHT_PURPLE + String.format("%,d", data.powder));
        lore.add(ChatColor.GRAY + "Tokens: " + ChatColor.GREEN + data.availableTokens);
        lore.add("");
        lore.add(ChatColor.GRAY + "Use tokens to unlock perks.");
        lore.add(ChatColor.GRAY + "Use powder to upgrade perks.");
        return namedItem(Material.GLOWSTONE_DUST, ChatColor.LIGHT_PURPLE + CURRENCY_NAME, lore);
    }

    private ItemStack createProgressItem(HeartProfileData data) {
        long currentFloor = thresholdForLevel(data.level);
        long nextThreshold = thresholdForLevel(Math.min(LEVEL_THRESHOLDS.size(), data.level + 1));
        long progress = Math.max(0L, data.xp - currentFloor);
        long needed = Math.max(1L, nextThreshold - currentFloor);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current Level: " + ChatColor.GOLD + data.level);
        if (data.level >= LEVEL_THRESHOLDS.size()) {
            lore.add(ChatColor.GOLD + "Maximum level reached!");
        } else {
            lore.add(ChatColor.GRAY + "Progress to Level " + (data.level + 1) + ":");
            lore.add(ChatColor.AQUA + String.format("%,d", progress) + ChatColor.GRAY + "/" + ChatColor.AQUA + String.format("%,d", needed));
            lore.add(ChatColor.GRAY + progressBar(progress, needed));
        }
        return namedItem(Material.EXPERIENCE_BOTTLE, ChatColor.AQUA + "Level Progress", lore);
    }

    private ItemStack createPeakItem(HeartProfileData data) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Permanent core upgrades");
        lore.add(ChatColor.GRAY + "for Minehub mining.");
        lore.add("");
        lore.add(ChatColor.GRAY + "Rank: " + ChatColor.GOLD + data.peakLevel + ChatColor.GRAY + "/" + ChatColor.GOLD + PEAK_MAX_LEVEL);
        lore.add(ChatColor.GRAY + "Heart XP: " + ChatColor.AQUA + "+" + (int)(peakXpMultiplier(data.peakLevel) * 100 - 100) + "%");
        lore.add(ChatColor.GRAY + "Powder Gain: " + ChatColor.LIGHT_PURPLE + "+" + (int)(peakPowderMultiplier(data.peakLevel) * 100) + "%");
        lore.add(ChatColor.GRAY + "Fortune: " + ChatColor.GOLD + "+" + (int)(peakFortuneBonus(data.peakLevel) * 100) + "%");

        if (data.peakLevel >= PEAK_MAX_LEVEL) {
            lore.add("");
            lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "✦ MASTER OF THE MINEHUB ✦");
            lore.add(ChatColor.GRAY + "Peak fully awakened!");
        } else {
            lore.add("");
            lore.add(ChatColor.GRAY + "Next: Level " + peakRequiredHeartLevel(data.peakLevel + 1));
            lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GREEN + "1 Token");
            lore.add(ChatColor.YELLOW + "Click to upgrade!");
        }

        return namedItem(Material.BEACON, ChatColor.GOLD + "" + ChatColor.BOLD + "Peak of the Minehub", lore);
    }

    private ItemStack createPerkItem(HeartProfileData data, MinehubPerk perk) {
        int rank = data.perkRank(perk);
        boolean unlocked = rank > 0;
        boolean maxed = rank >= perk.maxLevel();

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + perk.description());
        lore.add("");
        lore.add(ChatColor.GRAY + "Required Level: " + ChatColor.GOLD + perk.requiredHeartLevel());
        lore.add(ChatColor.GRAY + "Rank: " + (unlocked ? ChatColor.GREEN + String.valueOf(rank) : ChatColor.RED + "Locked")
            + ChatColor.GRAY + "/" + ChatColor.GREEN + perk.maxLevel());

        if (!unlocked) {
            lore.add(ChatColor.GRAY + "Unlock: " + ChatColor.GREEN + perk.tokenUnlockCost() + " Token(s)");
            lore.add(ChatColor.YELLOW + "Click to unlock!");
        } else if (maxed) {
            lore.add(ChatColor.GOLD + "Max rank!");
        } else {
            lore.add(ChatColor.GRAY + "Upgrade: " + ChatColor.LIGHT_PURPLE + perk.powderCostForNextRank(rank) + " " + CURRENCY_NAME);
            lore.add(ChatColor.YELLOW + "Click to upgrade!");
        }

        ItemStack item = namedItem(perk.icon(), perk.displayName(), lore);
        if (unlocked) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    private ItemStack createDailyRewardItem(HeartProfileData data) {
        int dailyRank = data.perkRank(MinehubPerk.DAILY_POWDER);
        List<String> lore = new ArrayList<>();

        if (dailyRank <= 0) {
            lore.add(ChatColor.RED + "Unlock Daily Powder perk first!");
        } else {
            long now = System.currentTimeMillis();
            long dayInMillis = 24L * 60L * 60L * 1000L;
            boolean canClaim = data.lastDailyClaimTime == 0 || (now - data.lastDailyClaimTime) >= dayInMillis;

            lore.add(ChatColor.GRAY + "Daily Powder Rank: " + ChatColor.GREEN + dailyRank);
            lore.add(ChatColor.GRAY + "Reward: " + ChatColor.LIGHT_PURPLE + (dailyRank * 500) + " " + CURRENCY_NAME);
            lore.add("");

            if (canClaim) {
                lore.add(ChatColor.GREEN + "Click to claim!");
            } else {
                long remaining = dayInMillis - (now - data.lastDailyClaimTime);
                long hoursLeft = remaining / (60L * 60L * 1000L);
                lore.add(ChatColor.YELLOW + "Available in " + hoursLeft + "h");
            }
        }

        return namedItem(Material.GOLDEN_APPLE, ChatColor.GOLD + "Daily Reward", lore);
    }

    private ItemStack namedItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String progressBar(long current, long max) {
        double ratio = max <= 0 ? 1.0 : Math.min(1.0, Math.max(0.0, current / (double) max));
        int filled = (int) Math.round(ratio * 20.0);
        StringBuilder builder = new StringBuilder();
        builder.append(ChatColor.GOLD);
        for (int i = 0; i < 20; i++) {
            if (i < filled) {
                builder.append('|');
            } else {
                builder.append(ChatColor.GRAY).append('|').append(ChatColor.GOLD);
            }
        }
        return builder.toString();
    }

    // ===== Data Persistence =====

    private void startAutoSave() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        long intervalSeconds = 300L; // 5 minutes
        autoSaveTask = Bukkit.getScheduler().runTaskTimer(plugin, this::saveDirty, intervalSeconds * 20L, intervalSeconds * 20L);
    }

    private void saveDirty() {
        if (!dirtyProfiles.isEmpty()) {
            saveAll();
        }
    }

    private void loadAll() {
        profileData.clear();
        if (!dataFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : config.getKeys(false)) {
            UUID profileId;
            try {
                profileId = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            ConfigurationSection section = config.getConfigurationSection(key);
            HeartProfileData data = HeartProfileData.load(section, 1, 2);
            profileData.put(profileId, data);
        }
    }

    private void saveAll() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, HeartProfileData> entry : profileData.entrySet()) {
            ConfigurationSection section = config.createSection(entry.getKey().toString());
            entry.getValue().save(section);
        }

        File parent = dataFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try {
            config.save(dataFile);
            dirtyProfiles.clear();
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save Minehub Heart data: " + e.getMessage());
        }
    }
}
