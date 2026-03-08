package io.papermc.Grivience.item;

/**
 * Base stat profiles for custom weapons.
 *
 * This is intentionally lore-independent: many custom items use non-Ascent Skyblock labels
 * (e.g. "Seaforce", "Ferocity") while the combat engine works with Ascent Skyblock stats.
 *
 * Reforge bonuses are applied elsewhere via {@link CustomItemService#reforgeStats(ReforgeType, org.bukkit.inventory.ItemStack)}.
 */
public final class CustomWeaponProfiles {
    private CustomWeaponProfiles() {
    }

    public static StatProfile stats(CustomWeaponType type) {
        if (type == null) {
            return StatProfile.ZERO;
        }

        return switch (type) {
            case ONI_CLEAVER -> new StatProfile(125.0D, 45.0D, 0.0D, 0.0D, 0.0D, 0.0D);
            case TENGU_GALEBLADE -> new StatProfile(110.0D, 20.0D, 18.0D, 20.0D, 0.0D, 0.0D);
            case TENGU_STORMBOW -> new StatProfile(118.0D, 16.0D, 22.0D, 24.0D, 0.0D, 0.0D);
            case TENGU_SHORTBOW -> new StatProfile(112.0D, 14.0D, 26.0D, 18.0D, 0.0D, 0.0D);
            case KAPPA_TIDEBREAKER -> new StatProfile(120.0D, 30.0D, 0.0D, 10.0D, 0.0D, 0.0D);
            case ONRYO_SPIRITBLADE -> new StatProfile(145.0D, 45.0D, 5.0D, 20.0D, 0.0D, 0.0D);
            case ONRYO_SHORTBOW -> new StatProfile(138.0D, 30.0D, 14.0D, 30.0D, 0.0D, 0.0D);
            case JOROGUMO_STINGER -> new StatProfile(95.0D, 18.0D, 8.0D, 10.0D, 0.0D, 0.0D);
            case JOROGUMO_SHORTBOW -> new StatProfile(104.0D, 14.0D, 24.0D, 18.0D, 0.0D, 0.0D);
            case KITSUNE_FANG -> new StatProfile(108.0D, 25.0D, 12.0D, 25.0D, 0.0D, 0.0D);
            case KITSUNE_DAWNBOW -> new StatProfile(126.0D, 22.0D, 14.0D, 40.0D, 0.0D, 0.0D);
            case KITSUNE_SHORTBOW -> new StatProfile(124.0D, 18.0D, 20.0D, 36.0D, 0.0D, 0.0D);
            case GASHADOKURO_NODACHI -> new StatProfile(165.0D, 70.0D, 4.0D, 30.0D, 0.0D, 0.0D);
            case FLYING_RAIJIN -> new StatProfile(210.0D, 100.0D, 10.0D, 35.0D, 0.0D, 0.0D);
            case HAYABUSA_KATANA -> new StatProfile(175.0D, 55.0D, 32.0D, 28.0D, 0.0D, 0.0D);
            case RAIJIN_SHORTBOW -> new StatProfile(140.0D, 24.0D, 30.0D, 42.0D, 0.0D, 0.0D);

            // Mage Weapons - Staffs
            case ARCANE_STAFF -> new StatProfile(85.0D, 15.0D, 5.0D, 15.0D, 0.0D, 120.0D);
            case FROSTBITE_STAFF -> new StatProfile(95.0D, 18.0D, 8.0D, 20.0D, 0.0D, 140.0D);
            case INFERNO_STAFF -> new StatProfile(110.0D, 25.0D, 10.0D, 25.0D, 0.0D, 130.0D);
            case STORMCALLER_STAFF -> new StatProfile(100.0D, 20.0D, 12.0D, 22.0D, 0.0D, 150.0D);
            case VOIDWALKER_STAFF -> new StatProfile(120.0D, 30.0D, 8.0D, 28.0D, 0.0D, 160.0D);
            case CELESTIAL_STAFF -> new StatProfile(130.0D, 35.0D, 10.0D, 32.0D, 0.0D, 180.0D);

            // Mage Weapons - Wands
            case FLAME_WAND -> new StatProfile(55.0D, 10.0D, 5.0D, 10.0D, 0.0D, 70.0D);
            case ICE_WAND -> new StatProfile(50.0D, 8.0D, 6.0D, 12.0D, 0.0D, 75.0D);
            case LIGHTNING_WAND -> new StatProfile(60.0D, 12.0D, 8.0D, 15.0D, 0.0D, 80.0D);
            case POISON_WAND -> new StatProfile(45.0D, 8.0D, 4.0D, 8.0D, 0.0D, 65.0D);
            case HEALING_WAND -> new StatProfile(40.0D, 5.0D, 0.0D, 5.0D, 0.0D, 90.0D);

            // Mage Weapons - Scepters
            case SCEPTER_OF_HEALING -> new StatProfile(65.0D, 12.0D, 5.0D, 10.0D, 0.0D, 110.0D);
            case SCEPTER_OF_DECAY -> new StatProfile(80.0D, 18.0D, 8.0D, 15.0D, 0.0D, 100.0D);
            case SCEPTER_OF_MENDING -> new StatProfile(70.0D, 14.0D, 6.0D, 12.0D, 0.0D, 125.0D);
        };
    }

    public record StatProfile(
            double flatDamage,
            double strength,
            double critChancePercent,
            double critDamagePercent,
            double attackSpeedPercent,
            double intelligence
    ) {
        public static final StatProfile ZERO = new StatProfile(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    }
}

