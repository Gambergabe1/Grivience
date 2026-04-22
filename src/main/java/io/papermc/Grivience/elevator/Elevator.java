package io.papermc.Grivience.elevator;

import java.util.ArrayList;
import java.util.List;

public class Elevator {
    private final String id;
    private String displayName;
    private final List<ElevatorFloor> floors = new ArrayList<>();

    public Elevator(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
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

    public List<ElevatorFloor> getFloors() {
        return floors;
    }

    public void addFloor(ElevatorFloor floor) {
        floors.add(floor);
    }
}
