package io.papermc.Grivience.skyblock.economy;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Profile-scoped purse operations (Skyblock-style).
 * This intentionally does not integrate with Vault; all balances live on the active Skyblock profile.
 */
public final class ProfileEconomyService {
    private final GriviencePlugin plugin;

    public ProfileEconomyService(GriviencePlugin plugin) {
        this.plugin = plugin;
    }

    public SkyBlockProfile getSelectedProfile(Player player) {
        if (player == null) {
            return null;
        }
        ProfileManager manager = plugin.getProfileManager();
        if (manager == null) {
            return null;
        }
        return manager.getSelectedProfile(player);
    }

    public SkyBlockProfile requireSelectedProfile(Player player) {
        if (player == null) {
            return null;
        }

        ProfileManager manager = plugin.getProfileManager();
        if (manager == null) {
            return null;
        }

        SkyBlockProfile profile = manager.getSelectedProfile(player);
        if (profile != null) {
            return profile;
        }

        // If the player has no profiles at all, create a default one so core systems can function.
        if (manager.getPlayerProfiles(player).isEmpty()) {
            manager.createProfile(player, "Default");
            profile = manager.getSelectedProfile(player);
            if (profile != null) {
                return profile;
            }
        }

        player.sendMessage(ChatColor.RED + "You need a Skyblock profile to do that.");
        player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.YELLOW + "/profile create <name>" + ChatColor.GRAY + " then " + ChatColor.YELLOW + "/profile select <name>");
        return null;
    }

    public double purse(Player player) {
        SkyBlockProfile profile = getSelectedProfile(player);
        if (profile == null) {
            return 0.0D;
        }
        return Math.max(0.0D, profile.getPurse());
    }

    public boolean has(Player player, double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0D) {
            return true;
        }
        SkyBlockProfile profile = getSelectedProfile(player);
        return profile != null && profile.getPurse() + 1e-9D >= amount;
    }

    public boolean withdraw(Player player, double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0D) {
            return true;
        }
        ProfileManager manager = plugin.getProfileManager();
        if (manager == null) {
            return false;
        }
        SkyBlockProfile profile = requireSelectedProfile(player);
        if (profile == null) {
            return false;
        }

        double purse = profile.getPurse();
        if (purse + 1e-9D < amount) {
            return false;
        }

        profile.setPurse(Math.max(0.0D, purse - amount));
        profile.addCoinsSpent(toSafeCoinStat(amount));
        manager.saveProfile(profile);
        return true;
    }

    public boolean deposit(Player player, double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0D) {
            return true;
        }
        ProfileManager manager = plugin.getProfileManager();
        if (manager == null) {
            return false;
        }
        SkyBlockProfile profile = requireSelectedProfile(player);
        if (profile == null) {
            return false;
        }

        profile.setPurse(Math.max(0.0D, profile.getPurse() + amount));
        profile.addCoinsEarned(toSafeCoinStat(amount));
        manager.saveProfile(profile);
        return true;
    }

    private static int toSafeCoinStat(double amount) {
        long rounded = Math.round(amount);
        if (rounded <= 0L) {
            return 0;
        }
        if (rounded >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) rounded;
    }

    /**
     * Executes common "eco" style commands against the selected profile purse.
     * Supported:
     * - eco give <player> <amount>
     * - eco take <player> <amount>
     * - eco set <player> <amount>
     */
    public boolean executeEcoLikeCommand(Player player, String rawCommand) {
        if (player == null || rawCommand == null) {
            return false;
        }

        String trimmed = rawCommand.trim();
        if (trimmed.isEmpty()) {
            return false;
        }

        String[] parts = trimmed.split("\\s+");
        if (parts.length < 4) {
            return false;
        }

        String root = parts[0].toLowerCase(Locale.ROOT);
        if (!root.equals("eco") && !root.equals("economy")) {
            return false;
        }

        String verb = parts[1].toLowerCase(Locale.ROOT);
        String target = parts[2];
        if (!target.equalsIgnoreCase(player.getName())) {
            return false;
        }

        double amount;
        try {
            amount = Double.parseDouble(parts[3]);
        } catch (NumberFormatException ignored) {
            return false;
        }
        if (!Double.isFinite(amount)) {
            return false;
        }

        return switch (verb) {
            case "give", "add" -> deposit(player, amount);
            case "take", "remove" -> withdraw(player, amount);
            case "set" -> {
                ProfileManager manager = plugin.getProfileManager();
                if (manager == null) {
                    yield false;
                }
                SkyBlockProfile profile = requireSelectedProfile(player);
                if (profile == null) {
                    yield false;
                }
                profile.setPurse(Math.max(0.0D, amount));
                manager.saveProfile(profile);
                yield true;
            }
            default -> false;
        };
    }
}

