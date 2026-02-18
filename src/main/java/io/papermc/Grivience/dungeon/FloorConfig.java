package io.papermc.Grivience.dungeon;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class FloorConfig {
    private final String id;
    private final String displayName;
    private final int minPartySize;
    private final int maxPartySize;
    private final int combatRooms;
    private final int puzzleRooms;
    private final int treasureRooms;
    private final List<RoomType> puzzleTypes;
    private final int roomSize;
    private final int baseMobCount;
    private final double mobHealthMultiplier;
    private final int mobDamageTier;
    private final boolean folkloreMobs;
    private final List<YokaiType> yokaiPool;
    private final List<EntityType> mobPool;
    private final EntityType bossType;
    private final String bossName;
    private final double bossHealthMultiplier;
    private final Material floorMaterial;
    private final Material wallMaterial;
    private final Map<String, List<String>> rewardsByGrade;

    public FloorConfig(
            String id,
            String displayName,
            int minPartySize,
            int maxPartySize,
            int combatRooms,
            int puzzleRooms,
            int treasureRooms,
            List<RoomType> puzzleTypes,
            int roomSize,
            int baseMobCount,
            double mobHealthMultiplier,
            int mobDamageTier,
            boolean folkloreMobs,
            List<YokaiType> yokaiPool,
            List<EntityType> mobPool,
            EntityType bossType,
            String bossName,
            double bossHealthMultiplier,
            Material floorMaterial,
            Material wallMaterial,
            Map<String, List<String>> rewardsByGrade
    ) {
        this.id = id;
        this.displayName = displayName;
        this.minPartySize = minPartySize;
        this.maxPartySize = maxPartySize;
        this.combatRooms = combatRooms;
        this.puzzleRooms = puzzleRooms;
        this.treasureRooms = treasureRooms;
        this.puzzleTypes = List.copyOf(puzzleTypes);
        this.roomSize = roomSize;
        this.baseMobCount = baseMobCount;
        this.mobHealthMultiplier = mobHealthMultiplier;
        this.mobDamageTier = mobDamageTier;
        this.folkloreMobs = folkloreMobs;
        this.yokaiPool = List.copyOf(yokaiPool);
        this.mobPool = List.copyOf(mobPool);
        this.bossType = bossType;
        this.bossName = bossName;
        this.bossHealthMultiplier = bossHealthMultiplier;
        this.floorMaterial = floorMaterial;
        this.wallMaterial = wallMaterial;
        this.rewardsByGrade = Collections.unmodifiableMap(rewardsByGrade);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public int minPartySize() {
        return minPartySize;
    }

    public int maxPartySize() {
        return maxPartySize;
    }

    public int combatRooms() {
        return combatRooms;
    }

    public int puzzleRooms() {
        return puzzleRooms;
    }

    public int treasureRooms() {
        return treasureRooms;
    }

    public List<RoomType> puzzleTypes() {
        return puzzleTypes;
    }

    public int encounterRooms() {
        return combatRooms + puzzleRooms + treasureRooms;
    }

    public int roomSize() {
        return roomSize;
    }

    public int baseMobCount() {
        return baseMobCount;
    }

    public double mobHealthMultiplier() {
        return mobHealthMultiplier;
    }

    public int mobDamageTier() {
        return mobDamageTier;
    }

    public boolean folkloreMobs() {
        return folkloreMobs;
    }

    public List<YokaiType> yokaiPool() {
        return yokaiPool;
    }

    public List<EntityType> mobPool() {
        return mobPool;
    }

    public EntityType bossType() {
        return bossType;
    }

    public String bossName() {
        return bossName;
    }

    public double bossHealthMultiplier() {
        return bossHealthMultiplier;
    }

    public Material floorMaterial() {
        return floorMaterial;
    }

    public Material wallMaterial() {
        return wallMaterial;
    }

    public List<String> rewardsForGrade(String grade) {
        String normalized = grade.toUpperCase(Locale.ROOT);
        List<String> direct = rewardsByGrade.get(normalized);
        if (direct != null && !direct.isEmpty()) {
            return direct;
        }
        if (Objects.equals(normalized, "S")) {
            return rewardsByGrade.getOrDefault("A", List.of());
        }
        if (Objects.equals(normalized, "A")) {
            return rewardsByGrade.getOrDefault("B", List.of());
        }
        if (Objects.equals(normalized, "B")) {
            return rewardsByGrade.getOrDefault("C", List.of());
        }
        if (Objects.equals(normalized, "C")) {
            return rewardsByGrade.getOrDefault("D", List.of());
        }
        return rewardsByGrade.getOrDefault("D", List.of());
    }

    public static FloorConfig fromSection(String id, ConfigurationSection section) {
        String displayName = section.getString("display-name", id);
        int minPartySize = Math.max(1, section.getInt("min-party-size", 1));
        int maxPartySize = Math.max(minPartySize, section.getInt("max-party-size", 5));
        int combatRooms = Math.max(0, section.getInt("combat-rooms", 4));
        int puzzleRooms = Math.max(0, section.getInt("puzzle-rooms", 1));
        int treasureRooms = Math.max(0, section.getInt("treasure-rooms", 1));

        List<RoomType> puzzleTypes = new ArrayList<>();
        for (String rawType : section.getStringList("puzzle-types")) {
            RoomType parsed = parseRoomType(rawType);
            if (parsed != null && parsed.isPuzzle()) {
                puzzleTypes.add(parsed);
            }
        }
        if (puzzleTypes.isEmpty()) {
            puzzleTypes = List.of(RoomType.PUZZLE_SEQUENCE, RoomType.PUZZLE_SYNC);
        }

        int roomSize = Math.max(21, section.getInt("room-size", 23));
        int baseMobCount = Math.max(1, section.getInt("base-mob-count", 5));
        double mobHealthMultiplier = Math.max(0.1D, section.getDouble("mob-health-multiplier", 1.0D));
        int mobDamageTier = Math.max(0, section.getInt("mob-damage-tier", 0));
        boolean folkloreMobs = section.getBoolean("folklore-mobs.enabled", true);

        List<YokaiType> yokaiPool = new ArrayList<>();
        for (String yokaiName : section.getStringList("folklore-mobs.pool")) {
            YokaiType type = YokaiType.parse(yokaiName);
            if (type != null) {
                yokaiPool.add(type);
            }
        }
        if (yokaiPool.isEmpty()) {
            if (mobDamageTier >= 1) {
                yokaiPool = YokaiType.strongerPool();
            } else {
                yokaiPool = YokaiType.defaultPool();
            }
        }

        List<EntityType> mobPool = new ArrayList<>();
        for (String typeName : section.getStringList("mob-pool")) {
            EntityType type = parseEntityType(typeName);
            if (type != null && type.isAlive() && type.isSpawnable()) {
                mobPool.add(type);
            }
        }
        if (mobPool.isEmpty()) {
            mobPool = List.of(EntityType.ZOMBIE, EntityType.SKELETON, EntityType.HUSK);
        }

        EntityType bossType = parseEntityType(section.getString("boss-type", "WITHER_SKELETON"));
        if (bossType == null || !bossType.isAlive() || !bossType.isSpawnable()) {
            bossType = EntityType.WITHER_SKELETON;
        }

        String bossName = section.getString("boss-name", "Shogun Warden");
        double bossHealthMultiplier = Math.max(1.0D, section.getDouble("boss-health-multiplier", 6.0D));

        Material floorMaterial = parseMaterial(section.getString("floor-material", "POLISHED_DEEPSLATE"), Material.POLISHED_DEEPSLATE);
        Material wallMaterial = parseMaterial(section.getString("wall-material", "STONE_BRICKS"), Material.STONE_BRICKS);

        Map<String, List<String>> rewards = new LinkedHashMap<>();
        ConfigurationSection rewardSection = section.getConfigurationSection("rewards");
        if (rewardSection != null) {
            for (String grade : rewardSection.getKeys(false)) {
                List<String> commands = rewardSection.getStringList(grade);
                if (!commands.isEmpty()) {
                    rewards.put(grade.toUpperCase(Locale.ROOT), List.copyOf(commands));
                }
            }
        }

        return new FloorConfig(
                id.toUpperCase(Locale.ROOT),
                displayName,
                minPartySize,
                maxPartySize,
                combatRooms,
                puzzleRooms,
                treasureRooms,
                puzzleTypes,
                roomSize,
                baseMobCount,
                mobHealthMultiplier,
                mobDamageTier,
                folkloreMobs,
                yokaiPool,
                mobPool,
                bossType,
                bossName,
                bossHealthMultiplier,
                floorMaterial,
                wallMaterial,
                rewards
        );
    }

    private static EntityType parseEntityType(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        try {
            return EntityType.valueOf(input.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static Material parseMaterial(String input, Material fallback) {
        if (input == null || input.isBlank()) {
            return fallback;
        }
        Material parsed = Material.matchMaterial(input.trim().toUpperCase(Locale.ROOT));
        if (parsed == null || !parsed.isBlock()) {
            return fallback;
        }
        return parsed;
    }

    private static RoomType parseRoomType(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = input.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (Objects.equals(normalized, "SEQUENCE")) {
            return RoomType.PUZZLE_SEQUENCE;
        }
        if (Objects.equals(normalized, "SYNC")) {
            return RoomType.PUZZLE_SYNC;
        }
        try {
            return RoomType.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
