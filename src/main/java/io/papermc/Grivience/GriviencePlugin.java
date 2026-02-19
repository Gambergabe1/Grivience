package io.papermc.Grivience;

import io.papermc.Grivience.command.DungeonCommand;
import io.papermc.Grivience.command.SkyblockMenuCommand;
import io.papermc.Grivience.dungeon.DungeonManager;
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
import io.papermc.Grivience.listener.ReforgeAnvilGuiListener;
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
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

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

    @Override
    public void onEnable() {
        saveDefaultConfig();

        partyManager = new PartyManager(this);
        customItemService = new CustomItemService(this);
        customItemService.reloadFromConfig();
        customItemService.registerRecipes();
        auraSkillsHook = new AuraSkillsHook(this);
        auraSkillsHook.reload();
        dungeonManager = new DungeonManager(this, partyManager, customItemService);
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
            armorCraftingListener = new ArmorCraftingListener(this, customArmorManager);
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

        DungeonCommand dungeonCommand = new DungeonCommand(this, partyManager, dungeonManager, guiManager, customItemService);
        registerCommand("dungeon", dungeonCommand);
        registerCommand("party", dungeonCommand);

        SkyblockMenuCommand skyblockMenuCommand = new SkyblockMenuCommand(skyblockMenuManager);
        registerCommand("skyblock", skyblockMenuCommand);

        IslandCommand islandCommand = new IslandCommand(islandManager, partyManager);
        registerCommand("island", islandCommand);

        HubCommand hubCommand = new HubCommand(this);
        registerCommand("hub", hubCommand);
        registerCommand("spawn", hubCommand);

        SkyblockAdminCommand adminCommand = new SkyblockAdminCommand(this);
        registerCommand("skyblockadmin", adminCommand);

        if (customMonsterManager != null) {
            MonsterSpawnAdminCommand monsterAdminCommand = new MonsterSpawnAdminCommand(this, customMonsterManager);
            registerCommand("mobspawn", monsterAdminCommand);
        }

        getServer().getPluginManager().registerEvents(new DungeonListener(this, dungeonManager, customItemService), this);
        getServer().getPluginManager().registerEvents(new FriendlyFireListener(dungeonManager, partyManager), this);
        getServer().getPluginManager().registerEvents(guiManager, this);
        getServer().getPluginManager().registerEvents(skyblockMenuManager, this);
        getServer().getPluginManager().registerEvents(navigationItemManager, this);
        getServer().getPluginManager().registerEvents(new SkyblockListener(this, islandManager), this);
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
    }

    public void reloadSystems() {
        reloadConfig();
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
}
