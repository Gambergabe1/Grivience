package io.papermc.Grivience.terrain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MountainProfileTest {

    @Test
    void centerColumnReachesSelectedPeakHeight() {
        MountainProfile profile = new MountainProfile(0, 20, 0, 20, 64, 96, 12345L);

        assertEquals(96, profile.heightAt(10, 10));
    }

    @Test
    void cornerColumnsFallBackToSelectionBaseHeight() {
        MountainProfile profile = new MountainProfile(0, 20, 0, 20, 64, 96, 12345L);

        assertEquals(64, profile.heightAt(0, 0));
        assertEquals(64, profile.heightAt(20, 0));
        assertEquals(64, profile.heightAt(0, 20));
        assertEquals(64, profile.heightAt(20, 20));
    }

    @Test
    void allGeneratedHeightsStayInsideSelectionRange() {
        MountainProfile profile = new MountainProfile(-8, 12, -10, 14, 40, 75, 987654321L);

        for (int x = profile.minX(); x <= profile.maxX(); x++) {
            for (int z = profile.minZ(); z <= profile.maxZ(); z++) {
                int height = profile.heightAt(x, z);
                assertTrue(height >= profile.baseY(), "height dropped below base at " + x + "," + z);
                assertTrue(height <= profile.peakY(), "height exceeded peak at " + x + "," + z);
            }
        }
    }

    @Test
    void nearCornerColumnsStillUseTheAllocatedFootprint() {
        MountainProfile profile = new MountainProfile(0, 20, 0, 20, 64, 96, 12345L);

        assertTrue(profile.heightAt(18, 18) > 64);
    }

    @Test
    void differentSeedsChangeTheProfileAwayFromCenter() {
        MountainProfile first = new MountainProfile(0, 20, 0, 20, 64, 96, 111L);
        MountainProfile second = new MountainProfile(0, 20, 0, 20, 64, 96, 222L);

        assertNotEquals(first.heightAt(6, 9), second.heightAt(6, 9));
    }
}
