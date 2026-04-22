package io.papermc.Grivience.elevator;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public record ElevatorFloor(String name, ItemStack icon, Location location, String requiredLayer) {}
