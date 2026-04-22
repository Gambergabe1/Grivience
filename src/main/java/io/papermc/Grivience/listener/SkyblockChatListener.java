package io.papermc.Grivience.listener;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.stats.SkyblockLevelManager;
import io.papermc.Grivience.nick.NickManager;
import io.papermc.Grivience.skills.SkyblockSkillManager;
import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;

import java.util.UUID;

/**
 * Modern Paper Chat Formatting with Hover support for Skyblock Levels.
 */
public final class SkyblockChatListener implements Listener {
    private final GriviencePlugin plugin;
    private final SkyblockLevelManager levelManager;
    private final SkyblockSkillManager skillManager;
    private final ProfileManager profileManager;
    private final NickManager nickManager;

    public SkyblockChatListener(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.levelManager = plugin.getSkyblockLevelManager();
        this.skillManager = plugin.getSkyblockSkillManager();
        this.profileManager = plugin.getProfileManager();
        this.nickManager = plugin.getNickManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (levelManager == null) return;

        // Get Profile ID for accurate data - Use authoritative resolution from LevelManager
        UUID profileId = levelManager.resolveProfileId(player);
        int level = levelManager.getLevel(profileId);
        
        // Build Level Component with Hover
        TextColor levelColor = TextColor.color(levelManager.getLevelColor(level).asBungee().getColor().getRGB());
        
        String hoverText = skillManager != null ? skillManager.getSkillHover(profileId) : "Level: " + level;
        Component hoverComp = MiniMessage.miniMessage().deserialize(hoverText);
        
        Component levelComp = Component.text("[")
                .color(NamedTextColor.DARK_GRAY)
                .append(Component.text(level).color(levelColor))
                .append(Component.text("⭐").color(NamedTextColor.YELLOW))
                .append(Component.text("] "))
                .hoverEvent(HoverEvent.showText(hoverComp));

        // Build Rank and Name Component
        Component rankAndName;
        if (nickManager != null && nickManager.isNicked(player)) {
            NickManager.NickData data = nickManager.getNickData(player);
            // Prefix + Name
            rankAndName = LegacyComponentSerializer.legacySection().deserialize(data.rank().getPrefix() + data.nickname());
        } else {
            String lpPrefix = "";
            try {
                net.luckperms.api.LuckPerms lp = net.luckperms.api.LuckPermsProvider.get();
                net.luckperms.api.model.user.User user = lp.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    String prefixStr = user.getCachedData().getMetaData().getPrefix();
                    if (prefixStr != null) {
                        lpPrefix = prefixStr;
                    }
                }
            } catch (Exception ignored) {}

            try {
                if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                    lpPrefix = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, lpPrefix);
                    // Also support the direct placeholder if they just want that
                    if (lpPrefix.isEmpty()) {
                        String fromPapi = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, "%luckperms_prefix%");
                        if (!fromPapi.equals("%luckperms_prefix%")) {
                            lpPrefix = fromPapi;
                        }
                    }
                }
            } catch (Exception ignored) {}
            
            // Hypixel style: The name inherits the last color of the prefix
            String fullColoredName = lpPrefix + player.getName();
            
            if (lpPrefix.contains("§")) {
                rankAndName = LegacyComponentSerializer.legacySection().deserialize(fullColoredName);
            } else {
                rankAndName = LegacyComponentSerializer.legacyAmpersand().deserialize(fullColoredName);
            }
        }

        // Final Prefix: [LEVEL] Rank Name
        // We use an empty base component to prevent Level hover from applying to the Name
        Component prefix = Component.empty()
                .append(levelComp)
                .append(rankAndName
                        .hoverEvent(HoverEvent.showText(Component.text("Click to view profile!", NamedTextColor.YELLOW)))
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/viewprofile " + player.getName())));
        
        // Update the renderer to use our custom format
        // Main text (message) is forced to WHITE
        event.renderer((source, sourceDisplayName, message, viewer) -> 
            prefix.append(Component.text(": ").color(NamedTextColor.WHITE))
                  .append(message.color(NamedTextColor.WHITE))
        );
    }
}
