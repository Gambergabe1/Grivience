package io.papermc.Grivience;

import io.papermc.Grivience.command.DungeonCommand;
import io.papermc.Grivience.dungeon.DungeonManager;
import io.papermc.Grivience.gui.DungeonGuiManager;
import io.papermc.Grivience.hook.AuraSkillsHook;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.listener.CustomArmorEffectListener;
import io.papermc.Grivience.listener.CustomWeaponCombatListener;
import io.papermc.Grivience.listener.DungeonListener;
import io.papermc.Grivience.listener.CustomItemEnchantListener;
import io.papermc.Grivience.listener.EnchantTableListener;
import io.papermc.Grivience.listener.FriendlyFireListener;
import io.papermc.Grivience.listener.ReforgeAnvilGuiListener;
import io.papermc.Grivience.party.PartyManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class GriviencePlugin extends JavaPlugin {
    private PartyManager partyManager;
    private DungeonManager dungeonManager;
    private DungeonGuiManager guiManager;
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

        getServer().getPluginManager().registerEvents(new DungeonListener(this, dungeonManager), this);
        getServer().getPluginManager().registerEvents(new FriendlyFireListener(dungeonManager, partyManager), this);
        getServer().getPluginManager().registerEvents(guiManager, this);
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
    }

    private void registerCommand(String name, DungeonCommand dungeonCommand) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().severe("Command '" + name + "' is missing from plugin.yml.");
            return;
        }
        command.setExecutor(dungeonCommand);
        command.setTabCompleter(dungeonCommand);
    }
}
