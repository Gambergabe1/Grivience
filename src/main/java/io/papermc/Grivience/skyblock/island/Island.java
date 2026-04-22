package io.papermc.Grivience.skyblock.island;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class Island implements ConfigurationSerializable {
    public enum VisitPolicy {
        OFF,
        ANYONE,
        FRIENDS,
        GUILD
    }

    public static final class VisitorStats {
        private long count;
        private long lastVisitedAt;
        private String lastKnownName;

        public VisitorStats(long count, long lastVisitedAt, String lastKnownName) {
            this.count = Math.max(0L, count);
            this.lastVisitedAt = Math.max(0L, lastVisitedAt);
            this.lastKnownName = lastKnownName;
        }

        public long getCount() {
            return count;
        }

        public long getLastVisitedAt() {
            return lastVisitedAt;
        }

        public String getLastKnownName() {
            return lastKnownName;
        }

        public void recordVisit(long now, String name) {
            if (count < Long.MAX_VALUE) {
                count++;
            }
            lastVisitedAt = Math.max(0L, now);
            if (name != null && !name.isBlank()) {
                lastKnownName = name;
            }
        }
    }

    private final UUID id;
    private final UUID owner;
    private UUID profileId;
    private Location center;
    private Location spawnPoint;
    private Location guestSpawnPoint;
    private int level;
    private int size;
    private int memberLimitUpgrade;
    private int guestLimitUpgrade;
    private int minionLimitUpgrade;
    private int endMinesLuckUpgrade;
    private int mobSpawnUpgrade;
    private int bankInterestUpgrade;
    private int bazaarFlipperUpgrade;
    private int islandSpeedUpgrade;
    private int magicFindUpgrade;
    private int petLuckUpgrade;
    private String name;
    private String profileName;
    private String description;
    private long createdAt;
    private long lastVisited;
    private VisitPolicy visitPolicy;
    private int guestLimit;
    private Map<String, VisitorStats> visits;
    private long totalVisits;
    private Set<UUID> members;

    public Island(UUID owner, String ownerName, Location center) {
        this.id = UUID.randomUUID();
        this.owner = owner;
        this.profileId = null;
        this.center = center;
        this.spawnPoint = null;
        this.guestSpawnPoint = null;
        this.level = 1;
        this.size = 32;
        this.memberLimitUpgrade = 0;
        this.guestLimitUpgrade = 0;
        this.minionLimitUpgrade = 0;
        this.name = ownerName + "'s Island";
        this.profileName = "Default";
        this.description = "A humble island home.";
        this.createdAt = System.currentTimeMillis();
        this.lastVisited = System.currentTimeMillis();
        this.visitPolicy = VisitPolicy.OFF;
        this.guestLimit = 1;
        this.visits = new HashMap<>();
        this.totalVisits = 0L;
        this.members = new HashSet<>();
    }

    public Island(Map<String, Object> data) {
        this.id = UUID.fromString((String) data.get("id"));
        this.owner = UUID.fromString((String) data.get("owner"));
        this.profileId = null;
        Object profileIdObj = data.get("profileId");
        if (profileIdObj == null) {
            profileIdObj = data.get("profile-id");
        }
        if (profileIdObj != null) {
            try {
                this.profileId = UUID.fromString(profileIdObj.toString());
            } catch (Exception ignored) {
            }
        }
        this.level = (int) data.getOrDefault("level", 1);
        this.size = (int) data.getOrDefault("size", 32);
        this.memberLimitUpgrade = ((Number) data.getOrDefault("memberLimitUpgrade", data.getOrDefault("member-limit-upgrade", 0))).intValue();
        this.guestLimitUpgrade = ((Number) data.getOrDefault("guestLimitUpgrade", data.getOrDefault("guest-limit-upgrade", 0))).intValue();
        this.minionLimitUpgrade = ((Number) data.getOrDefault("minionLimitUpgrade", data.getOrDefault("minion-limit-upgrade", 0))).intValue();
        this.endMinesLuckUpgrade = ((Number) data.getOrDefault("endMinesLuckUpgrade", data.getOrDefault("end-mines-luck-upgrade", 0))).intValue();
        this.mobSpawnUpgrade = ((Number) data.getOrDefault("mobSpawnUpgrade", data.getOrDefault("mob-spawn-upgrade", 0))).intValue();
        this.bankInterestUpgrade = ((Number) data.getOrDefault("bankInterestUpgrade", data.getOrDefault("bank-interest-upgrade", 0))).intValue();
        this.bazaarFlipperUpgrade = ((Number) data.getOrDefault("bazaarFlipperUpgrade", data.getOrDefault("bazaar-flipper-upgrade", 0))).intValue();
        this.islandSpeedUpgrade = ((Number) data.getOrDefault("islandSpeedUpgrade", data.getOrDefault("island-speed-upgrade", 0))).intValue();
        this.magicFindUpgrade = ((Number) data.getOrDefault("magicFindUpgrade", data.getOrDefault("magic-find-upgrade", 0))).intValue();
        this.petLuckUpgrade = ((Number) data.getOrDefault("petLuckUpgrade", data.getOrDefault("pet-luck-upgrade", 0))).intValue();
        this.name = (String) data.getOrDefault("name", "Unknown Island");
        this.profileName = (String) data.getOrDefault("profileName", "Default");
        this.description = (String) data.getOrDefault("description", "");
        this.createdAt = (long) data.getOrDefault("createdAt", 0L);
        this.lastVisited = (long) data.getOrDefault("lastVisited", 0L);
        this.totalVisits = ((Number) data.getOrDefault("totalVisits", 0L)).longValue();
        this.members = new HashSet<>();

        this.visitPolicy = VisitPolicy.OFF;
        Object policyObj = data.get("visitPolicy");
        if (policyObj == null) {
            policyObj = data.get("visit-policy");
        }
        if (policyObj != null) {
            try {
                this.visitPolicy = VisitPolicy.valueOf(policyObj.toString().trim().toUpperCase());
            } catch (Exception ignored) {
            }
        }

        this.guestLimit = ((Number) data.getOrDefault("guestLimit", data.getOrDefault("guest-limit", 1))).intValue();
        if (guestLimit == 0) {
            guestLimit = 1;
        }

        this.visits = new HashMap<>();
        Object visitsObj = data.get("visits");
        if (visitsObj instanceof ConfigurationSection visitsSection) {
            for (String key : visitsSection.getKeys(false)) {
                if (visitsSection.isConfigurationSection(key)) {
                    ConfigurationSection entry = visitsSection.getConfigurationSection(key);
                    if (entry == null) {
                        continue;
                    }
                    long count = entry.getLong("count", 0L);
                    long last = entry.getLong("last", entry.getLong("lastVisitedAt", 0L));
                    String name = entry.getString("name", key);
                    visits.put(key.toLowerCase(), new VisitorStats(count, last, name));
                    continue;
                }

                // Legacy format: visits.<nameLower> = <timestamp>. Migrate to count=1.
                long legacyLast = visitsSection.getLong(key, 0L);
                visits.put(key.toLowerCase(), new VisitorStats(legacyLast > 0L ? 1L : 0L, legacyLast, key));
            }
        }
        Object membersObj = data.get("members");
        if (membersObj instanceof List<?> list) {
            for (Object o : list) {
                try {
                    members.add(UUID.fromString(o.toString()));
                } catch (Exception ignored) {
                }
            }
        }

        String worldName = (String) data.get("world");
        double x = (double) data.get("x");
        double y = (double) data.get("y");
        double z = (double) data.get("z");
        World world = org.bukkit.Bukkit.getWorld(worldName);
        if (world != null) {
            this.center = new Location(world, x, y, z);
        }

        if (data.containsKey("spawnX") && data.containsKey("spawnY") && data.containsKey("spawnZ")) {
            double spawnX = (double) data.get("spawnX");
            double spawnY = (double) data.get("spawnY");
            double spawnZ = (double) data.get("spawnZ");
            float spawnYaw = (float) (double) data.getOrDefault("spawnYaw", 0.0f);
            float spawnPitch = (float) (double) data.getOrDefault("spawnPitch", 0.0f);
            if (world != null) {
                this.spawnPoint = new Location(world, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
            }
        }

        if (data.containsKey("guestSpawnX") && data.containsKey("guestSpawnY") && data.containsKey("guestSpawnZ")) {
            double spawnX = (double) data.get("guestSpawnX");
            double spawnY = (double) data.get("guestSpawnY");
            double spawnZ = (double) data.get("guestSpawnZ");
            float spawnYaw = (float) (double) data.getOrDefault("guestSpawnYaw", 0.0f);
            float spawnPitch = (float) (double) data.getOrDefault("guestSpawnPitch", 0.0f);
            if (world != null) {
                this.guestSpawnPoint = new Location(world, spawnX, spawnY, spawnZ, spawnYaw, spawnPitch);
            }
        }
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", id.toString());
        data.put("owner", owner.toString());
        if (profileId != null) {
            data.put("profileId", profileId.toString());
        }
        data.put("level", level);
        data.put("size", size);
        data.put("memberLimitUpgrade", memberLimitUpgrade);
        data.put("guestLimitUpgrade", guestLimitUpgrade);
        data.put("minionLimitUpgrade", minionLimitUpgrade);
        data.put("endMinesLuckUpgrade", endMinesLuckUpgrade);
        data.put("mobSpawnUpgrade", mobSpawnUpgrade);
        data.put("bankInterestUpgrade", bankInterestUpgrade);
        data.put("bazaarFlipperUpgrade", bazaarFlipperUpgrade);
        data.put("islandSpeedUpgrade", islandSpeedUpgrade);
        data.put("magicFindUpgrade", magicFindUpgrade);
        data.put("petLuckUpgrade", petLuckUpgrade);
        data.put("name", name);
        data.put("profileName", profileName);
        data.put("description", description);
        data.put("createdAt", createdAt);
        data.put("lastVisited", lastVisited);
        data.put("totalVisits", totalVisits);
        data.put("visitPolicy", visitPolicy != null ? visitPolicy.name() : VisitPolicy.OFF.name());
        data.put("guestLimit", guestLimit);

        if (!visits.isEmpty()) {
            Map<String, Object> visitsMap = new HashMap<>();
            for (Map.Entry<String, VisitorStats> entry : visits.entrySet()) {
                VisitorStats stats = entry.getValue();
                if (stats == null) {
                    continue;
                }
                Map<String, Object> statsMap = new HashMap<>();
                statsMap.put("name", stats.getLastKnownName());
                statsMap.put("count", stats.getCount());
                statsMap.put("last", stats.getLastVisitedAt());
                visitsMap.put(entry.getKey(), statsMap);
            }
            data.put("visits", visitsMap);
        }

        if (center != null) {
            data.put("world", center.getWorld().getName());
            data.put("x", center.getX());
            data.put("y", center.getY());
            data.put("z", center.getZ());
        }

        if (spawnPoint != null) {
            data.put("spawnX", spawnPoint.getX());
            data.put("spawnY", spawnPoint.getY());
            data.put("spawnZ", spawnPoint.getZ());
            data.put("spawnYaw", spawnPoint.getYaw());
            data.put("spawnPitch", spawnPoint.getPitch());
        }

        if (guestSpawnPoint != null) {
            data.put("guestSpawnX", guestSpawnPoint.getX());
            data.put("guestSpawnY", guestSpawnPoint.getY());
            data.put("guestSpawnZ", guestSpawnPoint.getZ());
            data.put("guestSpawnYaw", guestSpawnPoint.getYaw());
            data.put("guestSpawnPitch", guestSpawnPoint.getPitch());
        }
        if (members != null && !members.isEmpty()) {
            List<String> memberList = new java.util.ArrayList<>();
            for (UUID uuid : members) {
                memberList.add(uuid.toString());
            }
            data.put("members", memberList);
        }

        return data;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public UUID getProfileId() {
        return profileId;
    }

    public Location getCenter() {
        return center;
    }

    public Location getSpawnPoint() {
        if (spawnPoint != null) {
            return spawnPoint.clone().add(0.5, 1, 0.5);
        }
        if (center != null) {
            return center.clone().add(0.5, 1, 0.5);
        }
        return null;
    }

    public Location getGuestSpawnPoint() {
        if (guestSpawnPoint != null) {
            return guestSpawnPoint.clone().add(0.5, 1, 0.5);
        }
        return getSpawnPoint();
    }

    /**
     * Internal raw spawn location without menu/teleport offset adjustments.
     */
    public Location getRawSpawnPoint() {
        return spawnPoint == null ? null : spawnPoint.clone();
    }

    /**
     * Internal raw guest spawn location without menu/teleport offset adjustments.
     */
    public Location getRawGuestSpawnPoint() {
        return guestSpawnPoint == null ? null : guestSpawnPoint.clone();
    }

    public int getLevel() {
        return level;
    }

    public int getSize() {
        return size;
    }

    public String getName() {
        return name;
    }

    public String getProfileName() {
        return profileName;
    }

    public String getDescription() {
        return description;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastVisited() {
        return lastVisited;
    }

    public void setCenter(Location center) {
        this.center = center;
    }

    public void setSpawnPoint(Location spawnPoint) {
        this.spawnPoint = spawnPoint.clone();
        this.spawnPoint.setYaw(0);
        this.spawnPoint.setPitch(0);
    }

    public void setGuestSpawnPoint(Location guestSpawnPoint) {
        if (guestSpawnPoint == null) {
            this.guestSpawnPoint = null;
            return;
        }
        this.guestSpawnPoint = guestSpawnPoint.clone();
        this.guestSpawnPoint.setYaw(0);
        this.guestSpawnPoint.setPitch(0);
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getMemberLimitUpgrade() {
        return memberLimitUpgrade;
    }

    public void setMemberLimitUpgrade(int memberLimitUpgrade) {
        this.memberLimitUpgrade = memberLimitUpgrade;
    }

    public int getGuestLimitUpgrade() {
        return guestLimitUpgrade;
    }

    public void setGuestLimitUpgrade(int guestLimitUpgrade) {
        this.guestLimitUpgrade = guestLimitUpgrade;
    }

    public int getMinionLimitUpgrade() {
        return minionLimitUpgrade;
    }

    public int getEndMinesLuckUpgrade() {
        return endMinesLuckUpgrade;
    }

    public void setEndMinesLuckUpgrade(int endMinesLuckUpgrade) {
        this.endMinesLuckUpgrade = endMinesLuckUpgrade;
    }

    public int getMobSpawnUpgrade() {
        return mobSpawnUpgrade;
    }

    public void setMobSpawnUpgrade(int mobSpawnUpgrade) {
        this.mobSpawnUpgrade = mobSpawnUpgrade;
    }

    public int getBankInterestUpgrade() {
        return bankInterestUpgrade;
    }

    public void setBankInterestUpgrade(int bankInterestUpgrade) {
        this.bankInterestUpgrade = bankInterestUpgrade;
    }

    public int getBazaarFlipperUpgrade() {
        return bazaarFlipperUpgrade;
    }

    public void setBazaarFlipperUpgrade(int bazaarFlipperUpgrade) {
        this.bazaarFlipperUpgrade = bazaarFlipperUpgrade;
    }

    public int getIslandSpeedUpgrade() {
        return islandSpeedUpgrade;
    }

    public void setIslandSpeedUpgrade(int islandSpeedUpgrade) {
        this.islandSpeedUpgrade = islandSpeedUpgrade;
    }

    public int getMagicFindUpgrade() {
        return magicFindUpgrade;
    }

    public void setMagicFindUpgrade(int magicFindUpgrade) {
        this.magicFindUpgrade = magicFindUpgrade;
    }

    public int getPetLuckUpgrade() {
        return petLuckUpgrade;
    }

    public void setPetLuckUpgrade(int petLuckUpgrade) {
        this.petLuckUpgrade = petLuckUpgrade;
    }

    public int getMaxMembers() {
        return 2 + memberLimitUpgrade;
    }

    public void setMinionLimitUpgrade(int minionLimitUpgrade) {
        this.minionLimitUpgrade = minionLimitUpgrade;
    }

    public Set<UUID> getMembers() {
        return Set.copyOf(members);
    }

    public void addMember(UUID uuid) {
        if (uuid != null) {
            members.add(uuid);
        }
    }

    public void removeMember(UUID uuid) {
        if (uuid != null) {
            members.remove(uuid);
        }
    }

    public boolean isMember(UUID uuid) {
        return uuid != null && members.contains(uuid);
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public void setProfileId(UUID profileId) {
        this.profileId = profileId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLastVisited(long lastVisited) {
        this.lastVisited = lastVisited;
    }

    public VisitPolicy getVisitPolicy() {
        return visitPolicy != null ? visitPolicy : VisitPolicy.OFF;
    }

    public void setVisitPolicy(VisitPolicy visitPolicy) {
        this.visitPolicy = visitPolicy != null ? visitPolicy : VisitPolicy.OFF;
    }

    public int getGuestLimit() {
        return guestLimit;
    }

    /**
     * @param guestLimit Maximum guests allowed on the island at once.
     *                  Use -1 for unlimited. Values 0/invalid are coerced to 1.
     */
    public void setGuestLimit(int guestLimit) {
        if (guestLimit == 0) {
            this.guestLimit = 1;
            return;
        }
        this.guestLimit = guestLimit;
    }

    public Location getMinCorner() {
        if (center == null) return null;
        int halfSize = size / 2;
        return center.clone().subtract(halfSize, 0, halfSize);
    }

    public Location getMaxCorner() {
        if (center == null) return null;
        int halfSize = size / 2;
        return center.clone().add(halfSize, 256, halfSize);
    }

    public boolean isWithinIsland(Location location) {
        if (center == null || location == null) return false;
        if (!center.getWorld().getName().equalsIgnoreCase(location.getWorld().getName())) return false;

        // Island boundaries should be based on block coordinates to be consistent
        int halfSize = size / 2;
        int centerX = center.getBlockX();
        int centerZ = center.getBlockZ();
        
        int dx = Math.abs(location.getBlockX() - centerX);
        int dz = Math.abs(location.getBlockZ() - centerZ);

        return dx <= halfSize && dz <= halfSize;
    }

    public int getUpgradeLevel() {
        return (size - 32) / 16 + 1;
    }

    public void addVisit(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return;
        }
        String key = playerName.toLowerCase();
        long now = System.currentTimeMillis();
        VisitorStats stats = visits.computeIfAbsent(key, ignored -> new VisitorStats(0L, 0L, playerName));
        stats.recordVisit(now, playerName);
        if (totalVisits < Long.MAX_VALUE) {
            totalVisits++;
        }
        lastVisited = now;
    }

    public long getVisitCount(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            return 0L;
        }
        VisitorStats stats = visits.get(playerName.toLowerCase());
        return stats != null ? stats.getCount() : 0L;
    }

    public long getTotalVisits() {
        return totalVisits;
    }

    public Map<String, VisitorStats> getVisits() {
        return new HashMap<>(visits);
    }

    public List<String> getRecentVisitors(int limit) {
        return visits.entrySet().stream()
                .sorted((a, b) -> Long.compare(
                        b.getValue() != null ? b.getValue().getLastVisitedAt() : 0L,
                        a.getValue() != null ? a.getValue().getLastVisitedAt() : 0L
                ))
                .limit(limit)
                .map(entry -> {
                    VisitorStats stats = entry.getValue();
                    if (stats == null) {
                        return entry.getKey();
                    }
                    String name = stats.getLastKnownName();
                    return name != null && !name.isBlank() ? name : entry.getKey();
                })
                .toList();
    }
}
