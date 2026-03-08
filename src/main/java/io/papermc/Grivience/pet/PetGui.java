package io.papermc.Grivience.pet;

import io.papermc.Grivience.gui.SkyblockGui;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class PetGui implements Listener {
    private static final String TITLE = SkyblockGui.title("Pets");
    private static final int[] PET_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    private final PetManager petManager;

    public PetGui(PetManager petManager) {
        this.petManager = petManager;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new Holder(), 54, TITLE);
        SkyblockGui.fillAll(inv, SkyblockGui.filler(Material.BLACK_STAINED_GLASS_PANE));
        
        // Border
        ItemStack border = SkyblockGui.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(i + 45, border);
        }
        for (int i = 0; i < 54; i += 9) {
            inv.setItem(i, border);
            inv.setItem(i + 8, border);
        }

        inv.setItem(4, SkyblockGui.button(
                Material.BONE,
                ChatColor.GREEN + "Pets",
                java.util.List.of(
                        ChatColor.GRAY + "View and manage your pets.",
                        "",
                        ChatColor.GRAY + "Your pets gain Experience when you",
                        ChatColor.GRAY + "gain Skill XP in any Skill.",
                        "",
                        ChatColor.YELLOW + "Click a pet to equip it!",
                        ChatColor.YELLOW + "Click an equipped pet to de-equip it!"
                )
        ));

        int slotIndex = 0;
        Set<String> owned = petManager.ownedPets(player);
        List<PetDefinition> toShow = new ArrayList<>();
        for (String id : owned) {
            PetDefinition def = petManager.allPets().stream().filter(p -> p.id().equalsIgnoreCase(id)).findFirst().orElse(null);
            if (def != null) toShow.add(def);
        }
        
        // Sort: Equipped first, then by rarity, then by level
        String equipped = petManager.equippedPet(player);
        toShow.sort((a, b) -> {
            if (a.id().equalsIgnoreCase(equipped)) return -1;
            if (b.id().equalsIgnoreCase(equipped)) return 1;
            int rarityComp = b.rarity().ordinal() - a.rarity().ordinal();
            if (rarityComp != 0) return rarityComp;
            return b.id().compareTo(a.id());
        });

        for (PetDefinition def : toShow) {
            if (slotIndex >= PET_SLOTS.length) break;
            inv.setItem(PET_SLOTS[slotIndex], petManager.createPetItem(def.id(), player, true));
            slotIndex++;
        }

        inv.setItem(48, SkyblockGui.backButton("Skyblock Menu"));
        inv.setItem(49, SkyblockGui.closeButton());
        inv.setItem(50, scoreItem(player));
        
        player.openInventory(inv);
    }

    private ItemStack scoreItem(Player player) {
        ItemStack item = new ItemStack(Material.PAINTING);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Pet Score");
        
        int totalScore = 0;
        Set<String> owned = petManager.ownedPets(player);
        for (String id : owned) {
            PetDefinition def = petManager.allPets().stream().filter(p -> p.id().equalsIgnoreCase(id)).findFirst().orElse(null);
            if (def == null) continue;
            totalScore += switch (def.rarity()) {
                case COMMON -> 1;
                case UNCOMMON -> 2;
                case RARE -> 3;
                case EPIC -> 4;
                case LEGENDARY -> 5;
                case MYTHIC -> 6;
                case DIVINE -> 10;
                default -> 1;
            };
        }

        meta.setLore(java.util.List.of(
                ChatColor.GRAY + "Pet Score: " + ChatColor.AQUA + totalScore,
                "",
                ChatColor.GRAY + "Gain Pet Score by owning unique pets",
                ChatColor.GRAY + "of different rarities.",
                "",
                ChatColor.AQUA + "Provides +1% Pet Luck per 10 Score."
        ));
        item.setItemMeta(meta);
        return item;
    }


    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        // Back / Close
        if (event.getSlot() == 48 && clicked.getType() == Material.ARROW) {
            player.closeInventory();
            player.performCommand("skyblock menu");
            return;
        }
        if (event.getSlot() == 49 && clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }
        
        String petId = petManager.petId(clicked);
        if (petId == null) {
            return;
        }

        String currentlyEquipped = petManager.equippedPet(player);
        if (petId.equalsIgnoreCase(currentlyEquipped)) {
            petManager.equip(player, null);
            player.sendMessage(ChatColor.GREEN + "De-equipped pet.");
        } else {
            petManager.equip(player, petId);
            player.sendMessage(ChatColor.GREEN + "Equipped pet: " + ChatColor.AQUA + petId);
        }
        open(player);
    }

    private static final class Holder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}

