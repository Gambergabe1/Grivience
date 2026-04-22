package io.papermc.Grivience.skyblock.command;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.island.Island;
import io.papermc.Grivience.skyblock.island.IslandManager;
import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import io.papermc.Grivience.party.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class IslandCommand implements CommandExecutor, TabCompleter {
    private final GriviencePlugin plugin;
    private final IslandManager islandManager;
    private final PartyManager partyManager;
    private final ProfileManager profileManager;

    private final HubCommand hubCommand;

    // islandId -> (inviteeId -> expiresAtMillis)
    private final Map<UUID, Map<UUID, Long>> visitInvitesByIsland = new HashMap<>();
    private final Map<UUID, CoopInvite> coopInvitesByInvitee = new HashMap<>();

    public IslandCommand(GriviencePlugin plugin, IslandManager islandManager, PartyManager partyManager, ProfileManager profileManager, HubCommand hubCommand) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.partyManager = partyManager;
        this.profileManager = profileManager;
        this.hubCommand = hubCommand;
    }

    private record CoopInvite(UUID islandId, UUID sharedProfileId, UUID inviterId, long expiresAtMillis) {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }

        String root = command.getName().toLowerCase(Locale.ROOT);
        switch (root) {
            case "visit" -> {
                if (args.length < 1) {
                    player.sendMessage(ChatColor.RED + "Usage: /visit <player>");
                    return true;
                }
                String[] visitArgs = new String[args.length + 1];
                visitArgs[0] = "go";
                System.arraycopy(args, 0, visitArgs, 1, args.length);
                handleGo(player, visitArgs);
                return true;
            }
            case "invite" -> {
                if (args.length < 1) {
                    player.sendMessage(ChatColor.RED + "Usage: /invite <player>");
                    return true;
                }
                handleInvite(player, new String[] {"invite", args[0]});
                return true;
            }
            case "sbkick" -> {
                if (args.length < 1) {
                    player.sendMessage(ChatColor.RED + "Usage: /sbkick <player>");
                    return true;
                }
                handleKick(player, new String[] {"kick", args[0]});
                return true;
            }
            case "sbkickall" -> {
                handleKickAll(player);
                return true;
            }
            case "setguestspawn" -> {
                handleSetGuestSpawn(player);
                return true;
            }
            case "setspawn" -> {
                handleSetHome(player);
                return true;
            }
            default -> {
                // Continue to /island parsing below.
            }
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "go", "home", "visit" -> handleGo(player, args);
            case "create" -> handleCreate(player, args);
            case "expand", "upgrade" -> handleExpand(player, args);
            case "info" -> handleInfo(player);
            case "sethome", "setspawn" -> handleSetHome(player);
            case "setguestspawn" -> handleSetGuestSpawn(player);
            case "open" -> handleOpen(player);
            case "close" -> handleClose(player);
            case "visits", "visitsettings", "privacy" -> handleVisitSettings(player, args);
            case "invite" -> handleInvite(player, args);
            case "kick" -> handleKick(player, args);
            case "kickall" -> handleKickAll(player);
            case "leave" -> handleLeave(player);
            case "delete" -> handleDelete(player, args);
            case "setname" -> handleSetName(player, args);
            case "setdesc" -> handleSetDesc(player, args);
            case "warp" -> handleWarp(player);
            case "minions", "minion" -> handleMinions(player);
            case "coop" -> handleCoop(player, args);
            case "profile" -> handleProfile(player, args);
            case "help" -> sendHelp(player);
            default -> {
                player.sendMessage(ChatColor.RED + "Unknown command. Use /island help");
            }
        }
        return true;
    }

    private void handleGo(Player player, String[] args) {
        if (args.length >= 2) {
            String targetName = args[1];
            if (targetName.equalsIgnoreCase(player.getName())) {
                teleportHome(player);
                return;
            }

            Player onlineTarget = Bukkit.getPlayerExact(targetName);
            OfflinePlayer target = onlineTarget != null ? onlineTarget : Bukkit.getOfflinePlayer(targetName);
            if (onlineTarget == null && (target == null || (!target.hasPlayedBefore() && !target.isOnline()))) {
                player.sendMessage(ChatColor.RED + "Player not found: " + targetName);
                return;
            }

            Island targetIsland = resolveTargetIsland(target, onlineTarget);
            if (targetIsland == null) {
                player.sendMessage(ChatColor.RED + targetName + " does not have an island.");
                return;
            }
            if (targetIsland.getCenter() == null) {
                player.sendMessage(ChatColor.RED + "That island is not available.");
                return;
            }

            UUID visitorId = player.getUniqueId();
            boolean isCoop = visitorId.equals(targetIsland.getOwner()) || targetIsland.isMember(visitorId);
            boolean bypass = hasVisitBypass(player);
            boolean invited = hasValidVisitInvite(targetIsland.getId(), visitorId);

            if (!isCoop && !bypass && !canVisit(player, targetIsland, invited)) {
                sendVisitDeniedMessage(player, targetIsland);
                return;
            }

            if (!isCoop && !bypass && isIslandFullForNewGuest(player, targetIsland)) {
                player.sendMessage(ChatColor.RED + "That island is full.");
                player.sendMessage(ChatColor.GRAY + "Try again later.");
                return;
            }

            Location destination = isCoop ? islandManager.getSafeSpawnLocation(targetIsland) : islandManager.getSafeGuestSpawnLocation(targetIsland);
            if (destination == null) {
                player.sendMessage(ChatColor.RED + "Could not locate a safe teleport location.");
                return;
            }

            player.teleport(destination);
            String ownerName = target.getName() != null ? target.getName() : targetName;
            player.sendMessage(ChatColor.GREEN + "Teleported to " + ChatColor.AQUA + ownerName + ChatColor.GREEN + "'s island.");

            if (!isCoop && !bypass) {
                targetIsland.addVisit(player.getName());
                islandManager.saveIsland(targetIsland);

                long totalVisits = targetIsland.getTotalVisits();
                long playerVisits = targetIsland.getVisitCount(player.getName());
                player.sendMessage(ChatColor.GRAY + "Total visits to this island: " + ChatColor.YELLOW + totalVisits);
                if (playerVisits > 1) {
                    player.sendMessage(ChatColor.GRAY + "Your visits: " + ChatColor.AQUA + playerVisits);
                }
            }
            return;
        }

        teleportHome(player);
    }

    private void teleportHome(Player player) {
        Island island = islandManager.getIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island. Use /island create to create one.");
            return;
        }

        if (island.getCenter() == null) {
            player.sendMessage(ChatColor.RED + "Your island is not available.");
            return;
        }

        Location destination = islandManager.getSafeSpawnLocation(island);
        if (destination == null) {
            player.sendMessage(ChatColor.RED + "Could not locate a safe teleport location.");
            return;
        }

        player.teleport(destination);
        player.sendMessage(ChatColor.GREEN + "Teleported to your island.");
    }

    private void handleCreate(Player player, String[] args) {
        String profileName = args.length >= 2 ? args[1] : null;
        if (profileName == null) {
            islandManager.createIsland(player);
            return;
        }
        islandManager.createIsland(player, profileName);
    }

    private void handleExpand(Player player, String[] args) {
        Island island = islandManager.getIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island. Use /island create to create one.");
            return;
        }

        if (args.length >= 2) {
            try {
                int targetLevel = Integer.parseInt(args[1]);
                islandManager.expandIsland(player, targetLevel);
                return;
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid level. Use a number.");
                return;
            }
        }

        int nextLevel = islandManager.getNextUpgradeLevel(island);
        int nextSize = islandManager.getNextUpgradeSize(island);
        double cost = islandManager.getUpgradeCost(nextLevel);

        if (nextLevel > islandManager.getUpgradeSizes().size()) {
            player.sendMessage(ChatColor.RED + "Your island is at maximum level.");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== Island Expansion ===");
        player.sendMessage(ChatColor.GRAY + "Current Level: " + ChatColor.AQUA + island.getUpgradeLevel());
        player.sendMessage(ChatColor.GRAY + "Current Size: " + ChatColor.AQUA + island.getSize() + "x" + island.getSize());
        player.sendMessage(ChatColor.GRAY + "Next Level: " + ChatColor.GREEN + nextLevel);
        player.sendMessage(ChatColor.GRAY + "Next Size: " + ChatColor.GREEN + nextSize + "x" + nextSize);
        player.sendMessage(ChatColor.GRAY + "Cost: " + ChatColor.RED + "$" + String.format("%.2f", cost));
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Use /island expand " + nextLevel + " to upgrade.");
    }

    private void handleInfo(Player player) {
        Island island = islandManager.getIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return;
        }

        OfflinePlayer owner = Bukkit.getOfflinePlayer(island.getOwner());
        long totalVisits = island.getTotalVisits();
        List<String> recentVisitors = island.getRecentVisitors(5);

        player.sendMessage(ChatColor.GOLD + "=== Island Info ===");
        player.sendMessage(ChatColor.GRAY + "Name: " + ChatColor.WHITE + island.getName());
        player.sendMessage(ChatColor.GRAY + "Owner: " + ChatColor.AQUA + owner.getName());
        player.sendMessage(ChatColor.GRAY + "Level: " + ChatColor.AQUA + island.getUpgradeLevel());
        player.sendMessage(ChatColor.GRAY + "Size: " + ChatColor.AQUA + island.getSize() + "x" + island.getSize());
        player.sendMessage(ChatColor.GRAY + "Created: " + ChatColor.YELLOW + formatDate(island.getCreatedAt()));
        player.sendMessage(ChatColor.GRAY + "Last Visited: " + ChatColor.YELLOW + formatDate(island.getLastVisited()));
        player.sendMessage(ChatColor.GRAY + "Total Visits: " + ChatColor.GREEN + "" + totalVisits);
        if (!recentVisitors.isEmpty()) {
            player.sendMessage(ChatColor.GRAY + "Recent Visitors: " + ChatColor.AQUA + String.join(", ", recentVisitors));
        }
        player.sendMessage(ChatColor.GRAY + "Description: " + ChatColor.ITALIC + island.getDescription());
    }

    private void handleSetHome(Player player) {
        Island island = islandManager.getIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return;
        }

        Location playerLoc = player.getLocation();
        if (!island.isWithinIsland(playerLoc)) {
            player.sendMessage(ChatColor.RED + "You must be on your island to set the spawn point.");
            return;
        }

        island.setSpawnPoint(playerLoc);
        islandManager.saveIsland(island);

        player.sendMessage(ChatColor.GREEN + "Island spawn point set to your current location.");
        player.sendMessage(ChatColor.GRAY + "Use /island go to test it.");
    }

    private void handleSetGuestSpawn(Player player) {
        Island island = islandManager.getIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return;
        }

        Location playerLoc = player.getLocation();
        if (!island.isWithinIsland(playerLoc)) {
            player.sendMessage(ChatColor.RED + "You must be on your island to set the guest spawn point.");
            return;
        }

        island.setGuestSpawnPoint(playerLoc);
        islandManager.saveIsland(island);

        player.sendMessage(ChatColor.GREEN + "Guest spawn point set to your current location.");
        player.sendMessage(ChatColor.GRAY + "Visitors will spawn here when using /visit.");
    }

    private void handleOpen(Player player) {
        Island island = islandManager.getIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return;
        }

        island.setVisitPolicy(Island.VisitPolicy.ANYONE);
        island.setGuestLimit(islandManager.computeGuestLimit(player));
        islandManager.saveIsland(island);

        player.sendMessage(ChatColor.GREEN + "Your island is now open to " + ChatColor.YELLOW + "ANYONE" + ChatColor.GREEN + ".");
    }

    private void handleClose(Player player) {
        Island island = islandManager.getIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return;
        }

        island.setVisitPolicy(Island.VisitPolicy.OFF);
        islandManager.saveIsland(island);

        player.sendMessage(ChatColor.YELLOW + "Your island is now " + ChatColor.RED + "CLOSED" + ChatColor.YELLOW + " to visitors.");
    }

    private void handleVisitSettings(Player player, String[] args) {
        Island island = islandManager.getIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.GOLD + "=== Island Visit Settings ===");
            player.sendMessage(ChatColor.GRAY + "Current: " + ChatColor.YELLOW + island.getVisitPolicy().name());
            player.sendMessage(ChatColor.GRAY + "Guest Limit: " + ChatColor.AQUA + (island.getGuestLimit() < 0 ? "UNLIMITED" : island.getGuestLimit()));
            player.sendMessage("");
            player.sendMessage(ChatColor.YELLOW + "/island visits <off|anyone|friends|guild>");
            player.sendMessage(ChatColor.YELLOW + "/island open" + ChatColor.GRAY + " (shortcut for anyone)");
            player.sendMessage(ChatColor.YELLOW + "/island close" + ChatColor.GRAY + " (shortcut for off)");
            return;
        }

        Island.VisitPolicy policy = parseVisitPolicy(args[1]);
        if (policy == null) {
            player.sendMessage(ChatColor.RED + "Invalid setting.");
            player.sendMessage(ChatColor.YELLOW + "Valid: off, anyone, friends, guild");
            return;
        }

        island.setVisitPolicy(policy);
        island.setGuestLimit(islandManager.computeGuestLimit(player));
        islandManager.saveIsland(island);

        player.sendMessage(ChatColor.GREEN + "Visit setting updated to " + ChatColor.YELLOW + policy.name() + ChatColor.GREEN + ".");
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /invite <player>");
            return;
        }

        Island island = islandManager.getIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "That player is not online.");
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You cannot invite yourself.");
            return;
        }

        long expiresAt = System.currentTimeMillis() + inviteTimeoutMillis();
        Map<UUID, Long> invites = visitInvitesByIsland.computeIfAbsent(island.getId(), ignored -> new HashMap<>());
        cleanupExpiredInvites(invites);
        invites.put(target.getUniqueId(), expiresAt);

        int seconds = (int) Math.max(1L, (expiresAt - System.currentTimeMillis()) / 1000L);
        player.sendMessage(ChatColor.GREEN + "Invited " + ChatColor.AQUA + target.getName() + ChatColor.GREEN + " to visit your island.");
        target.sendMessage(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " has invited you to visit their island!");
        target.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.AQUA + "/visit " + player.getName() + ChatColor.YELLOW + " to accept. " + ChatColor.GRAY + "(" + seconds + "s)");
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /sbkick <player>");
            return;
        }

        Island island = resolveKickIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "That player is not online.");
            return;
        }

        if (island.getOwner() != null && island.getOwner().equals(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You cannot kick the island owner.");
            return;
        }
        if (island.isMember(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You cannot kick a co-op member.");
            return;
        }
        if (!island.isWithinIsland(target.getLocation())) {
            player.sendMessage(ChatColor.RED + "That player is not on your island.");
            return;
        }

        kickToHub(target);
        player.sendMessage(ChatColor.YELLOW + "Kicked " + ChatColor.AQUA + target.getName() + ChatColor.YELLOW + " from your island.");
        target.sendMessage(ChatColor.RED + "You were kicked from the island.");
    }

    private void handleKickAll(Player player) {
        Island island = resolveKickIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return;
        }
        if (island.getCenter() == null || island.getCenter().getWorld() == null) {
            player.sendMessage(ChatColor.RED + "Your island is not available.");
            return;
        }

        int kicked = 0;
        for (Player online : island.getCenter().getWorld().getPlayers()) {
            if (online == null || !online.isOnline()) {
                continue;
            }
            UUID id = online.getUniqueId();
            if (id.equals(player.getUniqueId())) {
                continue;
            }
            if (island.getOwner() != null && island.getOwner().equals(id)) {
                continue;
            }
            if (island.isMember(id)) {
                continue;
            }
            if (!island.isWithinIsland(online.getLocation())) {
                continue;
            }
            kickToHub(online);
            online.sendMessage(ChatColor.RED + "You were kicked from the island.");
            kicked++;
        }

        player.sendMessage(ChatColor.YELLOW + "Kicked " + ChatColor.AQUA + kicked + ChatColor.YELLOW + " guest(s) from your island.");
    }

    private void handleLeave(Player player) {
        if (player == null || profileManager == null) {
            return;
        }

        SkyBlockProfile selected = profileManager.getSelectedProfile(player);
        if (selected == null || !selected.isCoopMemberProfile()) {
            player.sendMessage(ChatColor.RED + "You can only leave a coop while using its coop profile.");
            player.sendMessage(ChatColor.GRAY + "Switch to the coop profile first, then use /island leave.");
            return;
        }

        UUID playerId = player.getUniqueId();
        UUID sharedProfileId = selected.getCanonicalProfileId();
        List<SkyBlockProfile> coopProfiles = profileManager.getCoopProfiles(playerId, sharedProfileId);
        if (coopProfiles.isEmpty()) {
            player.sendMessage(ChatColor.RED + "That coop profile is already detached.");
            return;
        }

        islandManager.saveProfileInventory(player, selected.getProfileId());

        Island island = islandManager.getIslandByProfileId(sharedProfileId);
        if (island != null) {
            islandManager.removeCoopMember(island, playerId);
        }

        for (SkyBlockProfile coopProfile : coopProfiles) {
            profileManager.deleteProfile(playerId, coopProfile.getProfileId());
        }

        ensureSelectedProfileAfterCoopRemoval(player);
        coopInvitesByInvitee.remove(playerId);
        player.sendMessage(ChatColor.YELLOW + "You left that coop profile.");
    }

    private void handleProfile(Player player, String[] args) {
        if (profileManager == null) {
            player.sendMessage(ChatColor.RED + "Profile system is not available.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /island profile <list|create|switch|delete> [name]");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "list" -> {
                List<SkyBlockProfile> profiles = profileManager.getPlayerProfiles(player);
                if (profiles.isEmpty()) {
                    player.sendMessage(ChatColor.GRAY + "You have no profiles.");
                    return;
                }
                SkyBlockProfile selected = profileManager.getSelectedProfile(player);
                player.sendMessage(ChatColor.GOLD + "Your Profiles:");
                for (SkyBlockProfile profile : profiles) {
                    boolean isSelected = selected != null && profile.getProfileId().equals(selected.getProfileId());
                    String coopTag = profile.isCoopMemberProfile() ? ChatColor.AQUA + " [COOP]" : "";
                    player.sendMessage(ChatColor.YELLOW + "- " + profile.getProfileName() + coopTag + (isSelected ? ChatColor.GREEN + " [SELECTED]" : ""));
                }
            }
            case "create" -> {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.YELLOW + "Usage: /island profile create <name>");
                    return;
                }
                String name = args[2];
                SkyBlockProfile created = profileManager.createProfile(player, name);
                if (created == null) {
                    return;
                }
                profileManager.selectProfile(player, created.getProfileId());
                islandManager.createIsland(player);
            }
            case "switch" -> {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.YELLOW + "Usage: /island profile switch <name>");
                    return;
                }
                profileManager.selectProfile(player, args[2]);
            }
            case "delete" -> {
                SkyBlockProfile target;
                if (args.length >= 3) {
                    target = profileManager.getProfile(player, args[2]);
                } else {
                    target = profileManager.getSelectedProfile(player);
                }
                if (target == null) {
                    player.sendMessage(ChatColor.RED + "Profile not found.");
                    return;
                }

                UUID targetId = target.getProfileId();
                String targetName = target.getProfileName();
                if (!profileManager.deleteProfile(player, targetName)) {
                    return;
                }
                islandManager.deleteIslandForProfile(player.getUniqueId(), targetId, targetName);
                player.sendMessage(ChatColor.YELLOW + "Deleted island data for profile '" + targetName + "'.");
            }
            default -> player.sendMessage(ChatColor.YELLOW + "Usage: /island profile <list|create|switch|delete> [name]");
        }
    }

    private void handleDelete(Player player, String[] args) {
        Island island = islandManager.getIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island to delete.");
            return;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            player.sendMessage(ChatColor.YELLOW + "This will delete your island permanently.");
            player.sendMessage(ChatColor.YELLOW + "Type: " + ChatColor.RED + "/island delete confirm");
            return;
        }

        islandManager.deleteIsland(player.getUniqueId());
        if (hubCommand != null) {
            hubCommand.teleportToHub(player);
        } else {
            Location hub = Bukkit.getWorlds().get(0).getSpawnLocation();
            player.teleport(hub);
        }
        player.sendMessage(ChatColor.GREEN + "Your island has been deleted.");
    }

    private void handleWarp(Player player) {
        if (partyManager == null) {
            player.sendMessage(ChatColor.RED + "Party system is not available.");
            return;
        }

        String error = partyManager.warpPartyToIsland(player);
        if (error != null) {
            player.sendMessage(ChatColor.RED + error);
        }
    }

    private void handleSetName(Player player, String[] args) {
        Island island = islandManager.getIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /island setname <name>");
            return;
        }

        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            nameBuilder.append(args[i]).append(" ");
        }
        String newName = nameBuilder.toString().trim();

        if (newName.length() > 32) {
            player.sendMessage(ChatColor.RED + "Name must be 32 characters or less.");
            return;
        }

        island.setName(newName);
        islandManager.saveIsland(island);

        player.sendMessage(ChatColor.GREEN + "Island name set to: " + ChatColor.AQUA + newName);
    }

    private void handleSetDesc(Player player, String[] args) {
        Island island = islandManager.getIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /island setdesc <description>");
            return;
        }

        StringBuilder descBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            descBuilder.append(args[i]).append(" ");
        }
        String newDesc = descBuilder.toString().trim();

        if (newDesc.length() > 100) {
            player.sendMessage(ChatColor.RED + "Description must be 100 characters or less.");
            return;
        }

        island.setDescription(newDesc);
        islandManager.saveIsland(island);

        player.sendMessage(ChatColor.GREEN + "Island description updated.");
    }

    private void handleCoop(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /island coop <add|remove|list|accept> <player>");
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "add" -> handleCoopAdd(player, args);
            case "remove" -> handleCoopRemove(player, args);
            case "list" -> handleCoopList(player);
            case "accept" -> handleCoopAccept(player, args);
            default -> player.sendMessage(ChatColor.YELLOW + "Usage: /island coop <add|remove|list|accept> <player>");
        }
    }

    private void handleCoopAdd(Player player, String[] args) {
        if (profileManager == null) {
            player.sendMessage(ChatColor.RED + "Profile system is not available.");
            return;
        }
        Island island = islandManager.getIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return;
        }
        if (args.length < 3) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /island coop add <player>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "That player must be online to accept a coop invite.");
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You cannot invite yourself.");
            return;
        }
        if (island.getOwner() != null && island.getOwner().equals(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "That player already owns this island.");
            return;
        }
        if (island.isMember(target.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "That player is already in this coop.");
            return;
        }
        if (island.getProfileId() == null) {
            player.sendMessage(ChatColor.RED + "This island profile is unavailable.");
            return;
        }
        if (islandManager.getCoopProfileId(target.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "That player is already in another coop.");
            return;
        }
        if (profileManager.getCoopProfile(target.getUniqueId(), island.getProfileId()) != null) {
            player.sendMessage(ChatColor.RED + "That player already has a linked profile for this coop.");
            return;
        }

        if (island.getMembers().size() + 1 >= island.getMaxMembers()) {
            player.sendMessage(ChatColor.RED + "Your island co-op is full!");
            player.sendMessage(ChatColor.GRAY + "Current limit: " + island.getMaxMembers() + " (including owner)");
            player.sendMessage(ChatColor.YELLOW + "Upgrade your co-op limit in the Island Upgrades menu.");
            return;
        }

        cleanupExpiredCoopInvites();
        long expiresAt = System.currentTimeMillis() + inviteTimeoutMillis();
        coopInvitesByInvitee.put(target.getUniqueId(),
                new CoopInvite(island.getId(), island.getProfileId(), player.getUniqueId(), expiresAt));

        int seconds = (int) Math.max(1L, (expiresAt - System.currentTimeMillis()) / 1000L);
        player.sendMessage(ChatColor.GREEN + "Sent a coop invite to " + ChatColor.AQUA + target.getName() + ChatColor.GREEN + ".");
        target.sendMessage(ChatColor.GOLD + player.getName() + ChatColor.YELLOW + " invited you to join their coop.");
        target.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.AQUA + "/island coop accept " + player.getName()
                + ChatColor.YELLOW + " to join. " + ChatColor.GRAY + "(" + seconds + "s)");
    }

    private void handleCoopRemove(Player player, String[] args) {
        if (profileManager == null) {
            player.sendMessage(ChatColor.RED + "Profile system is not available.");
            return;
        }
        Island island = islandManager.getIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return;
        }
        if (args.length < 3) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /island coop remove <player>");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        UUID targetId = target.getUniqueId();
        if (targetId.equals(player.getUniqueId())) {
            handleLeave(player);
            return;
        }
        if (island.getOwner() != null && island.getOwner().equals(targetId)) {
            player.sendMessage(ChatColor.RED + "You cannot remove the island owner from their own coop.");
            return;
        }
        if (!island.isMember(targetId)) {
            player.sendMessage(ChatColor.RED + "That player is not in this coop.");
            return;
        }

        List<SkyBlockProfile> coopProfiles = island.getProfileId() == null
                ? List.of()
                : profileManager.getCoopProfiles(targetId, island.getProfileId());
        Player onlineTarget = Bukkit.getPlayer(targetId);
        if (onlineTarget != null && onlineTarget.isOnline()) {
            SkyBlockProfile selected = profileManager.getSelectedProfile(onlineTarget);
            if (selected != null && selected.isCoopMemberProfile()
                    && island.getProfileId() != null
                    && island.getProfileId().equals(selected.getSharedProfileId())) {
                islandManager.saveProfileInventory(onlineTarget, selected.getProfileId());
            }
        }

        islandManager.removeCoopMember(island, targetId);
        for (SkyBlockProfile coopProfile : coopProfiles) {
            profileManager.deleteProfile(targetId, coopProfile.getProfileId());
        }
        coopInvitesByInvitee.remove(targetId);

        if (onlineTarget != null && onlineTarget.isOnline()) {
            ensureSelectedProfileAfterCoopRemoval(onlineTarget);
            onlineTarget.sendMessage(ChatColor.RED + "You were removed from the coop on " + ChatColor.AQUA + island.getName() + ChatColor.RED + ".");
        }

        String targetName = target.getName() != null ? target.getName() : args[2];
        player.sendMessage(ChatColor.YELLOW + "Removed " + ChatColor.AQUA + targetName + ChatColor.YELLOW + " from your island coop.");
    }

    private void handleCoopList(Player player) {
        Island island = islandManager.getIsland(player);
        if (island == null) {
            player.sendMessage(ChatColor.RED + "You don't have an island.");
            return;
        }

        List<String> names = new ArrayList<>();
        OfflinePlayer owner = Bukkit.getOfflinePlayer(island.getOwner());
        names.add((owner.getName() != null ? owner.getName() : island.getOwner().toString()) + " (Owner)");
        for (UUID id : island.getMembers()) {
            OfflinePlayer member = Bukkit.getOfflinePlayer(id);
            names.add(member.getName() != null ? member.getName() : id.toString());
        }
        player.sendMessage(ChatColor.GREEN + "Coop members: " + ChatColor.AQUA + String.join(", ", names));
    }

    private void handleCoopAccept(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /island coop accept <player>");
            return;
        }

        cleanupExpiredCoopInvites();
        CoopInvite invite = coopInvitesByInvitee.get(player.getUniqueId());
        if (invite == null) {
            player.sendMessage(ChatColor.RED + "You do not have a pending coop invite.");
            return;
        }

        OfflinePlayer inviter = Bukkit.getOfflinePlayer(args[2]);
        if (!inviter.getUniqueId().equals(invite.inviterId())) {
            player.sendMessage(ChatColor.RED + "That player did not send your active coop invite.");
            return;
        }

        Island targetIsland = islandManager.getIslandById(invite.islandId());
        if (targetIsland == null || targetIsland.getProfileId() == null
                || !targetIsland.getProfileId().equals(invite.sharedProfileId())) {
            coopInvitesByInvitee.remove(player.getUniqueId());
            player.sendMessage(ChatColor.RED + "That coop invite is no longer valid.");
            return;
        }

        UUID currentCoopProfileId = islandManager.getCoopProfileId(player.getUniqueId());
        if (currentCoopProfileId != null && !currentCoopProfileId.equals(invite.sharedProfileId())) {
            player.sendMessage(ChatColor.RED + "You are already in another coop.");
            return;
        }
        if (profileManager == null) {
            player.sendMessage(ChatColor.RED + "Profile system is not available.");
            return;
        }

        SkyBlockProfile sharedProfile = profileManager.getProfile(invite.sharedProfileId());
        if (sharedProfile == null) {
            coopInvitesByInvitee.remove(player.getUniqueId());
            player.sendMessage(ChatColor.RED + "That coop profile is unavailable.");
            return;
        }

        SkyBlockProfile coopProfile = profileManager.getCoopProfile(player.getUniqueId(), invite.sharedProfileId());
        if (coopProfile == null) {
            coopProfile = profileManager.createCoopProfile(player, sharedProfile);
            if (coopProfile == null) {
                player.sendMessage(ChatColor.RED + "Could not create your coop profile.");
                player.sendMessage(ChatColor.GRAY + "Make sure you have a free profile slot before accepting.");
                return;
            }
        }

        boolean joined = islandManager.addCoopMember(targetIsland, player.getUniqueId());
        if (!joined) {
            player.sendMessage(ChatColor.RED + "Could not join that coop.");
            player.sendMessage(ChatColor.GRAY + "You may already be in another co-op, or the target island profile is unavailable.");
            return;
        }

        coopInvitesByInvitee.remove(player.getUniqueId());
        profileManager.selectProfile(player, coopProfile.getProfileId());
        String inviterName = inviter.getName() != null ? inviter.getName() : args[2];
        player.sendMessage(ChatColor.GREEN + "Joined " + ChatColor.AQUA + inviterName + ChatColor.GREEN + "'s coop profile.");

        Player onlineInviter = Bukkit.getPlayer(invite.inviterId());
        if (onlineInviter != null && onlineInviter.isOnline()) {
            onlineInviter.sendMessage(ChatColor.GREEN + player.getName() + " joined your coop.");
        }
    }

    private boolean hasVisitBypass(Player player) {
        return player != null && (player.hasPermission("grivience.admin")
                || player.hasPermission("grivience.island.bypass")
                || player.hasPermission("grivience.visit.bypass"));
    }

    private void handleMinions(Player player) {
        if (player == null) {
            return;
        }
        if (plugin.getMinionGuiManager() != null) {
            plugin.getMinionGuiManager().openOverview(player);
            return;
        }
        player.sendMessage(ChatColor.RED + "Minion system unavailable.");
    }

    private long inviteTimeoutMillis() {
        int seconds = 60;
        if (plugin != null) {
            seconds = plugin.getConfig().getInt("skyblock.visiting.invite-timeout-seconds", 60);
        }
        seconds = Math.max(10, Math.min(3600, seconds));
        return seconds * 1000L;
    }

    private void cleanupExpiredInvites(Map<UUID, Long> invites) {
        if (invites == null || invites.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        invites.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue() < now);
    }

    private void cleanupExpiredCoopInvites() {
        long now = System.currentTimeMillis();
        coopInvitesByInvitee.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().expiresAtMillis() < now);
    }

    private void ensureSelectedProfileAfterCoopRemoval(Player player) {
        if (player == null || profileManager == null) {
            return;
        }

        SkyBlockProfile selected = profileManager.getSelectedProfile(player);
        if (selected != null) {
            return;
        }

        SkyBlockProfile created = profileManager.createProfile(player, "Default");
        if (created != null) {
            return;
        }

        selected = profileManager.getSelectedProfile(player);
        if (selected == null) {
            player.sendMessage(ChatColor.RED + "You no longer have a usable Skyblock profile selected.");
            player.sendMessage(ChatColor.GRAY + "Use /profile create <name> to make a new solo profile.");
        }
    }

    private boolean hasValidVisitInvite(UUID islandId, UUID visitorId) {
        if (islandId == null || visitorId == null) {
            return false;
        }
        Map<UUID, Long> invites = visitInvitesByIsland.get(islandId);
        if (invites == null || invites.isEmpty()) {
            return false;
        }
        cleanupExpiredInvites(invites);
        if (invites.isEmpty()) {
            visitInvitesByIsland.remove(islandId);
            return false;
        }
        Long expiresAt = invites.get(visitorId);
        return expiresAt != null && expiresAt >= System.currentTimeMillis();
    }

    private Island resolveTargetIsland(OfflinePlayer target, Player onlineTarget) {
        if (onlineTarget != null) {
            return islandManager.getIsland(onlineTarget);
        }
        if (target == null || target.getUniqueId() == null) {
            return null;
        }

        UUID ownerId = target.getUniqueId();
        if (profileManager != null) {
            SkyBlockProfile selected = profileManager.getSelectedProfile(ownerId);
            if (selected != null) {
                Island byProfile = islandManager.getIslandByProfileId(selected.getCanonicalProfileId());
                if (byProfile != null) {
                    return byProfile;
                }

                // Legacy islands keyed by profile name: migrate on access.
                for (Island candidate : islandManager.getIslandsForOwner(ownerId)) {
                    if (candidate == null) {
                        continue;
                    }
                    if (candidate.getProfileName() != null && candidate.getProfileName().equalsIgnoreCase(selected.getProfileName())) {
                        if (candidate.getProfileId() == null) {
                            candidate.setProfileId(selected.getCanonicalProfileId());
                            islandManager.saveIsland(candidate);
                        }
                        return candidate;
                    }
                }
            }
        }

        return islandManager.getIsland(ownerId);
    }

    private Island resolveKickIsland(Player player) {
        if (player == null) {
            return null;
        }
        UUID playerId = player.getUniqueId();

        Island current = islandManager.getIslandAt(player.getLocation());
        if (current != null) {
            if ((current.getOwner() != null && current.getOwner().equals(playerId)) || current.isMember(playerId)) {
                return current;
            }
        }

        Island owned = islandManager.getIsland(player);
        if (owned != null) {
            if ((owned.getOwner() != null && owned.getOwner().equals(playerId)) || owned.isMember(playerId)) {
                return owned;
            }
        }

        return null;
    }

    private void kickToHub(Player player) {
        if (player == null) {
            return;
        }
        if (hubCommand != null) {
            hubCommand.teleportToHub(player);
            return;
        }
        Location hub = Bukkit.getWorlds().getFirst().getSpawnLocation();
        player.teleport(hub);
    }

    private Island.VisitPolicy parseVisitPolicy(String input) {
        if (input == null) {
            return null;
        }
        String raw = input.trim().toLowerCase(Locale.ROOT);
        return switch (raw) {
            case "off", "closed", "close" -> Island.VisitPolicy.OFF;
            case "anyone", "open", "all", "public" -> Island.VisitPolicy.ANYONE;
            case "friends", "friend" -> Island.VisitPolicy.FRIENDS;
            case "guild" -> Island.VisitPolicy.GUILD;
            default -> null;
        };
    }

    private boolean canVisit(Player visitor, Island island, boolean invited) {
        if (visitor == null || island == null) {
            return false;
        }
        if (invited) {
            return true;
        }

        Island.VisitPolicy policy = island.getVisitPolicy();
        if (policy == null) {
            policy = Island.VisitPolicy.OFF;
        }

        return switch (policy) {
            case ANYONE -> true;
            case OFF -> false;
            case FRIENDS -> areFriends(visitor.getUniqueId(), island.getOwner());
            case GUILD -> areInSameGuild(visitor.getUniqueId(), island.getOwner());
        };
    }

    private boolean areFriends(UUID visitorId, UUID ownerId) {
        if (visitorId == null || ownerId == null) {
            return false;
        }
        if (partyManager == null) {
            return false;
        }
        return partyManager.areInSameParty(visitorId, ownerId);
    }

    private boolean areInSameGuild(UUID visitorId, UUID ownerId) {
        // Optional integration via LuckPerms meta. If you don't set this up, GUILD behaves like OFF.
        if (visitorId == null || ownerId == null) {
            return false;
        }
        String key = "guild";
        if (plugin != null) {
            key = plugin.getConfig().getString("skyblock.visiting.guild-meta-key", "guild");
        }
        String guildA = luckPermsMeta(visitorId, key);
        String guildB = luckPermsMeta(ownerId, key);
        if (guildA == null || guildB == null) {
            return false;
        }
        if (guildA.isBlank() || guildB.isBlank()) {
            return false;
        }
        return guildA.equalsIgnoreCase(guildB);
    }

    private String luckPermsMeta(UUID playerId, String key) {
        if (playerId == null || key == null || key.isBlank()) {
            return null;
        }
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            return null;
        }

        try {
            Object services = Bukkit.getServicesManager();
            Class<?> luckPermsClass = Class.forName("net.luckperms.api.LuckPerms");
            Object registration = services.getClass().getMethod("getRegistration", Class.class).invoke(services, luckPermsClass);
            if (registration == null) {
                return null;
            }
            Object api = registration.getClass().getMethod("getProvider").invoke(registration);
            if (api == null) {
                return null;
            }

            Object userManager = api.getClass().getMethod("getUserManager").invoke(api);
            if (userManager == null) {
                return null;
            }
            Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, playerId);
            if (user == null) {
                return null;
            }

            Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
            if (cachedData == null) {
                return null;
            }
            Object metaData = cachedData.getClass().getMethod("getMetaData").invoke(cachedData);
            if (metaData == null) {
                return null;
            }
            Object value = metaData.getClass().getMethod("getMetaValue", String.class).invoke(metaData, key);
            return value instanceof String s ? s : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void sendVisitDeniedMessage(Player player, Island island) {
        if (player == null || island == null) {
            return;
        }
        Island.VisitPolicy policy = island.getVisitPolicy();
        if (policy == null) {
            policy = Island.VisitPolicy.OFF;
        }
        switch (policy) {
            case OFF -> player.sendMessage(ChatColor.RED + "That island is closed to visitors.");
            case FRIENDS -> player.sendMessage(ChatColor.RED + "That island is only open to friends.");
            case GUILD -> player.sendMessage(ChatColor.RED + "That island is only open to guild members.");
            case ANYONE -> player.sendMessage(ChatColor.RED + "You cannot visit that island right now.");
        }
        player.sendMessage(ChatColor.GRAY + "You need permission or an invite to visit.");
    }

    private boolean isIslandFullForNewGuest(Player visitor, Island island) {
        if (visitor == null || island == null) {
            return false;
        }
        int limit = island.getGuestLimit();
        if (limit < 0) {
            return false; // unlimited
        }
        if (limit <= 0) {
            limit = 1;
        }

        // Already on the island: don't block.
        if (island.isWithinIsland(visitor.getLocation())) {
            return false;
        }

        if (island.getCenter() == null || island.getCenter().getWorld() == null) {
            return false;
        }

        int guests = 0;
        for (Player online : island.getCenter().getWorld().getPlayers()) {
            if (online == null || !online.isOnline()) {
                continue;
            }
            UUID id = online.getUniqueId();
            if (island.getOwner() != null && island.getOwner().equals(id)) {
                continue;
            }
            if (island.isMember(id)) {
                continue;
            }
            if (!island.isWithinIsland(online.getLocation())) {
                continue;
            }
            guests++;
        }

        return guests >= limit;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Island Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/island create" + ChatColor.GRAY + " - Create your island");
        player.sendMessage(ChatColor.YELLOW + "/island go" + ChatColor.GRAY + " - Teleport to your island");
        player.sendMessage(ChatColor.YELLOW + "/island go <player>" + ChatColor.GRAY + " - Visit a player's island");
        player.sendMessage(ChatColor.YELLOW + "/island expand" + ChatColor.GRAY + " - View expansion options");
        player.sendMessage(ChatColor.YELLOW + "/island expand <level>" + ChatColor.GRAY + " - Expand island size");
        player.sendMessage(ChatColor.YELLOW + "/island info" + ChatColor.GRAY + " - View island information");
        player.sendMessage(ChatColor.YELLOW + "/island sethome" + ChatColor.GRAY + " - Set island spawn point");
        player.sendMessage(ChatColor.YELLOW + "/island setguestspawn" + ChatColor.GRAY + " - Set visitor spawn point");
        player.sendMessage(ChatColor.YELLOW + "/island open" + ChatColor.GRAY + " - Allow anyone to visit");
        player.sendMessage(ChatColor.YELLOW + "/island close" + ChatColor.GRAY + " - Close island to visitors");
        player.sendMessage(ChatColor.YELLOW + "/island visits" + ChatColor.GRAY + " - Configure visit settings");
        player.sendMessage(ChatColor.YELLOW + "/island invite <player>" + ChatColor.GRAY + " - Invite a player to visit");
        player.sendMessage(ChatColor.YELLOW + "/island kick <player>" + ChatColor.GRAY + " - Kick a visitor from your island");
        player.sendMessage(ChatColor.YELLOW + "/island kickall" + ChatColor.GRAY + " - Kick all visitors from your island");
        player.sendMessage(ChatColor.YELLOW + "/island setname <name>" + ChatColor.GRAY + " - Rename your island");
        player.sendMessage(ChatColor.YELLOW + "/island setdesc <desc>" + ChatColor.GRAY + " - Set island description");
        player.sendMessage(ChatColor.YELLOW + "/island minions" + ChatColor.GRAY + " - Open minion management");
        player.sendMessage(ChatColor.YELLOW + "/island warp" + ChatColor.GRAY + " - Warp party to your island (Leader)");
        player.sendMessage(ChatColor.YELLOW + "/island coop <add|remove|list|accept> <player>" + ChatColor.GRAY + " - Manage coop members");
        player.sendMessage(ChatColor.YELLOW + "/island profile <list|create|switch|delete> <name>" + ChatColor.GRAY + " - Manage profiles");
        player.sendMessage(ChatColor.YELLOW + "/island delete confirm" + ChatColor.GRAY + " - Delete your island");
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
        return null;
    }

    private String formatDate(long timestamp) {
        long days = (System.currentTimeMillis() - timestamp) / (1000 * 60 * 60 * 24);
        if (days <= 0) {
            return "Today";
        } else if (days == 1) {
            return "Yesterday";
        } else if (days < 7) {
            return days + " days ago";
        } else if (days < 30) {
            return (days / 7) + " weeks ago";
        } else if (days < 365) {
            return (days / 30) + " months ago";
        } else {
            return (days / 365) + " years ago";
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String root = command.getName().toLowerCase(Locale.ROOT);
        switch (root) {
            case "visit", "invite", "sbkick" -> {
                if (args.length == 1) {
                    List<String> players = new ArrayList<>();
                    UUID senderId = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (senderId == null || !online.getUniqueId().equals(senderId)) {
                            players.add(online.getName());
                        }
                    }
                    return filterPrefix(players, args[0]);
                }
                return List.of();
            }
            case "sbkickall", "setguestspawn", "setspawn" -> {
                return List.of();
            }
            default -> {
                // Continue to /island completion below.
            }
        }

        if (args.length == 1) {
            List<String> commands = new ArrayList<>(List.of(
                    "go", "create", "expand", "info", "sethome", "setspawn", "setguestspawn",
                    "open", "close", "visits", "invite", "kick", "kickall", "minions",
                    "setname", "setdesc", "leave", "warp", "coop", "profile", "delete", "help"
            ));
            return filterPrefix(commands, args[0]);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("go") || args[0].equalsIgnoreCase("visit"))) {
            List<String> players = new ArrayList<>();
            UUID senderId = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (senderId == null || !online.getUniqueId().equals(senderId)) {
                    players.add(online.getName());
                }
            }
            return filterPrefix(players, args[1]);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick"))) {
            List<String> players = new ArrayList<>();
            UUID senderId = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (senderId == null || !online.getUniqueId().equals(senderId)) {
                    players.add(online.getName());
                }
            }
            return filterPrefix(players, args[1]);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("visits") || args[0].equalsIgnoreCase("visitsettings") || args[0].equalsIgnoreCase("privacy"))) {
            return filterPrefix(List.of("off", "anyone", "friends", "guild"), args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("profile")) {
            return filterPrefix(List.of("list", "create", "switch", "delete"), args[1]);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("coop")) {
            return filterPrefix(List.of("add", "remove", "list", "accept"), args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("coop")
                && (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("accept"))) {
            List<String> players = new ArrayList<>();
            UUID senderId = sender instanceof Player player ? player.getUniqueId() : null;
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (senderId == null || !online.getUniqueId().equals(senderId)) {
                    players.add(online.getName());
                }
            }
            return filterPrefix(players, args[2]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("profile")) {
            if (args[1].equalsIgnoreCase("switch") || args[1].equalsIgnoreCase("delete") || args[1].equalsIgnoreCase("create")) {
                return List.of("<name>");
            }
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("expand")) {
            if (sender instanceof Player player) {
                Island island = islandManager.getIsland(player);
                if (island != null) {
                    int nextLevel = islandManager.getNextUpgradeLevel(island);
                    int maxLevel = islandManager.getUpgradeSizes().size();
                    List<String> levels = new ArrayList<>();
                    for (int i = nextLevel; i <= maxLevel; i++) {
                        levels.add(String.valueOf(i));
                    }
                    return filterPrefix(levels, args[1]);
                }
            }
        }

        return List.of();
    }

    private List<String> filterPrefix(List<String> input, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> filtered = new ArrayList<>();
        for (String candidate : input) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                filtered.add(candidate);
            }
        }
        return filtered;
    }
}
