package io.papermc.Grivience.mines;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.collections.CollectionsManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Commission system for the Minehub - daily tasks that reward Mithril Powder and XP.
 * Players get 4 commissions per day that reset at midnight.
 */
public final class MinehubCommissionManager implements Listener {
    private static final String GUI_TITLE = ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Minehub Commissions";
    private static final int COMMISSIONS_PER_DAY = 4;
    private static final long DAY_IN_MILLIS = 24L * 60L * 60L * 1000L;

    private final GriviencePlugin plugin;
    private final CollectionsManager collectionsManager;
    private final MinehubHeartManager heartManager;
    private final File dataFile;
    private final Map<UUID, CommissionProfile> profileData = new HashMap<>();
    private BukkitTask autoSaveTask;

    public enum CommissionType {
        // Ore mining commissions
        MINE_COAL("Mine Coal", Material.COAL_ORE, 100, 150, 20, "Mine %d Coal Ore blocks"),
        MINE_IRON("Mine Iron", Material.IRON_ORE, 80, 120, 25, "Mine %d Iron Ore blocks"),
        MINE_COPPER("Mine Copper", Material.COPPER_ORE, 75, 110, 22, "Mine %d Copper Ore blocks"),
        MINE_GOLD("Mine Gold", Material.GOLD_ORE, 50, 80, 30, "Mine %d Gold Ore blocks"),
        MINE_LAPIS("Mine Lapis", Material.LAPIS_ORE, 40, 70, 35, "Mine %d Lapis Ore blocks"),
        MINE_REDSTONE("Mine Redstone", Material.REDSTONE_ORE, 60, 90, 32, "Mine %d Redstone Ore blocks"),
        MINE_DIAMOND("Mine Diamond", Material.DIAMOND_ORE, 20, 40, 50, "Mine %d Diamond Ore blocks"),
        MINE_EMERALD("Mine Emerald", Material.EMERALD_ORE, 15, 30, 60, "Mine %d Emerald Ore blocks"),
        MINE_SAPPHIRE("Mine Sapphire", Material.BLUE_STAINED_GLASS, 50, 85, 40, "Mine %d Sapphire blocks"),

        // Mixed commissions
        COLLECT_VARIETY("Ore Variety", Material.CHEST, 0, 0, 45, "Mine 5 different ore types (10 each)"),
        DEEPSLATE_MINER("Deepslate Miner", Material.DEEPSLATE, 120, 180, 35, "Mine %d Deepslate ores"),
        BLOCK_COLLECTOR("Block Collector", Material.DIAMOND_BLOCK, 15, 30, 55, "Mine %d mineral blocks (Lapis/Redstone/etc)"),

        // Advanced commissions
        SAPPHIRE_HUNTER("Sapphire Hunter", Material.BLUE_STAINED_GLASS_PANE, 80, 130, 50, "Mine %d Sapphire blocks"),
        OBSIDIAN_BREAKER("Obsidian Breaker", Material.OBSIDIAN, 25, 45, 65, "Mine %d Obsidian blocks"),
        MASTER_MINER("Master Miner", Material.GOLDEN_PICKAXE, 0, 0, 80, "Mine 300 total blocks in Minehub");

        private final String displayName;
        private final Material icon;
        private final int minTarget;
        private final int maxTarget;
        private final int rewardPowder;
        private final String descriptionFormat;

        CommissionType(String displayName, Material icon, int minTarget, int maxTarget, int rewardPowder, String descriptionFormat) {
            this.displayName = displayName;
            this.icon = icon;
            this.minTarget = minTarget;
            this.maxTarget = maxTarget;
            this.rewardPowder = rewardPowder;
            this.descriptionFormat = descriptionFormat;
        }

        public String displayName() { return displayName; }
        public Material icon() { return icon; }
        public int rewardPowder() { return rewardPowder; }

        public int rollTarget() {
            if (minTarget == 0 && maxTarget == 0) {
                return switch (this) {
                    case COLLECT_VARIETY -> 50; // 5 types × 10 each
                    case MASTER_MINER -> 300;
                    default -> 100;
                };
            }
            return ThreadLocalRandom.current().nextInt(minTarget, maxTarget + 1);
        }

        public String description(int target) {
            return String.format(descriptionFormat, target);
        }

        public boolean isSpecial() {
            return this == COLLECT_VARIETY || this == MASTER_MINER;
        }
    }

    public record ActiveCommission(CommissionType type, int target, int progress, long startedAt) {
        public boolean isComplete() {
            return progress >= target;
        }

        public double progressPercent() {
            return target <= 0 ? 100.0 : Math.min(100.0, (progress * 100.0) / target);
        }
    }

    private static final class CommissionProfile {
        private long lastResetTime;
        private final List<ActiveCommission> activeCommissions = new ArrayList<>();
        private int completedToday;
        private final Map<CommissionType, Integer> varietyProgress = new EnumMap<>(CommissionType.class);

        private CommissionProfile() {
            this.lastResetTime = System.currentTimeMillis();
        }
    }

    public MinehubCommissionManager(GriviencePlugin plugin, CollectionsManager collectionsManager, MinehubHeartManager heartManager) {
        this.plugin = plugin;
        this.collectionsManager = collectionsManager;
        this.heartManager = heartManager;
        this.dataFile = new File(plugin.getDataFolder(), "minehub-commissions.yml");
        load();
        startAutoSave();
        startDailyResetTask();
    }

    public void shutdown() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        save();
    }

    public void openCommissionsGui(Player player) {
        CommissionProfile profile = getProfile(player);
        checkAndReset(profile);

        Inventory inv = Bukkit.createInventory(player, 27, GUI_TITLE);

        // Fill background
        ItemStack filler = namedItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, filler);
        }

        // Info item
        inv.setItem(4, createInfoItem(profile));

        // Commission slots
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        for (int i = 0; i < COMMISSIONS_PER_DAY && i < profile.activeCommissions.size(); i++) {
            ActiveCommission commission = profile.activeCommissions.get(i);
            inv.setItem(slots[i], createCommissionItem(commission));
        }

        // Close button
        inv.setItem(22, namedItem(Material.BARRIER, ChatColor.RED + "Close", List.of()));

        player.openInventory(inv);
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

        if (event.getRawSlot() == 22) {
            player.closeInventory();
            return;
        }

        // Check if clicking on a completed commission
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        int clickedSlot = event.getRawSlot();
        for (int i = 0; i < slots.length; i++) {
            if (clickedSlot == slots[i]) {
                CommissionProfile profile = getProfile(player);
                if (i < profile.activeCommissions.size()) {
                    ActiveCommission commission = profile.activeCommissions.get(i);
                    if (commission.isComplete()) {
                        claimCommission(player, profile, i);
                        openCommissionsGui(player);
                    }
                }
                break;
            }
        }
    }

    public void recordMining(Player player, Material material) {
        CommissionProfile profile = getProfile(player);
        checkAndReset(profile);

        boolean updated = false;

        for (int i = 0; i < profile.activeCommissions.size(); i++) {
            ActiveCommission commission = profile.activeCommissions.get(i);
            if (commission.isComplete()) {
                continue;
            }

            boolean matches = false;

            switch (commission.type()) {
                case MINE_COAL -> matches = material == Material.COAL_ORE || material == Material.DEEPSLATE_COAL_ORE;
                case MINE_IRON -> matches = material == Material.IRON_ORE || material == Material.DEEPSLATE_IRON_ORE;
                case MINE_COPPER -> matches = material == Material.COPPER_ORE || material == Material.DEEPSLATE_COPPER_ORE;
                case MINE_GOLD -> matches = material == Material.GOLD_ORE || material == Material.DEEPSLATE_GOLD_ORE;
                case MINE_LAPIS -> matches = material == Material.LAPIS_ORE || material == Material.DEEPSLATE_LAPIS_ORE || material == Material.LAPIS_BLOCK;
                case MINE_REDSTONE -> matches = material == Material.REDSTONE_ORE || material == Material.DEEPSLATE_REDSTONE_ORE || material == Material.REDSTONE_BLOCK;
                case MINE_DIAMOND -> matches = material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE || material == Material.DIAMOND_BLOCK;
                case MINE_EMERALD -> matches = material == Material.EMERALD_ORE || material == Material.DEEPSLATE_EMERALD_ORE || material == Material.EMERALD_BLOCK;
                case MINE_SAPPHIRE -> matches = material == Material.BLUE_STAINED_GLASS || material == Material.BLUE_STAINED_GLASS_PANE || material == Material.BLUE_CONCRETE_POWDER;
                case DEEPSLATE_MINER -> matches = material.name().startsWith("DEEPSLATE_");
                case BLOCK_COLLECTOR -> matches = material == Material.LAPIS_BLOCK || material == Material.REDSTONE_BLOCK || material == Material.DIAMOND_BLOCK || material == Material.EMERALD_BLOCK;
                case SAPPHIRE_HUNTER -> matches = material == Material.BLUE_STAINED_GLASS || material == Material.BLUE_STAINED_GLASS_PANE || material == Material.BLUE_CONCRETE_POWDER;
                case OBSIDIAN_BREAKER -> matches = material == Material.OBSIDIAN;
                case MASTER_MINER -> matches = true; // All blocks count
                case COLLECT_VARIETY -> {
                    // Track variety progress separately
                    if (isValidOreType(material)) {
                        CommissionType oreType = getOreTypeFromMaterial(material);
                        if (oreType != null) {
                            int current = profile.varietyProgress.getOrDefault(oreType, 0);
                            if (current < 10) {
                                profile.varietyProgress.put(oreType, current + 1);
                                // Check if we have 5 types at 10 each
                                long completedTypes = profile.varietyProgress.values().stream().filter(v -> v >= 10).count();
                                ActiveCommission updatedComm = new ActiveCommission(
                                    commission.type(),
                                    commission.target(),
                                    (int) (completedTypes * 10),
                                    commission.startedAt()
                                );
                                profile.activeCommissions.set(i, updatedComm);
                                updated = true;
                            }
                        }
                    }
                    continue;
                }
            }

            if (matches) {
                ActiveCommission updatedCommission = new ActiveCommission(
                    commission.type(),
                    commission.target(),
                    Math.min(commission.target(), commission.progress() + 1),
                    commission.startedAt()
                );
                profile.activeCommissions.set(i, updatedCommission);
                updated = true;

                // Notify on completion
                if (updatedCommission.isComplete() && !commission.isComplete()) {
                    player.sendMessage(ChatColor.GREEN + "Commission Complete! " + ChatColor.GRAY + commission.type().displayName());
                    player.sendMessage(ChatColor.GRAY + "Open the commission menu to claim your reward!");
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6F, 1.5F);
                }
            }
        }

        if (updated) {
            save();
        }
    }

    private void claimCommission(Player player, CommissionProfile profile, int index) {
        if (index < 0 || index >= profile.activeCommissions.size()) {
            return;
        }

        ActiveCommission commission = profile.activeCommissions.get(index);
        if (!commission.isComplete()) {
            player.sendMessage(ChatColor.RED + "This commission is not complete yet!");
            return;
        }

        // Grant rewards
        int powderReward = commission.type().rewardPowder();
        int xpReward = (int) (powderReward * 0.5); // XP is 50% of powder

        if (heartManager != null) {
            heartManager.recordMining(player, Material.STONE); // Dummy material, just for XP/powder tracking
        }

        player.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "COMMISSION CLAIMED!");
        player.sendMessage(ChatColor.GRAY + "Reward: " + ChatColor.LIGHT_PURPLE + "+" + powderReward + " Mithril Powder"
            + ChatColor.DARK_GRAY + " | " + ChatColor.AQUA + "+" + xpReward + " Heart XP");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8F, 1.3F);

        // Remove completed commission
        profile.activeCommissions.remove(index);
        profile.completedToday++;

        // Generate new commission if more available today
        if (profile.activeCommissions.size() < COMMISSIONS_PER_DAY) {
            ActiveCommission newCommission = generateCommission();
            profile.activeCommissions.add(newCommission);
            player.sendMessage(ChatColor.YELLOW + "New commission available: " + ChatColor.GOLD + newCommission.type().displayName());
        }

        save();
    }

    private void checkAndReset(CommissionProfile profile) {
        long now = System.currentTimeMillis();
        if ((now - profile.lastResetTime) >= DAY_IN_MILLIS) {
            profile.activeCommissions.clear();
            profile.varietyProgress.clear();
            profile.completedToday = 0;
            profile.lastResetTime = now;

            // Generate new commissions
            for (int i = 0; i < COMMISSIONS_PER_DAY; i++) {
                profile.activeCommissions.add(generateCommission());
            }

            save();
        }
    }

    private ActiveCommission generateCommission() {
        // Weighted random selection
        CommissionType[] types = CommissionType.values();
        CommissionType selected;

        // 80% chance for normal commissions, 20% for special
        if (ThreadLocalRandom.current().nextDouble() < 0.8) {
            // Select from non-special commissions
            List<CommissionType> normalTypes = new ArrayList<>();
            for (CommissionType type : types) {
                if (!type.isSpecial()) {
                    normalTypes.add(type);
                }
            }
            selected = normalTypes.get(ThreadLocalRandom.current().nextInt(normalTypes.size()));
        } else {
            // Select special commission
            List<CommissionType> specialTypes = new ArrayList<>();
            for (CommissionType type : types) {
                if (type.isSpecial()) {
                    specialTypes.add(type);
                }
            }
            selected = specialTypes.get(ThreadLocalRandom.current().nextInt(specialTypes.size()));
        }

        int target = selected.rollTarget();
        return new ActiveCommission(selected, target, 0, System.currentTimeMillis());
    }

    private CommissionType getOreTypeFromMaterial(Material material) {
        return switch (material) {
            case COAL_ORE, DEEPSLATE_COAL_ORE -> CommissionType.MINE_COAL;
            case IRON_ORE, DEEPSLATE_IRON_ORE -> CommissionType.MINE_IRON;
            case COPPER_ORE, DEEPSLATE_COPPER_ORE -> CommissionType.MINE_COPPER;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE -> CommissionType.MINE_GOLD;
            case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> CommissionType.MINE_LAPIS;
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> CommissionType.MINE_REDSTONE;
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> CommissionType.MINE_DIAMOND;
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> CommissionType.MINE_EMERALD;
            case BLUE_STAINED_GLASS, BLUE_STAINED_GLASS_PANE, BLUE_CONCRETE_POWDER -> CommissionType.MINE_SAPPHIRE;
            default -> null;
        };
    }

    private boolean isValidOreType(Material material) {
        return getOreTypeFromMaterial(material) != null;
    }

    private ItemStack createInfoItem(CommissionProfile profile) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Complete commissions for rewards!");
        lore.add("");
        lore.add(ChatColor.GRAY + "Active: " + ChatColor.YELLOW + profile.activeCommissions.size() + ChatColor.GRAY + "/" + ChatColor.YELLOW + COMMISSIONS_PER_DAY);
        lore.add(ChatColor.GRAY + "Completed Today: " + ChatColor.GREEN + profile.completedToday);
        lore.add("");

        long now = System.currentTimeMillis();
        long nextReset = profile.lastResetTime + DAY_IN_MILLIS;
        long hoursUntilReset = (nextReset - now) / (60L * 60L * 1000L);
        lore.add(ChatColor.GRAY + "Resets in: " + ChatColor.AQUA + hoursUntilReset + "h");

        return namedItem(Material.MAP, ChatColor.DARK_PURPLE + "Commissions Info", lore);
    }

    private ItemStack createCommissionItem(ActiveCommission commission) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + commission.type().description(commission.target()));
        lore.add("");
        lore.add(ChatColor.GRAY + "Progress: " + ChatColor.YELLOW + commission.progress() + ChatColor.GRAY + "/" + ChatColor.YELLOW + commission.target());
        lore.add(progressBar(commission.progress(), commission.target()));
        lore.add("");
        lore.add(ChatColor.GRAY + "Reward: " + ChatColor.LIGHT_PURPLE + commission.type().rewardPowder() + " Mithril Powder");

        if (commission.isComplete()) {
            lore.add("");
            lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "✓ COMPLETE - Click to claim!");
        }

        ItemStack item = namedItem(commission.type().icon(), ChatColor.GOLD + commission.type().displayName(), lore);

        if (commission.isComplete()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
        }

        return item;
    }

    private String progressBar(int current, int max) {
        double ratio = max <= 0 ? 1.0 : Math.min(1.0, Math.max(0.0, current / (double) max));
        int filled = (int) Math.round(ratio * 20.0);
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

    private CommissionProfile getProfile(Player player) {
        UUID profileId = resolveProfileId(player);
        CommissionProfile profile = profileData.computeIfAbsent(profileId, ignored -> {
            CommissionProfile newProfile = new CommissionProfile();
            // Generate initial commissions
            for (int i = 0; i < COMMISSIONS_PER_DAY; i++) {
                newProfile.activeCommissions.add(generateCommission());
            }
            return newProfile;
        });
        return profile;
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

    private void startAutoSave() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        autoSaveTask = Bukkit.getScheduler().runTaskTimer(plugin, this::save, 6000L, 6000L); // Every 5 minutes
    }

    private void startDailyResetTask() {
        // Check for resets every hour
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (CommissionProfile profile : profileData.values()) {
                checkAndReset(profile);
            }
        }, 72000L, 72000L); // Every hour
    }

    private void load() {
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
            if (section == null) {
                continue;
            }

            CommissionProfile profile = new CommissionProfile();
            profile.lastResetTime = section.getLong("last-reset", System.currentTimeMillis());
            profile.completedToday = section.getInt("completed-today", 0);

            ConfigurationSection commissionsSection = section.getConfigurationSection("active");
            if (commissionsSection != null) {
                for (String commKey : commissionsSection.getKeys(false)) {
                    ConfigurationSection commSection = commissionsSection.getConfigurationSection(commKey);
                    if (commSection == null) continue;

                    String typeName = commSection.getString("type");
                    CommissionType type = null;
                    for (CommissionType t : CommissionType.values()) {
                        if (t.name().equals(typeName)) {
                            type = t;
                            break;
                        }
                    }

                    if (type != null) {
                        ActiveCommission commission = new ActiveCommission(
                            type,
                            commSection.getInt("target", 100),
                            commSection.getInt("progress", 0),
                            commSection.getLong("started", System.currentTimeMillis())
                        );
                        profile.activeCommissions.add(commission);
                    }
                }
            }

            profileData.put(profileId, profile);
        }
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, CommissionProfile> entry : profileData.entrySet()) {
            ConfigurationSection section = config.createSection(entry.getKey().toString());
            CommissionProfile profile = entry.getValue();

            section.set("last-reset", profile.lastResetTime);
            section.set("completed-today", profile.completedToday);

            ConfigurationSection commissionsSection = section.createSection("active");
            for (int i = 0; i < profile.activeCommissions.size(); i++) {
                ActiveCommission commission = profile.activeCommissions.get(i);
                ConfigurationSection commSection = commissionsSection.createSection(String.valueOf(i));
                commSection.set("type", commission.type().name());
                commSection.set("target", commission.target());
                commSection.set("progress", commission.progress());
                commSection.set("started", commission.startedAt());
            }
        }

        File parent = dataFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save Minehub Commissions data: " + e.getMessage());
        }
    }
}
