package io.papermc.Grivience.elevator;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.Method;

public final class ElevatorZnpcsHook {
    private static final String ZNPCS_PLUGIN_NAME = "ZNPCsPlus";
    private static final String NPC_INTERACT_EVENT_CLASS = "lol.pyr.znpcsplus.api.event.NpcInteractEvent";
    private static final String NPC_ENTRY_CLASS = "lol.pyr.znpcsplus.api.npc.NpcEntry";

    private final GriviencePlugin plugin;
    private final ElevatorManager manager;
    private final ElevatorGui gui;

    private boolean hooked;
    private Method getPlayerMethod;
    private Method getEntryMethod;
    private Method getNpcIdMethod;

    public ElevatorZnpcsHook(GriviencePlugin plugin, ElevatorManager manager, ElevatorGui gui) {
        this.plugin = plugin;
        this.manager = manager;
        this.gui = gui;
    }

    public void hookIfAvailable() {
        if (hooked) return;

        var znpcs = plugin.getServer().getPluginManager().getPlugin(ZNPCS_PLUGIN_NAME);
        if (znpcs == null || !znpcs.isEnabled()) return;

        try {
            Class<?> eventType = Class.forName(NPC_INTERACT_EVENT_CLASS);
            Class<?> entryType = Class.forName(NPC_ENTRY_CLASS);
            getPlayerMethod = eventType.getMethod("getPlayer");
            getEntryMethod = eventType.getMethod("getEntry");
            getNpcIdMethod = entryType.getMethod("getId");

            @SuppressWarnings("unchecked")
            Class<? extends Event> bukkitEventType = (Class<? extends Event>) eventType;

            Listener bridgeListener = new Listener() {};
            plugin.getServer().getPluginManager().registerEvent(
                    bukkitEventType,
                    bridgeListener,
                    EventPriority.MONITOR,
                    (listener, event) -> handleNpcInteract(event),
                    plugin,
                    true
            );

            hooked = true;
            plugin.getLogger().info("Hooked Elevator into ZNPCsPlus.");
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Elevator ZNPCS hook unavailable: " + exception.getMessage());
        }
    }

    private void handleNpcInteract(Event event) {
        if (!hooked) return;

        try {
            Object playerObject = getPlayerMethod.invoke(event);
            if (!(playerObject instanceof Player player)) return;
            
            Object entryObject = getEntryMethod.invoke(event);
            if (entryObject == null) return;
            
            Object npcIdObject = getNpcIdMethod.invoke(entryObject);
            if (!(npcIdObject instanceof String npcId) || npcId.isBlank()) return;

            if (npcId.toLowerCase().startsWith("elevator_")) {
                String elevatorId = npcId.substring("elevator_".length());
                Elevator elevator = manager.getElevator(elevatorId);
                if (elevator != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (player.isOnline()) {
                            gui.open(player, elevator);
                        }
                    });
                }
            }
        } catch (ReflectiveOperationException ignored) {}
    }
}
