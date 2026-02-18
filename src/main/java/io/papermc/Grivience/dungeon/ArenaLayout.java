package io.papermc.Grivience.dungeon;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public final class ArenaLayout {
    private final List<Location> roomCenters;
    private final List<Gate> gates;

    public ArenaLayout(List<Location> roomCenters, List<Gate> gates) {
        this.roomCenters = List.copyOf(roomCenters);
        this.gates = List.copyOf(gates);
    }

    public List<Location> roomCenters() {
        return roomCenters;
    }

    public List<Gate> gates() {
        return gates;
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
