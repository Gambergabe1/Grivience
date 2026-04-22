package io.papermc.Grivience.trade;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeListenerTest {

    @Test
    void cancelledShiftClicksAreNotMirroredIntoTradeOffers() {
        assertFalse(TradeListener.shouldMirrorBottomShiftClick(true, true));
        assertFalse(TradeListener.shouldMirrorBottomShiftClick(false, false));
        assertTrue(TradeListener.shouldMirrorBottomShiftClick(false, true));
    }
}
