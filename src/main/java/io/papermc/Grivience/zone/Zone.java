package io.papermc.Grivience.zone;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

/**
 * Represents a defined zone area with bounds and display properties.
 * Zones can be used for region-based scoreboard display, restrictions, or events.
 */
public final class Zone {
    private final String id;
    private String name;
    private String world;
    
    // Zone bounds (cuboid)
    private Location pos1;  // First corner (minimum)
    private Location pos2;  // Second corner (maximum)
    
    // Display properties
    private String displayName;
    private ChatColor color;
    private int priority;  // Higher priority zones override lower ones
    
    // Zone properties
    private boolean enabled;
    private String description;
    
    public Zone(String id) {
        this.id = id;
        this.name = id;
        this.displayName = id;
        this.color = ChatColor.WHITE;
        this.priority = 0;
        this.enabled = true;
        this.description = "";
    }
    
    public Zone(
            String id,
            String name,
            String world,
            Location pos1,
            Location pos2,
            String displayName,
            ChatColor color,
            int priority,
            boolean enabled,
            String description
    ) {
        this.id = id;
        this.name = name;
        this.world = world;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.displayName = displayName != null ? displayName : name;
        this.color = color != null ? color : ChatColor.WHITE;
        this.priority = priority;
        this.enabled = enabled;
        this.description = description != null ? description : "";
    }
    
    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getWorld() { return world; }
    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }
    public String getDisplayName() { return displayName; }
    public ChatColor getColor() { return color; }
    public int getPriority() { return priority; }
    public boolean isEnabled() { return enabled; }
    public String getDescription() { return description; }
    
    // Setters
    public void setName(String name) { this.name = name; }
    public void setWorld(String world) { this.world = world; }
    public void setPos1(Location pos1) { this.pos1 = pos1; }
    public void setPos2(Location pos2) { this.pos2 = pos2; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setColor(ChatColor color) { this.color = color; }
    public void setPriority(int priority) { this.priority = priority; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setDescription(String description) { this.description = description; }
    
    /**
     * Check if a location is within this zone's bounds.
     */
    public boolean contains(Location location) {
        if (!enabled || pos1 == null || pos2 == null) {
            return false;
        }
        
        if (world != null && !location.getWorld().getName().equals(world)) {
            return false;
        }
        
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());
        
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        
        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }
    
    /**
     * Get the center location of this zone.
     */
    public Location getCenter() {
        if (pos1 == null || pos2 == null) {
            return null;
        }
        
        World worldObj = Bukkit.getWorld(world);
        if (worldObj == null) {
            return null;
        }
        
        double midX = (pos1.getX() + pos2.getX()) / 2.0;
        double midY = (pos1.getY() + pos2.getY()) / 2.0;
        double midZ = (pos1.getZ() + pos2.getZ()) / 2.0;
        
        return new Location(worldObj, midX, midY, midZ);
    }
    
    /**
     * Get the size of this zone (volume).
     */
    public double getSize() {
        if (pos1 == null || pos2 == null) {
            return 0.0;
        }
        
        double width = Math.abs(pos2.getX() - pos1.getX());
        double height = Math.abs(pos2.getY() - pos1.getY());
        double depth = Math.abs(pos2.getZ() - pos1.getZ());
        
        return width * height * depth;
    }
    
    /**
     * Get formatted display name with color.
     */
    public String getColoredDisplayName() {
        return color + displayName;
    }
    
    /**
     * Save zone to configuration section.
     */
    public void save(ConfigurationSection section) {
        section.set("name", name);
        section.set("world", world);
        section.set("display-name", displayName);
        section.set("color", color.name());
        section.set("priority", priority);
        section.set("enabled", enabled);
        section.set("description", description);
        
        if (pos1 != null) {
            section.set("pos1.x", pos1.getX());
            section.set("pos1.y", pos1.getY());
            section.set("pos1.z", pos1.getZ());
        }
        if (pos2 != null) {
            section.set("pos2.x", pos2.getX());
            section.set("pos2.y", pos2.getY());
            section.set("pos2.z", pos2.getZ());
        }
    }
    
    /**
     * Load zone from configuration section.
     */
    public static Zone fromSection(String id, ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        
        String name = section.getString("name", id);
        String world = section.getString("world");
        String displayName = section.getString("display-name", name);
        
        ChatColor color = ChatColor.WHITE;
        try {
            color = ChatColor.valueOf(section.getString("color", "WHITE"));
        } catch (IllegalArgumentException ignored) {}
        
        int priority = section.getInt("priority", 0);
        boolean enabled = section.getBoolean("enabled", true);
        String description = section.getString("description", "");
        
        Zone zone = new Zone(id, name, world, null, null, displayName, color, priority, enabled, description);
        
        // Load positions
        ConfigurationSection pos1Section = section.getConfigurationSection("pos1");
        if (pos1Section != null && world != null) {
            World worldObj = Bukkit.getWorld(world);
            if (worldObj != null) {
                double x = pos1Section.getDouble("x", 0);
                double y = pos1Section.getDouble("y", 64);
                double z = pos1Section.getDouble("z", 0);
                zone.setPos1(new Location(worldObj, x, y, z));
            }
        }
        
        ConfigurationSection pos2Section = section.getConfigurationSection("pos2");
        if (pos2Section != null && world != null) {
            World worldObj = Bukkit.getWorld(world);
            if (worldObj != null) {
                double x = pos2Section.getDouble("x", 0);
                double y = pos2Section.getDouble("y", 64);
                double z = pos2Section.getDouble("z", 0);
                zone.setPos2(new Location(worldObj, x, y, z));
            }
        }
        
        return zone;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Zone zone = (Zone) o;
        return Objects.equals(id, zone.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Zone{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", world='" + world + '\'' +
                ", pos1=" + pos1 +
                ", pos2=" + pos2 +
                ", displayName='" + displayName + '\'' +
                ", color=" + color +
                ", priority=" + priority +
                ", enabled=" + enabled +
                '}';
    }
}
