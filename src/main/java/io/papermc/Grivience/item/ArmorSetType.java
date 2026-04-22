package io.papermc.Grivience.item;

public enum ArmorSetType {
    SHOGUN("Shogun", "Warlord's Resolve"),
    SHINOBI("Shinobi", "Shadowstep"),
    ONMYOJI("Onmyoji", "Spirit Ward"),
    TITAN("Titan", "Colossal Barrier"),
    LEVIATHAN("Leviathan", "Abyssal Bulwark"),
    GUARDIAN("Guardian", "Divine Protection"),
    MINER("Miner", "Double Drop Chance"),
    IRONCREST_GUARD("Ironcrest Guard", "Mine Defense"),
    DEEPCORE("Deepcore", "Instant Break"),
    RONIN("Ronin", "Way of the Blade"),
    KAPPA_GUARDIAN("Kappa Guardian", "Hydraulic Pressure"),
    TENGU_MASTER("Tengu Master", "High Altitude"),
    SKELETON_SOLDIER("Skeleton Soldier", "Undead Soul"),
    DRAGON_SLAYER("Dragon Slayer", "Dragon's Dominion"),
    GHOUL_OVERSEER("Ghoul Overseer", "Undead Command"),
    GILDED_HARVESTER("Gilded Harvester", "Bountiful Harvest"),
    DREADLORD("Dreadlord", "Dread Aura"),
    NECROMANCER("Necromancer", "Undead Commander"),
    SOULBOUND_MAGE("Soulbound Mage", "Pact of the Forbidden"),
    ROOKIE_SAMURAI("Rookie Samurai", "Bushido Spirit");

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
