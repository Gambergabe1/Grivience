package io.papermc.Grivience.skyblock.profile;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Represents a Skyblock-accurate player profile.
 * 
 * Features:
 * - Multiple profiles per player (like Skyblock)
 * - Profile-specific islands, inventories, collections, skills
 * - Profile banking, wardrobe, pets, quests
 * - Profile creation time, last save, playtime tracking
 * - Profile name, icon, description
 * - Profile bank, purse, museum
 * - Collection progress per profile
 * - Skill levels per profile
 */
public final class SkyBlockProfile {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Profile identification
    private final UUID profileId;
    private final UUID ownerId;
    private String profileName;
    private String profileIcon;
    private String description;
    
    // Profile metadata
    private final long createdAt;
    private long lastSaveTime;
    private long totalPlaytime;
    private boolean selected;
    
    // Profile economy
    private double purse;
    private double bankBalance;
    private double museumValue;
    
    // Profile progression
    private Map<String, Integer> skillLevels;
    private Map<String, Integer> collectionLevels;
    private Set<String> unlockedRecipes;
    private Set<String> completedQuests;
    
    // Profile inventory data (serialized)
    private String inventoryData;
    private String armorData;
    private String wardrobeData;
    private String enderChestData;
    
    // Profile pets
    private String equippedPet;
    private Map<String, PetData> petData;
    
    // Profile statistics
    private int deaths;
    private int kills;
    private int coinsEarned;
    private int coinsSpent;
    private int itemsFished;
    private int dungeonsCompleted;
    
    // Profile settings
    private boolean autoRecombobulator;
    private boolean showFashionPoints;
    
    public SkyBlockProfile(UUID ownerId, String profileName) {
        this(UUID.randomUUID(), ownerId, profileName, System.currentTimeMillis());
    }

    private SkyBlockProfile(UUID profileId, UUID ownerId, String profileName, long createdAt) {
        this.profileId = profileId != null ? profileId : UUID.randomUUID();
        this.ownerId = ownerId;
        String ownerSuffix = ownerId == null ? "unknown" : ownerId.toString().substring(0, 8);
        this.profileName = profileName != null ? profileName : "Profile " + ownerSuffix;
        this.profileIcon = "IRON_HELMET"; // Default icon
        this.description = "";
        this.createdAt = createdAt > 0L ? createdAt : System.currentTimeMillis();
        this.lastSaveTime = System.currentTimeMillis();
        this.totalPlaytime = 0;
        this.selected = false;
        this.purse = 0.0;
        this.bankBalance = 0.0;
        this.museumValue = 0.0;
        this.skillLevels = new HashMap<>();
        this.collectionLevels = new HashMap<>();
        this.unlockedRecipes = new HashSet<>();
        this.completedQuests = new HashSet<>();
        this.inventoryData = "";
        this.armorData = "";
        this.wardrobeData = "";
        this.enderChestData = "";
        this.equippedPet = null;
        this.petData = new HashMap<>();
        this.deaths = 0;
        this.kills = 0;
        this.coinsEarned = 0;
        this.coinsSpent = 0;
        this.itemsFished = 0;
        this.dungeonsCompleted = 0;
        this.autoRecombobulator = false;
        this.showFashionPoints = false;
        
        // Initialize default skill levels
        initializeSkillLevels();
    }
    
    private void initializeSkillLevels() {
        // Skyblock skills
        skillLevels.put("TAMING", 0);
        skillLevels.put("MINING", 0);
        skillLevels.put("COMBAT", 0);
        skillLevels.put("FORAGING", 0);
        skillLevels.put("FARMING", 0);
        skillLevels.put("FISHING", 0);
        skillLevels.put("ENCHANTING", 0);
        skillLevels.put("ALCHEMY", 0);
        skillLevels.put("CARPENTRY", 0);
        skillLevels.put("RUNECRAFTING", 0);
        skillLevels.put("SOCIAL", 0);
    }
    
    // ==================== GETTERS ====================
    
    public UUID getProfileId() {
        return profileId;
    }
    
    public UUID getOwnerId() {
        return ownerId;
    }
    
    public String getProfileName() {
        return profileName;
    }
    
    public String getProfileIcon() {
        return profileIcon;
    }
    
    public String getDescription() {
        return description;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public long getLastSaveTime() {
        return lastSaveTime;
    }
    
    public long getTotalPlaytime() {
        return totalPlaytime;
    }
    
    public boolean isSelected() {
        return selected;
    }
    
    public double getPurse() {
        return purse;
    }
    
    public double getBankBalance() {
        return bankBalance;
    }
    
    public double getMuseumValue() {
        return museumValue;
    }
    
    public Map<String, Integer> getSkillLevels() {
        return new HashMap<>(skillLevels);
    }
    
    public int getSkillLevel(String skill) {
        return skillLevels.getOrDefault(skill.toUpperCase(), 0);
    }
    
    public Map<String, Integer> getCollectionLevels() {
        return new HashMap<>(collectionLevels);
    }
    
    public int getCollectionLevel(String collection) {
        return collectionLevels.getOrDefault(collection.toUpperCase(), 0);
    }
    
    public Set<String> getUnlockedRecipes() {
        return new HashSet<>(unlockedRecipes);
    }
    
    public Set<String> getCompletedQuests() {
        return new HashSet<>(completedQuests);
    }
    
    public String getInventoryData() {
        return inventoryData;
    }
    
    public String getArmorData() {
        return armorData;
    }
    
    public String getWardrobeData() {
        return wardrobeData;
    }
    
    public String getEnderChestData() {
        return enderChestData;
    }
    
    public String getEquippedPet() {
        return equippedPet;
    }
    
    public Map<String, PetData> getPetData() {
        return new HashMap<>(petData);
    }
    
    public int getDeaths() {
        return deaths;
    }
    
    public int getKills() {
        return kills;
    }
    
    public int getCoinsEarned() {
        return coinsEarned;
    }
    
    public int getCoinsSpent() {
        return coinsSpent;
    }
    
    public int getItemsFished() {
        return itemsFished;
    }
    
    public int getDungeonsCompleted() {
        return dungeonsCompleted;
    }
    
    public boolean isAutoRecombobulator() {
        return autoRecombobulator;
    }
    
    public boolean isShowFashionPoints() {
        return showFashionPoints;
    }
    
    // ==================== SETTERS ====================
    
    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }
    
    public void setProfileIcon(String profileIcon) {
        this.profileIcon = profileIcon;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setLastSaveTime(long lastSaveTime) {
        this.lastSaveTime = lastSaveTime;
    }
    
    public void setTotalPlaytime(long totalPlaytime) {
        this.totalPlaytime = totalPlaytime;
    }
    
    public void addPlaytime(long milliseconds) {
        this.totalPlaytime += milliseconds;
    }
    
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    public void setPurse(double purse) {
        this.purse = purse;
    }
    
    public void addPurse(double amount) {
        this.purse += amount;
    }
    
    public void setBankBalance(double bankBalance) {
        this.bankBalance = bankBalance;
    }
    
    public void setMuseumValue(double museumValue) {
        this.museumValue = museumValue;
    }
    
    public void setSkillLevel(String skill, int level) {
        this.skillLevels.put(skill.toUpperCase(), Math.max(0, Math.min(60, level)));
    }
    
    public void addSkillXp(String skill, double xp) {
        // XP calculation handled by SkyBlockLevelManager
        int currentLevel = getSkillLevel(skill);
        if (currentLevel < 60) {
            // Level up logic would be here
        }
    }
    
    public void setCollectionLevel(String collection, int level) {
        this.collectionLevels.put(collection.toUpperCase(), level);
    }
    
    public void addCollectionProgress(String collection, int amount) {
        int current = getCollectionLevel(collection);
        this.collectionLevels.put(collection.toUpperCase(), current + amount);
    }
    
    public void unlockRecipe(String recipeId) {
        this.unlockedRecipes.add(recipeId.toUpperCase());
    }
    
    public void completeQuest(String questId) {
        this.completedQuests.add(questId.toUpperCase());
    }
    
    public void setInventoryData(String inventoryData) {
        this.inventoryData = inventoryData;
    }
    
    public void setArmorData(String armorData) {
        this.armorData = armorData;
    }
    
    public void setWardrobeData(String wardrobeData) {
        this.wardrobeData = wardrobeData;
    }
    
    public void setEnderChestData(String enderChestData) {
        this.enderChestData = enderChestData;
    }
    
    public void setEquippedPet(String petId) {
        this.equippedPet = petId;
    }
    
    public void setPetData(String petId, PetData data) {
        this.petData.put(petId, data);
    }
    
    public void addDeath() {
        this.deaths++;
    }
    
    public void addKill() {
        this.kills++;
    }
    
    public void addCoinsEarned(int amount) {
        this.coinsEarned += amount;
    }
    
    public void addCoinsSpent(int amount) {
        this.coinsSpent += amount;
    }
    
    public void addItemsFished(int amount) {
        this.itemsFished += amount;
    }
    
    public void addDungeonsCompleted(int amount) {
        this.dungeonsCompleted += amount;
    }
    
    public void setAutoRecombobulator(boolean autoRecombobulator) {
        this.autoRecombobulator = autoRecombobulator;
    }
    
    public void setShowFashionPoints(boolean showFashionPoints) {
        this.showFashionPoints = showFashionPoints;
    }
    
    // ==================== SERIALIZATION ====================
    
    /**
     * Serialize profile to ConfigurationSection.
     */
    public void save(ConfigurationSection section) {
        section.set("profile-id", profileId.toString());
        section.set("owner-id", ownerId.toString());
        section.set("profile-name", profileName);
        section.set("profile-icon", profileIcon);
        section.set("description", description);
        section.set("created-at", createdAt);
        section.set("last-save-time", lastSaveTime);
        section.set("total-playtime", totalPlaytime);
        section.set("selected", selected);
        section.set("purse", purse);
        section.set("bank-balance", bankBalance);
        section.set("museum-value", museumValue);
        
        // Skills
        ConfigurationSection skillsSection = section.createSection("skills");
        for (Map.Entry<String, Integer> entry : skillLevels.entrySet()) {
            skillsSection.set(entry.getKey(), entry.getValue());
        }
        
        // Collections
        ConfigurationSection collectionsSection = section.createSection("collections");
        for (Map.Entry<String, Integer> entry : collectionLevels.entrySet()) {
            collectionsSection.set(entry.getKey(), entry.getValue());
        }
        
        // Sets
        section.set("unlocked-recipes", new ArrayList<>(unlockedRecipes));
        section.set("completed-quests", new ArrayList<>(completedQuests));
        
        // Inventory data
        section.set("inventory-data", inventoryData);
        section.set("armor-data", armorData);
        section.set("wardrobe-data", wardrobeData);
        section.set("ender-chest-data", enderChestData);
        
        // Pets
        section.set("equipped-pet", equippedPet);
        ConfigurationSection petsSection = section.createSection("pets");
        for (Map.Entry<String, PetData> entry : petData.entrySet()) {
            ConfigurationSection petSection = petsSection.createSection(entry.getKey());
            entry.getValue().save(petSection);
        }
        
        // Statistics
        section.set("stats.deaths", deaths);
        section.set("stats.kills", kills);
        section.set("stats.coins-earned", coinsEarned);
        section.set("stats.coins-spent", coinsSpent);
        section.set("stats.items-fished", itemsFished);
        section.set("stats.dungeons-completed", dungeonsCompleted);
        
        // Settings
        section.set("settings.auto-recombobulator", autoRecombobulator);
        section.set("settings.show-fashion-points", showFashionPoints);
    }
    
    /**
     * Deserialize profile from ConfigurationSection.
     */
    public static SkyBlockProfile fromSection(ConfigurationSection section) {
        if (section == null) return null;
        
        String profileIdString = section.getString("profile-id");
        String ownerIdString = section.getString("owner-id");
        
        if (profileIdString == null || ownerIdString == null) return null;
        
        UUID profileId;
        UUID ownerId;
        try {
            profileId = UUID.fromString(profileIdString);
            ownerId = UUID.fromString(ownerIdString);
        } catch (IllegalArgumentException e) {
            return null;
        }
        
        String profileName = section.getString("profile-name", "Unknown Profile");
        long createdAt = section.getLong("created-at", System.currentTimeMillis());
        SkyBlockProfile profile = new SkyBlockProfile(profileId, ownerId, profileName, createdAt);
        
        profile.setProfileIcon(section.getString("profile-icon", "IRON_HELMET"));
        profile.setDescription(section.getString("description", ""));
        profile.setLastSaveTime(section.getLong("last-save-time", System.currentTimeMillis()));
        profile.setTotalPlaytime(section.getLong("total-playtime", 0));
        profile.setSelected(section.getBoolean("selected", false));
        profile.setPurse(section.getDouble("purse", 0.0));
        profile.setBankBalance(section.getDouble("bank-balance", 0.0));
        profile.setMuseumValue(section.getDouble("museum-value", 0.0));
        
        // Skills
        ConfigurationSection skillsSection = section.getConfigurationSection("skills");
        if (skillsSection != null) {
            for (String key : skillsSection.getKeys(false)) {
                profile.setSkillLevel(key, skillsSection.getInt(key, 0));
            }
        }
        
        // Collections
        ConfigurationSection collectionsSection = section.getConfigurationSection("collections");
        if (collectionsSection != null) {
            for (String key : collectionsSection.getKeys(false)) {
                profile.setCollectionLevel(key, collectionsSection.getInt(key, 0));
            }
        }
        
        // Sets
        List<String> recipes = section.getStringList("unlocked-recipes");
        if (recipes != null) {
            profile.unlockedRecipes.addAll(recipes);
        }
        
        List<String> quests = section.getStringList("completed-quests");
        if (quests != null) {
            profile.completedQuests.addAll(quests);
        }
        
        // Inventory data
        profile.setInventoryData(section.getString("inventory-data", ""));
        profile.setArmorData(section.getString("armor-data", ""));
        profile.setWardrobeData(section.getString("wardrobe-data", ""));
        profile.setEnderChestData(section.getString("ender-chest-data", ""));
        
        // Pets
        profile.setEquippedPet(section.getString("equipped-pet"));
        ConfigurationSection petsSection = section.getConfigurationSection("pets");
        if (petsSection != null) {
            for (String key : petsSection.getKeys(false)) {
                ConfigurationSection petSection = petsSection.getConfigurationSection(key);
                if (petSection != null) {
                    PetData petData = PetData.fromSection(petSection);
                    if (petData != null) {
                        profile.setPetData(key, petData);
                    }
                }
            }
        }
        
        // Statistics
        ConfigurationSection statsSection = section.getConfigurationSection("stats");
        if (statsSection != null) {
            profile.deaths = statsSection.getInt("deaths", 0);
            profile.kills = statsSection.getInt("kills", 0);
            profile.coinsEarned = statsSection.getInt("coins-earned", 0);
            profile.coinsSpent = statsSection.getInt("coins-spent", 0);
            profile.itemsFished = statsSection.getInt("items-fished", 0);
            profile.dungeonsCompleted = statsSection.getInt("dungeons-completed", 0);
        }
        
        // Settings
        ConfigurationSection settingsSection = section.getConfigurationSection("settings");
        if (settingsSection != null) {
            profile.setAutoRecombobulator(settingsSection.getBoolean("auto-recombobulator", false));
            profile.setShowFashionPoints(settingsSection.getBoolean("show-fashion-points", false));
        }
        
        return profile;
    }
    
    /**
     * Get formatted creation date.
     */
    public String getFormattedCreationDate() {
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(createdAt),
            java.time.ZoneId.systemDefault()
        ).format(DATE_FORMAT);
    }
    
    /**
     * Get formatted playtime.
     */
    public String getFormattedPlaytime() {
        long hours = totalPlaytime / 3600000;
        long minutes = (totalPlaytime % 3600000) / 60000;
        return String.format("%dh %dm", hours, minutes);
    }
    
    /**
     * Pet data inner class.
     */
    public static class PetData {
        private String petId;
        private int level;
        private double xp;
        private boolean active;
        
        public PetData(String petId) {
            this.petId = petId;
            this.level = 1;
            this.xp = 0;
            this.active = false;
        }
        
        public String getPetId() { return petId; }
        public int getLevel() { return level; }
        public double getXp() { return xp; }
        public boolean isActive() { return active; }
        
        public void setLevel(int level) { this.level = level; }
        public void setXp(double xp) { this.xp = xp; }
        public void setActive(boolean active) { this.active = active; }
        
        public void save(ConfigurationSection section) {
            section.set("pet-id", petId);
            section.set("level", level);
            section.set("xp", xp);
            section.set("active", active);
        }
        
        public static PetData fromSection(ConfigurationSection section) {
            if (section == null) return null;
            
            String petId = section.getString("pet-id");
            if (petId == null) return null;
            
            PetData data = new PetData(petId);
            data.setLevel(section.getInt("level", 1));
            data.setXp(section.getDouble("xp", 0.0));
            data.setActive(section.getBoolean("active", false));
            
            return data;
        }
    }
}

