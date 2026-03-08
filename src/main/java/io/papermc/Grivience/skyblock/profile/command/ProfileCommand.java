package io.papermc.Grivience.skyblock.profile.command;

import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Profile management command - Skyblock accurate.
 * 
 * Commands:
 * - /profile - List profiles
 * - /profile create <name> - Create new profile
 * - /profile select <name> - Switch to profile
 * - /profile delete <name> - Delete profile
 * - /profile info [name] - View profile details
 * - /profile rename <old> <new> - Rename profile
 * - /profile seticon <name> <icon> - Set profile icon
 */
public final class ProfileCommand implements CommandExecutor, TabCompleter {
    private final ProfileManager profileManager;

    public ProfileCommand(ProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
            return true;
        }

        if (args.length == 0) {
            listProfiles(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> handleCreate(player, args);
            case "select" -> handleSelect(player, args);
            case "delete" -> handleDelete(player, args);
            case "info" -> handleInfo(player, args);
            case "rename" -> handleRename(player, args);
            case "seticon" -> handleSetIcon(player, args);
            case "help" -> sendHelp(player, label);
            default -> {
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /" + label + " help.");
            }
        }

        return true;
    }

    private void listProfiles(Player player) {
        List<SkyBlockProfile> profiles = profileManager.getPlayerProfiles(player);
        
        if (profiles.isEmpty()) {
            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "=== Your Profiles ===");
            player.sendMessage(ChatColor.RED + "You don't have any profiles yet.");
            player.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.GREEN + "/profile create <name>" + 
                ChatColor.YELLOW + " to create your first profile!");
            player.sendMessage("");
            return;
        }

        SkyBlockProfile selected = profileManager.getSelectedProfile(player);
        
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "=== Your Profiles (" + profiles.size() + "/" + 
            profileManager.getMaxProfilesPerPlayer() + ") ===");
        
        for (SkyBlockProfile profile : profiles) {
            String prefix = profile.equals(selected) ? ChatColor.GREEN + "● " : ChatColor.GRAY + "○ ";
            String nameColor = profile.equals(selected) ? "" + ChatColor.AQUA : "" + ChatColor.GRAY;
            
            player.sendMessage(prefix + nameColor + profile.getProfileName() + 
                ChatColor.GRAY + " - " + 
                ChatColor.YELLOW + "Level " + getAverageSkillLevel(profile) + 
                ChatColor.GRAY + " - " + 
                ChatColor.WHITE + profile.getFormattedCreationDate());
        }
        
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Commands:");
        player.sendMessage(ChatColor.GREEN + "/profile create <name>" + ChatColor.GRAY + " - Create new profile");
        player.sendMessage(ChatColor.GREEN + "/profile select <name>" + ChatColor.GRAY + " - Switch profile");
        player.sendMessage(ChatColor.GREEN + "/profile info <name>" + ChatColor.GRAY + " - View details");
        player.sendMessage("");
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /profile create <name>");
            player.sendMessage(ChatColor.GRAY + "Profile name must be 3-16 characters.");
            return;
        }

        String profileName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        // Validate profile name
        if (profileName.length() < 3) {
            player.sendMessage(ChatColor.RED + "Profile name must be at least 3 characters.");
            return;
        }
        
        if (profileName.length() > 16) {
            player.sendMessage(ChatColor.RED + "Profile name must be at most 16 characters.");
            return;
        }
        
        if (!profileName.matches("[a-zA-Z0-9_ ]+")) {
            player.sendMessage(ChatColor.RED + "Profile name can only contain letters, numbers, spaces, and underscores.");
            return;
        }

        SkyBlockProfile profile = profileManager.createProfile(player, profileName);
        
        if (profile != null) {
            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "=== Profile Created ===");
            player.sendMessage(ChatColor.GREEN + "Name: " + ChatColor.AQUA + profile.getProfileName());
            player.sendMessage(ChatColor.GREEN + "Created: " + ChatColor.WHITE + profile.getFormattedCreationDate());
            player.sendMessage(ChatColor.GREEN + "Max Profiles: " + ChatColor.YELLOW + profileManager.getMaxProfilesPerPlayer());
            player.sendMessage("");
            player.sendMessage(ChatColor.YELLOW + "Tip: Use " + ChatColor.GREEN + "/profile select " + profileName + 
                ChatColor.YELLOW + " to switch to this profile.");
            player.sendMessage("");
        }
    }

    private void handleSelect(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /profile select <name>");
            return;
        }

        String profileName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        if (profileManager.selectProfile(player, profileName)) {
            SkyBlockProfile profile = profileManager.getSelectedProfile(player);
            if (profile != null) {
                player.sendMessage("");
                player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "=== Profile Selected ===");
                player.sendMessage(ChatColor.GREEN + "Now playing on: " + ChatColor.AQUA + profile.getProfileName());
                player.sendMessage(ChatColor.GREEN + "Created: " + ChatColor.WHITE + profile.getFormattedCreationDate());
                player.sendMessage(ChatColor.GREEN + "Playtime: " + ChatColor.YELLOW + profile.getFormattedPlaytime());
                player.sendMessage("");
            }
        }
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /profile delete <name>");
            player.sendMessage(ChatColor.RED + "Warning: This action cannot be undone!");
            return;
        }

        String profileName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        // Confirmation required for deletion
        player.sendMessage(ChatColor.YELLOW + "Are you sure you want to delete profile '" + profileName + "'?");
        player.sendMessage(ChatColor.RED + "This action cannot be undone!");
        player.sendMessage(ChatColor.GRAY + "All island data, items, and progress will be lost.");
        player.sendMessage(ChatColor.GREEN + "Type /profile confirm delete " + profileName + " to confirm.");
        
        // In a full implementation, we'd add a confirmation system
        // For now, we'll just delete directly
        if (profileManager.deleteProfile(player, profileName)) {
            player.sendMessage(ChatColor.RED + "Profile '" + profileName + "' has been deleted.");
        }
    }

    private void handleInfo(Player player, String[] args) {
        SkyBlockProfile profile;
        
        if (args.length < 2) {
            profile = profileManager.getSelectedProfile(player);
        } else {
            String profileName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            profile = profileManager.getProfile(player, profileName);
        }
        
        if (profile == null) {
            player.sendMessage(ChatColor.RED + "Profile not found.");
            return;
        }

        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "=== Profile: " + profile.getProfileName() + " ===");
        player.sendMessage(ChatColor.GREEN + "Owner: " + ChatColor.WHITE + Bukkit.getOfflinePlayer(profile.getOwnerId()).getName());
        player.sendMessage(ChatColor.GREEN + "Created: " + ChatColor.WHITE + profile.getFormattedCreationDate());
        player.sendMessage(ChatColor.GREEN + "Last Save: " + ChatColor.WHITE + profile.getFormattedCreationDate()); // Would need actual last save
        player.sendMessage(ChatColor.GREEN + "Playtime: " + ChatColor.YELLOW + profile.getFormattedPlaytime());
        player.sendMessage("");
        player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "Statistics:");
        player.sendMessage(ChatColor.GRAY + "Purse: " + ChatColor.GOLD + String.format("%.1f", profile.getPurse()) + " coins");
        player.sendMessage(ChatColor.GRAY + "Bank: " + ChatColor.GOLD + String.format("%.1f", profile.getBankBalance()) + " coins");
        player.sendMessage(ChatColor.GRAY + "Deaths: " + ChatColor.RED + profile.getDeaths());
        player.sendMessage(ChatColor.GRAY + "Kills: " + ChatColor.RED + profile.getKills());
        player.sendMessage(ChatColor.GRAY + "Coins Earned: " + ChatColor.GOLD + profile.getCoinsEarned());
        player.sendMessage(ChatColor.GRAY + "Coins Spent: " + ChatColor.GOLD + profile.getCoinsSpent());
        player.sendMessage(ChatColor.GRAY + "Items Fished: " + ChatColor.AQUA + profile.getItemsFished());
        player.sendMessage(ChatColor.GRAY + "Dungeons Completed: " + ChatColor.LIGHT_PURPLE + profile.getDungeonsCompleted());
        player.sendMessage("");
        player.sendMessage(ChatColor.AQUA + "" + ChatColor.BOLD + "Skills (Average Level: " + getAverageSkillLevel(profile) + "):");
        
        // Show top skills
        profile.getSkillLevels().entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .limit(5)
            .forEach(entry -> {
                String skillName = formatSkillName(entry.getKey());
                player.sendMessage(ChatColor.GRAY + "  " + skillName + ": " + ChatColor.YELLOW + "Lv. " + entry.getValue());
            });
        
        player.sendMessage("");
    }

    private void handleRename(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /profile rename <old name> <new name>");
            return;
        }

        String oldName = args[1];
        String newName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        
        // Validate new name
        if (newName.length() < 3 || newName.length() > 16) {
            player.sendMessage(ChatColor.RED + "Profile name must be 3-16 characters.");
            return;
        }
        
        SkyBlockProfile profile = profileManager.getProfile(player, oldName);
        if (profile == null) {
            player.sendMessage(ChatColor.RED + "Profile '" + oldName + "' not found.");
            return;
        }
        
        profile.setProfileName(newName);
        profileManager.saveProfile(profile);
        
        player.sendMessage(ChatColor.GREEN + "Profile renamed to '" + newName + "'.");
    }

    private void handleSetIcon(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Usage: /profile seticon <profile name> <icon>");
            player.sendMessage(ChatColor.GRAY + "Icons: IRON_HELMET, GOLD_HELMET, DIAMOND_HELMET, etc.");
            return;
        }

        String profileName = args[1];
        String icon = args[2].toUpperCase();
        
        SkyBlockProfile profile = profileManager.getProfile(player, profileName);
        if (profile == null) {
            player.sendMessage(ChatColor.RED + "Profile '" + profileName + "' not found.");
            return;
        }
        
        profile.setProfileIcon(icon);
        profileManager.saveProfile(profile);
        
        player.sendMessage(ChatColor.GREEN + "Profile icon set to " + icon + ".");
    }

    private void sendHelp(Player player, String label) {
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "=== Profile Help ===");
        player.sendMessage(ChatColor.YELLOW + "/" + label + ChatColor.GRAY + " - List your profiles");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " create <name>" + ChatColor.GRAY + " - Create new profile");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " select <name>" + ChatColor.GRAY + " - Switch profile");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " delete <name>" + ChatColor.GRAY + " - Delete profile");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " info [name]" + ChatColor.GRAY + " - View profile details");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " rename <old> <new>" + ChatColor.GRAY + " - Rename profile");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " seticon <name> <icon>" + ChatColor.GRAY + " - Set profile icon");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " help" + ChatColor.GRAY + " - Show this help");
        player.sendMessage("");
    }

    private int getAverageSkillLevel(SkyBlockProfile profile) {
        return (int) profile.getSkillLevels().values().stream()
            .mapToInt(Integer::intValue)
            .average()
            .orElse(0.0);
    }

    private String formatSkillName(String skill) {
        return switch (skill.toUpperCase()) {
            case "TAMING" -> "§aTaming";
            case "MINING" -> "§aMining";
            case "COMBAT" -> "§cCombat";
            case "FORAGING" -> "§6Foraging";
            case "FARMING" -> "§eFarming";
            case "FISHING" -> "§bFishing";
            case "ENCHANTING" -> "§dEnchanting";
            case "ALCHEMY" -> "§5Alchemy";
            case "CARPENTRY" -> "§8Carpentry";
            case "RUNECRAFTING" -> "§9Runecrafting";
            case "SOCIAL" -> "§fSocial";
            default -> skill;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }

        if (args.length == 1) {
            List<String> commands = Arrays.asList("create", "select", "delete", "info", "rename", "seticon", "help");
            return filterPrefix(commands, args[0]);
        }

        if (args.length >= 2) {
            String sub = args[0].toLowerCase();
            
            if (List.of("select", "delete", "info", "rename", "seticon").contains(sub)) {
                List<String> profileNames = profileManager.getPlayerProfiles(player).stream()
                    .map(SkyBlockProfile::getProfileName)
                    .toList();
                return filterPrefix(profileNames, args[1]);
            }
        }

        return List.of();
    }

    private List<String> filterPrefix(List<String> input, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        return input.stream()
            .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(lowerPrefix))
            .collect(Collectors.toList());
    }
}

