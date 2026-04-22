package io.papermc.Grivience.farming;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

public final class FarmingContestRules {
    private FarmingContestRules() {
    }

    public enum Bracket {
        NONE(0, ChatColor.DARK_GRAY, "None"),
        PARTICIPATION(1, ChatColor.WHITE, "Participation"),
        BRONZE(2, ChatColor.GOLD, "Bronze"),
        SILVER(3, ChatColor.GRAY, "Silver"),
        GOLD(4, ChatColor.YELLOW, "Gold"),
        PLATINUM(5, ChatColor.AQUA, "Platinum"),
        DIAMOND(6, ChatColor.AQUA, "Diamond");

        private final int tier;
        private final ChatColor color;
        private final String displayName;

        Bracket(int tier, ChatColor color, String displayName) {
            this.tier = tier;
            this.color = color;
            this.displayName = displayName;
        }

        public int tier() {
            return tier;
        }

        public String coloredName() {
            return color + displayName;
        }
    }

    public static List<FarmingContestCrop> selectContestCrops(List<FarmingContestCrop> pool, long seed, long slotIndex, int count) {
        List<FarmingContestCrop> source = new ArrayList<>();
        if (pool != null && !pool.isEmpty()) {
            source.addAll(pool);
        } else {
            Collections.addAll(source, FarmingContestCrop.values());
        }

        if (source.isEmpty()) return Collections.emptyList();

        int size = source.size();
        List<FarmingContestCrop> selected = new ArrayList<>();
        
        // Use a consistent deterministic selection based on slotIndex
        for (int i = 0; i < count; i++) {
            int index = (int) ((slotIndex * count + i) % size);
            selected.add(source.get(index));
        }
        
        return List.copyOf(selected);
    }

    public static Bracket resolveBracket(
            int participantCount,
            int placement,
            double bronzePercent,
            double silverPercent,
            double goldPercent,
            double platinumPercent,
            double diamondPercent
    ) {
        if (participantCount <= 0 || placement <= 0 || placement > participantCount) {
            return Bracket.NONE;
        }

        if (placement <= qualifyingCount(participantCount, diamondPercent)) {
            return Bracket.DIAMOND;
        }
        if (placement <= qualifyingCount(participantCount, platinumPercent)) {
            return Bracket.PLATINUM;
        }
        if (placement <= qualifyingCount(participantCount, goldPercent)) {
            return Bracket.GOLD;
        }
        if (placement <= qualifyingCount(participantCount, silverPercent)) {
            return Bracket.SILVER;
        }
        if (placement <= qualifyingCount(participantCount, bronzePercent)) {
            return Bracket.BRONZE;
        }
        return Bracket.NONE;
    }

    private static int qualifyingCount(int participantCount, double percent) {
        if (participantCount <= 0 || percent <= 0.0D) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(participantCount * percent));
    }

    private static long mixSeed(long seed, long slotIndex) {
        long mixed = seed ^ (slotIndex * 0x9E3779B97F4A7C15L);
        mixed ^= (mixed >>> 33);
        mixed *= 0xff51afd7ed558ccdL;
        mixed ^= (mixed >>> 33);
        mixed *= 0xc4ceb9fe1a85ec53L;
        mixed ^= (mixed >>> 33);
        return mixed;
    }
}
