package io.papermc.Grivience.item;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

public enum ReforgeType {
    GENTLE(
            "Gentle",
            ChatColor.GREEN,
            true,
            stats(0, 3, 0, 0, 8, 0),
            stats(0, 5, 0, 0, 10, 0),
            stats(0, 7, 0, 0, 15, 0),
            stats(0, 10, 0, 0, 20, 0),
            stats(0, 15, 0, 0, 25, 0),
            stats(0, 20, 0, 0, 30, 0)
    ),
    ODD(
            "Odd",
            ChatColor.YELLOW,
            true,
            stats(0, 0, 12, 10, 0, -5),
            stats(0, 0, 15, 15, 0, -10),
            stats(0, 0, 15, 15, 0, -18),
            stats(0, 0, 20, 22, 0, -32),
            stats(0, 0, 25, 30, 0, -50),
            stats(0, 0, 30, 40, 0, -75)
    ),
    FAST(
            "Fast",
            ChatColor.AQUA,
            true,
            stats(0, 0, 0, 0, 10, 0),
            stats(0, 0, 0, 0, 20, 0),
            stats(0, 0, 0, 0, 30, 0),
            stats(0, 0, 0, 0, 40, 0),
            stats(0, 0, 0, 0, 50, 0),
            stats(0, 0, 0, 0, 60, 0)
    ),
    FAIR(
            "Fair",
            ChatColor.LIGHT_PURPLE,
            true,
            stats(0, 2, 2, 2, 2, 2),
            stats(0, 3, 3, 3, 3, 3),
            stats(0, 4, 4, 4, 4, 4),
            stats(0, 7, 7, 7, 7, 7),
            stats(0, 10, 10, 10, 10, 10),
            stats(0, 12, 12, 12, 12, 12)
    ),
    EPIC(
            "Epic",
            ChatColor.DARK_PURPLE,
            true,
            stats(0, 15, 0, 10, 1, 0),
            stats(0, 20, 0, 15, 2, 0),
            stats(0, 25, 0, 20, 4, 0),
            stats(0, 32, 0, 27, 7, 0),
            stats(0, 40, 0, 35, 10, 0),
            stats(0, 50, 0, 45, 15, 0)
    ),
    SHARP(
            "Sharp",
            ChatColor.BLUE,
            true,
            stats(0, 0, 10, 20, 0, 0),
            stats(0, 0, 12, 30, 0, 0),
            stats(0, 0, 14, 40, 0, 0),
            stats(0, 0, 17, 55, 0, 0),
            stats(0, 0, 20, 75, 0, 0),
            stats(0, 0, 25, 90, 0, 0)
    ),
    HEROIC(
            "Heroic",
            ChatColor.DARK_AQUA,
            true,
            stats(0, 15, 0, 0, 1, 40),
            stats(0, 20, 0, 0, 2, 50),
            stats(0, 25, 0, 0, 2, 65),
            stats(0, 32, 0, 0, 3, 80),
            stats(0, 40, 0, 0, 5, 100),
            stats(0, 50, 0, 0, 7, 125)
    ),
    SPICY(
            "Spicy",
            ChatColor.RED,
            true,
            stats(0, 2, 1, 25, 1, 0),
            stats(0, 3, 1, 35, 2, 0),
            stats(0, 4, 1, 45, 4, 0),
            stats(0, 7, 1, 60, 7, 0),
            stats(0, 10, 1, 80, 10, 0),
            stats(0, 12, 1, 100, 15, 0)
    ),
    LEGENDARY(
            "Legendary",
            ChatColor.GOLD,
            true,
            stats(0, 3, 5, 5, 2, 5),
            stats(0, 7, 7, 10, 3, 8),
            stats(0, 12, 9, 15, 5, 12),
            stats(0, 18, 12, 22, 7, 18),
            stats(0, 25, 15, 28, 10, 25),
            stats(0, 32, 18, 36, 15, 35)
    );

    private final String displayName;
    private final ChatColor color;
    private final boolean blacksmithEligible;
    private final EnumMap<ItemRarity, ReforgeStatProfile> statByRarity;

    ReforgeType(
            String displayName,
            ChatColor color,
            boolean blacksmithEligible,
            ReforgeStatProfile common,
            ReforgeStatProfile uncommon,
            ReforgeStatProfile rare,
            ReforgeStatProfile epic,
            ReforgeStatProfile legendary,
            ReforgeStatProfile mythic
    ) {
        this.displayName = displayName;
        this.color = color;
        this.blacksmithEligible = blacksmithEligible;
        this.statByRarity = new EnumMap<>(ItemRarity.class);
        this.statByRarity.put(ItemRarity.COMMON, common);
        this.statByRarity.put(ItemRarity.UNCOMMON, uncommon);
        this.statByRarity.put(ItemRarity.RARE, rare);
        this.statByRarity.put(ItemRarity.EPIC, epic);
        this.statByRarity.put(ItemRarity.LEGENDARY, legendary);
        this.statByRarity.put(ItemRarity.MYTHIC, mythic);
    }

    public String displayName() {
        return displayName;
    }

    public ChatColor color() {
        return color;
    }

    public boolean blacksmithEligible() {
        return blacksmithEligible;
    }

    public ReforgeStatProfile statsFor(ItemRarity rarity) {
        ItemRarity effectiveRarity = rarity == null ? ItemRarity.RARE : rarity;
        return statByRarity.getOrDefault(effectiveRarity, statByRarity.get(ItemRarity.RARE));
    }

    public static List<ReforgeType> blacksmithPool() {
        List<ReforgeType> types = new ArrayList<>();
        for (ReforgeType type : values()) {
            if (type.blacksmithEligible()) {
                types.add(type);
            }
        }
        return types;
    }

    public static ReforgeType parse(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = input.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        for (ReforgeType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return null;
    }

    private static ReforgeStatProfile stats(
            double damageBonus,
            double strengthBonus,
            double critChanceBonus,
            double critDamageBonus,
            double attackSpeedBonus,
            double intelligenceBonus
    ) {
        return new ReforgeStatProfile(
                damageBonus,
                strengthBonus,
                critChanceBonus,
                critDamageBonus,
                attackSpeedBonus,
                intelligenceBonus
        );
    }

    public record ReforgeStatProfile(
            double damageBonus,
            double strengthBonus,
            double critChanceBonus,
            double critDamageBonus,
            double attackSpeedBonus,
            double intelligenceBonus
    ) {
    }
}
