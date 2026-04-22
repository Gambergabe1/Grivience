package io.papermc.Grivience.quest;

public record QuestObjective(
        ObjectiveType type,
        String target, // npcId, itemId, mobType, skillName
        int amount,    // required amount (1 for NPCs)
        String description
) {
    public enum ObjectiveType {
        TALK_TO_NPC,
        COLLECT_ITEMS,
        KILL_MOBS,
        REACH_LEVEL
    }

    public String progressLabel(int current) {
        if (type == ObjectiveType.TALK_TO_NPC) {
            return current >= 1 ? "✔ Done" : "Talk to " + target;
        }
        if (amount <= 1) {
            return current >= 1 ? "✔ Done" : description;
        }
        return description + " (" + current + "/" + amount + ")";
    }

    public boolean isComplete(int current) {
        return current >= amount;
    }
}
