package io.papermc.Grivience.command;

import io.papermc.Grivience.item.CustomArmorManager;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.minion.MinionManager;
import io.papermc.Grivience.minion.MinionType;
import io.papermc.Grivience.pet.PetDefinition;
import io.papermc.Grivience.pet.PetManager;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves all live admin-give item sources through one path so commands stay in sync.
 */
public final class AdminItemResolver {
    private static final Pattern MINION_ALIAS_PATTERN = Pattern.compile("^(.+)_minion(?:_t(\\d+))?$");

    private final CustomItemService customItemService;
    private final CustomArmorManager customArmorManager;
    private final PetManager petManager;
    private final MinionManager minionManager;

    public AdminItemResolver(
            CustomItemService customItemService,
            CustomArmorManager customArmorManager,
            PetManager petManager,
            MinionManager minionManager
    ) {
        this.customItemService = customItemService;
        this.customArmorManager = customArmorManager;
        this.petManager = petManager;
        this.minionManager = minionManager;
    }

    public ItemStack resolve(String rawKey, Player viewer) {
        String key = normalize(rawKey);
        if (key == null) {
            return null;
        }

        if (customItemService != null) {
            ItemStack item = customItemService.createItemByKey(key);
            if (item != null) {
                return item;
            }
        }

        ItemStack item = resolveArmor(key);
        if (item != null) {
            return item;
        }

        item = resolvePet(key, viewer);
        if (item != null) {
            return item;
        }

        item = resolveMinion(key);
        if (item != null) {
            return item;
        }

        item = resolveMinionFuel(key);
        if (item != null) {
            return item;
        }

        item = resolveMinionUpgrade(key);
        if (item != null) {
            return item;
        }

        return resolveCustomOnlyMinionIngredient(key);
    }

    public List<String> allKeys() {
        Set<String> keys = new LinkedHashSet<>();
        if (customItemService != null) {
            keys.addAll(customItemService.allItemKeys());
        }
        if (customArmorManager != null) {
            for (String setId : customArmorManager.getArmorSets().keySet()) {
                for (CustomArmorManager.ArmorPieceType piece : CustomArmorManager.ArmorPieceType.values()) {
                    keys.add(normalize(setId + "_" + piece.name()));
                }
            }
        }
        if (petManager != null) {
            for (PetDefinition pet : petManager.allPets()) {
                if (pet != null && pet.id() != null && !pet.id().isBlank()) {
                    String id = normalize(pet.id());
                    keys.add(id);
                    keys.add("pet:" + id);
                }
            }
        }
        if (minionManager != null) {
            for (MinionType type : MinionType.values()) {
                for (int tier = 1; tier <= type.maxTier(); tier++) {
                    keys.add("minion:" + normalize(type.id()) + ":" + tier);
                }
            }
            for (String fuelId : minionManager.fuelIds()) {
                String normalized = normalize(fuelId);
                if (normalized != null) {
                    keys.add("minion-fuel:" + normalized);
                }
            }
            for (String upgradeId : minionManager.upgradeIds()) {
                String normalized = normalize(upgradeId);
                if (normalized != null) {
                    keys.add("minion-upgrade:" + normalized);
                }
            }
            for (Map.Entry<String, MinionManager.IngredientDefinition> entry : MinionManager.getIngredients().entrySet()) {
                MinionManager.IngredientDefinition definition = entry.getValue();
                if (definition == null || !definition.customOnly()) {
                    continue;
                }
                String normalized = normalize(definition.id());
                if (normalized != null) {
                    keys.add("minion-ingredient:" + normalized);
                }
            }
        }
        return keys.stream()
                .filter(Objects::nonNull)
                .sorted()
                .toList();
    }

    public List<String> matchingKeys(String rawPrefix, int limit) {
        String prefix = normalize(rawPrefix);
        List<String> startsWith = new ArrayList<>();
        List<String> contains = new ArrayList<>();
        for (String key : allKeys()) {
            if (prefix == null || prefix.isBlank()) {
                startsWith.add(key);
                continue;
            }
            if (key.startsWith(prefix)) {
                startsWith.add(key);
                continue;
            }
            if (key.contains(prefix)) {
                contains.add(key);
            }
        }

        List<String> out = new ArrayList<>(startsWith.size() + contains.size());
        out.addAll(startsWith);
        out.addAll(contains);
        if (limit > 0 && out.size() > limit) {
            return out.subList(0, limit);
        }
        return out;
    }

    private ItemStack resolveArmor(String key) {
        if (customArmorManager == null) {
            return null;
        }

        String armorKey = stripAnyPrefix(key, "armor:");
        if (armorKey == null) {
            armorKey = key;
        }

        int split = armorKey.lastIndexOf('_');
        if (split <= 0 || split >= armorKey.length() - 1) {
            return null;
        }

        String setId = armorKey.substring(0, split);
        CustomArmorManager.ArmorPieceType piece = parseArmorPiece(armorKey.substring(split + 1));
        if (piece == null || customArmorManager.getArmorSet(setId) == null) {
            return null;
        }
        return customArmorManager.createArmorPiece(setId, piece);
    }

    private ItemStack resolvePet(String key, Player viewer) {
        if (petManager == null) {
            return null;
        }

        String petId = stripAnyPrefix(key, "pet:");
        if (petId == null) {
            petId = key;
        }

        for (PetDefinition pet : petManager.allPets()) {
            if (pet == null || pet.id() == null) {
                continue;
            }
            if (normalize(pet.id()).equals(petId)) {
                return petManager.createPetItem(pet.id(), viewer);
            }
        }
        return null;
    }

    private ItemStack resolveMinion(String key) {
        if (minionManager == null) {
            return null;
        }

        String typeKey = null;
        Integer tier = null;

        String prefixed = stripAnyPrefix(key, "minion:");
        if (prefixed != null) {
            String[] parts = prefixed.split(":");
            if (parts.length == 1) {
                typeKey = parts[0];
                tier = 1;
            } else if (parts.length == 2) {
                typeKey = parts[0];
                tier = parsePositiveInt(parts[1]);
            }
        } else {
            Matcher matcher = MINION_ALIAS_PATTERN.matcher(key);
            if (matcher.matches()) {
                typeKey = matcher.group(1);
                tier = matcher.group(2) == null ? 1 : parsePositiveInt(matcher.group(2));
            }
        }

        if (typeKey == null || tier == null) {
            return null;
        }

        MinionType type = MinionType.parse(typeKey);
        if (type == null) {
            return null;
        }
        return minionManager.createMinionItem(type, tier);
    }

    private ItemStack resolveMinionFuel(String key) {
        if (minionManager == null) {
            return null;
        }
        String fuelId = stripAnyPrefix(key, "minion-fuel:", "minion_fuel:");
        return fuelId == null ? null : minionManager.createFuelItem(fuelId, 1);
    }

    private ItemStack resolveMinionUpgrade(String key) {
        if (minionManager == null) {
            return null;
        }
        String upgradeId = stripAnyPrefix(key, "minion-upgrade:", "minion_upgrade:");
        return upgradeId == null ? null : minionManager.createUpgradeItem(upgradeId, 1);
    }

    private ItemStack resolveCustomOnlyMinionIngredient(String key) {
        if (minionManager == null) {
            return null;
        }

        String ingredientId = stripAnyPrefix(key, "minion-ingredient:", "minion_ingredient:");
        if (ingredientId == null) {
            ingredientId = key;
        }

        MinionManager.IngredientDefinition definition = MinionManager.getIngredients().get(ingredientId);
        if (definition == null || !definition.customOnly()) {
            return null;
        }
        return minionManager.createIngredientItem(definition.id(), 1);
    }

    private static CustomArmorManager.ArmorPieceType parseArmorPiece(String rawPiece) {
        String piece = normalize(rawPiece);
        if (piece == null) {
            return null;
        }
        return switch (piece) {
            case "helmet", "helm" -> CustomArmorManager.ArmorPieceType.HELMET;
            case "chestplate", "chest" -> CustomArmorManager.ArmorPieceType.CHESTPLATE;
            case "leggings", "legs" -> CustomArmorManager.ArmorPieceType.LEGGINGS;
            case "boots" -> CustomArmorManager.ArmorPieceType.BOOTS;
            default -> null;
        };
    }

    private static Integer parsePositiveInt(String raw) {
        try {
            int parsed = Integer.parseInt(raw);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String stripAnyPrefix(String key, String... prefixes) {
        if (key == null) {
            return null;
        }
        for (String prefix : prefixes) {
            if (key.startsWith(prefix)) {
                return normalize(key.substring(prefix.length()));
            }
        }
        return null;
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }
}
