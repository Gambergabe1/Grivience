package io.papermc.Grivience.welcome.quest;

/**
 * Represents the type of quest objective.
 */
public enum QuestType {
    // Mining quests
    MINE_BLOCKS,
    MINE_SPECIFIC_BLOCK,
    MINE_ORE,
    
    // Fishing quests
    CATCH_FISH,
    CATCH_SPECIFIC_FISH,
    
    // Combat quests
    KILL_MOB,
    KILL_SPECIFIC_MOB,
    KILL_MOBS_COUNT,
    
    // Farming quests
    HARVEST_CROPS,
    PLANT_CROPS,
    BREED_ANIMALS,
    
    // Foraging quests
    CHOP_WOOD,
    CHOP_SPECIFIC_WOOD,
    
    // General quests
    CRAFT_ITEM,
    EARN_MONEY,
    REACH_LEVEL,
    VISIT_LOCATION,
    TALK_TO_NPC,
    COMPLETE_DUNGEON,
    JOIN_PARTY,
    OPEN_BAZAAR,
    PLACE_ORDER,
    USE_CRAFTING_TABLE,
    USE_ENCHANTING_TABLE,
    USE_ANVIL
}
