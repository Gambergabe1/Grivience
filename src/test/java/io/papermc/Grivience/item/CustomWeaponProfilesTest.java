package io.papermc.Grivience.item;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomWeaponProfilesTest {

    @Test
    void parsesNewEndWeaponIds() {
        assertEquals(CustomWeaponType.RIFTBLADE, CustomWeaponType.parse("riftblade"));
        assertEquals(CustomWeaponType.VOID_ASPECT_BLADE, CustomWeaponType.parse("void aspect blade"));
        assertEquals(CustomWeaponType.RIFTBREAKER, CustomWeaponType.parse("RIFTBREAKER"));
        assertEquals(CustomWeaponType.SOVEREIGN_ASPECT, CustomWeaponType.parse("sovereign-aspect"));
        assertEquals(CustomWeaponType.VOIDFANG_DAGGER, CustomWeaponType.parse("voidfang dagger"));
        assertEquals(CustomWeaponType.WARP_BOW, CustomWeaponType.parse("warp bow"));
        assertEquals(CustomWeaponType.VOIDSHOT_BOW, CustomWeaponType.parse("voidshot_bow"));
        assertEquals(CustomWeaponType.RIFTSTORM_BOW, CustomWeaponType.parse("riftstorm bow"));
        assertEquals(CustomWeaponType.ORBITAL_LONGBOW, CustomWeaponType.parse("orbital longbow"));
    }

    @Test
    void exposesConfiguredEndWeaponStatProfiles() {
        assertEquals(new CustomWeaponProfiles.StatProfile(85.0D, 25.0D, 0.0D, 0.0D, 0.0D, 0.0D),
                CustomWeaponProfiles.stats(CustomWeaponType.RIFTBLADE));
        assertEquals(new CustomWeaponProfiles.StatProfile(130.0D, 45.0D, 10.0D, 0.0D, 0.0D, 0.0D),
                CustomWeaponProfiles.stats(CustomWeaponType.VOID_ASPECT_BLADE));
        assertEquals(new CustomWeaponProfiles.StatProfile(185.0D, 70.0D, 0.0D, 25.0D, 0.0D, 0.0D),
                CustomWeaponProfiles.stats(CustomWeaponType.RIFTBREAKER));
        assertEquals(new CustomWeaponProfiles.StatProfile(260.0D, 110.0D, 0.0D, 40.0D, 0.0D, 150.0D),
                CustomWeaponProfiles.stats(CustomWeaponType.SOVEREIGN_ASPECT));
        assertEquals(new CustomWeaponProfiles.StatProfile(140.0D, 35.0D, 20.0D, 0.0D, 25.0D, 0.0D),
                CustomWeaponProfiles.stats(CustomWeaponType.VOIDFANG_DAGGER));
        assertEquals(new CustomWeaponProfiles.StatProfile(120.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D),
                CustomWeaponProfiles.stats(CustomWeaponType.WARP_BOW));
        assertEquals(new CustomWeaponProfiles.StatProfile(165.0D, 0.0D, 10.0D, 0.0D, 0.0D, 0.0D),
                CustomWeaponProfiles.stats(CustomWeaponType.VOIDSHOT_BOW));
        assertEquals(new CustomWeaponProfiles.StatProfile(210.0D, 0.0D, 0.0D, 30.0D, 0.0D, 0.0D),
                CustomWeaponProfiles.stats(CustomWeaponType.RIFTSTORM_BOW));
        assertEquals(new CustomWeaponProfiles.StatProfile(270.0D, 0.0D, 0.0D, 50.0D, 0.0D, 0.0D),
                CustomWeaponProfiles.stats(CustomWeaponType.ORBITAL_LONGBOW));
    }
}
