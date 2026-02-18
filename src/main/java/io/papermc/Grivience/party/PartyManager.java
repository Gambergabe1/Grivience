package io.papermc.Grivience.party;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PartyManager {
    private final GriviencePlugin plugin;
    private final Map<UUID, Party> partiesById = new HashMap<>();
    private final Map<UUID, UUID> partyByPlayer = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> invitesByTarget = new HashMap<>();

    private int maxPartySize;
    private long inviteTimeoutMillis;

    public PartyManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        maxPartySize = Math.max(2, plugin.getConfig().getInt("dungeons.max-party-size", 5));
        int inviteTimeoutSeconds = Math.max(15, plugin.getConfig().getInt("dungeons.invite-timeout-seconds", 60));
        inviteTimeoutMillis = inviteTimeoutSeconds * 1000L;
    }

    public synchronized String createParty(Player leader) {
        if (isInParty(leader.getUniqueId())) {
            return "You are already in a party.";
        }
        Party party = new Party(leader.getUniqueId());
        partiesById.put(party.id(), party);
        partyByPlayer.put(leader.getUniqueId(), party.id());
        return null;
    }

    public synchronized String invite(Player inviter, Player target) {
        if (inviter.getUniqueId().equals(target.getUniqueId())) {
            return "You cannot invite yourself.";
        }

        Party party = getParty(inviter.getUniqueId());
        if (party == null) {
            String createResult = createParty(inviter);
            if (createResult != null) {
                return createResult;
            }
            party = getParty(inviter.getUniqueId());
        }

        if (party == null) {
            return "Party was not available. Try again.";
        }
        if (!party.isLeader(inviter.getUniqueId())) {
            return "Only the party leader can invite players.";
        }
        if (party.size() >= maxPartySize) {
            return "Party is full (" + maxPartySize + ").";
        }
        if (isInParty(target.getUniqueId())) {
            return target.getName() + " is already in a party.";
        }

        Map<UUID, Long> targetInvites = invitesByTarget.computeIfAbsent(target.getUniqueId(), ignored -> new HashMap<>());
        targetInvites.put(inviter.getUniqueId(), System.currentTimeMillis() + inviteTimeoutMillis);
        return null;
    }

    public synchronized String acceptInvite(Player target, UUID leaderId) {
        if (isInParty(target.getUniqueId())) {
            return "You are already in a party.";
        }

        Map<UUID, Long> invites = invitesByTarget.get(target.getUniqueId());
        if (invites == null || invites.isEmpty()) {
            return "You have no pending invites.";
        }

        cleanupExpiredInvites(target.getUniqueId());
        Long expiresAt = invites.get(leaderId);
        if (expiresAt == null || expiresAt < System.currentTimeMillis()) {
            return "That invite expired or does not exist.";
        }

        Party party = getParty(leaderId);
        if (party == null) {
            return "That party no longer exists.";
        }
        if (party.size() >= maxPartySize) {
            return "That party is already full.";
        }

        party.addMember(target.getUniqueId());
        partyByPlayer.put(target.getUniqueId(), party.id());
        invites.remove(leaderId);
        if (invites.isEmpty()) {
            invitesByTarget.remove(target.getUniqueId());
        }
        return null;
    }

    public synchronized String leave(Player player) {
        Party party = getParty(player.getUniqueId());
        if (party == null) {
            return "You are not in a party.";
        }

        UUID leavingId = player.getUniqueId();
        party.removeMember(leavingId);
        partyByPlayer.remove(leavingId);

        if (party.isEmpty()) {
            partiesById.remove(party.id());
            return null;
        }
        if (!partiesById.containsKey(party.id())) {
            partiesById.put(party.id(), party);
        }
        return null;
    }

    public synchronized String kick(Player leader, UUID targetId) {
        Party party = getParty(leader.getUniqueId());
        if (party == null) {
            return "You are not in a party.";
        }
        if (!party.isLeader(leader.getUniqueId())) {
            return "Only the party leader can kick members.";
        }
        if (!party.hasMember(targetId)) {
            return "That player is not in your party.";
        }
        if (leader.getUniqueId().equals(targetId)) {
            return "Use /dungeon party leave to leave your own party.";
        }

        party.removeMember(targetId);
        partyByPlayer.remove(targetId);
        return null;
    }

    public synchronized Party getParty(UUID playerId) {
        UUID partyId = partyByPlayer.get(playerId);
        if (partyId == null) {
            return null;
        }
        return partiesById.get(partyId);
    }

    public synchronized boolean isInParty(UUID playerId) {
        return partyByPlayer.containsKey(playerId);
    }

    public synchronized boolean areInSameParty(UUID first, UUID second) {
        if (first == null || second == null) {
            return false;
        }
        UUID firstParty = partyByPlayer.get(first);
        if (firstParty == null) {
            return false;
        }
        UUID secondParty = partyByPlayer.get(second);
        return firstParty.equals(secondParty);
    }

    public synchronized Collection<UUID> getInviteSenders(UUID targetId) {
        cleanupExpiredInvites(targetId);
        Map<UUID, Long> invites = invitesByTarget.get(targetId);
        if (invites == null) {
            return List.of();
        }
        return List.copyOf(invites.keySet());
    }

    public synchronized List<String> formatParty(Party party) {
        List<String> lines = new ArrayList<>();
        lines.add("Party " + shortId(party.id()) + " (" + party.size() + "/" + maxPartySize + ")");

        List<UUID> sorted = new ArrayList<>(party.members());
        sorted.sort(Comparator.comparing(this::nameOf, String.CASE_INSENSITIVE_ORDER));
        for (UUID memberId : sorted) {
            String marker = party.isLeader(memberId) ? "Leader" : "Member";
            lines.add("- " + nameOf(memberId) + " [" + marker + "]");
        }
        return lines;
    }

    public int maxPartySize() {
        return maxPartySize;
    }

    public synchronized String nameOf(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            return online.getName();
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        if (offlinePlayer.getName() != null) {
            return offlinePlayer.getName();
        }
        return uuid.toString().substring(0, 8).toLowerCase(Locale.ROOT);
    }

    public synchronized Set<UUID> membersOf(Party party) {
        return new HashSet<>(party.mutableMembers());
    }

    public synchronized List<PartySnapshot> partyFinderSnapshots() {
        List<PartySnapshot> snapshots = new ArrayList<>();
        for (Party party : partiesById.values()) {
            if (party.size() >= maxPartySize) {
                continue;
            }
            List<UUID> members = new ArrayList<>(party.members());
            List<String> memberNames = new ArrayList<>(members.size());
            for (UUID memberId : members) {
                memberNames.add(nameOf(memberId));
            }
            snapshots.add(new PartySnapshot(
                    party.id(),
                    party.leader(),
                    nameOf(party.leader()),
                    party.size(),
                    maxPartySize,
                    memberNames
            ));
        }
        snapshots.sort(Comparator.comparingInt(PartySnapshot::size).reversed());
        return snapshots;
    }

    private void cleanupExpiredInvites(UUID targetId) {
        Map<UUID, Long> invites = invitesByTarget.get(targetId);
        if (invites == null || invites.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        invites.entrySet().removeIf(entry -> entry.getValue() < now);
        if (invites.isEmpty()) {
            invitesByTarget.remove(targetId);
        }
    }

    private String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    public record PartySnapshot(
            UUID partyId,
            UUID leaderId,
            String leaderName,
            int size,
            int maxSize,
            List<String> memberNames
    ) {
    }
}
