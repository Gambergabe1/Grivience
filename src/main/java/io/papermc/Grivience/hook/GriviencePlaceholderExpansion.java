package io.papermc.Grivience.hook;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import io.papermc.Grivience.stats.SkyblockLevelManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.Locale;
import java.util.UUID;

public final class GriviencePlaceholderExpansion extends PlaceholderExpansion {
    private final GriviencePlugin plugin;

    public GriviencePlaceholderExpansion(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "grivience";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null || params == null || params.isBlank()) {
            return null;
        }

        UUID profileId = resolveProfileId(player);
        if (profileId == null) {
            return "0";
        }

        SkyblockLevelManager levelManager = plugin.getSkyblockLevelManager();
        int level = levelManager.getLevel(profileId);
        String normalized = params.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "skyblock_level" -> Integer.toString(level);
            case "skyblock_level_display" -> levelManager.getLevelColor(level) + Integer.toString(level) + "§e⭐";
            case "skyblock_level_color" -> levelManager.getLevelColor(level).toString();
            case "skyblock_level_xp" -> Long.toString(levelManager.getXp(profileId));
            case "skyblock_level_progress_percent" -> formatPercent(levelManager.getProgress(profileId));
            case "skyblock_level_hover" -> plugin.getSkyblockSkillManager() != null 
                    ? plugin.getSkyblockSkillManager().getSkillHover(profileId) 
                    : "Level: " + level;
            case "skyblock_level_fancy" -> {
                String color = levelManager.getLevelColor(level).name().toLowerCase(Locale.ROOT);
                String hover = plugin.getSkyblockSkillManager() != null 
                        ? plugin.getSkyblockSkillManager().getSkillHover(profileId) 
                        : "Level: " + level;
                // Returns a MiniMessage string that many modern chat plugins can parse
                yield "<hover:show_text:'" + hover + "'><" + color + ">" + level + "</" + color + "><yellow>⭐</yellow></hover>";
            }
            default -> null;
        };
    }

    private UUID resolveProfileId(OfflinePlayer player) {
        UUID ownerId = player.getUniqueId();
        if (ownerId == null) {
            return null;
        }

        var islandManager = plugin.getIslandManager();
        if (islandManager != null) {
            UUID coopProfileId = islandManager.getCoopProfileId(ownerId);
            if (coopProfileId != null) {
                return coopProfileId;
            }
        }

        ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager != null) {
            SkyBlockProfile profile = profileManager.getSelectedProfile(ownerId);
            if (profile != null && profile.getProfileId() != null) {
                return profile.getProfileId();
            }
        }

        return ownerId;
    }

    private String formatPercent(double progress) {
        return String.format(Locale.US, "%.1f", Math.max(0.0D, progress) * 100.0D);
    }
}
