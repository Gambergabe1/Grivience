package io.papermc.Grivience.npcshop;

import java.util.HashMap;
import java.util.Map;

public class NpcShop {
    private final String id;
    private final String displayName;
    private final Map<Integer, NpcShopItem> items = new HashMap<>();

    public NpcShop(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setItem(int slot, NpcShopItem item) {
        items.put(slot, item);
    }

    public Map<Integer, NpcShopItem> getItems() {
        return items;
    }
}
