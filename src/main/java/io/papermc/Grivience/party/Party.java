package io.papermc.Grivience.party;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class Party {
    private final UUID id;
    private UUID leader;
    private final LinkedHashSet<UUID> members;

    public Party(UUID leader) {
        this.id = UUID.randomUUID();
        this.leader = leader;
        this.members = new LinkedHashSet<>();
        this.members.add(leader);
    }

    public UUID id() {
        return id;
    }

    public UUID leader() {
        return leader;
    }

    public Set<UUID> members() {
        return Set.copyOf(members);
    }

    public int size() {
        return members.size();
    }

    public boolean isLeader(UUID playerId) {
        return leader.equals(playerId);
    }

    public boolean hasMember(UUID playerId) {
        return members.contains(playerId);
    }

    public void addMember(UUID playerId) {
        members.add(playerId);
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
        if (leader.equals(playerId) && !members.isEmpty()) {
            leader = members.iterator().next();
        }
    }

    public void setLeader(UUID newLeader) {
        if (members.contains(newLeader)) {
            this.leader = newLeader;
        }
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    public Collection<UUID> mutableMembers() {
        return members;
    }
}
