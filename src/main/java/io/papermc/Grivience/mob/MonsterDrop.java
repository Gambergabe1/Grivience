package io.papermc.Grivience.mob;

import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class MonsterDrop implements ConfigurationSerializable {
    private String material;
    private double chance;
    private int minAmount;
    private int maxAmount;
    private String customItemId;
    private boolean isCustomItem;

    public MonsterDrop() {
        this.material = "ROTTEN_FLESH";
        this.chance = 1.0;
        this.minAmount = 1;
        this.maxAmount = 1;
        this.customItemId = "";
        this.isCustomItem = false;
    }

    public MonsterDrop(Map<String, Object> data) {
        this.material = (String) data.getOrDefault("material", "ROTTEN_FLESH");
        this.chance = ((Number) data.getOrDefault("chance", 1.0)).doubleValue();
        this.minAmount = (int) data.getOrDefault("minAmount", 1);
        this.maxAmount = (int) data.getOrDefault("maxAmount", 1);
        this.customItemId = (String) data.getOrDefault("customItemId", "");
        this.isCustomItem = data.containsKey("customItemId");
    }

    public MonsterDrop(String material, double chance, int minAmount, int maxAmount) {
        this.material = material;
        this.chance = chance;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.isCustomItem = false;
    }

    public MonsterDrop(boolean isCustom, String customItemId, double chance, int minAmount, int maxAmount) {
        this.customItemId = customItemId;
        this.chance = chance;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.isCustomItem = isCustom;
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("material", material);
        data.put("chance", chance);
        data.put("minAmount", minAmount);
        data.put("maxAmount", maxAmount);
        if (isCustomItem) {
            data.put("customItemId", customItemId);
        }
        return data;
    }

    public ItemStack toItemStack() {
        if (isCustomItem && !customItemId.isEmpty()) {
            return null; // Will be handled by CustomItemService
        }

        Material mat = Material.getMaterial(material);
        if (mat == null) {
            mat = Material.ROTTEN_FLESH;
        }

        int amount = minAmount;
        if (maxAmount > minAmount) {
            amount = minAmount + (int) (Math.random() * (maxAmount - minAmount + 1));
        }

        return new ItemStack(mat, amount);
    }

    public String getMaterial() {
        return material;
    }

    public double getChance() {
        return chance;
    }

    public int getMinAmount() {
        return minAmount;
    }

    public int getMaxAmount() {
        return maxAmount;
    }

    public String getCustomItemId() {
        return customItemId;
    }

    public boolean isCustomItem() {
        return isCustomItem;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public void setChance(double chance) {
        this.chance = chance;
    }

    public void setMinAmount(int minAmount) {
        this.minAmount = minAmount;
    }

    public void setMaxAmount(int maxAmount) {
        this.maxAmount = maxAmount;
    }

    public void setCustomItemId(String customItemId) {
        this.customItemId = customItemId;
        this.isCustomItem = customItemId != null && !customItemId.isEmpty();
    }
}
