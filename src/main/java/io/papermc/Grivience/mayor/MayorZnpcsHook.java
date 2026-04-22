package io.papermc.Grivience.mayor;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.Method;
import java.util.UUID;

public final class MayorZnpcsHook {
    private static final String ZNPCS_PLUGIN_NAME = "ZNPCsPlus";
    private static final String API_PROVIDER_CLASS = "lol.pyr.znpcsplus.api.NpcApiProvider";
    private static final String NPC_TYPE_CLASS = "lol.pyr.znpcsplus.api.entity.NpcType";
    private static final String NPC_INTERACT_EVENT_CLASS = "lol.pyr.znpcsplus.api.event.NpcInteractEvent";
    private static final String NPC_ENTRY_CLASS = "lol.pyr.znpcsplus.api.npc.NpcEntry";

    private final GriviencePlugin plugin;
    private final MayorGui mayorGui;

    private boolean hooked;
    private Object api;
    
    private Method getPlayerMethod;
    private Method getEntryMethod;
    private Method getNpcIdMethod;

    public MayorZnpcsHook(GriviencePlugin plugin, MayorGui mayorGui) {
        this.plugin = plugin;
        this.mayorGui = mayorGui;
    }

    public void hookIfAvailable() {
        if (hooked) return;

        var znpcs = plugin.getServer().getPluginManager().getPlugin(ZNPCS_PLUGIN_NAME);
        if (znpcs == null || !znpcs.isEnabled()) return;

        try {
            Class<?> providerClass = Class.forName(API_PROVIDER_CLASS);
            Method getApiMethod = providerClass.getMethod("get");
            api = getApiMethod.invoke(null);

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
            plugin.getLogger().info("Hooked Mayor into ZNPCsPlus API.");
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Mayor ZNPCS hook unavailable: " + exception.getMessage());
        }
    }

    public void summonMayor(String id, Location location, String name, String skin) {
        if (!hooked || api == null) return;

        try {
            // delete if exists
            deleteMayor(id);

            // NpcApi.getNpcRegistry().create(String id, World world, NpcType type, Location location)
            Method getRegistryMethod = api.getClass().getMethod("getNpcRegistry");
            Object registry = getRegistryMethod.invoke(api);
            
            Method getTypesMethod = api.getClass().getMethod("getNpcTypeRegistry");
            Object typeRegistry = getTypesMethod.invoke(api);
            Method getTypeMethod = typeRegistry.getClass().getMethod("getByName", String.class);
            Object playerType = getTypeMethod.invoke(typeRegistry, "player");

            Method createMethod = registry.getClass().getMethod("create", String.class, org.bukkit.World.class, playerType.getClass().getInterfaces()[0], Location.class);
            Object entry = createMethod.invoke(registry, id, location.getWorld(), playerType, location);

            if (entry != null) {
                // We'll use dispatch commands for skin and holograms as the property API is very reflection-unfriendly
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "znpcs npc " + id + " skin " + skin);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "znpcs npc " + id + " hologram set " + name.replace(" ", "_"));
                    // Set to look at players
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "znpcs npc " + id + " toggle look");
                }, 10L);
                
                plugin.getLogger().info("Summoned Mayor NPC: " + id + " (" + name + ") at " + location.toString());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to summon mayor via ZNPCsPlus: " + e.getMessage());
        }
    }

    public void deleteMayor(String id) {
        if (!hooked || api == null) return;

        try {
            Method getRegistryMethod = api.getClass().getMethod("getNpcRegistry");
            Object registry = getRegistryMethod.invoke(api);
            
            Method getByIdMethod = registry.getClass().getMethod("getById", String.class);
            Object entry = getByIdMethod.invoke(registry, id);
            
            if (entry != null) {
                Method deleteMethod = registry.getClass().getMethod("delete", entry.getClass());
                deleteMethod.invoke(registry, entry);
                plugin.getLogger().info("Deleted Mayor NPC: " + id);
            }
        } catch (Exception ignored) {}
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

            // Check if it's our mayor
            if (npcId.startsWith("mayor_")) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (player.isOnline()) {
                        mayorGui.open(player);
                    }
                });
            }
        } catch (ReflectiveOperationException ignored) {}
    }
}
