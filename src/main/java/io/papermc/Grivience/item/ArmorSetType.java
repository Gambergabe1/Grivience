package io.papermc.Grivience.item;

public enum ArmorSetType {
    SHOGUN("Shogun", "Warlord's Resolve"),
    SHINOBI("Shinobi", "Shadowstep"),
    ONMYOJI("Onmyoji", "Spirit Ward");

    private final String displayName;
    private final String fullSetBonusName;

    ArmorSetType(String displayName, String fullSetBonusName) {
        this.displayName = displayName;
        this.fullSetBonusName = fullSetBonusName;
    }

    public String displayName() {
        return displayName;
    }

    public String fullSetBonusName() {
        return fullSetBonusName;
    }
}
