package io.papermc.Grivience.slayer;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SlayerManager {
    private final Map<UUID, SlayerQuest> activeQuests = new HashMap<>();

    public void startQuest(Player player, SlayerType type) {
        activeQuests.put(player.getUniqueId(), new SlayerQuest(type));
        player.sendMessage("§aStarted Slayer Quest: §e" + type.name() + "§a!");
    }

    public SlayerQuest getActiveQuest(Player player) {
        return activeQuests.get(player.getUniqueId());
    }

    public void clearQuest(Player player) {
        activeQuests.remove(player.getUniqueId());
    }

    public static class SlayerQuest {
        private final SlayerType type;
        private int currentXp;

        public SlayerQuest(SlayerType type) {
            this.type = type;
            this.currentXp = 0;
        }

        public SlayerType getType() {
            return type;
        }

        public int getCurrentXp() {
            return currentXp;
        }

        public void addXp(int amount) {
            this.currentXp += amount;
        }
    }
}