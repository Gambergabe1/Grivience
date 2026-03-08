package io.papermc.Grivience.quest;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.lang.reflect.Method;

public final class QuestZnpcsHook {
    private static final String ZNPCS_PLUGIN_NAME = "ZNPCsPlus";
    private static final String NPC_INTERACT_EVENT_CLASS = "lol.pyr.znpcsplus.api.event.NpcInteractEvent";
    private static final String NPC_ENTRY_CLASS = "lol.pyr.znpcsplus.api.npc.NpcEntry";

    private final GriviencePlugin plugin;
    private final QuestManager questManager;

    private boolean hooked;
    private boolean warnedAboutRuntimeError;
    private Method getPlayerMethod;
    private Method getEntryMethod;
    private Method getNpcIdMethod;

    public QuestZnpcsHook(GriviencePlugin plugin, QuestManager questManager) {
        this.plugin = plugin;
        this.questManager = questManager;
    }

    public void hookIfAvailable() {
        if (hooked) {
            return;
        }

        var znpcs = plugin.getServer().getPluginManager().getPlugin(ZNPCS_PLUGIN_NAME);
        if (znpcs == null || !znpcs.isEnabled()) {
            return;
        }

        try {
            Class<?> eventType = Class.forName(NPC_INTERACT_EVENT_CLASS);
            if (!Event.class.isAssignableFrom(eventType)) {
                plugin.getLogger().warning("ZNPCS hook skipped: NpcInteractEvent is not a Bukkit Event.");
                return;
            }

            Class<?> entryType = Class.forName(NPC_ENTRY_CLASS);
            getPlayerMethod = eventType.getMethod("getPlayer");
            getEntryMethod = eventType.getMethod("getEntry");
            getNpcIdMethod = entryType.getMethod("getId");

            @SuppressWarnings("unchecked")
            Class<? extends Event> bukkitEventType = (Class<? extends Event>) eventType;

            Listener bridgeListener = new Listener() {
            };
            plugin.getServer().getPluginManager().registerEvent(
                    bukkitEventType,
                    bridgeListener,
                    EventPriority.MONITOR,
                    (listener, event) -> handleNpcInteract(event),
                    plugin,
                    true
            );

            hooked = true;
            plugin.getLogger().info("Hooked into ZNPCsPlus conversation events.");
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("ZNPCS hook unavailable: " + exception.getMessage());
        }
    }

    private void handleNpcInteract(Event event) {
        if (!hooked) {
            return;
        }

        try {
            Object playerObject = getPlayerMethod.invoke(event);
            if (!(playerObject instanceof Player player)) {
                return;
            }
            Object entryObject = getEntryMethod.invoke(event);
            if (entryObject == null) {
                return;
            }
            Object npcIdObject = getNpcIdMethod.invoke(entryObject);
            if (!(npcIdObject instanceof String npcId) || npcId.isBlank()) {
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                questManager.handleNpcConversation(player, npcId, QuestTriggerSource.ZNPCS_EVENT, true);
            });
        } catch (ReflectiveOperationException exception) {
            if (!warnedAboutRuntimeError) {
                warnedAboutRuntimeError = true;
                plugin.getLogger().warning("ZNPCS runtime hook error: " + exception.getMessage());
            }
        }
    }
}
