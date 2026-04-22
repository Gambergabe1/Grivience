package io.papermc.Grivience.dragon;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.performance.ServerPerformanceMonitor;
import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EnderDragonPart;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import io.papermc.Grivience.enchantment.EnchantmentRegistry;
import io.papermc.Grivience.enchantment.SkyblockEnchantment;
import io.papermc.Grivience.enchantment.SkyblockEnchantStorage;

import static io.papermc.Grivience.dragon.DragonAscensionCombatModel.ContributionSnapshot;
import static io.papermc.Grivience.dragon.DragonAscensionCombatModel.FightTotals;
import static io.papermc.Grivience.dragon.DragonAscensionCombatModel.RewardTier;
import static io.papermc.Grivience.dragon.DragonAscensionCombatModel.ScoreWeights;
import static io.papermc.Grivience.dragon.DragonAscensionCombatModel.Thresholds;
import static io.papermc.Grivience.dragon.DragonAscensionCombatModel.computeScore;
import static io.papermc.Grivience.dragon.DragonAscensionCombatModel.damageBonusMultiplier;
import static io.papermc.Grivience.dragon.DragonAscensionCombatModel.hasMeaningfulParticipation;
import static io.papermc.Grivience.dragon.DragonAscensionCombatModel.lootChanceMultiplier;
import static io.papermc.Grivience.dragon.DragonAscensionCombatModel.qualifiesForRewards;
import static io.papermc.Grivience.dragon.DragonAscensionCombatModel.resistanceMultiplier;
import static io.papermc.Grivience.dragon.DragonAscensionCombatModel.rewardTier;

public final class DragonAscensionManager implements Listener, CommandExecutor, TabCompleter {
    private static final String MAIN_TITLE = ChatColor.DARK_PURPLE + "Dragon Watcher";
    private static final String CODEX_TITLE = ChatColor.DARK_AQUA + "Dragon Codex";
    private static final String REWARDS_TITLE = ChatColor.GOLD + "Ascension Rewards";
    private static final String SUMMARY_TITLE = ChatColor.LIGHT_PURPLE + "Ascension Summary";
    private static final String SUMMONING_EYE_KEY = "SUMMONING_EYE";
    private static final double DRAGONS_SPINE_DROP_CHANCE = 1.0D / 650.0D;
    private static final double DRAGON_SLAYER_ARMOR_DROP_CHANCE = 1.0D / 580000.0D;
    private static final double DRAGON_HEART_DROP_CHANCE = 1.0D / 10000.0D;
    private static final double DRAGON_TRACKER_ENCHANT_DROP_CHANCE = 1.0D / 2500.0D;
    private static final int SUMMARY_OPEN_RETRIES = 6;
    private static final long SUMMARY_OPEN_RETRY_DELAY_TICKS = 4L;
    private static final List<String> DRAGON_SLAYER_ARMOR_KEYS = List.of(
            "DRAGON_SLAYER_HELMET",
            "DRAGON_SLAYER_CHESTPLATE",
            "DRAGON_SLAYER_LEGGINGS",
            "DRAGON_SLAYER_BOOTS"
    );
    private static final List<String> COMMAND_SUGGESTIONS = List.of("status", "codex", "rewards", "summary", "forcesummon", "setaltar", "setpoint");
    private static final List<PedestalAspect> DEFAULT_PEDESTALS = List.of(
            PedestalAspect.YOUNG,
            PedestalAspect.STRONG,
            PedestalAspect.WISE,
            PedestalAspect.PROTECTOR,
            PedestalAspect.UNSTABLE,
            PedestalAspect.SUPERIOR,
            PedestalAspect.CHAOS,
            PedestalAspect.ANCIENT
    );

    private final GriviencePlugin plugin;
    private final CustomItemService customItemService;
    private final File dataFile;
    private final Map<String, ProgressionRecord> progressionByScope = new HashMap<>();
    private final Map<UUID, FightSummary> lastSummaryByPlayer = new HashMap<>();
    private final Map<UUID, GuiView> openViews = new HashMap<>();
    private final Map<Integer, Location> altarPoints = new HashMap<>();
    private final Map<Integer, UUID> altarPointHologramIds = new HashMap<>();
    private final Map<Integer, UUID> altarPointEyeDisplayIds = new HashMap<>();

    private YamlConfiguration dataConfig;
    private BukkitTask tickTask;
    private BossBar summoningBossBar;
    private BossBar fightBossBar;
    private SummoningState summoning;
    private FightSession fight;
    private boolean flashParticleAvailable = true;

    private boolean enabled;
    private String configuredWorldName;
    private String altarWorldName;
    private double altarX;
    private double altarY;
    private double altarZ;
    private float altarYaw;
    private float altarPitch;
    private double pedestalRadius;
    private int requiredEyes;
    private int dragonTypeLockEyes;
    private String summonItemKey;
    private Material summonMaterial;
    private long summoningTimeoutMillis;
    private double baseMutationChance;
    private double tierFourSpawnChance;
    private double tierFiveSpawnChance;
    private int tickPeriodTicks;
    private double arenaRadius;
    private long maxFightDurationTicks;
    private double baseDragonHealth;
    private double healthPerTier;
    private double baseShield;
    private double shieldPerTier;
    private int crystalCount;
    private double ascensionHealthPercent;
    private double baseDamageMultiplier;
    private double damagePerTier;
    private int minimumRewardActiveSeconds;
    private double minimumRewardScore;
    private boolean summaryAutoOpen;
    private int reputationPerKill;
    private int maxReputation;
    private double rareArmorChance;
    private double voidwingRelicBaseChance;
    private ScoreWeights scoreWeights;
    private Thresholds scoreThresholds;
    private List<PedestalAspect> pedestalAspects = new ArrayList<>(DEFAULT_PEDESTALS);

    public DragonAscensionManager(GriviencePlugin plugin, CustomItemService customItemService) {
        this.plugin = plugin;
        this.customItemService = customItemService;
        this.dataFile = new File(plugin.getDataFolder(), "dragon-ascension-data.yml");
        loadProgression();
        loadConfig();
        refreshAltarPointHolograms();
        startTicker();
    }

    public void reload() {
        saveProgression();
        stopTicker();
        clearSummoning(false);
        clearFight(false);
        removeAltarPointEyeDisplays();
        removeAltarPointHolograms();
        loadConfig();
        loadProgression();
        refreshAltarPointHolograms();
        refreshAltarPointEyeDisplays();
        startTicker();
    }

    public void shutdown() {
        stopTicker();
        clearSummoning(false);
        clearFight(false);
        removeAltarPointEyeDisplays();
        removeAltarPointHolograms();
        saveProgression();
    }

    public void openMain(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 45, MAIN_TITLE);
        fillBorders(inventory, Material.BLACK_STAINED_GLASS_PANE);

        EncounterProfile predictedProfile = currentEncounterProfile();
        ProgressionRecord progression = progressionFor(player);

        inventory.setItem(11, infoItem(
                Material.DRAGON_HEAD,
                ChatColor.LIGHT_PURPLE + "End Status",
                List.of(
                        ChatColor.GRAY + "Current state: " + statusLabel(),
                        ChatColor.GRAY + "World: " + worldLabel(),
                        ChatColor.GRAY + "Altar: " + altarSummary(),
                        ChatColor.GRAY + "Points set: " + ChatColor.WHITE + configuredPointCount() + ChatColor.GRAY + "/" + requiredEyes
                )
        ));
        inventory.setItem(13, infoItem(
                Material.END_CRYSTAL,
                ChatColor.AQUA + "Influence Meter",
                List.of(
                        ChatColor.GRAY + "Eyes placed: " + ChatColor.WHITE + placedEyeCount() + ChatColor.GRAY + "/" + requiredEyes,
                        ChatColor.GRAY + "Required item: " + ChatColor.LIGHT_PURPLE + summonItemKey,
                        ChatColor.GRAY + "Predicted dragon: " + predictedProfile.dragonType().displayName(),
                        ChatColor.GRAY + "Type lock: " + dragonTypeLockStatus(),
                        ChatColor.GRAY + tierStatusLabel(predictedProfile),
                        ChatColor.GRAY + "Mutation chance: " + ChatColor.RED + formatPercent(predictedProfile.mutationChance())
                )
        ));
        inventory.setItem(15, infoItem(
                Material.NETHER_STAR,
                ChatColor.GOLD + "Your Progress",
                List.of(
                        ChatColor.GRAY + "Reputation: " + ChatColor.GREEN + progression.reputation,
                        ChatColor.GRAY + "Kills: " + ChatColor.WHITE + progression.kills,
                        ChatColor.GRAY + "Best score: " + ChatColor.AQUA + formatScore(progression.bestScore),
                        ChatColor.GRAY + "Discovered dragons: " + ChatColor.WHITE + progression.discoveredTypes.size()
                )
        ));
        inventory.setItem(29, navigationItem(Material.BOOK, ChatColor.AQUA + "Dragon Codex", List.of(ChatColor.GRAY + "Abilities, mutations, and records")));
        inventory.setItem(31, navigationItem(Material.CHEST, ChatColor.GOLD + "View Rewards", List.of(ChatColor.GRAY + "Personal reward tiers and loot")));
        inventory.setItem(33, navigationItem(Material.PAPER, ChatColor.LIGHT_PURPLE + "Last Fight Summary", List.of(ChatColor.GRAY + "Contribution breakdown and last rewards")));

        openViews.put(player.getUniqueId(), GuiView.MAIN);
        player.openInventory(inventory);
    }

    public void openCodex(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, CODEX_TITLE);
        fillBorders(inventory, Material.CYAN_STAINED_GLASS_PANE);

        ProgressionRecord progression = progressionFor(player);
        int slot = 10;
        for (DragonType type : DragonType.values()) {
            boolean discovered = progression.discoveredTypes.contains(type);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "State: " + (discovered ? ChatColor.GREEN + "Discovered" : ChatColor.RED + "Undiscovered"));
            lore.add(ChatColor.GRAY + "Weakness: " + ChatColor.WHITE + type.weakness);
            lore.add(ChatColor.GRAY + "Drops: " + ChatColor.WHITE + rewardDisplayName(type.rewardKey));
            lore.add(ChatColor.GRAY + "Best score: " + ChatColor.AQUA + formatScore(progression.bestScores.getOrDefault(type, 0.0D)));
            inventory.setItem(slot++, infoItem(type.icon, type.displayName(), lore));
            if (slot % 9 == 8) {
                slot += 2;
            }
        }

        int mutationSlot = 37;
        for (MutationType mutation : MutationType.values()) {
            if (mutation == MutationType.NONE) {
                continue;
            }
            boolean discovered = progression.discoveredMutations.contains(mutation);
            inventory.setItem(mutationSlot++, infoItem(
                    mutation.icon,
                    mutation.displayName(),
                    List.of(
                            ChatColor.GRAY + "State: " + (discovered ? ChatColor.GREEN + "Discovered" : ChatColor.RED + "Undiscovered"),
                            ChatColor.GRAY + mutation.description,
                            ChatColor.GRAY + "Bonus drop: " + ChatColor.WHITE + rewardDisplayName(mutation.rewardKey)
                    )
            ));
        }

        inventory.setItem(49, navigationItem(Material.ARROW, ChatColor.YELLOW + "Back", List.of(ChatColor.GRAY + "Return to the watcher")));
        openViews.put(player.getUniqueId(), GuiView.CODEX);
        player.openInventory(inventory);
    }

    public void openRewards(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 45, REWARDS_TITLE);
        fillBorders(inventory, Material.ORANGE_STAINED_GLASS_PANE);

        inventory.setItem(10, rewardPreviewItem(RewardTier.S, Material.NETHERITE_INGOT, scoreThresholds.sTier(), 8, 5, true));
        inventory.setItem(12, rewardPreviewItem(RewardTier.A, Material.DIAMOND, scoreThresholds.aTier(), 6, 4, true));
        inventory.setItem(14, rewardPreviewItem(RewardTier.B, Material.EMERALD, scoreThresholds.bTier(), 4, 3, false));
        inventory.setItem(16, rewardPreviewItem(RewardTier.C, Material.IRON_INGOT, scoreThresholds.cTier(), 2, 2, false));
        inventory.setItem(22, infoItem(
                Material.END_CRYSTAL,
                ChatColor.LIGHT_PURPLE + "Scoring Weights",
                List.of(
                        ChatColor.GRAY + "Damage share: " + ChatColor.WHITE + scoreWeights.maxDamageSharePoints(),
                        ChatColor.GRAY + "Crystal objectives: " + ChatColor.WHITE + scoreWeights.crystalPoints(),
                        ChatColor.GRAY + "Mechanics: " + ChatColor.WHITE + scoreWeights.mechanicPointWeight(),
                        ChatColor.GRAY + "Summoning eyes: " + ChatColor.WHITE + scoreWeights.eyePoints(),
                        ChatColor.GRAY + "Active presence: " + ChatColor.WHITE + scoreWeights.presencePointPerSecond() + "/s"
                )
        ));
        inventory.setItem(31, infoItem(
                Material.DRAGON_HEAD,
                ChatColor.GOLD + "Rare Drops & Mechanics",
                List.of(
                        ChatColor.GRAY + "Dragon Slayer armor is part of the pool.",
                        ChatColor.GRAY + "Unique: " + ChatColor.LIGHT_PURPLE + "Ascension Shard" + ChatColor.GRAY + ", " + ChatColor.RED + "Dragon Heart" + ChatColor.GRAY + ", " + ChatColor.GOLD + "Voidwing Relic",
                        ChatColor.GOLD + "Tier 4 encounter chance" + ChatColor.GRAY + " (Peak Tier 4/5): " + ChatColor.WHITE + formatSpawnChance(tierFourEncounterChance(4, tierFourSpawnChance)),
                        ChatColor.DARK_RED + "Tier 5 encounter chance" + ChatColor.GRAY + " (Peak Tier 5): " + ChatColor.WHITE + formatSpawnChance(tierFiveEncounterChance(5, tierFourSpawnChance, tierFiveSpawnChance)),
                        ChatColor.RED + "Dragon Heart" + ChatColor.GRAY + " (Tier 5 only): " + ChatColor.WHITE + formatSpawnChance(DRAGON_HEART_DROP_CHANCE),
                        ChatColor.GOLD + "Dragon's Spine" + ChatColor.GRAY + " (Tier 4+, active): " + ChatColor.RED + "1 in 650",
                        ChatColor.GOLD + "Tier 4: " + ChatColor.WHITE + "Meteor echoes, faster aerial pressure, and spreading voidfire rifts",
                        ChatColor.RED + "Tier 5 Dragons: " + ChatColor.WHITE + "deal true damage",
                        ChatColor.RED + "Tier 5: " + ChatColor.WHITE + "denser meteor storms, harsher rifts, and cataclysm collapses",
                        ChatColor.GRAY + "(True Damage bypasses reputation resistance)",
                        ChatColor.GRAY + "Dragon Slayer Armor: " + ChatColor.RED + "1 in 580,000"
                )
        ));
        inventory.setItem(40, navigationItem(Material.ARROW, ChatColor.YELLOW + "Back", List.of(ChatColor.GRAY + "Return to the watcher")));

        openViews.put(player.getUniqueId(), GuiView.REWARDS);
        player.openInventory(inventory);
    }

    public void openSummary(Player player) {
        FightSummary summary = lastSummaryByPlayer.get(player.getUniqueId());
        if (summary == null) {
            player.sendMessage(ChatColor.RED + "You do not have a Dragon Ascension summary yet.");
            return;
        }

        Inventory inventory = Bukkit.createInventory(null, 54, SUMMARY_TITLE);
        fillBorders(inventory, Material.PURPLE_STAINED_GLASS_PANE);

        inventory.setItem(11, infoItem(
                summary.dragonType.icon,
                ChatColor.LIGHT_PURPLE + "Encounter",
                List.of(
                        ChatColor.GRAY + "Dragon: " + summary.dragonType.displayName(),
                        ChatColor.GRAY + "Mutation: " + summary.mutation.displayName(),
                        ChatColor.GRAY + "Tier: " + ChatColor.GOLD + "Tier " + summary.tier,
                        ChatColor.GRAY + "Reward rank: " + summary.rewardTierLabel()
                )
        ));
        inventory.setItem(13, infoItem(
                Material.BOOK,
                ChatColor.AQUA + "Contribution Score",
                List.of(
                        ChatColor.GRAY + "Final score: " + ChatColor.WHITE + formatScore(summary.score),
                        ChatColor.GRAY + "Damage: " + ChatColor.RED + formatScore(summary.damageDealt),
                        ChatColor.GRAY + "Crystals: " + ChatColor.WHITE + summary.crystalsDestroyed,
                        ChatColor.GRAY + "Mechanics: " + ChatColor.WHITE + summary.mechanicScore,
                        ChatColor.GRAY + "Eyes: " + ChatColor.WHITE + summary.eyesPlaced,
                        ChatColor.GRAY + "Active time: " + ChatColor.WHITE + Math.round(summary.activeTicks / 20.0D) + "s"
                )
        ));
        inventory.setItem(15, infoItem(
                Material.EXPERIENCE_BOTTLE,
                ChatColor.GREEN + "Progression",
                List.of(
                        ChatColor.GRAY + "Reputation now: " + ChatColor.GREEN + summary.reputationAfter,
                        ChatColor.GRAY + "Damage perk: " + ChatColor.RED + "+" + Math.round((damageBonusMultiplier(summary.reputationAfter) - 1.0D) * 100.0D) + "%",
                        ChatColor.GRAY + "Loot perk: " + ChatColor.GOLD + "+" + Math.round((lootChanceMultiplier(summary.reputationAfter) - 1.0D) * 100.0D) + "%"
                )
        ));

        int lootSlot = 28;
        for (ItemStack item : summary.lootPreview) {
            if (lootSlot >= 35) {
                break;
            }
            inventory.setItem(lootSlot++, item);
        }

        inventory.setItem(49, navigationItem(Material.ARROW, ChatColor.YELLOW + "Back", List.of(ChatColor.GRAY + "Return to the watcher")));
        openViews.put(player.getUniqueId(), GuiView.SUMMARY);
        player.openInventory(inventory);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players can open the Dragon Watcher GUI.");
                return true;
            }
            openMain(player);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        if (subcommand.equals("status")) {
            sender.sendMessage(ChatColor.LIGHT_PURPLE + "Dragon Ascension");
            sender.sendMessage(ChatColor.GRAY + "Status: " + statusLabel());
            sender.sendMessage(ChatColor.GRAY + "World: " + worldLabel());
            sender.sendMessage(ChatColor.GRAY + "Altar: " + altarSummary());
            sender.sendMessage(ChatColor.GRAY + "Points set: " + configuredPointCount() + "/" + requiredEyes);
            sender.sendMessage(ChatColor.GRAY + "Performance: " + performanceSummary());
            if (fight != null) {
                sender.sendMessage(ChatColor.GRAY + "Encounter: " + fight.profile.dragonType().displayName()
                        + ChatColor.GRAY + " Tier " + ChatColor.GOLD + fight.profile.tier()
                        + ChatColor.GRAY + " | Phase " + ChatColor.WHITE + fight.phase.displayName());
            } else if (summoning != null) {
                EncounterProfile profile = currentEncounterProfile();
                sender.sendMessage(ChatColor.GRAY + "Summoning: " + placedEyeCount() + "/" + requiredEyes
                        + ChatColor.GRAY + " -> " + profile.dragonType().displayName()
                        + ChatColor.GRAY + " Peak Tier " + ChatColor.GOLD + profile.tier());
                String rarityLabel = rareTierChanceLabel(profile.tier());
                if (!rarityLabel.isEmpty()) {
                    sender.sendMessage(ChatColor.GRAY + "Upper-tier odds: " + rarityLabel);
                }
            }
            return true;
        }
        if (subcommand.equals("codex")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players can open the Dragon Codex.");
                return true;
            }
            openCodex(player);
            return true;
        }
        if (subcommand.equals("rewards")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players can open the rewards GUI.");
                return true;
            }
            openRewards(player);
            return true;
        }
        if (subcommand.equals("summary")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players can open the summary GUI.");
                return true;
            }
            openSummary(player);
            return true;
        }
        if (subcommand.equals("forcesummon")) {
            if (!sender.hasPermission("grivience.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to force summon dragons.");
                return true;
            }
            if (fight != null) {
                sender.sendMessage(ChatColor.RED + "A Dragon Ascension encounter is already active.");
                return true;
            }
            if (summoning != null) {
                sender.sendMessage(ChatColor.RED + "A Dragon Ascension summoning is already in progress.");
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(ChatColor.RED + "Usage: /" + label + " forcesummon <type> <tier> [mutation]");
                sender.sendMessage(ChatColor.GRAY + "Types: " + ChatColor.WHITE + String.join(", ", dragonTypeCommandOptions()));
                sender.sendMessage(ChatColor.GRAY + "Mutations: " + ChatColor.WHITE + String.join(", ", mutationTypeCommandOptions()));
                return true;
            }

            DragonType dragonType = DragonType.parse(args[1]);
            if (dragonType == null) {
                sender.sendMessage(ChatColor.RED + "Unknown dragon type. Valid types: " + String.join(", ", dragonTypeCommandOptions()));
                return true;
            }

            int tier;
            try {
                tier = Integer.parseInt(args[2]);
            } catch (NumberFormatException exception) {
                sender.sendMessage(ChatColor.RED + "Tier must be a number between 1 and 5.");
                return true;
            }
            if (tier < 1 || tier > 5) {
                sender.sendMessage(ChatColor.RED + "Tier must be between 1 and 5.");
                return true;
            }

            MutationType mutation = MutationType.NONE;
            if (args.length >= 4) {
                mutation = MutationType.parse(args[3]);
                if (mutation == null) {
                    sender.sendMessage(ChatColor.RED + "Unknown mutation. Valid mutations: " + String.join(", ", mutationTypeCommandOptions()));
                    return true;
                }
            }

            World world = resolvedWorld();
            Location altar = altarLocation(world);
            if (world == null || altar == null) {
                sender.sendMessage(ChatColor.RED + "Dragon Ascension cannot start because the altar world is not available.");
                return true;
            }

            EncounterProfile forcedProfile = forcedEncounterProfile(dragonType, mutation, tier);
            String failure = launchEncounter(world, altar, forcedProfile, forcedProfile, Map.of());
            if (failure != null) {
                plugin.getLogger().warning("Forced Dragon Ascension startup aborted: " + failure);
                sender.sendMessage(ChatColor.RED + "Forced summon failed: " + failure + ".");
                return true;
            }

            sender.sendMessage(ChatColor.GREEN + "Forced summon started: "
                    + dragonType.displayName()
                    + ChatColor.GREEN + ", Tier "
                    + ChatColor.GOLD + tier
                    + (mutation != MutationType.NONE ? ChatColor.GREEN + ", " + mutation.displayName() : "")
                    + ChatColor.GREEN + ".");
            return true;
        }
        if (subcommand.equals("setaltar")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players can set the altar location.");
                return true;
            }
            if (!player.hasPermission("grivience.admin")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to edit the altar.");
                return true;
            }
            Location location = player.getLocation();
            plugin.getConfig().set("dragon-ascension.world-name", location.getWorld() != null ? location.getWorld().getName() : "");
            plugin.getConfig().set("dragon-ascension.altar.world", location.getWorld() != null ? location.getWorld().getName() : "");
            plugin.getConfig().set("dragon-ascension.altar.x", location.getX());
            plugin.getConfig().set("dragon-ascension.altar.y", location.getY());
            plugin.getConfig().set("dragon-ascension.altar.z", location.getZ());
            plugin.getConfig().set("dragon-ascension.altar.yaw", location.getYaw());
            plugin.getConfig().set("dragon-ascension.altar.pitch", location.getPitch());
            plugin.saveConfig();
            loadConfig();
            player.sendMessage(ChatColor.GREEN + "Dragon Ascension altar set to " + altarSummary() + ChatColor.GREEN + ".");
            return true;
        }
        if (subcommand.equals("setpoint")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(ChatColor.RED + "Only players can set altar points.");
                return true;
            }
            if (!player.hasPermission("grivience.admin")) {
                player.sendMessage(ChatColor.RED + "You do not have permission to edit altar points.");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Usage: /" + label + " setpoint <1-" + requiredEyes + ">");
                return true;
            }
            int pointIndex;
            try {
                pointIndex = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                player.sendMessage(ChatColor.RED + "Point index must be a number.");
                return true;
            }
            if (pointIndex < 1 || pointIndex > requiredEyes) {
                player.sendMessage(ChatColor.RED + "Point index must be between 1 and " + requiredEyes + ".");
                return true;
            }

            Block targetBlock = player.getTargetBlockExact(12);
            if (targetBlock == null || targetBlock.getType() == Material.AIR) {
                player.sendMessage(ChatColor.RED + "Look directly at the altar pillar block when setting the point.");
                return true;
            }

            Location point = targetBlock.getLocation().add(0.5D, 0.0D, 0.5D);
            saveAltarPoint(pointIndex - 1, point);
            player.sendMessage(ChatColor.GREEN + "Dragon altar point " + pointIndex + " set at "
                    + point.getWorld().getName() + " "
                    + Math.round(point.getX()) + " "
                    + Math.round(point.getY()) + " "
                    + Math.round(point.getZ()) + ChatColor.GREEN + ".");
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Usage: /" + label + " [status|codex|rewards|summary|forcesummon|setaltar|setpoint]");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> matches = new ArrayList<>();
            for (String suggestion : COMMAND_SUGGESTIONS) {
                if (suggestion.startsWith(prefix)) {
                    matches.add(suggestion);
                }
            }
            return matches;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setpoint")) {
            List<String> matches = new ArrayList<>();
            for (int index = 1; index <= requiredEyes; index++) {
                String value = Integer.toString(index);
                if (value.startsWith(args[1])) {
                    matches.add(value);
                }
            }
            return matches;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("forcesummon")) {
            List<String> matches = new ArrayList<>();
            for (String option : dragonTypeCommandOptions()) {
                if (option.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    matches.add(option);
                }
            }
            return matches;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("forcesummon")) {
            List<String> matches = new ArrayList<>();
            for (int tier = 1; tier <= 5; tier++) {
                String value = Integer.toString(tier);
                if (value.startsWith(args[2])) {
                    matches.add(value);
                }
            }
            return matches;
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("forcesummon")) {
            List<String> matches = new ArrayList<>();
            for (String option : mutationTypeCommandOptions()) {
                if (option.startsWith(args[3].toLowerCase(Locale.ROOT))) {
                    matches.add(option);
                }
            }
            return matches;
        }
        return List.of();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!enabled || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock() == null || event.getItem() == null || !isSummoningEye(event.getItem())) {
            return;
        }

        World world = resolvedWorld();
        if (world == null || !Objects.equals(event.getClickedBlock().getWorld(), world)) {
            return;
        }

        int pedestalIndex = pedestalIndex(event.getClickedBlock().getLocation());
        if (pedestalIndex < 0) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (configuredPointCount() < requiredEyes) {
            player.sendMessage(ChatColor.RED + "All " + requiredEyes + " altar points must be set before summoning can begin.");
            return;
        }
        if (fight != null) {
            player.sendMessage(ChatColor.RED + "A Dragon Ascension encounter is already active.");
            return;
        }

        if (summoning == null) {
            summoning = new SummoningState();
        }
        if (summoning.eyesByPedestal.containsKey(pedestalIndex)) {
            player.sendMessage(ChatColor.RED + "That pedestal already has an eye placed.");
            return;
        }

        if (!consumeOne(player, event.getHand())) {
            player.sendMessage(ChatColor.RED + "You need a Summoning Eye in the hand you are using.");
            return;
        }
        PedestalAspect aspect = pedestalAspects.get(Math.min(pedestalIndex, pedestalAspects.size() - 1));
        summoning.eyesByPedestal.put(pedestalIndex, new PlacedEye(player.getUniqueId(), aspect, pedestalIndex));
        summoning.lastInteractionMillis = System.currentTimeMillis();
        contribution(summoning.contributions, player.getUniqueId()).eyesPlaced++;
        EncounterLock lockedEncounter = maybeUpdateSummoningEncounterLock();

        Location pedestal = pedestalLocation(world, pedestalIndex);
        playEyePlacementCue(player, pedestal, aspect, pedestalIndex);
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Placed a Summoning Eye on the " + aspect.displayName + ChatColor.LIGHT_PURPLE + " pedestal.");
        if (lockedEncounter != null) {
            if (lockedEncounter.typeLocked() && lockedEncounter.tierChanged()) {
                notifyArena(ChatColor.GOLD + "Encounter locked: " + lockedEncounter.dragonType().displayName()
                        + ChatColor.GOLD + ", Peak Tier " + ChatColor.YELLOW + lockedEncounter.tier() + ChatColor.GOLD + ".");
            } else if (lockedEncounter.tierChanged()) {
                notifyArena(ChatColor.GOLD + "Peak encounter tier increased to " + ChatColor.YELLOW + "Tier "
                        + lockedEncounter.tier() + ChatColor.GOLD + ".");
            } else if (lockedEncounter.typeLocked()) {
                notifyArena(ChatColor.GOLD + "Dragon type locked: " + lockedEncounter.dragonType().displayName() + ChatColor.GOLD + ".");
            }
        }
        refreshAltarPointHolograms();
        refreshAltarPointEyeDisplays();
        updateSummoningBossBar();

        if (placedEyeCount() >= requiredEyes) {
            beginEncounter();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        GuiView view = openViews.get(player.getUniqueId());
        if (view == null || !isDragonView(event.getView().getTitle(), view)) {
            return;
        }

        event.setCancelled(true);
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= event.getView().getTopInventory().getSize()) {
            return;
        }

        if (view == GuiView.MAIN) {
            if (rawSlot == 29) {
                openCodex(player);
            } else if (rawSlot == 31) {
                openRewards(player);
            } else if (rawSlot == 33) {
                openSummary(player);
            }
            return;
        }

        if (rawSlot == 49 || rawSlot == 40) {
            openMain(player);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        GuiView view = openViews.get(player.getUniqueId());
        if (view != null && isDragonView(event.getView().getTitle(), view)) {
            openViews.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (summoningBossBar != null) {
            summoningBossBar.removePlayer(event.getPlayer());
        }
        if (fightBossBar != null) {
            fightBossBar.removePlayer(event.getPlayer());
        }
        openViews.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (summoningBossBar != null) {
            summoningBossBar.removePlayer(event.getPlayer());
        }
        if (fightBossBar != null && fight != null && !isInArena(event.getPlayer(), fight.altar, arenaRadius * 1.4D)) {
            fightBossBar.removePlayer(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (fight == null) {
            return;
        }

        Entity victim = event.getEntity();
        Player attacker = attackingPlayer(event.getDamager());

        if (isManagedCrystal(victim)) {
            handleCrystalDamage(event, attacker);
            return;
        }
        EnderDragon struckDragon = resolveManagedDragon(victim);
        if (struckDragon != null) {
            handleDragonDamage(event, attacker, struckDragon);
            return;
        }
        if (victim instanceof Player player && isFightDamageSource(event.getDamager())) {
            event.setCancelled(true);
            damagePlayer(player, event.getDamage(), managedDragon());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDeath(EntityDeathEvent event) {
        if (fight == null || !isManagedDragon(event.getEntity())) {
            return;
        }
        event.getDrops().clear();
        event.setDroppedExp(0);
        finishEncounter(true);
    }

    private void loadConfig() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("dragon-ascension");
        enabled = section == null || section.getBoolean("enabled", true);
        configuredWorldName = plugin.getConfig().getString("dragon-ascension.world-name", "").trim();
        altarWorldName = plugin.getConfig().getString("dragon-ascension.altar.world", configuredWorldName).trim();
        altarX = plugin.getConfig().getDouble("dragon-ascension.altar.x", 0.5D);
        altarY = plugin.getConfig().getDouble("dragon-ascension.altar.y", 80.0D);
        altarZ = plugin.getConfig().getDouble("dragon-ascension.altar.z", 0.5D);
        altarYaw = (float) plugin.getConfig().getDouble("dragon-ascension.altar.yaw", 0.0D);
        altarPitch = (float) plugin.getConfig().getDouble("dragon-ascension.altar.pitch", 0.0D);
        pedestalRadius = Math.max(2.5D, plugin.getConfig().getDouble("dragon-ascension.summoning.pedestal-radius", 6.0D));
        requiredEyes = Math.max(4, Math.min(8, plugin.getConfig().getInt("dragon-ascension.summoning.required-eyes", 8)));
        int configuredLockEyes = plugin.getConfig().getInt(
                "dragon-ascension.summoning.dragon-type-lock-eyes",
                Math.max(1, (requiredEyes + 1) / 2)
        );
        dragonTypeLockEyes = clampDragonTypeLockEyes(requiredEyes, configuredLockEyes);
        summonItemKey = plugin.getConfig().getString("dragon-ascension.summoning.item-key", SUMMONING_EYE_KEY).trim().toUpperCase(Locale.ROOT);
        summonMaterial = parseMaterial(plugin.getConfig().getString("dragon-ascension.summoning.item-material", "ENDER_EYE"), Material.ENDER_EYE);
        summoningTimeoutMillis = Math.max(30_000L, plugin.getConfig().getLong("dragon-ascension.summoning.timeout-seconds", 300L) * 1000L);
        baseMutationChance = clamp(plugin.getConfig().getDouble("dragon-ascension.summoning.base-mutation-chance", 8.0D), 0.0D, 100.0D);
        tierFourSpawnChance = sanitizeRareTierChance(plugin.getConfig().getDouble("dragon-ascension.summoning.tier-4-spawn-chance", 0.02D));
        double configuredTierFiveChance = sanitizeRareTierChance(plugin.getConfig().getDouble("dragon-ascension.summoning.tier-5-spawn-chance", 0.0025D));
        tierFiveSpawnChance = Math.min(Math.max(0.0D, 1.0D - tierFourSpawnChance), configuredTierFiveChance);
        if (configuredTierFiveChance > tierFiveSpawnChance) {
            plugin.getLogger().warning("dragon-ascension.summoning.tier-4-spawn-chance and tier-5-spawn-chance exceeded 100% combined. Clamping Tier 5 chance to the remaining probability budget.");
        }

        tickPeriodTicks = Math.max(5, plugin.getConfig().getInt("dragon-ascension.fight.tick-period", 10));
        arenaRadius = Math.max(20.0D, plugin.getConfig().getDouble("dragon-ascension.fight.arena-radius", 42.0D));
        maxFightDurationTicks = Math.max(20L * 180L, plugin.getConfig().getLong("dragon-ascension.fight.max-duration-seconds", 900L) * 20L);
        baseDragonHealth = Math.max(250.0D, plugin.getConfig().getDouble("dragon-ascension.fight.base-health", 500.0D));
        healthPerTier = Math.max(50.0D, plugin.getConfig().getDouble("dragon-ascension.fight.health-per-tier", 200.0D));
        baseShield = Math.max(100.0D, plugin.getConfig().getDouble("dragon-ascension.fight.phase-one.base-shield", 220.0D));
        shieldPerTier = Math.max(0.0D, plugin.getConfig().getDouble("dragon-ascension.fight.phase-one.shield-per-tier", 55.0D));
        crystalCount = Math.max(2, Math.min(8, plugin.getConfig().getInt("dragon-ascension.fight.phase-one.crystals", 4)));
        ascensionHealthPercent = clamp(plugin.getConfig().getDouble("dragon-ascension.fight.phase-two.ascension-health-percent", 0.30D), 0.10D, 0.75D);
        baseDamageMultiplier = Math.max(0.5D, plugin.getConfig().getDouble("dragon-ascension.fight.base-damage-multiplier", 1.0D));
        damagePerTier = Math.max(0.0D, plugin.getConfig().getDouble("dragon-ascension.fight.damage-per-tier", 0.12D));

        minimumRewardActiveSeconds = Math.max(10, plugin.getConfig().getInt("dragon-ascension.rewards.minimum-active-seconds", 30));
        minimumRewardScore = Math.max(0.0D, plugin.getConfig().getDouble("dragon-ascension.rewards.minimum-score", 12.0D));
        summaryAutoOpen = plugin.getConfig().getBoolean("dragon-ascension.rewards.summary-auto-open", true);
        rareArmorChance = DRAGON_SLAYER_ARMOR_DROP_CHANCE;
        voidwingRelicBaseChance = clamp(plugin.getConfig().getDouble("dragon-ascension.rewards.voidwing-relic-base-chance", 0.03D), 0.0D, 1.0D);

        reputationPerKill = Math.max(1, plugin.getConfig().getInt("dragon-ascension.progression.reputation-per-kill", 1));
        maxReputation = Math.max(1, plugin.getConfig().getInt("dragon-ascension.progression.max-reputation", 10));

        scoreWeights = new ScoreWeights(
                Math.max(1.0D, plugin.getConfig().getDouble("dragon-ascension.scoring.weights.max-damage-share-points", 35.0D)),
                Math.max(0.0D, plugin.getConfig().getDouble("dragon-ascension.scoring.weights.crystal-points", 10.0D)),
                Math.max(0.0D, plugin.getConfig().getDouble("dragon-ascension.scoring.weights.mechanic-point-weight", 4.0D)),
                Math.max(0.0D, plugin.getConfig().getDouble("dragon-ascension.scoring.weights.eye-points", 5.0D)),
                Math.max(0.0D, plugin.getConfig().getDouble("dragon-ascension.scoring.weights.presence-point-per-second", 0.30D)),
                Math.max(0.0D, plugin.getConfig().getDouble("dragon-ascension.scoring.weights.max-presence-points", 18.0D))
        );
        scoreThresholds = new Thresholds(
                Math.max(1.0D, plugin.getConfig().getDouble("dragon-ascension.scoring.thresholds.s-tier", 60.0D)),
                Math.max(1.0D, plugin.getConfig().getDouble("dragon-ascension.scoring.thresholds.a-tier", 42.0D)),
                Math.max(1.0D, plugin.getConfig().getDouble("dragon-ascension.scoring.thresholds.b-tier", 26.0D)),
                Math.max(1.0D, plugin.getConfig().getDouble("dragon-ascension.scoring.thresholds.c-tier", 12.0D))
        );

        pedestalAspects = new ArrayList<>();
        for (String configured : plugin.getConfig().getStringList("dragon-ascension.summoning.pedestal-aspects")) {
            PedestalAspect aspect = PedestalAspect.parse(configured);
            if (aspect != null) {
                pedestalAspects.add(aspect);
            }
        }
        if (pedestalAspects.isEmpty()) {
            pedestalAspects.addAll(DEFAULT_PEDESTALS);
        }
        while (pedestalAspects.size() < requiredEyes) {
            pedestalAspects.add(DEFAULT_PEDESTALS.get(pedestalAspects.size() % DEFAULT_PEDESTALS.size()));
        }
        if (pedestalAspects.size() > requiredEyes) {
            pedestalAspects = new ArrayList<>(pedestalAspects.subList(0, requiredEyes));
        }

        altarPoints.clear();
        ConfigurationSection pointsSection = plugin.getConfig().getConfigurationSection("dragon-ascension.altar.points");
        if (pointsSection != null) {
            for (String key : pointsSection.getKeys(false)) {
                int index;
                try {
                    index = Integer.parseInt(key) - 1;
                } catch (NumberFormatException exception) {
                    continue;
                }
                if (index < 0 || index >= requiredEyes) {
                    continue;
                }
                ConfigurationSection pointSection = pointsSection.getConfigurationSection(key);
                if (pointSection == null) {
                    continue;
                }
                World pointWorld = Bukkit.getWorld(pointSection.getString("world", altarWorldName));
                if (pointWorld == null) {
                    continue;
                }
                altarPoints.put(index, new Location(
                        pointWorld,
                        pointSection.getDouble("x"),
                        pointSection.getDouble("y"),
                        pointSection.getDouble("z")
                ));
            }
        }
    }

    private void loadProgression() {
        progressionByScope.clear();
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection profiles = dataConfig.getConfigurationSection("profiles");
        if (profiles == null) {
            return;
        }

        for (String key : profiles.getKeys(false)) {
            ConfigurationSection section = profiles.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            ProgressionRecord progression = new ProgressionRecord();
            progression.reputation = Math.max(0, section.getInt("reputation", 0));
            progression.kills = Math.max(0, section.getInt("kills", 0));
            progression.bestScore = Math.max(0.0D, section.getDouble("best-score", 0.0D));
            for (String typeName : section.getStringList("discovered-types")) {
                DragonType type = DragonType.parse(typeName);
                if (type != null) {
                    progression.discoveredTypes.add(type);
                }
            }
            for (String mutationName : section.getStringList("discovered-mutations")) {
                MutationType mutation = MutationType.parse(mutationName);
                if (mutation != null) {
                    progression.discoveredMutations.add(mutation);
                }
            }
            ConfigurationSection killsByType = section.getConfigurationSection("kills-by-type");
            if (killsByType != null) {
                for (String typeName : killsByType.getKeys(false)) {
                    DragonType type = DragonType.parse(typeName);
                    if (type != null) {
                        progression.killsByType.put(type, Math.max(0, killsByType.getInt(typeName, 0)));
                    }
                }
            }
            ConfigurationSection bestByType = section.getConfigurationSection("best-scores");
            if (bestByType != null) {
                for (String typeName : bestByType.getKeys(false)) {
                    DragonType type = DragonType.parse(typeName);
                    if (type != null) {
                        progression.bestScores.put(type, Math.max(0.0D, bestByType.getDouble(typeName, 0.0D)));
                    }
                }
            }
            progressionByScope.put(key, progression);
        }
    }

    private void saveProgression() {
        if (dataConfig == null) {
            dataConfig = new YamlConfiguration();
        }
        dataConfig.set("profiles", null);
        for (Map.Entry<String, ProgressionRecord> entry : progressionByScope.entrySet()) {
            String base = "profiles." + entry.getKey() + ".";
            ProgressionRecord progression = entry.getValue();
            dataConfig.set(base + "reputation", progression.reputation);
            dataConfig.set(base + "kills", progression.kills);
            dataConfig.set(base + "best-score", progression.bestScore);
            dataConfig.set(base + "discovered-types", progression.discoveredTypes.stream().map(Enum::name).toList());
            dataConfig.set(base + "discovered-mutations", progression.discoveredMutations.stream().map(Enum::name).toList());
            for (Map.Entry<DragonType, Integer> killEntry : progression.killsByType.entrySet()) {
                dataConfig.set(base + "kills-by-type." + killEntry.getKey().name(), killEntry.getValue());
            }
            for (Map.Entry<DragonType, Double> bestEntry : progression.bestScores.entrySet()) {
                dataConfig.set(base + "best-scores." + bestEntry.getKey().name(), bestEntry.getValue());
            }
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save dragon ascension progression: " + exception.getMessage());
        }
    }

    private void startTicker() {
        stopTicker();
        if (!enabled) {
            return;
        }
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, tickPeriodTicks, tickPeriodTicks);
    }

    private void stopTicker() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    private void tick() {
        if (!enabled) {
            return;
        }
        if (summoning != null) {
            tickSummoning();
        }
        if (fight != null) {
            tickFight();
        }
    }

    private void tickSummoning() {
        if (System.currentTimeMillis() - summoning.lastInteractionMillis > summoningTimeoutMillis) {
            notifyArena(ChatColor.RED + "Dragon Ascension summoning expired before all eyes were placed.");
            clearSummoning(true);
            return;
        }
        updateSummoningBossBar();
    }

    private void tickFight() {
        EnderDragon dragon = managedDragon();
        if (dragon == null || !dragon.isValid() || dragon.isDead()) {
            finishEncounter(false);
            return;
        }

        long previousElapsedTicks = fight.elapsedTicks;
        fight.elapsedTicks += tickPeriodTicks;
        updateFightBossBar(dragon);
        trackActivePresence();
        tickTargetedStrike();
        tickHazards(previousElapsedTicks);
        tickCataclysmPulse();
        tickDragonSpecificAbilities(dragon, previousElapsedTicks);
        tickCrystalBeams(dragon);

        if (fight.elapsedTicks >= maxFightDurationTicks) {
            notifyArena(ChatColor.RED + "The dragon escaped before the encounter could be completed.");
            finishEncounter(false);
            return;
        }

        switch (fight.phase) {
            case AERIAL -> {
                if (fight.aerialShield <= 0.0D || fight.crystalIds.isEmpty()) {
                    transitionToGround();
                    return;
                }
                if (crossedScaledPeriod(previousElapsedTicks, fight.elapsedTicks, aerialVolleyBasePeriod(fight.profile.tier()))) {
                    aerialVolley();
                }
            }
            case GROUND -> {
                if (dragon.getHealth() <= fight.maxHealth * ascensionHealthPercent) {
                    transitionToAscension();
                    return;
                }
                keepDragonNearArena(dragon, false);
                if (fight.pendingStrike == null
                        && crossedScaledPeriod(previousElapsedTicks, fight.elapsedTicks, targetedStrikeBasePeriod(fight.profile.tier(), false))) {
                    scheduleTargetedStrike();
                }
            }
            case ASCENSION -> {
                keepDragonNearArena(dragon, true);
                if (fight.pendingStrike == null
                        && crossedScaledPeriod(previousElapsedTicks, fight.elapsedTicks, targetedStrikeBasePeriod(fight.profile.tier(), true))) {
                    scheduleTargetedStrike();
                }
                if (crossedScaledPeriod(previousElapsedTicks, fight.elapsedTicks, hazardBasePeriod(fight.profile.tier()))) {
                    spawnHazard();
                }
                if (fight.pendingCataclysm == null
                        && cataclysmEnabled(fight.profile.tier())
                        && crossedScaledPeriod(previousElapsedTicks, fight.elapsedTicks, cataclysmBasePeriod(fight.profile.tier()))) {
                    scheduleCataclysmPulse();
                }
            }
        }
    }

    private void beginEncounter() {
        if (configuredPointCount() < requiredEyes) {
            notifySummoners(ChatColor.RED + "Dragon Ascension requires " + requiredEyes + " configured altar points.");
            clearSummoning(true);
            return;
        }
        World world = resolvedWorld();
        Location altar = altarLocation(world);
        if (world == null || altar == null) {
            notifySummoners(ChatColor.RED + "Dragon Ascension cannot start because the altar world is not available.");
            clearSummoning(true);
            return;
        }

        List<PlacedEye> placedEyes = summoning == null ? List.of() : new ArrayList<>(summoning.eyesByPedestal.values());
        DragonType lockedDragonType = summoning == null ? null : summoning.lockedDragonType;
        Integer lockedPeakTier = summoning == null ? null : summoning.lockedTier;
        EncounterProfile previewProfile = resolvePreviewProfile(placedEyes, lockedDragonType, lockedPeakTier);
        EncounterProfile profile = resolveSpawnProfile(placedEyes, lockedDragonType, lockedPeakTier);
        String failure = launchEncounter(world, altar, previewProfile, profile, summoning == null ? Map.of() : summoning.contributions);
        if (failure != null) {
            plugin.getLogger().warning("Dragon Ascension startup aborted: " + failure);
            notifySummoners(ChatColor.RED + "Dragon Ascension could not start safely. Check the altar configuration and server log.");
            clearSummoning(true);
            return;
        }

        clearSummoning(true);
        refreshAltarPointHolograms();
        refreshAltarPointEyeDisplays();
    }

    private String launchEncounter(
            World world,
            Location altar,
            EncounterProfile previewProfile,
            EncounterProfile profile,
            Map<UUID, PlayerContribution> seededContributions
    ) {
        double maxHealth = dragonMaxHealthForTier(baseDragonHealth, healthPerTier, profile.tier());
        double shield = dragonShieldForTier(baseShield, shieldPerTier, profile.tier());
        String readinessFailure = encounterReadinessFailure(world, altar, profile, maxHealth, shield);
        if (readinessFailure != null) {
            return readinessFailure;
        }

        prepareEncounterChunks(world, altar);

        EnderDragon dragon = world.spawn(altar.clone().add(0.0D, 18.0D, 0.0D), EnderDragon.class);
        dragon.setCustomName(dragonName(profile, Phase.AERIAL));
        dragon.setCustomNameVisible(true);
        dragon.setPersistent(true);
        dragon.setRemoveWhenFarAway(false);
        trySetDragonPhase(dragon, EnderDragon.Phase.CIRCLING);

        AttributeInstance maxHealthAttribute = dragon.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            maxHealthAttribute.setBaseValue(maxHealth);
            double effectiveMax = maxHealthAttribute.getValue();
            if (effectiveMax < maxHealth - 0.01) {
                plugin.getLogger().warning("Dragon health (" + maxHealth + ") was capped to " + effectiveMax + " by the server's attribute limit. Increase 'settings.attribute.max_health' in spigot.yml to support higher health.");
                maxHealth = effectiveMax;
            }
        }
        io.papermc.Grivience.util.SkyblockDamageScaleUtil.setHealthSafely(dragon, maxHealth);

        if (!validateSpawnedDragon(dragon, world, altar, maxHealth)) {
            dragon.remove();
            return "spawned dragon failed validation for " + stripColor(dragonName(profile, Phase.AERIAL));
        }

        FightSession pendingFight = new FightSession();
        pendingFight.dragonId = dragon.getUniqueId();
        pendingFight.altar = altar;
        pendingFight.profile = profile;
        pendingFight.phase = Phase.AERIAL;
        pendingFight.elapsedTicks = 0L;
        pendingFight.maxHealth = maxHealth;
        pendingFight.aerialShield = shield;
        pendingFight.maxAerialShield = shield;
        pendingFight.contributions.putAll(seededContributions);

        int spawnedCrystals = spawnPhaseCrystals(world, altar, pendingFight);
        if (spawnedCrystals != crystalCount) {
            removeCrystals(pendingFight);
            dragon.remove();
            return "spawned " + spawnedCrystals + "/" + crystalCount + " crystals";
        }

        fight = pendingFight;
        world.playSound(altar, Sound.ENTITY_ENDER_DRAGON_GROWL, 10.0F, 0.8F);
        world.playSound(altar, Sound.ENTITY_WITHER_SPAWN, 1.4F, 1.1F);
        playTierEncounterEffect(altar, profile.tier(), Phase.AERIAL);
        if (previewProfile.tier() > profile.tier()) {
            notifyArena(ChatColor.DARK_GRAY + "The upper-tier surge was unstable. The encounter settled at " + ChatColor.GOLD + "Tier " + profile.tier() + ChatColor.DARK_GRAY + ".");
        }
        notifyArena(ChatColor.LIGHT_PURPLE + "A " + profile.mutation().displayName()
                + ChatColor.LIGHT_PURPLE + " " + profile.dragonType().displayName()
                + ChatColor.LIGHT_PURPLE + " has begun its ascension. Tier " + ChatColor.GOLD + profile.tier() + ChatColor.LIGHT_PURPLE + ".");
        announceTierMechanics(profile.tier());
        updateFightBossBar(dragon);
        return null;
    }

    private void finishEncounter(boolean defeated) {
        FightSession currentFight = fight;
        if (currentFight == null) {
            return;
        }
        currentFight.failed = !defeated;
        if (currentFight.shieldTask != null) {
            currentFight.shieldTask.cancel();
        }
        Set<UUID> summaryRecipients = Set.of();

        EnderDragon dragon = managedDragon();
        if (dragon != null) {
            dragon.remove();
        }
        removeCrystals(currentFight);

        if (fightBossBar != null) {
            fightBossBar.removeAll();
            fightBossBar = null;
        }

        if (defeated) {
            summaryRecipients = distributeRewards(currentFight);
            notifyArena(ChatColor.GREEN + "The Dragon Ascension encounter was completed.");
        } else {
            for (UUID playerId : currentFight.contributions.keySet()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.sendMessage(ChatColor.RED + "The Dragon Ascension encounter ended without rewards.");
                }
            }
        }

        fight = null;
        clearSummoning(true);
        refreshAltarPointHolograms();
        refreshAltarPointEyeDisplays();

        if (defeated && summaryAutoOpen && !summaryRecipients.isEmpty()) {
            Set<UUID> recipients = new HashSet<>(summaryRecipients);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                for (UUID playerId : recipients) {
                    queueSummaryOpen(playerId, 0);
                }
            }, 2L);
        }
    }

    private void clearSummoning(boolean removeBossBar) {
        summoning = null;
        if (removeBossBar && summoningBossBar != null) {
            summoningBossBar.removeAll();
            summoningBossBar = null;
        }
        refreshAltarPointHolograms();
        refreshAltarPointEyeDisplays();
    }

    private void clearFight(boolean removeBossBar) {
        if (fight != null) {
            if (fight.shieldTask != null) {
                fight.shieldTask.cancel();
            }
            EnderDragon dragon = managedDragon();
            if (dragon != null) {
                dragon.remove();
            }
            removeCrystals(fight);
        }
        if (removeBossBar && fightBossBar != null) {
            fightBossBar.removeAll();
            fightBossBar = null;
        }
        fight = null;
        refreshAltarPointHolograms();
        refreshAltarPointEyeDisplays();
    }

    private void updateSummoningBossBar() {
        if (summoning == null) {
            if (summoningBossBar != null) {
                summoningBossBar.removeAll();
                summoningBossBar = null;
            }
            return;
        }

        EncounterProfile profile = currentEncounterProfile();
        if (summoningBossBar == null) {
            summoningBossBar = Bukkit.createBossBar("", BarColor.PURPLE, BarStyle.SOLID);
        }
        summoningBossBar.setTitle(ChatColor.LIGHT_PURPLE + "Dragon Influence "
                + ChatColor.GRAY + placedEyeCount() + "/" + requiredEyes
                + ChatColor.DARK_GRAY + " | "
                + profile.dragonType().displayName()
                + lockStatusSuffix()
                + ChatColor.DARK_GRAY + " | "
                + ChatColor.GOLD + (profile.tier() >= 4 ? "Peak Tier " : "Tier ") + profile.tier()
                + ChatColor.DARK_GRAY + " | "
                + ChatColor.RED + formatPercent(profile.mutationChance()));
        summoningBossBar.setProgress(clamp(placedEyeCount() / (double) requiredEyes, 0.0D, 1.0D));
        syncBossBarPlayers(summoningBossBar, nearbyPlayers(altarLocation(resolvedWorld()), arenaRadius * 1.6D));
    }

    private void updateFightBossBar(EnderDragon dragon) {
        if (fight == null) {
            if (fightBossBar != null) {
                fightBossBar.removeAll();
                fightBossBar = null;
            }
            return;
        }
        if (fightBossBar == null) {
            fightBossBar = Bukkit.createBossBar("", BarColor.RED, BarStyle.SEGMENTED_10);
        }
        String shieldText = fight.phase == Phase.AERIAL
                ? ChatColor.AQUA + " Shield " + ChatColor.WHITE + formatScore(fight.aerialShield)
                : ChatColor.GREEN + " HP " + ChatColor.WHITE + formatScore(dragon.getHealth()) + "/" + formatScore(fight.maxHealth);
        fightBossBar.setTitle(dragonName(fight.profile, fight.phase) + ChatColor.DARK_GRAY + " | " + shieldText);
        double progress = fight.phase == Phase.AERIAL
                ? clamp(fight.aerialShield / Math.max(1.0D, fight.maxAerialShield), 0.0D, 1.0D)
                : clamp(dragon.getHealth() / Math.max(1.0D, fight.maxHealth), 0.0D, 1.0D);
        fightBossBar.setProgress(progress);
        syncBossBarPlayers(fightBossBar, nearbyPlayers(fight.altar, arenaRadius * 1.5D));
    }

    private int spawnPhaseCrystals(World world, Location altar, FightSession session) {
        removeCrystals(session);
        int spawnedCrystals = 0;
        for (int index = 0; index < crystalCount; index++) {
            Location crystalLocation = crystalLocation(altar, index, crystalCount);
            if (crystalLocation == null || crystalLocation.getWorld() == null) {
                continue;
            }
            crystalLocation.getChunk().load();
            EnderCrystal crystal = world.spawn(crystalLocation, EnderCrystal.class);
            crystal.setShowingBottom(false);
            crystal.setBeamTarget(null);
            session.crystalIds.add(crystal.getUniqueId());
            spawnedCrystals++;
        }
        return spawnedCrystals;
    }

    private String encounterReadinessFailure(World world, Location altar, EncounterProfile profile, double maxHealth, double shield) {
        if (fight != null) {
            return "encounter already active";
        }
        if (world == null) {
            return "resolved world unavailable";
        }
        if (altar == null || altar.getWorld() == null || !world.equals(altar.getWorld())) {
            return "altar location invalid";
        }
        if (profile == null || profile.tier() < 1 || profile.tier() > 5) {
            return "encounter profile invalid";
        }
        if (!Double.isFinite(maxHealth) || maxHealth <= 0.0D) {
            return "dragon max health invalid";
        }
        if (!Double.isFinite(shield) || shield < 0.0D) {
            return "dragon shield invalid";
        }
        if (crystalCount <= 0) {
            return "phase-one crystal count invalid";
        }
        if (!Double.isFinite(arenaRadius) || arenaRadius < 10.0D) {
            return "arena radius invalid";
        }
        if (tickPeriodTicks <= 0 || maxFightDurationTicks <= 0L) {
            return "fight cadence invalid";
        }
        return null;
    }

    private EncounterProfile forcedEncounterProfile(DragonType dragonType, MutationType mutation, int tier) {
        EnumMap<DragonType, Integer> influence = new EnumMap<>(DragonType.class);
        for (DragonType type : DragonType.values()) {
            influence.put(type, type == dragonType ? 1 : 0);
        }
        MutationType resolvedMutation = mutation == null ? MutationType.NONE : mutation;
        int resolvedTier = Math.max(1, Math.min(5, tier));
        double mutationChance = resolvedMutation == MutationType.NONE ? 0.0D : 100.0D;
        return new EncounterProfile(dragonType, resolvedMutation, resolvedTier, mutationChance, influence);
    }

    private void prepareEncounterChunks(World world, Location altar) {
        if (world == null || altar == null) {
            return;
        }
        altar.getChunk().load();
        for (int index = 0; index < crystalCount; index++) {
            Location crystalLocation = crystalLocation(altar, index, crystalCount);
            if (crystalLocation != null && crystalLocation.getWorld() != null) {
                crystalLocation.getChunk().load();
            }
        }
    }

    private boolean validateSpawnedDragon(EnderDragon dragon, World world, Location altar, double expectedMaxHealth) {
        if (dragon == null || !dragon.isValid() || dragon.isDead()) {
            return false;
        }
        if (world == null || altar == null || !world.equals(dragon.getWorld())) {
            return false;
        }
        AttributeInstance maxHealthAttribute = dragon.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute == null) {
            return false;
        }
        double configuredMaxHealth = maxHealthAttribute.getBaseValue();
        return Double.isFinite(configuredMaxHealth)
                && Double.isFinite(dragon.getHealth())
                && Math.abs(configuredMaxHealth - expectedMaxHealth) <= 0.001D
                && dragon.getHealth() > 0.0D;
    }

    private void handleCrystalDamage(EntityDamageByEntityEvent event, Player attacker) {
        event.setCancelled(true);
        if (attacker == null) {
            return;
        }
        if (!fight.crystalIds.remove(event.getEntity().getUniqueId())) {
            return;
        }

        event.getEntity().remove();
        PlayerContribution contribution = contribution(fight.contributions, attacker.getUniqueId());
        contribution.crystalsDestroyed++;
        contribution.mechanicScore += 3;
        fight.aerialShield = Math.max(0.0D, fight.aerialShield - (fight.maxAerialShield / Math.max(1, crystalCount)));

        Location location = event.getEntity().getLocation();
        location.getWorld().spawnParticle(Particle.END_ROD, location, 48, 0.55D, 0.7D, 0.55D, 0.02D);
        location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 1.3F);
        attacker.sendMessage(ChatColor.AQUA + "Crystal destroyed. The dragon shield weakens.");
        if (fight.crystalIds.isEmpty() || fight.aerialShield <= 0.0D) {
            transitionToGround();
        }
    }

    private void handleDragonDamage(EntityDamageByEntityEvent event, Player attacker, EnderDragon dragon) {
        if (attacker == null) {
            event.setCancelled(true);
            return;
        }

        PlayerContribution contribution = contribution(fight.contributions, attacker.getUniqueId());
        contribution.activeTicks += tickPeriodTicks;
        double boostedDamage = event.getDamage() * damageBonusMultiplier(reputationLevel(attacker));
        double actualDamage = Math.max(0.0D, Math.min(boostedDamage, dragon.getHealth()));

        if (fight.phase == Phase.AERIAL) {
            event.setCancelled(true);
            if (!fight.crystalIds.isEmpty()) {
                attacker.sendActionBar(ChatColor.RED + "The dragon is invulnerable while crystals remain!");
                return;
            }
            fight.aerialShield = Math.max(0.0D, fight.aerialShield - Math.min(35.0D, actualDamage));
            contribution.mechanicScore += 1;
            attacker.sendActionBar(ChatColor.AQUA + "Shield remaining: " + formatScore(fight.aerialShield));
            if (fight.aerialShield <= 0.0D) {
                transitionToGround();
            }
            return;
        }

        if (fight.bulwarkActive) {
            boostedDamage *= 0.3;
            actualDamage *= 0.3;
            if (ThreadLocalRandom.current().nextDouble() < 0.1) {
                attacker.sendMessage(ChatColor.BLUE + "The dragon's Bulwark heavily reduces your damage!");
            }
        }

        event.setDamage(boostedDamage);
        contribution.damageDealt += actualDamage;
    }

    private void transitionToGround() {
        if (fight == null) {
            return;
        }
        EnderDragon dragon = managedDragon();
        if (dragon == null) {
            finishEncounter(false);
            return;
        }
        fight.phase = Phase.GROUND;
        removeCrystals(fight);
        fight.pendingStrike = null;
        fight.pendingCataclysm = null;
        trySetDragonPhase(dragon, EnderDragon.Phase.HOVER);
        io.papermc.Grivience.util.SkyblockDamageScaleUtil.setHealthSafely(dragon, fight.maxHealth);
        dragon.teleport(fight.altar.clone().add(0.0D, 8.0D, 0.0D));
        dragon.setCustomName(dragonName(fight.profile, fight.phase));
        notifyArena(ChatColor.GOLD + "The dragon descends. Ground phase has begun.");
        dragon.getWorld().playSound(fight.altar, Sound.ENTITY_ENDER_DRAGON_FLAP, 4.0F, 0.7F);
        playTierEncounterEffect(fight.altar, fight.profile.tier(), Phase.GROUND);

        if (fight.profile.dragonType() == DragonType.STRONG) {
            handleStrongTremor();
        }
    }

    private void transitionToAscension() {
        if (fight == null || fight.phase == Phase.ASCENSION) {
            return;
        }
        EnderDragon dragon = managedDragon();
        if (dragon == null) {
            finishEncounter(false);
            return;
        }
        fight.phase = Phase.ASCENSION;
        fight.pendingStrike = null;
        fight.pendingCataclysm = null;
        trySetDragonPhase(dragon, EnderDragon.Phase.STRAFING);
        dragon.setCustomName(dragonName(fight.profile, fight.phase));
        notifyArena(ChatColor.RED + "The dragon mutates in rage. Ascension phase has begun.");
        dragon.getWorld().playSound(fight.altar, Sound.ENTITY_ENDER_DRAGON_HURT, 6.0F, 0.5F);
        playTierEncounterEffect(fight.altar, fight.profile.tier(), Phase.ASCENSION);
        spawnHazard();
    }

    private void aerialVolley() {
        if (fight == null) {
            return;
        }
        List<Player> targets = activeArenaPlayers();
        if (targets.isEmpty()) {
            return;
        }
        int tier = fight.profile.tier();
        int volleys = aerialVolleyCount(tier);
        double splashRadius = aerialVolleySplashRadius(tier);
        double baseDamage = aerialVolleyDamage(tier);
        for (int index = 0; index < volleys; index++) {
            Player target = targets.get(ThreadLocalRandom.current().nextInt(targets.size()));
            Location impact = target.getLocation().clone().add(randomOffset(2.5D));
            World world = impact.getWorld();
            if (world == null) {
                continue;
            }
            world.spawnParticle(Particle.DRAGON_BREATH, impact, tier >= 4 ? 54 : 36, 1.2D, 0.4D, 1.2D, 0.02D);
            if (tier >= 4) {
                world.spawnParticle(tier >= 5 ? Particle.SOUL_FIRE_FLAME : Particle.FLAME, impact.clone().add(0.0D, 0.4D, 0.0D), 20, 0.8D, 0.4D, 0.8D, 0.02D);
                spawnFlashParticle(world, impact.clone().add(0.0D, 0.6D, 0.0D), 1, 0.1D, 0.1D, 0.1D);
            }
            world.playSound(impact, Sound.ENTITY_ENDER_DRAGON_FLAP, tier >= 4 ? 1.1F : 0.8F, tier >= 5 ? 0.8F : 1.3F);
            for (Player player : nearbyPlayers(impact, splashRadius)) {
                damagePlayer(player, baseDamage, managedDragon());
            }
        }
    }

    private void scheduleTargetedStrike() {
        if (fight == null) {
            return;
        }
        Player target = priorityTarget();
        if (target == null) {
            return;
        }
        int tier = fight.profile.tier();
        boolean ascension = fight.phase == Phase.ASCENSION;
        fight.pendingStrike = new TargetedStrike(
                target.getUniqueId(),
                target.getLocation().clone(),
                fight.elapsedTicks + targetedStrikeWarningTicks(tier),
                targetedStrikeRadius(tier, ascension)
        );
        target.sendMessage(ChatColor.RED + "The dragon marks your position. Move.");
        if (tier >= 4) {
            target.sendMessage(ChatColor.GOLD + "Meteor echoes will splinter from the impact.");
        }
        target.playSound(target.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0F, 0.8F);
    }

    private void tickTargetedStrike() {
        if (fight == null || fight.pendingStrike == null) {
            return;
        }

        TargetedStrike strike = fight.pendingStrike;
        World world = strike.location.getWorld();
        int tier = fight.profile.tier();
        boolean ascension = fight.phase == Phase.ASCENSION;
        if (world != null) {
            world.spawnParticle(Particle.ENCHANT, strike.location.clone().add(0.0D, 0.2D, 0.0D), 26, strike.radius, 0.1D, strike.radius, 0.02D);
            world.spawnParticle(Particle.SMOKE, strike.location.clone().add(0.0D, 0.3D, 0.0D), 12, strike.radius * 0.55D, 0.05D, strike.radius * 0.55D, 0.01D);
            previewMeteorEchoes(world, strike.location, tier, ascension);
        }
        if (fight.elapsedTicks < strike.resolveAtTick) {
            return;
        }

        Player target = Bukkit.getPlayer(strike.targetId);
        if (target != null && target.isOnline() && target.getWorld().equals(strike.location.getWorld())
                && target.getLocation().distanceSquared(strike.location) > strike.radius * strike.radius) {
            contribution(fight.contributions, target.getUniqueId()).mechanicScore += 3;
            target.sendMessage(ChatColor.GREEN + "You dodged the dragon strike.");
        }

        if (world != null) {
            world.spawnParticle(Particle.EXPLOSION, strike.location, 3);
            spawnFlashParticle(world, strike.location.clone().add(0.0D, 0.4D, 0.0D), 1, 0.1D, 0.1D, 0.1D);
            world.playSound(strike.location, Sound.ENTITY_GENERIC_EXPLODE, 1.0F, 0.7F);
        }
        for (Player player : nearbyPlayers(strike.location, strike.radius)) {
            damagePlayer(player, targetedStrikeDamage(tier, ascension), managedDragon());
            Vector knockback = player.getLocation().toVector().subtract(strike.location.toVector());
            if (knockback.lengthSquared() > 0.01D) {
                player.setVelocity(knockback.normalize().multiply(0.7D).setY(0.45D));
            }
        }
        resolveMeteorEchoes(strike.location, tier, ascension);
        if (tier >= 5 && ascension) {
            addHazardZone(strike.location.clone(), MutationType.CORRUPTED);
        }
        fight.pendingStrike = null;
    }

    private void spawnHazard() {
        if (fight == null) {
            return;
        }
        List<Player> players = activeArenaPlayers();
        if (players.isEmpty()) {
            return;
        }
        MutationType type = fight.profile.mutation() == MutationType.NONE ? MutationType.CORRUPTED : fight.profile.mutation();
        int clusters = hazardClusterCount(fight.profile.tier());
        for (int index = 0; index < clusters; index++) {
            Player source = players.get(ThreadLocalRandom.current().nextInt(players.size()));
            addHazardZone(source.getLocation().clone(), type);
        }
        if (fight.profile.tier() >= 4) {
            notifyArena(ChatColor.DARK_RED + "Voidfire rifts spread across the arena.");
        } else {
            players.get(ThreadLocalRandom.current().nextInt(players.size())).sendMessage(ChatColor.DARK_RED + "A corruption zone forms beneath the raid.");
        }
    }

    private void tickHazards(long previousElapsedTicks) {
        if (fight == null || fight.hazards.isEmpty()) {
            return;
        }
        List<HazardZone> expired = new ArrayList<>();
        int pulseCount = periodCrossings(previousElapsedTicks, fight.elapsedTicks, scaledPeriod(hazardPulsePeriod(fight.profile.tier())));
        for (HazardZone hazard : fight.hazards) {
            World world = hazard.location.getWorld();
            if (world != null) {
                Particle particle = switch (hazard.type) {
                    case ANCIENT -> Particle.ASH;
                    case FRENZIED -> Particle.FLAME;
                    default -> Particle.DRAGON_BREATH;
                };
                world.spawnParticle(particle, hazard.location.clone().add(0.0D, 0.2D, 0.0D), fight.profile.tier() >= 4 ? 36 : 22, hazard.radius, 0.15D, hazard.radius, 0.02D);
                if (fight.profile.tier() >= 5) {
                    spawnRingParticles(hazard.location.clone().add(0.0D, 0.1D, 0.0D), hazard.radius, Particle.SOUL_FIRE_FLAME, 16);
                }
            }

            if (pulseCount > 0) {
                for (int pulse = 0; pulse < pulseCount; pulse++) {
                    for (Player player : nearbyPlayers(hazard.location, hazard.radius)) {
                        damagePlayer(player, hazardDamage(fight.profile.tier()), managedDragon());
                    }
                }
            }
            if (fight.elapsedTicks >= hazard.expiresAtTick) {
                expired.add(hazard);
            }
        }
        fight.hazards.removeAll(expired);
    }

    private void tickCataclysmPulse() {
        if (fight == null || fight.pendingCataclysm == null) {
            return;
        }

        CataclysmPulse pulse = fight.pendingCataclysm;
        World world = pulse.location.getWorld();
        if (world != null) {
            long totalTicks = Math.max(5L, pulse.resolveAtTick - pulse.startTick);
            double progress = clamp((fight.elapsedTicks - pulse.startTick) / (double) totalTicks, 0.0D, 1.0D);
            double collapsingRadius = pulse.outerRadius - (pulse.outerRadius - pulse.safeRadius) * progress;
            spawnRingParticles(pulse.location.clone().add(0.0D, 0.2D, 0.0D), collapsingRadius, Particle.DRAGON_BREATH, 30);
            spawnRingParticles(pulse.location.clone().add(0.0D, 0.25D, 0.0D), pulse.safeRadius, Particle.END_ROD, 22);
            world.spawnParticle(Particle.REVERSE_PORTAL, pulse.location.clone().add(0.0D, 1.0D, 0.0D), 22, 0.6D, 0.4D, 0.6D, 0.08D);
        }
        if (fight.elapsedTicks < pulse.resolveAtTick) {
            return;
        }

        if (world != null) {
            world.spawnParticle(Particle.EXPLOSION, pulse.location.clone().add(0.0D, 0.6D, 0.0D), 8, 1.4D, 0.4D, 1.4D, 0.02D);
            spawnFlashParticle(world, pulse.location.clone().add(0.0D, 0.8D, 0.0D), 3, 0.4D, 0.3D, 0.4D);
            world.playSound(pulse.location, Sound.ENTITY_WITHER_SPAWN, 1.4F, 0.7F);
            world.playSound(pulse.location, Sound.ENTITY_GENERIC_EXPLODE, 1.6F, 0.6F);
        }

        double safeRadiusSquared = pulse.safeRadius * pulse.safeRadius;
        for (Player player : activeArenaPlayers()) {
            if (!player.getWorld().equals(pulse.location.getWorld())) {
                continue;
            }
            if (horizontalDistanceSquared(player.getLocation(), pulse.location) <= safeRadiusSquared) {
                contribution(fight.contributions, player.getUniqueId()).mechanicScore += 4;
                player.sendMessage(ChatColor.GREEN + "You collapsed into the safe core and withstood the cataclysm.");
                continue;
            }
            damagePlayer(player, cataclysmDamage(fight.profile.tier()), managedDragon());
            Vector knockback = player.getLocation().toVector().subtract(pulse.location.toVector());
            if (knockback.lengthSquared() > 0.01D) {
                player.setVelocity(knockback.normalize().multiply(1.0D).setY(0.45D));
            }
        }
        fight.pendingCataclysm = null;
    }

    private void tickDragonSpecificAbilities(EnderDragon dragon, long previousElapsedTicks) {
        if (fight == null || dragon == null) return;
        DragonType type = fight.profile.dragonType();
        int tier = fight.profile.tier();

        if (type == DragonType.YOUNG) {
            AttributeInstance speed = dragon.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(tier >= 4 ? 1.1 : 0.8);
            }
        }

        if (type == DragonType.UNSTABLE && crossedScaledPeriod(previousElapsedTicks, fight.elapsedTicks, 45L)) {
            handleUnstableLightning();
        }

        if (type == DragonType.WISE && crossedScaledPeriod(previousElapsedTicks, fight.elapsedTicks, 65L)) {
            handleWiseRunicVolley();
        }

        if (type == DragonType.STRONG && fight.phase != Phase.AERIAL && crossedScaledPeriod(previousElapsedTicks, fight.elapsedTicks, 120L)) {
            handleStrongTremor();
        }

        if (type == DragonType.PROTECTOR && crossedScaledPeriod(previousElapsedTicks, fight.elapsedTicks, 180L)) {
            handleProtectorBulwark();
        }
        
        if (type == DragonType.SUPERIOR) {
            if (crossedScaledPeriod(previousElapsedTicks, fight.elapsedTicks, 80L)) {
                handleUnstableLightning();
            }
            if (crossedScaledPeriod(previousElapsedTicks, fight.elapsedTicks, 150L)) {
                handleStrongTremor();
            }
            if (crossedScaledPeriod(previousElapsedTicks, fight.elapsedTicks, 100L)) {
                handleDragonsBreath();
            }
        }
    }

    private void handleDragonsBreath() {
        if (fight == null) return;
        EnderDragon dragon = managedDragon();
        if (dragon == null) return;

        Location head = dragon.getLocation().add(dragon.getLocation().getDirection().multiply(3.0)).add(0, 1.5, 0);
        World world = head.getWorld();
        if (world == null) return;
        world.playSound(head, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.5f);

        Vector direction = dragon.getLocation().getDirection();
        for (int i = 0; i < 12; i++) {
            double dist = i * 2.0;
            Location point = head.clone().add(direction.clone().multiply(dist));
            world.spawnParticle(Particle.DRAGON_BREATH, point, 25, 0.6, 0.6, 0.6, 0.02);
            for (Player player : nearbyPlayers(point, 3.0)) {
                damagePlayer(player, 10.0 + fight.profile.tier() * 2, dragon);
            }
        }
    }

    private void tickCrystalBeams(EnderDragon dragon) {
        if (fight == null || fight.crystalIds.isEmpty() || dragon == null) return;
        Location dragonLoc = dragon.getLocation().add(0, 1.5, 0);
        for (UUID crystalId : fight.crystalIds) {
            Entity crystal = Bukkit.getEntity(crystalId);
            if (crystal instanceof EnderCrystal enderCrystal) {
                enderCrystal.setBeamTarget(dragonLoc);
            }
        }
    }

    private void handleUnstableLightning() {
        if (fight == null) return;
        List<Player> players = activeArenaPlayers();
        if (players.isEmpty()) return;
        World world = fight.altar.getWorld();
        for (Player player : players) {
            if (ThreadLocalRandom.current().nextDouble() < 0.35) {
                Location loc = player.getLocation();
                world.strikeLightningEffect(loc);
                damagePlayer(player, 5.0 + fight.profile.tier() * 1.5, managedDragon());
                player.sendMessage(ChatColor.YELLOW + "Unstable lightning strikes!");
                player.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f, 1.2f);
            }
        }
    }

    private void handleWiseRunicVolley() {
        if (fight == null) return;
        List<Player> players = activeArenaPlayers();
        if (players.isEmpty()) return;
        EnderDragon dragon = managedDragon();
        if (dragon == null) return;
        
        Location start = dragon.getLocation().add(0, 1, 0);
        start.getWorld().spawnParticle(Particle.WITCH, start, 60, 1.2, 1.2, 1.2, 0.1);
        start.getWorld().playSound(start, Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.2f, 1.3f);
        
        for (int i = 0; i < 4; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (fight == null) return;
                aerialVolley(); 
            }, i * 8L);
        }
    }

    private void handleStrongTremor() {
        if (fight == null) return;
        notifyArena(ChatColor.RED + "The dragon creates a powerful tremor!");
        Location altar = fight.altar;
        World world = altar.getWorld();
        world.playSound(altar, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 2.0f, 0.5f);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, altar, 12, 6.0, 0.2, 6.0);
        
        for (Player player : activeArenaPlayers()) {
            Vector vec = player.getLocation().toVector().subtract(altar.toVector());
            if (vec.lengthSquared() < 0.1) vec = new Vector(0, 1, 0);
            player.setVelocity(vec.normalize().multiply(1.8).setY(0.9));
            damagePlayer(player, 12.0 + fight.profile.tier() * 2.5, managedDragon());
            player.sendMessage(ChatColor.RED + "The ground shakes violently!");
        }
    }

    private void handleProtectorBulwark() {
        if (fight == null) return;
        EnderDragon dragon = managedDragon();
        if (dragon == null) return;

        notifyArena(ChatColor.BLUE + "The Protector Dragon activates its Bulwark!");
        dragon.getWorld().playSound(dragon.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.2f, 0.4f);
        fight.bulwarkActive = true;
        
        if (fight.shieldTask != null) fight.shieldTask.cancel();
        
        fight.shieldTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (fight == null || managedDragon() == null || ticks > 160) {
                    if (fight != null) {
                        fight.bulwarkActive = false;
                        if (fight.shieldTask != null) {
                            fight.shieldTask.cancel();
                            fight.shieldTask = null;
                        }
                    }
                    return;
                }
                spawnRingParticles(managedDragon().getLocation().add(0, 1, 0), 4.0, Particle.END_ROD, 24);
                ticks += 5;
            }
        }, 0L, 5L);
    }

    private void scheduleCataclysmPulse() {
        if (fight == null || fight.altar == null || fight.altar.getWorld() == null) {
            return;
        }
        fight.pendingCataclysm = new CataclysmPulse(
                fight.altar.clone(),
                fight.elapsedTicks,
                fight.elapsedTicks + cataclysmWarningTicks(fight.profile.tier()),
                cataclysmSafeRadius(fight.profile.tier()),
                cataclysmOuterRadius(fight.profile.tier())
        );
        World world = fight.altar.getWorld();
        world.spawnParticle(Particle.REVERSE_PORTAL, fight.altar.clone().add(0.0D, 1.0D, 0.0D), 120, 1.1D, 0.8D, 1.1D, 0.12D);
        world.playSound(fight.altar, Sound.ENTITY_ENDER_DRAGON_GROWL, 3.2F, 0.6F);
        notifyArena(ChatColor.DARK_RED + "The dragon gathers a cataclysm. Collapse toward the altar core.");
    }

    private void resolveMeteorEchoes(Location center, int tier, boolean ascension) {
        int echoCount = meteorEchoCount(tier, ascension);
        if (echoCount <= 0 || center == null || center.getWorld() == null) {
            return;
        }

        World world = center.getWorld();
        double radius = meteorEchoRingRadius(tier, ascension);
        double splashRadius = meteorEchoSplashRadius(tier);
        for (int index = 0; index < echoCount; index++) {
            double angle = (Math.PI * 2.0D * index) / echoCount;
            Location echoLocation = center.clone().add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            world.spawnParticle(Particle.EXPLOSION, echoLocation, 2, 0.3D, 0.2D, 0.3D, 0.01D);
            world.spawnParticle(tier >= 5 ? Particle.SOUL_FIRE_FLAME : Particle.FLAME, echoLocation.clone().add(0.0D, 0.35D, 0.0D), 18, 0.6D, 0.25D, 0.6D, 0.02D);
            world.playSound(echoLocation, Sound.ENTITY_GENERIC_EXPLODE, 0.6F, tier >= 5 ? 0.7F : 1.1F);
            for (Player player : nearbyPlayers(echoLocation, splashRadius)) {
                damagePlayer(player, meteorEchoDamage(tier, ascension), managedDragon());
            }
        }
    }

    private void previewMeteorEchoes(World world, Location center, int tier, boolean ascension) {
        int echoCount = meteorEchoCount(tier, ascension);
        if (world == null || center == null || echoCount <= 0) {
            return;
        }

        double radius = meteorEchoRingRadius(tier, ascension);
        for (int index = 0; index < echoCount; index++) {
            double angle = (Math.PI * 2.0D * index) / echoCount;
            Location echoLocation = center.clone().add(Math.cos(angle) * radius, 0.1D, Math.sin(angle) * radius);
            world.spawnParticle(tier >= 5 ? Particle.SOUL_FIRE_FLAME : Particle.FLAME, echoLocation, 5, 0.25D, 0.05D, 0.25D, 0.01D);
        }
    }

    private void addHazardZone(Location location, MutationType type) {
        if (fight == null || location == null) {
            return;
        }
        World world = location.getWorld();
        Location centered = location.clone();
        centered.setY(fight.altar != null ? fight.altar.getY() : centered.getY());
        fight.hazards.add(new HazardZone(centered, hazardRadius(fight.profile.tier()), fight.elapsedTicks + hazardDurationTicks(fight.profile.tier()), type));
        if (world != null) {
            world.spawnParticle(Particle.DRAGON_BREATH, centered.clone().add(0.0D, 0.2D, 0.0D), 28, 1.4D, 0.15D, 1.4D, 0.02D);
            if (fight.profile.tier() >= 4) {
                world.playSound(centered, Sound.ENTITY_BLAZE_SHOOT, 0.7F, fight.profile.tier() >= 5 ? 0.5F : 0.8F);
            }
        }
    }

    private void keepDragonNearArena(EnderDragon dragon, boolean enraged) {
        if (fight == null) {
            return;
        }
        if (!dragon.getWorld().equals(fight.altar.getWorld())) {
            dragon.teleport(fight.altar.clone().add(0.0D, enraged ? 18.0D : 9.0D, 0.0D));
            return;
        }
        if (dragon.getLocation().distanceSquared(fight.altar) > arenaRadius * arenaRadius * 2.25D) {
            dragon.teleport(fight.altar.clone().add(0.0D, enraged ? 18.0D : 9.0D, 0.0D));
        }
    }

    private Set<UUID> distributeRewards(FightSession completedFight) {
        double topDamage = completedFight.contributions.values().stream()
                .mapToDouble(contribution -> contribution.damageDealt)
                .max()
                .orElse(0.0D);
        FightTotals totals = new FightTotals(topDamage);
        Set<UUID> summaryRecipients = new HashSet<>();

        for (Map.Entry<UUID, PlayerContribution> entry : completedFight.contributions.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                continue;
            }

            PlayerContribution contribution = entry.getValue();
            ContributionSnapshot snapshot = contribution.snapshot();
            double score = computeScore(snapshot, totals, scoreWeights);
            boolean meaningfulParticipation = hasMeaningfulParticipation(snapshot);
            RewardTier tier = qualifiesForRewards(snapshot, score, minimumRewardActiveSeconds, minimumRewardScore)
                    ? rewardTier(score, scoreThresholds)
                    : RewardTier.NONE;

            ProgressionRecord progression = progressionFor(player);
            if (meaningfulParticipation) {
                progression.bestScore = Math.max(progression.bestScore, score);
                progression.discoveredTypes.add(completedFight.profile.dragonType());
                if (completedFight.profile.mutation() != MutationType.NONE) {
                    progression.discoveredMutations.add(completedFight.profile.mutation());
                }
                progression.bestScores.merge(completedFight.profile.dragonType(), score, Math::max);
            }
            if (tier != RewardTier.NONE) {
                progression.kills++;
                progression.reputation = Math.min(maxReputation, progression.reputation + reputationPerKill);
                progression.killsByType.merge(completedFight.profile.dragonType(), 1, Integer::sum);
            }

            List<ItemStack> rewards = buildRewards(player, completedFight, tier, meaningfulParticipation);
            for (ItemStack reward : rewards) {
                giveItem(player, reward);
            }

            FightSummary summary = new FightSummary(
                    completedFight.profile.dragonType(),
                    completedFight.profile.mutation(),
                    completedFight.profile.tier(),
                    tier,
                    score,
                    contribution.damageDealt,
                    contribution.crystalsDestroyed,
                    contribution.mechanicScore,
                    contribution.eyesPlaced,
                    contribution.activeTicks,
                    rewards.stream().map(ItemStack::clone).toList(),
                    progression.reputation
            );
            lastSummaryByPlayer.put(player.getUniqueId(), summary);
            summaryRecipients.add(player.getUniqueId());

            if (tier == RewardTier.NONE) {
                if (meaningfulParticipation) {
                    player.sendMessage(ChatColor.YELLOW + "Encounter recorded in your Dragon Codex, but you did not reach the minimum contribution needed for personal loot.");
                } else {
                    player.sendMessage(ChatColor.RED + "You did not reach the minimum contribution needed for personal loot.");
                }
            } else {
                player.sendMessage(ChatColor.GREEN + "Dragon Ascension rewards delivered. Rank " + summary.rewardTierLabel() + ChatColor.GREEN + ".");
            }
        }
        saveProgression();
        return summaryRecipients;
    }

    private List<ItemStack> buildRewards(Player player, FightSession completedFight, RewardTier tier, boolean meaningfulParticipation) {
        List<ItemStack> rewards = new ArrayList<>();
        if (tier != RewardTier.NONE) {
            int scaleCount = switch (tier) {
                case S -> 8;
                case A -> 6;
                case B -> 4;
                case C -> 2;
                default -> 0;
            } + Math.max(0, completedFight.profile.tier() - 1);
            int materialCount = switch (tier) {
                case S -> 5;
                case A -> 4;
                case B -> 3;
                case C -> 2;
                default -> 0;
            };

            rewards.add(customReward("DRAGON_SCALE", scaleCount, Material.DRAGON_BREATH, ChatColor.LIGHT_PURPLE + "Dragon Scale"));
            rewards.add(customReward(completedFight.profile.dragonType().rewardKey, materialCount + completedFight.profile.tier() - 1, Material.END_STONE, completedFight.profile.dragonType().displayName()));
            rewards.add(customReward("ASCENSION_SHARD", ascensionShardAmount(completedFight, tier), Material.END_CRYSTAL, ChatColor.LIGHT_PURPLE + "Ascension Shard"));
            if (completedFight.profile.mutation() != MutationType.NONE) {
                rewards.add(customReward(completedFight.profile.mutation().rewardKey, Math.max(1, materialCount - 1), Material.OBSIDIAN, completedFight.profile.mutation().displayName()));
            }
            if (completedFight.profile.dragonType() == DragonType.SUPERIOR) {
                rewards.add(customReward("KUNZITE", 1 + completedFight.profile.tier() / 2, Material.AMETHYST_SHARD, ChatColor.LIGHT_PURPLE + "Kunzite"));
            }
            if (shouldDropDragonHeart(completedFight)) {
                rewards.add(customReward("DRAGON_HEART", 1, Material.NETHER_STAR, ChatColor.RED + "Dragon Heart"));
            }
            if (shouldDropVoidwingRelic(player, completedFight, tier)) {
                rewards.add(customReward("VOIDWING_RELIC", 1, Material.DRAGON_BREATH, ChatColor.GOLD + "Voidwing Relic"));
            }
            if (shouldDropDragonTrackerEnchant(player, completedFight, tier)) {
                SkyblockEnchantment tracker = EnchantmentRegistry.get("dragon_tracker");
                if (tracker != null) {
                    ItemStack book = new SkyblockEnchantStorage(plugin).createEnchantedBook(tracker, 1);
                    if (book != null) {
                        rewards.add(book);
                    }
                }
            }
        }
        if (isDragonsSpineEligible(completedFight.profile.tier(), meaningfulParticipation) && shouldDropDragonsSpine(completedFight)) {
            rewards.add(customReward("DRAGONS_SPINE", 1, Material.BONE, ChatColor.GOLD + "Dragon's Spine"));
        }
        if (tier != RewardTier.NONE) {
            rewards.addAll(buildDragonSlayerDrops());
        }

        return rewards;
    }

    private int ascensionShardAmount(FightSession completedFight, RewardTier tier) {
        int baseAmount = switch (tier) {
            case S -> 4;
            case A -> 3;
            case B -> 2;
            case C -> 1;
            default -> 0;
        };
        baseAmount += Math.max(0, completedFight.profile.tier() - 2);
        if (completedFight.profile.mutation() != MutationType.NONE) {
            baseAmount++;
        }
        return Math.max(1, baseAmount);
    }

    private List<ItemStack> buildDragonSlayerDrops() {
        List<ItemStack> drops = new ArrayList<>();
        List<String> availableKeys = new ArrayList<>(DRAGON_SLAYER_ARMOR_KEYS);
        java.util.Collections.shuffle(availableKeys, ThreadLocalRandom.current());

        if (!availableKeys.isEmpty() && ThreadLocalRandom.current().nextDouble() < rareArmorChance) {
            addDragonSlayerPiece(drops, availableKeys.remove(0));
        }
        return drops;
    }

    private void addDragonSlayerPiece(List<ItemStack> rewards, String key) {
        ItemStack armor = customItemService.createItemByKey(key);
        if (armor != null && armor.getType() != Material.AIR) {
            rewards.add(armor);
        }
    }

    private boolean shouldDropDragonHeart(FightSession completedFight) {
        return shouldDropDragonHeart(completedFight.profile.tier(), ThreadLocalRandom.current().nextDouble());
    }

    static boolean shouldDropDragonHeart(int dragonTier, double roll) {
        return dragonTier >= 5 && roll < DRAGON_HEART_DROP_CHANCE;
    }

    private boolean shouldDropVoidwingRelic(Player player, FightSession completedFight, RewardTier tier) {
        double chance = voidwingRelicBaseChance
                + completedFight.profile.tier() * 0.01D
                + (tier == RewardTier.S ? 0.05D : tier == RewardTier.A ? 0.02D : 0.0D)
                + (completedFight.profile.mutation() == MutationType.CORRUPTED ? 0.04D : 0.0D)
                + (completedFight.profile.dragonType() == DragonType.SUPERIOR ? 0.03D : 0.0D);
        chance = clamp(chance * lootChanceMultiplier(reputationLevel(player)), 0.0D, 0.40D);
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    private boolean shouldDropDragonTrackerEnchant(Player player, FightSession completedFight, RewardTier tier) {
        double chance = DRAGON_TRACKER_ENCHANT_DROP_CHANCE
                + completedFight.profile.tier() * 0.0005D
                + (tier == RewardTier.S ? 0.001D : tier == RewardTier.A ? 0.0005D : 0.0D);
        chance *= lootChanceMultiplier(reputationLevel(player));
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    private boolean shouldDropDragonsSpine(FightSession completedFight) {
        return shouldDropDragonsSpine(completedFight.profile.tier(), ThreadLocalRandom.current().nextDouble());
    }

    static boolean shouldDropDragonsSpine(int dragonTier, double roll) {
        return dragonTier >= 4 && roll < DRAGONS_SPINE_DROP_CHANCE;
    }

    static boolean isDragonsSpineEligible(int dragonTier, boolean meaningfulParticipation) {
        return meaningfulParticipation && dragonTier >= 4;
    }

    private ItemStack customReward(String key, int amount, Material fallbackMaterial, String fallbackName) {
        ItemStack item = customItemService.createItemByKey(key);
        if (item == null || item.getType() == Material.AIR) {
            item = infoItem(fallbackMaterial, fallbackName, List.of(ChatColor.GRAY + "Fallback reward item"));
        }
        item.setAmount(Math.max(1, Math.min(item.getMaxStackSize(), amount)));
        return item;
    }

    private void giveItem(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        if (leftovers.isEmpty()) {
            return;
        }
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private void trackActivePresence() {
        if (fight == null) {
            return;
        }
        for (Player player : activeArenaPlayers()) {
            contribution(fight.contributions, player.getUniqueId()).activeTicks += tickPeriodTicks;
        }
    }

    private List<Player> activeArenaPlayers() {
        if (fight == null) {
            return List.of();
        }
        return nearbyPlayers(fight.altar, arenaRadius).stream()
                .filter(player -> player.isOnline() && !player.isDead())
                .toList();
    }

    private Player priorityTarget() {
        if (fight == null) {
            return null;
        }
        return activeArenaPlayers().stream()
                .max(Comparator.comparingDouble(player -> fight.contributions.getOrDefault(player.getUniqueId(), new PlayerContribution()).damageDealt))
                .orElseGet(() -> {
                    List<Player> players = activeArenaPlayers();
                    if (players.isEmpty()) {
                        return null;
                    }
                    return players.get(ThreadLocalRandom.current().nextInt(players.size()));
                });
    }

    private void removeCrystals(FightSession session) {
        if (session == null) {
            return;
        }
        for (UUID crystalId : new HashSet<>(session.crystalIds)) {
            Entity entity = Bukkit.getEntity(crystalId);
            if (entity != null) {
                entity.remove();
            }
        }
        session.crystalIds.clear();
    }

    private void notifySummoners(String message) {
        if (summoning == null) {
            return;
        }
        for (UUID playerId : summoning.contributions.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }

    private void notifyArena(String message) {
        Location altar = fight != null ? fight.altar : altarLocation(resolvedWorld());
        for (Player player : nearbyPlayers(altar, arenaRadius * 1.6D)) {
            player.sendMessage(message);
        }
    }

    private void queueSummaryOpen(UUID playerId, int attempt) {
        if (playerId == null || !lastSummaryByPlayer.containsKey(playerId)) {
            return;
        }

        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }
        if (isDragonView(player.getOpenInventory().getTitle(), GuiView.SUMMARY)) {
            return;
        }

        player.closeInventory();
        openSummary(player);

        if (attempt + 1 >= SUMMARY_OPEN_RETRIES) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player retryPlayer = Bukkit.getPlayer(playerId);
            if (retryPlayer == null || !retryPlayer.isOnline()) {
                return;
            }
            if (isDragonView(retryPlayer.getOpenInventory().getTitle(), GuiView.SUMMARY)) {
                return;
            }
            queueSummaryOpen(playerId, attempt + 1);
        }, SUMMARY_OPEN_RETRY_DELAY_TICKS);
    }

    private String dragonTypeLockStatus() {
        if (summoning == null || summoning.lockedDragonType == null) {
            return ChatColor.YELLOW + "Locks at " + dragonTypeLockEyes + "/" + requiredEyes + " eyes";
        }
        return ChatColor.GREEN + "Locked at " + summoning.lockedAtEyeCount + "/" + requiredEyes;
    }

    private String tierStatusLabel(EncounterProfile predictedProfile) {
        String rarityLabel = rareTierChanceLabel(predictedProfile.tier());
        if (summoning == null || summoning.lockedTier == null) {
            return "Tier estimate: " + ChatColor.GOLD + "Peak Tier " + predictedProfile.tier()
                    + (rarityLabel.isEmpty() ? "" : ChatColor.GRAY + " (" + rarityLabel + ChatColor.GRAY + ")");
        }
        return "Tier lock: " + ChatColor.GREEN + "Peak at "
                + Math.max(1, summoning.tierLockedAtEyeCount) + "/" + requiredEyes
                + ChatColor.GRAY + " -> " + ChatColor.GOLD + "Tier " + summoning.lockedTier
                + (rarityLabel.isEmpty() ? "" : ChatColor.GRAY + " (" + rarityLabel + ChatColor.GRAY + ")");
    }

    private String lockStatusSuffix() {
        if (summoning == null || summoning.lockedDragonType == null) {
            return "";
        }
        return ChatColor.DARK_GRAY + " [" + ChatColor.GREEN + "Locked" + ChatColor.DARK_GRAY + "]";
    }

    private List<Player> nearbyPlayers(Location origin, double radius) {
        if (origin == null || origin.getWorld() == null) {
            return List.of();
        }
        double radiusSquared = radius * radius;
        List<Player> players = new ArrayList<>();
        for (Player player : origin.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(origin) <= radiusSquared) {
                players.add(player);
            }
        }
        return players;
    }

    private void syncBossBarPlayers(BossBar bossBar, Collection<Player> desiredPlayers) {
        Set<UUID> desiredIds = new HashSet<>();
        for (Player player : desiredPlayers) {
            desiredIds.add(player.getUniqueId());
            if (!bossBar.getPlayers().contains(player)) {
                bossBar.addPlayer(player);
            }
        }
        for (Player viewer : new ArrayList<>(bossBar.getPlayers())) {
            if (!desiredIds.contains(viewer.getUniqueId())) {
                bossBar.removePlayer(viewer);
            }
        }
    }

    private boolean isInArena(Player player, Location altar, double radius) {
        return altar != null
                && player != null
                && player.getWorld().equals(altar.getWorld())
                && player.getLocation().distanceSquared(altar) <= radius * radius;
    }

    private boolean isManagedDragon(Entity entity) {
        return resolveManagedDragon(entity) != null;
    }

    private boolean isManagedCrystal(Entity entity) {
        return entity != null && fight != null && fight.crystalIds.contains(entity.getUniqueId());
    }

    private EnderDragon resolveManagedDragon(Entity entity) {
        if (entity == null || fight == null) {
            return null;
        }
        if (entity.getUniqueId().equals(fight.dragonId) && entity instanceof EnderDragon dragon) {
            return dragon;
        }
        if (entity instanceof EnderDragonPart dragonPart) {
            EnderDragon parent = dragonPart.getParent();
            if (parent != null && parent.getUniqueId().equals(fight.dragonId)) {
                return parent;
            }
        }
        return null;
    }

    private boolean isFightDamageSource(Entity damager) {
        if (damager == null || fight == null) {
            return false;
        }
        if (isManagedDragon(damager)) {
            return true;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity entity) {
            return isManagedDragon(entity);
        }
        return false;
    }

    private EnderDragon managedDragon() {
        if (fight == null || fight.dragonId == null) {
            return null;
        }
        Entity entity = Bukkit.getEntity(fight.dragonId);
        return entity instanceof EnderDragon dragon ? dragon : null;
    }

    private Player attackingPlayer(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    private World resolvedWorld() {
        if (!configuredWorldName.isBlank()) {
            World world = Bukkit.getWorld(configuredWorldName);
            if (world != null) {
                return world;
            }
        }
        if (!altarWorldName.isBlank()) {
            World world = Bukkit.getWorld(altarWorldName);
            if (world != null) {
                return world;
            }
        }
        return Bukkit.getWorlds().stream()
                .filter(world -> world.getEnvironment() == World.Environment.THE_END)
                .findFirst()
                .orElse(null);
    }

    private Location altarLocation(World world) {
        if (world == null) {
            return null;
        }
        return new Location(world, altarX, altarY, altarZ, altarYaw, altarPitch);
    }

    private String altarSummary() {
        World world = resolvedWorld();
        if (world == null) {
            return ChatColor.RED + "Unresolved";
        }
        return world.getName() + " " + Math.round(altarX) + " " + Math.round(altarY) + " " + Math.round(altarZ);
    }

    private int pedestalIndex(Location clicked) {
        World world = resolvedWorld();
        if (clicked == null || world == null || !world.equals(clicked.getWorld())) {
            return -1;
        }
        for (int index = 0; index < requiredEyes; index++) {
            Location pedestal = pedestalLocation(world, index);
            if (pedestal != null) {
                double distanceSquared = horizontalDistanceSquared(pedestal, clicked);
                if (distanceSquared <= 2.0D && Math.abs(clicked.getY() - pedestal.getY()) <= 2.0D) {
                    return index;
                }
            }
        }
        return -1;
    }

    private Location pedestalLocation(World world, int index) {
        Location configuredPoint = altarPoints.get(index);
        if (configuredPoint != null) {
            return configuredPoint.clone();
        }
        Location altar = altarLocation(world);
        if (altar == null) {
            return null;
        }
        double angle = (-Math.PI / 2.0D) + (Math.PI * 2.0D * index / requiredEyes);
        double x = altar.getX() + Math.cos(angle) * pedestalRadius;
        double z = altar.getZ() + Math.sin(angle) * pedestalRadius;
        return new Location(world, x, altar.getY(), z);
    }

    private Location crystalLocation(Location altar, int index, int total) {
        Location configuredPoint = altarPoints.get(index);
        if (configuredPoint != null) {
            return configuredPoint.clone().add(0.0D, 4.0D + (index % 2), 0.0D);
        }
        double angle = Math.PI * 2.0D * index / total;
        double radius = pedestalRadius - 1.2D;
        return altar.clone().add(Math.cos(angle) * radius, 4.0D + (index % 2), Math.sin(angle) * radius);
    }

    private void saveAltarPoint(int index, Location location) {
        altarPoints.put(index, location.clone());
        String base = "dragon-ascension.altar.points." + (index + 1) + ".";
        if (location.getWorld() != null && (configuredWorldName.isBlank() || altarWorldName.isBlank())) {
            plugin.getConfig().set("dragon-ascension.world-name", location.getWorld().getName());
            plugin.getConfig().set("dragon-ascension.altar.world", location.getWorld().getName());
            configuredWorldName = location.getWorld().getName();
            altarWorldName = location.getWorld().getName();
        }
        plugin.getConfig().set(base + "world", location.getWorld() != null ? location.getWorld().getName() : "");
        plugin.getConfig().set(base + "x", location.getX());
        plugin.getConfig().set(base + "y", location.getY());
        plugin.getConfig().set(base + "z", location.getZ());
        plugin.saveConfig();
        refreshAltarPointHolograms();
    }

    private int configuredPointCount() {
        return altarPoints.size();
    }

    private boolean isSummoningEye(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        String itemId = customItemService.itemId(item);
        if (itemId != null) {
            return itemId.equalsIgnoreCase(summonItemKey);
        }
        return summonItemKey.isBlank() && item.getType() == summonMaterial;
    }

    private void playEyePlacementCue(Player player, Location pedestal, PedestalAspect aspect, int pedestalIndex) {
        if (pedestal == null || pedestal.getWorld() == null) {
            return;
        }

        World world = pedestal.getWorld();
        ItemStack displayEye = customItemService.createItemByKey(summonItemKey);
        if (displayEye != null && displayEye.getType() != Material.AIR) {
            world.spawnParticle(Particle.ITEM, pedestal.clone().add(0.0D, 1.15D, 0.0D), 18, 0.18D, 0.20D, 0.18D, 0.02D, displayEye);
        }
        world.spawnParticle(Particle.PORTAL, pedestal.clone().add(0.0D, 1.15D, 0.0D), 48, 0.28D, 0.40D, 0.28D, 0.02D);
        world.spawnParticle(Particle.END_ROD, pedestal.clone().add(0.0D, 1.05D, 0.0D), 24, 0.18D, 0.35D, 0.18D, 0.01D);
        world.playSound(pedestal, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0F, 1.15F);
        world.playSound(pedestal, Sound.BLOCK_BEACON_POWER_SELECT, 0.9F, 1.55F);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.35F);
        player.sendActionBar(ChatColor.LIGHT_PURPLE + "Summoning Eye placed at altar point "
                + ChatColor.WHITE + (pedestalIndex + 1)
                + ChatColor.DARK_GRAY + "/"
                + ChatColor.WHITE + requiredEyes
                + ChatColor.GRAY + " [" + aspect.displayName + ChatColor.GRAY + "]");
    }

    private void refreshAltarPointHolograms() {
        removeAltarPointHolograms();
        for (int index = 0; index < requiredEyes; index++) {
            Location point = altarPoints.get(index);
            if (point == null || point.getWorld() == null) {
                continue;
            }
            String text = pointHologramText(index);
            ArmorStand hologram = point.getWorld().spawn(point.clone().add(0.0D, 2.1D, 0.0D), ArmorStand.class, stand -> {
                stand.setVisible(false);
                stand.setMarker(true);
                stand.setGravity(false);
                stand.setSmall(true);
                stand.setCustomNameVisible(true);
                stand.setCustomName(text);
                stand.setInvulnerable(true);
                stand.setPersistent(false);
                stand.setSilent(true);
            });
            altarPointHologramIds.put(index, hologram.getUniqueId());
        }
    }

    private void refreshAltarPointEyeDisplays() {
        removeAltarPointEyeDisplays();
        if (summoning == null || summoning.eyesByPedestal.isEmpty()) {
            return;
        }

        ItemStack displayItem = customItemService.createItemByKey(summonItemKey);
        if (displayItem == null || displayItem.getType() == Material.AIR) {
            return;
        }

        for (Map.Entry<Integer, PlacedEye> entry : summoning.eyesByPedestal.entrySet()) {
            Location point = altarPoints.get(entry.getKey());
            if (point == null || point.getWorld() == null) {
                continue;
            }

            ArmorStand stand = point.getWorld().spawn(point.clone().add(0.0D, 0.15D, 0.0D), ArmorStand.class, armorStand -> {
                armorStand.setVisible(false);
                armorStand.setMarker(true);
                armorStand.setGravity(false);
                armorStand.setSmall(true);
                armorStand.setInvulnerable(true);
                armorStand.setPersistent(false);
                armorStand.setSilent(true);
                armorStand.setBasePlate(false);
                armorStand.setArms(false);
                armorStand.setCustomNameVisible(false);
                if (armorStand.getEquipment() != null) {
                    armorStand.getEquipment().setHelmet(displayItem.clone());
                }
            });
            altarPointEyeDisplayIds.put(entry.getKey(), stand.getUniqueId());
        }
    }

    private String pointHologramText(int index) {
        String aspectName = pedestalAspects.get(Math.min(index, pedestalAspects.size() - 1)).displayName;
        boolean occupied = summoning != null && summoning.eyesByPedestal.containsKey(index);
        if (occupied) {
            return ChatColor.GREEN + "Eye Placed" + ChatColor.DARK_GRAY + " [" + ChatColor.WHITE + (index + 1) + ChatColor.DARK_GRAY + "]";
        }
        return aspectName + ChatColor.GRAY + " - " + ChatColor.LIGHT_PURPLE + "Place Summoning Eye Here";
    }

    private void removeAltarPointHolograms() {
        for (UUID hologramId : altarPointHologramIds.values()) {
            Entity entity = Bukkit.getEntity(hologramId);
            if (entity != null) {
                entity.remove();
            }
        }
        altarPointHologramIds.clear();
    }

    private void removeAltarPointEyeDisplays() {
        for (UUID displayId : altarPointEyeDisplayIds.values()) {
            Entity entity = Bukkit.getEntity(displayId);
            if (entity != null) {
                entity.remove();
            }
        }
        altarPointEyeDisplayIds.clear();
    }

    private EncounterProfile currentEncounterProfile() {
        List<PlacedEye> placedEyes = summoning == null ? List.of() : new ArrayList<>(summoning.eyesByPedestal.values());
        DragonType lockedDragonType = summoning == null ? null : summoning.lockedDragonType;
        Integer lockedTier = summoning == null ? null : summoning.lockedTier;
        return resolvePreviewProfile(placedEyes, lockedDragonType, lockedTier);
    }

    private EncounterProfile resolveProfile(List<PlacedEye> placedEyes) {
        return resolvePreviewProfile(placedEyes, null, null);
    }

    private EncounterProfile resolvePreviewProfile(List<PlacedEye> placedEyes, DragonType lockedDragonType, Integer lockedTier) {
        return resolveProfile(placedEyes, lockedDragonType, lockedTier, false);
    }

    private EncounterProfile resolveSpawnProfile(List<PlacedEye> placedEyes, DragonType lockedDragonType, Integer lockedTier) {
        return resolveProfile(placedEyes, lockedDragonType, lockedTier, true);
    }

    private EncounterProfile resolveProfile(List<PlacedEye> placedEyes, DragonType lockedDragonType, Integer lockedTier, boolean actualSpawn) {
        EnumMap<DragonType, Integer> influence = new EnumMap<>(DragonType.class);
        for (DragonType type : DragonType.values()) {
            influence.put(type, 0);
        }

        double powerTotal = 0.0D;
        double mutationChance = baseMutationChance;
        Set<PedestalAspect> diversity = EnumSet.noneOf(PedestalAspect.class);

        for (PlacedEye eye : placedEyes) {
            diversity.add(eye.aspect);
            powerTotal += eye.aspect.powerValue;
            mutationChance += eye.aspect.mutationContribution;
            eye.aspect.applyInfluence(influence);
        }

        DragonType dragonType = lockedDragonType == null
                ? resolveDominantDragonType(influence)
                : lockedDragonType;

        double averagePower = placedEyes.isEmpty() ? 1.0D : powerTotal / placedEyes.size();
        int peakTier = lockedTier == null
                ? resolveTier(averagePower, placedEyes.size())
                : lockedTier;
        int tier = actualSpawn
                ? resolveEncounterTierForSpawn(peakTier, ThreadLocalRandom.current().nextDouble(), ThreadLocalRandom.current().nextDouble(), tierFourSpawnChance, tierFiveSpawnChance)
                : peakTier;

        mutationChance = mutationChanceForTier(mutationChance, diversity.size(), tier);

        MutationType mutation = MutationType.NONE;
        if (actualSpawn && !placedEyes.isEmpty() && ThreadLocalRandom.current().nextDouble(100.0D) < mutationChance) {
            mutation = chooseMutation(placedEyes, dragonType);
        }

        return new EncounterProfile(dragonType, mutation, tier, mutationChance, influence);
    }

    private EncounterLock maybeUpdateSummoningEncounterLock() {
        if (summoning == null
                || summoning.eyesByPedestal.size() < dragonTypeLockEyes) {
            return null;
        }
        List<PlacedEye> placedEyes = new ArrayList<>(summoning.eyesByPedestal.values());
        boolean typeLocked = false;
        if (summoning.lockedDragonType == null) {
            summoning.lockedDragonType = resolveDominantDragonType(placedEyes);
            summoning.lockedAtEyeCount = summoning.eyesByPedestal.size();
            typeLocked = true;
        }
        double powerTotal = placedEyes.stream().mapToDouble(eye -> eye.aspect.powerValue).sum();
        int candidateTier = resolveTier(powerTotal / Math.max(1, placedEyes.size()), placedEyes.size());
        boolean tierChanged = false;
        if (summoning.lockedTier == null || candidateTier > summoning.lockedTier) {
            summoning.lockedTier = candidateTier;
            summoning.tierLockedAtEyeCount = summoning.eyesByPedestal.size();
            tierChanged = true;
        }
        if (!typeLocked && !tierChanged) {
            return null;
        }
        return new EncounterLock(summoning.lockedDragonType, summoning.lockedTier, typeLocked, tierChanged);
    }

    private int resolveTier(double averagePower, int placedEyeCount) {
        return resolveTier(averagePower, pedestalAspects.stream().map(aspect -> aspect.powerValue).toList(), placedEyeCount);
    }

    static int resolveTier(double averagePower, List<Double> powerValues, int placedEyeCount) {
        if (placedEyeCount <= 0 || powerValues == null || powerValues.isEmpty()) {
            return 1;
        }

        double minPower = powerValues.stream()
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(averagePower);
        double maxPower = powerValues.stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(averagePower);
        if (Math.abs(maxPower - minPower) < 1.0E-6D) {
            return 3;
        }

        // Normalize against the altar's full power range so 8/8 summons do not collapse
        // back to a mid-tier result when weaker mandatory pedestals are added.
        double normalized = clamp((averagePower - minPower) / (maxPower - minPower), 0.0D, 1.0D);
        return Math.min(5, 1 + (int) Math.floor(normalized * 5.0D));
    }

    // Tier 4 and Tier 5 are encoded as exact encounter odds, not nested percentages.
    // For a peak Tier 5 summon:
    // - Tier 5 occurs with exactly tierFiveChance.
    // - Tier 4 occurs with exactly tierFourChance.
    // - Tier 3 receives the remaining probability budget.
    // Because Tier 5 is checked first, the Tier 4 roll is converted to a conditional
    // threshold across the non-Tier-5 remainder so the final encounter odds stay exact.
    static int resolveEncounterTierForSpawn(int peakTier, double tierFourRoll, double tierFiveRoll, double tierFourChance, double tierFiveChance) {
        int normalizedPeakTier = Math.max(1, Math.min(5, peakTier));
        if (normalizedPeakTier < 4) {
            return normalizedPeakTier;
        }

        double normalizedTierFourChance = tierFourEncounterChance(normalizedPeakTier, tierFourChance);
        if (normalizedPeakTier == 4) {
            return tierFourRoll < normalizedTierFourChance ? 4 : 3;
        }

        double normalizedTierFiveChance = tierFiveEncounterChance(normalizedPeakTier, tierFourChance, tierFiveChance);
        if (tierFiveRoll < normalizedTierFiveChance) {
            return 5;
        }
        if (tierFourRoll < tierFourConditionalRollChance(normalizedTierFourChance, normalizedTierFiveChance)) {
            return 4;
        }
        return 3;
    }

    static double tierFourEncounterChance(int peakTier, double tierFourChance) {
        return peakTier >= 4 ? sanitizeRareTierChance(tierFourChance) : 0.0D;
    }

    static double tierFiveEncounterChance(int peakTier, double tierFourChance, double tierFiveChance) {
        if (peakTier < 5) {
            return 0.0D;
        }
        double normalizedTierFourChance = sanitizeRareTierChance(tierFourChance);
        return Math.min(Math.max(0.0D, 1.0D - normalizedTierFourChance), sanitizeRareTierChance(tierFiveChance));
    }

    static double tierFourConditionalRollChance(double tierFourChance, double tierFiveChance) {
        double normalizedTierFourChance = sanitizeRareTierChance(tierFourChance);
        double normalizedTierFiveChance = sanitizeRareTierChance(tierFiveChance);
        double remainingProbability = Math.max(0.0D, 1.0D - normalizedTierFiveChance);
        if (remainingProbability <= 0.0D) {
            return 0.0D;
        }
        return clamp(normalizedTierFourChance / remainingProbability, 0.0D, 1.0D);
    }

    static int clampDragonTypeLockEyes(int requiredEyes, int configuredLockEyes) {
        int maximumMeaningfulLockEyes = Math.max(1, Math.min(requiredEyes, (requiredEyes + 1) / 2));
        return Math.max(1, Math.min(maximumMeaningfulLockEyes, configuredLockEyes));
    }

    static double dragonMaxHealthForTier(double baseHealth, double healthPerTier, int tier) {
        int effectiveTier = Math.max(1, tier);
        double health = Math.max(1.0D, baseHealth) + Math.max(0.0D, healthPerTier) * (effectiveTier - 1);
        if (effectiveTier == 4) {
            health *= 1.5D;
        } else if (effectiveTier >= 5) {
            health *= 2.5D;
        }
        return health;
    }

    static double dragonShieldForTier(double baseShield, double shieldPerTier, int tier) {
        int effectiveTier = Math.max(1, tier);
        return Math.max(0.0D, baseShield) + Math.max(0.0D, shieldPerTier) * (effectiveTier - 1);
    }

    static long aerialVolleyBasePeriod(int tier) {
        return tier >= 5 ? 50L : tier >= 4 ? 65L : 80L;
    }

    static long targetedStrikeBasePeriod(int tier, boolean ascension) {
        if (tier >= 5) {
            return ascension ? 35L : 45L;
        }
        if (tier >= 4) {
            return ascension ? 45L : 55L;
        }
        return ascension ? 55L : 70L;
    }

    static long hazardBasePeriod(int tier) {
        return tier >= 5 ? 40L : tier >= 4 ? 50L : 60L;
    }

    static int aerialVolleyCount(int tier) {
        return tier >= 5 ? 5 : tier >= 4 ? 4 : 3;
    }

    static int hazardClusterCount(int tier) {
        return tier >= 5 ? 3 : tier >= 4 ? 2 : 1;
    }

    static int meteorEchoCount(int tier, boolean ascension) {
        if (tier < 4) {
            return 0;
        }
        if (tier >= 5) {
            return ascension ? 4 : 3;
        }
        return ascension ? 3 : 2;
    }

    static boolean cataclysmEnabled(int tier) {
        return tier >= 5;
    }

    static long cataclysmBasePeriod(int tier) {
        return cataclysmEnabled(tier) ? 90L : Long.MAX_VALUE;
    }

    private void announceTierMechanics(int tier) {
        if (tier >= 5) {
            notifyArena(ChatColor.DARK_RED + "Tier 5 rage: meteor storms, voidfire rifts, and cataclysm collapses now define the raid.");
            return;
        }
        if (tier >= 4) {
            notifyArena(ChatColor.GOLD + "Tier 4 omen: impacts splinter into meteor echoes and rifts begin to spread.");
        }
    }

    private void playTierEncounterEffect(Location center, int tier, Phase phase) {
        if (center == null || center.getWorld() == null || tier < 4) {
            return;
        }

        World world = center.getWorld();
        world.spawnParticle(Particle.REVERSE_PORTAL, center.clone().add(0.0D, 1.0D, 0.0D), tier >= 5 ? 140 : 90, 2.0D, 1.0D, 2.0D, 0.08D);
        world.spawnParticle(tier >= 5 ? Particle.SOUL_FIRE_FLAME : Particle.END_ROD, center.clone().add(0.0D, 0.6D, 0.0D), tier >= 5 ? 52 : 34, 1.8D, 0.6D, 1.8D, 0.03D);
        spawnRingParticles(center.clone().add(0.0D, 0.25D, 0.0D), tier >= 5 ? 8.5D : 6.5D, tier >= 5 ? Particle.SOUL_FIRE_FLAME : Particle.END_ROD, tier >= 5 ? 26 : 18);
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, tier >= 5 ? 5.5F : 3.8F, phase == Phase.ASCENSION ? 0.6F : 0.85F);
        if (tier >= 5) {
            world.playSound(center, Sound.ENTITY_WITHER_SPAWN, 1.0F, 0.85F);
            spawnFlashParticle(world, center.clone().add(0.0D, 0.8D, 0.0D), 3, 0.3D, 0.2D, 0.3D);
        }
    }

    private DragonType resolveDominantDragonType(List<PlacedEye> placedEyes) {
        EnumMap<DragonType, Integer> influence = new EnumMap<>(DragonType.class);
        for (DragonType type : DragonType.values()) {
            influence.put(type, 0);
        }
        for (PlacedEye eye : placedEyes) {
            eye.aspect.applyInfluence(influence);
        }
        return resolveDominantDragonType(influence);
    }

    private DragonType resolveDominantDragonType(EnumMap<DragonType, Integer> influence) {
        DragonType dominantType = DragonType.YOUNG;
        int highestInfluence = Integer.MIN_VALUE;
        for (DragonType type : DragonType.values()) {
            int amount = influence.getOrDefault(type, 0);
            if (amount > highestInfluence) {
                highestInfluence = amount;
                dominantType = type;
            }
        }
        return dominantType;
    }

    private MutationType chooseMutation(List<PlacedEye> placedEyes, DragonType dragonType) {
        int frenziedBias = 0;
        int ancientBias = 0;
        int corruptedBias = 0;
        for (PlacedEye eye : placedEyes) {
            switch (eye.aspect) {
                case STRONG, UNSTABLE -> frenziedBias += 3;
                case WISE, PROTECTOR, ANCIENT -> ancientBias += 3;
                case CHAOS -> corruptedBias += 3;
                case SUPERIOR -> {
                    ancientBias += 1;
                    frenziedBias += 1;
                    corruptedBias += 1;
                }
                default -> corruptedBias += 1;
            }
        }
        if (dragonType == DragonType.UNSTABLE || dragonType == DragonType.STRONG) {
            frenziedBias += 2;
        } else if (dragonType == DragonType.WISE || dragonType == DragonType.PROTECTOR || dragonType == DragonType.SUPERIOR) {
            ancientBias += 2;
        } else {
            corruptedBias += 1;
        }

        int total = frenziedBias + ancientBias + corruptedBias;
        if (total <= 0) {
            return MutationType.CORRUPTED;
        }
        int roll = ThreadLocalRandom.current().nextInt(total);
        if (roll < frenziedBias) {
            return MutationType.FRENZIED;
        }
        if (roll < frenziedBias + ancientBias) {
            return MutationType.ANCIENT;
        }
        return MutationType.CORRUPTED;
    }

    private int placedEyeCount() {
        return summoning == null ? 0 : summoning.eyesByPedestal.size();
    }

    private String statusLabel() {
        if (!enabled) {
            return ChatColor.RED + "Disabled";
        }
        if (fight != null) {
            return ChatColor.RED + "Active Fight";
        }
        if (summoning != null) {
            return ChatColor.LIGHT_PURPLE + "Summoning";
        }
        return ChatColor.GREEN + "Idle";
    }

    private String worldLabel() {
        World world = resolvedWorld();
        return world != null ? world.getName() : ChatColor.RED + "Unavailable";
    }

    private String performanceSummary() {
        ServerPerformanceMonitor monitor = plugin.getServerPerformanceMonitor();
        return monitor != null ? monitor.getStatusSummary() : "Unavailable";
    }

    private long scaledPeriod(long basePeriod) {
        ServerPerformanceMonitor monitor = plugin.getServerPerformanceMonitor();
        return monitor != null ? monitor.scalePeriod(basePeriod, 2, 3) : basePeriod;
    }

    private boolean crossedScaledPeriod(long previousElapsedTicks, long currentElapsedTicks, long basePeriod) {
        return periodCrossings(previousElapsedTicks, currentElapsedTicks, scaledPeriod(basePeriod)) > 0;
    }

    static int periodCrossings(long previousElapsedTicks, long currentElapsedTicks, long period) {
        long normalizedPeriod = Math.max(1L, period);
        long normalizedPrevious = Math.max(0L, previousElapsedTicks);
        long normalizedCurrent = Math.max(normalizedPrevious, currentElapsedTicks);
        long previousWindow = normalizedPrevious / normalizedPeriod;
        long currentWindow = normalizedCurrent / normalizedPeriod;
        long crossings = currentWindow - previousWindow;
        return crossings > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, crossings);
    }

    private double dragonDamageMultiplier(FightSession fightSession) {
        double multiplier = baseDamageMultiplier + Math.max(0, fightSession.profile.tier() - 1) * damagePerTier;
        if (fightSession.phase == Phase.ASCENSION) {
            multiplier += 0.25D;
        }
        if (fightSession.profile.mutation() == MutationType.FRENZIED) {
            multiplier += 0.15D;
        } else if (fightSession.profile.mutation() == MutationType.ANCIENT) {
            multiplier += 0.08D;
        }
        return Math.max(0.5D, multiplier);
    }

    private static long targetedStrikeWarningTicks(int tier) {
        return tier >= 4 ? 35L : 30L;
    }

    private static double targetedStrikeRadius(int tier, boolean ascension) {
        double radius = ascension ? 4.85D : 4.5D;
        if (tier >= 4) {
            radius += 0.65D;
        }
        if (tier >= 5) {
            radius += 0.55D;
        }
        return radius;
    }

    private static double targetedStrikeDamage(int tier, boolean ascension) {
        double damage = 8.0D + tier * 2.0D;
        if (tier >= 4) {
            damage += ascension ? 2.0D : 1.0D;
        }
        return damage;
    }

    private static double aerialVolleySplashRadius(int tier) {
        return tier >= 5 ? 4.25D : tier >= 4 ? 3.75D : 3.25D;
    }

    private static double aerialVolleyDamage(int tier) {
        return tier >= 5 ? 9.5D : tier >= 4 ? 8.0D : 5.0D + tier;
    }

    private static long hazardPulsePeriod(int tier) {
        return tier >= 5 ? 10L : tier >= 4 ? 15L : 20L;
    }

    private static long hazardDurationTicks(int tier) {
        return tier >= 5 ? 125L : tier >= 4 ? 110L : 100L;
    }

    private static double hazardRadius(int tier) {
        return 3.5D + tier * 0.15D + Math.max(0, tier - 3) * 0.35D;
    }

    private static double hazardDamage(int tier) {
        return tier >= 5 ? 10.0D : tier >= 4 ? 8.5D : 4.0D + tier;
    }

    private static double meteorEchoRingRadius(int tier, boolean ascension) {
        if (tier >= 5) {
            return ascension ? 4.5D : 4.0D;
        }
        return ascension ? 4.0D : 3.4D;
    }

    private static double meteorEchoSplashRadius(int tier) {
        return tier >= 5 ? 2.85D : 2.55D;
    }

    private static double meteorEchoDamage(int tier, boolean ascension) {
        if (tier >= 5) {
            return ascension ? 8.5D : 7.5D;
        }
        return ascension ? 7.0D : 6.0D;
    }

    private static long cataclysmWarningTicks(int tier) {
        return cataclysmEnabled(tier) ? 30L : 0L;
    }

    private static double cataclysmSafeRadius(int tier) {
        return tier >= 5 ? 6.5D : 0.0D;
    }

    private double cataclysmOuterRadius(int tier) {
        return tier >= 5 ? Math.min(arenaRadius, 20.0D) : 0.0D;
    }

    private static double cataclysmDamage(int tier) {
        return tier >= 5 ? 8.0D : 0.0D;
    }

    private ProgressionRecord progressionFor(Player player) {
        return progressionByScope.computeIfAbsent(progressionScope(player), ignored -> new ProgressionRecord());
    }

    private String progressionScope(Player player) {
        ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager == null || player == null) {
            return player != null ? player.getUniqueId().toString() : "global";
        }

        SkyBlockProfile selected = profileManager.getSelectedProfile(player);
        SkyBlockProfile canonical = profileManager.resolveSharedProfile(selected);
        if (canonical != null && canonical.getCanonicalProfileId() != null) {
            return canonical.getCanonicalProfileId().toString();
        }
        return player.getUniqueId().toString();
    }

    private int reputationLevel(Player player) {
        return progressionFor(player).reputation;
    }

    private PlayerContribution contribution(Map<UUID, PlayerContribution> contributions, UUID playerId) {
        return contributions.computeIfAbsent(playerId, ignored -> new PlayerContribution());
    }

    private String dragonName(EncounterProfile profile, Phase phase) {
        String mutationPrefix = profile.mutation() == MutationType.NONE ? "" : ChatColor.RED + stripColor(profile.mutation().displayName()) + " ";
        return ChatColor.DARK_PURPLE + "Tier " + profile.tier()
                + ChatColor.GRAY + " "
                + mutationPrefix
                + stripColor(profile.dragonType().displayName())
                + ChatColor.GRAY + " [" + phase.displayName() + "]";
    }

    private boolean consumeOne(Player player, EquipmentSlot hand) {
        if (player.getGameMode().name().equalsIgnoreCase("CREATIVE")) {
            return true;
        }
        if (hand == null) {
            return false;
        }

        if (hand == EquipmentSlot.OFF_HAND) {
            ItemStack stack = player.getInventory().getItemInOffHand();
            if (!isSummoningEye(stack)) {
                return false;
            }
            player.getInventory().setItemInOffHand(decrementedCopy(stack));
            player.updateInventory();
            return true;
        }

        ItemStack stack = player.getInventory().getItemInMainHand();
        if (!isSummoningEye(stack)) {
            return false;
        }
        player.getInventory().setItemInMainHand(decrementedCopy(stack));
        player.updateInventory();
        return true;
    }

    private ItemStack decrementedCopy(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return new ItemStack(Material.AIR);
        }
        if (stack.getAmount() <= 1) {
            return new ItemStack(Material.AIR);
        }
        ItemStack copy = stack.clone();
        copy.setAmount(stack.getAmount() - 1);
        return copy;
    }

    private ItemStack rewardPreviewItem(RewardTier tier, Material material, double threshold, int scales, int bonusMaterials, boolean rarePossible) {
        return infoItem(
                material,
                tierColor(tier) + tier.name() + " Personal Chest",
                List.of(
                        ChatColor.GRAY + "Score needed: " + ChatColor.WHITE + formatScore(threshold),
                        ChatColor.GRAY + "Dragon Scales: " + ChatColor.WHITE + scales + "+",
                        ChatColor.GRAY + "Dragon loot: " + ChatColor.WHITE + bonusMaterials + "+",
                        ChatColor.GRAY + "Rare armor: " + (rarePossible ? ChatColor.GOLD + "Possible" : ChatColor.DARK_GRAY + "Low chance")
                )
        );
    }

    private ItemStack navigationItem(Material material, String name, List<String> lore) {
        return infoItem(material, name, lore);
    }

    private ItemStack infoItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fillBorders(Inventory inventory, Material fillMaterial) {
        ItemStack filler = infoItem(fillMaterial, " ", List.of());
        int size = inventory.getSize();
        for (int slot = 0; slot < size; slot++) {
            int row = slot / 9;
            int column = slot % 9;
            if (row == 0 || row == (size / 9) - 1 || column == 0 || column == 8) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private boolean isDragonView(String title, GuiView view) {
        return switch (view) {
            case MAIN -> MAIN_TITLE.equals(title);
            case CODEX -> CODEX_TITLE.equals(title);
            case REWARDS -> REWARDS_TITLE.equals(title);
            case SUMMARY -> SUMMARY_TITLE.equals(title);
        };
    }

    private Material parseMaterial(String name, Material fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial(name.trim());
        return material != null ? material : fallback;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Vector randomOffset(double radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return new Vector(random.nextDouble(-radius, radius), 0.0D, random.nextDouble(-radius, radius));
    }

    private static void spawnRingParticles(Location center, double radius, Particle particle, int points) {
        if (center == null || center.getWorld() == null || radius <= 0.0D || points <= 0) {
            return;
        }
        World world = center.getWorld();
        for (int index = 0; index < points; index++) {
            double angle = (Math.PI * 2.0D * index) / points;
            Location point = center.clone().add(Math.cos(angle) * radius, 0.0D, Math.sin(angle) * radius);
            world.spawnParticle(particle, point, 1, 0.05D, 0.05D, 0.05D, 0.0D);
        }
    }

    private void spawnFlashParticle(World world, Location location, int count, double offsetX, double offsetY, double offsetZ) {
        if (world == null || location == null) {
            return;
        }
        if (flashParticleAvailable) {
            try {
                world.spawnParticle(Particle.FLASH, location, count, offsetX, offsetY, offsetZ, 0.0D);
                return;
            } catch (IllegalArgumentException exception) {
                flashParticleAvailable = false;
                plugin.getLogger().warning("Particle.FLASH is not supported by this server build for Dragon Ascension visuals. Falling back to END_ROD.");
            }
        }
        world.spawnParticle(Particle.END_ROD, location, Math.max(2, count * 4), offsetX, offsetY, offsetZ, 0.01D);
    }

    private void damagePlayer(Player player, double baseDamage, EnderDragon dragon) {
        if (!isInArena(player, fight.altar, arenaRadius)) {
            return;
        }

        double multiplier = dragonDamageMultiplier(fight);
        // Tier 5 dragons deal True Damage: they bypass the resistance multiplier from reputation.
        if (fight.profile.tier() < 5) {
            multiplier *= resistanceMultiplier(reputationLevel(player));
        } else {
            // Tier 5 visual cue for true damage
            player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0.0D, 1.0D, 0.0D), 12, 0.3D, 0.5D, 0.3D, 0.02D);
        }

        player.damage(baseDamage * multiplier, dragon);
    }

    private double horizontalDistanceSquared(Location first, Location second) {

        double dx = first.getX() - second.getX();
        double dz = first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

    private static String formatPercent(double percent) {
        return String.format(Locale.ROOT, "%.0f%%", percent);
    }

    private static String formatSpawnChance(double chance) {
        return String.format(Locale.ROOT, "%.2f%%", sanitizeRareTierChance(chance) * 100.0D);
    }

    private String rareTierChanceLabel(int peakTier) {
        if (peakTier >= 5) {
            return ChatColor.GOLD + "Tier 4 " + formatSpawnChance(tierFourEncounterChance(peakTier, tierFourSpawnChance))
                    + ChatColor.GRAY + ", "
                    + ChatColor.DARK_RED + "Tier 5 " + formatSpawnChance(tierFiveEncounterChance(peakTier, tierFourSpawnChance, tierFiveSpawnChance));
        }
        if (peakTier >= 4) {
            return ChatColor.GOLD + "Tier 4 " + formatSpawnChance(tierFourEncounterChance(peakTier, tierFourSpawnChance));
        }
        return "";
    }

    private static double mutationChanceForTier(double baseMutationChance, int diversityCount, int tier) {
        double mutationChance = baseMutationChance;
        mutationChance += Math.max(0, diversityCount) * 2.0D;
        mutationChance += Math.max(0, tier - 1) * 3.0D;
        return clamp(mutationChance, 0.0D, 90.0D);
    }

    static double sanitizeRareTierChance(double chance) {
        if (!Double.isFinite(chance)) {
            return 0.0D;
        }
        return clamp(chance, 0.0D, 1.0D);
    }

    private static String formatScore(double score) {
        return String.format(Locale.ROOT, "%.1f", score);
    }

    private String rewardDisplayName(String key) {
        if (key == null || key.isBlank()) {
            return "None";
        }
        ItemStack item = customItemService.createItemByKey(key);
        if (item != null && item.getType() != Material.AIR) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                return stripColor(meta.getDisplayName());
            }
        }
        return humanizeKey(key);
    }

    private static String humanizeKey(String key) {
        if (key == null || key.isBlank()) {
            return "None";
        }
        String[] words = key.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.isEmpty() ? key : builder.toString();
    }

    private static List<String> dragonTypeCommandOptions() {
        List<String> options = new ArrayList<>();
        for (DragonType type : DragonType.values()) {
            options.add(type.name().toLowerCase(Locale.ROOT));
        }
        return options;
    }

    private static List<String> mutationTypeCommandOptions() {
        List<String> options = new ArrayList<>();
        for (MutationType mutation : MutationType.values()) {
            options.add(mutation.name().toLowerCase(Locale.ROOT));
        }
        return options;
    }

    private static ChatColor tierColor(RewardTier tier) {
        return switch (tier) {
            case S -> ChatColor.GOLD;
            case A -> ChatColor.GREEN;
            case B -> ChatColor.AQUA;
            case C -> ChatColor.YELLOW;
            default -> ChatColor.DARK_GRAY;
        };
    }

    private static String stripColor(String text) {
        return text == null ? "" : ChatColor.stripColor(text);
    }

    private static void trySetDragonPhase(EnderDragon dragon, EnderDragon.Phase phase) {
        try {
            dragon.setPhase(phase);
        } catch (IllegalArgumentException ignored) {
        }
    }

    private enum GuiView {
        MAIN,
        CODEX,
        REWARDS,
        SUMMARY
    }

    private enum Phase {
        AERIAL("Aerial"),
        GROUND("Ground"),
        ASCENSION("Ascension");

        private final String displayName;

        Phase(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    private enum DragonType {
        YOUNG(ChatColor.GREEN + "Young Dragon", Material.LIME_STAINED_GLASS, "ENDSTONE_SHARD", "Break the shield with fast crystal clears"),
        STRONG(ChatColor.RED + "Strong Dragon", Material.NETHERITE_SCRAP, "OBSIDIAN_CORE", "Avoid front-loaded slam damage"),
        WISE(ChatColor.AQUA + "Wise Dragon", Material.ENCHANTED_BOOK, "RIFT_ESSENCE", "Respect spell volleys and utility casts"),
        PROTECTOR(ChatColor.BLUE + "Protector Dragon", Material.SHIELD, "VOID_CRYSTAL", "Sustain through longer shield windows"),
        UNSTABLE(ChatColor.YELLOW + "Unstable Dragon", Material.BLAZE_POWDER, "OBSIDIAN_CORE", "Dodge spike mechanics and marked bursts"),
        SUPERIOR(ChatColor.GOLD + "Superior Dragon", Material.DRAGON_HEAD, "KUNZITE", "Handle every mechanic cleanly");

        private final String displayName;
        private final Material icon;
        private final String rewardKey;
        private final String weakness;

        DragonType(String displayName, Material icon, String rewardKey, String weakness) {
            this.displayName = displayName;
            this.icon = icon;
            this.rewardKey = rewardKey;
            this.weakness = weakness;
        }

        public String displayName() {
            return displayName;
        }

        public static DragonType parse(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                return DragonType.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }

    private enum MutationType {
        NONE(ChatColor.GRAY + "None", Material.GRAY_DYE, "", "Base dragon behavior."),
        CORRUPTED(ChatColor.DARK_PURPLE + "Corrupted", Material.OBSIDIAN, "VOID_CRYSTAL", "Creates void-corruption zones that punish static play."),
        FRENZIED(ChatColor.RED + "Frenzied", Material.BLAZE_POWDER, "OBSIDIAN_CORE", "Lowers survivability in exchange for faster ability loops."),
        ANCIENT(ChatColor.GOLD + "Ancient", Material.ANCIENT_DEBRIS, "KUNZITE", "Harder to break down but pays out better materials.");

        private final String displayName;
        private final Material icon;
        private final String rewardKey;
        private final String description;

        MutationType(String displayName, Material icon, String rewardKey, String description) {
            this.displayName = displayName;
            this.icon = icon;
            this.rewardKey = rewardKey;
            this.description = description;
        }

        public String displayName() {
            return displayName;
        }

        public static MutationType parse(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                return MutationType.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }

    private enum PedestalAspect {
        YOUNG(ChatColor.GREEN + "Young", 1.0D, 1.0D) {
            @Override
            void applyInfluence(EnumMap<DragonType, Integer> influence) {
                influence.merge(DragonType.YOUNG, 3, Integer::sum);
            }
        },
        STRONG(ChatColor.RED + "Strong", 2.8D, 2.5D) {
            @Override
            void applyInfluence(EnumMap<DragonType, Integer> influence) {
                influence.merge(DragonType.STRONG, 3, Integer::sum);
            }
        },
        WISE(ChatColor.AQUA + "Wise", 2.5D, 2.0D) {
            @Override
            void applyInfluence(EnumMap<DragonType, Integer> influence) {
                influence.merge(DragonType.WISE, 3, Integer::sum);
            }
        },
        PROTECTOR(ChatColor.BLUE + "Protector", 2.3D, 2.0D) {
            @Override
            void applyInfluence(EnumMap<DragonType, Integer> influence) {
                influence.merge(DragonType.PROTECTOR, 3, Integer::sum);
            }
        },
        UNSTABLE(ChatColor.YELLOW + "Unstable", 3.0D, 3.5D) {
            @Override
            void applyInfluence(EnumMap<DragonType, Integer> influence) {
                influence.merge(DragonType.UNSTABLE, 3, Integer::sum);
            }
        },
        SUPERIOR(ChatColor.GOLD + "Superior", 3.5D, 2.5D) {
            @Override
            void applyInfluence(EnumMap<DragonType, Integer> influence) {
                for (DragonType type : DragonType.values()) {
                    influence.merge(type, 1, Integer::sum);
                }
                influence.merge(DragonType.SUPERIOR, 3, Integer::sum);
            }
        },
        CHAOS(ChatColor.DARK_PURPLE + "Chaos", 2.7D, 4.0D) {
            @Override
            void applyInfluence(EnumMap<DragonType, Integer> influence) {
                for (DragonType type : DragonType.values()) {
                    influence.merge(type, 1, Integer::sum);
                }
            }
        },
        ANCIENT(ChatColor.LIGHT_PURPLE + "Ancient", 3.8D, 4.0D) {
            @Override
            void applyInfluence(EnumMap<DragonType, Integer> influence) {
                influence.merge(DragonType.WISE, 1, Integer::sum);
                influence.merge(DragonType.PROTECTOR, 2, Integer::sum);
                influence.merge(DragonType.SUPERIOR, 2, Integer::sum);
            }
        };

        private final String displayName;
        private final double powerValue;
        private final double mutationContribution;

        PedestalAspect(String displayName, double powerValue, double mutationContribution) {
            this.displayName = displayName;
            this.powerValue = powerValue;
            this.mutationContribution = mutationContribution;
        }

        abstract void applyInfluence(EnumMap<DragonType, Integer> influence);

        static PedestalAspect parse(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                return PedestalAspect.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }

    private record EncounterProfile(
            DragonType dragonType,
            MutationType mutation,
            int tier,
            double mutationChance,
            EnumMap<DragonType, Integer> influence
    ) {
    }

    private record PlacedEye(UUID playerId, PedestalAspect aspect, int pedestalIndex) {
    }

    private static final class SummoningState {
        private final Map<Integer, PlacedEye> eyesByPedestal = new HashMap<>();
        private final Map<UUID, PlayerContribution> contributions = new HashMap<>();
        private long lastInteractionMillis = System.currentTimeMillis();
        private DragonType lockedDragonType;
        private Integer lockedTier;
        private int lockedAtEyeCount;
        private int tierLockedAtEyeCount;
    }

    private static final class FightSession {
        private UUID dragonId;
        private Location altar;
        private EncounterProfile profile;
        private Phase phase;
        private long elapsedTicks;
        private double maxHealth;
        private double aerialShield;
        private double maxAerialShield;
        private final Map<UUID, PlayerContribution> contributions = new HashMap<>();
        private final Set<UUID> crystalIds = new HashSet<>();
        private final List<HazardZone> hazards = new ArrayList<>();
        private TargetedStrike pendingStrike;
        private CataclysmPulse pendingCataclysm;
        private boolean failed;
        private boolean bulwarkActive;
        private BukkitTask shieldTask;
    }

    private static final class PlayerContribution {
        private double damageDealt;
        private int crystalsDestroyed;
        private int mechanicScore;
        private int eyesPlaced;
        private long activeTicks;

        private ContributionSnapshot snapshot() {
            return new ContributionSnapshot(damageDealt, crystalsDestroyed, mechanicScore, eyesPlaced, activeTicks);
        }
    }

    private static final class ProgressionRecord {
        private int reputation;
        private int kills;
        private double bestScore;
        private final EnumSet<DragonType> discoveredTypes = EnumSet.noneOf(DragonType.class);
        private final EnumSet<MutationType> discoveredMutations = EnumSet.noneOf(MutationType.class);
        private final Map<DragonType, Integer> killsByType = new EnumMap<>(DragonType.class);
        private final Map<DragonType, Double> bestScores = new EnumMap<>(DragonType.class);
    }

    private static final class FightSummary {
        private final DragonType dragonType;
        private final MutationType mutation;
        private final int tier;
        private final RewardTier rewardTier;
        private final double score;
        private final double damageDealt;
        private final int crystalsDestroyed;
        private final int mechanicScore;
        private final int eyesPlaced;
        private final long activeTicks;
        private final List<ItemStack> lootPreview;
        private final int reputationAfter;

        private FightSummary(
                DragonType dragonType,
                MutationType mutation,
                int tier,
                RewardTier rewardTier,
                double score,
                double damageDealt,
                int crystalsDestroyed,
                int mechanicScore,
                int eyesPlaced,
                long activeTicks,
                List<ItemStack> lootPreview,
                int reputationAfter
        ) {
            this.dragonType = dragonType;
            this.mutation = mutation;
            this.tier = tier;
            this.rewardTier = rewardTier;
            this.score = score;
            this.damageDealt = damageDealt;
            this.crystalsDestroyed = crystalsDestroyed;
            this.mechanicScore = mechanicScore;
            this.eyesPlaced = eyesPlaced;
            this.activeTicks = activeTicks;
            this.lootPreview = lootPreview;
            this.reputationAfter = reputationAfter;
        }

        private String rewardTierLabel() {
            return tierColor(rewardTier) + rewardTier.name();
        }
    }

    private record TargetedStrike(UUID targetId, Location location, long resolveAtTick, double radius) {
    }

    private record HazardZone(Location location, double radius, long expiresAtTick, MutationType type) {
    }

    private record CataclysmPulse(Location location, long startTick, long resolveAtTick, double safeRadius, double outerRadius) {
    }

    private record EncounterLock(DragonType dragonType, int tier, boolean typeLocked, boolean tierChanged) {
    }
}
