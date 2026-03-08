package io.papermc.Grivience.quest;

public final class QuestProgress {
    private boolean active;
    private int completions;

    public boolean active() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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

    public boolean hasCompletedAtLeastOnce() {
        return completions > 0;
    }
}
