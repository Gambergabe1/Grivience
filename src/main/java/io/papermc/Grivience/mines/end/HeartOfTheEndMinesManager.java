package io.papermc.Grivience.mines.end;

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
import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;

/**
 * Custom Heart of the Mountain-style progression for End Hub and End Mines mining.
 * Uses level thresholds, tokens to unlock perks, and dust to upgrade perk ranks.
 */
public final class HeartOfTheEndMinesManager implements Listener {
    private static final String GUI_TITLE = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Heart of the End Mines";
    private static final String DUST_NAME = "End Dust";
    private static final int PEAK_SLOT = 22;
    private static final int PEAK_MAX_LEVEL = 5;
    private static final List<Long> LEGACY_LEVEL_THRESHOLDS = List.of(0L, 75L, 200L, 450L, 800L, 1300L, 2000L, 3000L, 4300L, 6000L);
    private static final List<Long> DEFAULT_LEVEL_THRESHOLDS = List.of(0L, 75L, 225L, 525L, 975L, 1700L, 2800L, 4400L, 6650L, 9750L);
    private static final List<Integer> DEFAULT_TIER_TOKEN_REWARDS = List.of(0, 1, 2, 2, 2, 2, 2, 3, 2, 2, 2);

    private final GriviencePlugin plugin;
    private final CollectionsManager collectionsManager;
    private final File dataFile;
    private final Map<UUID, HeartProfileData> profileData = new HashMap<>();
    private final Set<UUID> dirtyProfiles = new java.util.HashSet<>();
    private BukkitTask autoSaveTask;

    public enum MiningSource {
        END_MINE,
        KUNZITE
    }

    public record MiningBonus(int bonusDrops, double rareDropMultiplier, boolean echoTriggered, int xpAwarded, int dustAwarded) {
        public static MiningBonus none() {
            return new MiningBonus(0, 1.0D, false, 0, 0);
        }
    }

    public record HeartProfileRef(UUID profileId, String displayName) {
    }

    private enum PurchaseResult {
        OPENED,
        UNLOCKED,
        UPGRADED,
        PEAK_UPGRADED,
        MAXED,
        NEEDS_LEVEL,
        NEEDS_TOKENS,
        NEEDS_DUST
    }

    private enum HeartPerk {
        KUNZITE_FORTUNE(
                "kunzite_fortune",
                ChatColor.LIGHT_PURPLE + "Kunzite Fortune",
                Material.PINK_STAINED_GLASS,
                20,
                1,
                10,
                1,
                30,
                15,
                "Adds a 12% chance per rank for +1 Kunzite."
        ),
        VOID_FORTUNE(
                "void_fortune",
                ChatColor.AQUA + "Void Fortune",
                Material.END_STONE,
                24,
                2,
                10,
                1,
                35,
                18,
                "Adds a 10% chance per rank for +1 End Mines material."
        ),
        RIFT_INSIGHT(
                "rift_insight",
                ChatColor.DARK_PURPLE + "Rift Insight",
                Material.AMETHYST_SHARD,
                29,
                3,
                8,
                1,
                45,
                25,
                "Boosts rare End Mines drop chance by 5% per rank."
        ),
        POWDER_SENSE(
                "powder_sense",
                ChatColor.GOLD + "Dust Sense",
                Material.GLOWSTONE_DUST,
                31,
                4,
                8,
                1,
                40,
                20,
                "Boosts End Dust gains by 10% per rank."
        ),
        ECHO_SURGE(
                "echo_surge",
                ChatColor.LIGHT_PURPLE + "Echo Surge",
                Material.ENDER_EYE,
                33,
                6,
                5,
                1,
                75,
                45,
                "Adds a 5% chance per rank to trigger an Echo Surge (doubles drop and rewards)."
        );

        private final String id;
        private final String displayName;
        private final Material icon;
        private final int slot;
        private final int requiredHeartLevel;
        private final int maxLevel;
        private final int tokenUnlockCost;
        private final int baseDustCost;
        private final int dustCostStep;
        private final String description;

        HeartPerk(
                String id,
                String displayName,
                Material icon,
                int slot,
                int requiredHeartLevel,
                int maxLevel,
                int tokenUnlockCost,
                int baseDustCost,
                int dustCostStep,
                String description
        ) {
            this.id = id;
            this.displayName = displayName;
            this.icon = icon;
            this.slot = slot;
            this.requiredHeartLevel = requiredHeartLevel;
            this.maxLevel = maxLevel;
            this.tokenUnlockCost = tokenUnlockCost;
            this.baseDustCost = baseDustCost;
            this.dustCostStep = dustCostStep;
            this.description = description;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        public Material icon() {
            return icon;
        }

        public int slot() {
            return slot;
        }

        public int requiredHeartLevel() {
            return requiredHeartLevel;
        }

        public int maxLevel() {
            return maxLevel;
        }

        public int tokenUnlockCost() {
            return tokenUnlockCost;
        }

        public int dustCostForNextRank(int currentRank) {
            if (currentRank <= 0 || currentRank >= maxLevel) {
                return 0;
            }
            return baseDustCost + ((currentRank - 1) * dustCostStep);
        }

        public String description() {
            return description;
        }

        public static HeartPerk fromSlot(int slot) {
            for (HeartPerk perk : values()) {
                if (perk.slot == slot) {
                    return perk;
                }
            }
            return null;
        }
    }

    private static final class HeartProfileData {
        private int level;
        private long xp;
        private long dust;
        private int availableTokens;
        private int peakLevel;
        private final EnumMap<HeartPerk, Integer> perkRanks = new EnumMap<>(HeartPerk.class);

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
            data.dust = Math.max(0L, section.getLong("dust", section.getLong("powder", 0L)));
            data.peakLevel = Math.max(0, Math.min(PEAK_MAX_LEVEL, section.getInt("peak-level", 0)));
            ConfigurationSection perks = section.getConfigurationSection("perks");
            if (perks != null) {
                for (HeartPerk perk : HeartPerk.values()) {
                    int rank = Math.max(0, Math.min(perk.maxLevel(), perks.getInt(perk.id(), 0)));
                    if (rank > 0) {
                        data.perkRanks.put(perk, rank);
                    }
                }
            }
            return data;
        }

        private int perkRank(HeartPerk perk) {
            return perkRanks.getOrDefault(perk, 0);
        }

        private void setPerkRank(HeartPerk perk, int rank) {
            if (rank <= 0) {
                perkRanks.remove(perk);
                return;
            }
            perkRanks.put(perk, rank);
        }

        private void save(ConfigurationSection section) {
            section.set("level", level);
            section.set("xp", xp);
            section.set("dust", dust);
            section.set("powder", null);
            section.set("available-tokens", availableTokens);
            section.set("peak-level", peakLevel);
            ConfigurationSection perks = section.createSection("perks");
            for (Map.Entry<HeartPerk, Integer> entry : perkRanks.entrySet()) {
                if (entry.getValue() != null && entry.getValue() > 0) {
                    perks.set(entry.getKey().id(), entry.getValue());
                }
            }
        }
    }

    public HeartOfTheEndMinesManager(GriviencePlugin plugin, CollectionsManager collectionsManager) {
        this.plugin = plugin;
        this.collectionsManager = collectionsManager;
        this.dataFile = new File(plugin.getDataFolder(), "end-mines/heart/player_data.yml");
        loadAll();
        startAutoSave();
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("end-mines.heart.enabled", true);
    }

    public void shutdown() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
        saveAll();
    }

    public void openGui(Player player) {
        if (player == null) {
            return;
        }
        if (!isEnabled()) {
            player.sendMessage(ChatColor.RED + "Heart of the End Mines is disabled.");
            return;
        }

        HeartProfileData data = getData(player);
        List<Long> thresholds = getThresholds();
        Inventory inventory = Bukkit.createInventory(player, 54, GUI_TITLE);
        fillBackground(inventory);
        inventory.setItem(0, createHelpItem());
        inventory.setItem(4, createSummaryItem(data, thresholds));
        inventory.setItem(8, createCurrencyItem(data));
        inventory.setItem(13, createTierTrackItem(data, thresholds));
        inventory.setItem(PEAK_SLOT, createPeakItem(data));
        inventory.setItem(45, createNextUnlockItem(data));
        inventory.setItem(49, namedItem(Material.BARRIER, ChatColor.RED + "Close", List.of(ChatColor.GRAY + "Close the Heart menu.")));

        for (HeartPerk perk : HeartPerk.values()) {
            inventory.setItem(perk.slot(), createPerkItem(data, perk));
        }

        player.openInventory(inventory);
    }

    public void sendStats(Player player) {
        if (player == null) {
            return;
        }
        HeartProfileData data = getData(player);
        List<Long> thresholds = getThresholds();
        long currentFloor = thresholdForLevel(data.level, thresholds);
        long nextThreshold = thresholdForLevel(Math.min(thresholds.size(), data.level + 1), thresholds);
        long progress = Math.max(0L, data.xp - currentFloor);
        long needed = Math.max(1L, nextThreshold - currentFloor);

        player.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "=== Heart of the End Mines ===");
        player.sendMessage(ChatColor.GRAY + "Level: " + ChatColor.LIGHT_PURPLE + data.level + ChatColor.GRAY + "/" + ChatColor.LIGHT_PURPLE + thresholds.size());
        player.sendMessage(ChatColor.GRAY + "XP: " + ChatColor.AQUA + formatLong(data.xp));
        player.sendMessage(ChatColor.GRAY + "Progress: " + ChatColor.AQUA + formatLong(progress) + ChatColor.GRAY + "/" + ChatColor.AQUA + formatLong(needed));
        player.sendMessage(ChatColor.GRAY + DUST_NAME + ": " + ChatColor.GOLD + formatLong(data.dust));
        player.sendMessage(ChatColor.GRAY + "Tokens: " + ChatColor.GREEN + data.availableTokens);
        player.sendMessage(ChatColor.GRAY + "Peak of the End Mines: " + ChatColor.LIGHT_PURPLE + data.peakLevel + ChatColor.GRAY + "/" + ChatColor.LIGHT_PURPLE + PEAK_MAX_LEVEL);
    }

    public MiningBonus recordMining(Player player, MiningSource source) {
        if (player == null || source == null || !isEnabled()) {
            return MiningBonus.none();
        }

        HeartProfileData data = getData(player);
        int rankFortune = source == MiningSource.KUNZITE ? data.perkRank(HeartPerk.KUNZITE_FORTUNE) : data.perkRank(HeartPerk.VOID_FORTUNE);
        int bonusDrops = rollBonusDrops(rankFortune, source == MiningSource.KUNZITE ? 0.12D : 0.10D);
        double rareDropMultiplier = (1.0D + (data.perkRank(HeartPerk.RIFT_INSIGHT) * 0.05D)) * peakRareDropMultiplier(data.peakLevel);

        double globalHotemPower = 1.0;
        if (plugin.getGlobalEventManager() != null) {
            globalHotemPower = plugin.getGlobalEventManager().getMultiplier(io.papermc.Grivience.event.GlobalEventManager.BoosterType.ENDMINES_HEART);
        }

        int xpAwarded = (int) Math.max(1, Math.round(baseXpFor(source) * peakXpMultiplier(data.peakLevel) * globalHotemPower));
        int dustAwarded = rollDustFor(source);
        double dustMultiplier = (1.0D + (data.perkRank(HeartPerk.POWDER_SENSE) * 0.10D)) * peakDustMultiplier(data.peakLevel) * globalHotemPower;
        dustAwarded = Math.max(1, (int) Math.round(dustAwarded * dustMultiplier));

        boolean echoTriggered = false;
        int echoRank = data.perkRank(HeartPerk.ECHO_SURGE);
        if (echoRank > 0 && ThreadLocalRandom.current().nextDouble() < echoRank * 0.05D) {
            echoTriggered = true;
            bonusDrops += 1;
            dustAwarded += source == MiningSource.KUNZITE ? 2 : 1;
            xpAwarded += source == MiningSource.KUNZITE ? 6 : 2;
            player.sendMessage(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "⚡ ECHO SURGE! " + ChatColor.GRAY + "Your Heart echoes through the node!");
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6F, 1.8F);
        }

        applyProgress(player, data, xpAwarded, dustAwarded);
        return new MiningBonus(bonusDrops, rareDropMultiplier, echoTriggered, xpAwarded, dustAwarded);
    }

    public void grantProgress(Player player, long xpReward, long dustReward, String source) {
        if (player == null || !isEnabled()) {
            return;
        }
        int safeXp = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, xpReward));
        int safeDust = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, dustReward));
        if (safeXp <= 0 && safeDust <= 0) {
            return;
        }
        HeartProfileData data = getData(player);
        applyProgress(player, data, safeXp, safeDust);
    }

    public HeartProfileRef resolveProfileTarget(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        ProfileManager profileManager = plugin.getProfileManager();
        try {
            UUID uuid = UUID.fromString(input);
            if (profileManager != null) {
                SkyBlockProfile directProfile = profileManager.getProfile(uuid);
                if (directProfile != null && directProfile.getProfileId() != null) {
                    return new HeartProfileRef(directProfile.getProfileId(), profileLabel(directProfile));
                }

                SkyBlockProfile selected = profileManager.getSelectedProfile(uuid);
                if (selected != null && selected.getProfileId() != null) {
                    return new HeartProfileRef(selected.getProfileId(), profileLabel(selected));
                }
            }
            if (profileData.containsKey(uuid)) {
                return new HeartProfileRef(uuid, uuid.toString());
            }
        } catch (IllegalArgumentException ignored) {
        }

        Player online = Bukkit.getPlayerExact(input);
        if (online != null) {
            UUID profileId = resolveProfileId(online);
            if (profileId != null) {
                return new HeartProfileRef(profileId, online.getName() + " (" + selectedProfileName(online.getUniqueId()) + ")");
            }
        }

        if (profileManager != null) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(input);
            if (offline != null && offline.getUniqueId() != null) {
                SkyBlockProfile selected = profileManager.getSelectedProfile(offline.getUniqueId());
                if (selected != null && selected.getProfileId() != null) {
                    String ownerName = offline.getName() != null ? offline.getName() : selected.getOwnerId().toString();
                    return new HeartProfileRef(selected.getProfileId(), ownerName + " (" + selected.getProfileName() + ")");
                }
            }
        }

        return null;
    }

    public boolean resetProfileProgress(UUID profileId) {
        if (profileId == null) {
            return false;
        }
        HeartProfileData removed = profileData.remove(profileId);
        dirtyProfiles.remove(profileId);
        if (removed == null) {
            return false;
        }
        saveAll();
        return true;
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
        if (slot == PEAK_SLOT) {
            PurchaseResult result = purchasePeak(player);
            switch (result) {
                case PEAK_UPGRADED -> player.sendMessage(ChatColor.GREEN + "Upgraded Peak of the End Mines.");
                case MAXED -> player.sendMessage(ChatColor.YELLOW + "Peak of the End Mines is already maxed.");
                case NEEDS_LEVEL -> player.sendMessage(ChatColor.RED + "You need a higher Heart tier to deepen your Peak of the End Mines.");
                case NEEDS_TOKENS -> player.sendMessage(ChatColor.RED + "You need 1 Token of the End Mines to upgrade Peak of the End Mines.");
                default -> {
                    return;
                }
            }
            openGui(player);
            return;
        }

        HeartPerk perk = HeartPerk.fromSlot(slot);
        if (perk == null) {
            return;
        }

        PurchaseResult result = purchase(player, perk);
        switch (result) {
            case UNLOCKED -> player.sendMessage(ChatColor.GREEN + "Unlocked " + perk.displayName() + ChatColor.GREEN + ".");
            case UPGRADED -> player.sendMessage(ChatColor.GREEN + "Upgraded " + perk.displayName() + ChatColor.GREEN + ".");
            case MAXED -> player.sendMessage(ChatColor.YELLOW + "That perk is already maxed.");
            case NEEDS_LEVEL -> player.sendMessage(ChatColor.RED + "You need Heart of the End Mines level " + perk.requiredHeartLevel() + " to unlock that perk.");
            case NEEDS_TOKENS -> player.sendMessage(ChatColor.RED + "You need " + perk.tokenUnlockCost() + " token(s) to unlock that perk.");
            case NEEDS_DUST -> player.sendMessage(ChatColor.RED + "You do not have enough " + DUST_NAME + " for that upgrade.");
            default -> {
                return;
            }
        }

        openGui(player);
    }

    private PurchaseResult purchase(Player player, HeartPerk perk) {
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

        int cost = perk.dustCostForNextRank(rank);
        if (data.dust < cost) {
            return PurchaseResult.NEEDS_DUST;
        }

        data.dust -= cost;
        data.setPerkRank(perk, rank + 1);
        markDirty(player);
        return PurchaseResult.UPGRADED;
    }

    private HeartProfileData getData(Player player) {
        UUID profileId = resolveProfileId(player);
        return profileData.computeIfAbsent(profileId, ignored -> HeartProfileData.createDefault(getStartingLevel(), getStartingTokens()));
    }

    private void applyProgress(Player player, HeartProfileData data, int xpAwarded, int dustAwarded) {
        if (player == null || data == null) {
            return;
        }
        data.xp += Math.max(0, xpAwarded);
        data.dust += Math.max(0, dustAwarded);
        levelIfNeeded(player, data);
        markDirty(player);
        sendGainActionBar(player, xpAwarded, dustAwarded);
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

    private String selectedProfileName(UUID ownerId) {
        ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager == null || ownerId == null) {
            return "Profile";
        }
        SkyBlockProfile profile = profileManager.getSelectedProfile(ownerId);
        if (profile == null || profile.getProfileName() == null || profile.getProfileName().isBlank()) {
            return "Profile";
        }
        return profile.getProfileName();
    }

    private String profileLabel(SkyBlockProfile profile) {
        if (profile == null) {
            return "Profile";
        }
        String profileName = profile.getProfileName() == null || profile.getProfileName().isBlank()
                ? "Profile"
                : profile.getProfileName();
        String ownerName = profile.getOwnerId() == null ? "Unknown" : profile.getOwnerId().toString();
        OfflinePlayer owner = profile.getOwnerId() == null ? null : Bukkit.getOfflinePlayer(profile.getOwnerId());
        if (owner != null && owner.getName() != null && !owner.getName().isBlank()) {
            ownerName = owner.getName();
        }
        return ownerName + " (" + profileName + ")";
    }

    private void markDirty(Player player) {
        UUID profileId = resolveProfileId(player);
        if (profileId != null) {
            dirtyProfiles.add(profileId);
        }
    }

    private PurchaseResult purchasePeak(Player player) {
        HeartProfileData data = getData(player);
        int nextLevel = data.peakLevel + 1;
        if (data.peakLevel >= PEAK_MAX_LEVEL) {
            return PurchaseResult.MAXED;
        }
        if (data.level < peakRequiredHeartLevel(nextLevel)) {
            return PurchaseResult.NEEDS_LEVEL;
        }
        if (data.availableTokens < 1) {
            return PurchaseResult.NEEDS_TOKENS;
        }
        data.availableTokens -= 1;
        data.peakLevel = nextLevel;
        markDirty(player);
        return PurchaseResult.PEAK_UPGRADED;
    }

    private int getStartingLevel() {
        return Math.max(1, plugin.getConfig().getInt("end-mines.heart.starting-level", 1));
    }

    private int getStartingTokens() {
        return Math.max(0, plugin.getConfig().getInt("end-mines.heart.starting-tokens", 1));
    }

    private List<Integer> getTierTokenRewards() {
        return normalizeTierTokenRewards(
                plugin.getConfig().getIntegerList("end-mines.heart.tier-token-rewards"),
                getThresholds().size(),
                plugin.getConfig().getInt("end-mines.heart.tokens-per-level", 1)
        );
    }

    private List<Long> getThresholds() {
        List<Long> configured = plugin.getConfig().getLongList("end-mines.heart.level-thresholds");
        return normalizeThresholds(configured);
    }

    static List<Long> normalizeThresholds(List<Long> configured) {
        if (configured == null || configured.isEmpty()) {
            return DEFAULT_LEVEL_THRESHOLDS;
        }
        List<Long> normalized = new ArrayList<>();
        for (Long value : configured) {
            if (value == null) {
                continue;
            }
            normalized.add(Math.max(0L, value));
        }
        if (normalized.isEmpty()) {
            return DEFAULT_LEVEL_THRESHOLDS;
        }
        normalized.set(0, 0L);
        if (normalized.equals(LEGACY_LEVEL_THRESHOLDS)) {
            return DEFAULT_LEVEL_THRESHOLDS;
        }

        List<Long> widened = new ArrayList<>(normalized.size());
        widened.add(0L);

        long previousGap = 0L;
        long previousThreshold = 0L;
        for (int index = 1; index < normalized.size(); index++) {
            long requestedGap = Math.max(1L, normalized.get(index) - normalized.get(index - 1));
            if (index >= 2) {
                requestedGap = Math.max(requestedGap, previousGap + 1L);
            }
            previousThreshold += requestedGap;
            widened.add(previousThreshold);
            previousGap = requestedGap;
        }
        return List.copyOf(widened);
    }

    static List<Integer> normalizeTierTokenRewards(List<Integer> configured, int maxTier, int legacyTokensPerLevel) {
        int safeMaxTier = Math.max(1, maxTier);
        List<Integer> base;
        if (configured != null && !configured.isEmpty()) {
            List<Integer> normalized = new ArrayList<>();
            for (Integer value : configured) {
                if (value == null) {
                    continue;
                }
                normalized.add(Math.max(0, value));
            }
            if (!normalized.isEmpty()) {
                if (normalized.get(0) != 0) {
                    normalized.add(0, 0);
                } else {
                    normalized.set(0, 0);
                }
                base = normalized;
            } else {
                base = List.of();
            }
        } else if (legacyTokensPerLevel > 1) {
            List<Integer> repeated = new ArrayList<>();
            repeated.add(0);
            for (int tier = 1; tier <= safeMaxTier; tier++) {
                repeated.add(Math.max(1, legacyTokensPerLevel));
            }
            base = repeated;
        } else {
            base = DEFAULT_TIER_TOKEN_REWARDS;
        }

        List<Integer> expanded = new ArrayList<>();
        expanded.add(0);
        for (int tier = 1; tier <= safeMaxTier; tier++) {
            int fallback = tokenRewardFromDefault(tier);
            int reward = tier < base.size() ? Math.max(0, base.get(tier)) : fallback;
            expanded.add(reward);
        }
        return List.copyOf(expanded);
    }

    private static int tokenRewardFromDefault(int tier) {
        int safeTier = Math.max(1, tier);
        if (safeTier < DEFAULT_TIER_TOKEN_REWARDS.size()) {
            return DEFAULT_TIER_TOKEN_REWARDS.get(safeTier);
        }
        return DEFAULT_TIER_TOKEN_REWARDS.get(DEFAULT_TIER_TOKEN_REWARDS.size() - 1);
    }

    private int tokensAwardedForTier(int tier) {
        List<Integer> rewards = getTierTokenRewards();
        if (tier <= 0) {
            return 0;
        }
        if (tier < rewards.size()) {
            return rewards.get(tier);
        }
        return rewards.get(rewards.size() - 1);
    }

    private long thresholdForLevel(int level, List<Long> thresholds) {
        if (thresholds.isEmpty()) {
            return 0L;
        }
        int index = Math.max(0, Math.min(thresholds.size() - 1, level - 1));
        return thresholds.get(index);
    }

    private void levelIfNeeded(Player player, HeartProfileData data) {
        List<Long> thresholds = getThresholds();
        int maxLevel = thresholds.size();
        while (data.level < maxLevel && data.xp >= thresholdForLevel(data.level + 1, thresholds)) {
            data.level++;
            int awardedTokens = tokensAwardedForTier(data.level);
            data.availableTokens += awardedTokens;
            player.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "HEART OF THE END MINES TIER UP! "
                    + ChatColor.LIGHT_PURPLE + "Tier " + data.level);
            player.sendMessage(ChatColor.GRAY + "You gained " + ChatColor.GREEN + awardedTokens
                    + ChatColor.GRAY + " Token(s) of the End Mines.");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.2F);

            SkyblockLevelManager levelManager = plugin.getSkyblockLevelManager();
            if (levelManager != null) {
                levelManager.awardObjectiveXp(
                        player,
                        "skill.end_mines.heart.level_" + data.level,
                        "skill",
                        5L,
                        "Heart of the End Mines Tier " + data.level,
                        true
                );
            }
        }
    }

    private int baseXpFor(MiningSource source) {
        return switch (source) {
            case END_MINE -> Math.max(1, plugin.getConfig().getInt("end-mines.heart.rewards.end-mine-xp", 1));
            case KUNZITE -> Math.max(1, plugin.getConfig().getInt("end-mines.heart.rewards.kunzite-xp", 8));
        };
    }

    private int rollDustFor(MiningSource source) {
        int min;
        int max;
        if (source == MiningSource.KUNZITE) {
            min = configuredReward("end-mines.heart.rewards.kunzite-dust-min", "end-mines.heart.rewards.kunzite-powder-min", 2);
            max = Math.max(min, configuredReward("end-mines.heart.rewards.kunzite-dust-max", "end-mines.heart.rewards.kunzite-powder-max", 4));
        } else {
            min = configuredReward("end-mines.heart.rewards.end-mine-dust-min", "end-mines.heart.rewards.end-mine-powder-min", 1);
            max = Math.max(min, configuredReward("end-mines.heart.rewards.end-mine-dust-max", "end-mines.heart.rewards.end-mine-powder-max", 2));
        }
        if (max <= min) {
            return min;
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private int configuredReward(String primaryPath, String legacyPath, int fallback) {
        if (plugin.getConfig().isSet(primaryPath)) {
            return Math.max(1, plugin.getConfig().getInt(primaryPath, fallback));
        }
        if (legacyPath != null && plugin.getConfig().isSet(legacyPath)) {
            return Math.max(1, plugin.getConfig().getInt(legacyPath, fallback));
        }
        return Math.max(1, plugin.getConfig().getInt(primaryPath,
                legacyPath == null ? fallback : plugin.getConfig().getInt(legacyPath, fallback)));
    }

    private int rollBonusDrops(int perkRank, double chancePerRank) {
        int bonus = 0;
        for (int i = 0; i < perkRank; i++) {
            if (ThreadLocalRandom.current().nextDouble() < chancePerRank) {
                bonus++;
            }
        }
        return bonus;
    }

    private void startAutoSave() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        long intervalSeconds = Math.max(60L, plugin.getConfig().getLong("end-mines.heart.auto-save-interval-seconds", 300L));
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
        int defaultLevel = getStartingLevel();
        int defaultTokens = getStartingTokens();
        for (String key : config.getKeys(false)) {
            UUID profileId;
            try {
                profileId = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            ConfigurationSection section = config.getConfigurationSection(key);
            HeartProfileData data = HeartProfileData.load(section, defaultLevel, defaultTokens);
            normalizeData(data);
            profileData.put(profileId, data);
        }
    }

    private void normalizeData(HeartProfileData data) {
        if (data == null) {
            return;
        }
        int maxLevel = getThresholds().size();
        data.level = Math.max(1, Math.min(maxLevel, data.level));
        data.availableTokens = Math.max(0, data.availableTokens);
        data.xp = Math.max(0L, data.xp);
        data.dust = Math.max(0L, data.dust);
        data.peakLevel = Math.max(0, Math.min(PEAK_MAX_LEVEL, data.peakLevel));
        for (HeartPerk perk : HeartPerk.values()) {
            int current = data.perkRank(perk);
            if (current > perk.maxLevel()) {
                data.setPerkRank(perk, perk.maxLevel());
            }
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
            plugin.getLogger().warning("Failed to save Heart of the End Mines data: " + e.getMessage());
        }
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = namedItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        ItemStack frame = namedItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        setDecorativeSlots(inventory, frame,
                0, 1, 2, 3, 5, 6, 7, 8,
                9, 17, 18, 26, 27, 35,
                36, 37, 38, 39, 43, 44,
                45, 46, 47, 48, 49, 50, 51, 52, 53);

        ItemStack connector = namedItem(Material.MAGENTA_STAINED_GLASS_PANE, ChatColor.LIGHT_PURPLE + "Heart Path", List.of());
        setDecorativeSlots(inventory, connector, 12, 14, 21, 23, 30, 32, 40, 41, 42);
    }

    private void setDecorativeSlots(Inventory inventory, ItemStack item, int... slots) {
        for (int slot : slots) {
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, item);
            }
        }
    }

    private ItemStack createSummaryItem(HeartProfileData data, List<Long> thresholds) {
        long currentFloor = thresholdForLevel(data.level, thresholds);
        long nextThreshold = thresholdForLevel(Math.min(thresholds.size(), data.level + 1), thresholds);
        long progress = Math.max(0L, data.xp - currentFloor);
        long needed = Math.max(1L, nextThreshold - currentFloor);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Tier: " + ChatColor.LIGHT_PURPLE + data.level + ChatColor.GRAY + "/" + ChatColor.LIGHT_PURPLE + thresholds.size());
        lore.add(ChatColor.GRAY + "XP: " + ChatColor.AQUA + formatLong(data.xp));
        lore.add(ChatColor.GRAY + "Progress: " + ChatColor.AQUA + formatLong(progress) + ChatColor.GRAY + "/" + ChatColor.AQUA + formatLong(needed));
        lore.add(ChatColor.GRAY + progressBar(progress, needed));
        if (data.level < thresholds.size()) {
            lore.add(ChatColor.GRAY + "Next Tier Reward: " + ChatColor.GREEN + "+" + tokensAwardedForTier(data.level + 1) + " Token(s)");
        }
        lore.add(ChatColor.GRAY + "Peak: " + ChatColor.LIGHT_PURPLE + data.peakLevel + ChatColor.GRAY + "/" + ChatColor.LIGHT_PURPLE + PEAK_MAX_LEVEL);
        lore.add("");
        lore.add(ChatColor.GRAY + "Mine in the End Hub and End Mines");
        lore.add(ChatColor.GRAY + "to level up your Heart, earn");
        lore.add(ChatColor.GRAY + "tokens, and deepen your Peak.");
        return namedItem(Material.NETHER_STAR, ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Heart of the End Mines", lore);
    }

    private ItemStack createCurrencyItem(HeartProfileData data) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + DUST_NAME + ": " + ChatColor.GOLD + formatLong(data.dust));
        lore.add(ChatColor.GRAY + "Available Tokens: " + ChatColor.GREEN + data.availableTokens);
        lore.add("");
        lore.add(ChatColor.GRAY + "Unlock new nodes with tokens.");
        lore.add(ChatColor.GRAY + "Use " + DUST_NAME + ChatColor.GRAY + " to rank");
        lore.add(ChatColor.GRAY + "up unlocked perks like HOTM.");
        return namedItem(Material.GLOWSTONE_DUST, ChatColor.GOLD + "End Mines Dust", lore);
    }

    private ItemStack createTierTrackItem(HeartProfileData data, List<Long> thresholds) {
        long currentFloor = thresholdForLevel(data.level, thresholds);
        long nextThreshold = thresholdForLevel(Math.min(thresholds.size(), data.level + 1), thresholds);
        long progress = Math.max(0L, data.xp - currentFloor);
        long needed = Math.max(1L, nextThreshold - currentFloor);
        int nextReward = data.level < thresholds.size() ? tokensAwardedForTier(data.level + 1) : 0;

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current Tier: " + ChatColor.LIGHT_PURPLE + data.level);
        if (data.level >= thresholds.size()) {
            lore.add(ChatColor.GOLD + "Maximum Heart tier reached.");
        } else {
            lore.add(ChatColor.GRAY + "Progress to Tier " + (data.level + 1) + ":");
            lore.add(ChatColor.AQUA + formatLong(progress) + ChatColor.GRAY + "/" + ChatColor.AQUA + formatLong(needed));
            lore.add(ChatColor.GRAY + progressBar(progress, needed));
            lore.add(ChatColor.GRAY + "Next Reward: " + ChatColor.GREEN + "+" + nextReward + " Token(s)");
        }
        lore.add("");
        lore.add(ChatColor.GRAY + "This is your HOTM-style");
        lore.add(ChatColor.GRAY + "tier track for the End.");
        return namedItem(Material.EXPERIENCE_BOTTLE, ChatColor.AQUA + "Tier Track", lore);
    }

    private ItemStack createHelpItem() {
        return namedItem(
                Material.BOOK,
                ChatColor.AQUA + "How It Works",
                List.of(
                        ChatColor.GRAY + "The Heart menu is laid out like",
                        ChatColor.GRAY + "a HOTM-style mining tree.",
                        "",
                        ChatColor.GRAY + "Mine End nodes to gain Heart XP",
                        ChatColor.GRAY + "and " + ChatColor.GOLD + DUST_NAME + ChatColor.GRAY + ".",
                        ChatColor.GRAY + "Tier ups grant " + ChatColor.GREEN + "Tokens" + ChatColor.GRAY + ".",
                        ChatColor.GRAY + "Use Tokens to unlock nodes and",
                        ChatColor.GRAY + DUST_NAME + " to rank them up.",
                        ChatColor.GRAY + "Peak of the End Mines is the",
                        ChatColor.GRAY + "permanent center core."
                )
        );
    }

    private ItemStack createNextUnlockItem(HeartProfileData data) {
        HeartPerk nextLocked = null;
        for (HeartPerk perk : HeartPerk.values()) {
            if (data.perkRank(perk) <= 0) {
                nextLocked = perk;
                break;
            }
        }

        List<String> lore = new ArrayList<>();
        if (nextLocked == null) {
            lore.add(ChatColor.GOLD + "Every node is unlocked.");
            lore.add(ChatColor.GRAY + "Focus on maxing ranks and");
            lore.add(ChatColor.GRAY + "deepening Peak of the End Mines.");
            return namedItem(Material.EMERALD_BLOCK, ChatColor.GREEN + "Tree Completion", lore);
        }

        boolean levelReady = data.level >= nextLocked.requiredHeartLevel();
        boolean tokenReady = data.availableTokens >= nextLocked.tokenUnlockCost();
        lore.add(ChatColor.GRAY + "Next Locked Node:");
        lore.add(nextLocked.displayName());
        lore.add("");
        lore.add(ChatColor.GRAY + "Required Tier: "
                + (levelReady ? ChatColor.GREEN : ChatColor.RED) + nextLocked.requiredHeartLevel());
        lore.add(ChatColor.GRAY + "Unlock Cost: "
                + (tokenReady ? ChatColor.GREEN : ChatColor.RED) + nextLocked.tokenUnlockCost() + " Token(s)");
        lore.add("");
        lore.add(ChatColor.GRAY + "Status: "
                + (levelReady && tokenReady ? ChatColor.GREEN + "Ready to unlock" : ChatColor.YELLOW + "Keep progressing"));
        return namedItem(Material.COMPASS, ChatColor.YELLOW + "Next Unlock", lore);
    }

    private ItemStack createPeakItem(HeartProfileData data) {
        int nextLevel = Math.min(PEAK_MAX_LEVEL, data.peakLevel + 1);
        boolean maxed = data.peakLevel >= PEAK_MAX_LEVEL;
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "HOTM-style permanent core");
        lore.add(ChatColor.GRAY + "upgrades for your End mining.");
        lore.add("");
        lore.add(ChatColor.GRAY + "Current Rank: " + ChatColor.LIGHT_PURPLE + data.peakLevel + ChatColor.GRAY + "/" + ChatColor.LIGHT_PURPLE + PEAK_MAX_LEVEL);
        lore.add(ChatColor.GRAY + "Heart XP Gain: " + ChatColor.AQUA + percent(peakXpMultiplier(data.peakLevel) - 1.0D));
        lore.add(ChatColor.GRAY + DUST_NAME + " Gain: " + ChatColor.GOLD + percent(peakDustMultiplier(data.peakLevel) - 1.0D));
        lore.add(ChatColor.GRAY + "Rare Drop Bonus: " + ChatColor.LIGHT_PURPLE + percent(peakRareDropMultiplier(data.peakLevel) - 1.0D));
        if (maxed) {
            lore.add("");
            lore.add(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "✦ Endwalker's Blessing ✦");
            lore.add(ChatColor.GRAY + "The peak resonates with the End.");
            lore.add(ChatColor.GRAY + "All mining bonuses amplified to maximum.");
        }
        lore.add("");
        if (maxed) {
            lore.add(ChatColor.GOLD + "" + ChatColor.BOLD + "PEAK FULLY AWAKENED");
        } else {
            lore.add(ChatColor.GRAY + "Next Rank Requires Heart Tier: " + ChatColor.LIGHT_PURPLE + peakRequiredHeartLevel(nextLevel));
            lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GREEN + "1 Token of the End Mines");
            lore.add(ChatColor.YELLOW + "Click to deepen the Peak.");
        }
        return namedItem(Material.DRAGON_HEAD, ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Peak of the End Mines", lore);
    }

    private ItemStack createPerkItem(HeartProfileData data, HeartPerk perk) {
        int rank = data.perkRank(perk);
        boolean unlocked = rank > 0;
        boolean maxed = rank >= perk.maxLevel();
        List<String> lore = new ArrayList<>();

        lore.add(ChatColor.GRAY + perk.description());
        lore.add("");
        lore.add(ChatColor.GRAY + "Required Heart Tier: " + ChatColor.LIGHT_PURPLE + perk.requiredHeartLevel());
        lore.add(ChatColor.GRAY + "Current Rank: " + (unlocked ? ChatColor.GREEN + String.valueOf(rank) : ChatColor.RED + "Locked")
                + ChatColor.GRAY + "/" + ChatColor.GREEN + perk.maxLevel());
        lore.add(ChatColor.GRAY + "Current Effect: " + ChatColor.AQUA + currentEffect(perk, rank));
        lore.add("");

        if (!unlocked) {
            lore.add(ChatColor.GRAY + "Unlock Cost: " + ChatColor.GREEN + perk.tokenUnlockCost() + " Token(s)");
            lore.add(ChatColor.YELLOW + "Click to unlock.");
        } else if (maxed) {
            lore.add(ChatColor.GOLD + "Max rank reached.");
        } else {
            lore.add(ChatColor.GRAY + "Next Effect: " + ChatColor.AQUA + currentEffect(perk, rank + 1));
            lore.add(ChatColor.GRAY + "Upgrade Cost: " + ChatColor.GOLD + perk.dustCostForNextRank(rank) + " " + DUST_NAME);
            lore.add(ChatColor.YELLOW + "Click to upgrade.");
        }

        ItemStack item = namedItem(perk.icon(), perk.displayName(), lore);
        if (unlocked) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    private String currentEffect(HeartPerk perk, int rank) {
        int safeRank = Math.max(0, rank);
        return switch (perk) {
            case KUNZITE_FORTUNE -> "+" + (safeRank * 12) + "% extra Kunzite chance";
            case VOID_FORTUNE -> "+" + (safeRank * 10) + "% extra End Mines material chance";
            case RIFT_INSIGHT -> "+" + (safeRank * 5) + "% rare drop multiplier";
            case POWDER_SENSE -> "+" + (safeRank * 10) + "% " + DUST_NAME + " gain";
            case ECHO_SURGE -> "+" + (safeRank * 5) + "% Echo Surge chance";
        };
    }

    private int peakRequiredHeartLevel(int nextPeakLevel) {
        return switch (Math.max(1, Math.min(PEAK_MAX_LEVEL, nextPeakLevel))) {
            case 1 -> 2;
            case 2 -> 4;
            case 3 -> 6;
            case 4 -> 8;
            default -> 10;
        };
    }

    private double peakXpMultiplier(int peakLevel) {
        return 1.0D + (Math.max(0, peakLevel) * 0.08D);
    }

    private double peakDustMultiplier(int peakLevel) {
        return 1.0D + (Math.max(0, peakLevel) * 0.05D);
    }

    private double peakRareDropMultiplier(int peakLevel) {
        return 1.0D + (Math.max(0, peakLevel) * 0.03D);
    }

    private String percent(double ratio) {
        return "+" + String.format(Locale.US, "%.0f%%", Math.max(0.0D, ratio) * 100.0D);
    }

    private void sendGainActionBar(Player player, int xpAwarded, int dustAwarded) {
        if (player == null) {
            return;
        }
        StringBuilder message = new StringBuilder();
        if (xpAwarded > 0) {
            message.append(ChatColor.AQUA).append('+').append(xpAwarded).append(" Heart XP");
        }
        if (dustAwarded > 0) {
            if (message.length() > 0) {
                message.append(ChatColor.DARK_GRAY).append("  •  ");
            }
            message.append(ChatColor.GOLD).append('+').append(dustAwarded).append(' ').append(DUST_NAME);
        }
        if (message.length() > 0) {
            player.sendActionBar(message.toString());
        }
    }

    private ItemStack namedItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String progressBar(long current, long max) {
        double ratio = max <= 0 ? 1.0D : Math.min(1.0D, Math.max(0.0D, current / (double) max));
        int filled = (int) Math.round(ratio * 20.0D);
        StringBuilder builder = new StringBuilder();
        builder.append(ChatColor.DARK_PURPLE);
        for (int i = 0; i < 20; i++) {
            if (i < filled) {
                builder.append('|');
            } else {
                builder.append(ChatColor.GRAY).append('|').append(ChatColor.DARK_PURPLE);
            }
        }
        return builder.toString();
    }

    private String formatLong(long value) {
        return String.format(Locale.US, "%,d", value);
    }
}
