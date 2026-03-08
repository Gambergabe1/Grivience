package io.papermc.Grivience.welcome.quest;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Registry containing all welcome quest line quests.
 * Organized in a progressive difficulty curve for new players.
 */
public class WelcomeQuestRegistry {
    private static final Map<String, WelcomeQuest> QUESTS = new LinkedHashMap<>();

    /**
     * Initialize and register all welcome quests.
     */
    public static void init() {
        if (!QUESTS.isEmpty()) {
            return; // Already initialized
        }

        // ==================== TIER 1: BEGINNER QUESTS ====================
        // These introduce basic game mechanics

        // Quest 1: First Steps - Mine cobblestone
        register(WelcomeQuest.builder("first_steps", "First Steps")
            .description("Mine your first cobblestone blocks to start your journey!")
            .tier(QuestTier.BEGINNER)
            .type(QuestType.MINE_SPECIFIC_BLOCK)
            .requiredAmount(10)
            .moneyReward(100)
            .xpReward(50)
            .icon(Material.COBBLESTONE)
            .build());

        // Quest 2: Lumberjack - Chop wood
        register(WelcomeQuest.builder("lumberjack", "Lumberjack")
            .description("Chop down some trees to gather wood for tools and building.")
            .tier(QuestTier.BEGINNER)
            .type(QuestType.CHOP_WOOD)
            .requiredAmount(20)
            .moneyReward(150)
            .xpReward(75)
            .icon(Material.OAK_LOG)
            .previousQuestId("first_steps")
            .build());

        // Quest 3: Getting Started - Craft a crafting table
        register(WelcomeQuest.builder("getting_started", "Getting Started")
            .description("Craft a crafting table to unlock more recipes.")
            .tier(QuestTier.BEGINNER)
            .type(QuestType.USE_CRAFTING_TABLE)
            .requiredAmount(1)
            .moneyReward(200)
            .xpReward(100)
            .icon(Material.CRAFTING_TABLE)
            .previousQuestId("lumberjack")
            .build());

        // Quest 4: Tool Time - Craft wooden tools
        register(WelcomeQuest.builder("tool_time", "Tool Time")
            .description("Craft your first set of wooden tools.")
            .tier(QuestTier.BEGINNER)
            .type(QuestType.CRAFT_ITEM)
            .requiredAmount(3)
            .moneyReward(250)
            .xpReward(125)
            .icon(Material.WOODEN_PICKAXE)
            .previousQuestId("getting_started")
            .build());

        // Quest 5: Farmer's Beginnings - Plant crops
        register(WelcomeQuest.builder("farmers_beginnings", "Farmer's Beginnings")
            .description("Plant some seeds to start your farming journey.")
            .tier(QuestTier.BEGINNER)
            .type(QuestType.PLANT_CROPS)
            .requiredAmount(10)
            .moneyReward(300)
            .xpReward(150)
            .icon(Material.WHEAT_SEEDS)
            .previousQuestId("tool_time")
            .build());

        // Quest 6: First Catch - Go fishing
        register(WelcomeQuest.builder("first_catch", "First Catch")
            .description("Try your luck at fishing to catch some fish.")
            .tier(QuestTier.BEGINNER)
            .type(QuestType.CATCH_FISH)
            .requiredAmount(3)
            .moneyReward(350)
            .xpReward(175)
            .icon(Material.FISHING_ROD)
            .previousQuestId("farmers_beginnings")
            .build());

        // Quest 7: Market Explorer - Open the bazaar
        register(WelcomeQuest.builder("market_explorer", "Market Explorer")
            .description("Open the bazaar to see what items you can trade.")
            .tier(QuestTier.BEGINNER)
            .type(QuestType.OPEN_BAZAAR)
            .requiredAmount(1)
            .moneyReward(400)
            .xpReward(200)
            .icon(Material.EMERALD)
            .previousQuestId("first_catch")
            .build());

        // Quest 8: Social Butterfly - Join a party
        register(WelcomeQuest.builder("social_butterfly", "Social Butterfly")
            .description("Join or create a party to play with friends!")
            .tier(QuestTier.BEGINNER)
            .type(QuestType.JOIN_PARTY)
            .requiredAmount(1)
            .moneyReward(500)
            .xpReward(250)
            .icon(Material.PLAYER_HEAD)
            .previousQuestId("market_explorer")
            .build());

        // ==================== TIER 2: INTERMEDIATE QUESTS ====================
        // These build on basic knowledge

        // Quest 9: Deep Miner - Mine iron ore
        register(WelcomeQuest.builder("deep_miner", "Deep Miner")
            .description("Venture deeper and mine some iron ore.")
            .tier(QuestTier.INTERMEDIATE)
            .type(QuestType.MINE_ORE)
            .requiredAmount(16)
            .moneyReward(600)
            .xpReward(300)
            .icon(Material.IRON_ORE)
            .previousQuestId("social_butterfly")
            .build());

        // Quest 10: Iron Age - Craft iron tools
        register(WelcomeQuest.builder("iron_age", "Iron Age")
            .description("Upgrade your tools to iron for better efficiency.")
            .tier(QuestTier.INTERMEDIATE)
            .type(QuestType.CRAFT_ITEM)
            .requiredAmount(3)
            .moneyReward(700)
            .xpReward(350)
            .icon(Material.IRON_PICKAXE)
            .previousQuestId("deep_miner")
            .build());

        // Quest 11: Harvester - Harvest crops
        register(WelcomeQuest.builder("harvester", "Harvester")
            .description("Harvest your grown crops.")
            .tier(QuestTier.INTERMEDIATE)
            .type(QuestType.HARVEST_CROPS)
            .requiredAmount(50)
            .moneyReward(800)
            .xpReward(400)
            .icon(Material.WHEAT)
            .previousQuestId("iron_age")
            .build());

        // Quest 12: Monster Hunter - Kill hostile mobs
        register(WelcomeQuest.builder("monster_hunter", "Monster Hunter")
            .description("Defeat hostile mobs to protect yourself and gain loot.")
            .tier(QuestTier.INTERMEDIATE)
            .type(QuestType.KILL_MOBS_COUNT)
            .requiredAmount(10)
            .moneyReward(900)
            .xpReward(450)
            .icon(Material.IRON_SWORD)
            .previousQuestId("harvester")
            .build());

        // Quest 13: Entrepreneur - Place a bazaar order
        register(WelcomeQuest.builder("entrepreneur", "Entrepreneur")
            .description("Place your first buy or sell order on the bazaar.")
            .tier(QuestTier.INTERMEDIATE)
            .type(QuestType.PLACE_ORDER)
            .requiredAmount(1)
            .moneyReward(1000)
            .xpReward(500)
            .icon(Material.BOOK)
            .previousQuestId("monster_hunter")
            .build());

        // Quest 14: Enchanter - Use an enchanting table
        register(WelcomeQuest.builder("enchanter", "Enchanter")
            .description("Enchant an item to make it more powerful.")
            .tier(QuestTier.INTERMEDIATE)
            .type(QuestType.USE_ENCHANTING_TABLE)
            .requiredAmount(1)
            .moneyReward(1100)
            .xpReward(550)
            .icon(Material.ENCHANTING_TABLE)
            .previousQuestId("entrepreneur")
            .build());

        // Quest 15: Blacksmith - Use an anvil
        register(WelcomeQuest.builder("blacksmith", "Blacksmith")
            .description("Use an anvil to combine enchantments or repair tools.")
            .tier(QuestTier.INTERMEDIATE)
            .type(QuestType.USE_ANVIL)
            .requiredAmount(1)
            .moneyReward(1200)
            .xpReward(600)
            .icon(Material.ANVIL)
            .previousQuestId("enchanter")
            .build());

        // ==================== TIER 3: ADVANCED QUESTS ====================
        // These require good understanding of game mechanics

        // Quest 16: Diamond Digger - Mine diamonds
        register(WelcomeQuest.builder("diamond_digger", "Diamond Digger")
            .description("Mine precious diamonds for advanced gear.")
            .tier(QuestTier.ADVANCED)
            .type(QuestType.MINE_ORE)
            .requiredAmount(5)
            .moneyReward(1500)
            .xpReward(750)
            .icon(Material.DIAMOND)
            .previousQuestId("blacksmith")
            .build());

        // Quest 17: Wealth Builder - Earn money
        register(WelcomeQuest.builder("wealth_builder", "Wealth Builder")
            .description("Earn money through trading and activities.")
            .tier(QuestTier.ADVANCED)
            .type(QuestType.EARN_MONEY)
            .requiredAmount(5000)
            .moneyReward(2000)
            .xpReward(1000)
            .icon(Material.GOLD_BLOCK)
            .previousQuestId("diamond_digger")
            .build());

        // Quest 18: Dungeon Explorer - Complete a dungeon
        register(WelcomeQuest.builder("dungeon_explorer", "Dungeon Explorer")
            .description("Enter and complete a dungeon run.")
            .tier(QuestTier.ADVANCED)
            .type(QuestType.COMPLETE_DUNGEON)
            .requiredAmount(1)
            .moneyReward(2500)
            .xpReward(1250)
            .icon(Material.NETHER_STAR)
            .previousQuestId("wealth_builder")
            .build());

        // Quest 19: Animal Breeder - Breed animals
        register(WelcomeQuest.builder("animal_breeder", "Animal Breeder")
            .description("Breed animals to expand your farm.")
            .tier(QuestTier.ADVANCED)
            .type(QuestType.BREED_ANIMALS)
            .requiredAmount(5)
            .moneyReward(1800)
            .xpReward(900)
            .icon(Material.WHEAT)
            .previousQuestId("dungeon_explorer")
            .build());

        // Quest 20: Master Builder - Visit a location
        register(WelcomeQuest.builder("master_builder", "Master Builder")
            .description("Explore the server and visit key locations.")
            .tier(QuestTier.ADVANCED)
            .type(QuestType.VISIT_LOCATION)
            .requiredAmount(3)
            .moneyReward(2000)
            .xpReward(1000)
            .icon(Material.MAP)
            .previousQuestId("animal_breeder")
            .build());

        // ==================== TIER 4: EXPERT QUESTS ====================
        // These are challenging and require dedication

        // Quest 21: Master Miner - Mine lots of blocks
        register(WelcomeQuest.builder("master_miner", "Master Miner")
            .description("Become a master miner by mining thousands of blocks.")
            .tier(QuestTier.EXPERT)
            .type(QuestType.MINE_BLOCKS)
            .requiredAmount(1000)
            .moneyReward(3000)
            .xpReward(1500)
            .icon(Material.DIAMOND_PICKAXE)
            .previousQuestId("master_builder")
            .build());

        // Quest 22: Master Lumberjack - Chop lots of wood
        register(WelcomeQuest.builder("master_lumberjack", "Master Lumberjack")
            .description("Chop down a massive amount of wood.")
            .tier(QuestTier.EXPERT)
            .type(QuestType.CHOP_SPECIFIC_WOOD)
            .requiredAmount(500)
            .moneyReward(2800)
            .xpReward(1400)
            .icon(Material.DIAMOND_AXE)
            .previousQuestId("master_miner")
            .build());

        // Quest 23: Master Fisher - Catch many fish
        register(WelcomeQuest.builder("master_fisher", "Master Fisher")
            .description("Catch a large number of fish.")
            .tier(QuestTier.EXPERT)
            .type(QuestType.CATCH_FISH)
            .requiredAmount(50)
            .moneyReward(2600)
            .xpReward(1300)
            .icon(Material.DIAMOND_HOE)
            .previousQuestId("master_lumberjack")
            .build());

        // Quest 24: Slayer - Kill many mobs
        register(WelcomeQuest.builder("slayer", "Slayer")
            .description("Become a renowned monster slayer.")
            .tier(QuestTier.EXPERT)
            .type(QuestType.KILL_MOBS_COUNT)
            .requiredAmount(100)
            .moneyReward(3200)
            .xpReward(1600)
            .icon(Material.DIAMOND_SWORD)
            .previousQuestId("master_fisher")
            .build());

        // ==================== TIER 5: MASTER QUESTS ====================
        // These are the ultimate challenges

        // Quest 25: Server Veteran - Complete all quests
        register(WelcomeQuest.builder("server_veteran", "Server Veteran")
            .description("Complete all other welcome quests to become a server veteran!")
            .tier(QuestTier.MASTER)
            .type(QuestType.COMPLETE_DUNGEON)
            .requiredAmount(10)
            .moneyReward(5000)
            .xpReward(2500)
            .icon(Material.NETHERITE_INGOT)
            .previousQuestId("slayer")
            .build());
    }

    private static void register(WelcomeQuest quest) {
        QUESTS.put(quest.getId(), quest);
    }

    /**
     * Get a quest by its ID.
     */
    public static WelcomeQuest get(String id) {
        return QUESTS.get(id);
    }

    /**
     * Get all registered quests.
     */
    public static Collection<WelcomeQuest> getAll() {
        return Collections.unmodifiableCollection(QUESTS.values());
    }

    /**
     * Get quests by tier.
     */
    public static List<WelcomeQuest> getByTier(QuestTier tier) {
        List<WelcomeQuest> result = new ArrayList<>();
        for (WelcomeQuest quest : QUESTS.values()) {
            if (quest.getTier() == tier) {
                result.add(quest);
            }
        }
        return result;
    }

    /**
     * Get the first quest in the quest line.
     */
    public static WelcomeQuest getFirstQuest() {
        for (WelcomeQuest quest : QUESTS.values()) {
            if (quest.getPreviousQuestId() == null) {
                return quest;
            }
        }
        return null;
    }

    /**
     * Get the next quest in the quest line.
     */
    public static WelcomeQuest getNextQuest(String currentQuestId) {
        for (WelcomeQuest quest : QUESTS.values()) {
            if (currentQuestId.equals(quest.getPreviousQuestId())) {
                return quest;
            }
        }
        return null;
    }

    /**
     * Check if a quest exists.
     */
    public static boolean hasQuest(String id) {
        return QUESTS.containsKey(id);
    }

    /**
     * Get the total count of registered quests.
     */
    public static int count() {
        return QUESTS.size();
    }

    /**
     * Get quests that are available (prerequisite completed).
     */
    public static List<WelcomeQuest> getAvailableQuests(Set<String> completedQuests) {
        List<WelcomeQuest> available = new ArrayList<>();
        for (WelcomeQuest quest : QUESTS.values()) {
            if (quest.getPreviousQuestId() == null || completedQuests.contains(quest.getPreviousQuestId())) {
                if (!completedQuests.contains(quest.getId()) || quest.isRepeatable()) {
                    available.add(quest);
                }
            }
        }
        return available;
    }
}
