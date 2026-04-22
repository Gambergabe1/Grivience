package io.papermc.Grivience.npcshop;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.bazaar.BazaarShopManager;
import io.papermc.Grivience.skyblock.economy.ProfileEconomyService;

import java.util.HashMap;
import java.util.Map;

public class NpcShopManager {
    private final GriviencePlugin plugin;
    private final NpcShopGui gui;
    private final Map<String, NpcShop> shops = new HashMap<>();

    public NpcShopManager(GriviencePlugin plugin, BazaarShopManager bazaarManager) {
        this.plugin = plugin;
        this.gui = new NpcShopGui(plugin, new ProfileEconomyService(plugin), bazaarManager);
        plugin.getServer().getPluginManager().registerEvents(this.gui, plugin);
        
        registerDefaultShops();
    }

    public void registerShop(NpcShop shop) {
        shops.put(shop.getId().toLowerCase(), shop);
    }

    public NpcShop getShop(String id) {
        return shops.get(id.toLowerCase());
    }

    public Map<String, NpcShop> getShops() {
        return shops;
    }

    public NpcShopGui getGui() {
        return gui;
    }
    
    private void registerDefaultShops() {
        io.papermc.Grivience.item.CustomItemService customItemService = plugin.getCustomItemService();
        if (customItemService == null) {
            customItemService = new io.papermc.Grivience.item.CustomItemService(plugin);
        }

        NpcShop adventurer = new NpcShop("adventurer", org.bukkit.ChatColor.DARK_GREEN + "Adventurer");
        adventurer.setItem(0, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.ROTTEN_FLESH, 1), 8.0));
        adventurer.setItem(1, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BONE, 1), 8.0));
        adventurer.setItem(2, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.STRING, 1), 10.0));
        adventurer.setItem(3, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.SLIME_BALL, 1), 14.0));
        adventurer.setItem(4, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.GUNPOWDER, 1), 16.0));
        registerShop(adventurer);
        
        NpcShop armorsmith = new NpcShop("armorsmith", org.bukkit.ChatColor.DARK_GRAY + "Armorsmith");
        // Leather Set
        armorsmith.setItem(0, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_HELMET, 1), 15.0));
        armorsmith.setItem(1, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_CHESTPLATE, 1), 25.0));
        armorsmith.setItem(2, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_LEGGINGS, 1), 20.0));
        armorsmith.setItem(3, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.LEATHER_BOOTS, 1), 10.0));
        // Chainmail Set
        armorsmith.setItem(4, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHAINMAIL_HELMET, 1), 30.0));
        armorsmith.setItem(5, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHAINMAIL_CHESTPLATE, 1), 50.0));
        armorsmith.setItem(6, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHAINMAIL_LEGGINGS, 1), 40.0));
        armorsmith.setItem(7, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHAINMAIL_BOOTS, 1), 25.0));
        // Iron Set
        armorsmith.setItem(8, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_HELMET, 1), 50.0));
        armorsmith.setItem(9, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_CHESTPLATE, 1), 80.0));
        armorsmith.setItem(10, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_LEGGINGS, 1), 70.0));
        armorsmith.setItem(11, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_BOOTS, 1), 40.0));

        // Rookie Samurai Set
        armorsmith.setItem(12, new NpcShopItem(customItemService.createArmor(io.papermc.Grivience.item.CustomArmorType.ROOKIE_SAMURAI_KABUTO), 1500.0));
        armorsmith.setItem(13, new NpcShopItem(customItemService.createArmor(io.papermc.Grivience.item.CustomArmorType.ROOKIE_SAMURAI_DO), 2500.0));
        armorsmith.setItem(14, new NpcShopItem(customItemService.createArmor(io.papermc.Grivience.item.CustomArmorType.ROOKIE_SAMURAI_KOTE), 2000.0));
        armorsmith.setItem(15, new NpcShopItem(customItemService.createArmor(io.papermc.Grivience.item.CustomArmorType.ROOKIE_SAMURAI_SUNEATE), 1200.0));

        registerShop(armorsmith);
        
        NpcShop weaponsmith = new NpcShop("weaponsmith", org.bukkit.ChatColor.DARK_RED + "Weaponsmith");
        // Custom Weapon for Newbies
        weaponsmith.setItem(0, new NpcShopItem(customItemService.createWeapon(io.papermc.Grivience.item.CustomWeaponType.NEWBIE_KATANA), 500.0));
        
        // Basic Weapons
        weaponsmith.setItem(1, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.WOODEN_SWORD, 1), 5.0));
        weaponsmith.setItem(2, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.STONE_SWORD, 1), 15.0));
        weaponsmith.setItem(3, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_SWORD, 1), 100.0));
        
        // Ranged
        weaponsmith.setItem(4, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BOW, 1), 50.0));
        weaponsmith.setItem(5, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CROSSBOW, 1), 80.0));
        weaponsmith.setItem(6, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.ARROW, 16), 16.0));
        weaponsmith.setItem(7, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.ARROW, 64), 64.0));
        registerShop(weaponsmith);

        registerBuilderShop();

        NpcShop fisher = new NpcShop("fish_merchant", org.bukkit.ChatColor.AQUA + "Fish Merchant");
        fisher.setItem(0, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.FISHING_ROD, 1), 100.0));
        fisher.setItem(1, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.COD, 16), 80.0));
        fisher.setItem(2, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.SALMON, 16), 120.0));
        fisher.setItem(3, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.PUFFERFISH, 1), 50.0));
        fisher.setItem(4, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.TROPICAL_FISH, 1), 50.0));
        registerShop(fisher);

        registerLarryShop();
    }

    private void registerLarryShop() {
        NpcShop larry = new NpcShop("larry", org.bukkit.ChatColor.DARK_PURPLE + "Larry the Wizard");
        io.papermc.Grivience.item.CustomItemService customItemService = plugin.getCustomItemService();
        
        if (customItemService != null) {
            larry.setItem(0, new NpcShopItem(customItemService.createArmor(io.papermc.Grivience.item.CustomArmorType.SOULBOUND_HELM), 5000000.0));
            larry.setItem(1, new NpcShopItem(customItemService.createArmor(io.papermc.Grivience.item.CustomArmorType.SOULBOUND_CHESTPLATE), 8000000.0));
            larry.setItem(2, new NpcShopItem(customItemService.createArmor(io.papermc.Grivience.item.CustomArmorType.SOULBOUND_LEGGINGS), 7000000.0));
            larry.setItem(3, new NpcShopItem(customItemService.createArmor(io.papermc.Grivience.item.CustomArmorType.SOULBOUND_BOOTS), 4000000.0));
        }
        
        registerShop(larry);
    }

    private void registerBuilderShop() {
        NpcShop builder = new NpcShop("builder", org.bukkit.ChatColor.BLUE + "Builder");
        
        builder.setItem(0, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.OAK_LOG), 10, "builder_wood"));
        builder.setItem(1, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.STONE), 10, "builder_stone"));
        builder.setItem(2, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.COBBLED_DEEPSLATE), 10, "builder_deepslate"));
        builder.setItem(3, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.WHITE_CONCRETE), 10, "builder_concrete"));
        builder.setItem(4, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.GLASS), 10, "builder_glass"));
        builder.setItem(5, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.SANDSTONE), 10, "builder_sandstone"));
        builder.setItem(6, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.PRISMARINE), 10, "builder_prismarine"));
        builder.setItem(7, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.TERRACOTTA), 10, "builder_clay"));
        builder.setItem(8, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHERRACK), 10, "builder_nether"));
        builder.setItem(9, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DIRT), 5));
        builder.setItem(10, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.GRAVEL), 5));
        builder.setItem(11, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.SAND), 5));

        registerShop(builder);

        // Woods
        NpcShop wood = new NpcShop("builder_wood", org.bukkit.ChatColor.GOLD + "Builder > Wood");
        addWoodSet(wood, "OAK", 0);
        addWoodSet(wood, "SPRUCE", 4);
        addWoodSet(wood, "BIRCH", 8);
        addWoodSet(wood, "JUNGLE", 12);
        addWoodSet(wood, "ACACIA", 16);
        addWoodSet(wood, "DARK_OAK", 20);
        addWoodSet(wood, "MANGROVE", 24);
        // Let's fit more in if we can or just use more sub-shops, but 28 slots is plenty.
        // Continuing re-indexing to be sequential.
        registerShop(wood);

        // Stones
        NpcShop stone = new NpcShop("builder_stone", org.bukkit.ChatColor.GRAY + "Builder > Stone");
        stone.setItem(0, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.STONE), 5));
        stone.setItem(1, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.STONE_BRICKS), 5));
        stone.setItem(2, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.MOSSY_STONE_BRICKS), 7));
        stone.setItem(3, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CRACKED_STONE_BRICKS), 7));
        stone.setItem(4, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHISELED_STONE_BRICKS), 10));
        stone.setItem(5, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.COBBLESTONE), 3));
        stone.setItem(6, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.MOSSY_COBBLESTONE), 5));
        stone.setItem(7, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.SMOOTH_STONE), 8));
        stone.setItem(8, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.POLISHED_ANDESITE), 10));
        stone.setItem(9, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.POLISHED_DIORITE), 10));
        stone.setItem(10, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.POLISHED_GRANITE), 10));
        registerShop(stone);

        // Deepslate
        NpcShop deep = new NpcShop("builder_deepslate", org.bukkit.ChatColor.DARK_GRAY + "Builder > Deepslate");
        deep.setItem(0, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.COBBLED_DEEPSLATE), 5));
        deep.setItem(1, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.POLISHED_DEEPSLATE), 8));
        deep.setItem(2, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DEEPSLATE_BRICKS), 10));
        deep.setItem(3, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DEEPSLATE_TILES), 10));
        deep.setItem(4, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CHISELED_DEEPSLATE), 15));
        registerShop(deep);

        // Concrete
        NpcShop concrete = new NpcShop("builder_concrete", org.bukkit.ChatColor.WHITE + "Builder > Concrete");
        String[] colors = {"WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY", "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"};
        for (int i = 0; i < colors.length; i++) {
            concrete.setItem(i, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.valueOf(colors[i] + "_CONCRETE")), 10));
        }
        registerShop(concrete);

        // Glass
        NpcShop glass = new NpcShop("builder_glass", org.bukkit.ChatColor.AQUA + "Builder > Glass");
        glass.setItem(0, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.GLASS), 5));
        glass.setItem(1, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.GLASS_PANE), 2));
        for (int i = 0; i < colors.length; i++) {
            glass.setItem(2 + i, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.valueOf(colors[i] + "_STAINED_GLASS")), 10));
        }
        registerShop(glass);

        // Sandstone
        NpcShop sand = new NpcShop("builder_sandstone", org.bukkit.ChatColor.YELLOW + "Builder > Sandstone");
        sand.setItem(0, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.SANDSTONE), 5));
        sand.setItem(1, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.SMOOTH_SANDSTONE), 8));
        sand.setItem(2, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CUT_SANDSTONE), 8));
        sand.setItem(3, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.RED_SANDSTONE), 5));
        sand.setItem(4, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.SMOOTH_RED_SANDSTONE), 8));
        sand.setItem(5, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.CUT_RED_SANDSTONE), 8));
        registerShop(sand);

        // Prismarine
        NpcShop pris = new NpcShop("builder_prismarine", org.bukkit.ChatColor.DARK_AQUA + "Builder > Prismarine");
        pris.setItem(0, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.PRISMARINE), 15));
        pris.setItem(1, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.PRISMARINE_BRICKS), 20));
        pris.setItem(2, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.DARK_PRISMARINE), 25));
        pris.setItem(3, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.SEA_LANTERN), 50));
        registerShop(pris);

        // Nether
        NpcShop nether = new NpcShop("builder_nether", org.bukkit.ChatColor.RED + "Builder > Nether");
        nether.setItem(0, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHERRACK), 2));
        nether.setItem(1, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.NETHER_BRICKS), 10));
        nether.setItem(2, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.RED_NETHER_BRICKS), 15));
        nether.setItem(3, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BASALT), 10));
        nether.setItem(4, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.POLISHED_BASALT), 12));
        nether.setItem(5, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BLACKSTONE), 10));
        nether.setItem(6, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.POLISHED_BLACKSTONE_BRICKS), 15));
        nether.setItem(7, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.QUARTZ_BLOCK), 20));
        nether.setItem(8, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.QUARTZ_BRICKS), 25));
        nether.setItem(9, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.PURPUR_BLOCK), 20));
        nether.setItem(10, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.PURPUR_PILLAR), 25));
        nether.setItem(11, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.END_STONE), 10));
        nether.setItem(12, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.END_STONE_BRICKS), 15));
        registerShop(nether);

        // Clay/Bricks
        NpcShop clay = new NpcShop("builder_clay", org.bukkit.ChatColor.RED + "Builder > Clay & Bricks");
        clay.setItem(0, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.TERRACOTTA), 10));
        clay.setItem(1, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BRICKS), 15));
        clay.setItem(2, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.MUD_BRICKS), 12));
        for (int i = 0; i < colors.length; i++) {
            clay.setItem(3 + i, new NpcShopItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.valueOf(colors[i] + "_TERRACOTTA")), 15));
        }
        registerShop(clay);
    }

    private void addWoodSet(NpcShop shop, String prefix, int startSlot) {
        String logType = (prefix.equalsIgnoreCase("CRIMSON") || prefix.equalsIgnoreCase("WARPED")) ? "_STEM" : "_LOG";
        
        org.bukkit.Material logMat = org.bukkit.Material.matchMaterial(prefix + logType);
        org.bukkit.Material plankMat = org.bukkit.Material.matchMaterial(prefix + "_PLANKS");
        org.bukkit.Material slabMat = org.bukkit.Material.matchMaterial(prefix + "_SLAB");
        org.bukkit.Material stairMat = org.bukkit.Material.matchMaterial(prefix + "_STAIRS");

        if (logMat != null) shop.setItem(startSlot, new NpcShopItem(new org.bukkit.inventory.ItemStack(logMat), 10));
        if (plankMat != null) shop.setItem(startSlot + 1, new NpcShopItem(new org.bukkit.inventory.ItemStack(plankMat), 5));
        if (slabMat != null) shop.setItem(startSlot + 2, new NpcShopItem(new org.bukkit.inventory.ItemStack(slabMat), 3));
        if (stairMat != null) shop.setItem(startSlot + 3, new NpcShopItem(new org.bukkit.inventory.ItemStack(stairMat), 8));
    }
}
