package io.papermc.Grivience.item;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

public enum ReforgeType {
    JAGGED(
            "Jagged",
            ChatColor.RED,
            true,
            stats(12.0D, 8.0D, 4.0D, 10.0D),
            stats(14.0D, 10.0D, 5.0D, 13.0D),
            stats(17.0D, 13.0D, 6.0D, 17.0D),
            stats(21.0D, 17.0D, 8.0D, 23.0D),
            stats(25.0D, 22.0D, 10.0D, 30.0D),
            stats(30.0D, 28.0D, 12.0D, 38.0D)
    ),
    TITANIC(
            "Titanic",
            ChatColor.GOLD,
            true,
            stats(6.0D, 28.0D, 0.0D, 10.0D),
            stats(7.0D, 32.0D, 0.0D, 12.0D),
            stats(9.0D, 38.0D, 0.0D, 16.0D),
            stats(12.0D, 46.0D, 1.0D, 22.0D),
            stats(15.0D, 55.0D, 2.0D, 29.0D),
            stats(19.0D, 68.0D, 3.0D, 37.0D)
    ),
    ARCANE(
            "Arcane",
            ChatColor.LIGHT_PURPLE,
            false,
            stats(0.0D, 7.0D, 7.0D, 20.0D),
            stats(0.0D, 9.0D, 8.0D, 24.0D),
            stats(2.0D, 12.0D, 10.0D, 30.0D),
            stats(4.0D, 16.0D, 12.0D, 38.0D),
            stats(6.0D, 21.0D, 14.0D, 48.0D),
            stats(8.0D, 28.0D, 17.0D, 60.0D)
    ),
    TEMPEST(
            "Tempest",
            ChatColor.AQUA,
            false,
            stats(8.0D, 14.0D, 5.0D, 12.0D),
            stats(10.0D, 17.0D, 6.0D, 15.0D),
            stats(12.0D, 22.0D, 8.0D, 20.0D),
            stats(16.0D, 28.0D, 10.0D, 26.0D),
            stats(21.0D, 35.0D, 12.0D, 33.0D),
            stats(27.0D, 44.0D, 15.0D, 42.0D)
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
            double critDamageBonus
    ) {
        return new ReforgeStatProfile(damageBonus, strengthBonus, critChanceBonus, critDamageBonus);
    }

    public record ReforgeStatProfile(
            double damageBonus,
            double strengthBonus,
            double critChanceBonus,
            double critDamageBonus
    ) {
    }
}
