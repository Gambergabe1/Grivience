package io.papermc.Grivience.mob;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CustomMonster implements ConfigurationSerializable {
    private final String id;
    private String displayName;
    private EntityType entityType;
    private double health;
    private double damage;
    private double speed;
    private List<MonsterDrop> drops;
    private int expReward;
    private boolean glowing;

    public CustomMonster(String id) {
        this.id = id;
        this.displayName = id;
        this.entityType = EntityType.ZOMBIE;
        this.health = 20.0;
        this.damage = 3.0;
        this.speed = 0.23;
        this.drops = new ArrayList<>();
        this.expReward = 10;
        this.glowing = false;
    }

    public CustomMonster(Map<String, Object> data) {
        this.id = (String) data.get("id");
        this.displayName = (String) data.getOrDefault("displayName", id);
        String typeName = (String) data.getOrDefault("entityType", "ZOMBIE");
        try {
            this.entityType = EntityType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.entityType = EntityType.ZOMBIE;
        }
        this.health = ((Number) data.getOrDefault("health", 20.0)).doubleValue();
        this.damage = ((Number) data.getOrDefault("damage", 3.0)).doubleValue();
        this.speed = ((Number) data.getOrDefault("speed", 0.23)).doubleValue();
        this.expReward = (int) data.getOrDefault("expReward", 10);
        this.glowing = (boolean) data.getOrDefault("glowing", false);

        this.drops = new ArrayList<>();
        Object dropsObj = data.get("drops");
        if (dropsObj instanceof List) {
            for (Object dropObj : (List<?>) dropsObj) {
                if (dropObj instanceof Map) {
                    drops.add(new MonsterDrop((Map<String, Object>) dropObj));
                }
            }
        }
    }

    @Override
    @NotNull
    public Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("displayName", displayName);
        data.put("entityType", entityType.name());
        data.put("health", health);
        data.put("damage", damage);
        data.put("speed", speed);
        data.put("expReward", expReward);
        data.put("glowing", glowing);

        List<Map<String, Object>> dropsList = new ArrayList<>();
        for (MonsterDrop drop : drops) {
            dropsList.add(drop.serialize());
        }
        data.put("drops", dropsList);

        return data;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public double getHealth() {
        return health;
    }

    public double getDamage() {
        return damage;
    }

    public double getSpeed() {
        return speed;
    }

    public List<MonsterDrop> getDrops() {
        return drops;
    }

    public int getExpReward() {
        return expReward;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void setExpReward(int expReward) {
        this.expReward = expReward;
    }
}
