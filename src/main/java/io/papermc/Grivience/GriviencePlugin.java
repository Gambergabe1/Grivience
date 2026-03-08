package io.papermc.Grivience;

import io.papermc.Grivience.bazaar.BazaarShopManager;
import io.papermc.Grivience.bazaar.BazaarGuiManager;
import io.papermc.Grivience.bazaar.NpcSellShopGui;
import io.papermc.Grivience.command.BazaarCommand;
import io.papermc.Grivience.command.BlacksmithCommand;
import io.papermc.Grivience.command.DungeonCommand;
import io.papermc.Grivience.command.GiveItemCommand;
import io.papermc.Grivience.command.DungeonHubCommand;
import io.papermc.Grivience.command.NpcSellShopCommand;
import io.papermc.Grivience.command.ReforgeCommand;
import io.papermc.Grivience.command.SkyblockMenuCommand;
import io.papermc.Grivience.command.GrivienceReloadCommand;
import io.papermc.Grivience.command.SanityCheckCommand;
import io.papermc.Grivience.command.WardrobeCommand;
import io.papermc.Grivience.command.PetCommand;
import io.papermc.Grivience.command.AdminTeleportCommand;
import io.papermc.Grivience.command.JumpPadCommand;
import io.papermc.Grivience.collections.CollectionsManager;
import io.papermc.Grivience.collections.CollectionGUI;
import io.papermc.Grivience.collections.CollectionCommand;
import io.papermc.Grivience.collections.CollectionListener;
import io.papermc.Grivience.enchantment.EnchantmentManager;
import io.papermc.Grivience.crafting.CraftingManager;




import io.papermc.Grivience.welcome.WelcomeManager;
import io.papermc.Grivience.welcome.quest.QuestLineManager;
import io.papermc.Grivience.fasttravel.FastTravelManager;
import io.papermc.Grivience.fasttravel.FastTravelGui;
import io.papermc.Grivience.fasttravel.FastTravelCommand;
import io.papermc.Grivience.fasttravel.FastTravelListener;
import io.papermc.Grivience.dungeon.DungeonManager;
import io.papermc.Grivience.dungeon.RewardChestManager;
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
import io.papermc.Grivience.listener.AttackSpeedListener;
import io.papermc.Grivience.listener.ItemStarCleanupListener;
import io.papermc.Grivience.listener.BlacksmithGuiListener;
import io.papermc.Grivience.listener.MobTargetLimiterListener;
import io.papermc.Grivience.listener.FarmingFortuneListener;
import io.papermc.Grivience.listener.ProjectileGroundCleanupListener;
import io.papermc.Grivience.listener.MobHealthListener;
import io.papermc.Grivience.listener.EnchantedFarmCraftListener;
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
import io.papermc.Grivience.mob.MonsterSpawnAdminCommand;
import io.papermc.Grivience.party.PartyManager;
import io.papermc.Grivience.mines.MiningItemListener;
import io.papermc.Grivience.mines.MiningSystemManager;
import io.papermc.Grivience.mines.MiningEventManager;
import io.papermc.Grivience.mines.MiningEventCommand;
import io.papermc.Grivience.mines.InspectorShopGui;
import io.papermc.Grivience.mines.DrillMechanicGui;
import io.papermc.Grivience.mines.DrillUpgradeCraftListener;
import io.papermc.Grivience.event.GlobalEventManager;
import io.papermc.Grivience.event.GlobalEventCommand;
import io.papermc.Grivience.mines.end.EndMinesCommand;
import io.papermc.Grivience.mines.end.EndMinesManager;
import io.papermc.Grivience.mines.end.EndMinesMiningListener;
import io.papermc.Grivience.mines.end.EndMinesProtectionListener;
import io.papermc.Grivience.mines.end.mob.EndMinesMobManager;
import io.papermc.Grivience.skyblock.command.HubCommand;
import io.papermc.Grivience.skyblock.command.IslandCommand;
import io.papermc.Grivience.skyblock.command.IslandBypassCommand;
import io.papermc.Grivience.skyblock.command.MinehubCommand;
import io.papermc.Grivience.skyblock.command.FarmHubCommand;
import io.papermc.Grivience.skyblock.command.SkyblockAdminCommand;
import io.papermc.Grivience.skyblock.hub.MinehubWorldManager;
import io.papermc.Grivience.skyblock.island.IslandManager;
import io.papermc.Grivience.skyblock.listener.NetherPortalListener;
import io.papermc.Grivience.skyblock.listener.SkyblockListener;
import io.papermc.Grivience.skyblock.listener.IslandProtectionListener;
import io.papermc.Grivience.skyblock.listener.FarmHubCropRegenerationListener;
import io.papermc.Grivience.skyblock.listener.DeathPenaltyListener;
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
import io.papermc.Grivience.quest.QuestManager;
import io.papermc.Grivience.quest.QuestCommand;
import io.papermc.Grivience.quest.QuestGui;
import io.papermc.Grivience.quest.QuestListener;
import io.papermc.Grivience.quest.QuestZnpcsHook;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class GriviencePlugin extends JavaPlugin {
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
    private CustomArmorManager customArmorManager;
    private ArmorSetBonusListener armorSetBonusListener;
    private ArmorCraftingListener armorCraftingListener;
    private CustomMonsterManager customMonsterManager;
    private CustomMonsterDeathListener customMonsterDeathListener;
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
    private NpcSellShopGui npcSellShopGui;
    private RewardChestManager rewardChestManager;
    private ResourcePackManager resourcePackManager;
    private BlacksmithGuiListener blacksmithGuiListener;
    private JumpPadManager jumpPadManager;
    private WardrobeManager wardrobeManager;
    private WardrobeGui wardrobeGui;
    private QuestManager questManager;
    private QuestGui questGui;
    private QuestZnpcsHook questZnpcsHook;
    private PetManager petManager;
    private PetGui petGui;
    private io.papermc.Grivience.gui.SkyblockLevelGui skyblockLevelGui;
    private io.papermc.Grivience.skills.SkillsGui skillsGui;
    private SkyblockScoreboardManager skyblockScoreboardManager;
    private BitsManager bitsManager;
    private BossBarAnnouncementManager bossBarAnnouncementManager;
    private io.papermc.Grivience.zone.ZoneManager zoneManager;
    private io.papermc.Grivience.zone.ZoneScoreboardListener zoneScoreboardListener;
    private io.papermc.Grivience.item.GrapplingHookManager grapplingHookManager;
    private io.papermc.Grivience.item.StaffManager staffManager;
    private io.papermc.Grivience.skyblock.profile.ProfileManager profileManager;
    private io.papermc.Grivience.skyblock.profile.gui.ProfileGui profileGui;
    private CollectionsManager collectionsManager;
    private CollectionGUI collectionGui;
    private CollectionListener collectionListener;
    private EnchantmentManager enchantmentManager;
    private CraftingManager craftingManager;

    private WelcomeManager welcomeManager;
    private QuestLineManager questLineManager;
    private FastTravelManager fastTravelManager;
    private FastTravelGui fastTravelGui;
    private io.papermc.Grivience.storage.StorageManager storageManager;
    private io.papermc.Grivience.storage.StorageGui storageGui;
    private io.papermc.Grivience.storage.StorageListener storageListener;
    private MiningSystemManager miningSystemManager;
    private MiningEventManager miningEventManager;
    private MinionManager minionManager;
    private MinionGuiManager minionGuiManager;
    private InspectorShopGui inspectorShopGui;
    private DrillMechanicGui drillMechanicGui;
    private GlobalEventManager globalEventManager;
    private io.papermc.Grivience.nick.NickManager nickManager;
    private io.papermc.Grivience.nick.NickGuiManager nickGuiManager;
    private org.bukkit.NamespacedKey grapplingHookKey;

    private io.papermc.Grivience.gui.GuardianRecipeGui guardianRecipeGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        applyConfigUpgrades();
        getServer().getPluginManager().registerEvents(new DeathPenaltyListener(this), this);

        grapplingHookKey = new org.bukkit.NamespacedKey(this, "grappling_hook_id");

        partyManager = new PartyManager(this);
        customItemService = new CustomItemService(this);
        customItemService.reloadFromConfig();
        customItemService.registerRecipes();
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
                customArmorManager
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

            MonsterSpawnAdminCommand monsterSpawnAdminCommand = new MonsterSpawnAdminCommand(this, customMonsterManager);
            registerCommand("mobspawn", monsterSpawnAdminCommand);
            getServer().getPluginManager().registerEvents(monsterSpawnAdminCommand.getMonsterGui(), this);
            getLogger().info("Custom monster system enabled with " + customMonsterManager.getMonsters().size() + " monsters.");
        } else {
            registerCommand("mobspawn", (sender, command, label, args) -> {
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
        getServer().getPluginManager().registerEvents(new DungeonListener(this, dungeonManager, customItemService), this);
        getServer().getPluginManager().registerEvents(new FriendlyFireListener(dungeonManager, partyManager), this);
        getServer().getPluginManager().registerEvents(new MobHealthListener(this), this);
        getServer().getPluginManager().registerEvents(new MobTargetLimiterListener(), this);
        getServer().getPluginManager().registerEvents(new ProjectileGroundCleanupListener(), this);
        getServer().getPluginManager().registerEvents(new EnchantedFarmCraftListener(customItemService), this);
        getServer().getPluginManager().registerEvents(new DrillUpgradeCraftListener(this, customItemService), this);
        getServer().getPluginManager().registerEvents(new FarmingFortuneListener(this), this);
        getServer().getPluginManager().registerEvents(new AttackSpeedListener(this), this);
        getServer().getPluginManager().registerEvents(new SkyblockCombatRefreshListener(this, skyblockCombatEngine, skyblockManaManager), this);
        enchantTableListener = new EnchantTableListener(this);
        enchantTableListener.reloadFromConfig();
        customItemEnchantListener = new CustomItemEnchantListener(customItemService);
        getServer().getPluginManager().registerEvents(enchantTableListener, this);
        getServer().getPluginManager().registerEvents(customItemEnchantListener, this);
        reforgeAnvilGuiListener = new ReforgeAnvilGuiListener(this, customItemService);
        blacksmithGuiListener = new BlacksmithGuiListener(this, customItemService);
        getServer().getPluginManager().registerEvents(reforgeAnvilGuiListener, this);
        getServer().getPluginManager().registerEvents(blacksmithGuiListener, this);
        customArmorEffectListener = new CustomArmorEffectListener(this, customItemService);
        bazaarShopManager = new BazaarShopManager(this, customItemService, customArmorManager);
        bazaarGuiManager = new BazaarGuiManager(this, bazaarShopManager);
        npcSellShopGui = new NpcSellShopGui(bazaarShopManager);
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
        petManager = new PetManager(this);
        petManager.setLevelManager(skyblockLevelManager);
        petManager.registerRecipes();
        petGui = new PetGui(petManager);
        getServer().getPluginManager().registerEvents(petGui, this);
        getServer().getPluginManager().registerEvents(new PetConsumeListener(petManager), this);
        getServer().getPluginManager().registerEvents(new PetExpListener(this, petManager), this);
        getServer().getPluginManager().registerEvents(new PetSkillXpListener(this, petManager, skyblockLevelManager), this);
        bitsManager = new BitsManager(getDataFolder());
        bossBarAnnouncementManager = new BossBarAnnouncementManager(this);
        bossBarAnnouncementManager.start();
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

        minionManager = new MinionManager(this, islandManager);
        minionGuiManager = new MinionGuiManager(this, minionManager);
        getServer().getPluginManager().registerEvents(minionGuiManager, this);
        getServer().getPluginManager().registerEvents(new MinionListener(minionManager, minionGuiManager), this);
        getServer().getPluginManager().registerEvents(new MinionCraftingListener(minionManager), this);
        registerCommand("minion", new MinionCommand(this, minionManager, minionGuiManager));
        skyblockMenuManager.setMinionGuiManager(minionGuiManager);
        getLogger().info("Minion system enabled with " + minionManager.getTotalMinions() + " placed minions.");

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
        getServer().getPluginManager().registerEvents(new NetherPortalListener(this, hubCommand), this);
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
        getServer().getPluginManager().registerEvents(new RecipeUnlockListener(this, collectionsManager, skyblockLevelManager), this);
        // Ensure all custom recipes are crafted in the vanilla crafting table UI (not the 2x2 player grid).
        getServer().getPluginManager().registerEvents(new CustomRecipeWorkbenchOnlyListener(this), this);
        // Enforce collection-tier requirements when crafting via the vanilla crafting UI.
        getServer().getPluginManager().registerEvents(new RecipeCollectionGateListener(collectionsManager), this);

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
        getLogger().info("Storage System enabled with " + io.papermc.Grivience.storage.StorageType.values().length + " storage types.");

        skyblockScoreboardManager = new SkyblockScoreboardManager(
                this,
                skyblockLevelManager,
                islandManager,
                dungeonManager,
                bitsManager,
                zoneManager
        );
        getServer().getPluginManager().registerEvents(skyblockScoreboardManager, this); // NEW LINE
        skyblockScoreboardManager.start();
        ReforgeCommand reforgeCommand = new ReforgeCommand(reforgeAnvilGuiListener);
        BazaarCommand bazaarCommand = new BazaarCommand(bazaarShopManager, bazaarGuiManager);
        SanityCheckCommand sanityCheckCommand = new SanityCheckCommand(this, armorCraftingListener, islandManager, customItemService);

        DungeonCommand dungeonCommand = new DungeonCommand(this, partyManager, dungeonManager, guiManager, customItemService, customArmorManager, petManager);
        registerCommand("dungeon", dungeonCommand);
        registerCommand("party", dungeonCommand);

        GiveItemCommand giveItemCommand = new GiveItemCommand(customItemService);
        registerCommand("gi", giveItemCommand);
        registerCommand("giveitem", giveItemCommand);

        registerCommand("reforge", reforgeCommand);
        registerCommand("bazaar", bazaarCommand);
        registerCommand("npcshop", new NpcSellShopCommand(npcSellShopGui));
        registerCommand("grisanity", sanityCheckCommand);
        registerCommand("grivience", new GrivienceReloadCommand(this));
        registerCommand("wardrobe", new WardrobeCommand(wardrobeGui));
        registerCommand("pets", new PetCommand(petGui));
        registerCommand("quest", new QuestCommand(questManager, questGui));
        registerCommand("admintp", new AdminTeleportCommand());
        registerCommand("jumppad", new JumpPadCommand(jumpPadManager));
        registerCommand("blacksmith", new BlacksmithCommand(blacksmithGuiListener));

        // Re-register Crafting Commands
        CraftingManager.CraftingCommand craftingCmd = new CraftingManager.CraftingCommand(craftingManager);
        registerCommand("craft", craftingCmd);
        registerCommand("crafting", craftingCmd);
        registerCommand("recipe", craftingCmd);
    } // Missing closing brace for onEnable()



    @Override
    public void onDisable() {
        if (customArmorEffectListener != null) {
            customArmorEffectListener.shutdown();
        }
        if (skyblockCombatEngine != null) {
            skyblockCombatEngine.shutdown();
        }
        if (skyblockManaManager != null) {
            skyblockManaManager.shutdown();
        }
        if (skyblockScoreboardManager != null) {
            skyblockScoreboardManager.shutdown();
        }
        if (skyblockLevelManager != null) {
            skyblockLevelManager.save();
        }
        if (dungeonManager != null) {
            dungeonManager.shutdown();
        }
        if (bazaarShopManager != null) {
            bazaarShopManager.shutdown();
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
        if (wardrobeManager != null) {
            wardrobeManager.saveAll();
        }
        if (storageManager != null) {
            storageManager.disable();
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
        if (staffManager != null) {
            staffManager = null;
        }
        if (profileManager != null) {
            profileManager.shutdown();
        }
        if (collectionListener != null) {
            collectionListener.shutdown();
        }
        if (collectionsManager != null) {
            collectionsManager.disable();
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
        if (endMinesMobManager != null) {
            endMinesMobManager.shutdown();
            endMinesMobManager.enable();
        }
        if (customItemService != null) {
            customItemService.reloadFromConfig();
            customItemService.registerRecipes();
        }
        if (skyblockLevelManager != null) {
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

    public CollectionGUI getCollectionGui() {
        return collectionGui;
    }

    public io.papermc.Grivience.storage.StorageManager getStorageManager() {
        return storageManager;
    }

    public io.papermc.Grivience.storage.StorageGui getStorageGui() {
        return storageGui;
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

    public io.papermc.Grivience.skyblock.island.IslandManager getIslandManager() {
        return islandManager;
    }

    public TradeManager getTradeManager() {
        return tradeManager;
    }

    public io.papermc.Grivience.skyblock.profile.ProfileManager getProfileManager() {
        return profileManager;
    }

    public io.papermc.Grivience.pet.PetManager getPetManager() {
        return petManager;
    }

    public io.papermc.Grivience.skyblock.profile.gui.ProfileGui getProfileGui() {
        return profileGui;
    }

    public MinionManager getMinionManager() {
        return minionManager;
    }

    public MinionGuiManager getMinionGuiManager() {
        return minionGuiManager;
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
                "grivience.visit.guestlimit.mvp=5",
                "grivience.visit.guestlimit.mvpplus=7",
                "grivience.visit.guestlimit.youtuber=15"
        ));

        // Keep inventory on, but apply Skyblock-style coin loss on death (purse only).
        config.addDefault("skyblock.death.purse-loss.enabled", true);
        config.addDefault("skyblock.death.purse-loss.percent", 0.5D);
        // If true, right-clicking a crafting table opens the custom /craft GUI instead of the vanilla UI.
        config.addDefault("crafting.override-crafting-table", false);
        config.addDefault("collections.auto-save-interval-seconds", 300);
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
        applyEndMinesDefaults(config);
        applyVoidMinerArmorDefaults(config);
        applyHarvesterEmbraceArmorDefaults(config);
        applyAbyssalDiverArmorDefaults(config);
        applyCrypticConquerorArmorDefaults(config);
        applyArcaneWeaverArmorDefaults(config);
        applySkyblockLevelingDefaults(config);
        applySkyblockPetDefaults(config);
        applyMinionDefaults(config);
        applyJumpPadDefaults(config);
        applyScoreboardDefaults(config);
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

    private void applyDungeonHubDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        config.addDefault("dungeons.hub.world", "world");
        config.addDefault("dungeons.hub.x", 0.0D);
        config.addDefault("dungeons.hub.y", 120.0D);
        config.addDefault("dungeons.hub.z", 0.0D);
        config.addDefault("dungeons.hub.yaw", 0.0D);
        config.addDefault("dungeons.hub.pitch", 0.0D);
    }

    private void applyMinehubDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "skyblock.minehub-";
        config.addDefault(base + "world", "minehub_world");

        config.addDefault(base + "spawn.x", 0.5D);
        config.addDefault(base + "spawn.y", 100.0D);
        config.addDefault(base + "spawn.z", 0.5D);
        config.addDefault(base + "spawn.yaw", 0.0D);
        config.addDefault(base + "spawn.pitch", 0.0D);
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

    private void applyJumpPadDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "jump-pads.";
        config.addDefault(base + "horizontal-velocity", 2.20D);
        config.addDefault(base + "vertical-boost", 1.04D);
        config.addDefault(base + "arrival-teleport-delay-ticks", 15);
        config.addDefault(base + "cooldown-ms", 450L);
        config.addDefault(base + "fall-cancel-window-ms", 3000L);
        config.addDefault(base + "hypixel-effects", true);
        config.addDefault(base + "min-vertical-speed", 0.58D);
        config.addDefault(base + "max-vertical-speed", 1.42D);
        config.addDefault(base + "max-horizontal-speed", 3.85D);
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

        config.addDefault(base + "mobs.enabled", true);
        config.addDefault(base + "mobs.spawn-interval-ticks", 40);
        config.addDefault(base + "mobs.spawn-radius", 28);
        config.addDefault(base + "mobs.max-near-player", 12);
        config.addDefault(base + "mobs.despawn-distance", 72);
        config.addDefault(base + "mobs.safe-radius-from-spawn", 12);
        config.addDefault(base + "mobs.spawn-attempts", 10);
        config.addDefault(base + "mobs.spawn-mode", "around_players");
        config.addDefault(base + "mobs.damage-multiplier", 1.25D);

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

    private void applyVoidMinerArmorDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "custom-armor.sets.void_miner.";

        config.addDefault(base + "display-name", "&5Void Miner");
        config.addDefault(base + "description", "&7Mining gear infused with rift energy.");
        config.addDefault(base + "pieces-required", 2);
        config.addDefault(base + "crafting-material", "custom:obsidian_core");
        config.addDefault(base + "bonuses", List.of(
                "&72 Pieces: &fHaste I &8(End Mines)",
                "&74 Pieces: &fRare Drops +35% &8(End Mines)",
                "&74 Pieces: &fNight Vision &8(End Mines)"
        ));

        addVoidMinerPieceDefaults(config, base + "pieces.helmet.",
                "DIAMOND_HELMET",
                "&5Void Miner Helmet",
                2,
                10.0D,
                List.of(
                        "&7Part of the &5Void Miner &7set.",
                        "&8Forged in the End Mines.",
                        "",
                        "&7Defense: &a+2",
                        "&7Health: &a+10",
                        "",
                        "&6Set Bonus: &5Void Fortune",
                        "&7Wear 2 pieces: &fHaste I &8(End Mines)",
                        "&7Wear 4 pieces: &fRare Drops +35% &8(End Mines)",
                        "&7Wear 4 pieces: &fNight Vision &8(End Mines)",
                        "",
                        "&5EPIC &8HELMET"
                ));

        addVoidMinerPieceDefaults(config, base + "pieces.chestplate.",
                "DIAMOND_CHESTPLATE",
                "&5Void Miner Chestplate",
                6,
                20.0D,
                List.of(
                        "&7Part of the &5Void Miner &7set.",
                        "&8Hums with void resonance.",
                        "",
                        "&7Defense: &a+6",
                        "&7Health: &a+20",
                        "",
                        "&6Set Bonus: &5Void Fortune",
                        "&7Wear 2 pieces: &fHaste I &8(End Mines)",
                        "&7Wear 4 pieces: &fRare Drops +35% &8(End Mines)",
                        "&7Wear 4 pieces: &fNight Vision &8(End Mines)",
                        "",
                        "&5EPIC &8CHESTPLATE"
                ));

        addVoidMinerPieceDefaults(config, base + "pieces.leggings.",
                "DIAMOND_LEGGINGS",
                "&5Void Miner Leggings",
                5,
                15.0D,
                List.of(
                        "&7Part of the &5Void Miner &7set.",
                        "&8Stitched with chorus weave.",
                        "",
                        "&7Defense: &a+5",
                        "&7Health: &a+15",
                        "",
                        "&6Set Bonus: &5Void Fortune",
                        "&7Wear 2 pieces: &fHaste I &8(End Mines)",
                        "&7Wear 4 pieces: &fRare Drops +35% &8(End Mines)",
                        "&7Wear 4 pieces: &fNight Vision &8(End Mines)",
                        "",
                        "&5EPIC &8LEGGINGS"
                ));

        addVoidMinerPieceDefaults(config, base + "pieces.boots.",
                "DIAMOND_BOOTS",
                "&5Void Miner Boots",
                2,
                10.0D,
                List.of(
                        "&7Part of the &5Void Miner &7set.",
                        "&8Lightweight and unstable.",
                        "",
                        "&7Defense: &a+2",
                        "&7Health: &a+10",
                        "",
                        "&6Set Bonus: &5Void Fortune",
                        "&7Wear 2 pieces: &fHaste I &8(End Mines)",
                        "&7Wear 4 pieces: &fRare Drops +35% &8(End Mines)",
                        "&7Wear 4 pieces: &fNight Vision &8(End Mines)",
                        "",
                        "&5EPIC &8BOOTS"
                ));
    }

    private void applyHarvesterEmbraceArmorDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "custom-armor.sets.harvester_embrace.";

        config.addDefault(base + "display-name", "&aHarvester's Embrace");
        config.addDefault(base + "description", "&7Nature's gentle touch, enhancing growth.");
        config.addDefault(base + "pieces-required", 2);
        config.addDefault(base + "crafting-material", "custom:verdant_fiber");
        config.addDefault(base + "bonuses", List.of(
                "&72 Pieces: &aIncreased Crop Yield &8(+15%)",
                "&74 Pieces: &aInstant Replant &8(Automatically replants crops after harvest)"
        ));

        addCustomArmorPieceDefaults(config, base + "pieces.helmet.",
                "LEATHER_HELMET",
                "&aHarvester's Visage",
                1, // Armor
                5.0D, // Health
                false, // Glowing
                List.of(
                        "&7Part of the &aHarvester's Embrace &7set.",
                        "&8Whispers of the fertile earth.",
                        "",
                        "&7Defense: &a+1",
                        "&7Health: &a+5",
                        "",
                        "&6Set Bonus: &aBountiful Harvest",
                        "&7Wear 2 pieces: &aIncreased Crop Yield &8(+15%)",
                        "&7Wear 4 pieces: &aInstant Replant &8(Automatically replants crops after harvest)",
                        "",
                        "&aUNCOMMON &8HELMET"
                ));

        addCustomArmorPieceDefaults(config, base + "pieces.chestplate.",
                "LEATHER_CHESTPLATE",
                "&aHarvester's Tunic",
                3, // Armor
                10.0D, // Health
                false, // Glowing
                List.of(
                        "&7Part of the &aHarvester's Embrace &7set.",
                        "&8Feel the pulse of the fields.",
                        "",
                        "&7Defense: &a+3",
                        "&7Health: &a+10",
                        "",
                        "&6Set Bonus: &aBountiful Harvest",
                        "&7Wear 2 pieces: &aIncreased Crop Yield &8(+15%)",
                        "&7Wear 4 pieces: &aInstant Replant &8(Automatically replants crops after harvest)",
                        "",
                        "&aUNCOMMON &8CHESTPLATE"
                ));

        addCustomArmorPieceDefaults(config, base + "pieces.leggings.",
                "LEATHER_LEGGINGS",
                "&aHarvester's Breeches",
                2, // Armor
                8.0D, // Health
                false, // Glowing
                List.of(
                        "&7Part of the &aHarvester's Embrace &7set.",
                        "&8Comfortable for long days.",
                        "",
                        "&7Defense: &a+2",
                        "&7Health: &a+8",
                        "",
                        "&6Set Bonus: &aBountiful Harvest",
                        "&7Wear 2 pieces: &aIncreased Crop Yield &8(+15%)",
                        "&7Wear 4 pieces: &aInstant Replant &8(Automatically replants crops after harvest)",
                        "",
                        "&aUNCOMMON &8LEGGINGS"
                ));

        addCustomArmorPieceDefaults(config, base + "pieces.boots.",
                "LEATHER_BOOTS",
                "&aHarvester's Striders",
                1, // Armor
                5.0D, // Health
                false, // Glowing
                List.of(
                        "&7Part of the &aHarvester's Embrace &7set.",
                        "&8Light upon the soil.",
                        "",
                        "&7Defense: &a+1",
                        "&7Health: &a+5",
                        "",
                        "&6Set Bonus: &aBountiful Harvest",
                        "&7Wear 2 pieces: &aIncreased Crop Yield &8(+15%)",
                        "&7Wear 4 pieces: &aInstant Replant &8(Automatically replants crops after harvest)",
                        "",
                        "&aUNCOMMON &8BOOTS"
                ));
    }

    private void applyAbyssalDiverArmorDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "custom-armor.sets.abyssal_diver.";

        config.addDefault(base + "display-name", "&bAbyssal Diver");
        config.addDefault(base + "description", "&7Gear for those who brave the crushing depths.");
        config.addDefault(base + "pieces-required", 2);
        config.addDefault(base + "crafting-material", "custom:abyssal_scale");
        config.addDefault(base + "bonuses", List.of(
                "&72 Pieces: &bIncreased Fishing Speed &8(+20%)",
                "&74 Pieces: &bTreasure Hunter &8(Higher chance for rare fishing loot)"
        ));

        addCustomArmorPieceDefaults(config, base + "pieces.helmet.",
                "IRON_HELMET",
                "&bAbyssal Helmet",
                2, // Armor
                8.0D, // Health
                false, // Glowing
                List.of(
                        "&7Part of the &bAbyssal Diver &7set.",
                        "&8Enhanced senses in the dark.",
                        "",
                        "&7Defense: &a+2",
                        "&7Health: &a+8",
                        "",
                        "&6Set Bonus: &bDeep Sea Mastery",
                        "&7Wear 2 pieces: &bIncreased Fishing Speed &8(+20%)",
                        "&7Wear 4 pieces: &bTreasure Hunter &8(Higher chance for rare fishing loot)",
                        "",
                        "&bRARE &8HELMET"
                ));

        addCustomArmorPieceDefaults(config, base + "pieces.chestplate.",
                "IRON_CHESTPLATE",
                "&bAbyssal Chestplate",
                4, // Armor
                15.0D, // Health
                false, // Glowing
                List.of(
                        "&7Part of the &bAbyssal Diver &7set.",
                        "&8Resistant to oceanic pressures.",
                        "",
                        "&7Defense: &a+4",
                        "&7Health: &a+15",
                        "",
                        "&6Set Bonus: &bDeep Sea Mastery",
                        "&7Wear 2 pieces: &bIncreased Fishing Speed &8(+20%)",
                        "&7Wear 4 pieces: &bTreasure Hunter &8(Higher chance for rare fishing loot)",
                        "",
                        "&bRARE &8CHESTPLATE"
                ));

        addCustomArmorPieceDefaults(config, base + "pieces.leggings.",
                "IRON_LEGGINGS",
                "&bAbyssal Leggings",
                3, // Armor
                12.0D, // Health
                false, // Glowing
                List.of(
                        "&7Part of the &bAbyssal Diver &7set.",
                        "&8Swift through the water.",
                        "",
                        "&7Defense: &a+3",
                        "&7Health: &a+12",
                        "",
                        "&6Set Bonus: &bDeep Sea Mastery",
                        "&7Wear 2 pieces: &bIncreased Fishing Speed &8(+20%)",
                        "&7Wear 4 pieces: &bTreasure Hunter &8(Higher chance for rare fishing loot)",
                        "",
                        "&bRARE &8LEGGINGS"
                ));

        addCustomArmorPieceDefaults(config, base + "pieces.boots.",
                "IRON_BOOTS",
                "&bAbyssal Boots",
                2, // Armor
                8.0D, // Health
                false, // Glowing
                List.of(
                        "&7Part of the &bAbyssal Diver &7set.",
                        "&8Anchored to the deep.",
                        "",
                        "&7Defense: &a+2",
                        "&7Health: &a+8",
                        "",
                        "&6Set Bonus: &bDeep Sea Mastery",
                        "&7Wear 2 pieces: &bIncreased Fishing Speed &8(+20%)",
                        "&7Wear 4 pieces: &bTreasure Hunter &8(Higher chance for rare fishing loot)",
                        "",
                        "&bRARE &8BOOTS"
                ));
    }

    private void applyCrypticConquerorArmorDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "custom-armor.sets.cryptic_conqueror.";

        config.addDefault(base + "display-name", "&cCryptic Conqueror");
        config.addDefault(base + "description", "&7Gear of a true dungeon hero, bathed in glory.");
        config.addDefault(base + "pieces-required", 2);
        config.addDefault(base + "crafting-material", "custom:dungeon_relic_shard");
        config.addDefault(base + "bonuses", List.of(
                "&72 Pieces: &cIncreased Damage in Dungeons &8(+10%)",
                "&74 Pieces: &cDungeon Resilience &8(Take 15% less damage from dungeon mobs)"
        ));

        addCustomArmorPieceDefaults(config, base + "pieces.helmet.",
                "GOLDEN_HELMET",
                "&cCryptic Helmet",
                3, // Armor
                10.0D, // Health
                true, // Glowing
                List.of(
                        "&7Part of the &cCryptic Conqueror &7set.",
                        "&8Echoes of forgotten heroes.",
                        "",
                        "&7Defense: &a+3",
                        "&7Health: &a+10",
                        "",
                        "&6Set Bonus: &cDungeon's Might",
                        "&7Wear 2 pieces: &cIncreased Damage in Dungeons &8(+10%)",
                        "&7Wear 4 pieces: &cDungeon Resilience &8(Take 15% less damage from dungeon mobs)",
                        "",
                        "&cEPIC &8HELMET"
                ));

        addCustomArmorPieceDefaults(config, base + "pieces.chestplate.",
                "GOLDEN_CHESTPLATE",
                "&cCryptic Chestplate",
                6, // Armor
                25.0D, // Health
                true, // Glowing
                List.of(
                        "&7Part of the &cCryptic Conqueror &7set.",
                        "&8Forged in dungeon depths.",
                        "",
                        "&7Defense: &a+6",
                        "&7Health: &a+25",
                        "",
                        "&6Set Bonus: &cDungeon's Might",
                        "&7Wear 2 pieces: &cIncreased Damage in Dungeons &8(+10%)",
                        "&7Wear 4 pieces: &cDungeon Resilience &8(Take 15% less damage from dungeon mobs)",
                        "",
                        "&cEPIC &8CHESTPLATE"
                ));

        addCustomArmorPieceDefaults(config, base + "pieces.leggings.",
                "GOLDEN_LEGGINGS",
                "&cCryptic Leggings",
                5, // Armor
                20.0D, // Health
                true, // Glowing
                List.of(
                        "&7Part of the &cCryptic Conqueror &7set.",
                        "&8Stained with monster blood.",
                        "",
                        "&7Defense: &a+5",
                        "&7Health: &a+20",
                        "",
                        "&6Set Bonus: &cDungeon's Might",
                        "&7Wear 2 pieces: &cIncreased Damage in Dungeons &8(+10%)",
                        "&7Wear 4 pieces: &cDungeon Resilience &8(Take 15% less damage from dungeon mobs)",
                        "",
                        "&cEPIC &8LEGGINGS"
                ));

        addCustomArmorPieceDefaults(config, base + "pieces.boots.",
                "GOLDEN_BOOTS",
                "&cCryptic Boots",
                3, // Armor
                10.0D, // Health
                true, // Glowing
                List.of(
                        "&7Part of the &cCryptic Conqueror &7set.",
                        "&8Treads of a champion.",
                        "",
                        "&7Defense: &a+3",
                        "&7Health: &a+10",
                        "",
                        "&6Set Bonus: &cDungeon's Might",
                        "&7Wear 2 pieces: &cIncreased Damage in Dungeons &8(+10%)",
                        "&7Wear 4 pieces: &cDungeon Resilience &8(Take 15% less damage from dungeon mobs)",
                        "",
                        "&cEPIC &8BOOTS"
                ));
    }

    private void applyArcaneWeaverArmorDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "custom-armor.sets.arcane_weaver.";

        config.addDefault(base + "display-name", "&5Arcane Weaver");
        config.addDefault(base + "description", "&7Woven with raw magic, it amplifies the caster's will.");
        config.addDefault(base + "pieces-required", 2);
        config.addDefault(base + "crafting-material", "custom:mana_infused_fabric");
        config.addDefault(base + "bonuses", List.of(
                "&72 Pieces: &5Mana Regeneration &8(+10% per second)",
                "&74 Pieces: &5Spell Efficiency &8(Spells cost 15% less mana)"
        ));

        addCustomArmorPieceDefaults(config, base + "pieces.helmet.",
                "LEATHER_HELMET", // Can be dyed purple
                "&5Arcane Hood",
                0, // Armor
                5.0D, // Health
                true, // Glowing
                List.of(
                        "&7Part of the &5Arcane Weaver &7set.",
                        "&8Focuses the mind.",
                        "",
                        "&7Defense: &a+0",
                        "&7Health: &a+5",
                        "",
                        "&6Set Bonus: &5Mystic Flow",
                        "&7Wear 2 pieces: &5Mana Regeneration &8(+10% per second)",
                        "&7Wear 4 pieces: &5Spell Efficiency &8(Spells cost 15% less mana)",
                        "",
                        "&5RARE &8HELMET"
                ));

        addCustomArmorPieceDefaults(config, base + "pieces.chestplate.",
                "LEATHER_CHESTPLATE", // Can be dyed purple
                "&5Arcane Robes",
                1, // Armor
                10.0D, // Health
                true, // Glowing
                List.of(
                        "&7Part of the &5Arcane Weaver &7set.",
                        "&8Flowing with ancient power.",
                        "",
                        "&7Defense: &a+1",
                        "&7Health: &a+10",
                        "",
                        "&6Set Bonus: &5Mystic Flow",
                        "&7Wear 2 pieces: &5Mana Regeneration &8(+10% per second)",
                        "&7Wear 4 pieces: &5Spell Efficiency &8(Spells cost 15% less mana)",
                        "",
                        "&5RARE &8CHESTPLATE"
                ));

        addCustomArmorPieceDefaults(config, base + "pieces.leggings.",
                "LEATHER_LEGGINGS", // Can be dyed purple
                "&5Arcane Trousers",
                0, // Armor
                8.0D, // Health
                true, // Glowing
                List.of(
                        "&7Part of the &5Arcane Weaver &7set.",
                        "&8Light for swift movement.",
                        "",
                        "&7Defense: &a+0",
                        "&7Health: &a+8",
                        "",
                        "&6Set Bonus: &5Mystic Flow",
                        "&7Wear 2 pieces: &5Mana Regeneration &8(+10% per second)",
                        "&7Wear 4 pieces: &5Spell Efficiency &8(Spells cost 15% less mana)",
                        "",
                        "&5RARE &8LEGGINGS"
                ));

        addCustomArmorPieceDefaults(config, base + "pieces.boots.",
                "LEATHER_BOOTS", // Can be dyed purple
                "&5Arcane Boots",
                0, // Armor
                5.0D, // Health
                true, // Glowing
                List.of(
                        "&7Part of the &5Arcane Weaver &7set.",
                        "&8Silent steps for spellcasting.",
                        "",
                        "&7Defense: &a+0",
                        "&7Health: &a+5",
                        "",
                        "&6Set Bonus: &5Mystic Flow",
                        "&7Wear 2 pieces: &5Mana Regeneration &8(+10% per second)",
                        "&7Wear 4 pieces: &5Spell Efficiency &8(Spells cost 15% less mana)",
                        "",
                        "&5RARE &8BOOTS"
                ));
    }

    private void addCustomArmorPieceDefaults(
            org.bukkit.configuration.file.FileConfiguration config,
            String base,
            String material,
            String displayName,
            int armor,
            double health,
            boolean glowing,
            List<String> lore
    ) {
        config.addDefault(base + "material", material);
        config.addDefault(base + "display-name", displayName);
        config.addDefault(base + "armor", armor);
        config.addDefault(base + "health", health);
        config.addDefault(base + "glowing", glowing);
        config.addDefault(base + "lore", lore);
    }

    private void addVoidMinerPieceDefaults(
            org.bukkit.configuration.file.FileConfiguration config,
            String base,
            String material,
            String displayName,
            int armor,
            double health,
            List<String> lore
    ) {
        config.addDefault(base + "material", material);
        config.addDefault(base + "display-name", displayName);
        config.addDefault(base + "armor", armor);
        config.addDefault(base + "health", health);
        config.addDefault(base + "glowing", true);
        config.addDefault(base + "lore", lore);
    }

    private void applySkyblockLevelingDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "skyblock-leveling.";
        config.addDefault(base + "xp-per-level", 100L);
        config.addDefault(base + "max-level", 521);
        config.addDefault(base + "notify-xp-gain", true);

        // Skill leveling settings (Skyblock-style)
        config.addDefault(base + "skill-leveling.enabled", true);
        config.addDefault(base + "skill-leveling.max-level", 60);

        // Skyblock XP rewards for leveling up skills (Skyblock-style defaults)
        config.addDefault(base + "skill-level-xp-rewards.level-1-to-10", 5L);
        config.addDefault(base + "skill-level-xp-rewards.level-11-to-25", 10L);
        config.addDefault(base + "skill-level-xp-rewards.level-26-to-50", 20L);
        config.addDefault(base + "skill-level-xp-rewards.level-51-to-60", 30L);

        config.addDefault(base + "action-xp.combat-kill", 0L);
        config.addDefault(base + "action-xp.mining-ore", 0L);
        config.addDefault(base + "action-xp.foraging-log", 0L);
        config.addDefault(base + "action-xp.farming-harvest", 0L);
        config.addDefault(base + "action-xp.fishing-catch", 0L);
        config.addDefault(base + "action-xp.quest-complete", 0L);
        config.addDefault(base + "action-xp.dungeon-complete", 0L);
        config.addDefault(base + "action-xp.island-create", 0L);
        config.addDefault(base + "action-xp.island-upgrade", 0L);

        config.addDefault(base + "skill-actions-per-level.combat", 40L);
        config.addDefault(base + "skill-actions-per-level.mining", 140L);
        config.addDefault(base + "skill-actions-per-level.foraging", 140L);
        config.addDefault(base + "skill-actions-per-level.farming", 120L);
        config.addDefault(base + "skill-actions-per-level.fishing", 20L);
        config.addDefault(base + "skill-level-cap", 60);

        config.addDefault(base + "dungeons.catacombs-runs-per-level", 3L);
        config.addDefault(base + "dungeons.class-runs-per-level", 4L);
        config.addDefault(base + "dungeons.catacombs-level-cap", 50);
        config.addDefault(base + "dungeons.class-level-cap", 50);
        config.addDefault(base + "dungeons.class-level-xp-reward", 4L);
        config.addDefault(base + "dungeons.catacombs-level-xp-rewards.level-1-to-39", 20L);
        config.addDefault(base + "dungeons.catacombs-level-xp-rewards.level-40-to-50", 40L);
        config.addDefault(base + "dungeons.floor-completion-xp.entrance", 10L);
        config.addDefault(base + "dungeons.floor-completion-xp.floor-1", 10L);
        config.addDefault(base + "dungeons.floor-completion-xp.floor-2", 20L);
        config.addDefault(base + "dungeons.floor-completion-xp.floor-3", 20L);
        config.addDefault(base + "dungeons.floor-completion-xp.floor-4", 20L);
        config.addDefault(base + "dungeons.floor-completion-xp.floor-5", 30L);
        config.addDefault(base + "dungeons.floor-completion-xp.floor-6", 30L);
        config.addDefault(base + "dungeons.floor-completion-xp.floor-7", 30L);
        config.addDefault(base + "dungeons.floor-completion-xp.master-1", 50L);
        config.addDefault(base + "dungeons.floor-completion-xp.master-2", 50L);
        config.addDefault(base + "dungeons.floor-completion-xp.master-3", 50L);
        config.addDefault(base + "dungeons.floor-completion-xp.master-4", 50L);
        config.addDefault(base + "dungeons.floor-completion-xp.master-5", 50L);
        config.addDefault(base + "dungeons.floor-completion-xp.master-6", 50L);
        config.addDefault(base + "dungeons.floor-completion-xp.master-7", 50L);
        config.addDefault(base + "dungeons.objective-xp.first-s-rank", 20L);
        config.addDefault(base + "dungeons.objective-xp.first-a-rank", 10L);
        config.addDefault(base + "dungeons.objective-xp.score-200", 5L);
        config.addDefault(base + "dungeons.objective-xp.score-250", 10L);
        config.addDefault(base + "dungeons.objective-xp.score-300", 15L);

        config.addDefault(base + "bestiary.tier-size", 10);
        config.addDefault(base + "bestiary.family-tier-xp", 1L);
        config.addDefault(base + "bestiary.milestone-every-tiers", 10);
        config.addDefault(base + "bestiary.milestone-every-kills", 10);
        config.addDefault(base + "bestiary.milestone-xp", 10L);

        config.addDefault(base + "level-rewards.health-per-level", 5);
        config.addDefault(base + "level-rewards.strength-per-5-levels", 1);
        addLevelUnlockDefault(config, 3, "Community Shop");
        addLevelUnlockDefault(config, 5, "Auction House");
        addLevelUnlockDefault(config, 7, "Bazaar");
        addLevelUnlockDefault(config, 9, "Museum");
        addLevelUnlockDefault(config, 10, "Depth Strider Enchantment");
        addLevelUnlockDefault(config, 15, "Garden");
        addLevelUnlockDefault(config, 20, "Hex");
        addLevelUnlockDefault(config, 25, "Skill Average 15");
        addLevelUnlockDefault(config, 50, "Skill Average 20");
        addLevelUnlockDefault(config, 70, "Garden Level 5");
        addLevelUnlockDefault(config, 100, "Skill Average 25");
        addLevelUnlockDefault(config, 120, "Garden Level 10");
        addLevelUnlockDefault(config, 150, "Skill Average 30");
        addLevelUnlockDefault(config, 200, "Skill Average 35");
        addLevelUnlockDefault(config, 240, "Garden Level 15");
        addLevelUnlockDefault(config, 250, "Skill Average 40");
        addLevelUnlockDefault(config, 280, "Garden Level 20");
        addLevelUnlockDefault(config, 300, "Skill Average 45");
        addLevelUnlockDefault(config, 350, "Skill Average 50");
        addLevelUnlockDefault(config, 400, "Skill Average 55");
        addLevelUnlockDefault(config, 450, "Skill Average 60");
        addLevelUnlockDefault(config, 500, "Skill Average 65");

        addSkyblockTrackDefault(config, "core", "&6Core", "NETHER_STAR", "category_xp.core",
                List.of(1000L, 5000L, 10000L, 15000L, 17089L), List.of(0L, 0L, 0L, 0L, 0L),
                List.of("&7Profile and account progression."));
        addSkyblockTrackDefault(config, "event", "&dEvent", "FIREWORK_STAR", "category_xp.event",
                List.of(250L, 1000L, 2000L, 3000L, 3391L), List.of(0L, 0L, 0L, 0L, 0L),
                List.of("&7Seasonal event progression."));
        addSkyblockTrackDefault(config, "dungeon", "&5Dungeon", "WITHER_SKELETON_SKULL", "category_xp.dungeon",
                List.of(250L, 1000L, 2000L, 3200L, 3905L), List.of(0L, 0L, 0L, 0L, 0L),
                List.of("&7Catacombs and class progression."));
        addSkyblockTrackDefault(config, "essence", "&bEssence", "AMETHYST_SHARD", "category_xp.essence",
                List.of(100L, 300L, 600L, 900L, 1085L), List.of(0L, 0L, 0L, 0L, 0L),
                List.of("&7Essence-related progression."));
        addSkyblockTrackDefault(config, "slaying", "&cSlaying", "IRON_SWORD", "category_xp.slaying",
                List.of(500L, 2000L, 4000L, 6000L, 7410L), List.of(0L, 0L, 0L, 0L, 0L),
                List.of("&7Bestiary and boss progression."));
        addSkyblockTrackDefault(config, "skill", "&aSkill Related", "ENCHANTED_BOOK", "category_xp.skill",
                List.of(500L, 2500L, 5000L, 9000L, 10818L), List.of(0L, 0L, 0L, 0L, 0L),
                List.of("&7Skill-level progression."));
        addSkyblockTrackDefault(config, "misc", "&eMisc", "COMPASS", "category_xp.misc",
                List.of(250L, 1000L, 2500L, 4500L, 5782L), List.of(0L, 0L, 0L, 0L, 0L),
                List.of("&7Misc progression."));
        addSkyblockTrackDefault(config, "story", "&9Story", "WRITABLE_BOOK", "category_xp.story",
                List.of(100L, 400L, 900L, 1300L, 1590L), List.of(0L, 0L, 0L, 0L, 0L),
                List.of("&7Story progression."));
    }

    private void applySkyblockPetDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "skyblock-pets.";
        config.addDefault(base + "enabled", true);

        // Skyblock-accurate defaults: pets contribute stats via the SkyblockCombatStatsService, not via Bukkit potion effects/attributes.
        // Legacy effects are still supported for servers that configured pets.yml that way, but are disabled by default.
        config.addDefault(base + "legacy.potion-effects.enabled", false);
        config.addDefault(base + "legacy.attribute-modifiers.enabled", false);
        config.addDefault(base + "legacy.cleanup-on-join", true);
        config.addDefault(base + "legacy.cleanup-min-duration-ticks", 20 * 60 * 8);

        // Skill XP -> Pet XP (server-tunable). If these are unset, PetSkillXpListener falls back to skyblock-leveling.action-xp.*.
        config.addDefault(base + "skill-xp.combat-kill", 50L);
        config.addDefault(base + "skill-xp.mining-ore", 50L);
        config.addDefault(base + "skill-xp.foraging-log", 50L);
        config.addDefault(base + "skill-xp.farming-harvest", 50L);
        config.addDefault(base + "skill-xp.fishing-catch", 200L);

        // Skyblock-like multipliers for pet EXP earned from Skill XP.
        config.addDefault(base + "multipliers.mining-fishing", 1.5D);
        config.addDefault(base + "multipliers.non-matching", 1.0D / 3.0D);
        config.addDefault(base + "multipliers.non-matching-alchemy-enchanting", 1.0D / 12.0D);
    }

    private void applyMinionDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "minions.";
        config.addDefault(base + "enabled", true);
        config.addDefault(base + "tick-interval-ticks", 20L);
        config.addDefault(base + "auto-save-interval-seconds", 60);
        config.addDefault(base + "base-island-limit", 5);
        config.addDefault(base + "limit-per-island-level", 1);
        config.addDefault(base + "speed-multiplier", 1.0D);
        config.addDefault(base + "storage-multiplier", 1.0D);
        config.addDefault(base + "max-catchup-hours", 24);
        config.addDefault(base + "max-actions-per-tick", 3600);
    }

    private void addLevelUnlockDefault(
            org.bukkit.configuration.file.FileConfiguration config,
            int level,
            String unlock
    ) {
        config.addDefault("skyblock-leveling.level-rewards.feature-unlocks." + level, List.of(unlock));
    }

    private void addSkyblockTrackDefault(
            org.bukkit.configuration.file.FileConfiguration config,
            String id,
            String displayName,
            String icon,
            String counterKey,
            List<Long> milestones,
            List<Long> rewards,
            List<String> lore
    ) {
        String path = "skyblock-leveling.tracks." + id + ".";
        config.addDefault(path + "display-name", displayName);
        config.addDefault(path + "icon", icon);
        config.addDefault(path + "counter-key", counterKey);
        config.addDefault(path + "milestones", milestones);
        config.addDefault(path + "rewards", rewards);
        config.addDefault(path + "lore", lore);
    }

    private void applyScoreboardDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "scoreboard.custom.";
        config.addDefault(base + "enabled", true);
        config.addDefault(base + "title", "&e&lSkyblock");
        config.addDefault(base + "update-ticks", 20L);
        config.addDefault(base + "show-time-line", false);
        config.addDefault(base + "use-skyblock-calendar", true);
        config.addDefault(base + "profile-label", "Profile");
        config.addDefault(base + "bits-default", 0L);
        config.addDefault(base + "default-objective", "Reach Skyblock Level 5");
        config.addDefault(base + "footer", "&eSkyblock");
    }

    private void applyResourcePackDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        config.addDefault("resource-pack.enabled", true);
        config.addDefault("resource-pack.url", "");
        config.addDefault("resource-pack.hash", "");
        config.addDefault("resource-pack.required", true);
        config.addDefault("resource-pack.prompt", "&eThis pack enables custom item textures. Accept?");
        config.addDefault("resource-pack.local.enabled", true);
        config.addDefault("resource-pack.local.file", "resource-pack.zip");
        config.addDefault("resource-pack.local.host", "localhost");
        config.addDefault("resource-pack.local.port", 8765);
        // Default model IDs for custom weapons (override in config if desired)
        int model = 1001;
        String base = "resource-pack.models.";
        config.addDefault(base + "oni_cleaver", model++);
        config.addDefault(base + "tengu_galeblade", model++);
        config.addDefault(base + "tengu_stormbow", model++);
        config.addDefault(base + "tengu_shortbow", model++);
        config.addDefault(base + "kappa_tidebreaker", model++);
        config.addDefault(base + "onryo_spiritblade", model++);
        config.addDefault(base + "onryo_shortbow", model++);
        config.addDefault(base + "jorogumo_stinger", model++);
        config.addDefault(base + "jorogumo_shortbow", model++);
        config.addDefault(base + "kitsune_fang", model++);
        config.addDefault(base + "kitsune_dawnbow", model++);
        config.addDefault(base + "kitsune_shortbow", model++);
        config.addDefault(base + "gashadokuro_nodachi", model++);
        config.addDefault(base + "flying_raijin", model++);
        // Optional name remaps for packs that select models by item name (e.g., Blades of Majestica)
        String names = "resource-pack.name-map.";
        config.addDefault(names + "oni_cleaver", "Demon Lord's Sword");
        config.addDefault(names + "tengu_galeblade", "Chrono Blade");
        config.addDefault(names + "tengu_stormbow", "Crescent Rose");
        config.addDefault(names + "tengu_shortbow", "Amethyst Shuriken");
        config.addDefault(names + "kappa_tidebreaker", "Ocean's Rage");
        config.addDefault(names + "onryo_spiritblade", "Ashura's Blade");
        config.addDefault(names + "onryo_shortbow", "Cyber Katana");
        config.addDefault(names + "jorogumo_stinger", "Demonic Blade");
        config.addDefault(names + "jorogumo_shortbow", "Demonic Cleaver");
        config.addDefault(names + "kitsune_fang", "Divine Justice");
        config.addDefault(names + "kitsune_dawnbow", "Edge Of The Astral Plane");
        config.addDefault(names + "kitsune_shortbow", "Ancient Royal Great Sword");
        config.addDefault(names + "gashadokuro_nodachi", "Demon Lord's Great Axe");
        config.addDefault(names + "flying_raijin", "Creation Splitter");
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

    public CustomItemService getCustomItemService() {
        return customItemService;
    }

    public org.bukkit.NamespacedKey getGrapplingHookKey() {
        return grapplingHookKey;
    }
}
