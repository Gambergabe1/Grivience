package io.papermc.Grivience.mayor;

import java.util.ArrayList;
import java.util.List;

public class Mayor {
    private final String name;
    private String buffDescription;
    private String skinName;
    private final List<String> actions;

    public Mayor(String name, String buffDescription, String skinName) {
        this.name = name;
        this.buffDescription = buffDescription;
        this.skinName = skinName;
        this.actions = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getBuffDescription() {
        return buffDescription;
    }

    public void setBuffDescription(String buffDescription) {
        this.buffDescription = buffDescription;
    }

    public String getSkinName() {
        return skinName;
    }

    public void setSkinName(String skinName) {
        this.skinName = skinName;
    }

    public List<String> getActions() {
        return new ArrayList<>(actions);
    }

    public void addAction(String action) {
        this.actions.add(action);
    }

    public void clearActions() {
        this.actions.clear();
    }
}
