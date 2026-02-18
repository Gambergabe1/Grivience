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
    ONMYOJI_SANDALS(ArmorSetType.ONMYOJI, ArmorWeightClass.BALANCED, "Reliable movement and sustain uptime.", 16, 30, 6);

    private static final EnumMap<ArmorSetType, List<CustomArmorType>> PIECES_BY_SET = new EnumMap<>(ArmorSetType.class);

    private final ArmorSetType setType;
    private final ArmorWeightClass weightClass;
    private final String gameplayDescription;
    private final int defense;
    private final int health;
    private final int healSpeed;

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
            int healSpeed
    ) {
        this.setType = setType;
        this.weightClass = weightClass;
        this.gameplayDescription = gameplayDescription;
        this.defense = defense;
        this.health = health;
        this.healSpeed = healSpeed;
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
