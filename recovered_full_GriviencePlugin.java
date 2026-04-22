
IMPORTANT: The file content has been truncated.
Status: Showing lines 1-2000 of 3384 total lines.
Action: To read more of the file, you can use the 'start_line' and 'end_line' parameters in a subsequent 'read_file' call. For example, to read the next section of the file, use start_line: 2001.

--- FILE CONTENT (truncated) ---
package io.papermc.Grivience;

import io.papermc.Grivience.accessory.AccessoryManager;
import io.papermc.Grivience.bazaar.BazaarShopManager;
import io.papermc.Grivience.bazaar.BazaarGuiManager;
import io.papermc.Grivience.bazaar.NpcSellShopGui;
import io.papermc.Grivience.command.AccessoryCommand;
import io.papermc.Grivience.command.AdminItemResolver;
import io.papermc.Grivience.command.BazaarCommand;
import io.papermc.Grivience.command.BlacksmithCommand;
import io.papermc.Grivience.command.DungeonCommand;
import io.papermc.Grivience.command.GiveItemCommand;
import io.papermc.Grivience.command.DungeonHubCommand;
import io.papermc.Grivience.command.NpcSellShopCommand;
import io.papermc.Grivience.command.PersonalCompactorCommand;
import io.papermc.Grivience.command.ReforgeCommand;
import io.papermc.Grivience.command.SkyblockMenuCommand;
import io.papermc.Grivience.command.GrivienceReloadCommand;
import io.papermc.Grivience.command.SanityCheckCommand;
import io.papermc.Grivience.command.WardrobeCommand;
import io.papermc.Grivience.command.PetCommand;
import io.papermc.Grivience.command.PortalRouteCommand;
import io.papermc.Grivience.command.AdminTeleportCommand;
import io.papermc.Grivience.command.MinehubHeartCommand;
import io.papermc.Grivience.command.JumpPadCommand;
import io.papermc.Grivience.command.MountainCommand;
import io.papermc.Grivience.collections.CollectionsManager;
import io.papermc.Grivience.collections.CollectionGUI;
import io.papermc.Grivience.collections.CollectionCommand;
import io.papermc.Grivience.collections.CollectionListener;
import io.papermc.Grivience.enchantment.EnchantmentManager;
import io.papermc.Grivience.crafting.CraftingManager;
import io.papermc.Grivience.compactor.PersonalCompactorListener;
import io.papermc.Grivience.compactor.PersonalCompactorManager;
import io.papermc.Grivience.farming.FarmingContestCommand;
import io.papermc.Grivience.farming.FarmingContestManager;
import io.papermc.Grivience.hook.GriviencePlaceholderExpansion;




import io.papermc.Grivience.welcome.WelcomeManager;
import io.papermc.Grivience.welcome.quest.QuestLineManager;
import io.papermc.Grivience.fasttravel.FastTravelManager;
import io.papermc.Grivience.fasttravel.FastTravelGui;
import io.papermc.Grivience.fasttravel.FastTravelCommand;
import io.papermc.Grivience.fasttravel.FastTravelListener;
import io.papermc.Grivience.dungeon.DungeonManager;
import io.papermc.Grivience.dungeon.RewardChestManager;
import io.papermc.Grivience.dragon.DragonAscensionManager;
import io.papermc.Grivience.gui.DungeonGuiManager;
import io.papermc.Grivience.gui.SkyblockMenuManager;
import io.papermc.Grivience.item.ArmorCraftingListener;
import io.papermc.Grivience.item.ArmorSetBonusListener;
import io.papermc.Grivience.item.CustomArmorManager;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.NavigationItemManager;
import io.papermc.Grivience.listener.CustomArmorEffectListener;
import io.papermc.Grivience.listener.CustomWeaponCombatListener;
import io.papermc.Grivience.listener.DungeonListener;
import io.papermc.Grivience.listener.CustomItemEnchantListener;
import io.papermc.Grivience.listener.EnchantTableListener;
import io.papermc.Grivience.listener.FriendlyFireListener;
import io.papermc.Grivience.listener.CustomRecipeWorkbenchOnlyListener;
import io.papermc.Grivience.listener.RecipeCollectionGateListener;
import io.papermc.Grivience.listener.RecipeUnlockListener;
import io.papermc.Grivience.listener.ReforgeAnvilGuiListener;
import io.papermc.Grivience.listener.ResourcePackManager;
import io.papermc.Grivience.listener.ReplenishListener;
import io.papermc.Grivience.listener.AttackSpeedListener;
import io.papermc.Grivience.listener.AutoPickupDropListener;
import io.papermc.Grivience.listener.ArmorDurabilityListener;
import io.papermc.Grivience.listener.ItemStarCleanupListener;
import io.papermc.Grivience.listener.BlacksmithGuiListener;
import io.papermc.Grivience.listener.CustomFishingListener;
import io.papermc.Grivience.listener.InventoryStackSafetyListener;
import io.papermc.Grivience.listener.PluginHiderListener;
import io.papermc.Grivience.listener.MobTargetLimiterListener;
import io.papermc.Grivience.listener.FarmingFortuneListener;
import io.papermc.Grivience.listener.GodPotionListener;
import io.papermc.Grivience.listener.MobSunBurnListener;
import io.papermc.Grivience.listener.ProjectileGroundCleanupListener;
import io.papermc.Grivience.listener.MobHealthListener;
import io.papermc.Grivience.listener.EnchantedItemRecipeGuardListener;
import io.papermc.Grivience.listener.HungerProtectionListener;
import io.papermc.Grivience.skills.SkyblockSkillManager;
import io.papermc.Grivience.skills.SkillXpAdminCommand;
import io.papermc.Grivience.stats.SkyblockLevelListener;
import io.papermc.Grivience.stats.SkyblockLevelManager;
import io.papermc.Grivience.stats.SkyblockManaManager;
import io.papermc.Grivience.stats.SkyblockStatsManager;
import io.papermc.Grivience.stats.SkyblockScoreboardManager;
import io.papermc.Grivience.stats.SkyblockCombatEngine;
import io.papermc.Grivience.stats.SkyblockCombatRefreshListener;
import io.papermc.Grivience.stats.SkyblockCombatStatsService;
import io.papermc.Grivience.stats.BitsManager;
import io.papermc.Grivience.stats.command.BitsAdminCommand;
import io.papermc.Grivience.stats.command.SkyblockLevelAdminCommand;
import io.papermc.Grivience.skyblock.economy.command.CoinsAdminCommand;
import io.papermc.Grivience.minion.MinionCommand;
import io.papermc.Grivience.minion.MinionCraftingListener;
import io.papermc.Grivience.minion.MinionGuiManager;
import io.papermc.Grivience.minion.MinionListener;
import io.papermc.Grivience.minion.MinionManager;
import io.papermc.Grivience.announcement.BossBarAnnouncementManager;
import io.papermc.Grivience.announcement.command.BossBarAnnounceCommand;
import io.papermc.Grivience.jumppad.JumpPadListener;
import io.papermc.Grivience.jumppad.JumpPadManager;
import io.papermc.Grivience.mob.CustomMonsterDeathListener;
import io.papermc.Grivience.mob.CustomMonsterManager;
import io.papermc.Grivience.mob.CrimsonWardenAdminCommand;
import io.papermc.Grivience.mob.CrimsonWardenBossListener;
import io.papermc.Grivience.mob.CrimsonWardenSpawnManager;
import io.papermc.Grivience.mob.MonsterSpawnAdminCommand;
import io.papermc.Grivience.mines.MinehubCommissionManager;
import io.papermc.Grivience.mines.MinehubHeartManager;
import io.papermc.Grivience.mines.MinehubOreGenerationListener;
import io.papermc.Grivience.party.PartyManager;
import io.papermc.Grivience.mines.MiningItemListener;
import io.papermc.Grivience.mines.MiningSystemManager;
import io.papermc.Grivience.mines.MiningEventManager;
import io.papermc.Grivience.mines.MiningEventCommand;
import io.papermc.Grivience.mines.InspectorShopGui;
import io.papermc.Grivience.mines.DrillMechanicGui;
import io.papermc.Grivience.mines.DrillForgeCommand;
import io.papermc.Grivience.mines.DrillUpgradeCraftListener;
import io.papermc.Grivience.event.GlobalEventManager;
import io.papermc.Grivience.event.GlobalEventCommand;
import io.papermc.Grivience.mines.end.EndMinesCommand;
import io.papermc.Grivience.mines.end.EndMinesConvergenceManager;
import io.papermc.Grivience.mines.end.EndHubKunziteListener;
import io.papermc.Grivience.mines.end.EndMinesManager;
import io.papermc.Grivience.mines.end.EndMinesMiningListener;
import io.papermc.Grivience.mines.end.EndMinesProtectionListener;
import io.papermc.Grivience.mines.end.HeartOfTheEndMinesManager;
import io.papermc.Grivience.mines.end.mob.EndMinesMobManager;
import io.papermc.Grivience.skyblock.command.HubCommand;
import io.papermc.Grivience.skyblock.command.IslandCommand;
import io.papermc.Grivience.skyblock.command.IslandBypassCommand;
import io.papermc.Grivience.skyblock.command.MinehubCommand;
import io.papermc.Grivience.skyblock.command.FarmHubCommand;
import io.papermc.Grivience.skyblock.command.SkyblockAdminCommand;
import io.papermc.Grivience.skyblock.hub.HubWorldFeaturesManager;
import io.papermc.Grivience.skyblock.hub.MinehubWorldManager;
import io.papermc.Grivience.skyblock.island.IslandManager;
import io.papermc.Grivience.skyblock.listener.PortalRoutingListener;
import io.papermc.Grivience.skyblock.listener.SkyblockListener;
import io.papermc.Grivience.skyblock.listener.IslandProtectionListener;
import io.papermc.Grivience.skyblock.listener.FarmHubCropRegenerationListener;
import io.papermc.Grivience.skyblock.listener.DeathPenaltyListener;
import io.papermc.Grivience.skyblock.portal.PortalRoutingManager;
import io.papermc.Grivience.trade.TradeCommand;
import io.papermc.Grivience.trade.TradeListener;
import io.papermc.Grivience.trade.TradeManager;
import io.papermc.Grivience.wardrobe.WardrobeGui;
import io.papermc.Grivience.wardrobe.WardrobeManager;
import io.papermc.Grivience.pet.PetManager;
import io.papermc.Grivience.pet.PetGui;
import io.papermc.Grivience.pet.PetExpListener;
import io.papermc.Grivience.pet.PetConsumeListener;
import io.papermc.Grivience.pet.PetSkillXpListener;
import io.papermc.Grivience.performance.ServerPerformanceMonitor;
import io.papermc.Grivience.quest.QuestManager;
import io.papermc.Grivience.quest.QuestCommand;
import io.papermc.Grivience.quest.QuestGui;
import io.papermc.Grivience.quest.QuestListener;
import io.papermc.Grivience.quest.QuestZnpcsHook;
import io.papermc.Grivience.wizard.WizardTowerCommand;
import io.papermc.Grivience.wizard.WizardTowerManager;
import io.papermc.Grivience.wizard.WizardTowerZnpcsHook;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class GriviencePlugin extends JavaPlugin {
    private static final List<Long> DEFAULT_HEART_OF_END_MINES_THRESHOLDS = List.of(0L, 75L, 225L, 525L, 975L, 1700L, 2800L, 4400L, 6650L, 9750L);

    private PartyManager partyManager;
    private DungeonManager dungeonManager;
    private DungeonGuiManager guiManager;
    private SkyblockMenuManager skyblockMenuManager;
    private TradeManager tradeManager;
    private io.papermc.Grivience.bank.BankManager bankManager;
    private NavigationItemManager navigationItemManager;
    private IslandManager islandManager;
    private MinehubWorldManager minehubWorldManager;
    private EndMinesManager endMinesManager;
    private EndMinesMobManager endMinesMobManager;
    private HeartOfTheEndMinesManager heartOfTheEndMinesManager;
    private EndMinesConvergenceManager endMinesConvergenceManager;
    private CustomArmorManager customArmorManager;
    private ArmorSetBonusListener armorSetBonusListener;
    private ArmorCraftingListener armorCraftingListener;
    private CustomMonsterManager customMonsterManager;
    private CustomMonsterDeathListener customMonsterDeathListener;
    private CrimsonWardenBossListener crimsonWardenBossListener;
    private CrimsonWardenSpawnManager crimsonWardenSpawnManager;
    private CustomItemService customItemService;
    private SkyblockLevelManager skyblockLevelManager;
    private SkyblockSkillManager skyblockSkillManager;
    private SkyblockStatsManager skyblockStatsManager;
    private SkyblockManaManager skyblockManaManager;
    private SkyblockCombatStatsService skyblockCombatStatsService;
    private SkyblockCombatEngine skyblockCombatEngine;
    private CustomWeaponCombatListener customWeaponCombatListener;
    private EnchantTableListener enchantTableListener;
    private CustomItemEnchantListener customItemEnchantListener;
    private ReforgeAnvilGuiListener reforgeAnvilGuiListener;
    private CustomArmorEffectListener customArmorEffectListener;
    private BazaarShopManager bazaarShopManager;
    private BazaarGuiManager bazaarGuiManager;
    private io.papermc.Grivience.auction.AuctionManager auctionManager;
    private io.papermc.Grivience.auction.gui.AuctionGuiManager auctionGuiManager;
    private NpcSellShopGui npcSellShopGui;
    private io.papermc.Grivience.npcshop.NpcShopManager npcShopManager;
    private io.papermc.Grivience.elevator.ElevatorManager elevatorManager;
    private io.papermc.Grivience.elevator.ElevatorGui elevatorGui;
    private io.papermc.Grivience.elevator.ElevatorZnpcsHook elevatorZnpcsHook;
    private RewardChestManager rewardChestManager;
    private ResourcePackManager resourcePackManager;
    private BlacksmithGuiListener blacksmithGuiListener;
    private JumpPadManager jumpPadManager;
    private WardrobeManager wardrobeManager;
    private WardrobeGui wardrobeGui;
    private QuestManager questManager;
    private QuestGui questGui;
    private QuestZnpcsHook questZnpcsHook;
    private WizardTowerManager wizardTowerManager;
    private WizardTowerZnpcsHook wizardTowerZnpcsHook;
    private HubWorldFeaturesManager hubWorldFeaturesManager;
    private PetManager petManager;
    private PetGui petGui;
    private io.papermc.Grivience.gui.SkyblockLevelGui skyblockLevelGui;
    private io.papermc.Grivience.skills.SkillsGui skillsGui;
    private SkyblockScoreboardManager skyblockScoreboardManager;
    private BitsManager bitsManager;
    private BossBarAnnouncementManager bossBarAnnouncementManager;
    private io.papermc.Grivience.announcement.AutoAnnouncementManager autoAnnouncementManager;
    private io.papermc.Grivience.zone.ZoneManager zoneManager;
    private io.papermc.Grivience.zone.ZoneScoreboardListener zoneScoreboardListener;
    private io.papermc.Grivience.item.GrapplingHookManager grapplingHookManager;
    private io.papermc.Grivience.item.StaffManager staffManager;
    private io.papermc.Grivience.skyblock.profile.ProfileManager profileManager;
    private io.papermc.Grivience.skyblock.profile.gui.ProfileGui profileGui;
    private DragonAscensionManager dragonAscensionManager;
    private CollectionsManager collectionsManager;
    private MinehubHeartManager minehubHeartManager;
    private MinehubCommissionManager minehubCommissionManager;
    private CollectionGUI collectionGui;
    private CollectionListener collectionListener;
    private EnchantmentManager enchantmentManager;
    private CraftingManager craftingManager;
    private io.papermc.Grivience.slayer.SlayerGui slayerGui;
    private io.papermc.Grivience.mayor.MayorGui mayorGui;
    private io.papermc.Grivience.slayer.SlayerZnpcsHook slayerZnpcsHook;
    private io.papermc.Grivience.mayor.MayorZnpcsHook mayorZnpcsHook;
    private io.papermc.Grivience.soul.SoulManager soulManager;
    private io.papermc.Grivience.slayer.SlayerManager slayerManager;
    private io.papermc.Grivience.mayor.MayorManager mayorManager;

    private WelcomeManager welcomeManager;
    private QuestLineManager questLineManager;
    private FastTravelManager fastTravelManager;
    private FastTravelGui fastTravelGui;
    private io.papermc.Grivience.storage.StorageManager storageManager;
    private io.papermc.Grivience.storage.StorageGui storageGui;
    private io.papermc.Grivience.storage.StorageListener storageListener;
    private AccessoryManager accessoryManager;
    private PersonalCompactorManager personalCompactorManager;
    private MiningSystemManager miningSystemManager;
    private MiningEventManager miningEventManager;
    private MinionManager minionManager;
    private MinionGuiManager minionGuiManager;
    private ServerPerformanceMonitor serverPerformanceMonitor;
    private InspectorShopGui inspectorShopGui;
    private DrillMechanicGui drillMechanicGui;
    private GlobalEventManager globalEventManager;
    private io.papermc.Grivience.nick.NickManager nickManager;
    private io.papermc.Grivience.nick.NickGuiManager nickGuiManager;
    private FarmingContestManager farmingContestManager;
    private GriviencePlaceholderExpansion griviencePlaceholderExpansion;
    private org.bukkit.NamespacedKey grapplingHookKey;

    private io.papermc.Grivience.gui.GuardianRecipeGui guardianRecipeGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        applyConfigUpgrades();
        getServer().getPluginManager().registerEvents(new DeathPenaltyListener(this), this);
        getServer().getPluginManager().registerEvents(new PluginHiderListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryStackSafetyListener(this), this);

        grapplingHookKey = new org.bukkit.NamespacedKey(this, "grappling_hook_id");
        serverPerformanceMonitor = new ServerPerformanceMonitor(this);
        serverPerformanceMonitor.start();

        partyManager = new PartyManager(this);
        customItemService = new CustomItemService(this);
        customItemService.reloadFromConfig();
        customItemService.registerRecipes();
        accessoryManager = new AccessoryManager(this, customItemService);
        getServer().getPluginManager().registerEvents(new ItemStarCleanupListener(this, customItemService), this);
        skyblockLevelManager = new SkyblockLevelManager(this);
        skyblockLevelManager.load();
        skyblockSkillManager = new SkyblockSkillManager(this, skyblockLevelManager);
        skyblockLevelManager.setSkillManager(skyblockSkillManager);

        skillsGui = new io.papermc.Grivience.skills.SkillsGui(this, skyblockSkillManager);
        getServer().getPluginManager().registerEvents(skillsGui, this);
        registerCommand("skills", new io.papermc.Grivience.skills.SkillsCommand(skillsGui));
        registerCommand("skillxp", new SkillXpAdminCommand(skyblockSkillManager));

        skyblockLevelGui = new io.papermc.Grivience.gui.SkyblockLevelGui(this, skyblockLevelManager);
        getServer().getPluginManager().registerEvents(skyblockLevelGui, this);
        skyblockStatsManager = new SkyblockStatsManager(this, skyblockLevelManager);
        skyblockStatsManager.setSkillManager(skyblockSkillManager);
        skyblockStatsManager.reload();
        rewardChestManager = new RewardChestManager(this);
        dungeonManager = new DungeonManager(this, partyManager, customItemService, rewardChestManager);
        skyblockManaManager = new SkyblockManaManager(this, skyblockStatsManager, dungeonManager, null);
        guiManager = new DungeonGuiManager(this, partyManager, dungeonManager);
        skyblockMenuManager = new SkyblockMenuManager(this);

        // GUI managers handle their own inventory click events; they must be registered as listeners.
        getServer().getPluginManager().registerEvents(guiManager, this);
        getServer().getPluginManager().registerEvents(skyblockMenuManager, this);

        // Core hub/menu commands (aliases are handled by Bukkit via plugin.yml).
        SkyblockMenuCommand skyblockCommand = new SkyblockMenuCommand(skyblockMenuManager);
        registerCommand("skyblock", skyblockCommand);

        // Trading (Hypixel-style /trade) should be usable everywhere, including while visiting islands.
        tradeManager = new TradeManager(this);
        TradeCommand tradeCommand = new TradeCommand(tradeManager);
        registerCommand("trade", tradeCommand);
        getServer().getPluginManager().registerEvents(new TradeListener(this, tradeManager), this);
 
        HubCommand hubCommand = new HubCommand(this);
        registerCommand("hub", hubCommand);

        MinehubCommand minehubCommand = new MinehubCommand(this);
        registerCommand("minehub", minehubCommand);

        FarmHubCommand farmHubCommand = new FarmHubCommand(this);
        registerCommand("farmhub", farmHubCommand);

        DungeonHubCommand dungeonHubCommand = new DungeonHubCommand(this);
        registerCommand("dungeonhub", dungeonHubCommand);

        hubWorldFeaturesManager = new HubWorldFeaturesManager(this);
        getServer().getPluginManager().registerEvents(hubWorldFeaturesManager, this);
        hubWorldFeaturesManager.applyNow();

        PortalRoutingManager portalRoutingManager = new PortalRoutingManager(this);
        registerCommand("portalroute", new PortalRouteCommand(portalRoutingManager));

        navigationItemManager = new NavigationItemManager(this, skyblockMenuManager);
        getServer().getPluginManager().registerEvents(navigationItemManager, this);
        islandManager = new IslandManager(this);
        islandManager.initializeWorld();

        // Worlds must exist before FastTravelManager loads destinations.
        endMinesManager = new EndMinesManager(this);
        endMinesManager.initializeWorld();
        EndMinesCommand endMinesCommand = new EndMinesCommand(this, endMinesManager);
        registerCommand("endmine", endMinesCommand);
        getServer().getPluginManager().registerEvents(new EndMinesProtectionListener(this, endMinesManager), this);

        minehubWorldManager = new MinehubWorldManager(this);
        minehubWorldManager.initializeWorld();

        // Ensure worlds stay initialized (every 5 minutes)
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (islandManager != null && islandManager.getIslandWorld() == null) {
                getLogger().info("Skyblock world was null/unloaded, re-initializing...");
                islandManager.initializeWorld();
            }
            if (endMinesManager != null && endMinesManager.isEnabled() && endMinesManager.getWorld() == null) {
                getLogger().info("End Mines world was null/unloaded, re-initializing...");
                endMinesManager.initializeWorld();
            }
            if (minehubWorldManager != null && minehubWorldManager.getWorld() == null) {
                getLogger().info("Minehub world was null/unloaded, re-initializing...");
                minehubWorldManager.initializeWorld();
            }
        }, 6000L, 6000L); // 5 minutes (20 ticks * 60 * 5)

        partyManager.setIslandManager(islandManager);
        skyblockMenuManager.setIslandManager(islandManager);

        // Initialize custom armor system
        if (getConfig().getBoolean("custom-armor.enabled", true)) {
            customArmorManager = new CustomArmorManager(this);
            customArmorManager.reloadFromConfig();
            skyblockLevelManager.setArmorManager(customArmorManager);
            armorSetBonusListener = new ArmorSetBonusListener(this, customArmorManager);
            armorCraftingListener = new ArmorCraftingListener(this, customArmorManager, customItemService);
            armorCraftingListener.registerRecipes();
            getServer().getPluginManager().registerEvents(armorSetBonusListener, this);
            getServer().getPluginManager().registerEvents(armorCraftingListener, this);
            getLogger().info("Custom armor system enabled with " + customArmorManager.getArmorSets().size() + " armor sets.");
        }
        // Custom armor manager is created above; wire it into the menus after initialization.
        skyblockMenuManager.setManagers(skyblockLevelManager, skyblockStatsManager, skyblockManaManager, customArmorManager);
        skyblockManaManager.setArmorManager(customArmorManager);
        skyblockManaManager.start();

        // Skyblock combat engine (Skyblock-style health/defense mapping + mitigation).
        skyblockCombatStatsService = new SkyblockCombatStatsService(
                this,
                skyblockLevelManager,
                skyblockStatsManager,
                customItemService,
                customArmorManager,
                accessoryManager
        );
        skyblockCombatStatsService.setSkillManager(skyblockSkillManager);
        skyblockCombatEngine = new SkyblockCombatEngine(this, skyblockCombatStatsService);
        skyblockManaManager.setCombatEngine(skyblockCombatEngine);
        skyblockMenuManager.setCombatEngine(skyblockCombatEngine);

        // Initialize custom monster system
        if (getConfig().getBoolean("custom-monsters.enabled", true)) {
            customMonsterManager = new CustomMonsterManager(this);
            customMonsterManager.setCustomItemService(customItemService);
            customMonsterDeathListener = new CustomMonsterDeathListener(this, customMonsterManager);
            getServer().getPluginManager().registerEvents(customMonsterDeathListener, this);
            crimsonWardenBossListener = new CrimsonWardenBossListener(this, customItemService, customArmorManager);
            getServer().getPluginManager().registerEvents(crimsonWardenBossListener, this);
            crimsonWardenSpawnManager = new CrimsonWardenSpawnManager(this, customMonsterManager);
            getServer().getPluginManager().registerEvents(crimsonWardenSpawnManager, this);

            MonsterSpawnAdminCommand monsterSpawnAdminCommand = new MonsterSpawnAdminCommand(this, customMonsterManager);
            registerCommand("mobspawn", monsterSpawnAdminCommand);
            registerCommand("crimsonwarden", new CrimsonWardenAdminCommand(crimsonWardenSpawnManager));
            getServer().getPluginManager().registerEvents(monsterSpawnAdminCommand.getMonsterGui(), this);
            getLogger().info("Custom monster system enabled with " + customMonsterManager.getMonsters().size() + " monsters.");
        } else {
            registerCommand("mobspawn", (sender, command, label, args) -> {
                sender.sendMessage(org.bukkit.ChatColor.RED + "Custom monster system is disabled in config.");
                return true;
            });
            registerCommand("crimsonwarden", (sender, command, label, args) -> {
                sender.sendMessage(org.bukkit.ChatColor.RED + "Custom monster system is disabled in config.");
                return true;
            });
        }
        customWeaponCombatListener = new CustomWeaponCombatListener(
                this,
                customItemService,
                skyblockCombatEngine,
                skyblockManaManager,
                dungeonManager,
                partyManager
        );
        customWeaponCombatListener.setSkillManager(skyblockSkillManager);
        customWeaponCombatListener.reloadFromConfig();
        getServer().getPluginManager().registerEvents(customWeaponCombatListener, this);
        getServer().getPluginManager().registerEvents(new MobSunBurnListener(this), this);
        getServer().getPluginManager().registerEvents(new DungeonListener(this, dungeonManager, customItemService), this);
        getServer().getPluginManager().registerEvents(new FriendlyFireListener(dungeonManager, partyManager), this);
        getServer().getPluginManager().registerEvents(new io.papermc.Grivience.listener.MobHealthListener(this), this);
        getServer().getPluginManager().registerEvents(new io.papermc.Grivience.listener.MobSpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new io.papermc.Grivience.listener.MobTargetLimiterListener(), this);
        getServer().getPluginManager().registerEvents(new ProjectileGroundCleanupListener(), this);
        getServer().getPluginManager().registerEvents(new DrillUpgradeCraftListener(this, customItemService), this);
        getServer().getPluginManager().registerEvents(new FarmingFortuneListener(this), this);
        getServer().getPluginManager().registerEvents(new ReplenishListener(this), this);
        getServer().getPluginManager().registerEvents(new AutoPickupDropListener(this), this);
        getServer().getPluginManager().registerEvents(new AttackSpeedListener(this), this);
        getServer().getPluginManager().registerEvents(new ArmorDurabilityListener(this), this);
        getServer().getPluginManager().registerEvents(new HungerProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new SkyblockCombatRefreshListener(this, skyblockCombatEngine, skyblockManaManager), this);
        enchantTableListener = new EnchantTableListener(this);
        enchantTableListener.reloadFromConfig();
        customItemEnchantListener = new CustomItemEnchantListener(customItemService);
        getServer().getPluginManager().registerEvents(enchantTableListener, this);
        getServer().getPluginManager().registerEvents(customItemEnchantListener, this);
        getServer().getPluginManager().registerEvents(new io.papermc.Grivience.listener.GodPotionListener(this, customItemService), this);
        reforgeAnvilGuiListener = new ReforgeAnvilGuiListener(this, customItemService);
        blacksmithGuiListener = new BlacksmithGuiListener(this, customItemService);
        getServer().getPluginManager().registerEvents(reforgeAnvilGuiListener, this);
        getServer().getPluginManager().registerEvents(blacksmithGuiListener, this);
        customArmorEffectListener = new CustomArmorEffectListener(this, customItemService);
        bazaarShopManager = new BazaarShopManager(this, customItemService, customArmorManager);
        bazaarGuiManager = new BazaarGuiManager(this, bazaarShopManager);
        auctionManager = new io.papermc.Grivience.auction.AuctionManager(this);
        auctionGuiManager = new io.papermc.Grivience.auction.gui.AuctionGuiManager(this, auctionManager);
        getServer().getPluginManager().registerEvents(auctionGuiManager, this);
        npcSellShopGui = new NpcSellShopGui(bazaarShopManager);
        npcShopManager = new io.papermc.Grivience.npcshop.NpcShopManager(this, bazaarShopManager);
        elevatorManager = new io.papermc.Grivience.elevator.ElevatorManager(this);
        elevatorGui = new io.papermc.Grivience.elevator.ElevatorGui(this, elevatorManager);
        elevatorZnpcsHook = new io.papermc.Grivience.elevator.ElevatorZnpcsHook(this, elevatorManager, elevatorGui);
        getServer().getPluginManager().registerEvents(elevatorGui, this);
        elevatorZnpcsHook.hookIfAvailable();
        
        getServer().getPluginManager().registerEvents(bazaarGuiManager, this);
        getServer().getPluginManager().registerEvents(npcSellShopGui, this);
        resourcePackManager = new ResourcePackManager(this);
        getServer().getPluginManager().registerEvents(resourcePackManager, this);
        jumpPadManager = new JumpPadManager(this);
        getServer().getPluginManager().registerEvents(new JumpPadListener(this, jumpPadManager), this);
        wardrobeManager = new WardrobeManager(this, getDataFolder());
        wardrobeGui = new WardrobeGui(this, wardrobeManager);
        WardrobeCommand wardrobeCommand = new WardrobeCommand(wardrobeGui);
        registerCommand("wardrobe", wardrobeCommand);
        getServer().getPluginManager().registerEvents(wardrobeGui, this);
        questManager = new QuestManager(this);
        questGui = new QuestGui(this, questManager);
        getServer().getPluginManager().registerEvents(questGui, this);
        questZnpcsHook = new QuestZnpcsHook(this, questManager);
        wizardTowerManager = new WizardTowerManager(this);
        getServer().getPluginManager().registerEvents(wizardTowerManager, this);
        wizardTowerZnpcsHook = new WizardTowerZnpcsHook(this, wizardTowerManager);
        petManager = new PetManager(this);
        petManager.setLevelManager(skyblockLevelManager);
        petManager.registerRecipes();
        petGui = new PetGui(petManager);
        getServer().getPluginManager().registerEvents(petGui, this);
        getServer().getPluginManager().registerEvents(new PetConsumeListener(petManager), this);
        getServer().getPluginManager().registerEvents(new PetExpListener(this, petManager), this);
        getServer().getPluginManager().registerEvents(new PetSkillXpListener(this, petManager, skyblockLevelManager), this);
        getServer().getPluginManager().registerEvents(new CustomFishingListener(this), this);
        bitsManager = new BitsManager(getDataFolder());
        bossBarAnnouncementManager = new BossBarAnnouncementManager(this);
        bossBarAnnouncementManager.start();

        autoAnnouncementManager = new io.papermc.Grivience.announcement.AutoAnnouncementManager(this);
        autoAnnouncementManager.start();

        registerCommand("bitsadmin", new BitsAdminCommand(bitsManager));
        registerCommand("coinsadmin", new CoinsAdminCommand(this));
        registerCommand("sbadmin", new SkyblockLevelAdminCommand(skyblockLevelManager));
        registerCommand("bossbar", new BossBarAnnounceCommand(bossBarAnnouncementManager));
        
        // Initialize zone system
        zoneManager = new io.papermc.Grivience.zone.ZoneManager(this);
        zoneScoreboardListener = new io.papermc.Grivience.zone.ZoneScoreboardListener(this, zoneManager);
        getServer().getPluginManager().registerEvents(zoneScoreboardListener, this);
        zoneScoreboardListener.start();
        registerCommand("zone", new io.papermc.Grivience.zone.ZoneCommand(this, zoneManager));
        ensureNewbieMinesZone();
        ensureOaklysWoodDepoZone();
        ensureWheatAreaZone();
        ensureUndeadCemeteryZone();
        zoneManager.reload(); // Reload to refresh internal cache with the finalized world names
        getLogger().info("Zone system enabled with " + zoneManager.getAllZones().size() + " zones.");
        
        // Initialize nick system (LuckPerms-optional).
        if (getServer().getPluginManager().getPlugin("LuckPerms") != null) {
            nickManager = new io.papermc.Grivience.nick.NickManager(this);
            nickGuiManager = new io.papermc.Grivience.nick.NickGuiManager(this, nickManager);
            getServer().getPluginManager().registerEvents(nickGuiManager, this);
            getServer().getPluginManager().registerEvents(new io.papermc.Grivience.nick.NickListener(nickManager), this);

            io.papermc.Grivience.nick.NickCommand nickCmd = new io.papermc.Grivience.nick.NickCommand(nickManager, nickGuiManager);
            var nick = getCommand("nick");
            if (nick != null) {
                nick.setExecutor(nickCmd);
                nick.setTabCompleter(nickCmd);
            }
            var unnick = getCommand("unnick");
            if (unnick != null) {
                unnick.setExecutor(nickCmd);
                unnick.setTabCompleter(nickCmd);
            }
        } else {
            getLogger().warning("LuckPerms not found; nick system disabled.");
            CommandExecutor disabledNick = (sender, command, label, args) -> {
                sender.sendMessage(org.bukkit.ChatColor.RED + "Nicknames require LuckPerms. Install LuckPerms to use /nick.");
                return true;
            };
            registerCommand("nick", disabledNick);
            registerCommand("unnick", disabledNick);
        }
        
        guardianRecipeGui = new io.papermc.Grivience.gui.GuardianRecipeGui(this, customItemService);
        getServer().getPluginManager().registerEvents(guardianRecipeGui, this);
        getServer().getPluginManager().registerEvents(new io.papermc.Grivience.listener.SkyblockChatListener(this), this);
        getServer().getPluginManager().registerEvents(new io.papermc.Grivience.listener.GuardianFragmentListener(this, customItemService, guardianRecipeGui), this);

        // Initialize grappling hook system
        if (getConfig().getBoolean("grappling-hook.enabled", true)) {
            grapplingHookManager = new io.papermc.Grivience.item.GrapplingHookManager(this);
            customItemService.setGrapplingHookManager(grapplingHookManager);
            getServer().getPluginManager().registerEvents(
                new io.papermc.Grivience.item.GrapplingHookListener(this, grapplingHookManager), this
            );
            getLogger().info("Grappling hook system enabled.");
        }
        
        // Initialize staff system
        if (getConfig().getBoolean("staffs.enabled", true)) {
            staffManager = new io.papermc.Grivience.item.StaffManager(this);
            getServer().getPluginManager().registerEvents(
                new io.papermc.Grivience.item.StaffProtectionListener(this, staffManager), this
            );
            getLogger().info("Staff system enabled with protections.");
        }
        
        // Initialize profile system
        profileManager = new io.papermc.Grivience.skyblock.profile.ProfileManager(this);
        profileGui = new io.papermc.Grivience.skyblock.profile.gui.ProfileGui(profileManager);
        getServer().getPluginManager().registerEvents(profileGui, this);
        io.papermc.Grivience.skyblock.profile.command.ProfileCommand profileCommand = new io.papermc.Grivience.skyblock.profile.command.ProfileCommand(profileManager);
        registerCommand("profile", profileCommand);
        registerCommand("profiles", profileCommand);
        getLogger().info("Profile system enabled (Skyblock accurate).");

        dragonAscensionManager = new DragonAscensionManager(this, customItemService);
        getServer().getPluginManager().registerEvents(dragonAscensionManager, this);
        registerCommand("dragonwatcher", dragonAscensionManager);

        farmingContestManager = new FarmingContestManager(this);
        getServer().getPluginManager().registerEvents(farmingContestManager, this);
        registerCommand("farmcontest", new FarmingContestCommand(farmingContestManager));
        skyblockMenuManager.setFarmingContestManager(farmingContestManager);

        minionManager = new MinionManager(this, islandManager);
        minionGuiManager = new MinionGuiManager(this, minionManager);
        getServer().getPluginManager().registerEvents(minionGuiManager, this);
        getServer().getPluginManager().registerEvents(new MinionListener(minionManager, minionGuiManager), this);
        getServer().getPluginManager().registerEvents(new MinionCraftingListener(minionManager), this);
        getServer().getPluginManager().registerEvents(new EnchantedItemRecipeGuardListener(minionManager), this);
        registerCommand("minion", new MinionCommand(this, minionManager, minionGuiManager));
        skyblockMenuManager.setMinionGuiManager(minionGuiManager);
        getLogger().info("Minion system enabled with " + minionManager.getTotalMinions() + " placed minions.");

        personalCompactorManager = new PersonalCompactorManager(this);
        getServer().getPluginManager().registerEvents(new PersonalCompactorListener(this, personalCompactorManager), this);
        registerCommand("compactor", new PersonalCompactorCommand(personalCompactorManager));

        // Bank system (profile-scoped storage for coins).
        bankManager = new io.papermc.Grivience.bank.BankManager(this);
        io.papermc.Grivience.bank.BankCommand bankCommand = new io.papermc.Grivience.bank.BankCommand(bankManager);
        registerCommand("bank", bankCommand);
        getServer().getPluginManager().registerEvents(new io.papermc.Grivience.bank.BankListener(this, bankManager), this);

        // Skyblock core commands/listeners.
        IslandBypassCommand islandBypassCommand = new IslandBypassCommand(islandManager);
        registerCommand("islandbypass", islandBypassCommand);

        IslandProtectionListener islandProtectionListener = new IslandProtectionListener(islandManager);
        islandProtectionListener.setBypassCommand(islandBypassCommand);
        getServer().getPluginManager().registerEvents(islandProtectionListener, this);
        getServer().getPluginManager().registerEvents(new SkyblockListener(this, islandManager), this);
        getServer().getPluginManager().registerEvents(new PortalRoutingListener(this, portalRoutingManager), this);
        getServer().getPluginManager().registerEvents(new FarmHubCropRegenerationListener(this), this);

        IslandCommand islandCommand = new IslandCommand(this, islandManager, partyManager, profileManager, hubCommand);
        registerCommand("island", islandCommand);
        registerCommand("visit", islandCommand);
        registerCommand("invite", islandCommand);
        registerCommand("sbkick", islandCommand);
        registerCommand("sbkickall", islandCommand);
        registerCommand("setguestspawn", islandCommand);
        registerCommand("setspawn", islandCommand);

        SkyblockAdminCommand skyblockAdminCommand = new SkyblockAdminCommand(this, islandManager);
        registerCommand("skyblockadmin", skyblockAdminCommand);

        // Initialize mining system manager early
        miningSystemManager = new MiningSystemManager(this);
        miningEventManager = new MiningEventManager(this);
        getServer().getPluginManager().registerEvents(new SkyblockLevelListener(skyblockLevelManager, miningEventManager), this);
        inspectorShopGui = new InspectorShopGui(this, customItemService, new io.papermc.Grivience.skyblock.economy.ProfileEconomyService(this));
        getServer().getPluginManager().registerEvents(inspectorShopGui, this);
        
        drillMechanicGui = new DrillMechanicGui(this, customItemService, new io.papermc.Grivience.skyblock.economy.ProfileEconomyService(this));
        getServer().getPluginManager().registerEvents(drillMechanicGui, this);
        registerCommand("drillforge", new DrillForgeCommand(drillMechanicGui));
        
        globalEventManager = new GlobalEventManager(this, miningEventManager);
        skyblockMenuManager.setGlobalEventManager(globalEventManager);
        GlobalEventCommand globalEventCommand = new GlobalEventCommand(globalEventManager);
        registerCommand("globalevent", globalEventCommand);

        MiningEventCommand mineEventCommand = new MiningEventCommand(miningEventManager);
        var cmd = getCommand("mineevent");
        if (cmd != null) {
            cmd.setExecutor(mineEventCommand);
            cmd.setTabCompleter(mineEventCommand);
        } else {
            getLogger().warning("Command 'mineevent' not found in plugin.yml!");
        }

        // Initialize collections system (Skyblock accurate)
        collectionsManager = new CollectionsManager(this);
        collectionsManager.load();
        collectionGui = new CollectionGUI(this, collectionsManager);
        getServer().getPluginManager().registerEvents(collectionGui, this);
        collectionListener = new CollectionListener(this, collectionsManager);
        getServer().getPluginManager().registerEvents(collectionListener, this);
        getServer().getPluginManager().registerEvents(new MinehubOreGenerationListener(this), this);
        heartOfTheEndMinesManager = new HeartOfTheEndMinesManager(this, collectionsManager);
        getServer().getPluginManager().registerEvents(heartOfTheEndMinesManager, this);
        endMinesConvergenceManager = new EndMinesConvergenceManager(this, endMinesManager, collectionsManager, customItemService);
        getServer().getPluginManager().registerEvents(endMinesConvergenceManager, this);
        getServer().getPluginManager().registerEvents(new EndHubKunziteListener(this, customItemService, collectionsManager), this);
        getServer().getPluginManager().registerEvents(
                new EndMinesMiningListener(this, endMinesManager, customItemService, collectionsManager, customArmorManager, miningSystemManager, miningEventManager),
                this
        );
        endMinesMobManager = new EndMinesMobManager(this, endMinesManager, customItemService, collectionsManager);
        getServer().getPluginManager().registerEvents(endMinesMobManager, this);
        endMinesMobManager.enable();
        CollectionCommand collectionCommand = new CollectionCommand(this, collectionsManager, collectionGui);
        registerCommand("collection", collectionCommand);
        registerCommand("collections", collectionCommand);
        getLogger().info("Collections system enabled with " + collectionsManager.getEnabledCollections().size() + " collections.");

        // Register RecipeUnlockListener
        RecipeUnlockListener recipeUnlockListener = new RecipeUnlockListener(this, collectionsManager, skyblockLevelManager);
        getServer().getPluginManager().registerEvents(recipeUnlockListener, this);
        Bukkit.getOnlinePlayers().forEach(recipeUnlockListener::syncRecipeBook);
        // Ensure all custom recipes are crafted in the vanilla crafting table UI (not the 2x2 player grid).
        getServer().getPluginManager().registerEvents(new CustomRecipeWorkbenchOnlyListener(this), this);
        // Enforce collection-tier requirements when crafting via the vanilla crafting UI.
        getServer().getPluginManager().registerEvents(new RecipeCollectionGateListener(collectionsManager), this);

        // Initialize minehub heart and commission system
        minehubHeartManager = new MinehubHeartManager(this, collectionsManager);
        getServer().getPluginManager().registerEvents(minehubHeartManager, this);
        minehubCommissionManager = new MinehubCommissionManager(this, collectionsManager, minehubHeartManager);
        getServer().getPluginManager().registerEvents(minehubCommissionManager, this);
        MinehubHeartCommand minehubHeartCommand = new MinehubHeartCommand(this, minehubHeartManager, minehubCommissionManager);
        registerCommand("hotm", minehubHeartCommand);
        getLogger().info("Minehub Heart and Commission system enabled.");

        // Initialize enchantment system (Skyblock accurate)
        enchantmentManager = new EnchantmentManager(this);
        enchantmentManager.enable();
        getLogger().info("Enchantment System enabled with " + io.papermc.Grivience.enchantment.EnchantmentRegistry.count() + " enchantments.");

        // Initialize crafting system (Skyblock accurate)
        craftingManager = new CraftingManager(this);
        craftingManager.enable();
        skyblockMenuManager.setCraftingManager(craftingManager);
        getLogger().info("Crafting System enabled with " + io.papermc.Grivience.crafting.RecipeRegistry.count() + " recipes.");



        // Initialize welcome event system (Hard Hat Harry)
        welcomeManager = new WelcomeManager(this);
        welcomeManager.enable();
        getLogger().info("Welcome Event System enabled (Hard Hat Harry)");

        // Initialize welcome quest line system
        questLineManager = new QuestLineManager(this);
        questLineManager.enable();
        getLogger().info("Welcome Quest Line enabled with " + io.papermc.Grivience.welcome.quest.WelcomeQuestRegistry.count() + " quests");

        // Initialize fast travel system
        fastTravelManager = new FastTravelManager(this);
        fastTravelManager.load();
        fastTravelGui = new FastTravelGui(this, fastTravelManager);
        getServer().getPluginManager().registerEvents(new MiningItemListener(this, miningSystemManager, customItemService), this);
        getServer().getPluginManager().registerEvents(fastTravelGui, this);
        FastTravelListener fastTravelListener = new FastTravelListener(this, fastTravelManager);
        getServer().getPluginManager().registerEvents(fastTravelListener, this);
        FastTravelCommand fastTravelCommand = new FastTravelCommand(fastTravelManager, fastTravelGui);
        registerCommand("fasttravel", fastTravelCommand);
        registerCommand("ft", fastTravelCommand);
        skyblockMenuManager.setFastTravelGui(fastTravelGui);
        getLogger().info("Fast Travel system enabled with " + fastTravelManager.getAllPoints().size() + " destinations (including hub warps).");

        // Initialize storage system (Skyblock accurate)
        storageManager = new io.papermc.Grivience.storage.StorageManager(this);
        storageManager.load();
        storageGui = new io.papermc.Grivience.storage.StorageGui(this, storageManager);
        storageListener = new io.papermc.Grivience.storage.StorageListener(this, storageManager);
        getServer().getPluginManager().registerEvents(storageListener, this);
        io.papermc.Grivience.storage.StorageGuiListener storageGuiListener = new io.papermc.Grivience.storage.StorageGuiListener(this, storageManager, storageGui);
        getServer().getPluginManager().registerEvents(storageGuiListener, this);
        storageGui.setStorageListener(storageListener);
        io.papermc.Grivience.storage.StorageCommand storageCommand = new io.papermc.Grivience.storage.StorageCommand(storageManager, storageGui);
        registerCommand("storage", storageCommand);
        registerCommand("storages", storageCommand);
        registerCommand("accessory", new AccessoryCommand(this, accessoryManager));
        getLogger().info("Storage System enabled with " + io.papermc.Grivience.storage.StorageType.values().length + " storage types.");

        skyblockScoreboardManager = new SkyblockScoreboardManager(
                this,
                skyblockLevelManager,
                islandManager,
                dungeonManager,
                bitsManager,
                zoneManager,
                farmingContestManager
        );
        getServer().getPluginManager().registerEvents(skyblockScoreboardManager, this); // NEW LINE
        skyblockScoreboardManager.start();
        ReforgeCommand reforgeCommand = new ReforgeCommand(reforgeAnvilGuiListener);
        BazaarCommand bazaarCommand = new BazaarCommand(bazaarShopManager, bazaarGuiManager);
        SanityCheckCommand sanityCheckCommand = new SanityCheckCommand(this, armorCraftingListener, islandManager, customItemService);

        AdminItemResolver adminItemResolver = new AdminItemResolver(customItemService, customArmorManager, petManager, minionManager);
        DungeonCommand dungeonCommand = new DungeonCommand(this, partyManager, dungeonManager, guiManager, adminItemResolver);
        registerCommand("dungeon", dungeonCommand);
        registerCommand("party", dungeonCommand);

        GiveItemCommand giveItemCommand = new GiveItemCommand(adminItemResolver);
        registerCommand("gi", giveItemCommand);
        registerCommand("giveitem", giveItemCommand);

        registerCommand("reforge", reforgeCommand);
        registerCommand("bazaar", bazaarCommand);
        registerCommand("ah", new io.papermc.Grivience.command.AuctionCommand(auctionGuiManager));
        registerCommand("npcshop", new NpcSellShopCommand(npcSellShopGui));
        registerCommand("shop", new io.papermc.Grivience.npcshop.NpcShopCommand(npcShopManager));
        registerCommand("elevator", new io.papermc.Grivience.elevator.ElevatorCommand(elevatorManager, elevatorGui));
        registerCommand("grisanity", sanityCheckCommand);
        registerCommand("grivience", new GrivienceReloadCommand(this));
        registerCommand("discord", new io.papermc.Grivience.command.DiscordCommand(this));
        registerCommand("wardrobe", new WardrobeCommand(wardrobeGui));
        registerCommand("pets", new PetCommand(petGui));
        registerCommand("quest", new QuestCommand(questManager, questGui));
        registerCommand("admintp", new AdminTeleportCommand());
        registerCommand("jumppad", new JumpPadCommand(jumpPadManager));
        if (isWorldEditAvailable()) {
            registerCommand("mountain", new MountainCommand(this));
        } else {
            registerCommand("mountain", (sender, command, label, args) -> {
                sender.sendMessage(org.bukkit.ChatColor.RED + "FastAsyncWorldEdit or WorldEdit is required for /" + label + ".");
                return true;
            });
        }
        registerCommand("blacksmith", new BlacksmithCommand(blacksmithGuiListener));
        registerCommand("wizardtower", new WizardTowerCommand(wizardTowerManager));

        // Re-register Crafting Commands
        CraftingManager.CraftingCommand craftingCmd = new CraftingManager.CraftingCommand(craftingManager);
        registerCommand("craft", craftingCmd);
        registerCommand("crafting", craftingCmd);
        registerCommand("recipe", craftingCmd);
        registerPlaceholderExpansion();

        if (questZnpcsHook != null) {
            questZnpcsHook.hookIfAvailable();
        }
        if (wizardTowerZnpcsHook != null) {
            wizardTowerZnpcsHook.hookIfAvailable();
        }

        // Initialize Slayer System
        slayerManager = new io.papermc.Grivience.slayer.SlayerManager();
        getServer().getPluginManager().registerEvents(new io.papermc.Grivience.slayer.SlayerListener(this, slayerManager), this);
        registerCommand("slayer", new io.papermc.Grivience.slayer.SlayerCommand(slayerManager));
        slayerGui = new io.papermc.Grivience.slayer.SlayerGui(this, slayerManager);
        getServer().getPluginManager().registerEvents(slayerGui, this);
        slayerZnpcsHook = new io.papermc.Grivience.slayer.SlayerZnpcsHook(this, slayerGui);
        slayerZnpcsHook.hookIfAvailable();
        getLogger().info("Slayer system initialized.");

        // Initialize Soul System
        soulManager = new io.papermc.Grivience.soul.SoulManager(this);
        getServer().getPluginManager().registerEvents(new io.papermc.Grivience.soul.SoulListener(this, soulManager), this);
        registerCommand("soul", new io.papermc.Grivience.soul.SoulCommand(soulManager));

        // Initialize Mayor System
        mayorManager = new io.papermc.Grivience.mayor.MayorManager(this);
        registerCommand("mayor", new io.papermc.Grivience.mayor.MayorCommand(mayorManager));
        mayorGui = new io.papermc.Grivience.mayor.MayorGui(this, mayorManager);
        getServer().getPluginManager().registerEvents(mayorGui, this);
        mayorZnpcsHook = new io.papermc.Grivience.mayor.MayorZnpcsHook(this, mayorGui);
        mayorZnpcsHook.hookIfAvailable();
        mayorManager.setZnpcsHook(mayorZnpcsHook);
        mayorManager.load();
        getLogger().info("Mayor system initialized.");
    } // Missing closing brace for onEnable()



    @Override
    public void onDisable() {
        if (mayorManager != null) {
            mayorManager.stop();
            mayorManager.save();
        }
        if (serverPerformanceMonitor != null) {
            serverPerformanceMonitor.shutdown();
        }
        if (griviencePlaceholderExpansion != null && griviencePlaceholderExpansion.isRegistered()) {
            griviencePlaceholderExpansion.unregister();
        }
        if (customArmorEffectListener != null) {
            customArmorEffectListener.shutdown();
        }
        if (crimsonWardenSpawnManager != null) {
            crimsonWardenSpawnManager.shutdown();
        }
        if (skyblockCombatEngine != null) {
            skyblockCombatEngine.shutdown();
        }
        if (minehubHeartManager != null) {
            minehubHeartManager.shutdown();
        }
        if (minehubCommissionManager != null) {
            minehubCommissionManager.shutdown();
        }
        if (heartOfTheEndMinesManager != null) {
            heartOfTheEndMinesManager.shutdown();
        }
        if (endMinesConvergenceManager != null) {
            endMinesConvergenceManager.shutdown();
        }
        if (skyblockManaManager != null) {
            skyblockManaManager.shutdown();
        }
        if (skyblockScoreboardManager != null) {
            skyblockScoreboardManager.shutdown();
        }
        if (skyblockLevelManager != null) {
            skyblockLevelManager.shutdown();
        }
        if (dungeonManager != null) {
            dungeonManager.shutdown();
        }
        if (bazaarShopManager != null) {
            bazaarShopManager.shutdown();
        }
        if (drillMechanicGui != null) {
            drillMechanicGui.shutdown();
        }
        if (endMinesMobManager != null) {
            endMinesMobManager.shutdown();
        }
        if (endMinesManager != null) {
            endMinesManager.shutdown();
        }
        if (resourcePackManager != null) {
            resourcePackManager.shutdown();
        }
        if (bossBarAnnouncementManager != null) {
            bossBarAnnouncementManager.shutdown();
        }
        if (autoAnnouncementManager != null) {
            autoAnnouncementManager.stop();
        }
        if (wardrobeManager != null) {
            wardrobeManager.saveAll();
        }
        if (storageManager != null) {
            storageManager.disable();
        }
        if (personalCompactorManager != null) {
            personalCompactorManager.shutdown();
        }
        if (minionManager != null) {
            minionManager.shutdown();
        }
        if (islandManager != null) {
            islandManager.shutdown();
        }
        if (miningEventManager != null) {
            miningEventManager.cleanup();
        }
        if (wizardTowerManager != null) {
            wizardTowerManager.shutdown();
        }
        if (staffManager != null) {
            staffManager = null;
        }
        if (collectionListener != null) {
            collectionListener.shutdown();
        }
        if (collectionsManager != null) {
            collectionsManager.disable();
        }
        if (profileManager != null) {
            profileManager.shutdown();
        }
        if (dragonAscensionManager != null) {
            dragonAscensionManager.shutdown();
        }
        if (farmingContestManager != null) {
            farmingContestManager.shutdown();
        }
        if (enchantmentManager != null) {
            enchantmentManager.disable();
        }
        if (craftingManager != null) {
            craftingManager.disable();
        }

        if (welcomeManager != null) {
            welcomeManager.disable();
        }
        if (questLineManager != null) {
            questLineManager.disable();
        }
        if (fastTravelManager != null) {
            fastTravelManager.save();
        }
        if (zoneScoreboardListener != null) {
            zoneScoreboardListener.stop();
        }
        grapplingHookManager = null;
        if (jumpPadManager != null) {
            jumpPadManager.save();
        }
        jumpPadManager = null;
    }

    public void reloadSystems() {
        reloadConfig();
        applyConfigUpgrades();
        if (mayorManager != null) {
            mayorManager.load();
        }
        if (serverPerformanceMonitor != null) {
            serverPerformanceMonitor.reload();
        }
        if (partyManager != null) {
            partyManager.reloadFromConfig();
        }
        // IslandManager reload - preserves all island data
        if (islandManager != null) {
            islandManager.loadConfig();
            islandManager.initializeWorld();
            // Note: Islands are NOT cleared during reload to prevent data loss
            getLogger().info("Islands preserved during reload: " + islandManager.getAllIslands().size() + " islands.");
        }
        if (minionManager != null) {
            minionManager.reloadFromConfig();
        }
        if (dungeonManager != null) {
            dungeonManager.reloadFromConfig();
        }
        if (endMinesManager != null) {
            endMinesManager.shutdown();
            endMinesManager.initializeWorld();
        }
        if (minehubWorldManager != null) {
            minehubWorldManager.initializeWorld();
        }
        if (hubWorldFeaturesManager != null) {
            hubWorldFeaturesManager.applyNow();
        }
        if (endMinesMobManager != null) {
            endMinesMobManager.shutdown();
            endMinesMobManager.enable();
        }
        if (endMinesConvergenceManager != null) {
            endMinesConvergenceManager.reload();
        }
        if (customItemService != null) {
            customItemService.reloadFromConfig();
            customItemService.registerRecipes();
        }
        if (skyblockLevelManager != null) {
            skyblockLevelManager.save();
            skyblockLevelManager.load();
        }
        if (skyblockStatsManager != null) {
            skyblockStatsManager.reload();
        }
        if (skyblockManaManager != null) {
            skyblockManaManager.shutdown();
            skyblockManaManager.start();
        }
        if (skyblockCombatEngine != null) {
            skyblockCombatEngine.reload();
        }
        if (skyblockScoreboardManager != null) {
            skyblockScoreboardManager.reload();
        }
        if (customWeaponCombatListener != null) {
            customWeaponCombatListener.reloadFromConfig();
        }
        if (enchantTableListener != null) {
            enchantTableListener.reloadFromConfig();
        }
        if (enchantmentManager != null) {
            enchantmentManager.reload();
        }
        if (craftingManager != null) {
            craftingManager.reload();
        }

        if (welcomeManager != null) {
            welcomeManager.reload();
        }
        if (wizardTowerManager != null) {
            wizardTowerManager.reload();
        }
        if (questZnpcsHook != null) {
            questZnpcsHook.hookIfAvailable();
        }
        if (wizardTowerZnpcsHook != null) {
            wizardTowerZnpcsHook.hookIfAvailable();
        }
        if (questLineManager != null) {
            questLineManager.reload();
        }
        if (fastTravelManager != null) {
            fastTravelManager.load();
        }
        if (customArmorManager != null) {
            customArmorManager.reloadFromConfig();
            if (armorCraftingListener != null) {
                armorCraftingListener.registerRecipes();
            }
        }
        if (customMonsterManager != null) {
            customMonsterManager.reload();
        }
        if (crimsonWardenSpawnManager != null) {
            crimsonWardenSpawnManager.reload();
        }
        if (bazaarShopManager != null) {
            bazaarShopManager.reloadFromConfig();
        }
        if (rewardChestManager != null) {
            rewardChestManager.reloadFromConfig();
        }
        if (resourcePackManager != null) {
            resourcePackManager.reloadFromConfig();
        }
        if (jumpPadManager != null) {
            jumpPadManager.reload();
        }
        if (questManager != null) {
            questManager.reload();
        }
        if (petManager != null) {
            petManager.reloadPets();
            petManager.registerRecipes();
        }
        if (bossBarAnnouncementManager != null) {
            bossBarAnnouncementManager.reload();
        }
        if (zoneManager != null) {
            zoneManager.reload();
        }
        if (zoneScoreboardListener != null) {
            zoneScoreboardListener.reload();
        }
        if (grapplingHookManager != null) {
            grapplingHookManager.reloadFromConfig();
        }
        if (staffManager != null) {
            staffManager.reloadFromConfig();
        }
        if (profileManager != null) {
            profileManager.loadConfig();
        }
        if (dragonAscensionManager != null) {
            dragonAscensionManager.reload();
        }
        if (farmingContestManager != null) {
            farmingContestManager.reload();
        }
        if (collectionsManager != null) {
            collectionsManager.reload();
        }
        if (storageManager != null) {
            storageManager.reload();
        }
        getLogger().info("All systems reloaded. Islands preserved.");
    }

    public io.papermc.Grivience.gui.SkyblockLevelGui getSkyblockLevelGui() {
        return skyblockLevelGui;
    }

    public SkyblockLevelManager getSkyblockLevelManager() {
        return skyblockLevelManager;
    }

    public SkyblockStatsManager getSkyblockStatsManager() {
        return skyblockStatsManager;
    }

    public SkyblockManaManager getSkyblockManaManager() {
        return skyblockManaManager;
    }

    public CollectionsManager getCollectionsManager() {
        return collectionsManager;
    }

    public MinehubHeartManager getMinehubHeartManager() {
        return minehubHeartManager;
    }

    public MinehubCommissionManager getMinehubCommissionManager() {
        return minehubCommissionManager;
    }

    public CollectionGUI getCollectionGui() {
        return collectionGui;
    }

    public io.papermc.Grivience.storage.StorageManager getStorageManager() {
        return storageManager;
    }

    public io.papermc.Grivience.storage.StorageGui getStorageGui() {
        return storageGui;
    }

    public AccessoryManager getAccessoryManager() {
        return accessoryManager;
    }

    public PersonalCompactorManager getPersonalCompactorManager() {
        return personalCompactorManager;
    }

    public CustomArmorManager getCustomArmorManager() {
        return customArmorManager;
    }

    public ArmorCraftingListener getArmorCraftingListener() {
        return armorCraftingListener;
    }

    public ArmorSetBonusListener getArmorSetBonusListener() {
        return armorSetBonusListener;
    }

    public CustomMonsterManager getCustomMonsterManager() {
        return customMonsterManager;
    }

    public CraftingManager getCraftingManager() {
        return craftingManager;
    }
    
    public io.papermc.Grivience.npcshop.NpcShopManager getNpcShopManager() {
        return npcShopManager;
    }

    public io.papermc.Grivience.slayer.SlayerManager getSlayerManager() {
        return slayerManager;
    }

    public io.papermc.Grivience.mayor.MayorManager getMayorManager() {
        return mayorManager;
    }

    public io.papermc.Grivience.soul.SoulManager getSoulManager() {
        return soulManager;
    }

    public io.papermc.Grivience.fasttravel.FastTravelManager getFastTravelManager() {
        return fastTravelManager;
    }

    public io.papermc.Grivience.zone.ZoneManager getZoneManager() {
        return zoneManager;
    }

    public io.papermc.Grivience.mines.end.mob.EndMinesMobManager getEndMinesMobManager() {
        return endMinesMobManager;
    }

    public HeartOfTheEndMinesManager getHeartOfTheEndMinesManager() {
        return heartOfTheEndMinesManager;
    }

    public EndMinesConvergenceManager getEndMinesConvergenceManager() {
        return endMinesConvergenceManager;
    }

    public io.papermc.Grivience.skyblock.island.IslandManager getIslandManager() {
        return islandManager;
    }

    public TradeManager getTradeManager() {
        return tradeManager;
    }

    public io.papermc.Grivience.skyblock.profile.ProfileManager getProfileManager() {
        return profileManager;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public io.papermc.Grivience.pet.PetManager getPetManager() {
        return petManager;
    }

    public io.papermc.Grivience.skyblock.profile.gui.ProfileGui getProfileGui() {
        return profileGui;
    }

    public FarmingContestManager getFarmingContestManager() {
        return farmingContestManager;
    }

    public MinionManager getMinionManager() {
        return minionManager;
    }

    public MinionGuiManager getMinionGuiManager() {
        return minionGuiManager;
    }

    public SkyblockScoreboardManager getSkyblockScoreboardManager() {
        return skyblockScoreboardManager;
    }

    public io.papermc.Grivience.dungeon.DungeonManager getDungeonManager() {
        return dungeonManager;
    }

    public io.papermc.Grivience.skills.SkyblockSkillManager getSkyblockSkillManager() {
        return skyblockSkillManager;
    }

    public SkyblockMenuManager getSkyblockMenuManager() {
        return skyblockMenuManager;
    }

    public io.papermc.Grivience.nick.NickManager getNickManager() {
        return nickManager;
    }

    public io.papermc.Grivience.item.StaffManager getStaffManager() {
        return staffManager;
    }

    public io.papermc.Grivience.enchantment.EnchantmentManager getEnchantmentManager() {
        return enchantmentManager;
    }

    public MiningSystemManager getMiningSystemManager() {
        return miningSystemManager;
    }

    public MiningEventManager getMiningEventManager() {
        return miningEventManager;
    }

    public WizardTowerManager getWizardTowerManager() {
        return wizardTowerManager;
    }

    public InspectorShopGui getInspectorShopGui() {
        return inspectorShopGui;
    }

    public NpcSellShopGui getNpcSellShopGui() {
        return npcSellShopGui;
    }

    public DrillMechanicGui getDrillMechanicGui() {
        return drillMechanicGui;
    }

    public GlobalEventManager getGlobalEventManager() {
        return globalEventManager;
    }

    public SkyblockCombatEngine getSkyblockCombatEngine() {
        return skyblockCombatEngine;
    }

    public ServerPerformanceMonitor getServerPerformanceMonitor() {
        return serverPerformanceMonitor;
    }

    public CustomItemService getCustomItemService() {
        return customItemService;
    }

    public org.bukkit.NamespacedKey getGrapplingHookKey() {
        return grapplingHookKey;
    }

    private void registerPlaceholderExpansion() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }

        griviencePlaceholderExpansion = new GriviencePlaceholderExpansion(this);
        if (griviencePlaceholderExpansion.register()) {
            getLogger().info("Registered PlaceholderAPI expansion for Skyblock placeholders.");
            return;
        }

        getLogger().warning("Failed to register PlaceholderAPI expansion.");
    }

    private void registerCommand(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("Command '" + name + "' is missing from plugin.yml.");
            return;
        }
        command.setExecutor(executor);
        if (executor instanceof TabCompleter tabCompleter) {
            command.setTabCompleter(tabCompleter);
        }
    }

    private boolean isWorldEditAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("WorldEdit")
                || Bukkit.getPluginManager().isPluginEnabled("FastAsyncWorldEdit");
    }

    private void applyConfigUpgrades() {
        // Adds new config sections/keys when updating without overwriting existing user values.
        var config = getConfig();
        config.addDefault("navigation-item.enabled", true);

        // Global damage tuning for custom mobs (multiplies the configured/base attack damage values).
        config.addDefault("custom-monsters.damage-multiplier", 1.25D);
        config.addDefault("dungeons.mob-damage-multiplier", 1.25D);

        // Hypixel-style island visiting defaults.
        config.addDefault("skyblock.visiting.default-policy", "OFF");
        config.addDefault("skyblock.visiting.invite-timeout-seconds", 60);
        config.addDefault("skyblock.visiting.guild-meta-key", "guild");
        config.addDefault("skyblock.visiting.guest-limit.default", 1);
        config.addDefault("skyblock.visiting.guest-limit.permission-limits", List.of(
                "grivience.visit.guestlimit.vip=3",
                "grivience.visit.guestlimit.vipplus=4",
                "grivience.visit.guestlimit.vipplusplus=5",
                "grivience.visit.guestlimit.mvp=6",
                "grivience.visit.guestlimit.mvpplus=7",
                "grivience.visit.guestlimit.mvpplusplus=10",
                "grivience.visit.guestlimit.youtuber=15"
        ));

        // Keep inventory on, but apply Skyblock-style coin loss on death (purse only).
        config.addDefault("skyblock.death.purse-loss.enabled", true);
        config.addDefault("skyblock.death.purse-loss.percent", 0.5D);
        // If true, right-clicking a crafting table opens the custom /craft GUI instead of the vanilla UI.
        config.addDefault("crafting.override-crafting-table", false);
        config.addDefault("collections.auto-save-interval-seconds", 300);
        config.addDefault("skyblock.auto-pickup.block-drops", true);
        config.addDefault("personal-compactor.enabled", true);
        config.addDefault("personal-compactor.auto-compact", true);
        config.addDefault("skyblock-guide.first-join-auto-open", true);
        config.addDefault("skyblock-guide.first-join-open-delay-ticks", 80L);
        config.addDefault("skyblock-guide.first-join-chat-tip", true);
        applyPluginHiderDefaults(config);
        String base = "dungeons.reward-chest.";
        config.addDefault(base + "options-per-run", 5);
        addPoolDefault(config, "coins_small", 60, "EMERALD", "&aCoin Pouch",
                List.of("&7Vault deposit", "&6+15,000 coins"),
                List.of("eco give {player} 15000"));
        addPoolDefault(config, "coins_large", 25, "EMERALD_BLOCK", "&6Fortune Cache",
                List.of("&7Larger coin bundle", "&6+50,000 coins"),
                List.of("eco give {player} 50000"));
        addPoolDefault(config, "essence", 35, "EXPERIENCE_BOTTLE", "&bDungeon Essence",
                List.of("&7Infuse gear or trades", "&b+8 levels"),
                List.of("xp give {player} 8 levels"));
        addPoolDefault(config, "reforge_stone", 20, "ENCHANTED_BOOK", "&dReforge Stone",
                List.of("&7Random dungeon reforge stone"),
                List.of("reforge give {player} random"));
        addPoolDefault(config, "boss_relic", 15, "NETHERITE_SCRAP", "&cBoss Relic Cache",
                List.of("&7High-tier crafting mats"),
                List.of("give {player} netherite_ingot 2"));
        applyDungeonHubDefaults(config);
        applyMinehubDefaults(config);
        applyFarmhubDefaults(config);
        applyHubFeatureDefaults(config);
        applyCustomFishingDefaults(config);
        applyPortalRoutingDefaults(config);
        applyEndHubDefaults(config);
        applyEndMinesDefaults(config);
        applyDragonAscensionDefaults(config);
        applyCrimsonWardenDefaults(config);
        applyVoidMinerArmorDefaults(config);
        applyHarvesterEmbraceArmorDefaults(config);
        applyGildedHarvesterArmorDefaults(config);
        applyRootboundGarbArmorDefaults(config);
        applyTaterguardArmorDefaults(config);
        applyMelonMonarchArmorDefaults(config);
        applyWartwovenRegaliaArmorDefaults(config);
        applyAbyssalDiverArmorDefaults(config);
        applyCrypticConquerorArmorDefaults(config);
        applyArcaneWeaverArmorDefaults(config);
        applySkyblockLevelingDefaults(config);
        applySkyblockPetDefaults(config);
        applyMinionDefaults(config);
        applyWizardTowerDefaults(config);
        applyFarmingContestDefaults(config);
        applyJumpPadDefaults(config);
        applyScoreboardDefaults(config);
        applyPerformanceMonitorDefaults(config);
        applyResourcePackDefaults(config);
        config.options().copyDefaults(true);
        saveConfig();
    }

    private void addPoolDefault(
            org.bukkit.configuration.file.FileConfiguration config,
            String id,
            int weight,
            String icon,
            String name,
            List<String> lore,
            List<String> commands
    ) {
        String path = "dungeons.reward-chest.pools." + id + ".";
        config.addDefault(path + "weight", weight);
        config.addDefault(path + "icon", icon);
        config.addDefault(path + "name", name);
        config.addDefault(path + "lore", lore);
        config.addDefault(path + "commands", commands);
    }

    private void applyPluginHiderDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "security.plugin-hider.";
        config.addDefault(base + "enabled", true);
        config.addDefault(base + "bypass-permission", "grivience.pluginhider.bypass");
        config.addDefault(base + "block-message", "&cUnknown command. Type \"/help\" for help.");
        config.addDefault(base + "blocked-commands", List.of(
                "pl",
                "plugins",
                "bukkit:pl",
                "bukkit:plugins",
                "ver",
                "version",
                "bukkit:ver",
                "bukkit:version",
                "paper:plugins",
                "paper:version",
                "spigot:plugins",
                "spigot:version",
                "icanhasbukkit"
        ));
    }

    private void applyDungeonHubDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        config.addDefault("dungeons.hub.world", "world");
        config.addDefault("dungeons.hub.x", 0.0D);
        config.addDefault("dungeons.hub.y", 120.0D);
        config.addDefault("dungeons.hub.z", 0.0D);
        config.addDefault("dungeons.hub.yaw", 0.0D);
        config.addDefault("dungeons.hub.pitch", 0.0D);
    }

    private void applyPortalRoutingDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        boolean legacyNetherEnabled = config.getBoolean("skyblock.nether-portal-to-hub.enabled", true);
        config.addDefault("skyblock.portal-routing.nether.enabled", legacyNetherEnabled);
        config.addDefault("skyblock.portal-routing.nether.target", "hub");
        config.addDefault("skyblock.portal-routing.nether.world-name", config.getString("skyblock.hub-world", "world"));
        config.addDefault("skyblock.portal-routing.end.enabled", false);
        config.addDefault("skyblock.portal-routing.end.target", "hub");
        config.addDefault("skyblock.portal-routing.end.world-name", "world_the_end");
    }

    private void applyMinehubDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "skyblock.minehub-";
        config.addDefault(base + "world", "minehub_world");

        config.addDefault(base + "spawn.x", 0.5D);
        config.addDefault(base + "spawn.y", 100.0D);
        config.addDefault(base + "spawn.z", 0.5D);
        config.addDefault(base + "spawn.yaw", 0.0D);
        config.addDefault(base + "spawn.pitch", 0.0D);

        String oreGen = "skyblock.minehub-ore-gen.";
        config.addDefault(oreGen + "enabled", true);
        config.addDefault(oreGen + "delay-ticks", 60L);
        config.addDefault(oreGen + "placeholder-block", "BEDROCK");
        config.addDefault(oreGen + "breakable-blocks", List.of(
                "COAL_ORE",
                "IRON_ORE",
                "COPPER_ORE",
                "GOLD_ORE",
                "LAPIS_ORE",
                "LAPIS_BLOCK",
                "REDSTONE_ORE",
                "REDSTONE_BLOCK",
                "DIAMOND_ORE",
                "DIAMOND_BLOCK",
                "EMERALD_ORE",
                "EMERALD_BLOCK",
                "DEEPSLATE_COAL_ORE",
                "DEEPSLATE_IRON_ORE",
                "DEEPSLATE_COPPER_ORE",
                "DEEPSLATE_GOLD_ORE",
                "DEEPSLATE_LAPIS_ORE",
                "DEEPSLATE_REDSTONE_ORE",
                "DEEPSLATE_DIAMOND_ORE",
                "DEEPSLATE_EMERALD_ORE",
                "OBSIDIAN",
                "BLUE_STAINED_GLASS",
                "BLUE_STAINED_GLASS_PANE",
                "BLUE_CONCRETE_POWDER"
        ));

        String yLevels = oreGen + "y-levels.";

        String surface = yLevels + "surface.";
        config.addDefault(surface + "min-y", 91);
        config.addDefault(surface + "max-y", 138);
        config.addDefault(surface + "ores.COAL_ORE", 55);
        config.addDefault(surface + "ores.IRON_ORE", 30);
        config.addDefault(surface + "ores.COPPER_ORE", 15);

        String mid = yLevels + "mid.";
        config.addDefault(mid + "min-y", 70);
        config.addDefault(mid + "max-y", 90);
        config.addDefault(mid + "ores.IRON_ORE", 40);
        config.addDefault(mid + "ores.COAL_ORE", 30);
        config.addDefault(mid + "ores.GOLD_ORE", 15);
        config.addDefault(mid + "ores.COPPER_ORE", 15);

        String lapis = yLevels + "lapis_exclusive.";
        config.addDefault(lapis + "min-y", 26);
        config.addDefault(lapis + "max-y", 69);
        config.addDefault(lapis + "ores.LAPIS_ORE", 50);
        config.addDefault(lapis + "ores.LAPIS_BLOCK", 50);

        String redstone = yLevels + "redstone_exclusive.";
        config.addDefault(redstone + "min-y", 1);
        config.addDefault(redstone + "max-y", 25);
        config.addDefault(redstone + "ores.REDSTONE_ORE", 50);
        config.addDefault(redstone + "ores.REDSTONE_BLOCK", 50);

        String emerald = yLevels + "emerald_exclusive.";
        config.addDefault(emerald + "min-y", -32);
        config.addDefault(emerald + "max-y", 0);
        config.addDefault(emerald + "ores.EMERALD_ORE", 50);
        config.addDefault(emerald + "ores.EMERALD_BLOCK", 50);

        String diamond = yLevels + "diamond_exclusive.";
        config.addDefault(diamond + "min-y", -49);
        config.addDefault(diamond + "max-y", -33);
        config.addDefault(diamond + "ores.DIAMOND_ORE", 35);
        config.addDefault(diamond + "ores.DIAMOND_BLOCK", 25);
        config.addDefault(diamond + "ores.OBSIDIAN", 20);
        config.addDefault(diamond + "ores.BLUE_STAINED_GLASS", 10);
        config.addDefault(diamond + "ores.BLUE_STAINED_GLASS_PANE", 5);
        config.addDefault(diamond + "ores.BLUE_CONCRETE_POWDER", 5);

        String abyss = yLevels + "abyss.";
        config.addDefault(abyss + "min-y", -58);
        config.addDefault(abyss + "max-y", -50);
        config.addDefault(abyss + "ores.DEEPSLATE_IRON_ORE", 50);
        config.addDefault(abyss + "ores.DEEPSLATE_GOLD_ORE", 30);
        config.addDefault(abyss + "ores.DEEPSLATE_COAL_ORE", 20);
    }

    private void applyFarmhubDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "skyblock.farmhub-";
        config.addDefault(base + "world", "world");
        config.addDefault(base + "spawn.x", 0.5D);
        config.addDefault(base + "spawn.y", 70.0D);
        config.addDefault(base + "spawn.z", 0.5D);
        config.addDefault(base + "spawn.yaw", 0.0D);
        config.addDefault(base + "spawn.pitch", 0.0D);

        String area = "skyblock.farmhub-crop-area.";
        config.addDefault(area + "enabled", false);
        config.addDefault(area + "world", "world");
        config.addDefault(area + "pos1.x", 0);
        config.addDefault(area + "pos1.y", 64);
        config.addDefault(area + "pos1.z", 0);
        config.addDefault(area + "pos2.x", 0);
        config.addDefault(area + "pos2.y", 64);
        config.addDefault(area + "pos2.z", 0);

        String regen = "skyblock.farmhub-crop-regen.";
        config.addDefault(regen + "enabled", true);
        config.addDefault(regen + "delay-ticks", 60L);

        String growth = "skyblock.farmhub-crop-growth.";
        config.addDefault(growth + "enabled", true);
        config.addDefault(growth + "multiplier", 4.0D);
        config.addDefault(growth + "force-growth", true);
        config.addDefault(growth + "force-interval-ticks", 20L);
        config.addDefault(growth + "max-scan-blocks", 300000L);

        String farmland = "skyblock.farmhub-farmland.";
        config.addDefault(farmland + "force-hydrated", true);
        config.addDefault(farmland + "hydrate-interval-ticks", 40L);
        config.addDefault(farmland + "max-scan-blocks", 350000L);

        String maintenance = "skyblock.farmhub-maintenance.";
        config.addDefault(maintenance + "scan-blocks-per-run", 50000L);
    }

    private void applyHubFeatureDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String features = "skyblock.hub-features.";
        config.addDefault(features + "enable-mob-spawning", true);

        String area = "skyblock.hub-crop-area.";
        config.addDefault(area + "enabled", false);
        config.addDefault(area + "world", config.getString("skyblock.hub-world", "world"));
        config.addDefault(area + "pos1.x", 0);
        config.addDefault(area + "pos1.y", 64);
        config.addDefault(area + "pos1.z", 0);
        config.addDefault(area + "pos2.x", 0);
        config.addDefault(area + "pos2.y", 64);
        config.addDefault(area + "pos2.z", 0);

        String regen = "skyblock.hub-crop-regen.";
        config.addDefault(regen + "enabled", true);
        config.addDefault(regen + "delay-ticks", 60L);

        String tree = "skyblock.hub-tree-regen.";
        config.addDefault(tree + "enabled", true);
        config.addDefault(tree + "delay-ticks", 120L);
        config.addDefault(tree + "max-tree-blocks", 256);

        String growth = "skyblock.hub-crop-growth.";
        config.addDefault(growth + "enabled", true);
        config.addDefault(growth + "multiplier", 4.0D);
        config.addDefault(growth + "force-growth", true);
        config.addDefault(growth + "force-interval-ticks", 20L);
        config.addDefault(growth + "max-scan-blocks", 300000L);

        String farmland = "skyblock.hub-farmland.";
        config.addDefault(farmland + "force-hydrated", true);
        config.addDefault(farmland + "hydrate-interval-ticks", 40L);
        config.addDefault(farmland + "max-scan-blocks", 350000L);

        String maintenance = "skyblock.hub-maintenance.";
        config.addDefault(maintenance + "scan-blocks-per-run", 50000L);
    }

    private void applyCustomFishingDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "skyblock.custom-fishing.";
        config.addDefault(base + "enabled", true);
        config.addDefault(base + "allowed-worlds", List.of("hub", "farmhub"));
        config.addDefault(base + "require-open-water", true);

        String wait = base + "base-wait.";
        config.addDefault(wait + "min-ticks", 100);
        config.addDefault(wait + "max-ticks", 260);

        String lure = base + "base-lure.";
        config.addDefault(lure + "min-ticks", 20);
        config.addDefault(lure + "max-ticks", 45);

        String speed = base + "speed.";
        config.addDefault(speed + "lure-level-reduction", 0.10D);
        config.addDefault(speed + "abyssal-diver-two-piece-multiplier", 0.80D);

        String rare = base + "rare-loot.";
        config.addDefault(rare + "base-bonus", 0.00D);
        config.addDefault(rare + "abyssal-diver-full-set-bonus", 0.08D);
        config.addDefault(rare + "rain-bonus", 0.02D);
        config.addDefault(rare + "night-bonus", 0.03D);
        config.addDefault(rare + "deep-water-threshold", 4);
        config.addDefault(rare + "deep-water-bonus", 0.03D);
    }

    private void applyEndHubDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "end-hub.kunzite.";
        String defaultEndWorld = config.getString("skyblock.portal-routing.end.world-name", "world_the_end");
        config.addDefault(base + "enabled", true);
        config.addDefault(base + "world-name", defaultEndWorld);
        config.addDefault(base + "regen.enabled", true);
        config.addDefault(base + "regen.placeholder-block", "BEDROCK");
        config.addDefault(base + "respawn-delay-ticks", 60);
        config.addDefault(base + "drop-min", 1);
        config.addDefault(base + "drop-max", 2);
        config.addDefault(base + "blocks", List.of(
                "PINK_STAINED_GLASS",
                "PINK_STAINED_GLASS_PANE",
                "PINK_CONCRETE",
                "PINK_WOOL"
        ));
    }

    private void applyJumpPadDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "jump-pads.";
        config.addDefault(base + "horizontal-velocity", 2.20D);
        config.addDefault(base + "vertical-boost", 1.04D);
        config.addDefault(base + "arrival-teleport-delay-ticks", 15);
        config.addDefault(base + "midpoint-teleport-ratio", 0.50D);
        config.addDefault(base + "arrival-y-offset", 0.15D);
        config.addDefault(base + "cooldown-ms", 450L);
        config.addDefault(base + "fall-cancel-window-ms", 3000L);
        config.addDefault(base + "hypixel-effects", true);
        config.addDefault(base + "min-vertical-speed", 0.58D);
        config.addDefault(base + "max-vertical-speed", 1.42D);
        config.addDefault(base + "max-horizontal-speed", 3.85D);
        config.addDefault(base + "post-teleport-horizontal-speed", 0.0D);
        config.addDefault(base + "post-teleport-vertical-speed", 0.10D);
        config.addDefault(base + "post-teleport-lock-ticks", 6);
        config.addDefault(base + "cross-world-preview-distance", 8.0D);
        config.addDefault(base + "cross-world-preview-rise", 3.0D);
    }

    private void applyEndMinesDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "end-mines.";

        config.addDefault(base + "enabled", true);
        config.addDefault(base + "world-name", "skyblock_end_mines");
        config.addDefault(base + "required-level", 25);

        config.addDefault(base + "auto-generate", true);
        config.addDefault(base + "generated", false);

        config.addDefault(base + "spawn.x", 0.5D);
        config.addDefault(base + "spawn.y", 80.0D);
        config.addDefault(base + "spawn.z", 0.5D);
        config.addDefault(base + "spawn.yaw", 0.0D);
        config.addDefault(base + "spawn.pitch", 0.0D);

        config.addDefault(base + "generation.blocks-per-tick", 2500);
        config.addDefault(base + "generation.radius", 48);
        config.addDefault(base + "generation.min-y", 60);
        config.addDefault(base + "generation.max-y", 78);
        config.addDefault(base + "generation.seed", 0L);

        config.addDefault(base + "mining.regen.enabled", true);
        config.addDefault(base + "mining.regen.delay-ticks", 60);
        config.addDefault(base + "mining.regen.placeholder-block", "BEDROCK");
        config.addDefault(base + "mining.tool-requirements.enabled", true);
        config.addDefault(base + "mining.tool-requirements.end-stone-breaking-power", 3);
        config.addDefault(base + "mining.tool-requirements.obsidian-breaking-power", 6);
        config.addDefault(base + "mining.tool-requirements.crystal-breaking-power", 7);
        config.addDefault(base + "mining.crystal-nodes.amethyst-hits", 3);
        config.addDefault(base + "mining.crystal-nodes.budding-hits", 4);
        config.addDefault(base + "mining.crystal-nodes.hit-reset-ms", 4000L);
        config.addDefault(base + "mining.crystal-nodes.regen-delay-ticks", 180);
        config.addDefault(base + "mining.crystal-nodes.placeholder-block", "BEDROCK");
        config.addDefault(base + "mining.crystal-nodes.reward-multiplier", 2);
        config.addDefault(base + "heart.enabled", true);
        config.addDefault(base + "heart.starting-level", 1);
        config.addDefault(base + "heart.starting-tokens", 1);
        config.addDefault(base + "heart.tokens-per-level", 1);
        config.addDefault(base + "heart.tier-token-rewards", List.of(0, 1, 2, 2, 2, 2, 2, 3, 2, 2, 2));
        config.addDefault(base + "heart.auto-save-interval-seconds", 300);
        config.addDefault(base + "heart.level-thresholds", DEFAULT_HEART_OF_END_MINES_THRESHOLDS);
        config.addDefault(base + "heart.rewards.end-mine-xp", 1);
        config.addDefault(base + "heart.rewards.end-mine-dust-min", 1);
        config.addDefault(base + "heart.rewards.end-mine-dust-max", 2);
        config.addDefault(base + "heart.rewards.kunzite-xp", 8);
        config.addDefault(base + "heart.rewards.kunzite-dust-min", 2);
        config.addDefault(base + "heart.rewards.kunzite-dust-max", 4);
        config.addDefault(base + "convergence.enabled", true);
        config.addDefault(base + "convergence.cooldown-seconds", 3600L);
        config.addDefault(base + "convergence.altar-radius", 4.5D);
        config.addDefault(base + "convergence.altar.world", "");
        config.addDefault(base + "convergence.altar.x", 0.5D);
        config.addDefault(base + "convergence.altar.y", 80.0D);
        config.addDefault(base + "convergence.altar.z", 0.5D);
        config.addDefault(base + "convergence.altar.yaw", 0.0D);
        config.addDefault(base + "convergence.altar.pitch", 0.0D);
        config.addDefault(base + "convergence.altar.material", "RESPAWN_ANCHOR");
        config.addDefault(base + "convergence.hologram.enabled", true);
        config.addDefault(base + "convergence.hologram.refresh-ticks", 40L);
        config.addDefault(base + "convergence.hologram.base-height", 2.25D);
        config.addDefault(base + "convergence.hologram.line-spacing", 0.28D);
        config.addDefault(base + "convergence.heart-xp-reward", 60L);
        config.addDefault(base + "convergence.heart-dust-reward", 25L);
        config.addDefault(base + "convergence.reward-draws", 3);
        config.addDefault(base + "convergence.required-item-ids", List.of(
                "KUNZITE",
                "RIFT_ESSENCE",
                "VOID_CRYSTAL",
                "OBSIDIAN_CORE",
                "CHORUS_WEAVE"
        ));
        config.addDefault(base + "convergence.reward-pool", List.of(
                "ORE_FRAGMENT:48:96:32",
                "ENDSTONE_SHARD:24:48:24",
                "KUNZITE:2:6:10",
                "RIFT_ESSENCE:2:5:8",
                "VOID_CRYSTAL:2:5:8",
                "OBSIDIAN_CORE:2:5:8",
                "CHORUS_WEAVE:2:5:8",
                "VOLTA:1:2:7",
                "OIL_BARREL:1:1:3",
                "MITHRIL_ENGINE:1:1:1",
                "MEDIUM_FUEL_TANK:1:1:1"
        ));

        config.addDefault(base + "mobs.enabled", true);
        config.addDefault(base + "mobs.spawn-interval-ticks", 40);
        config.addDefault(base + "mobs.spawn-radius", 28);
        config.addDefault(base + "mobs.max-near-player", 12);
        config.addDefault(base + "mobs.global-max", 120);
        config.addDefault(base + "mobs.despawn-distance", 72);
        config.addDefault(base + "mobs.safe-radius-from-spawn", 12);
        config.addDefault(base + "mobs.activation-safe-radius-from-spawn", 40);
        config.addDefault(base + "mobs.spawn-attempts", 10);
        config.addDefault(base + "mobs.spawn-mode", "around_players");
        config.addDefault(base + "mobs.damage-multiplier", 1.25D);
        config.addDefault(base + "mobs.proximity-upgrade.enabled", true);
        config.addDefault(base + "mobs.proximity-upgrade.radius", 7.5D);
        config.addDefault(base + "mobs.proximity-upgrade.level", 15);
        config.addDefault(base + "mobs.proximity-upgrade.health-multiplier", 1.75D);
        config.addDefault(base + "mobs.proximity-upgrade.damage-multiplier", 1.35D);
        config.addDefault(base + "mobs.proximity-upgrade.speed-multiplier", 1.06D);
        config.addDefault(base + "mobs.proximity-upgrade.armor-bonus", 8.0D);
        config.addDefault(base + "mobs.proximity-upgrade.eligible-types", List.of(
                "riftwalker",
                "enderman_excavator",
                "crystal_sentry"
        ));

        config.addDefault(base + "access.operator-only", false);

        config.addDefault(base + "protection.mineable-blocks", List.of(
                "END_STONE",
                "END_STONE_BRICKS",
                "PURPUR_BLOCK",
                "PURPUR_PILLAR",
                "OBSIDIAN",
                "CRYING_OBSIDIAN",
                "AMETHYST_BLOCK",
                "BUDDING_AMETHYST",
                "CHORUS_PLANT",
                "CHORUS_FLOWER"
        ));
    }

    private void applyDragonAscensionDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "dragon-ascension.";

        config.addDefault(base + "enabled", true);
        config.addDefault(base + "world-name", "");
        config.addDefault(base + "altar.world", "");
        config.addDefault(base + "altar.x", 0.5D);
        config.addDefault(base + "altar.y", 80.0D);
        config.addDefault(base + "altar.z", 0.5D);
        config.addDefault(base + "altar.yaw", 0.0D);
        config.addDefault(base + "altar.pitch", 0.0D);

        config.addDefault(base + "summoning.required-eyes", 8);
        config.addDefault(base + "summoning.pedestal-radius", 6.0D);
        config.addDefault(base + "summoning.item-key", "SUMMONING_EYE");
        config.addDefault(base + "summoning.item-material", "ENDER_EYE");
        config.addDefault(base + "summoning.timeout-seconds", 300L);
        config.addDefault(base + "summoning.base-mutation-chance", 8.0D);
        config.addDefault(base + "summoning.tier-4-spawn-chance", 0.02D);
        config.addDefault(base + "summoning.tier-5-spawn-chance", 0.0025D);
        config.addDefault(base + "summoning.dragon-type-lock-eyes", 4);
        config.addDefault(base + "summoning.pedestal-aspects", List.of(
                "YOUNG",
                "STRONG",
                "WISE",
                "PROTECTOR",
                "UNSTABLE",
                "SUPERIOR",
                "CHAOS",
                "ANCIENT"
        ));

        config.addDefault(base + "fight.tick-period", 10);
        config.addDefault(base + "fight.arena-radius", 42.0D);
        config.addDefault(base + "fight.max-duration-seconds", 900L);
        config.addDefault(base + "fight.base-health", 500.0D);
        config.addDefault(base + "fight.health-per-tier", 200.0D);
        config.addDefault(base + "fight.base-damage-multiplier", 1.0D);
        config.addDefault(base + "fight.damage-per-tier", 0.12D);
        config.addDefault(base + "fight.phase-one.base-shield", 220.0D);
        config.addDefault(base + "fight.phase-one.shield-per-tier", 55.0D);
        config.addDefault(base + "fight.phase-one.crystals", 4);
        config.addDefault(base + "fight.phase-two.ascension-health-percent", 0.30D);

        config.addDefault(base + "scoring.weights.max-damage-share-points", 35.0D);
        config.addDefault(base + "scoring.weights.crystal-points", 10.0D);
        config.addDefault(base + "scoring.weights.mechanic-point-weight", 4.0D);
        config.addDefault(base + "scoring.weights.eye-points", 5.0D);
        config.addDefault(base + "scoring.weights.presence-point-per-second", 0.30D);
        config.addDefault(base + "scoring.weights.max-presence-points", 18.0D);
        config.addDefault(base + "scoring.thresholds.s-tier", 60.0D);
        config.addDefault(base + "scoring.thresholds.a-tier", 42.0D);
        config.addDefault(base + "scoring.thresholds.b-tier", 26.0D);
        config.addDefault(base + "scoring.thresholds.c-tier", 12.0D);

        config.addDefault(base + "rewards.minimum-active-seconds", 30);
        config.addDefault(base + "rewards.minimum-score", 12.0D);
        config.addDefault(base + "rewards.summary-auto-open", true);
        config.addDefault(base + "rewards.rare-armor-chance", 1.0D / 580000.0D);
        config.addDefault(base + "rewards.voidwing-relic-base-chance", 0.03D);

        config.addDefault(base + "progression.reputation-per-kill", 1);
        config.addDefault(base + "progression.max-reputation", 10);
    }

    private void applyCrimsonWardenDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String monster = "custom-monsters.monsters.crimson_warden.";
        config.addDefault(monster + "display-name", "&4Crimson Warden");
        config.addDefault(monster + "entity-type", "ZOMBIE");
        config.addDefault(monster + "health", 400.0D);
        config.addDefault(monster + "damage", 17.5D);
        config.addDefault(monster + "speed", 0.24D);
        config.addDefault(monster + "level", 5);
        config.addDefault(monster + "exp-reward", 0);
        config.addDefault(monster + "glowing", true);

        String base = "custom-armor.sets.crimson_warden.";
        config.addDefault(base + "display-name", "&5Crimson Warden Armor");
        config.addDefault(base + "description", "&7The first line of defense, risen from the shadows of spawn.");
        config.addDefault(base + "pieces-required", 4);
        config.addDefault(base + "crafting-material", "DIAMOND");
