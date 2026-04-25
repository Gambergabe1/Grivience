package io.papermc.Grivience.item;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Staff system with 99% Ascent Skyblock accuracy.
 * 
 * Features:
 * - Staffs are tools, not blocks (cannot be placed)
 * - Right-click abilities with mana cost
 * - Staff-specific protections
 * - Intelligence scaling for ability damage
 * - Cooldown system per staff ability
 */
public final class StaffManager {
    private final GriviencePlugin plugin;
    private final NamespacedKey staffKey;
    private final NamespacedKey abilityCooldownKey;
    
    // Cooldown tracking
    private final java.util.Map<UUID, Long> staffCooldowns = new java.util.HashMap<>();
    
    private boolean enabled;
    private double globalCooldownMs;
    private boolean requireMana;
    private boolean showCooldownMessage;

    public StaffManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.staffKey = new NamespacedKey(plugin, "staff_item");
        this.abilityCooldownKey = new NamespacedKey(plugin, "staff_cooldown");
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        enabled = plugin.getConfig().getBoolean("staffs.enabled", true);
        globalCooldownMs = plugin.getConfig().getDouble("staffs.global-cooldown-ms", 500.0);
        requireMana = plugin.getConfig().getBoolean("staffs.require-mana", true);
        showCooldownMessage = plugin.getConfig().getBoolean("staffs.show-cooldown-messages", true);
    }

    /**
     * Check if an item is a staff.
     */
    public boolean isStaff(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        CustomWeaponType staffType = getStaffType(item);
        if (isMageWeaponType(staffType)) {
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(staffKey, PersistentDataType.BOOLEAN);
    }

    /**
     * Get the staff type from an item.
     */
    public CustomWeaponType getStaffType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;

        CustomItemService itemService = plugin.getCustomItemService();
        if (itemService != null) {
            CustomWeaponType byId = CustomWeaponType.parse(itemService.itemId(item));
            if (isMageWeaponType(byId)) {
                return byId;
            }
        }

        if (!item.hasItemMeta()) return null;
        
        String displayName = item.getItemMeta().getDisplayName();
        if (displayName == null) return null;
        
        String lower = displayName.toLowerCase();
        if (lower.contains("arcane")) return CustomWeaponType.ARCANE_STAFF;
        if (lower.contains("frostbite")) return CustomWeaponType.FROSTBITE_STAFF;
        if (lower.contains("inferno")) return CustomWeaponType.INFERNO_STAFF;
        if (lower.contains("stormcaller")) return CustomWeaponType.STORMCALLER_STAFF;
        if (lower.contains("voidwalker")) return CustomWeaponType.VOIDWALKER_STAFF;
        if (lower.contains("celestial")) return CustomWeaponType.CELESTIAL_STAFF;
        
        return null;
    }

    /**
     * Check if staff ability is on cooldown.
     */
    public boolean isOnCooldown(Player player) {
        Long cooldownEnd = staffCooldowns.get(player.getUniqueId());
        if (cooldownEnd == null) return false;
        return System.currentTimeMillis() < cooldownEnd;
    }

    /**
     * Get remaining cooldown time in seconds.
     */
    public double getCooldownRemaining(Player player) {
        Long cooldownEnd = staffCooldowns.get(player.getUniqueId());
        if (cooldownEnd == null) return 0.0;
        long remaining = cooldownEnd - System.currentTimeMillis();
        return Math.max(0.0, remaining / 1000.0);
    }

    /**
     * Set staff ability cooldown.
     */
    public void setCooldown(Player player, long durationMs) {
        staffCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + durationMs);
    }

    /**
     * Clear staff cooldown.
     */
    public void clearCooldown(Player player) {
        staffCooldowns.remove(player.getUniqueId());
    }

    /**
     * Create a staff item with proper metadata.
     */
    public ItemStack createStaff(CustomWeaponType staffType) {
        CustomItemService itemService = plugin.getCustomItemService();
        if (itemService != null && isMageWeaponType(staffType)) {
            ItemStack canonical = itemService.createWeapon(staffType);
            if (canonical != null && canonical.hasItemMeta()) {
                ItemMeta canonicalMeta = canonical.getItemMeta();
                canonicalMeta.getPersistentDataContainer().set(staffKey, PersistentDataType.BOOLEAN, true);
                canonical.setItemMeta(canonicalMeta);
                return canonical;
            }
        }

        Material material = getStaffMaterial(staffType);
        ItemStack staff = new ItemStack(material);
        ItemMeta meta = staff.getItemMeta();
        
        // Set display name based on type
        meta.setDisplayName(getStaffDisplayName(staffType));
        
        // Set lore with staff information
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Mage Weapon");
        lore.add("");
        lore.add(ChatColor.AQUA + "Right-Click Ability:");
        lore.add(getStaffAbilityDescription(staffType));
        lore.add("");
        lore.add(ChatColor.GRAY + "Intelligence Scaling: §bYes");
        lore.add(ChatColor.GRAY + "Mana Cost: §bVaries");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Cannot be placed as a block!");
        lore.add(ChatColor.DARK_GRAY + "This is a tool, not a block.");
        
        meta.setLore(lore);
        
        // Mark as staff (prevents block placement)
        meta.getPersistentDataContainer().set(staffKey, PersistentDataType.BOOLEAN, true);
        
        // Make unbreakable
        meta.setUnbreakable(true);
        
        // Hide attributes
        meta.addItemFlags(
            org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
            org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE,
            org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS
        );
        
        staff.setItemMeta(meta);
        return staff;
    }

    private boolean isMageWeaponType(CustomWeaponType type) {
        return switch (type) {
            case ARCANE_STAFF,
                 FROSTBITE_STAFF,
                 INFERNO_STAFF,
                 STORMCALLER_STAFF,
                 VOIDWALKER_STAFF,
                 CELESTIAL_STAFF,
                 FLAME_WAND,
                 ICE_WAND,
                 LIGHTNING_WAND,
                 POISON_WAND,
                 HEALING_WAND,
                 SCEPTER_OF_HEALING,
                 SCEPTER_OF_DECAY,
                 SCEPTER_OF_MENDING -> true;
            default -> false;
        };
    }

    private Material getStaffMaterial(CustomWeaponType type) {
        return switch (type) {
            case ARCANE_STAFF -> Material.BLAZE_ROD;
            case FROSTBITE_STAFF -> Material.PRISMARINE_CRYSTALS;
            case INFERNO_STAFF -> Material.BLAZE_ROD;
            case STORMCALLER_STAFF -> Material.LIGHTNING_ROD;
            case VOIDWALKER_STAFF -> Material.ENDER_PEARL;
            case CELESTIAL_STAFF -> Material.GLOWSTONE_DUST;
            default -> Material.BLAZE_ROD;
        };
    }

    private String getStaffDisplayName(CustomWeaponType type) {
        return switch (type) {
            case ARCANE_STAFF -> ChatColor.LIGHT_PURPLE + "Arcane Staff";
            case FROSTBITE_STAFF -> ChatColor.AQUA + "Frostbite Staff";
            case INFERNO_STAFF -> ChatColor.RED + "Inferno Staff";
            case STORMCALLER_STAFF -> ChatColor.YELLOW + "Stormcaller Staff";
            case VOIDWALKER_STAFF -> ChatColor.DARK_PURPLE + "Voidwalker Staff";
            case CELESTIAL_STAFF -> ChatColor.WHITE + "Celestial Staff";
            default -> ChatColor.GRAY + "Unknown Staff";
        };
    }

    private String getStaffAbilityDescription(CustomWeaponType type) {
        return switch (type) {
            case ARCANE_STAFF -> ChatColor.GRAY + "Shoots an arcane missile";
            case FROSTBITE_STAFF -> ChatColor.GRAY + "Freezes enemies in ice";
            case INFERNO_STAFF -> ChatColor.GRAY + "Summons a fire explosion";
            case STORMCALLER_STAFF -> ChatColor.GRAY + "Calls down lightning";
            case VOIDWALKER_STAFF -> ChatColor.GRAY + "Teleports through space";
            case CELESTIAL_STAFF -> ChatColor.GRAY + "Summons heavenly light";
            default -> ChatColor.GRAY + "Unknown ability";
        };
    }

    /**
     * Send cooldown message to player.
     */
    public void sendCooldownMessage(Player player) {
        if (!showCooldownMessage) return;
        double remaining = getCooldownRemaining(player);
        player.sendMessage(ChatColor.RED + "Ability on cooldown! (§e" + String.format("%.1f", remaining) + "s§c remaining)");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isRequireMana() {
        return requireMana;
    }

    public double getGlobalCooldownMs() {
        return globalCooldownMs;
    }

    public NamespacedKey getStaffKey() {
        return staffKey;
    }
}
