package io.papermc.Grivience.bazaar;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class BazaarNpcShopPricingTest {

    @Test
    void cappedNpcUnitPrice_neverExceedsBazaarCap() {
        assertEquals(90.0, BazaarShopManager.cappedNpcUnitPrice(100.0, 0.90), 1e-9);
        assertEquals(100.0, BazaarShopManager.cappedNpcUnitPrice(100.0, 1.25), 1e-9);
        assertEquals(100.0, BazaarShopManager.cappedNpcUnitPrice(100.0, 1.00), 1e-9);
    }

    @Test
    void cappedNpcUnitPrice_handlesInvalidInputs() {
        assertEquals(0.0, BazaarShopManager.cappedNpcUnitPrice(Double.NaN, 0.90), 1e-9);
        assertEquals(0.0, BazaarShopManager.cappedNpcUnitPrice(0.0, 0.90), 1e-9);
        assertEquals(0.0, BazaarShopManager.cappedNpcUnitPrice(100.0, -5.0), 1e-9);
    }
}
