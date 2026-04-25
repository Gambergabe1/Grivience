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
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.net.URLEncoder;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.util.Vector;

public final class PetManager implements Listener {
    private static final String PET_MODIFIER_NAME_PREFIX = "grivience_pet_";
    private static final int DEFAULT_LEGACY_EFFECT_CLEANUP_MIN_DURATION_TICKS = 20 * 60 * 8;

    private final GriviencePlugin plugin;
    private final NamespacedKey petIdKey;
    private final NamespacedKey petXpKey;
    private final NamespacedKey petAppliedEffectsKey;
    private final Map<String, PetDefinition> pets = new HashMap<>();
    private final File petsFile;
    private final File legacyPetDataFile;
    private final Map<String, PlayerProfile> textureCache = new HashMap<>();
    private io.papermc.Grivience.stats.SkyblockLevelManager levelManager;

    // Tracks which potion effects were last applied by the pet system per player (so we don't
    // accidentally remove unrelated effects from other systems).
    private final Map<UUID, Set<PotionEffectType>> appliedPetEffects = new HashMap<>();

    // Visual pet tracking
    private final Map<UUID, ArmorStand> activeVisualPets = new HashMap<>();
    private org.bukkit.scheduler.BukkitTask visualTask;

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
        this.petXpKey = new NamespacedKey(plugin, "pet_xp");
        this.petAppliedEffectsKey = new NamespacedKey(plugin, "pet_effect_types");
        this.petsFile = new File(plugin.getDataFolder(), "pets.yml");
        this.legacyPetDataFile = new File(plugin.getDataFolder(), "pet-data.yml");
        ensureDefaultPetsFile();
        loadPets();
        reloadSettings();
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startVisualTask();
    }

    private void startVisualTask() {
        if (visualTask != null) visualTask.cancel();
        visualTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::updateVisualPets, 1L, 1L);
    }

    private void updateVisualPets() {
        for (Map.Entry<UUID, ArmorStand> entry : activeVisualPets.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            ArmorStand pet = entry.getValue();

            if (player == null || !player.isOnline() || !pet.isValid() || !pet.getWorld().equals(player.getWorld())) {
                continue;
            }

            // Verify helmet (ensure texture is always displayed)
            if (pet.getEquipment().getHelmet() == null || pet.getEquipment().getHelmet().getType() != Material.PLAYER_HEAD) {
                String equipped = equippedPet(player);
                if (equipped != null) {
                    PetDefinition def = pets.get(equipped);
                    if (def != null) {
                        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                        applyTexture(head, def.headTexture());
                        pet.getEquipment().setHelmet(head);
                    }
                }
            }

            Location target = player.getLocation().clone().add(0, 1.2, 0);            
            // Offset to the side and slightly behind
            Vector direction = player.getLocation().getDirection().setY(0).normalize();
            Vector side = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
            
            // Hover position (oscillating slightly)
            double time = System.currentTimeMillis() / 1000.0;
            double hover = Math.sin(time * 2.0) * 0.15;
            
            Location hoverLoc = target.clone()
                    .add(direction.multiply(-0.8))
                    .add(side.multiply(0.8))
                    .add(0, hover, 0);

            // Smooth interpolation
            Location current = pet.getLocation();
            Vector vel = hoverLoc.toVector().subtract(current.toVector());
            double dist = vel.length();
            
            if (dist > 8.0) {
                pet.teleport(hoverLoc);
            } else if (dist > 0.05) {
                // Move towards hover location
                pet.teleport(current.add(vel.multiply(0.15)));
            }
            
            // Look at player or in player direction
            Location petLoc = pet.getLocation();
            petLoc.setDirection(player.getLocation().getDirection());
            pet.teleport(petLoc);
        }
    }

    public void setLevelManager(io.papermc.Grivience.stats.SkyblockLevelManager levelManager) {
        this.levelManager = levelManager;
    }

    public void reloadPets() {
        shutdown();
        loadPets();
        reloadSettings();
        respawnAllVisualPets();
    }

    public void respawnAllVisualPets() {
        startVisualTask();
        for (Player player : Bukkit.getOnlinePlayers()) {
            String equipped = equippedPet(player);
            if (equipped != null) {
                spawnVisualPet(player, equipped);
            }
        }
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
            plugin.getServer().removeRecipe(key);
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
                max.abilityDamage() * scale,
                max.farmingFortune() * scale
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
                skull.setOwnerProfile(profile);
            }
        }
        
        PetProgress progress = viewer == null ? new PetProgress(1, 0L, 0L, xpForNext(def, 1)) : progress(viewer, def.id());
        int level = progress.level();

        // 100% Accurate Hypixel Skyblock Name Format: [Lvl X] [Pet Name]
        String rawDisplayName = "&7[Lvl " + level + "] " + def.rarity().color() + def.displayName();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(rawDisplayName)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        
        // Skill Type Line (e.g. Combat Pet)
        lore.add(Component.text(capitalize(def.skillType().name()) + " Pet", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        // Stats section (Dynamic based on level)
        PetStatBonuses stats = def.stats();
        if (stats != null) {
            double scale = (double) level / 100.0;
            appendStatComponent(lore, "Health", stats.health() * scale, NamedTextColor.RED, "");
            appendStatComponent(lore, "Defense", stats.defense() * scale, NamedTextColor.GREEN, "");
            appendStatComponent(lore, "Strength", stats.strength() * scale, NamedTextColor.RED, "");
            appendStatComponent(lore, "Crit Chance", stats.critChance() * scale, NamedTextColor.BLUE, "%");
            appendStatComponent(lore, "Crit Damage", stats.critDamage() * scale, NamedTextColor.BLUE, "%");
            appendStatComponent(lore, "Intelligence", stats.intelligence() * scale, NamedTextColor.AQUA, "");
            appendStatComponent(lore, "Speed", stats.speed() * scale, NamedTextColor.WHITE, "");
            appendStatComponent(lore, "Attack Speed", stats.attackSpeed() * scale, NamedTextColor.YELLOW, "");
            appendStatComponent(lore, "Ferocity", stats.ferocity() * scale, NamedTextColor.RED, "");
            appendStatComponent(lore, "Magic Find", stats.magicFind() * scale, NamedTextColor.AQUA, "");
            appendStatComponent(lore, "Pet Luck", stats.petLuck() * scale, NamedTextColor.LIGHT_PURPLE, "");
            appendStatComponent(lore, "Sea Creature Chance", stats.seaCreatureChance() * scale, NamedTextColor.DARK_AQUA, "%");
            appendStatComponent(lore, "True Defense", stats.trueDefense() * scale, NamedTextColor.WHITE, "");
            appendStatComponent(lore, "Ability Damage", stats.abilityDamage() * scale, NamedTextColor.RED, "%");
            lore.add(Component.empty());
        }

        // Abilities section (Dynamic based on level and rarity)
        for (PetDefinition.PetAbility ability : def.abilities()) {
            if (def.rarity().ordinal() < ability.minRarity().ordinal()) continue;
            
            lore.add(Component.text(ability.name(), NamedTextColor.GOLD)
                    .decoration(TextDecoration.ITALIC, false));
            double val = ability.getValue(level);
            for (String descLine : ability.description()) {
                String processed = "&7" + descLine.replace("{value}", String.format(Locale.US, "%.1f", val));
                lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(processed)
                        .decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.empty());
        }

        // Pet Item Slot (Accurate to Skyblock)
        lore.add(Component.text("Held Item: ", NamedTextColor.GRAY)
                .append(Component.text("None", NamedTextColor.RED))
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        // XP Bar
        if (level < def.maxLevel()) {
            long into = progress.expIntoLevel();
            long toNext = progress.expToNextLevel();
            double pct = (double) into / toNext * 100.0;
            
            lore.add(Component.text("Progress to Level " + (level + 1) + ": ", NamedTextColor.GRAY)
                    .append(Component.text(String.format(Locale.US, "%.1f%%", pct), NamedTextColor.YELLOW))
                    .decoration(TextDecoration.ITALIC, false));
            
            int bars = 20;
            int filled = (int) (pct / 100.0 * bars);
            StringBuilder barStr = new StringBuilder("&2");
            for (int i = 0; i < bars; i++) {
                if (i == filled) barStr.append("&8");
                barStr.append("-");
            }
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(barStr.toString())
                    .decoration(TextDecoration.ITALIC, false));
            
            lore.add(Component.text(String.format(Locale.US, "%,d", into), NamedTextColor.YELLOW)
                    .append(Component.text("/", NamedTextColor.GOLD))
                    .append(Component.text(String.format(Locale.US, "%,d", toNext), NamedTextColor.YELLOW))
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("MAX LEVEL", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        
        if (isInGui && viewer != null) {
            String equipped = equippedPet(viewer);
            lore.add(Component.empty());
            if (id.equalsIgnoreCase(equipped)) {
                lore.add(Component.text("Click to despawn!", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text("Click to summon!", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
            }
        } else {
            lore.add(Component.empty());
            lore.add(Component.text("Right-click to add to pets menu!", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Keep as item to trade.", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        lore.add(Component.text(def.rarity().name(), def.rarity().adventureColor())
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(petIdKey, PersistentDataType.STRING, def.id());
        if (!isInGui) {
            meta.getPersistentDataContainer().set(petXpKey, PersistentDataType.LONG, progress.totalExp());
        }
        item.setItemMeta(meta);
        return item;
    }

    private void appendStatComponent(List<Component> lore, String name, double val, net.kyori.adventure.text.format.TextColor color, String suffix) {
        if (val == 0) return;
        String sign = val > 0 ? "+" : "";
        Component comp = Component.text(name + ": ", NamedTextColor.GRAY)
                .append(Component.text(sign + String.format(Locale.US, "%.1f", val) + suffix, color))
                .decoration(TextDecoration.ITALIC, false);
        lore.add(comp);
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

    public long getStoredXp(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0L;
        Long xp = item.getItemMeta().getPersistentDataContainer().get(petXpKey, PersistentDataType.LONG);
        return xp != null ? xp : 0L;
    }

    public void setPetXp(Player player, String petId, long xp) {
        if (player == null || petId == null || xp < 0L) return;
        io.papermc.Grivience.skyblock.profile.SkyBlockProfile profile = requireProfile(player);
        if (profile == null) return;
        String normalizedId = petId.toLowerCase(Locale.ROOT);
        PetDefinition def = pets.get(normalizedId);
        if (def == null) return;
        
        io.papermc.Grivience.skyblock.profile.SkyBlockProfile.PetData data = profile.getPetData().get(normalizedId);
        if (data == null) {
            data = new io.papermc.Grivience.skyblock.profile.SkyBlockProfile.PetData(normalizedId);
            profile.setPetData(normalizedId, data);
        }
        
        int maxLevel = Math.max(1, Math.min(def.maxLevel(), PetXpTable.maxTableLevel()));
        long capTotal = PetXpTable.totalExpForLevel(def.rarity(), maxLevel);
        long actualXp = Math.min(xp, capTotal);
        
        data.setXp(actualXp);
        data.setLevel(PetXpTable.levelForTotalExp(def.rarity(), actualXp, maxLevel));
        saveProfile(profile);
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
            despawnVisualPet(player);
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
            spawnVisualPet(player, normalizedId);
        }
        saveProfile(profile);
    }

    public void spawnVisualPet(Player player, String petId) {
        despawnVisualPet(player);
        
        PetDefinition def = pets.get(petId);
        if (def == null) return;

        Location spawnLoc = player.getLocation().add(0, 1, 0);
        ArmorStand stand = player.getWorld().spawn(spawnLoc, ArmorStand.class, as -> {
            as.setVisible(false);
            as.setGravity(false);
            as.setSmall(true);
            as.setMarker(false); // Marker causes nametag to be at feet
            as.setInvulnerable(true);
            as.setBasePlate(false);
            as.setArms(false);
            as.setCanPickupItems(false);
            as.setCustomNameVisible(true);
            as.setPersistent(false);

            as.getPersistentDataContainer().set(petIdKey, org.bukkit.persistence.PersistentDataType.STRING, petId);
            
            // Disable ticking if possible for performance
            try {
                as.getClass().getMethod("setCanTick", boolean.class).invoke(as, false);
            } catch (Exception ignored) {}

            // Set custom name with level and rarity color
            int level = getLevel(player, petId);
            String rawName = "&7[Lvl " + level + "] " + def.rarity().color() + player.getName() + "'s " + ChatColor.stripColor(def.displayName());
            as.customName(LegacyComponentSerializer.legacyAmpersand().deserialize(rawName));
            
            // Set head texture
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            applyTexture(head, def.headTexture());
            as.getEquipment().setHelmet(head);
        });

        activeVisualPets.put(player.getUniqueId(), stand);
    }

    public void despawnVisualPet(Player player) {
        ArmorStand stand = activeVisualPets.remove(player.getUniqueId());
        if (stand != null) {
            stand.remove();
        }
    }

    public void shutdown() {
        if (visualTask != null) visualTask.cancel();
        for (ArmorStand stand : activeVisualPets.values()) {
            if (stand != null) stand.remove();
        }
        activeVisualPets.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String equipped = equippedPet(event.getPlayer());
        if (equipped != null) {
            // Delay slightly to ensure world is loaded and profile is ready
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (event.getPlayer().isOnline()) {
                    spawnVisualPet(event.getPlayer(), equipped);
                }
            }, 20L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        despawnVisualPet(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        String equipped = equippedPet(event.getPlayer());
        if (equipped != null) {
            spawnVisualPet(event.getPlayer(), equipped);
        }
    }

    @EventHandler
    public void onChunkLoad(org.bukkit.event.world.ChunkLoadEvent event) {
        for (org.bukkit.entity.Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof ArmorStand as) {
                if (as.getPersistentDataContainer().has(petIdKey, org.bukkit.persistence.PersistentDataType.STRING)) {
                    if (!activeVisualPets.containsValue(as)) {
                        as.remove();
                    }
                } else if (!as.isVisible() && as.isSmall() && as.isInvulnerable() && !as.hasGravity()) {
                    net.kyori.adventure.text.Component name = as.customName();
                    if (name != null) {
                        String legacyName = LegacyComponentSerializer.legacyAmpersand().serialize(name);
                        if (legacyName.contains("[Lvl ") && legacyName.contains("'s ")) {
                            if (!activeVisualPets.containsValue(as)) {
                                as.remove();
                            }
                        }
                    }
                }
            }
        }
    }

    public double getAbilityMultiplier(Player attacker, Entity victim) {
        String petId = equippedPet(attacker);
        if (petId == null) return 1.0D;

        if (petId.equalsIgnoreCase("dragon")) {
            // Dragon pet: Dragon Claw ability
            if (victim instanceof org.bukkit.entity.EnderDragon || victim instanceof org.bukkit.entity.EnderCrystal) {
                int level = getLevel(attacker, "dragon");
                return 1.0 + (10.0 + (level - 1) * 0.40D) / 100.0;
            }
        } else if (petId.equalsIgnoreCase("enderman")) {
            // Ender Affinity: Damage to End mobs
            if (victim.getType().name().contains("ENDER") || victim.getType().name().contains("SHULKER")) {
                int level = getLevel(attacker, "enderman");
                return 1.0 + (10.0 + (level - 1) * 0.30D) / 100.0;
            }
        } else if (petId.equalsIgnoreCase("lion")) {
            // King of the Jungle: +5-20% Damage
            int level = getLevel(attacker, "lion");
            return 1.0 + (5.0 + (level - 1) * 0.15D) / 100.0;
        } else if (petId.equalsIgnoreCase("wither_skeleton")) {
            // Death's Touch: +10-30% damage to skeletons
            if (victim.getType() == org.bukkit.entity.EntityType.SKELETON || victim.getType() == org.bukkit.entity.EntityType.WITHER_SKELETON) {
                int level = getLevel(attacker, "wither_skeleton");
                return 1.0 + (10.0 + (level - 1) * 0.20D) / 100.0;
            }
        } else if (petId.equalsIgnoreCase("blaze") && attacker.getWorld().getEnvironment() == org.bukkit.World.Environment.NETHER) {
            // Nether Affinity: +5-20% damage in nether
            int level = getLevel(attacker, "blaze");
            return 1.0 + (5.0 + (level - 1) * 0.15D) / 100.0;
        }
        return 1.0D;
    }

    public double getIncomingDamageMultiplier(Player player) {
        String petId = equippedPet(player);
        if (petId == null) return 1.0D;

        if (petId.equalsIgnoreCase("turtle")) {
            // Turtle pet: Shell Shield ability
            int level = getLevel(player, "turtle");
            double reduction = (5.0 + (level - 1) * 0.15D) / 100.0;
            return 1.0 - reduction;
        } else if (petId.equalsIgnoreCase("blaze") && player.getWorld().getEnvironment() == org.bukkit.World.Environment.NETHER) {
            // Nether Affinity: +10% Damage reduction in nether
            int level = getLevel(player, "blaze");
            double reduction = (10.0 + (level - 1) * 0.10D) / 100.0;
            return 1.0 - reduction;
        }
        return 1.0D;
    }

    public double getManaCostMultiplier(Player player) {
        String petId = equippedPet(player);
        if (petId == null) return 1.0D;

        if (petId.equalsIgnoreCase("sheep")) {
            // Mana Saver: 5-20% reduction
            int level = getLevel(player, "sheep");
            double reduction = (5.0 + (level - 1) * 0.15D) / 100.0;
            return 1.0 - reduction;
        }
        return 1.0D;
    }

    public double getFarmingFortune(Player player) {
        PetStatBonuses bonuses = equippedStatBonuses(player);
        return bonuses.farmingFortune();
    }

    public double getMiningSpeed(Player player) {
        String petId = equippedPet(player);
        if (petId == null) return 0.0D;

        if (petId.equalsIgnoreCase("mithril_golem")) {
            // Mithril Affinity: +20-100 Mining Speed
            int level = getLevel(player, "mithril_golem");
            return 20.0 + (level - 1) * 0.81D; // approx
        }
        return 0.0D;
    }

    public double getSkillXpMultiplier(Player player, io.papermc.Grivience.skills.SkyblockSkill skill) {
        String petId = equippedPet(player);
        if (petId == null) return 1.0D;

        if (petId.equalsIgnoreCase("monkey") && skill == io.papermc.Grivience.skills.SkyblockSkill.FORAGING) {
            // Tree Grinder: +10-50% Foraging XP
            int level = getLevel(player, "monkey");
            return 1.0 + (10.0 + (level - 1) * 0.40D) / 100.0;
        } else if (petId.equalsIgnoreCase("squid") && skill == io.papermc.Grivience.skills.SkyblockSkill.FISHING) {
            // Fishing Luck: +10-30% Fishing XP
            int level = getLevel(player, "squid");
            return 1.0 + (10.0 + (level - 1) * 0.20D) / 100.0;
        }
        return 1.0D;
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
                if (headUrl == null || headUrl.isBlank()) {
                    headUrl = fallback.headTexture();
                }
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

        // Merge any missing default pets
        for (Map.Entry<String, PetDefinition> entry : defaults.entrySet()) {
            if (!pets.containsKey(entry.getKey())) {
                pets.put(entry.getKey(), entry.getValue());
            }
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
                new PetStatBonuses(20.0D, 0.0D, 25.0D, 0.0D, 0.0D, 0.0D, 0, 0, 0, 0, 0, 0, 0, 0, 0),
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
                new PetStatBonuses(0.0D, 0.0D, 0.0D, 0.0D, 20.0D, 0.0D, 0, 0, 0, 0, 0, 0, 0, 0, 0),
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
                new PetStatBonuses(10.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0, 0, 0, 0, 0, 0, 0, 0, 0),
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
                new PetStatBonuses(10.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0, 0, 0, 0, 0, 0, 0, 0, 50.0D),
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
                new PetStatBonuses(20.0D, 40.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                PetSkillType.COMBAT,
                PetRarity.EPIC,
                100,
                List.of(new PetDefinition.PetAbility("Shell Shield", List.of("&7Reduces damage taken by &9{value}%."), 5.0, 20.0, PetRarity.EPIC))
        ));
        map.put("bee", new PetDefinition("bee",
                "&eBee Keeper",
                Material.PLAYER_HEAD,
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODkwZWZjOGFhNWFhYzI3NDJhYjA0NTY0NTAxYzRmNTZlOWM3MTY5YjI3MmQwNTMwZjc4YzIwOTg2NTU3NzkyOCJ9fX0=",
                List.of("&7Haste and luck for farming"),
                List.of(),
                Map.of(),
                1.15D,
                new PetStatBonuses(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 10.0D, 0, 0, 0, 0, 0, 0, 0, 0, 10.0D),
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
                new PetStatBonuses(10.0D, 0.0D, 30.0D, 5.0D, 0.0D, 0.0D, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                PetSkillType.COMBAT,
                PetRarity.LEGENDARY,
                100,
                List.of(new PetDefinition.PetAbility("Dragon Claw", List.of("&7Increases damage against dragons by &c{value}%."), 10.0, 50.0, PetRarity.LEGENDARY))
        ));
        map.put("enderman", new PetDefinition("enderman",
                "&dEnderman",
                Material.PLAYER_HEAD,
                "http://textures.minecraft.net/texture/902a2c496c14109489569722877a3d1354b673278839077229e612c62c9c7f1a",
                List.of("&7A void-dweller companion."),
                List.of(),
                Map.of(),
                1.0D,
                new PetStatBonuses(0, 0, 0, 0, 75.0D, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                PetSkillType.COMBAT,
                PetRarity.LEGENDARY,
                100,
                List.of(new PetDefinition.PetAbility("Ender Affinity", List.of("&7Increases damage against Ender mobs by &c{value}%."), 10.0, 40.0, PetRarity.LEGENDARY))
        ));
        map.put("elephant", new PetDefinition("elephant",
                "&6Elephant",
                Material.PLAYER_HEAD,
                "http://textures.minecraft.net/texture/719d3637e17f22754f9a0c776495df0271ed4c0d024e031a0e052f53ca6",
                List.of("&7The ultimate farming companion."),
                List.of(),
                Map.of(),
                1.20D,
                new PetStatBonuses(100.0D, 0, 0, 0, 0, 75.0D, 0, 0, 0, 0, 0, 0, 0, 0, 150.0D),
                PetSkillType.FARMING,
                PetRarity.LEGENDARY,
                100,
                List.of(new PetDefinition.PetAbility("Trunk Efficiency", List.of("&7Grants &6+{value} Farming Fortune."), 30.0, 150.0, PetRarity.LEGENDARY))
        ));
        map.put("blue_whale", new PetDefinition("blue_whale",
                "&bBlue Whale",
                Material.PLAYER_HEAD,
                "http://textures.minecraft.net/texture/95f4e0c0d16568218820f4c3ec33c5e88414b536e26ba7a0d4c810c95843b092",
                List.of("&7A tanky underwater titan."),
                List.of(),
                Map.of(),
                1.0D,
                new PetStatBonuses(200.0D, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                PetSkillType.FISHING,
                PetRarity.LEGENDARY,
                100,
                List.of(new PetDefinition.PetAbility("Bulk", List.of("&7Increases max health by &a{value}%."), 10.0, 20.0, PetRarity.LEGENDARY))
        ));
        map.put("tiger", new PetDefinition("tiger",
                "&6Tiger",
                Material.PLAYER_HEAD,
                "http://textures.minecraft.net/texture/fc42f654b790409a341b5275a5e3f420c29a0937c86518175d409419515582f3",
                List.of("&7A fierce predator."),
                List.of(),
                Map.of(),
                1.0D,
                new PetStatBonuses(0, 0, 15.0D, 5.0D, 50.0D, 0, 0, 0, 30.0D, 0, 0, 0, 0, 0, 0),
                PetSkillType.COMBAT,
                PetRarity.LEGENDARY,
                100,
                List.of(new PetDefinition.PetAbility("Merciless Swipe", List.of("&7Grants &c+{value} Ferocity."), 10.0, 30.0, PetRarity.LEGENDARY))
        ));
        map.put("lion", new PetDefinition("lion",
                "&6Lion",
                Material.PLAYER_HEAD,
                "http://textures.minecraft.net/texture/b9281a70460395350c3886d3896dfa99370776b6d510619946ca39b7d87f941e",
                List.of("&7King of the jungle."),
                List.of(),
                Map.of(),
                1.0D,
                new PetStatBonuses(0, 0, 50.0D, 0, 0, 0, 30.0D, 0, 0, 0, 0, 0, 0, 0, 0),
                PetSkillType.COMBAT,
                PetRarity.LEGENDARY,
                100,
                List.of(new PetDefinition.PetAbility("King of the Jungle", List.of("&7Deal &c+{value}% &7damage to mobs."), 5.0, 20.0, PetRarity.LEGENDARY))
        ));
        map.put("monkey", new PetDefinition("monkey",
                "&6Monkey",
                Material.PLAYER_HEAD,
                "http://textures.minecraft.net/texture/13cd4acc03759021481f44e591741e17d69288417d457614d9b4b025f16f",
                List.of("&7A nimble foraging pet."),
                List.of(),
                Map.of(),
                1.0D,
                new PetStatBonuses(0, 0, 0, 0, 0, 50.0D, 20.0D, 0, 0, 0, 0, 0, 0, 0, 0),
                PetSkillType.FORAGING,
                PetRarity.LEGENDARY,
                100,
                List.of(new PetDefinition.PetAbility("Tree Grinder", List.of("&7Grants &a+{value}% &7Foraging XP."), 10.0, 50.0, PetRarity.LEGENDARY))
        ));
        map.put("silverfish", new PetDefinition("silverfish",
                "&7Silverfish",
                Material.PLAYER_HEAD,
                "http://textures.minecraft.net/texture/da91bcff3ef8734f40f097581163456382103f6f1966e3ef382b6b0c60639912",
                List.of("&7A helpful mining pet."),
                List.of(),
                Map.of(),
                1.0D,
                new PetStatBonuses(0, 50.0D, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 15.0D, 0, 0),
                PetSkillType.MINING,
                PetRarity.RARE,
                100,
                List.of(new PetDefinition.PetAbility("True Defense", List.of("&7Grants &f+{value} True Defense."), 5.0, 15.0, PetRarity.RARE))
        ));
        map.put("ender_dragon", new PetDefinition("ender_dragon",
                "&dEnder Dragon",
                Material.PLAYER_HEAD,
                "http://textures.minecraft.net/texture/7243c330f80e9221160a08e678b7be465d6a866a2e411b590e87f8b9e64e56",
                List.of("&7The ultimate combat pet."),
                List.of(),
                Map.of(),
                1.1D,
                new PetStatBonuses(50, 50, 50, 10, 50, 50, 10, 10, 0, 0, 0, 0, 10, 10, 0),
                PetSkillType.COMBAT,
                PetRarity.LEGENDARY,
                100,
                List.of(new PetDefinition.PetAbility("One with the Dragon", List.of("&7Buffs all stats by &a{value}%."), 1.0, 10.0, PetRarity.LEGENDARY))
        ));
        map.put("black_cat", new PetDefinition("black_cat",
                "&0Black Cat",
                Material.PLAYER_HEAD,
                "http://textures.minecraft.net/texture/e4a1a6873919e8316dfc633a697193b2a54316d2f3c05f013d80d22c95350",
                List.of("&7A lucky omen."),
                List.of(),
                Map.of(),
                1.0D,
                new PetStatBonuses(0, 0, 0, 0, 0, 100, 100, 0, 0, 15.0D, 15.0D, 0, 0, 0, 0),
                PetSkillType.COMBAT,
                PetRarity.LEGENDARY,
                100,
                List.of(new PetDefinition.PetAbility("Looting Luck", List.of("&7Grants &d+{value} Magic Find &7and Pet Luck."), 5.0, 15.0, PetRarity.LEGENDARY))
        ));
        map.put("sheep", new PetDefinition("sheep",
                "&fSheep",
                Material.PLAYER_HEAD,
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjMxZjljY2M2YjNlMzJlY2YxM2I4YTExYWMyOWNkMzNkMThjOTVmYzczZGI4YTY2YzVkNjU3Y2NiOGJlNzAifX19",
                List.of("&7A mage's best friend."),
                List.of(),
                Map.of(),
                1.0D,
                new PetStatBonuses(0, 0, 0, 0, 0, 100.0D, 0, 0, 0, 0, 0, 0, 0, 10.0D, 0),
                PetSkillType.ALCHEMY,
                PetRarity.LEGENDARY,
                100,
                List.of(new PetDefinition.PetAbility("Mana Saver", List.of("&7Reduces mana costs by &b{value}%."), 5.0, 20.0, PetRarity.LEGENDARY))
        ));
        map.put("blaze", new PetDefinition("blaze",
                "&6Blaze",
                Material.PLAYER_HEAD,
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjRiMWI5Y2UyZTlhNmNlOGE5ODVkMzk3NzZlMjkwODA3N2I4MmU2YTMzM2QyYTgxYTQ0MTQzOGVhYjM5ZjhlMSJ9fX0=",
                List.of("&7Harness the power of fire."),
                List.of(),
                Map.of(),
                1.0D,
                new PetStatBonuses(0, 0, 30.0D, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                PetSkillType.COMBAT,
                PetRarity.LEGENDARY,
                100,
                List.of(new PetDefinition.PetAbility("Nether Affinity", List.of("&7Increases stats by &c{value}% &7in the Nether."), 5.0, 20.0, PetRarity.LEGENDARY))
        ));
        map.put("squid", new PetDefinition("squid",
                "&bSquid",
                Material.PLAYER_HEAD,
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMDE0MzNiZTI0MjM2NmFmMTI2ZGE0MzRiODczNWRmMWViNWIzY2IyY2VkZTM5MTQ1OTc0ZTljNDgzNjA3YmFjIn19fQ==",
                List.of("&7Master of the oceans."),
                List.of(),
                Map.of(),
                1.0D,
                new PetStatBonuses(50.0D, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                PetSkillType.FISHING,
                PetRarity.LEGENDARY,
                100,
                List.of(new PetDefinition.PetAbility("Fishing Luck", List.of("&7Grants &b+{value}% &7Fishing XP."), 10.0, 30.0, PetRarity.LEGENDARY))
        ));
        map.put("mithril_golem", new PetDefinition("mithril_golem",
                "&7Mithril Golem",
                Material.PLAYER_HEAD,
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzFiMmRmZThlZDVkZmZjYjE2ODdiYzFjMjQ5YzM5ZGUyZDhhNmMzZDkwMzA1Yzk1ZjZkMWExYTMzMGEwYjEifX19",
                List.of("&7Forged in the deep mines."),
                List.of(),
                Map.of(),
                1.0D,
                new PetStatBonuses(0, 50.0D, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 20.0D, 0, 0),
                PetSkillType.MINING,
                PetRarity.LEGENDARY,
                100,
                List.of(new PetDefinition.PetAbility("Mithril Affinity", List.of("&7Grants &3+{value} Mining Speed."), 20.0, 100.0, PetRarity.LEGENDARY))
        ));
        map.put("wither_skeleton", new PetDefinition("wither_skeleton",
                "&8Wither Skeleton",
                Material.PLAYER_HEAD,
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMmQyNmYyZGZkZjVkZmZjMTZmYzgwODExYTg0MzUyNGRhZjEyYzQ5MzFlYzg1MDMwNzc3NWM2ZDM1YTVmNDZjMSJ9fX0=",
                List.of("&7A withered warrior."),
                List.of(),
                Map.of(),
                1.0D,
                new PetStatBonuses(0, 0, 25.0D, 0, 25.0D, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                PetSkillType.COMBAT,
                PetRarity.LEGENDARY,
                100,
                List.of(new PetDefinition.PetAbility("Death's Touch", List.of("&7Deal &c+{value}% &7damage to Skeletons."), 10.0, 30.0, PetRarity.LEGENDARY))
        ));
        map.put("bal", new PetDefinition("bal",
                "&cBal",
                Material.PLAYER_HEAD,
                "http://textures.minecraft.net/texture/41df60840e69123891460395350c3886d3896dfa99370776b6d510619946ca3",
                List.of("&7Born in the magma."),
                List.of(),
                Map.of(),
                1.0D,
                new PetStatBonuses(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                PetSkillType.COMBAT,
                PetRarity.LEGENDARY,
                100,
                List.of(new PetDefinition.PetAbility("Protective Barrier", List.of("&7Grants immunity to heat in Magma Fields."), 0, 0, PetRarity.LEGENDARY))
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
        double farmingFortune = section.getDouble("farming-fortune", section.getDouble("farming_fortune", 0.0D));

        return new PetStatBonuses(health, defense, strength, critChance, critDamage, intelligence,
                speed, attackSpeed, ferocity, magicFind, petLuck, seaCreatureChance, trueDefense, abilityDamage, farmingFortune);
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

    private PlayerProfile buildHeadProfile(String texture) {
        if (texture == null || texture.isBlank()) return null;
        try {
            // Using a consistent UUID based on the texture string for 100% accurate caching
            UUID uuid = UUID.nameUUIDFromBytes(texture.getBytes(StandardCharsets.UTF_8));
            PlayerProfile profile = Bukkit.createPlayerProfile(uuid, "GriviencePet");
            PlayerTextures textures = profile.getTextures();
            
            String skinUrl = null;

            if (texture.startsWith("http")) {
                skinUrl = texture;
            } else if (isBase64Json(texture)) {
                // Robust extraction from Base64 JSON
                String decoded = new String(Base64.getDecoder().decode(texture), StandardCharsets.UTF_8);
                // Simple pattern matching for "url":"..."
                int urlIdx = decoded.indexOf("\"url\":\"");
                if (urlIdx != -1) {
                    int start = urlIdx + 7;
                    int end = decoded.indexOf("\"", start);
                    if (end != -1) skinUrl = decoded.substring(start, end);
                }
                
                // Fallback for different casing
                if (skinUrl == null) {
                    urlIdx = decoded.indexOf("\"URL\":\"");
                    if (urlIdx != -1) {
                        int start = urlIdx + 7;
                        int end = decoded.indexOf("\"", start);
                        if (end != -1) skinUrl = decoded.substring(start, end);
                    }
                }
            } else if (texture.length() > 64) {
                // Could be a raw Base64 string that isn't JSON, or a long hash
                // If it doesn't contain a dot (like a domain), assume it's a hash or Base64
                if (!texture.contains(".")) {
                    skinUrl = "http://textures.minecraft.net/texture/" + texture;
                } else {
                    skinUrl = texture.startsWith("http") ? texture : "http://" + texture;
                }
            } else {
                // Short string is definitely a hash
                skinUrl = "http://textures.minecraft.net/texture/" + texture;
            }

            if (skinUrl != null) {
                if (!skinUrl.startsWith("http")) skinUrl = "http://" + skinUrl;
                textures.setSkin(new URL(skinUrl));
                profile.setTextures(textures);
                return profile;
            }
            return null;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to build head profile for pet texture: " + e.getMessage());
            return null;
        }
    }

    private boolean isBase64Json(String text) {
        if (text == null || text.length() < 20) return false;
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(text);
            String decoded = new String(decodedBytes, StandardCharsets.UTF_8);
            return (decoded.contains("{") && decoded.contains("}")) && 
                   (decoded.toLowerCase(Locale.ROOT).contains("url") || decoded.toLowerCase(Locale.ROOT).contains("textures"));
        } catch (Exception e) {
            return false;
        }
    }

    private void applyTexture(ItemStack item, String texture) {
        if (item == null || texture == null || texture.isBlank()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta skull) {
            PlayerProfile profile = textureCache.computeIfAbsent(texture, this::buildHeadProfile);
            if (profile != null) {
                skull.setOwnerProfile(profile);
            }
            item.setItemMeta(meta);
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

        double petXp = Math.max(0.0D, (double) skillXp) * 0.25D;
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
        if (plugin.getSkyblockSkillManager() != null) {
            plugin.getSkyblockSkillManager().addXp(player, io.papermc.Grivience.skills.SkyblockSkill.TAMING, awarded / 4);
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

    public io.papermc.Grivience.skyblock.profile.SkyBlockProfile requireProfile(Player player) {
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

    public void saveProfile(io.papermc.Grivience.skyblock.profile.SkyBlockProfile profile) {
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

