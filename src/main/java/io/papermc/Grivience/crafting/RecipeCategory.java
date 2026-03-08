package io.papermc.Grivience.crafting;

public enum RecipeCategory {
    FARMING("Farming"),
    MINING("Mining"),
    COMBAT("Combat"),
    FISHING("Fishing"),
    FORAGING("Foraging"),
    ENCHANTING("Enchanting"),
    ALCHEMY("Alchemy"),
    CARPENTRY("Carpentry"),
    SLAYER("Slayer"),
    SPECIAL("Special");

    private final String displayName;

    RecipeCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
