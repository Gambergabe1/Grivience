package io.papermc.Grivience.quest;

import java.util.HashMap;
import java.util.Map;

public final class QuestProgress {
    private boolean active;
    private int completions;
    private final Map<Integer, Integer> objectiveProgress = new HashMap<>();

    public boolean active() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        if (!active) {
            objectiveProgress.clear();
        }
    }

    public int completions() {
        return completions;
    }

    public void setCompletions(int completions) {
        this.completions = Math.max(0, completions);
    }

    public void incrementCompletions() {
        completions++;
    }

    public int getObjectiveProgress(int index) {
        return objectiveProgress.getOrDefault(index, 0);
    }

    public void setObjectiveProgress(int index, int value) {
        objectiveProgress.put(index, Math.max(0, value));
    }

    public void incrementObjectiveProgress(int index, int delta) {
        int current = getObjectiveProgress(index);
        setObjectiveProgress(index, current + delta);
    }

    public Map<Integer, Integer> getAllObjectiveProgress() {
        return new HashMap<>(objectiveProgress);
    }

    public void clearObjectiveProgress() {
        objectiveProgress.clear();
    }

    public boolean hasCompletedAtLeastOnce() {
        return completions > 0;
    }
}
