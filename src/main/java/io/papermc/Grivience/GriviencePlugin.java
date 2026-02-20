package io.papermc.Grivience;

import io.papermc.Grivience.bazaar.BazaarShopManager;
import io.papermc.Grivience.command.BazaarCommand;
import io.papermc.Grivience.command.DungeonCommand;
import io.papermc.Grivience.command.DungeonHubCommand;
import io.papermc.Grivience.command.ReforgeCommand;
import io.papermc.Grivience.command.SkyblockMenuCommand;
import io.papermc.Grivience.command.SanityCheckCommand;
import io.papermc.Grivience.command.WardrobeCommand;
import io.papermc.Grivience.dungeon.DungeonManager;
import io.papermc.Grivience.dungeon.RewardChestManager;
import io.papermc.Grivience.gui.DungeonGuiManager;
import io.papermc.Grivience.gui.SkyblockMenuManager;
import io.papermc.Grivience.hook.AuraSkillsHook;
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
import io.papermc.Grivience.listener.RecipeUnlockListener;
import io.papermc.Grivience.listener.ReforgeAnvilGuiListener;
import io.papermc.Grivience.listener.ResourcePackManager;
import io.papermc.Grivience.mob.CustomMonsterDeathListener;
import io.papermc.Grivience.mob.CustomMonsterManager;
import io.papermc.Grivience.mob.MonsterSpawnAdminCommand;
import io.papermc.Grivience.party.PartyManager;
import io.papermc.Grivience.skyblock.command.HubCommand;
import io.papermc.Grivience.skyblock.command.IslandCommand;
import io.papermc.Grivience.skyblock.command.SkyblockAdminCommand;
import io.papermc.Grivience.skyblock.island.IslandManager;
import io.papermc.Grivience.skyblock.listener.NetherPortalListener;
import io.papermc.Grivience.skyblock.listener.SkyblockListener;
import io.papermc.Grivience.skyblock.listener.IslandProtectionListener;
import io.papermc.Grivience.wardrobe.WardrobeGui;
import io.papermc.Grivience.wardrobe.WardrobeManager;
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
    private NavigationItemManager navigationItemManager;
    private IslandManager islandManager;
    private CustomArmorManager customArmorManager;
    private ArmorSetBonusListener armorSetBonusListener;
    private ArmorCraftingListener armorCraftingListener;
    private CustomMonsterManager customMonsterManager;
    private CustomMonsterDeathListener customMonsterDeathListener;
    private CustomItemService customItemService;
    private AuraSkillsHook auraSkillsHook;
    private CustomWeaponCombatListener customWeaponCombatListener;
    private EnchantTableListener enchantTableListener;
    private CustomItemEnchantListener customItemEnchantListener;
    private ReforgeAnvilGuiListener reforgeAnvilGuiListener;
    private CustomArmorEffectListener customArmorEffectListener;
    private BazaarShopManager bazaarShopManager;
    private RewardChestManager rewardChestManager;
    private ResourcePackManager resourcePackManager;
    private WardrobeManager wardrobeManager;
    private WardrobeGui wardrobeGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        applyConfigUpgrades();

        partyManager = new PartyManager(this);
        customItemService = new CustomItemService(this);
        customItemService.reloadFromConfig();
        customItemService.registerRecipes();
        auraSkillsHook = new AuraSkillsHook(this);
        auraSkillsHook.reload();
        rewardChestManager = new RewardChestManager(this);
        dungeonManager = new DungeonManager(this, partyManager, customItemService, rewardChestManager);
        guiManager = new DungeonGuiManager(this, partyManager, dungeonManager);
        skyblockMenuManager = new SkyblockMenuManager(this);
        navigationItemManager = new NavigationItemManager(this, skyblockMenuManager);
        islandManager = new IslandManager(this);
        islandManager.initializeWorld();
        partyManager.setIslandManager(islandManager);
        skyblockMenuManager.setIslandManager(islandManager);

        // Initialize custom armor system
        if (getConfig().getBoolean("custom-armor.enabled", true)) {
            customArmorManager = new CustomArmorManager(this);
            customArmorManager.reloadFromConfig();
            armorSetBonusListener = new ArmorSetBonusListener(this, customArmorManager);
            armorCraftingListener = new ArmorCraftingListener(this, customArmorManager, customItemService);
            armorCraftingListener.registerRecipes();
            getLogger().info("Custom armor system enabled with " + customArmorManager.getArmorSets().size() + " armor sets.");
        }

        // Initialize custom monster system
        if (getConfig().getBoolean("custom-monsters.enabled", true)) {
            customMonsterManager = new CustomMonsterManager(this);
            customMonsterManager.setCustomItemService(customItemService);
            customMonsterDeathListener = new CustomMonsterDeathListener(this, customMonsterManager);
            getLogger().info("Custom monster system enabled with " + customMonsterManager.getMonsters().size() + " monsters.");
        }
        customWeaponCombatListener = new CustomWeaponCombatListener(
                this,
                customItemService,
                auraSkillsHook,
                dungeonManager,
                partyManager
        );
        customWeaponCombatListener.reloadFromConfig();
        enchantTableListener = new EnchantTableListener(this);
        enchantTableListener.reloadFromConfig();
        customItemEnchantListener = new CustomItemEnchantListener(customItemService);
        reforgeAnvilGuiListener = new ReforgeAnvilGuiListener(this, customItemService);
        customArmorEffectListener = new CustomArmorEffectListener(this, customItemService);
        bazaarShopManager = new BazaarShopManager(this, customItemService, customArmorManager);
        resourcePackManager = new ResourcePackManager(this);
        wardrobeManager = new WardrobeManager(getDataFolder());
        wardrobeGui = new WardrobeGui(this, wardrobeManager);
        ReforgeCommand reforgeCommand = new ReforgeCommand(reforgeAnvilGuiListener);
        BazaarCommand bazaarCommand = new BazaarCommand(bazaarShopManager);
        SanityCheckCommand sanityCheckCommand = new SanityCheckCommand(this, armorCraftingListener, islandManager, customItemService);

        DungeonCommand dungeonCommand = new DungeonCommand(this, partyManager, dungeonManager, guiManager, customItemService);
        registerCommand("dungeon", dungeonCommand);
        registerCommand("party", dungeonCommand);
        registerCommand("reforge", reforgeCommand);
        registerCommand("bazaar", bazaarCommand);
        registerCommand("grisanity", sanityCheckCommand);
        registerCommand("wardrobe", new WardrobeCommand(wardrobeGui));

        SkyblockMenuCommand skyblockMenuCommand = new SkyblockMenuCommand(skyblockMenuManager);
        registerCommand("skyblock", skyblockMenuCommand);

        IslandCommand islandCommand = new IslandCommand(islandManager, partyManager);
        registerCommand("island", islandCommand);
        registerCommand("visit", islandCommand);

        HubCommand hubCommand = new HubCommand(this);
        registerCommand("hub", hubCommand);
        registerCommand("spawn", hubCommand);
        DungeonHubCommand dungeonHubCommand = new DungeonHubCommand(this);
        registerCommand("dungeonhub", dungeonHubCommand);

        SkyblockAdminCommand adminCommand = new SkyblockAdminCommand(this, islandManager);
        registerCommand("skyblockadmin", adminCommand);

        if (customMonsterManager != null) {
            MonsterSpawnAdminCommand monsterAdminCommand = new MonsterSpawnAdminCommand(this, customMonsterManager);
            registerCommand("mobspawn", monsterAdminCommand);
        }

        getServer().getPluginManager().registerEvents(new DungeonListener(this, dungeonManager, customItemService), this);
        getServer().getPluginManager().registerEvents(rewardChestManager, this);
        getServer().getPluginManager().registerEvents(new FriendlyFireListener(dungeonManager, partyManager), this);
        getServer().getPluginManager().registerEvents(guiManager, this);
        getServer().getPluginManager().registerEvents(skyblockMenuManager, this);
        getServer().getPluginManager().registerEvents(navigationItemManager, this);
        getServer().getPluginManager().registerEvents(new SkyblockListener(this, islandManager), this);
        getServer().getPluginManager().registerEvents(new IslandProtectionListener(islandManager), this);
        getServer().getPluginManager().registerEvents(new NetherPortalListener(this, hubCommand), this);
        if (customArmorManager != null) {
            getServer().getPluginManager().registerEvents(armorSetBonusListener, this);
            getServer().getPluginManager().registerEvents(armorCraftingListener, this);
        }
        if (customMonsterManager != null) {
            getServer().getPluginManager().registerEvents(customMonsterDeathListener, this);
        }
        getServer().getPluginManager().registerEvents(customWeaponCombatListener, this);
        getServer().getPluginManager().registerEvents(enchantTableListener, this);
        getServer().getPluginManager().registerEvents(customItemEnchantListener, this);
        getServer().getPluginManager().registerEvents(reforgeAnvilGuiListener, this);
        getServer().getPluginManager().registerEvents(bazaarShopManager, this);
        getServer().getPluginManager().registerEvents(resourcePackManager, this);
        getServer().getPluginManager().registerEvents(new RecipeUnlockListener(this), this);
        getLogger().info("Grivience dungeon system enabled.");
    }

    @Override
    public void onDisable() {
        if (customArmorEffectListener != null) {
            customArmorEffectListener.shutdown();
        }
        if (dungeonManager != null) {
            dungeonManager.shutdown();
        }
        if (bazaarShopManager != null) {
            bazaarShopManager.shutdown();
        }
        if (resourcePackManager != null) {
            resourcePackManager.shutdown();
        }
    }

    public void reloadSystems() {
        reloadConfig();
        applyConfigUpgrades();
        if (partyManager != null) {
            partyManager.reloadFromConfig();
        }
        if (dungeonManager != null) {
            dungeonManager.reloadFromConfig();
        }
        if (customItemService != null) {
            customItemService.reloadFromConfig();
            customItemService.registerRecipes();
        }
        if (auraSkillsHook != null) {
            auraSkillsHook.reload();
        }
        if (customWeaponCombatListener != null) {
            customWeaponCombatListener.reloadFromConfig();
        }
        if (enchantTableListener != null) {
            enchantTableListener.reloadFromConfig();
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

    private void applyResourcePackDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        config.addDefault("resource-pack.enabled", false);
        config.addDefault("resource-pack.url", "");
        config.addDefault("resource-pack.hash", "");
        config.addDefault("resource-pack.required", false);
        config.addDefault("resource-pack.prompt", "&eThis pack enables custom item textures. Accept?");
        config.addDefault("resource-pack.local.enabled", false);
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
    }
}

