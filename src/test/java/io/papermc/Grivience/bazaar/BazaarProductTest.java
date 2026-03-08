package io.papermc.Grivience.bazaar;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class BazaarProductTest {
    @Test
    void updateFromOrderBook_usesLowestSellAndHighestBuyWhenAvailable() {
        BazaarProduct product = product("DIAMOND");

        List<BazaarOrder> buys = List.of(
                order("ORD-00000001", BazaarOrder.OrderType.BUY, 100.0, 10, 1000L),
                order("ORD-00000002", BazaarOrder.OrderType.BUY, 90.0, 10, 1100L)
        );
        List<BazaarOrder> sells = List.of(
                order("ORD-00000003", BazaarOrder.OrderType.SELL, 110.0, 10, 900L),
                order("ORD-00000004", BazaarOrder.OrderType.SELL, 120.0, 10, 800L)
        );

        product.updateFromOrderBook(buys, sells);

        assertEquals(100.0, product.getHighestBuyOrder(), 1e-9);
        assertEquals(110.0, product.getLowestSellOrder(), 1e-9);
        assertEquals(110.0, product.getInstantBuyPrice(), 1e-9);
        assertEquals(100.0, product.getInstantSellPrice(), 1e-9);
        assertEquals(2, product.getBuyOrderCount());
        assertEquals(2, product.getSellOrderCount());
    }

    @Test
    void updateFromOrderBook_instantBuyUnavailableWhenNoSells() {
        BazaarProduct product = product("DIAMOND");

        List<BazaarOrder> buys = List.of(
                order("ORD-00000001", BazaarOrder.OrderType.BUY, 100.0, 10, 1000L)
        );

        product.updateFromOrderBook(buys, List.of());

        assertEquals(100.0, product.getHighestBuyOrder(), 1e-9);
        assertTrue(Double.isNaN(product.getLowestSellOrder()));
        assertTrue(Double.isNaN(product.getInstantBuyPrice()));
        assertEquals(100.0, product.getInstantSellPrice(), 1e-9);
        assertEquals(1, product.getBuyOrderCount());
        assertEquals(0, product.getSellOrderCount());
    }

    @Test
    void updateFromOrderBook_instantSellUnavailableWhenNoBuys() {
        BazaarProduct product = product("DIAMOND");

        List<BazaarOrder> sells = List.of(
                order("ORD-00000001", BazaarOrder.OrderType.SELL, 110.0, 10, 1000L)
        );

        product.updateFromOrderBook(List.of(), sells);

        assertTrue(Double.isNaN(product.getHighestBuyOrder()));
        assertEquals(110.0, product.getLowestSellOrder(), 1e-9);
        assertEquals(110.0, product.getInstantBuyPrice(), 1e-9);
        assertTrue(Double.isNaN(product.getInstantSellPrice()));
        assertEquals(0, product.getBuyOrderCount());
        assertEquals(1, product.getSellOrderCount());
    }

    @Test
    void updateFromOrderBook_instantUnavailableWhenNoOrdersExist() {
        BazaarProduct product = product("DIAMOND");

        product.updateFromOrderBook(List.of(), List.of());

        assertTrue(Double.isNaN(product.getHighestBuyOrder()));
        assertTrue(Double.isNaN(product.getLowestSellOrder()));
        assertTrue(Double.isNaN(product.getInstantBuyPrice()));
        assertTrue(Double.isNaN(product.getInstantSellPrice()));
        assertEquals(0, product.getBuyOrderCount());
        assertEquals(0, product.getSellOrderCount());
    }

    private static BazaarProduct product(String productId) {
        return new BazaarProduct(
                productId,
                productId,
                Material.DIAMOND,
                BazaarProduct.BazaarCategory.MINING,
                BazaarProduct.BazaarSubcategory.ORE,
                false,
                null,
                64
        );
    }

    private static BazaarOrder order(String orderId, BazaarOrder.OrderType type, double price, int amount, long createdAt) {
        UUID owner = UUID.randomUUID();
        UUID profile = UUID.randomUUID();
        return new BazaarOrder(
                orderId,
                owner,
                profile,
                "Tester",
                "DIAMOND",
                type,
                price,
                amount,
                createdAt,
                createdAt + 3600_000L
        );
    }
}
