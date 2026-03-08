package io.papermc.Grivience.nick;

import net.wesjd.anvilgui.AnvilGUI;
import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.bazaar.BazaarKeys;
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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hypixel-accurate Nickname GUI System.
 */
public final class NickGuiManager implements Listener {
    private final GriviencePlugin plugin;
    private final NickManager nickManager;

    public NickGuiManager(GriviencePlugin plugin, NickManager nickManager) {
        this.plugin = plugin;
        this.nickManager = nickManager;
    }

    public void openMain(Player player) {
        Inventory inventory = Bukkit.createInventory(new NickMainHolder(), 54, "Nickname Settings");
        fillBorder(inventory);

        boolean isNicked = nickManager.isNicked(player);
        NickManager.NickData data = nickManager.getNickData(player);

        // Status Item (Slot 13)
        setItem(inventory, 13, createStatusItem(player, isNicked, data));

        // Set Nickname (Slot 29)
        setItem(inventory, 29, createActionItem(Material.NAME_TAG, "&aSet Nickname", 
            new String[]{"&7Choose a name to hide", "&7your real identity."}, "set_name"));

        // Set Rank (Slot 31)
        setItem(inventory, 31, createActionItem(Material.GOLD_INGOT, "&aSet Rank", 
            new String[]{"&7Choose a rank to display", "&7next to your name."}, "set_rank"));

        // Reset Nickname (Slot 33)
        if (isNicked) {
            setItem(inventory, 33, createActionItem(Material.BARRIER, "&cReset Nickname", 
                new String[]{"&7Remove your current", "&7nickname and rank."}, "reset"));
        } else {
            setItem(inventory, 33, createActionItem(Material.GRAY_DYE, "&7Reset Nickname", 
                new String[]{"&8You aren't nicked!"}, "noop"));
        }

        // Close (Slot 49)
        setItem(inventory, 49, createActionItem(Material.BARRIER, "&cClose", new String[]{}, "close"));

        player.openInventory(inventory);
    }

    public void openRankMenu(Player player) {
        Inventory inventory = Bukkit.createInventory(new NickRankHolder(), 36, "Select a Rank");
        fillBorder(inventory);

        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        NickManager.NickRank[] ranks = NickManager.NickRank.values();

        for (int i = 0; i < Math.min(ranks.length, slots.length); i++) {
            NickManager.NickRank rank = ranks[i];
            setItem(inventory, slots[i], createRankItem(rank));
        }

        setItem(inventory, 31, createActionItem(Material.ARROW, "&aBack", new String[]{}, "back"));

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        if (event.getInventory().getHolder() instanceof NickHolder) {
            event.setCancelled(true);

            // Only process actions for clicks in the top inventory
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getInventory())) {
                ItemStack clicked = event.getCurrentItem();
                if (clicked == null || !clicked.hasItemMeta()) return;

                String action = clicked.getItemMeta().getPersistentDataContainer().get(BazaarKeys.ACTION_KEY, PersistentDataType.STRING);
                if (action != null) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                    handleAction(player, event.getInventory().getHolder(), clicked, action);
                }
            }
        }
    }

    private void handleAction(Player player, InventoryHolder holder, ItemStack clicked, String action) {
        switch (action) {
            case "close" -> player.closeInventory();
            case "back" -> openMain(player);
            case "set_name" -> openNameInput(player);
            case "set_rank" -> openRankMenu(player);
            case "reset" -> {
                nickManager.removeNick(player);
                openMain(player);
            }
            case "select_rank" -> {
                String rankName = clicked.getItemMeta().getPersistentDataContainer().get(BazaarKeys.VALUE_KEY, PersistentDataType.STRING);
                if (rankName != null) {
                    NickManager.NickRank rank = NickManager.NickRank.valueOf(rankName);
                    String currentNick = nickManager.isNicked(player) ? nickManager.getNickData(player).nickname() : player.getName();
                    nickManager.applyNick(player, currentNick, rank);
                    openMain(player);
                }
            }
        }
    }

    private void openNameInput(Player player) {
        new AnvilGUI.Builder()
                .plugin(plugin)
                .title("Enter Nickname")
                .itemLeft(new ItemStack(Material.NAME_TAG))
                .text("Name...")
                .onClick((slot, stateSnapshot) -> {
                    if (slot != AnvilGUI.Slot.OUTPUT) return Collections.emptyList();
                    
                    String name = stateSnapshot.getText().trim();
                    if (name.isEmpty() || name.equals("Name...") || name.length() > 16) {
                        player.sendMessage(ChatColor.RED + "Invalid name length (1-16 chars).");
                        return List.of(AnvilGUI.ResponseAction.close());
                    }
                    
                    return List.of(AnvilGUI.ResponseAction.run(() -> {
                        NickManager.NickRank rank = nickManager.isNicked(player) ? nickManager.getNickData(player).rank() : NickManager.NickRank.DEFAULT;
                        nickManager.applyNick(player, name, rank);
                        openMain(player);
                    }));
                })
                .open(player);
    }

    private ItemStack createStatusItem(Player player, boolean isNicked, NickManager.NickData data) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + "Your Nickname: " + (isNicked ? data.rank().getPrefix() + data.nickname() : ChatColor.GRAY + "None"));
        
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Real Name: " + ChatColor.WHITE + player.getName());
        lore.add(ChatColor.GRAY + "Nicked: " + (isNicked ? ChatColor.GREEN + "YES" : ChatColor.RED + "NO"));
        if (isNicked) {
            lore.add(ChatColor.GRAY + "Rank: " + data.rank().getName());
        }
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRankItem(NickManager.NickRank rank) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(rank.getColor() + rank.getName());
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Prefix: " + rank.getPrefix() + "Name");
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to select!");
        
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(BazaarKeys.ACTION_KEY, PersistentDataType.STRING, "select_rank");
        meta.getPersistentDataContainer().set(BazaarKeys.VALUE_KEY, PersistentDataType.STRING, rank.name());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createActionItem(Material material, String name, String[] lore, String action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        List<String> l = new ArrayList<>();
        for (String s : lore) l.add(ChatColor.translateAlternateColorCodes('&', s));
        if (!l.isEmpty()) {
            l.add("");
            l.add(ChatColor.YELLOW + "Click to select!");
        }
        meta.setLore(l);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(BazaarKeys.ACTION_KEY, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private void fillBorder(Inventory inv) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);
        for (int i = 0; i < inv.getSize(); i++) {
            if (i < 9 || i >= inv.getSize() - 9 || i % 9 == 0 || (i + 1) % 9 == 0) {
                inv.setItem(i, pane);
            }
        }
    }

    private void setItem(Inventory inv, int slot, ItemStack item) {
        inv.setItem(slot, item);
    }

    private interface NickHolder extends InventoryHolder {}
    private static class NickMainHolder implements NickHolder { @Override public Inventory getInventory() { return null; } }
    private static class NickRankHolder implements NickHolder { @Override public Inventory getInventory() { return null; } }
}
