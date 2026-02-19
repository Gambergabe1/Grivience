package io.papermc.Grivience.dungeon;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public final class ArenaLayout {
    private final List<Location> roomCenters;
    private final List<Gate> gates;
    private final List<Cuboid> cleanupVolumes;

    public ArenaLayout(List<Location> roomCenters, List<Gate> gates, List<Cuboid> cleanupVolumes) {
        this.roomCenters = List.copyOf(roomCenters);
        this.gates = List.copyOf(gates);
        this.cleanupVolumes = List.copyOf(cleanupVolumes);
    }

    public List<Location> roomCenters() {
        return roomCenters;
    }

    public List<Gate> gates() {
        return gates;
    }

    public List<Cuboid> cleanupVolumes() {
        return cleanupVolumes;
    }

    public static final class Cuboid {
        private final int minX;
        private final int maxX;
        private final int minY;
        private final int maxY;
        private final int minZ;
        private final int maxZ;

        public Cuboid(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
            this.minX = Math.min(minX, maxX);
            this.maxX = Math.max(minX, maxX);
            this.minY = Math.min(minY, maxY);
            this.maxY = Math.max(minY, maxY);
            this.minZ = Math.min(minZ, maxZ);
            this.maxZ = Math.max(minZ, maxZ);
        }

        public int minX() {
            return minX;
        }

        public int maxX() {
            return maxX;
        }

        public int minY() {
            return minY;
        }

        public int maxY() {
            return maxY;
        }

        public int minZ() {
            return minZ;
        }

        public int maxZ() {
            return maxZ;
        }
    }

    public static final class Gate {
        private final int index;
        private final List<Location> barrierBlocks;

        public Gate(int index, List<Location> barrierBlocks) {
            this.index = index;
            this.barrierBlocks = List.copyOf(barrierBlocks);
        }

        public int index() {
            return index;
        }

        public List<Location> barrierBlocks() {
            return barrierBlocks;
        }

        public static Gate of(int index, List<Location> barrierBlocks) {
            return new Gate(index, new ArrayList<>(barrierBlocks));
        }
    }
}
