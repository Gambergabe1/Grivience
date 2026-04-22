package io.papermc.Grivience.mines;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.collections.CollectionsManager;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.skyblock.economy.ProfileEconomyService;
import io.papermc.Grivience.util.DropDeliveryUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class DrillForgeManager {
    private static final int MAX_QUEUE_SIZE = 3;
    public static final int MAX_HEAT = 100;
    public static final int OVERDRIVE_HEAT_COST = 20;
    public static final double OVERDRIVE_COIN_COST = 2500.0D;
    public static final long OVERDRIVE_BASE_DURATION_MILLIS = 12L * 60L * 1000L;
    private static final long HEAT_DECAY_INTERVAL_MILLIS = 24L * 60L * 60L * 1000L;  // 24 hours
    private static final int HEAT_DECAY_AMOUNT = 10;  // -10 heat per day of inactivity

    public enum StartResult {
        STARTED,
        QUEUE_FULL,
        NEEDS_ITEMS,
        NEEDS_COINS,
        INVALID
    }

    public enum ClaimResult {
        CLAIMED,
        NOT_READY,
        EMPTY
    }

    public enum OverdriveResult {
        ACTIVATED,
        ALREADY_ACTIVE,
        NEEDS_HEAT,
        NEEDS_COINS
    }

    public record ForgeIngredient(String customItemId, Material material, int amount) {
        public ForgeIngredient {
            customItemId = customItemId == null || customItemId.isBlank() ? null : customItemId.trim().toUpperCase(Locale.ROOT);
            amount = Math.max(1, amount);
        }

        public static ForgeIngredient custom(String itemId, int amount) {
            return new ForgeIngredient(itemId, null, amount);
        }

        public static ForgeIngredient vanilla(Material material, int amount) {
            return new ForgeIngredient(null, material, amount);
        }
    }

    public enum ForgeProjectType {
        VOLTA_INFUSION(
                "volta_infusion",
                ChatColor.AQUA + "Volta Infusion",
                "VOLTA",
                4,
                4L * 60L * 1000L,
                350.0D,
                4,
                "Compact volatile ore charge into premium drill fuel.",
                List.of(
                        ForgeIngredient.custom("ENCHANTED_SAPPHIRE", 1),
                        ForgeIngredient.vanilla(Material.REDSTONE, 32)
                )
        ),
        OIL_BARREL_COMPRESSION(
                "oil_barrel_compression",
                ChatColor.GOLD + "Oil Barrel Compression",
                "OIL_BARREL",
                2,
                7L * 60L * 1000L,
                650.0D,
                6,
                "Pressure-bond dense fuel stock for long mining runs.",
                List.of(
                        ForgeIngredient.custom("ENCHANTED_TITANIUM", 4),
                        ForgeIngredient.custom("VOLTA", 1),
                        ForgeIngredient.vanilla(Material.COAL_BLOCK, 8)
                )
        ),
        PROSPECTOR_COMPASS_MAPPING(
                "prospector_compass_mapping",
                ChatColor.AQUA + "Prospector Compass Mapping",
                "PROSPECTOR_COMPASS",
                1,
                5L * 60L * 1000L,
                900.0D,
                5,
                "Etch vein signatures into a reusable route compass.",
                List.of(
                        ForgeIngredient.custom("ENCHANTED_SAPPHIRE", 8),
                        ForgeIngredient.custom("RIFT_ESSENCE", 6),
                        ForgeIngredient.vanilla(Material.COMPASS, 1)
                )
        ),
        MINING_SCROLL_COMPILATION(
                "mining_scroll_compilation",
                ChatColor.YELLOW + "Mining Scroll Compilation",
                "MINING_XP_SCROLL",
                2,
                3L * 60L * 1000L,
                750.0D,
                3,
                "Compile drilling telemetry into bonus mining knowledge.",
                List.of(
                        ForgeIngredient.custom("ENCHANTED_SAPPHIRE", 4),
                        ForgeIngredient.custom("RIFT_ESSENCE", 4),
                        ForgeIngredient.vanilla(Material.PAPER, 8)
                )
        ),
        STABILITY_ANCHOR_FORGING(
                "stability_anchor_forging",
                ChatColor.DARK_PURPLE + "Stability Anchor Forging",
                "STABILITY_ANCHOR",
                1,
                8L * 60L * 1000L,
                1600.0D,
                7,
                "Bind pressure-resistant plating into a deep-core anchor.",
                List.of(
                        ForgeIngredient.custom("VOID_CRYSTAL", 6),
                        ForgeIngredient.custom("ENCHANTED_TITANIUM", 8),
                        ForgeIngredient.vanilla(Material.IRON_BLOCK, 6)
                )
        ),
        MITHRIL_ENGINE_RESONANCE(
                "mithril_engine_resonance",
                ChatColor.BLUE + "Mithril Engine Resonance",
                "MITHRIL_ENGINE",
                1,
                8L * 60L * 1000L,
                2400.0D,
                8,
                "Tune a starter engine around shard-stabilized drill pulses.",
                List.of(
                        ForgeIngredient.custom("ENCHANTED_SAPPHIRE", 16),
                        ForgeIngredient.custom("ENCHANTED_TITANIUM", 8),
                        ForgeIngredient.vanilla(Material.REDSTONE_BLOCK, 8),
                        ForgeIngredient.vanilla(Material.PISTON, 1)
                )
        ),
        TITANIUM_ENGINE_REFINEMENT(
                "titanium_engine_refinement",
                ChatColor.DARK_PURPLE + "Titanium Engine Refinement",
                "TITANIUM_ENGINE",
                1,
                12L * 60L * 1000L,
                4200.0D,
                10,
                "Refine a stronger core for harsher End and mine pressure.",
                List.of(
                        ForgeIngredient.custom("MITHRIL_ENGINE", 1),
                        ForgeIngredient.custom("ENCHANTED_TITANIUM", 16),
                        ForgeIngredient.custom("VOLTA", 4),
                        ForgeIngredient.vanilla(Material.STICKY_PISTON, 1)
                )
        ),
        GEMSTONE_ENGINE_ATTUNEMENT(
                "gemstone_engine_attunement",
                ChatColor.GOLD + "Gemstone Engine Attunement",
                "GEMSTONE_ENGINE",
                1,
                16L * 60L * 1000L,
                6500.0D,
                12,
                "Attune a gemstone coil for elite drilling throughput.",
                List.of(
                        ForgeIngredient.custom("TITANIUM_ENGINE", 1),
                        ForgeIngredient.custom("KUNZITE", 16),
                        ForgeIngredient.custom("ENCHANTED_SAPPHIRE", 16),
                        ForgeIngredient.custom("VOID_CRYSTAL", 10),
                        ForgeIngredient.vanilla(Material.OBSERVER, 2)
                )
        ),
        DIVAN_ENGINE_ASCENSION(
                "divan_engine_ascension",
                ChatColor.LIGHT_PURPLE + "Divan Engine Ascension",
                "DIVAN_ENGINE",
                1,
                24L * 60L * 1000L,
                10000.0D,
                15,
                "Ascend your forge with a mythic-grade pulse reactor.",
                List.of(
                        ForgeIngredient.custom("GEMSTONE_ENGINE", 1),
                        ForgeIngredient.custom("ENCHANTED_TITANIUM", 32),
                        ForgeIngredient.custom("KUNZITE", 24),
                        ForgeIngredient.custom("VOID_CRYSTAL", 16),
                        ForgeIngredient.custom("CHORUS_WEAVE", 12),
                        ForgeIngredient.vanilla(Material.NETHER_STAR, 1)
                )
        ),
        TITANIUM_DRILL_FORGING(
                "titanium_drill_forging",
                ChatColor.DARK_PURPLE + "Titanium Drill Forging",
                "TITANIUM_DRILL",
                1,
                30L * 60L * 1000L,
                15000.0D,
                20,
                "Forge a heavy-duty drill chassis from refined titanium plates.",
                List.of(
                        ForgeIngredient.custom("IRONCREST_DRILL", 1),
                        ForgeIngredient.custom("ENCHANTED_TITANIUM", 16),
                        ForgeIngredient.custom("VOID_CRYSTAL", 20),
                        ForgeIngredient.custom("STABILITY_ANCHOR", 1)
                )
        ),
        GEMSTONE_DRILL_FORGING(
                "gemstone_drill_forging",
                ChatColor.GOLD + "Gemstone Drill Forging",
                "GEMSTONE_DRILL",
                1,
                45L * 60L * 1000L,
                25000.0D,
                30,
                "Forged with crystalline precision for the ultimate mining experience.",
                List.of(
                        ForgeIngredient.custom("TITANIUM_DRILL", 1),
                        ForgeIngredient.custom("KUNZITE", 32),
                        ForgeIngredient.custom("ENCHANTED_SAPPHIRE", 32),
                        ForgeIngredient.custom("GEMSTONE_ENGINE", 1)
                )
        ),
        MEDIUM_TANK_EXPANSION(
                "medium_tank_expansion",
                ChatColor.BLUE + "Medium Tank Expansion",
                "MEDIUM_FUEL_TANK",
                1,
                10L * 60L * 1000L,
                2000.0D,
                6,
                "Expand a drill tank with reinforced End pressure plating.",
                List.of(
                        ForgeIngredient.custom("ENCHANTED_TITANIUM", 8),
                        ForgeIngredient.custom("VOLTA", 4),
                        ForgeIngredient.vanilla(Material.CAULDRON, 1)
                )
        ),
        LARGE_TANK_REINFORCEMENT(
                "large_tank_reinforcement",
                ChatColor.DARK_PURPLE + "Large Tank Reinforcement",
                "LARGE_FUEL_TANK",
                1,
                18L * 60L * 1000L,
                4200.0D,
                10,
                "Reinforce a huge drill tank for expedition-grade fuel storage.",
                List.of(
                        ForgeIngredient.custom("MEDIUM_FUEL_TANK", 1),
                        ForgeIngredient.custom("VOID_CRYSTAL", 12),
                        ForgeIngredient.custom("KUNZITE", 16),
                        ForgeIngredient.vanilla(Material.CAULDRON, 1)
                )
        ),
        GUARDIAN_HELM_FORGING(
                "guardian_helm_forging",
                ChatColor.WHITE + "Guardian Helm Forging",
                "GUARDIAN_HELM",
                1,
                20L * 60L * 1000L,
                2500.0D,
                8,
                "Bind Guardian Fragments into a protective holy helm.",
                List.of(
                        ForgeIngredient.custom("GUARDIAN_FRAGMENT", 50)
                )
        ),
        GUARDIAN_CHESTPLATE_FORGING(
                "guardian_chestplate_forging",
                ChatColor.WHITE + "Guardian Chestplate Forging",
                "GUARDIAN_CHESTPLATE",
                1,
                45L * 60L * 1000L,
                5000.0D,
                12,
                "The ultimate radiant protection, forged from many soul shards.",
                List.of(
                        ForgeIngredient.custom("GUARDIAN_FRAGMENT", 80)
                )
        ),
        GUARDIAN_LEGGINGS_FORGING(
                "guardian_leggings_forging",
                ChatColor.WHITE + "Guardian Leggings Forging",
                "GUARDIAN_LEGGINGS",
                1,
                35L * 60L * 1000L,
                4000.0D,
                10,
                "Etch divine law into reinforced protective plating.",
                List.of(
                        ForgeIngredient.custom("GUARDIAN_FRAGMENT", 70)
                )
        ),
        GUARDIAN_BOOTS_FORGING(
                "guardian_boots_forging",
                ChatColor.WHITE + "Guardian Boots Forging",
                "GUARDIAN_BOOTS",
                1,
                25L * 60L * 1000L,
                2000.0D,
                7,
                "Hallow the ground with every step using soul-infused threads.",
                List.of(
                        ForgeIngredient.custom("GUARDIAN_FRAGMENT", 40)
                )
        ),
        SPEED_BOOST_BLEND(
                "speed_boost_blend",
                ChatColor.YELLOW + "Speed Boost Blend",
                "TEMP_MINING_SPEED_BOOST",
                2,
                6L * 60L * 1000L,
                1400.0D,
                5,
                "Blend high-response compounds for short burst mining speed.",
                List.of(
                        ForgeIngredient.custom("VOLTA", 1),
                        ForgeIngredient.custom("RIFT_ESSENCE", 5),
                        ForgeIngredient.custom("ENDSTONE_SHARD", 20)
                )
        ),
        HEAT_CATALYST_SYNTHESIS(
                "heat_catalyst_synthesis",
                ChatColor.RED + "Heat Catalyst Synthesis",
                "HEAT_CATALYST",
                1,
                10L * 60L * 1000L,
                1800.0D,
                0,
                "Synthesize a volatile catalyst that grants +20 instant forge heat.",
                List.of(
                        ForgeIngredient.custom("VOLTA", 2),
                        ForgeIngredient.custom("RIFT_ESSENCE", 8),
                        ForgeIngredient.custom("OBSIDIAN_CORE", 4),
                        ForgeIngredient.vanilla(Material.BLAZE_POWDER, 16)
                )
        ),
        FORTUNE_COOKIE_BAKING(
                "fortune_cookie_baking",
                ChatColor.GOLD + "Fortune Cookie Baking",
                "FORTUNE_COOKIE",
                3,
                8L * 60L * 1000L,
                2200.0D,
                4,
                "Bake mystical cookies that grant +50 Mining Fortune for 1 hour.",
                List.of(
                        ForgeIngredient.custom("KUNZITE", 4),
                        ForgeIngredient.custom("RIFT_ESSENCE", 6),
                        ForgeIngredient.vanilla(Material.SUGAR, 16),
                        ForgeIngredient.vanilla(Material.WHEAT, 16)
                )
        ),
        DRILL_WARRANTY_FORGING(
                "drill_warranty_forging",
                ChatColor.AQUA + "Drill Warranty Forging",
                "DRILL_WARRANTY_TICKET",
                1,
                15L * 60L * 1000L,
                5000.0D,
                8,
                "Forge a warranty ticket that prevents drill part breakage on death.",
                List.of(
                        ForgeIngredient.custom("TITANIUM_ENGINE", 1),
                        ForgeIngredient.custom("VOID_CRYSTAL", 8),
                        ForgeIngredient.custom("CHORUS_WEAVE", 6),
                        ForgeIngredient.vanilla(Material.TOTEM_OF_UNDYING, 1)
                )
        ),
        REFINED_TITANIUM_FORGING(
                "refined_titanium_forging",
                ChatColor.AQUA + "Refined Titanium Forging",
                "ENCHANTED_TITANIUM",
                1,
                12L * 60L * 1000L,
                5000.0D,
                15,
                "Forge Titanium into its highly refined, compressed form.",
                List.of(
                        ForgeIngredient.custom("TITANIUM", 16)
                )
        ),
        TITANIUM_BLOCK_FORGING(
                "titanium_block_forging",
                ChatColor.WHITE + "Titanium Block Forging",
                "TITANIUM_BLOCK",
                1,
                24L * 60L * 1000L,
                12000.0D,
                25,
                "Compress Titanium into a solid, high-density industrial block.",
                List.of(
                        ForgeIngredient.custom("TITANIUM", 64),
                        ForgeIngredient.custom("ENCHANTED_TITANIUM", 4)
                )
        ),
        PERSONAL_COMPACTOR_5000_FORGING(
                "personal_compactor_5000_forging",
                ChatColor.DARK_PURPLE + "Personal Compactor 5000",
                "PERSONAL_COMPACTOR_5000",
                1,
                12L * 60L * 60L * 1000L, // 12 hours
                15000.0D,
                20,
                "An elite compaction unit with 5 slots.",
                List.of(
                        ForgeIngredient.custom("TITANIUM", 20),
                        ForgeIngredient.custom("ENCHANTED_COBBLESTONE", 128),
                        ForgeIngredient.custom("ENCHANTED_REDSTONE", 64)
                )
        ),
        PERSONAL_COMPACTOR_6000_FORGING(
                "personal_compactor_6000_forging",
                ChatColor.LIGHT_PURPLE + "Personal Compactor 6000",
                "PERSONAL_COMPACTOR_6000",
                1,
                24L * 60L * 60L * 1000L, // 24 hours
                35000.0D,
                35,
                "A master-grade compaction unit with 9 slots.",
                List.of(
                        ForgeIngredient.custom("TITANIUM", 60),
                        ForgeIngredient.custom("ENCHANTED_COBBLESTONE", 192),
                        ForgeIngredient.custom("ENCHANTED_REDSTONE_BLOCK", 92)
                )
        ),
        PERSONAL_COMPACTOR_7000_FORGING(
                "personal_compactor_7000_forging",
                ChatColor.GOLD + "Personal Compactor 7000",
                "PERSONAL_COMPACTOR_7000",
                1,
                48L * 60L * 60L * 1000L, // 48 hours
                75000.0D,
                50,
                "The ultimate automated sorting solution with 12 slots.",
                List.of(
                        ForgeIngredient.custom("ENCHANTED_TITANIUM", 30),
                        ForgeIngredient.custom("ENCHANTED_COBBLESTONE", 256),
                        ForgeIngredient.custom("ENCHANTED_REDSTONE_BLOCK", 128)
                )
        );

        private final String id;
        private final String displayName;
        private final String outputItemId;
        private final int outputAmount;
        private final long baseDurationMillis;
        private final double coinCost;
        private final int heatGain;
        private final String description;
        private final List<ForgeIngredient> ingredients;

        ForgeProjectType(
                String id,
                String displayName,
                String outputItemId,
                int outputAmount,
                long baseDurationMillis,
                double coinCost,
                int heatGain,
                String description,
                List<ForgeIngredient> ingredients
        ) {
            this.id = id;
            this.displayName = displayName;
            this.outputItemId = outputItemId;
            this.outputAmount = Math.max(1, outputAmount);
            this.baseDurationMillis = Math.max(60_000L, baseDurationMillis);
            this.coinCost = Math.max(0.0D, coinCost);
            this.heatGain = Math.max(1, heatGain);
            this.description = description;
            this.ingredients = List.copyOf(ingredients);
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        public String outputItemId() {
            return outputItemId;
        }

        public int outputAmount() {
            return outputAmount;
        }

        public long baseDurationMillis() {
            return baseDurationMillis;
        }

        public double coinCost() {
            return coinCost;
        }

        public int heatGain() {
            return heatGain;
        }

        public String description() {
            return description;
        }

        public List<ForgeIngredient> ingredients() {
            return ingredients;
        }

        public ItemStack createOutput(CustomItemService itemService) {
            if (itemService == null) {
                return null;
            }
            ItemStack output = itemService.createItemByKey(outputItemId);
            if (output == null) {
                return null;
            }
            output.setAmount(Math.max(1, outputAmount));
            return output;
        }

        public static ForgeProjectType parse(String input) {
            if (input == null || input.isBlank()) {
                return null;
            }
            String normalized = input.trim().toLowerCase(Locale.ROOT);
            for (ForgeProjectType type : values()) {
                if (type.id.equals(normalized)) {
                    return type;
                }
            }
            return null;
        }
    }

    public record ActiveProjectView(ForgeProjectType type, long startedAt, long readyAt, boolean ready) {
    }

    public record ForgeSnapshot(
            int heat,
            int heatTier,
            int speedBonusPercent,
            long overdriveRemainingMillis,
            int totalClaims,
            int activeProjects,
            int readyProjects
    ) {
    }

    private static final class ActiveProject {
        private final String projectId;
        private final long startedAt;
        private final long readyAt;

        private ActiveProject(String projectId, long startedAt, long readyAt) {
            this.projectId = projectId;
            this.startedAt = startedAt;
            this.readyAt = readyAt;
        }
    }

    private static final class ProfileData {
        private int heat;
        private int totalClaims;
        private long overdriveUntil;
        private int overdriveTier;
        private long lastActivityTimestamp;
        private final List<ActiveProject> activeProjects = new ArrayList<>();
    }

    private final GriviencePlugin plugin;
    private final CustomItemService itemService;
    private final ProfileEconomyService economyService;
    private final CollectionsManager collectionsManager;
    private final File dataFile;
    private final Map<UUID, ProfileData> profileData = new HashMap<>();

    public DrillForgeManager(GriviencePlugin plugin, CustomItemService itemService, ProfileEconomyService economyService) {
        this.plugin = plugin;
        this.itemService = itemService;
        this.economyService = economyService;
        this.collectionsManager = plugin == null ? null : plugin.getCollectionsManager();
        this.dataFile = plugin == null ? null : new File(plugin.getDataFolder(), "drill-forge-data.yml");
        load();
    }

    public void shutdown() {
        save();
    }

    public List<ForgeProjectType> catalog() {
        return List.of(ForgeProjectType.values());
    }

    public ForgeSnapshot snapshot(Player player) {
        ProfileData data = getData(player);
        int ready = 0;
        long now = System.currentTimeMillis();
        for (ActiveProject project : sortedProjects(data)) {
            if (project.readyAt <= now) {
                ready++;
            }
        }
        int heatTier = heatTierFor(data.heat);
        return new ForgeSnapshot(
                data.heat,
                heatTier,
                speedBonusPercentFor(data.heat),
                Math.max(0L, data.overdriveUntil - now),
                data.totalClaims,
                data.activeProjects.size(),
                ready
        );
    }

    public List<ActiveProjectView> activeProjects(Player player) {
        ProfileData data = getData(player);
        long now = System.currentTimeMillis();
        List<ActiveProjectView> views = new ArrayList<>();
        for (ActiveProject project : sortedProjects(data)) {
            ForgeProjectType type = ForgeProjectType.parse(project.projectId);
            if (type != null) {
                views.add(new ActiveProjectView(type, project.startedAt, project.readyAt, project.readyAt <= now));
            }
        }
        return views;
    }

    public long projectedDurationMillis(Player player, ForgeProjectType type) {
        if (type == null) {
            return 60_000L;
        }
        return adjustedDurationMillis(type.baseDurationMillis(), snapshot(player).heat());
    }

    public StartResult startProject(Player player, ForgeProjectType type) {
        if (player == null || type == null) {
            return StartResult.INVALID;
        }
        ProfileData data = getData(player);
        if (data.activeProjects.size() >= MAX_QUEUE_SIZE) {
            player.sendMessage(ChatColor.RED + "Your Drill Forge queue is full.");
            return StartResult.QUEUE_FULL;
        }
        if (!hasIngredients(player, type.ingredients())) {
            player.sendMessage(ChatColor.RED + "You are missing materials for " + type.displayName() + ChatColor.RED + ".");
            return StartResult.NEEDS_ITEMS;
        }
        if (!economyService.has(player, type.coinCost())) {
            player.sendMessage(ChatColor.RED + "You need " + ChatColor.GOLD + formatCoins(type.coinCost()) + ChatColor.RED + " coins.");
            return StartResult.NEEDS_COINS;
        }
        if (!economyService.withdraw(player, type.coinCost())) {
            player.sendMessage(ChatColor.RED + "Failed to withdraw forge coins. Try again.");
            return StartResult.NEEDS_COINS;
        }

        removeIngredients(player, type.ingredients());
        long startedAt = System.currentTimeMillis();
        long adjustedDuration = adjustedDurationMillis(type.baseDurationMillis(), data.heat);
        long readyAt = startedAt + adjustedDuration;
        int speedBonus = speedBonusPercentFor(data.heat);
        data.activeProjects.add(new ActiveProject(type.id(), startedAt, readyAt));
        save();

        player.playSound(player.getLocation(), Sound.BLOCK_BLASTFURNACE_FIRE_CRACKLE, 0.9F, 1.2F);
        player.sendMessage(ChatColor.GREEN + "Started forge project: " + type.displayName());
        player.sendMessage(ChatColor.GRAY + "Ready in " + ChatColor.AQUA + formatDuration(adjustedDuration)
                + ChatColor.GRAY + ". Queue: " + ChatColor.YELLOW + data.activeProjects.size() + ChatColor.GRAY + "/" + ChatColor.YELLOW + MAX_QUEUE_SIZE);
        player.sendMessage(ChatColor.GRAY + "Current Forge Speed: " + ChatColor.GREEN + "+" + speedBonus + "%");
        return StartResult.STARTED;
    }

    public ClaimResult claimProject(Player player, int displayIndex) {
        if (player == null) {
            return ClaimResult.EMPTY;
        }
        ProfileData data = getData(player);
        List<ActiveProject> sorted = sortedProjects(data);
        if (displayIndex < 0 || displayIndex >= sorted.size()) {
            player.sendMessage(ChatColor.RED + "There is no forge project in that slot.");
            return ClaimResult.EMPTY;
        }

        ActiveProject active = sorted.get(displayIndex);
        long now = System.currentTimeMillis();
        if (active.readyAt > now) {
            player.sendMessage(ChatColor.RED + "That project is still forging. Ready in " + ChatColor.YELLOW + formatDuration(active.readyAt - now) + ChatColor.RED + ".");
            return ClaimResult.NOT_READY;
        }

        ForgeProjectType type = ForgeProjectType.parse(active.projectId);
        if (type == null) {
            data.activeProjects.remove(active);
            save();
            player.sendMessage(ChatColor.RED + "That forge project was invalid and has been cleared.");
            return ClaimResult.EMPTY;
        }

        ItemStack output = type.createOutput(itemService);
        if (output == null || output.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "Failed to create forge output.");
            return ClaimResult.EMPTY;
        }

        int previousHeat = data.heat;
        data.activeProjects.remove(active);
        data.totalClaims += 1;
        data.heat = Math.min(MAX_HEAT, previousHeat + type.heatGain());
        data.lastActivityTimestamp = System.currentTimeMillis();
        save();

        DropDeliveryUtil.giveToInventoryOrDrop(player, output, player.getLocation());
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0F, 1.3F);
        player.sendMessage(ChatColor.GREEN + "Claimed " + type.displayName() + ChatColor.GREEN + ".");
        player.sendMessage(ChatColor.GRAY + "Forge Heat: " + ChatColor.GOLD + previousHeat + ChatColor.DARK_GRAY + " -> "
                + ChatColor.GOLD + data.heat + ChatColor.GRAY + "/" + ChatColor.GOLD + MAX_HEAT);
        player.sendMessage(ChatColor.GRAY + "Forge Speed: " + ChatColor.GREEN + "+" + speedBonusPercentFor(previousHeat) + "%"
                + ChatColor.DARK_GRAY + " -> " + ChatColor.GREEN + "+" + speedBonusPercentFor(data.heat) + "%");
        int nextMilestone = nextHeatMilestone(data.heat);
        if (nextMilestone > 0) {
            if (data.heat < OVERDRIVE_HEAT_COST) {
                player.sendMessage(ChatColor.DARK_GRAY + "Need " + ChatColor.YELLOW + (OVERDRIVE_HEAT_COST - data.heat)
                        + ChatColor.DARK_GRAY + " more heat to activate Overdrive.");
            } else {
                player.sendMessage(ChatColor.DARK_GRAY + "Next Overdrive upgrade at " + ChatColor.YELLOW + nextMilestone
                        + ChatColor.DARK_GRAY + " heat (" + ChatColor.YELLOW + (nextMilestone - data.heat) + ChatColor.DARK_GRAY + " more).");
            }
        } else {
            player.sendMessage(ChatColor.AQUA + "Forge Heat is maxed.");
        }
        return ClaimResult.CLAIMED;
    }

    public OverdriveResult activateOverdrive(Player player) {
        if (player == null) {
            return OverdriveResult.NEEDS_HEAT;
        }
        ProfileData data = getData(player);
        long remaining = Math.max(0L, data.overdriveUntil - System.currentTimeMillis());
        if (remaining > 0L) {
            player.sendMessage(ChatColor.RED + "Forge Overdrive is already active for " + ChatColor.YELLOW + formatDuration(remaining) + ChatColor.RED + ".");
            return OverdriveResult.ALREADY_ACTIVE;
        }
        if (data.heat < OVERDRIVE_HEAT_COST) {
            player.sendMessage(ChatColor.RED + "You need " + ChatColor.GOLD + OVERDRIVE_HEAT_COST + ChatColor.RED + " Forge Heat to ignite Overdrive.");
            return OverdriveResult.NEEDS_HEAT;
        }
        if (!economyService.has(player, OVERDRIVE_COIN_COST)) {
            player.sendMessage(ChatColor.RED + "You need " + ChatColor.GOLD + formatCoins(OVERDRIVE_COIN_COST) + ChatColor.RED + " coins.");
            return OverdriveResult.NEEDS_COINS;
        }
        if (!economyService.withdraw(player, OVERDRIVE_COIN_COST)) {
            player.sendMessage(ChatColor.RED + "Failed to withdraw Overdrive coins. Try again.");
            return OverdriveResult.NEEDS_COINS;
        }

        int ignitionHeat = data.heat;
        int ignitionTier = overdriveTierForHeat(ignitionHeat);
        long duration = overdriveDurationMillisForHeat(ignitionHeat);
        data.heat = Math.max(0, ignitionHeat - OVERDRIVE_HEAT_COST);
        data.overdriveTier = ignitionTier;
        data.overdriveUntil = System.currentTimeMillis() + duration;
        save();

        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 0.8F, 1.5F);
        player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "⚡ FORGE OVERDRIVE ONLINE ⚡");
        player.sendMessage(ChatColor.GRAY + "Overdrive Level: " + overdriveTierColor(ignitionTier) + overdriveTierName(ignitionTier)
                + ChatColor.GRAY + " | Duration: " + ChatColor.YELLOW + formatDuration(duration));
        player.sendMessage(ChatColor.GRAY + "Mining Fortune: " + ChatColor.GOLD + "+" + overdriveMiningFortuneBonus(ignitionTier)
                + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "Mining Speed: " + ChatColor.GREEN + "+" + overdriveMiningSpeedBonus(ignitionTier) + "%");
        player.sendMessage(ChatColor.GRAY + "Fuel Burn: " + ChatColor.GREEN + "-" + overdriveFuelReduction(ignitionTier) + ChatColor.GRAY + " per block"
                + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "Cooldown: " + ChatColor.GREEN + "-" + (overdriveCooldownReductionMillis(ignitionTier) / 1000L) + "s");
        if (overdriveRareFindBonus(ignitionTier) > 0) {
            player.sendMessage(ChatColor.GRAY + "Rare Find: " + ChatColor.LIGHT_PURPLE + "+" + overdriveRareFindBonus(ignitionTier) + "%");
        }
        player.sendMessage(ChatColor.GRAY + "Remaining Heat: " + ChatColor.GOLD + data.heat + ChatColor.GRAY + "/" + ChatColor.GOLD + MAX_HEAT);
        return OverdriveResult.ACTIVATED;
    }

    public boolean isOverdriveActive(Player player) {
        return overdriveRemainingMillis(player) > 0L;
    }

    public int activeOverdriveTier(Player player) {
        if (!isOverdriveActive(player)) {
            return -1;
        }
        return clampOverdriveTier(getData(player).overdriveTier);
    }

    public long overdriveRemainingMillis(Player player) {
        return Math.max(0L, getData(player).overdriveUntil - System.currentTimeMillis());
    }

    public long adjustedAbilityCooldownMillis(Player player, long baseCooldownMillis) {
        int tier = activeOverdriveTier(player);
        if (tier < 0) {
            return baseCooldownMillis;
        }
        return Math.max(12_000L, baseCooldownMillis - overdriveCooldownReductionMillis(tier));
    }

    public int adjustedAbilityDurationTicks(Player player, int baseDurationTicks) {
        int tier = activeOverdriveTier(player);
        if (tier < 0) {
            return baseDurationTicks;
        }
        return baseDurationTicks + overdriveAbilityDurationBonusTicks(tier);
    }

    public int adjustedAbilityAmplifier(Player player, int baseAmplifier) {
        int tier = activeOverdriveTier(player);
        if (tier < 0) {
            return baseAmplifier;
        }
        return baseAmplifier + overdriveAbilityAmplifierBonus(tier);
    }

    public int adjustedFuelCostPerBlock(Player player, int baseFuelCost) {
        int tier = activeOverdriveTier(player);
        return adjustedFuelCostPerBlock(baseFuelCost, tier, tier >= 0);
    }

    public int activeOverdriveMiningFortuneBonus(Player player) {
        int tier = activeOverdriveTier(player);
        return tier >= 0 ? overdriveMiningFortuneBonus(tier) : 0;
    }

    public int activeOverdriveMiningSpeedBonus(Player player) {
        int tier = activeOverdriveTier(player);
        return tier >= 0 ? overdriveMiningSpeedBonus(tier) : 0;
    }

    public int activeOverdriveRareFindBonus(Player player) {
        int tier = activeOverdriveTier(player);
        return tier >= 0 ? overdriveRareFindBonus(tier) : 0;
    }

    public String overdriveActionBarSuffix(Player player) {
        long remaining = overdriveRemainingMillis(player);
        if (remaining <= 0L) {
            return "";
        }
        return ChatColor.DARK_GRAY + " | " + ChatColor.AQUA + "OVERDRIVE " + ChatColor.WHITE + formatDuration(remaining);
    }

    public static int heatTierFor(int heat) {
        return Math.max(0, Math.min(4, Math.max(0, heat) / 20));
    }

    public static int speedBonusPercentFor(int heat) {
        int clampedHeat = Math.max(0, Math.min(MAX_HEAT, heat));
        return Math.max(0, Math.min(40, (int) Math.round(clampedHeat * 0.4D)));
    }

    public static String heatTierName(int heat) {
        return switch (heatTierFor(heat)) {
            case 0 -> "Cold";
            case 1 -> "Warming";
            case 2 -> "Hot";
            case 3 -> "Blazing";
            default -> "White-Hot";
        };
    }

    public static ChatColor heatTierColor(int heat) {
        return switch (heatTierFor(heat)) {
            case 0 -> ChatColor.GRAY;
            case 1 -> ChatColor.YELLOW;
            case 2 -> ChatColor.GOLD;
            case 3 -> ChatColor.RED;
            default -> ChatColor.WHITE;
        };
    }

    public static int nextHeatMilestone(int heat) {
        int clampedHeat = Math.max(0, Math.min(MAX_HEAT, heat));
        if (clampedHeat < 20) {
            return 20;
        }
        if (clampedHeat < 40) {
            return 40;
        }
        if (clampedHeat < 60) {
            return 60;
        }
        if (clampedHeat < 80) {
            return 80;
        }
        if (clampedHeat < MAX_HEAT) {
            return MAX_HEAT;
        }
        return -1;
    }

    public static String nextHeatMilestoneName(int heat) {
        int nextMilestone = nextHeatMilestone(heat);
        return switch (nextMilestone) {
            case 20 -> "Overdrive Ready";
            case 40 -> "Overdrive Level 2";
            case 60 -> "Overdrive Level 3";
            case 80 -> "Overdrive Level 4";
            case MAX_HEAT -> "Overdrive Level 5";
            default -> "Complete";
        };
    }

    public static int overdriveTierForHeat(int heat) {
        int clampedHeat = Math.max(0, Math.min(MAX_HEAT, heat));
        if (clampedHeat >= MAX_HEAT) {
            return 4;
        }
        if (clampedHeat >= 80) {
            return 3;
        }
        if (clampedHeat >= 60) {
            return 2;
        }
        if (clampedHeat >= 40) {
            return 1;
        }
        return 0;
    }

    public static String overdriveTierName(int tier) {
        return "Level " + (clampOverdriveTier(tier) + 1);
    }

    public static ChatColor overdriveTierColor(int tier) {
        return switch (clampOverdriveTier(tier)) {
            case 0 -> ChatColor.YELLOW;
            case 1 -> ChatColor.GOLD;
            case 2 -> ChatColor.RED;
            case 3 -> ChatColor.DARK_RED;
            default -> ChatColor.LIGHT_PURPLE;
        };
    }

    public static long overdriveDurationMillisForHeat(int heat) {
        return OVERDRIVE_BASE_DURATION_MILLIS + (clampOverdriveTier(overdriveTierForHeat(heat)) * 90_000L);
    }

    public static int overdriveFuelReduction(int tier) {
        return 3 + clampOverdriveTier(tier);
    }

    public static long overdriveCooldownReductionMillis(int tier) {
        return 6_000L + (clampOverdriveTier(tier) * 2_000L);
    }

    public static int overdriveAbilityDurationBonusTicks(int tier) {
        return 40 + (clampOverdriveTier(tier) * 20);
    }

    public static int overdriveAbilityAmplifierBonus(int tier) {
        return clampOverdriveTier(tier) >= 1 ? 1 : 0;
    }

    public static int overdriveMiningFortuneBonus(int tier) {
        int clampedTier = clampOverdriveTier(tier);
        return 5 + (clampedTier * 5);  // Level 1: +10, Level 2: +15, Level 3: +20, Level 4: +25, Level 5: +30
    }

    public static int overdriveMiningSpeedBonus(int tier) {
        int clampedTier = clampOverdriveTier(tier);
        return 10 + (clampedTier * 2);  // Level 1: +12%, Level 2: +14%, Level 3: +16%, Level 4: +18%, Level 5: +20%
    }

    public static int overdriveRareFindBonus(int tier) {
        return clampOverdriveTier(tier) >= 4 ? 10 : 0;  // +10% at max tier only
    }

    public static long adjustedDurationMillis(long baseDurationMillis, int heat) {
        double multiplier = 1.0D - (speedBonusPercentFor(heat) / 100.0D);
        return Math.max(60_000L, Math.round(Math.max(60_000L, baseDurationMillis) * multiplier));
    }

    public static int adjustedFuelCostPerBlock(int baseFuelCost, boolean overdriveActive) {
        return adjustedFuelCostPerBlock(baseFuelCost, overdriveActive ? 1 : 0, overdriveActive);
    }

    public static int adjustedFuelCostPerBlock(int baseFuelCost, int overdriveTier, boolean overdriveActive) {
        int base = Math.max(1, baseFuelCost);
        if (!overdriveActive) {
            return base;
        }
        return Math.max(4, base - overdriveFuelReduction(overdriveTier));
    }

    private static int clampOverdriveTier(int tier) {
        return Math.max(0, Math.min(4, tier));
    }

    private ProfileData getData(Player player) {
        UUID profileId = resolveProfileId(player);
        ProfileData data = profileData.computeIfAbsent(profileId, ignored -> {
            ProfileData newData = new ProfileData();
            newData.lastActivityTimestamp = System.currentTimeMillis();
            return newData;
        });
        applyHeatDecay(data);
        return data;
    }

    private void applyHeatDecay(ProfileData data) {
        if (data == null || data.heat <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        if (data.lastActivityTimestamp <= 0L) {
            data.lastActivityTimestamp = now;
            return;
        }
        long elapsed = now - data.lastActivityTimestamp;
        int decayPeriods = (int) (elapsed / HEAT_DECAY_INTERVAL_MILLIS);
        if (decayPeriods > 0) {
            int totalDecay = decayPeriods * HEAT_DECAY_AMOUNT;
            int previousHeat = data.heat;
            data.heat = Math.max(0, data.heat - totalDecay);
            data.lastActivityTimestamp = now;
            if (previousHeat != data.heat) {
                save();
            }
        }
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

    private List<ActiveProject> sortedProjects(ProfileData data) {
        List<ActiveProject> sorted = new ArrayList<>();
        if (data != null) {
            for (ActiveProject project : data.activeProjects) {
                if (ForgeProjectType.parse(project.projectId) != null) {
                    sorted.add(project);
                }
            }
        }
        sorted.sort(Comparator.comparingLong(project -> project.readyAt));
        return sorted;
    }

    private boolean hasIngredients(Player player, List<ForgeIngredient> ingredients) {
        if (player == null || ingredients == null) {
            return false;
        }
        for (ForgeIngredient ingredient : ingredients) {
            if (countIngredient(player, ingredient) < ingredient.amount()) {
                return false;
            }
        }
        return true;
    }

    private int countIngredient(Player player, ForgeIngredient ingredient) {
        if (player == null || ingredient == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            if (ingredient.customItemId() != null) {
                String itemId = itemService == null ? null : itemService.itemId(stack);
                if (ingredient.customItemId().equalsIgnoreCase(itemId)) {
                    total += Math.max(1, stack.getAmount());
                }
                continue;
            }
            if (ingredient.material() == stack.getType() && (itemService == null || itemService.itemId(stack) == null)) {
                total += Math.max(1, stack.getAmount());
            }
        }
        return total;
    }

    private void removeIngredients(Player player, List<ForgeIngredient> ingredients) {
        if (player == null || ingredients == null) {
            return;
        }
        for (ForgeIngredient ingredient : ingredients) {
            int remaining = ingredient.amount();
            for (int slot = 0; slot < player.getInventory().getSize() && remaining > 0; slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack == null || stack.getType().isAir()) {
                    continue;
                }
                if (!matchesIngredient(stack, ingredient)) {
                    continue;
                }
                int taken = Math.min(stack.getAmount(), remaining);
                int newAmount = stack.getAmount() - taken;
                remaining -= taken;
                if (newAmount <= 0) {
                    player.getInventory().setItem(slot, null);
                } else {
                    stack.setAmount(newAmount);
                }
            }
        }
    }

    private boolean matchesIngredient(ItemStack stack, ForgeIngredient ingredient) {
        if (stack == null || ingredient == null) {
            return false;
        }
        if (ingredient.customItemId() != null) {
            return ingredient.customItemId().equalsIgnoreCase(itemService == null ? null : itemService.itemId(stack));
        }
        return ingredient.material() == stack.getType() && (itemService == null || itemService.itemId(stack) == null);
    }

    private void load() {
        profileData.clear();
        if (dataFile == null || !dataFile.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection profiles = yaml.getConfigurationSection("profiles");
        if (profiles == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (String key : profiles.getKeys(false)) {
            UUID profileId;
            try {
                profileId = UUID.fromString(key);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            ConfigurationSection section = profiles.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            ProfileData data = new ProfileData();
            data.heat = Math.max(0, Math.min(MAX_HEAT, section.getInt("heat", 0)));
            data.totalClaims = Math.max(0, section.getInt("total-claims", 0));
            data.overdriveUntil = Math.max(0L, section.getLong("overdrive-until", 0L));
            data.overdriveTier = clampOverdriveTier(section.getInt("overdrive-tier", data.overdriveUntil > now ? 1 : 0));
            data.lastActivityTimestamp = Math.max(0L, section.getLong("last-activity", now));
            ConfigurationSection queueSection = section.getConfigurationSection("queue");
            if (queueSection != null) {
                for (String queueKey : queueSection.getKeys(false)) {
                    ConfigurationSection entry = queueSection.getConfigurationSection(queueKey);
                    if (entry == null) {
                        continue;
                    }
                    String projectId = entry.getString("project-id", "");
                    ForgeProjectType type = ForgeProjectType.parse(projectId);
                    if (type == null) {
                        continue;
                    }
                    long startedAt = Math.max(0L, entry.getLong("started-at", 0L));
                    long readyAt = Math.max(startedAt, entry.getLong("ready-at", startedAt));
                    data.activeProjects.add(new ActiveProject(type.id(), startedAt, readyAt));
                }
            }
            profileData.put(profileId, data);
        }
    }

    private void save() {
        if (dataFile == null) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        ConfigurationSection profiles = yaml.createSection("profiles");
        for (Map.Entry<UUID, ProfileData> entry : profileData.entrySet()) {
            ConfigurationSection section = profiles.createSection(entry.getKey().toString());
            ProfileData data = entry.getValue();
            section.set("heat", Math.max(0, Math.min(MAX_HEAT, data.heat)));
            section.set("total-claims", Math.max(0, data.totalClaims));
            section.set("overdrive-until", Math.max(0L, data.overdriveUntil));
            section.set("overdrive-tier", clampOverdriveTier(data.overdriveTier));
            section.set("last-activity", Math.max(0L, data.lastActivityTimestamp));
            ConfigurationSection queue = section.createSection("queue");
            int index = 0;
            for (ActiveProject project : data.activeProjects) {
                ForgeProjectType type = ForgeProjectType.parse(project.projectId);
                if (type == null) {
                    continue;
                }
                ConfigurationSection projectSection = queue.createSection(String.valueOf(index++));
                projectSection.set("project-id", type.id());
                projectSection.set("started-at", project.startedAt);
                projectSection.set("ready-at", project.readyAt);
            }
        }
        File parent = dataFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try {
            yaml.save(dataFile);
        } catch (IOException e) {
            if (plugin != null) {
                plugin.getLogger().warning("Failed to save Drill Forge data: " + e.getMessage());
            }
        }
    }

    public String formatDuration(long millis) {
        long seconds = Math.max(0L, millis / 1000L);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long leftover = seconds % 60L;
        if (hours > 0L) {
            return hours + "h " + minutes + "m " + leftover + "s";
        }
        if (minutes > 0L) {
            return minutes + "m " + leftover + "s";
        }
        return leftover + "s";
    }

    public String formatCoins(double value) {
        return String.format(Locale.US, "%,.0f", Math.max(0.0D, value));
    }

    public String formatInt(int value) {
        return String.format(Locale.US, "%,d", value);
    }
}
