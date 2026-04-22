package io.papermc.Grivience.mob;

import io.papermc.Grivience.gui.SkyblockGui;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MonsterGui implements Listener {
    private final CustomMonsterManager monsterManager;

    public MonsterGui(CustomMonsterManager monsterManager) {
        this.monsterManager = monsterManager;
    }

    public void open(Player player) {
        MonsterHolder holder = new MonsterHolder();
        int size = 54;
        Inventory inv = Bukkit.createInventory(holder, size, SkyblockGui.title("Custom Monsters"));
        holder.inventory = inv;

        ItemStack filler = SkyblockGui.filler(Material.BLACK_STAINED_GLASS_PANE);
        for (int i = 0; i < size; i++) {
            inv.setItem(i, filler.clone());
        }

        Map<String, CustomMonster> monsters = monsterManager.getMonsters();
        int slot = 10;
        for (CustomMonster monster : monsters.values()) {
            if (slot >= 44) break; // Simple pagination if needed, but 60 monsters is enough for 54 slot menu
            if (slot % 9 == 8) slot += 2;
            
            inv.setItem(slot++, createMonsterItem(monster));
        }

        inv.setItem(49, SkyblockGui.closeButton());

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    private ItemStack createMonsterItem(CustomMonster monster) {
        Material mat;
        try {
            mat = Material.valueOf(monster.getEntityType().name() + "_SPAWN_EGG");
        } catch (IllegalArgumentException e) {
            mat = Material.ZOMBIE_SPAWN_EGG;
        }

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "ID: " + ChatColor.DARK_GRAY + monster.getId());
        lore.add(ChatColor.GRAY + "Type: " + ChatColor.YELLOW + monster.getEntityType().name());
        lore.add(ChatColor.GRAY + "Level: " + ChatColor.YELLOW + monster.getLevel());
        lore.add("");
        lore.add(ChatColor.GRAY + "Health: " + ChatColor.RED + monster.getHealth());
        lore.add(ChatColor.GRAY + "Damage: " + ChatColor.RED + monster.getDamage());
        lore.add(ChatColor.GRAY + "Speed: " + ChatColor.RED + monster.getSpeed());
        lore.add("");
        lore.add(ChatColor.GRAY + "XP Reward: " + ChatColor.GREEN + monster.getExpReward());
        lore.add(ChatColor.GRAY + "Glowing: " + (monster.isGlowing() ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to spawn one at your feet!");

        return SkyblockGui.button(mat, ChatColor.RED + monster.getDisplayName(), lore);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MonsterHolder)) {
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR || clicked.getType() == Material.BLACK_STAINED_GLASS_PANE) {
            return;
        }

        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        // Extract ID from lore
        List<String> lore = clicked.getItemMeta().getLore();
        if (lore == null || lore.isEmpty()) return;

        String idLine = lore.get(0); // "§7ID: §8monster_id"
        String monsterId = ChatColor.stripColor(idLine).replace("ID: ", "").trim();

        if (monsterManager.getMonster(monsterId) != null) {
            CustomMonsterManager.SpawnBatchResult result = monsterManager.spawnMonstersSafely(monsterId, player.getLocation(), 1);
            if (result.spawned() > 0) {
                player.sendMessage(ChatColor.GREEN + "Spawned " + ChatColor.YELLOW + monsterId + ChatColor.GREEN + " nearby!");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            } else {
                player.sendMessage(ChatColor.RED + "No safe spawn location was available for " + ChatColor.YELLOW + monsterId + ChatColor.RED + ".");
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        }
    }

    static final class MonsterHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
