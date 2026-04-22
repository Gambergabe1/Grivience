package io.papermc.Grivience.wizard;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

public final class WizardTowerZnpcsHook {
    private static final String ZNPCS_PLUGIN_NAME = "ZNPCs";

    private final GriviencePlugin plugin;
    private final WizardTowerManager manager;

    private boolean hooked;

    public WizardTowerZnpcsHook(GriviencePlugin plugin, WizardTowerManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void hookIfAvailable() {
        if (hooked) {
            return;
        }

        var znpcs = plugin.getServer().getPluginManager().getPlugin(ZNPCS_PLUGIN_NAME);
        if (znpcs == null || !znpcs.isEnabled()) {
            return;
        }

        Listener bridgeListener = new Listener() {
            @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
            public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
                handleNpcInteract(event);
            }
        };
        plugin.getServer().getPluginManager().registerEvents(bridgeListener, plugin);
        hooked = true;
        plugin.getLogger().info("Hooked into normal ZNPCs interaction events for Wizard Tower NPC binding.");
    }

    private void handleNpcInteract(PlayerInteractAtEntityEvent event) {
        if (!hooked) {
            return;
        }
        if (!manager.isEnabled()) {
            return;
        }
        if (manager.getTrackedNpcId().isBlank()) {
            return;
        }

        String clickedNpcId = Integer.toString(event.getRightClicked().getEntityId());
        if (!manager.isTrackedNpc(clickedNpcId)) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!event.getPlayer().isOnline()) {
                return;
            }
            manager.handleNpcInteraction(event.getPlayer(), clickedNpcId);
        });
    }
}
