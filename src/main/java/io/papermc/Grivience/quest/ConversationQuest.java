package io.papermc.Grivience.quest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ConversationQuest {
    private final String id;
    private String displayName;
    private String description;
    private String world;
    private String starterNpcId;
    private String targetNpcId;
    private final List<String> rewardCommands;
    private final List<String> prerequisites;
    private final List<QuestObjective> objectives;
    private boolean repeatable;
    private boolean enabled;

    public ConversationQuest(
            String id,
            String displayName,
            String description,
            String starterNpcId,
            String targetNpcId,
            List<String> rewardCommands,
            boolean repeatable,
            boolean enabled
    ) {
        this(id, displayName, description, "Hub", starterNpcId, targetNpcId, rewardCommands, List.of(), List.of(), repeatable, enabled);
    }

    public ConversationQuest(
            String id,
            String displayName,
            String description,
            String world,
            String starterNpcId,
            String targetNpcId,
            List<String> rewardCommands,
            List<String> prerequisites,
            List<QuestObjective> objectives,
            boolean repeatable,
            boolean enabled
    ) {
        this.id = normalizeId(id);
        this.displayName = sanitizeText(displayName, this.id);
        this.description = sanitizeText(description, "Talk to an NPC.");
        this.world = sanitizeText(world, "Hub");
        this.starterNpcId = normalizeNpcId(starterNpcId);
        this.targetNpcId = normalizeNpcId(targetNpcId);
        this.rewardCommands = new ArrayList<>();
        if (rewardCommands != null) {
            for (String rewardCommand : rewardCommands) {
                if (rewardCommand != null && !rewardCommand.isBlank()) {
                    this.rewardCommands.add(rewardCommand.trim());
                }
            }
        }
        this.prerequisites = new ArrayList<>();
        if (prerequisites != null) {
            for (String pre : prerequisites) {
                String normalized = normalizeId(pre);
                if (!normalized.isBlank()) {
                    this.prerequisites.add(normalized);
                }
            }
        }
        this.objectives = new ArrayList<>();
        if (objectives != null) {
            this.objectives.addAll(objectives);
        }
        this.repeatable = repeatable;
        this.enabled = enabled;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = sanitizeText(displayName, id);
    }

    public String description() {
        return description;
    }

    public void setDescription(String description) {
        this.description = sanitizeText(description, "Talk to an NPC.");
    }

    public String world() {
        return world;
    }

    public void setWorld(String world) {
        this.world = sanitizeText(world, "Hub");
    }

    public String starterNpcId() {
        return starterNpcId;
    }

    public void setStarterNpcId(String starterNpcId) {
        this.starterNpcId = normalizeNpcId(starterNpcId);
    }

    public String targetNpcId() {
        return targetNpcId;
    }

    public void setTargetNpcId(String targetNpcId) {
        this.targetNpcId = normalizeNpcId(targetNpcId);
    }

    public List<String> rewardCommands() {
        return rewardCommands;
    }

    public List<String> prerequisites() {
        return prerequisites;
    }

    public List<QuestObjective> objectives() {
        return objectives;
    }

    public boolean repeatable() {
        return repeatable;
    }

    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean hasStarterNpc() {
        return starterNpcId != null && !starterNpcId.isBlank();
    }

    public boolean hasTargetNpc() {
        return targetNpcId != null && !targetNpcId.isBlank();
    }

    public boolean matchesStarterNpc(String npcId) {
        if (!hasStarterNpc()) {
            return false;
        }
        return starterNpcId.equals(normalizeNpcId(npcId));
    }

    public boolean matchesTargetNpc(String npcId) {
        if (!hasTargetNpc()) {
            return false;
        }
        return targetNpcId.equals(normalizeNpcId(npcId));
    }

    public static String normalizeId(String id) {
        if (id == null || id.isBlank()) {
            return "";
        }
        return id.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    public static String normalizeNpcId(String npcId) {
        if (npcId == null || npcId.isBlank()) {
            return "";
        }
        return npcId.trim().toLowerCase(Locale.ROOT);
    }

    private static String sanitizeText(String text, String fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        return text.trim();
    }
}
