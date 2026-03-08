package io.papermc.Grivience.bank;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BankListener implements Listener {
    private static final long INPUT_TIMEOUT_MS = 30_000L;

    private final GriviencePlugin plugin;
    private final BankManager bankManager;
    private final Map<UUID, BankInput> inputByPlayer = new ConcurrentHashMap<>();

    public BankListener(GriviencePlugin plugin, BankManager bankManager) {
        this.plugin = plugin;
        this.bankManager = bankManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top != null ? top.getHolder() : null;
        if (!(holder instanceof BankGui.BankHolder bankHolder)) {
            return;
        }

        event.setCancelled(true);

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= top.getSize()) {
            return;
        }

        BankGui.ViewType viewType = bankHolder.getViewType();

        if (rawSlot == BankGui.CLOSE_SLOT) {
            player.closeInventory();
            return;
        }

        if (viewType == BankGui.ViewType.MAIN) {
            if (rawSlot == BankGui.DEPOSIT_MENU_SLOT) {
                bankManager.openDeposit(player);
                return;
            }
            if (rawSlot == BankGui.WITHDRAW_MENU_SLOT) {
                bankManager.openWithdraw(player);
                return;
            }
            if (rawSlot == BankGui.BACK_SLOT) {
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> player.performCommand("skyblock menu"));
            }
            return;
        }

        // Deposit/Withdraw views
        boolean deposit = viewType == BankGui.ViewType.DEPOSIT;

        if (rawSlot == BankGui.BACK_SLOT) {
            bankManager.openMain(player);
            return;
        }

        if (rawSlot == BankGui.AMOUNT_CUSTOM_SLOT) {
            beginInput(player, viewType);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.2F);
            return;
        }

        boolean changed = false;
        if (rawSlot == BankGui.AMOUNT_ALL_SLOT) {
            changed = deposit ? bankManager.depositAll(player) : bankManager.withdrawAll(player);
        } else if (rawSlot == BankGui.AMOUNT_100_SLOT) {
            changed = deposit ? bankManager.deposit(player, 100) : bankManager.withdraw(player, 100);
        } else if (rawSlot == BankGui.AMOUNT_1K_SLOT) {
            changed = deposit ? bankManager.deposit(player, 1_000) : bankManager.withdraw(player, 1_000);
        } else if (rawSlot == BankGui.AMOUNT_10K_SLOT) {
            changed = deposit ? bankManager.deposit(player, 10_000) : bankManager.withdraw(player, 10_000);
        } else if (rawSlot == BankGui.AMOUNT_100K_SLOT) {
            changed = deposit ? bankManager.deposit(player, 100_000) : bankManager.withdraw(player, 100_000);
        } else if (rawSlot == BankGui.AMOUNT_1M_SLOT) {
            changed = deposit ? bankManager.deposit(player, 1_000_000) : bankManager.withdraw(player, 1_000_000);
        }

        if (changed) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.2F);
            bankManager.refreshOpenInventory(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (top != null && top.getHolder() instanceof BankGui.BankHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        inputByPlayer.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        Inventory inv = event.getInventory();
        if (inv != null && inv.getHolder() instanceof BankGui.BankHolder) {
            inputByPlayer.remove(player.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        BankInput input = inputByPlayer.get(playerId);
        if (input == null) {
            return;
        }

        event.setCancelled(true);
        String message = event.getMessage();

        Bukkit.getScheduler().runTask(plugin, () -> {
            BankInput latest = inputByPlayer.get(playerId);
            if (latest == null) {
                return;
            }

            if (System.currentTimeMillis() > latest.expiresAtMs) {
                inputByPlayer.remove(playerId);
                player.sendMessage(ChatColor.RED + "Bank entry timed out.");
                return;
            }

            String msg = message == null ? "" : message.trim();
            if (msg.equalsIgnoreCase("cancel")) {
                inputByPlayer.remove(playerId);
                player.sendMessage(ChatColor.YELLOW + "Bank entry cancelled.");
                return;
            }

            String normalized = msg.replace(",", "").replace("_", "");
            long coins;
            try {
                coins = Long.parseLong(normalized);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid number. Type a whole number (e.g. 25000), or 'cancel'.");
                return;
            }

            if (coins < 0L) {
                player.sendMessage(ChatColor.RED + "Amount must be 0 or more.");
                return;
            }

            if (coins == 0L) {
                inputByPlayer.remove(playerId);
                player.sendMessage(ChatColor.YELLOW + "No coins moved.");
                return;
            }

            boolean changed;
            if (latest.viewType == BankGui.ViewType.DEPOSIT) {
                changed = bankManager.deposit(player, coins);
            } else if (latest.viewType == BankGui.ViewType.WITHDRAW) {
                changed = bankManager.withdraw(player, coins);
            } else {
                changed = false;
            }

            inputByPlayer.remove(playerId);
            if (changed) {
                bankManager.refreshOpenInventory(player);
            }
        });
    }

    private void beginInput(Player player, BankGui.ViewType viewType) {
        if (player == null) {
            return;
        }

        BankGui.ViewType type = viewType == null ? BankGui.ViewType.DEPOSIT : viewType;
        inputByPlayer.put(player.getUniqueId(), new BankInput(type, System.currentTimeMillis() + INPUT_TIMEOUT_MS));

        player.sendMessage(ChatColor.GOLD + "Bank " + (type == BankGui.ViewType.DEPOSIT ? "Deposit" : "Withdrawal"));
        player.sendMessage(ChatColor.GRAY + "Type the number of coins in chat.");
        player.sendMessage(ChatColor.DARK_GRAY + "Type 'cancel' to stop.");
    }

    private static final class BankInput {
        final BankGui.ViewType viewType;
        final long expiresAtMs;

        private BankInput(BankGui.ViewType viewType, long expiresAtMs) {
            this.viewType = viewType;
            this.expiresAtMs = expiresAtMs;
        }
    }
}
