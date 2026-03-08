package io.papermc.Grivience.pet;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class PetManager {
    private static final String PET_MODIFIER_NAME_PREFIX = "grivience_pet_";
    private static final int DEFAULT_LEGACY_EFFECT_CLEANUP_MIN_DURATION_TICKS = 20 * 60 * 8;

    private final GriviencePlugin plugin;
    private final NamespacedKey petIdKey;
    private final NamespacedKey petAppliedEffectsKey;
    private final Map<String, PetDefinition> pets = new HashMap<>();
    private final File petsFile;
    private final File legacyPetDataFile;
    private final Map<String, PlayerProfile> textureCache = new HashMap<>();
    private io.papermc.Grivience.stats.SkyblockLevelManager levelManager;

    // Tracks which potion effects were last applied by the pet system per player (so we don't
    // accidentally remove unrelated effects from other systems).
    private final Map<UUID, Set<PotionEffectType>> appliedPetEffects = new HashMap<>();

    private boolean expEnabled;
    private double miningFishingXpMultiplier;
    private double nonMatchingSkillMultiplier;
    private double nonMatchingAlchemyEnchantingMultiplier;
    private boolean legacyPotionEffectsEnabled;
    private boolean legacyAttributeModifiersEnabled;
    private boolean legacyCleanupEnabled;
    private int legacyCleanupMinDurationTicks;

    public PetManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.petIdKey = new NamespacedKey(plugin, "pet_id");
        this.petAppliedEffectsKey = new NamespacedKey(plugin, "pet_effect_types");
        this.petsFile = new File(plugin.getDataFolder(), "pets.yml");
        this.legacyPetDataFile = new File(plugin.getDataFolder(), "pet-data.yml");
        ensureDefaultPetsFile();
        loadPets();
        reloadSettings();
    }

    public void setLevelManager(io.papermc.Grivience.stats.SkyblockLevelManager levelManager) {
        this.levelManager = levelManager;
    }

    public void reloadPets() {
        loadPets();
        reloadSettings();
    }

    public boolean isExpEnabled() {
        return expEnabled;
    }

    public void reloadSettings() {
        expEnabled = plugin.getConfig().getBoolean("skyblock-pets.enabled", true);
        miningFishingXpMultiplier = clampFinite(plugin.getConfig().getDouble("skyblock-pets.multipliers.mining-fishing", 1.5D), 0.0D, 10.0D);
        nonMatchingSkillMultiplier = clampFinite(plugin.getConfig().getDouble("skyblock-pets.multipliers.non-matching", 1.0D / 3.0D), 0.0D, 1.0D);
        nonMatchingAlchemyEnchantingMultiplier = clampFinite(plugin.getConfig().getDouble("skyblock-pets.multipliers.non-matching-alchemy-enchanting", 1.0D / 12.0D), 0.0D, 1.0D);

        legacyPotionEffectsEnabled = plugin.getConfig().getBoolean("skyblock-pets.legacy.potion-effects.enabled", false);
        legacyAttributeModifiersEnabled = plugin.getConfig().getBoolean("skyblock-pets.legacy.attribute-modifiers.enabled", false);
        legacyCleanupEnabled = plugin.getConfig().getBoolean("skyblock-pets.legacy.cleanup-on-join", true);
        legacyCleanupMinDurationTicks = Math.max(0, plugin.getConfig().getInt(
                "skyblock-pets.legacy.cleanup-min-duration-ticks",
                DEFAULT_LEGACY_EFFECT_CLEANUP_MIN_DURATION_TICKS
        ));
    }

    public void registerRecipes() {
        for (PetDefinition def : pets.values()) {
            NamespacedKey key = new NamespacedKey(plugin, "pet_" + def.id());
            org.bukkit.inventory.ShapelessRecipe recipe = new org.bukkit.inventory.ShapelessRecipe(key, createPetItem(def.id(), null));
            recipe.addIngredient(Material.EGG);
            recipe.addIngredient(def.icon() != null ? def.icon() : Material.LEAD);
            plugin.getServer().addRecipe(recipe);
        }
    }

    public Collection<PetDefinition> allPets() {
        return pets.values();
    }

    public record PetProgress(
            int level,
            long totalExp,
            long expIntoLevel,
            long expToNextLevel
    ) {
    }

    public PetStatBonuses equippedStatBonuses(Player player) {
        if (player == null) {
            return PetStatBonuses.ZERO;
        }

        String petId = equippedPet(player);
        if (petId == null) {
            return PetStatBonuses.ZERO;
        }

        PetDefinition def = pets.get(petId);
        if (def == null) {
            return PetStatBonuses.ZERO;
        }

        PetStatBonuses max = def.stats();
        if (max == null) {
            return PetStatBonuses.ZERO;
        }

        int level = getLevel(player, petId);
        int maxLevel = Math.max(1, def.maxLevel());
        double scale = maxLevel <= 1 ? 1.0D : ((double) level / (double) maxLevel);
        if (!Double.isFinite(scale) || scale <= 0.0D) {
            return PetStatBonuses.ZERO;
        }

        return new PetStatBonuses(
                max.health() * scale,
                max.defense() * scale,
                max.strength() * scale,
                max.critChance() * scale,
                max.critDamage() * scale,
                max.intelligence() * scale,
                max.speed() * scale,
                max.attackSpeed() * scale,
                max.ferocity() * scale,
                max.magicFind() * scale,
                max.petLuck() * scale,
                max.seaCreatureChance() * scale,
                max.trueDefense() * scale,
                max.abilityDamage() * scale
        );
    }


    public Set<String> ownedPets(Player player) {
        io.papermc.Grivience.skyblock.profile.SkyBlockProfile profile = requireProfile(player);
        if (profile == null) {
            return Set.of();
        }
        maybeMigrateLegacy(player, profile);
        return Collections.unmodifiableSet(profile.getPetData().keySet());
    }

    public boolean unlockPet(Player player, String petId) {
        if (player == null || petId == null) {
            return false;
        }
        String normalizedId = petId.toLowerCase(Locale.ROOT);
        if (!pets.containsKey(normalizedId)) {
            return false;
        }

        io.papermc.Grivience.skyblock.profile.SkyBlockProfile profile = requireProfile(player);
        if (profile == null) {
            return false;
        }
        maybeMigrateLegacy(player, profile);

        Map<String, io.papermc.Grivience.skyblock.profile.SkyBlockProfile.PetData> owned = profile.getPetData();
        if (owned.containsKey(normalizedId)) {
            player.sendMessage(ChatColor.YELLOW + "You already own that pet.");
            return false;
        }

        io.papermc.Grivience.skyblock.profile.SkyBlockProfile.PetData data =
                new io.papermc.Grivience.skyblock.profile.SkyBlockProfile.PetData(normalizedId);
        profile.setPetData(normalizedId, data);

        saveProfile(profile);

        player.sendMessage(ChatColor.GREEN + "Unlocked pet: " + ChatColor.AQUA + pets.get(normalizedId).displayName());
        
        // Update Pet Score for Skyblock Leveling
        updateTotalPetScore(player);
        
        return true;
    }

    /**
     * Calculates and updates the player's total Pet Score in the level manager.
     */
    public void updateTotalPetScore(Player player) {
        if (player == null || levelManager == null) {
            return;
        }
        
        io.papermc.Grivience.skyblock.profile.SkyBlockProfile profile = requireProfile(player);
        if (profile == null) {
            return;
        }
        
        int totalScore = 0;
        for (String petId : profile.getPetData().keySet()) {
            PetDefinition def = pets.get(petId.toLowerCase(Locale.ROOT));
            if (def == null || def.rarity() == null) continue;
            
            totalScore += switch (def.rarity()) {
                case COMMON -> 1;
                case UNCOMMON -> 2;
                case RARE -> 3;
                case EPIC -> 4;
                case LEGENDARY -> 5;
                case MYTHIC -> 6;
                case DIVINE -> 10;
                case SPECIAL, VERY_SPECIAL -> 5;
                default -> 1;
            };
        }
        
        levelManager.updatePetScore(player, totalScore);
    }

    public ItemStack createPetItem(String id, Player viewer) {
        return createPetItem(id, viewer, false);
    }

    public ItemStack createPetItem(String id, Player viewer, boolean isInGui) {
        PetDefinition def = pets.get(id);
        if (def == null) {
            return null;
        }
        
        // Base Item: Custom Head with Texture
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta skull && def.headTexture() != null && !def.headTexture().isBlank()) {
            PlayerProfile profile = textureCache.computeIfAbsent(def.headTexture(), this::buildHeadProfile);
            if (profile != null) {
                skull.setPlayerProfile(profile);
            }
        }
        
        PetProgress progress = viewer == null ? new PetProgress(1, 0L, 0L, xpForNext(def, 1)) : progress(viewer, def.id());
        int level = progress.level();

        // 100% Accurate Hypixel Skyblock Name Format: [Lvl X] [Pet Name]
        meta.setDisplayName(ChatColor.GRAY + "[Lvl " + level + "] " + def.rarity().color() + def.displayName());

        List<String> lore = new ArrayList<>();
        
        // Skill Type Line (e.g. Combat Pet)
        lore.add(ChatColor.DARK_GRAY + capitalize(def.skillType().name()) + " Pet");
        lore.add("");

        // Stats section (Dynamic based on level)
        PetStatBonuses stats = def.stats();
        if (stats != null) {
            double scale = (double) level / 100.0;
            appendStat(lore, "Health", stats.health() * scale, ChatColor.RED);
            appendStat(lore, "Defense", stats.defense() * scale, ChatColor.GREEN);
            appendStat(lore, "Strength", stats.strength() * scale, ChatColor.RED);
            appendStat(lore, "Crit Chance", stats.critChance() * scale, ChatColor.BLUE, "%");
            appendStat(lore, "Crit Damage", stats.critDamage() * scale, ChatColor.BLUE, "%");
            appendStat(lore, "Intelligence", stats.intelligence() * scale, ChatColor.AQUA);
            appendStat(lore, "Speed", stats.speed() * scale, ChatColor.WHITE);
            appendStat(lore, "Attack Speed", stats.attackSpeed() * scale, ChatColor.YELLOW);
            appendStat(lore, "Ferocity", stats.ferocity() * scale, ChatColor.RED);
            appendStat(lore, "Magic Find", stats.magicFind() * scale, ChatColor.AQUA);
            appendStat(lore, "Pet Luck", stats.petLuck() * scale, ChatColor.LIGHT_PURPLE);
            appendStat(lore, "Sea Creature Chance", stats.seaCreatureChance() * scale, ChatColor.DARK_AQUA, "%");
            appendStat(lore, "True Defense", stats.trueDefense() * scale, ChatColor.WHITE);
            appendStat(lore, "Ability Damage", stats.abilityDamage() * scale, ChatColor.RED, "%");
            lore.add("");
        }


        // Abilities section (Dynamic based on level and rarity)
        for (PetDefinition.PetAbility ability : def.abilities()) {
            if (def.rarity().ordinal() < ability.minRarity().ordinal()) continue;
            
            lore.add(ChatColor.GOLD + ability.name());
            double val = ability.getValue(level);
            for (String descLine : ability.description()) {
                lore.add(ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', descLine.replace("{value}", String.format("%.1f", val))));
            }
            lore.add("");
        }

        // Pet Item Slot (Accurate to Skyblock)
        lore.add(ChatColor.GRAY + "Held Item: " + ChatColor.RED + "None");
        lore.add("");

        // Progress bar / XP section
        if (progress.level() >= def.maxLevel()) {
            lore.add(ChatColor.DARK_GRAY + "MAX LEVEL");
        } else {
            long into = progress.expIntoLevel();
            long toNext = progress.expToNextLevel();
            double pct = (double) into / toNext * 100.0;
            
            lore.add(ChatColor.GRAY + "Progress to Level " + (level + 1) + ": " + ChatColor.YELLOW + String.format("%.1f%%", pct));
            lore.add(progressBar(into, toNext));
            lore.add(ChatColor.GRAY + "(" + formatXp(into) + "/" + formatXp(toNext) + ")");
        }
        lore.add("");

        // Rarity line (100% Accurate Footer)
        lore.add(def.rarity().color() + ChatColor.BOLD.toString() + def.rarity().name().toUpperCase(Locale.ROOT));

        if (isInGui) {
            String equipped = equippedPet(viewer);
            if (id.equalsIgnoreCase(equipped)) {
                lore.add("");
                lore.add(ChatColor.RED + "Click to despawn!");
            } else {
                lore.add("");
                lore.add(ChatColor.YELLOW + "Click to summon!");
            }
        } else {
            lore.add("");
            lore.add(ChatColor.YELLOW + "Right-click to add to pets menu!");
            lore.add(ChatColor.DARK_GRAY + "Keep as item to trade.");
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(petIdKey, PersistentDataType.STRING, def.id());
        item.setItemMeta(meta);
        return item;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }


    private void appendStat(List<String> lore, String name, double value, ChatColor color) {
        appendStat(lore, name, value, color, "");
    }

    private void appendStat(List<String> lore, String name, double value, ChatColor color, String suffix) {
        if (Math.abs(value) < 0.01) return;
        String sign = value > 0 ? "+" : "";
        lore.add(ChatColor.GRAY + name + ": " + color + sign + (int) Math.round(value) + suffix);
    }

    private String progressBar(long current, long target) {
        int bars = 20;
        int filled = (int) ((double) current / target * bars);
        StringBuilder sb = new StringBuilder(ChatColor.DARK_GRAY + "[");
        sb.append(ChatColor.GREEN);
        for (int i = 0; i < filled; i++) sb.append("-");
        sb.append(ChatColor.WHITE);
        for (int i = filled; i < bars; i++) sb.append("-");
        sb.append(ChatColor.DARK_GRAY).append("]");
        return sb.toString();
    }

    private String formatXp(long xp) {
        if (xp < 1000) return String.valueOf(xp);
        if (xp < 1000000) return String.format("%.1fk", xp / 1000.0);
        return String.format("%.1fM", xp / 1000000.0);
    }

    public String petId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(petIdKey, PersistentDataType.STRING);
    }

    public void equip(Player player, String id) {
        if (player == null) {
            return;
        }

        io.papermc.Grivience.skyblock.profile.SkyBlockProfile profile = requireProfile(player);
        if (profile == null) {
            return;
        }
        maybeMigrateLegacy(player, profile);

        String normalizedId = id == null ? null : id.toLowerCase(Locale.ROOT);
        if (normalizedId == null || !pets.containsKey(normalizedId) || !profile.getPetData().containsKey(normalizedId)) {
            profile.setEquippedPet(null);
            for (io.papermc.Grivience.skyblock.profile.SkyBlockProfile.PetData data : profile.getPetData().values()) {
                if (data != null) {
                    data.setActive(false);
                }
            }
            clearEffects(player);
            saveProfile(profile);
            return;
        }

        for (io.papermc.Grivience.skyblock.profile.SkyBlockProfile.PetData data : profile.getPetData().values()) {
            if (data != null) {
                data.setActive(false);
            }
        }
        io.papermc.Grivience.skyblock.profile.SkyBlockProfile.PetData active = profile.getPetData().get(normalizedId);
        if (active != null) {
            active.setActive(true);
        }

        profile.setEquippedPet(normalizedId);
        clearEffects(player);
        PetDefinition def = pets.get(normalizedId);
        if (def != null) {
            applyEffects(player, def);
        }
        saveProfile(profile);
    }

    public String equippedPet(Player player) {
        io.papermc.Grivience.skyblock.profile.SkyBlockProfile profile = requireProfile(player);
        if (profile == null) {
            return null;
        }
        maybeMigrateLegacy(player, profile);
        String equipped = profile.getEquippedPet();
        if (equipped == null || equipped.isBlank()) {
            return null;
        }
        return equipped.toLowerCase(Locale.ROOT);
    }

    public void applyCurrent(Player player) {
        clearEffects(player);
        String id = equippedPet(player);
        if (id != null) {
            PetDefinition def = pets.get(id);
            if (def != null) {
                applyEffects(player, def);
            }
        }
    }

    private void applyEffects(Player player, PetDefinition def) {
        if (player == null || def == null) {
            return;
        }
        int level = getLevel(player, def.id());
        int extraAmp = Math.max(0, (level - 1) / 20);

        Set<PotionEffectType> applied = new HashSet<>();
        if (legacyPotionEffectsEnabled && def.effects() != null) {
            for (PotionEffect effect : def.effects()) {
                if (effect == null || effect.getType() == null) {
                    continue;
                }
                player.addPotionEffect(new PotionEffect(
                        effect.getType(),
                        effect.getDuration(),
                        effect.getAmplifier() + extraAmp,
                        effect.isAmbient(),
                        effect.hasParticles(),
                        effect.hasIcon()
                ));
                applied.add(effect.getType());
            }
        }
        if (!applied.isEmpty()) {
            appliedPetEffects.put(player.getUniqueId(), applied);
            persistAppliedEffectTypes(player, applied);
        } else {
            persistAppliedEffectTypes(player, Set.of());
        }

        if (legacyAttributeModifiersEnabled && def.attributeBonuses() != null) {
            double mult = 1.0D + (level - 1) * 0.01D;
            for (Map.Entry<org.bukkit.attribute.Attribute, Double> entry : def.attributeBonuses().entrySet()) {
                if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                AttributeInstance inst = player.getAttribute(entry.getKey());
                if (inst == null) {
                    continue;
                }
                double amount = entry.getValue() * mult;
                if (!Double.isFinite(amount) || Math.abs(amount) < 1e-9D) {
                    continue;
                }
                inst.addModifier(new org.bukkit.attribute.AttributeModifier(
                        new NamespacedKey(plugin, "pet_mod_" + attributeKey(entry.getKey())),
                        amount,
                        org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER,
                        org.bukkit.inventory.EquipmentSlotGroup.ANY
                ));
            }
        }
    }

    private static final Set<PotionEffectType> LEGACY_CLEANUP_POTION_TYPES = buildLegacyCleanupPotionTypes();

    private static Set<PotionEffectType> buildLegacyCleanupPotionTypes() {
        Set<PotionEffectType> types = new HashSet<>();
        Collections.addAll(types,
                PotionEffectType.SPEED,
                PotionEffectType.STRENGTH,
                PotionEffectType.SATURATION,
                PotionEffectType.HASTE,
                PotionEffectType.RESISTANCE,
                PotionEffectType.FIRE_RESISTANCE,
                PotionEffectType.LUCK
        );
        types.remove(null);
        return Collections.unmodifiableSet(types);
    }

    /**
     * Cleans up legacy pet potion effects that may have persisted across reconnects/server restarts.
     * This is a best-effort heuristic meant to remove effects the old pet system applied (ambient/no particles/icon, long duration).
     */
    public void cleanupLegacyPotionEffects(Player player) {
        if (player == null) {
            return;
        }
        if (!legacyCleanupEnabled || legacyPotionEffectsEnabled) {
            return;
        }

        int minDurationTicks = Math.max(0, legacyCleanupMinDurationTicks);
        if (minDurationTicks <= 0 || LEGACY_CLEANUP_POTION_TYPES.isEmpty()) {
            return;
        }

        for (PotionEffect effect : player.getActivePotionEffects()) {
            if (effect == null || effect.getType() == null) {
                continue;
            }
            if (!LEGACY_CLEANUP_POTION_TYPES.contains(effect.getType())) {
                continue;
            }
            if (effect.getDuration() < minDurationTicks) {
                continue;
            }
            if (!effect.isAmbient() || effect.hasParticles() || effect.hasIcon()) {
                continue;
            }
            player.removePotionEffect(effect.getType());
        }
    }

    private void persistAppliedEffectTypes(Player player, Set<PotionEffectType> types) {
        if (player == null) {
            return;
        }
        if (types == null || types.isEmpty()) {
            player.getPersistentDataContainer().remove(petAppliedEffectsKey);
            return;
        }

        StringBuilder joined = new StringBuilder();
        for (PotionEffectType type : types) {
            if (type == null || type.getName() == null || type.getName().isBlank()) {
                continue;
            }
            if (joined.length() > 0) {
                joined.append(',');
            }
            joined.append(type.getName().toUpperCase(Locale.ROOT));
        }

        if (joined.isEmpty()) {
            player.getPersistentDataContainer().remove(petAppliedEffectsKey);
            return;
        }
        player.getPersistentDataContainer().set(petAppliedEffectsKey, PersistentDataType.STRING, joined.toString());
    }

    private Set<PotionEffectType> readAppliedEffectTypes(Player player) {
        if (player == null) {
            return Set.of();
        }
        String raw = player.getPersistentDataContainer().get(petAppliedEffectsKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }

        Set<PotionEffectType> out = new HashSet<>();
        for (String part : raw.split(",")) {
            if (part == null) {
                continue;
            }
            String name = part.trim();
            if (name.isEmpty()) {
                continue;
            }
            PotionEffectType type = PotionEffectType.getByName(name.toUpperCase(Locale.ROOT));
            if (type != null) {
                out.add(type);
            }
        }
        return out;
    }

    private void clearEffects(Player player) {
        if (player == null) {
            return;
        }

        UUID playerId = player.getUniqueId();

        // Remove only potion effects previously applied by the pet system for this player.
        Set<PotionEffectType> toRemove = new HashSet<>();
        Set<PotionEffectType> appliedEffects = appliedPetEffects.remove(playerId);
        if (appliedEffects != null && !appliedEffects.isEmpty()) {
            toRemove.addAll(appliedEffects);
        }
        Set<PotionEffectType> persisted = readAppliedEffectTypes(player);
        if (!persisted.isEmpty()) {
            toRemove.addAll(persisted);
        }
        if (!toRemove.isEmpty()) {
            for (PotionEffectType type : toRemove) {
                if (type != null) {
                    player.removePotionEffect(type);
                }
            }
        }
        persistAppliedEffectTypes(player, Set.of());

        // Remove pet attribute modifiers (by stable UUID per attribute) so swapping pets cannot stack stats.
        for (Attribute attribute : Registry.ATTRIBUTE) {
            if (attribute == null) {
                continue;
            }
            AttributeInstance inst = player.getAttribute(attribute);
            if (inst == null) {
                continue;
            }

            NamespacedKey modifierKey = new NamespacedKey(plugin, "pet_mod_" + attributeKey(attribute));
            List<org.bukkit.attribute.AttributeModifier> modifiersToRemove = new ArrayList<>();
            for (org.bukkit.attribute.AttributeModifier modifier : inst.getModifiers()) {
                if (modifier == null) {
                    continue;
                }
                if (modifierKey.equals(modifier.getKey())) {
                    modifiersToRemove.add(modifier);
                    continue;
                }
                if (modifier.getKey().getKey().startsWith("pet_mod_")) {
                    modifiersToRemove.add(modifier);
                }
            }
            for (org.bukkit.attribute.AttributeModifier modifier : modifiersToRemove) {
                inst.removeModifier(modifier);
            }
        }
    }

    public void handleQuit(Player player) {
        clearEffects(player);
    }


    private static String attributeKey(org.bukkit.attribute.Attribute attribute) {
        if (attribute == null) {
            return "unknown";
        }
        try {
            org.bukkit.NamespacedKey key = Registry.ATTRIBUTE.getKey(attribute);
            if (key != null) {
                return key.getKey().toLowerCase(Locale.ROOT);
            }
        } catch (Exception ignored) {
        }
        return attribute.toString().toLowerCase(Locale.ROOT);
    }

    private void loadPets() {
        pets.clear();
        YamlConfiguration yaml = null;
        ConfigurationSection root = null;

        if (petsFile.exists()) {
            yaml = YamlConfiguration.loadConfiguration(petsFile);
            if (!looksLikeEquippedData(yaml)) {
                root = yaml.getConfigurationSection("pets");
                if (root == null) {
                    root = yaml; // allow top-level pet IDs
                }
            }
        }
        if (root == null) {
            root = plugin.getConfig().getConfigurationSection("pets");
        }
        if (root == null || root.getKeys(false).isEmpty()) {
            pets.putAll(defaultPets());
            return;
        }

        // Backward-compatible defaults: if an existing pets.yml lacks the newer keys (rarity/skill-type/stats),
        // we fill them from the built-in defaults so servers don't lose pet bonuses after upgrading.
        Map<String, PetDefinition> defaults = defaultPets();
        for (String id : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) continue;
            String normalizedId = id.toLowerCase(Locale.ROOT);
            String name = sec.getString("name", id);
            Material icon = Material.matchMaterial(sec.getString("icon", "PLAYER_HEAD"));
            if (icon == null) icon = Material.PLAYER_HEAD;
            String headUrl = sec.getString("head-texture", null);
            List<String> lore = sec.getStringList("lore");
            List<PotionEffect> effects = new ArrayList<>();
            Map<org.bukkit.attribute.Attribute, Double> attrs = new HashMap<>();
            double cropMult = sec.getDouble("crop-multiplier", 1.0D);
            ConfigurationSection statsSection = sec.getConfigurationSection("stats");
            PetStatBonuses stats = readStats(statsSection);
            
            // Abilities
            List<PetDefinition.PetAbility> abilities = new ArrayList<>();
            ConfigurationSection abilitiesSec = sec.getConfigurationSection("abilities");
            if (abilitiesSec != null) {
                for (String abId : abilitiesSec.getKeys(false)) {
                    ConfigurationSection abSec = abilitiesSec.getConfigurationSection(abId);
                    if (abSec == null) continue;
                    String abName = abSec.getString("name", abId);
                    List<String> abLore = abSec.getStringList("description");
                    double start = abSec.getDouble("start-value", 0.0);
                    double end = abSec.getDouble("end-value", 0.0);
                    PetRarity minRarity = PetRarity.valueOf(abSec.getString("min-rarity", "COMMON").toUpperCase(Locale.ROOT));
                    abilities.add(new PetDefinition.PetAbility(abName, abLore, start, end, minRarity));
                }
            }

            ConfigurationSection effSec = sec.getConfigurationSection("effects");
            if (effSec != null) {
                for (String effKey : effSec.getKeys(false)) {
                    PotionEffectType type = PotionEffectType.getByName(effKey.toUpperCase(Locale.ROOT));
                    if (type == null) continue;
                    int amplifier = effSec.getInt(effKey + ".amplifier", 0);
                    int duration = effSec.getInt(effKey + ".duration", 20 * 60 * 10); // default 10m
                    effects.add(new PotionEffect(type, duration, amplifier, true, false, false));
                }
            }
            ConfigurationSection attrSec = sec.getConfigurationSection("attributes");
            if (attrSec != null) {
                for (String key : attrSec.getKeys(false)) {
                    org.bukkit.attribute.Attribute attr = parseAttribute(key);
                    if (attr != null) {
                        attrs.put(attr, attrSec.getDouble(key, 0.0D));
                    }
                }
            }
            PetSkillType skillType = PetSkillType.parse(sec.getString("skill-type", sec.getString("skill", "ALL")), PetSkillType.ALL);
            String rarityName = sec.getString("rarity", "COMMON");
            PetRarity rarity = PetRarity.COMMON;
            try {
                rarity = PetRarity.valueOf(rarityName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
            int maxLevel = sec.getInt("max-level", 100);

            PetDefinition fallback = defaults.get(normalizedId);
            if (fallback != null) {
                if (statsSection == null) {
                    stats = fallback.stats();
                }
                if (!sec.contains("skill-type") && !sec.contains("skill") && fallback.skillType() != null) {
                    skillType = fallback.skillType();
                }
                if (!sec.contains("rarity") && fallback.rarity() != null) {
                    rarity = fallback.rarity();
                }
                if (!sec.contains("max-level") && fallback.maxLevel() > 0) {
                    maxLevel = fallback.maxLevel();
                }
                if (abilities.isEmpty() && !fallback.abilities().isEmpty()) {
                    abilities = fallback.abilities();
                }
            }

            pets.put(normalizedId, new PetDefinition(normalizedId, name, icon, headUrl, lore, effects, attrs, cropMult, stats, skillType, rarity, maxLevel, abilities));
        }
        if (pets.isEmpty()) {
            pets.putAll(defaultPets());
        }
    }


    private void ensureDefaultPetsFile() {
        if (!petsFile.exists()) {
            try {
                plugin.saveResource("pets.yml", false);
            } catch (IllegalArgumentException ignored) {
                // Resource not packaged; silently skip.
            }
        }
    }

    private Map<String, PetDefinition> defaultPets() {
        Map<String, PetDefinition> map = new HashMap<>();
        map.put("griffin", new PetDefinition("griffin",
                "&6Griffin",
                Material.PLAYER_HEAD,
                "http://textures.minecraft.net/texture/91c5e326245bd1c7075e84f7491437132d0b1328c972061c158e9a1e2432286c",
                List.of("&7A legendary companion.", "&7Grants combat stat bonuses."),
                List.of(),
                Map.of(),
                1.05D,
                new PetStatBonuses(20.0D, 0.0D, 25.0D, 0.0D, 0.0D, 0.0D),
                PetSkillType.COMBAT,
                PetRarity.LEGENDARY,
                100,
                List.of(new PetDefinition.PetAbility("Legendary Bond", List.of("&7Grants &c+{value} Strength &7and &c+20 Health", "&7at max level."), 5.0, 25.0, PetRarity.LEGENDARY))
        ));
        map.put("wolf", new PetDefinition("wolf",
                "&fWolf",
                Material.PLAYER_HEAD,
                "http://textures.minecraft.net/texture/9dd850f5111840e53955ca7483215bf4c5215c0f0481c4ad8edb9dcaab1ed68f",
                List.of("&7A loyal combat pet.", "&7Boosts crit-related stats."),
                List.of(),
                Map.of(),
                1.0D,
                new PetStatBonuses(0.0D, 0.0D, 0.0D, 0.0D, 20.0D, 0.0D),
                PetSkillType.COMBAT,
                PetRarity.RARE,
                100,
                List.of(new PetDefinition.PetAbility("Pack Leader", List.of("&7Gain &9+{value}% Crit Damage &7for", "&7each wolf nearby."), 5.0, 20.0, PetRarity.RARE))
        ));
        map.put("rabbit", new PetDefinition("rabbit",
                "&aRabbit",
                Material.PLAYER_HEAD,
                "http://textures.minecraft.net/texture/e087f28f68a799beb7f94f4d0723c1c5d19c73cd3a63675b8622c2c58eea8b2",
                List.of("&7A helpful farming pet."),
                List.of(),
                Map.of(),
                1.1D,
                new PetStatBonuses(10.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D),
                PetSkillType.FARMING,
                PetRarity.UNCOMMON,
                100,
                List.of(new PetDefinition.PetAbility("Happy Feet", List.of("&7Increases farming speed by &a+{value}%."), 1.0, 10.0, PetRarity.UNCOMMON))
        ));
        map.put("farmer", new PetDefinition("farmer",
                "&aRabbit Forager",
                Material.PLAYER_HEAD,
                "http://textures.minecraft.net/texture/e087f28f68a799beb7f94f4d0723c1c5d19c73cd3a63675b8622c2c58eea8b2",
                List.of("&7Harvest helper", "&7Boosts farming-focused stats."),
                List.of(),
                Map.of(),
                1.25D,
                new PetStatBonuses(10.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D),
                PetSkillType.FARMING,
                PetRarity.UNCOMMON,
                100,
                List.of(new PetDefinition.PetAbility("Farming Fortune", List.of("&7Gain &6+{value} Farming Fortune."), 10.0, 50.0, PetRarity.UNCOMMON))
        ));
        map.put("turtle", new PetDefinition("turtle",
                "&2Turtle Guardian",
                Material.PLAYER_HEAD,
                "http://textures.minecraft.net/texture/5f2c517e3d1bc4e8e9b35bd50a6481d948e57808b4374c1c1ff717a8ba4a898",
                List.of("&7Resistance and extra armor"),
                List.of(),
                Map.of(),
                1.0D,
                new PetStatBonuses(20.0D, 40.0D, 0.0D, 0.0D, 0.0D, 0.0D),
                PetSkillType.COMBAT,
                PetRarity.EPIC,
                100,
                List.of(new PetDefinition.PetAbility("Shell Shield", List.of("&7Reduces damage taken by &9{value}%."), 5.0, 20.0, PetRarity.EPIC))
        ));
        map.put("bee", new PetDefinition("bee",
                "&eBee Keeper",
                Material.PLAYER_HEAD,
                "http://textures.minecraft.net/texture/7c4c2e3d8f3a6b1d6c1f5e3acda6af7d2a2c87a1b7109a2c8b3f4d7c9a5e3b",
                List.of("&7Haste and luck for farming"),
                List.of(),
                Map.of(),
                1.15D,
                new PetStatBonuses(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 10.0D),
                PetSkillType.FARMING,
                PetRarity.UNCOMMON,
                100,
                List.of(new PetDefinition.PetAbility("Hive Mind", List.of("&7Gain &b+{value} Intelligence &7for each", "&7nearby bee."), 1.0, 10.0, PetRarity.UNCOMMON))
        ));
        map.put("dragon", new PetDefinition("dragon",
                "&cYoung Dragon",
                Material.PLAYER_HEAD,
                "http://textures.minecraft.net/texture/7f2df2569ca426cf4348b8461e7d1ddbb7a1a996a96f2ef749acd972d4b8ca9",
                List.of("&7Bonus damage and strength", "&7Grants offensive stat bonuses."),
                List.of(),
                Map.of(),
                1.0D,
                new PetStatBonuses(10.0D, 0.0D, 30.0D, 5.0D, 0.0D, 0.0D),
                PetSkillType.COMBAT,
                PetRarity.LEGENDARY,
                100,
                List.of(new PetDefinition.PetAbility("Dragon Claw", List.of("&7Increases damage against dragons by &c{value}%."), 10.0, 50.0, PetRarity.LEGENDARY))
        ));
        return map;
    }


    private PetStatBonuses readStats(ConfigurationSection section) {
        if (section == null) {
            return PetStatBonuses.ZERO;
        }
        double health = section.getDouble("health", 0.0D);
        double defense = section.getDouble("defense", 0.0D);
        double strength = section.getDouble("strength", 0.0D);
        double critChance = section.getDouble("crit-chance", section.getDouble("crit_chance", 0.0D));
        double critDamage = section.getDouble("crit-damage", section.getDouble("crit_damage", 0.0D));
        double intelligence = section.getDouble("intelligence", 0.0D);
        double speed = section.getDouble("speed", 0.0D);
        double attackSpeed = section.getDouble("attack-speed", section.getDouble("attack_speed", 0.0D));
        double ferocity = section.getDouble("ferocity", 0.0D);
        double magicFind = section.getDouble("magic-find", section.getDouble("magic_find", 0.0D));
        double petLuck = section.getDouble("pet-luck", section.getDouble("pet_luck", 0.0D));
        double seaCreatureChance = section.getDouble("sea-creature-chance", section.getDouble("sea_creature_chance", 0.0D));
        double trueDefense = section.getDouble("true-defense", section.getDouble("true_defense", 0.0D));
        double abilityDamage = section.getDouble("ability-damage", section.getDouble("ability_damage", 0.0D));
        
        return new PetStatBonuses(health, defense, strength, critChance, critDamage, intelligence, 
                speed, attackSpeed, ferocity, magicFind, petLuck, seaCreatureChance, trueDefense, abilityDamage);
    }


    private org.bukkit.attribute.Attribute parseAttribute(String key) {
        if (key == null || key.isBlank()) return null;
        String upper = key.toUpperCase(Locale.ROOT);
        for (Attribute attribute : Registry.ATTRIBUTE) {
            String namespaced = Registry.ATTRIBUTE.getKey(attribute).getKey();
            if (namespaced.equalsIgnoreCase(upper) || namespaced.equalsIgnoreCase("generic_" + upper.toLowerCase(Locale.ROOT))) {
                return attribute;
            }
        }
        return null;
    }

    private boolean looksLikeEquippedData(YamlConfiguration yaml) {
        for (String key : yaml.getKeys(false)) {
            if (isUuid(key)) {
                return true;
            }
        }
        return false;
    }

    private boolean isUuid(String key) {
        return key != null && key.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }

    private PlayerProfile buildHeadProfile(String textureUrl) {
        try {
            java.net.URL url = new java.net.URL(textureUrl);
            PlayerProfile profile = Bukkit.createProfile(java.util.UUID.nameUUIDFromBytes(textureUrl.getBytes()));
            var textures = profile.getTextures();
            textures.setSkin(url);
            profile.setTextures(textures);
            return profile;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to build head profile for pet texture: " + e.getMessage());
            return null;
        }
    }

    public int getLevel(Player player, String petId) {
        io.papermc.Grivience.skyblock.profile.SkyBlockProfile profile = requireProfile(player);
        if (profile == null || petId == null) {
            return 1;
        }
        maybeMigrateLegacy(player, profile);
        String normalizedId = petId.toLowerCase(Locale.ROOT);
        PetDefinition def = pets.get(normalizedId);
        if (def == null) {
            return 1;
        }

        io.papermc.Grivience.skyblock.profile.SkyBlockProfile.PetData data = profile.getPetData().get(normalizedId);
        if (data == null) {
            return 1;
        }

        long total = normalizeTotalExp(def, data);
        int level = PetXpTable.levelForTotalExp(def.rarity(), total, def.maxLevel());
        data.setLevel(level);
        data.setXp(total);
        return level;
    }

    public int getXp(Player player, String petId) {
        io.papermc.Grivience.skyblock.profile.SkyBlockProfile profile = requireProfile(player);
        if (profile == null || petId == null) {
            return 0;
        }
        maybeMigrateLegacy(player, profile);
        String normalizedId = petId.toLowerCase(Locale.ROOT);
        PetDefinition def = pets.get(normalizedId);
        if (def == null) {
            return 0;
        }

        io.papermc.Grivience.skyblock.profile.SkyBlockProfile.PetData data = profile.getPetData().get(normalizedId);
        if (data == null) {
            return 0;
        }

        long total = normalizeTotalExp(def, data);
        data.setXp(total);
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    public void addSkillXp(Player player, PetSkillType skillType, long skillXp) {
        if (!expEnabled) {
            return;
        }
        if (player == null || skillType == null || skillXp <= 0L) {
            return;
        }
        // Gaining Taming XP does not grant pet EXP on Skyblock.
        if (skillType == PetSkillType.TAMING) {
            return;
        }

        String petId = equippedPet(player);
        if (petId == null) {
            return;
        }
        PetDefinition def = pets.get(petId);
        if (def == null) {
            return;
        }

        double petXp = Math.max(0.0D, (double) skillXp);
        if (skillType == PetSkillType.MINING || skillType == PetSkillType.FISHING) {
            petXp *= miningFishingXpMultiplier;
        }

        PetSkillType petSkill = def.skillType();
        if (petSkill != null && petSkill != PetSkillType.ALL && petSkill != skillType) {
            if (skillType == PetSkillType.ALCHEMY || skillType == PetSkillType.ENCHANTING) {
                petXp *= nonMatchingAlchemyEnchantingMultiplier;
            } else {
                petXp *= nonMatchingSkillMultiplier;
            }
        }

        long awarded = (long) Math.floor(petXp);
        if (awarded <= 0L) {
            return;
        }
        addXp(player, awarded);

        // 100% Accurate Hypixel Skyblock: Taming XP gain
        // Gain 1/4 of the pet EXP as Taming Skill XP.
        if (levelManager != null) {
            levelManager.awardCategoryXp(player, "taming", awarded / 4, "Pet Experience", false);
        }
    }

    public void addXp(Player player, int amount) {
        addXp(player, (long) amount);
    }

    public void addXp(Player player, long amount) {
        if (player == null || amount <= 0L) {
            return;
        }

        io.papermc.Grivience.skyblock.profile.SkyBlockProfile profile = requireProfile(player);
        if (profile == null) {
            return;
        }
        maybeMigrateLegacy(player, profile);

        String petId = equippedPet(player);
        if (petId == null) {
            return;
        }

        PetDefinition def = pets.get(petId);
        if (def == null) {
            return;
        }

        io.papermc.Grivience.skyblock.profile.SkyBlockProfile.PetData data = profile.getPetData().get(petId);
        if (data == null) {
            data = new io.papermc.Grivience.skyblock.profile.SkyBlockProfile.PetData(petId);
            profile.setPetData(petId, data);
        }

        int maxLevel = Math.max(1, Math.min(def.maxLevel(), PetXpTable.maxTableLevel()));
        long capTotal = PetXpTable.totalExpForLevel(def.rarity(), maxLevel);

        long beforeTotal = normalizeTotalExp(def, data);
        int beforeLevel = PetXpTable.levelForTotalExp(def.rarity(), beforeTotal, maxLevel);
        if (beforeLevel >= maxLevel || beforeTotal >= capTotal) {
            data.setLevel(maxLevel);
            data.setXp(capTotal);
            return;
        }

        long afterTotal = Math.min(capTotal, beforeTotal + amount);
        int afterLevel = PetXpTable.levelForTotalExp(def.rarity(), afterTotal, maxLevel);
        data.setXp(afterTotal);
        data.setLevel(afterLevel);

        if (afterLevel > beforeLevel) {
            for (int lvl = beforeLevel + 1; lvl <= afterLevel; lvl++) {
                player.sendMessage(ChatColor.GOLD + "Your pet " + ChatColor.AQUA + def.displayName() + ChatColor.GOLD + " leveled up to " + ChatColor.GREEN + lvl + ChatColor.GOLD + "!");
            }
        }

        saveProfile(profile);
        applyCurrent(player);
    }

    public long xpForNext(PetDefinition def, int level) {
        if (def == null) {
            return 0L;
        }
        int maxLevel = Math.max(1, Math.min(def.maxLevel(), PetXpTable.maxTableLevel()));
        int clamped = Math.max(1, Math.min(level, maxLevel));
        if (clamped >= maxLevel) {
            return 0L;
        }
        return PetXpTable.expToNextLevel(def.rarity(), clamped);
    }

    public PetProgress progress(Player player, String petId) {
        if (player == null || petId == null) {
            return new PetProgress(1, 0L, 0L, 0L);
        }
        String normalizedId = petId.toLowerCase(Locale.ROOT);
        PetDefinition def = pets.get(normalizedId);
        if (def == null) {
            return new PetProgress(1, 0L, 0L, 0L);
        }

        io.papermc.Grivience.skyblock.profile.SkyBlockProfile profile = requireProfile(player);
        if (profile == null) {
            return new PetProgress(1, 0L, 0L, 0L);
        }
        maybeMigrateLegacy(player, profile);

        io.papermc.Grivience.skyblock.profile.SkyBlockProfile.PetData data = profile.getPetData().get(normalizedId);
        if (data == null) {
            return new PetProgress(1, 0L, 0L, xpForNext(def, 1));
        }

        long total = normalizeTotalExp(def, data);
        int maxLevel = Math.max(1, Math.min(def.maxLevel(), PetXpTable.maxTableLevel()));
        long capTotal = PetXpTable.totalExpForLevel(def.rarity(), maxLevel);
        total = Math.min(capTotal, Math.max(0L, total));

        int level = PetXpTable.levelForTotalExp(def.rarity(), total, maxLevel);
        long totalAtLevel = PetXpTable.totalExpForLevel(def.rarity(), level);
        long intoLevel = Math.max(0L, total - totalAtLevel);
        long toNext = level >= maxLevel ? 0L : PetXpTable.expToNextLevel(def.rarity(), level);
        return new PetProgress(level, total, intoLevel, toNext);
    }

    private long normalizeTotalExp(PetDefinition def, io.papermc.Grivience.skyblock.profile.SkyBlockProfile.PetData data) {
        if (def == null || data == null) {
            return 0L;
        }
        int maxLevel = Math.max(1, Math.min(def.maxLevel(), PetXpTable.maxTableLevel()));
        int storedLevel = Math.max(1, Math.min(data.getLevel(), maxLevel));
        long storedXp = Math.max(0L, Math.round(data.getXp()));

        long totalAtLevel = PetXpTable.totalExpForLevel(def.rarity(), storedLevel);
        // Legacy formats stored "xp into current level" rather than cumulative total.
        if (storedLevel > 1 && storedXp < totalAtLevel && storedXp <= 50_000L) {
            return totalAtLevel + storedXp;
        }
        return storedXp;
    }

    private double clampFinite(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private io.papermc.Grivience.skyblock.profile.SkyBlockProfile requireProfile(Player player) {
        if (player == null) {
            return null;
        }
        io.papermc.Grivience.skyblock.profile.ProfileManager manager = plugin.getProfileManager();
        if (manager == null) {
            return null;
        }
        io.papermc.Grivience.skyblock.profile.SkyBlockProfile profile = manager.getSelectedProfile(player);
        if (profile != null) {
            return profile;
        }
        if (manager.getPlayerProfiles(player).isEmpty()) {
            manager.createProfile(player, "Default");
            profile = manager.getSelectedProfile(player);
        }
        return profile;
    }

    private void saveProfile(io.papermc.Grivience.skyblock.profile.SkyBlockProfile profile) {
        if (profile == null) {
            return;
        }
        io.papermc.Grivience.skyblock.profile.ProfileManager manager = plugin.getProfileManager();
        if (manager != null) {
            manager.saveProfile(profile);
        }
    }

    private void maybeMigrateLegacy(Player player, io.papermc.Grivience.skyblock.profile.SkyBlockProfile profile) {
        if (player == null || profile == null) {
            return;
        }
        if (!legacyPetDataFile.exists()) {
            return;
        }
        // Only migrate once per profile (avoid re-reading legacy file constantly).
        if (!profile.getPetData().isEmpty() || (profile.getEquippedPet() != null && !profile.getEquippedPet().isBlank())) {
            return;
        }

        YamlConfiguration yaml;
        try {
            yaml = YamlConfiguration.loadConfiguration(legacyPetDataFile);
        } catch (Exception ignored) {
            return;
        }

        String base = player.getUniqueId().toString();
        if (!yaml.contains(base)) {
            return;
        }

        List<String> owned = yaml.getStringList(base + ".owned");
        if (owned != null) {
            for (String raw : owned) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String petId = raw.toLowerCase(Locale.ROOT);
                if (!pets.containsKey(petId)) {
                    continue;
                }
                profile.setPetData(petId, new io.papermc.Grivience.skyblock.profile.SkyBlockProfile.PetData(petId));
            }
        }

        ConfigurationSection levelSec = yaml.getConfigurationSection(base + ".levels");
        if (levelSec != null) {
            for (String key : levelSec.getKeys(false)) {
                String petId = key == null ? null : key.toLowerCase(Locale.ROOT);
                if (petId == null || !profile.getPetData().containsKey(petId)) {
                    continue;
                }
                io.papermc.Grivience.skyblock.profile.SkyBlockProfile.PetData data = profile.getPetData().get(petId);
                if (data == null) {
                    continue;
                }
                int level = Math.max(1, levelSec.getInt(key, 1));
                data.setLevel(level);
            }
        }

        ConfigurationSection xpSec = yaml.getConfigurationSection(base + ".xp");
        if (xpSec != null) {
            for (String key : xpSec.getKeys(false)) {
                String petId = key == null ? null : key.toLowerCase(Locale.ROOT);
                if (petId == null || !profile.getPetData().containsKey(petId)) {
                    continue;
                }
                io.papermc.Grivience.skyblock.profile.SkyBlockProfile.PetData data = profile.getPetData().get(petId);
                if (data == null) {
                    continue;
                }
                PetDefinition def = pets.get(petId);
                if (def == null) {
                    continue;
                }
                int maxLevel = Math.max(1, Math.min(def.maxLevel(), PetXpTable.maxTableLevel()));
                int storedLevel = Math.max(1, Math.min(data.getLevel(), maxLevel));
                long intoLevel = Math.max(0L, xpSec.getLong(key, 0L));
                long totalAtLevel = PetXpTable.totalExpForLevel(def.rarity(), storedLevel);
                long capTotal = PetXpTable.totalExpForLevel(def.rarity(), maxLevel);
                long total = Math.min(capTotal, totalAtLevel + intoLevel);
                data.setXp(total);
                data.setLevel(PetXpTable.levelForTotalExp(def.rarity(), total, maxLevel));
            }
        }

        String equipped = yaml.getString(base + ".equipped", null);
        if (equipped != null && !equipped.isBlank()) {
            String petId = equipped.toLowerCase(Locale.ROOT);
            if (profile.getPetData().containsKey(petId)) {
                profile.setEquippedPet(petId);
                io.papermc.Grivience.skyblock.profile.SkyBlockProfile.PetData data = profile.getPetData().get(petId);
                if (data != null) {
                    data.setActive(true);
                }
            }
        }

        // Remove migrated player entry to avoid repeated migrations.
        yaml.set(base, null);
        try {
            yaml.save(legacyPetDataFile);
        } catch (IOException ignored) {
        }

        saveProfile(profile);
    }
}

