package io.papermc.Grivience.item;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

public enum CustomArmorType {
    SHOGUN_KABUTO(ArmorSetType.SHOGUN, ArmorWeightClass.HEAVY, "Frontline tanking and gate pressure control.", 22, 45, 4),
    SHOGUN_DO_MARU(ArmorSetType.SHOGUN, ArmorWeightClass.HEAVY, "Absorbs heavy blows while advancing.", 34, 80, 6),
    SHOGUN_HAIDATE(ArmorSetType.SHOGUN, ArmorWeightClass.HEAVY, "Stability under close-range swarms.", 28, 62, 5),
    SHOGUN_GETA(ArmorSetType.SHOGUN, ArmorWeightClass.HEAVY, "Anchors your stance in prolonged fights.", 18, 34, 3),

    SHINOBI_MENPO(ArmorSetType.SHINOBI, ArmorWeightClass.LIGHT, "Hit-and-run skirmishing and flanks.", 16, 28, 8),
    SHINOBI_JACKET(ArmorSetType.SHINOBI, ArmorWeightClass.LIGHT, "Fast rotations between room objectives.", 24, 56, 9),
    SHINOBI_LEGGINGS(ArmorSetType.SHINOBI, ArmorWeightClass.LIGHT, "Mobility-focused sustained tempo.", 20, 44, 8),
    SHINOBI_TABI(ArmorSetType.SHINOBI, ArmorWeightClass.LIGHT, "Sprint-and-reset survivability.", 14, 24, 10),

    ONMYOJI_EBOSHI(ArmorSetType.ONMYOJI, ArmorWeightClass.BALANCED, "Balanced sustain and ward support.", 18, 40, 6),
    ONMYOJI_ROBE(ArmorSetType.ONMYOJI, ArmorWeightClass.BALANCED, "Steady midline sustain under pressure.", 29, 74, 8),
    ONMYOJI_HAKAMA(ArmorSetType.ONMYOJI, ArmorWeightClass.BALANCED, "Balanced defense with stable recovery.", 24, 56, 7),
    ONMYOJI_SANDALS(ArmorSetType.ONMYOJI, ArmorWeightClass.BALANCED, "Reliable movement and sustain uptime.", 16, 30, 6),

    // Tank Sets - High Health, Defense, and Mana
    TITAN_HELM(ArmorSetType.TITAN, ArmorWeightClass.HEAVY, "Unbreakable frontline defense with massive health pool.", 45, 150, 8, 25),
    TITAN_CHESTPLATE(ArmorSetType.TITAN, ArmorWeightClass.HEAVY, "Absorbs devastating blows while protecting allies.", 68, 220, 12, 40),
    TITAN_LEGGINGS(ArmorSetType.TITAN, ArmorWeightClass.HEAVY, "Reinforced plating for sustained tanking.", 56, 180, 10, 30),
    TITAN_BOOTS(ArmorSetType.TITAN, ArmorWeightClass.HEAVY, "Grounded stance against overwhelming force.", 38, 120, 8, 20),

    LEVIATHAN_HELM(ArmorSetType.LEVIATHAN, ArmorWeightClass.HEAVY, "Deep sea armor with immense pressure resistance.", 42, 140, 15, 35),
    LEVIATHAN_CHESTPLATE(ArmorSetType.LEVIATHAN, ArmorWeightClass.HEAVY, "Abyssal scales that shrug off fatal strikes.", 64, 200, 20, 50),
    LEVIATHAN_LEGGINGS(ArmorSetType.LEVIATHAN, ArmorWeightClass.HEAVY, "Oceanic currents flow through reinforced greaves.", 52, 165, 18, 45),
    LEVIATHAN_BOOTS(ArmorSetType.LEVIATHAN, ArmorWeightClass.HEAVY, "Treads that command the tides themselves.", 35, 110, 14, 30),

    GUARDIAN_HELM(ArmorSetType.GUARDIAN, ArmorWeightClass.BALANCED, "Holy protection blessed by ancient guardians.", 38, 125, 20, 40),
    GUARDIAN_CHESTPLATE(ArmorSetType.GUARDIAN, ArmorWeightClass.BALANCED, "Radiant armor that shields the virtuous.", 58, 185, 30, 60),
    GUARDIAN_LEGGINGS(ArmorSetType.GUARDIAN, ArmorWeightClass.BALANCED, "Divine law etched into protective plates.", 48, 150, 25, 50),
    GUARDIAN_BOOTS(ArmorSetType.GUARDIAN, ArmorWeightClass.BALANCED, "Sacred steps that hallow the ground beneath.", 32, 100, 18, 35),
    
    // Miner's Set
    MINER_HELM(ArmorSetType.MINER, ArmorWeightClass.LIGHT, "+Mining Speed bonus.", 10, 20, 5),
    MINER_CHESTPLATE(ArmorSetType.MINER, ArmorWeightClass.LIGHT, "+Mining Speed bonus.", 15, 30, 8),
    MINER_LEGGINGS(ArmorSetType.MINER, ArmorWeightClass.LIGHT, "+Mining Speed bonus.", 12, 25, 6),
    MINER_BOOTS(ArmorSetType.MINER, ArmorWeightClass.LIGHT, "+Mining Speed bonus.", 8, 15, 4),

    // Ironcrest Guard Set
    IRONCREST_HELM(ArmorSetType.IRONCREST_GUARD, ArmorWeightClass.HEAVY, "+Defense in mine zones.", 25, 50, 10),
    IRONCREST_CHESTPLATE(ArmorSetType.IRONCREST_GUARD, ArmorWeightClass.HEAVY, "+Defense in mine zones.", 40, 80, 15),
    IRONCREST_LEGGINGS(ArmorSetType.IRONCREST_GUARD, ArmorWeightClass.HEAVY, "+Defense in mine zones.", 32, 65, 12),
    IRONCREST_BOOTS(ArmorSetType.IRONCREST_GUARD, ArmorWeightClass.HEAVY, "+Defense in mine zones.", 20, 40, 8),

    // Deepcore Set
    DEEPCORE_HELM(ArmorSetType.DEEPCORE, ArmorWeightClass.HEAVY, "Immunity to mining slow effects.", 35, 70, 12),
    DEEPCORE_CHESTPLATE(ArmorSetType.DEEPCORE, ArmorWeightClass.HEAVY, "Immunity to mining slow effects.", 55, 110, 18),
    DEEPCORE_LEGGINGS(ArmorSetType.DEEPCORE, ArmorWeightClass.HEAVY, "Immunity to mining slow effects.", 45, 90, 15),
    DEEPCORE_BOOTS(ArmorSetType.DEEPCORE, ArmorWeightClass.HEAVY, "Immunity to mining slow effects.", 30, 60, 10),

    // --- NEW DUNGEON SETS ---
    RONIN_HELM(ArmorSetType.RONIN, ArmorWeightClass.BALANCED, "Exiled warrior's headgear.", 15, 30, 5, 10),
    RONIN_CHESTPLATE(ArmorSetType.RONIN, ArmorWeightClass.BALANCED, "Tattered but resilient chestplate.", 25, 55, 8, 15),
    RONIN_LEGGINGS(ArmorSetType.RONIN, ArmorWeightClass.BALANCED, "Reinforced traveling trousers.", 20, 45, 6, 12),
    RONIN_BOOTS(ArmorSetType.RONIN, ArmorWeightClass.BALANCED, "Swift-step sandals.", 12, 25, 4, 8),

    KAPPA_GUARDIAN_HELM(ArmorSetType.KAPPA_GUARDIAN, ArmorWeightClass.HEAVY, "Water-infused defensive helm.", 28, 60, 12, 20),
    KAPPA_GUARDIAN_CHESTPLATE(ArmorSetType.KAPPA_GUARDIAN, ArmorWeightClass.HEAVY, "Shell-like heavy chestplate.", 42, 100, 18, 30),
    KAPPA_GUARDIAN_LEGGINGS(ArmorSetType.KAPPA_GUARDIAN, ArmorWeightClass.HEAVY, "Plated greaves of the river.", 34, 80, 15, 25),
    KAPPA_GUARDIAN_BOOTS(ArmorSetType.KAPPA_GUARDIAN, ArmorWeightClass.HEAVY, "Weighted boots for riverbeds.", 22, 50, 10, 15),

    TENGU_MASTER_HELM(ArmorSetType.TENGU_MASTER, ArmorWeightClass.LIGHT, "Feathered helm of the mountain lord.", 35, 85, 10, 50),
    TENGU_MASTER_CHESTPLATE(ArmorSetType.TENGU_MASTER, ArmorWeightClass.LIGHT, "Wind-resistant master's robe.", 55, 140, 15, 80),
    TENGU_MASTER_LEGGINGS(ArmorSetType.TENGU_MASTER, ArmorWeightClass.LIGHT, "Lightweight master's hakama.", 45, 110, 12, 60),
    TENGU_MASTER_BOOTS(ArmorSetType.TENGU_MASTER, ArmorWeightClass.LIGHT, "Boots that walk on the wind.", 28, 70, 8, 40),

    SKELETON_SOLDIER_HELM(ArmorSetType.SKELETON_SOLDIER, ArmorWeightClass.BALANCED, "Ancient bone helmet.", 10, 20, 2, 5),
    SKELETON_SOLDIER_CHESTPLATE(ArmorSetType.SKELETON_SOLDIER, ArmorWeightClass.BALANCED, "Ribbed bone chestplate.", 18, 40, 4, 10),
    SKELETON_SOLDIER_LEGGINGS(ArmorSetType.SKELETON_SOLDIER, ArmorWeightClass.BALANCED, "Femur-reinforced leggings.", 14, 32, 3, 8),
    SKELETON_SOLDIER_BOOTS(ArmorSetType.SKELETON_SOLDIER, ArmorWeightClass.BALANCED, "Swift calcified boots.", 8, 15, 2, 5);

    private static final EnumMap<ArmorSetType, List<CustomArmorType>> PIECES_BY_SET = new EnumMap<>(ArmorSetType.class);

    private final ArmorSetType setType;
    private final ArmorWeightClass weightClass;
    private final String gameplayDescription;
    private final int defense;
    private final int health;
    private final int healSpeed;
    private final int mana;

    static {
        for (ArmorSetType setType : ArmorSetType.values()) {
            PIECES_BY_SET.put(setType, new ArrayList<>());
        }
        for (CustomArmorType type : values()) {
            PIECES_BY_SET.get(type.setType()).add(type);
        }
        for (ArmorSetType setType : ArmorSetType.values()) {
            PIECES_BY_SET.put(setType, List.copyOf(PIECES_BY_SET.get(setType)));
        }
    }

    CustomArmorType(
            ArmorSetType setType,
            ArmorWeightClass weightClass,
            String gameplayDescription,
            int defense,
            int health,
            int healSpeed,
            int mana
    ) {
        this.setType = setType;
        this.weightClass = weightClass;
        this.gameplayDescription = gameplayDescription;
        this.defense = defense;
        this.health = health;
        this.healSpeed = healSpeed;
        this.mana = mana;
    }

    // Legacy constructor for existing armor sets (defaults mana to 0)
    CustomArmorType(
            ArmorSetType setType,
            ArmorWeightClass weightClass,
            String gameplayDescription,
            int defense,
            int health,
            int healSpeed
    ) {
        this(setType, weightClass, gameplayDescription, defense, health, healSpeed, 0);
    }

    public ArmorSetType setType() {
        return setType;
    }

    public int defense() {
        return defense;
    }

    public ArmorWeightClass weightClass() {
        return weightClass;
    }

    public String gameplayDescription() {
        return gameplayDescription;
    }

    public int health() {
        return health;
    }

    public int healSpeed() {
        return healSpeed;
    }

    public int mana() {
        return mana;
    }

    public static List<CustomArmorType> piecesForSet(ArmorSetType setType) {
        if (setType == null) {
            return List.of();
        }
        return PIECES_BY_SET.getOrDefault(setType, List.of());
    }

    public static CustomArmorType parse(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = input.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (CustomArmorType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
