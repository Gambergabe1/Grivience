package io.papermc.Grivience.bazaar;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

public final class BazaarValidationTest {

    @Test
    void testPriceSpreadValidation() {
        // Assume market price is 100
        double marketPrice = 100.0;
        double maxSpread = 5.0; // 5%
        
        // Inside spread
        assertTrue(isWithinSpread(100.0, marketPrice, maxSpread));
        assertTrue(isWithinSpread(105.0, marketPrice, maxSpread));
        assertTrue(isWithinSpread(95.0, marketPrice, maxSpread));
        assertTrue(isWithinSpread(102.5, marketPrice, maxSpread));
        
        // Outside spread
        assertFalse(isWithinSpread(105.1, marketPrice, maxSpread));
        assertFalse(isWithinSpread(94.9, marketPrice, maxSpread));
        assertFalse(isWithinSpread(110.0, marketPrice, maxSpread));
        assertFalse(isWithinSpread(90.0, marketPrice, maxSpread));
    }

    private boolean isWithinSpread(double unitPrice, double marketPrice, double maxPriceSpreadPercent) {
        if (Double.isNaN(marketPrice) || marketPrice <= 0) return true;
        double spreadPercent = Math.abs(unitPrice - marketPrice) / marketPrice * 100;
        return spreadPercent <= maxPriceSpreadPercent + 1e-9;
    }
}
