package io.papermc.Grivience.bazaar;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class BazaarEligibilityTest {

    @Test
    void rejectsKnownAuctionHouseItemFamilies() {
        assertFalse(BazaarShopManager.isBazaarEligibleCustomKey("oni_cleaver", Material.DIAMOND_SWORD, 1));
        assertFalse(BazaarShopManager.isBazaarEligibleCustomKey("shogun_kabuto", Material.LEATHER_HELMET, 1));
        assertFalse(BazaarShopManager.isBazaarEligibleCustomKey("ironcrest_drill", Material.IRON_PICKAXE, 1));
        assertFalse(BazaarShopManager.isBazaarEligibleCustomKey("grappling_hook", Material.FISHING_ROD, 1));
        assertFalse(BazaarShopManager.isBazaarEligibleCustomKey("gentle_stone", Material.FLINT, 64));
    }

    @Test
    void rejectsNonCommodityPatterns() {
        assertFalse(BazaarShopManager.isBazaarEligibleCustomKey("mystic_talisman", Material.PAPER, 64));
        assertFalse(BazaarShopManager.isBazaarEligibleCustomKey("unknown_key", Material.DIAMOND_SWORD, 1));
        assertFalse(BazaarShopManager.isBazaarEligibleCustomKey("unknown_key", Material.WRITTEN_BOOK, 1));
    }

    @Test
    void acceptsStackableCommodityItems() {
        assertTrue(BazaarShopManager.isBazaarEligibleCustomKey("storm_sigil", Material.PAPER, 64));
        assertTrue(BazaarShopManager.isBazaarEligibleCustomKey("enchanted_wheat", Material.HAY_BLOCK, 64));
    }
}
