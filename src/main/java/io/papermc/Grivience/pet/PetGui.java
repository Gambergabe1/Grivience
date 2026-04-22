package io.papermc.Grivience.pet;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.gui.SkyblockGui;
import io.papermc.Grivience.skyblock.economy.ProfileEconomyService;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public final class PetGui implements Listener {
    private static final String TITLE = SkyblockGui.title("Pets");
    private static final String SELL_TITLE = SkyblockGui.title("Sell Pet");
    private static final String TRADE_TITLE = SkyblockGui.title("Pet Trading");
    private static final String WITHDRAW_TITLE = SkyblockGui.title("Withdraw Pet");
    private static final int[] PET_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final PetManager petManager;
    private final GriviencePlugin plugin;
    private final ProfileEconomyService economyService;

    // Trading state
    private final Map<UUID, PetTradeSession> pendingTrades = new HashMap<>();

    public PetGui(PetManager petManager, GriviencePlugin plugin) {
        this.petManager = petManager;
        this.plugin = plugin;
        this.economyService = new ProfileEconomyService(plugin);
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

        // Action buttons
        inv.setItem(51, sellMenuButton());
        inv.setItem(52, tradeMenuButton());

        player.openInventory(inv);
    }

    private ItemStack sellMenuButton() {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Sell Pets");
        meta.setLore(java.util.List.of(
                ChatColor.GRAY + "Click to open the pet sell menu.",
                "",
                ChatColor.YELLOW + "Sell pets for coins!"
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack tradeMenuButton() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Trade Pets");
        meta.setLore(java.util.List.of(
                ChatColor.GRAY + "Click to start trading pets.",
                "",
                ChatColor.YELLOW + "Trade pets with other players!"
        ));
        item.setItemMeta(meta);
        return item;
    }

    public void openSellMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new SellHolder(), 54, SELL_TITLE);
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
                Material.GOLD_INGOT,
                ChatColor.GOLD + "Sell Pets",
                java.util.List.of(
                        ChatColor.GRAY + "Click a pet to sell it.",
                        "",
                        ChatColor.YELLOW + "Warning: Selling is permanent!"
                )
        ));

        int slotIndex = 0;
        Set<String> owned = petManager.ownedPets(player);
        String equipped = petManager.equippedPet(player);

        for (String id : owned) {
            if (slotIndex >= PET_SLOTS.length) break;
            // Don't show equipped pets for selling
            if (id.equalsIgnoreCase(equipped)) continue;

            PetDefinition def = petManager.allPets().stream()
                    .filter(p -> p.id().equalsIgnoreCase(id))
                    .findFirst().orElse(null);
            if (def != null) {
                ItemStack petItem = petManager.createPetItem(def.id(), player, true);
                if (petItem != null) {
                    // Add sell value to lore
                    ItemMeta meta = petItem.getItemMeta();
                    List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    int level = petManager.getLevel(player, id);
                    long sellValue = calculateSellValue(def, level);
                    lore.add("");
                    lore.add(ChatColor.GOLD + "Sell Value: " + ChatColor.YELLOW + formatCoins(sellValue));
                    lore.add("");
                    lore.add(ChatColor.RED + "Click to sell!");
                    meta.setLore(lore);
                    petItem.setItemMeta(meta);
                    inv.setItem(PET_SLOTS[slotIndex], petItem);
                    slotIndex++;
                }
            }
        }

        inv.setItem(48, SkyblockGui.button(
                Material.ARROW,
                ChatColor.YELLOW + "Back to Pets",
                java.util.List.of(ChatColor.GRAY + "Return to your pet menu.")
        ));
        inv.setItem(49, SkyblockGui.closeButton());

        // Info button showing coin balance
        double balance = economyService.purse(player);
        inv.setItem(50, SkyblockGui.button(
                Material.NETHER_STAR,
                ChatColor.AQUA + "Your Balance",
                java.util.List.of(
                        ChatColor.GRAY + "Current Coins: " + ChatColor.GOLD + formatCoins((long) balance)
                )
        ));

        player.openInventory(inv);
    }

    public void openTradeMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new TradeHolder(), 54, TRADE_TITLE);
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
                Material.EMERALD,
                ChatColor.GREEN + "Pet Trading",
                java.util.List.of(
                        ChatColor.GRAY + "Select a pet to trade.",
                        "",
                        ChatColor.YELLOW + "Then share the trade code with",
                        ChatColor.YELLOW + "another player!"
                )
        ));

        int slotIndex = 0;
        Set<String> owned = petManager.ownedPets(player);
        String equipped = petManager.equippedPet(player);

        for (String id : owned) {
            if (slotIndex >= PET_SLOTS.length) break;
            // Don't show equipped pets for trading
            if (id.equalsIgnoreCase(equipped)) continue;

            PetDefinition def = petManager.allPets().stream()
                    .filter(p -> p.id().equalsIgnoreCase(id))
                    .findFirst().orElse(null);
            if (def != null) {
                ItemStack petItem = petManager.createPetItem(def.id(), player, true);
                if (petItem != null) {
                    // Add trade info to lore
                    ItemMeta meta = petItem.getItemMeta();
                    List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    lore.add("");
                    lore.add(ChatColor.GREEN + "Click to select for trade!");
                    meta.setLore(lore);
                    petItem.setItemMeta(meta);
                    inv.setItem(PET_SLOTS[slotIndex], petItem);
                    slotIndex++;
                }
            }
        }

        inv.setItem(48, SkyblockGui.button(
                Material.ARROW,
                ChatColor.YELLOW + "Back to Pets",
                java.util.List.of(ChatColor.GRAY + "Return to your pet menu.")
        ));
        inv.setItem(49, SkyblockGui.closeButton());

        // Trade info
        PetTradeSession session = pendingTrades.get(player.getUniqueId());
        if (session != null && session.selectedPetId != null) {
            inv.setItem(50, SkyblockGui.button(
                    Material.BOOK,
                    ChatColor.AQUA + "Trade Code",
                    java.util.List.of(
                            ChatColor.GRAY + "Your code: " + ChatColor.YELLOW + session.tradeCode,
                            "",
                            ChatColor.GRAY + "Share this code with the",
                            ChatColor.GRAY + "player you want to trade with.",
                            "",
                            ChatColor.GRAY + "Selected: " + ChatColor.AQUA + session.selectedPetId
                    )
            ));
        } else {
            inv.setItem(50, SkyblockGui.button(
                    Material.BOOK,
                    ChatColor.GRAY + "No Pet Selected",
                    java.util.List.of(
                            ChatColor.GRAY + "Click a pet to select it",
                            ChatColor.GRAY + "for trading."
                    )
            ));
        }

        // How to redeem
        inv.setItem(52, SkyblockGui.button(
                Material.COMPASS,
                ChatColor.YELLOW + "How to Trade",
                java.util.List.of(
                        ChatColor.GRAY + "1. Select a pet above",
                        ChatColor.GRAY + "2. Share your trade code",
                        ChatColor.GRAY + "3. Other player enters code",
                        ChatColor.GRAY + "   with /pet trade <code>",
                        ChatColor.GRAY + "4. Both confirm to complete!"
                )
        ));

        player.openInventory(inv);
    }

    private long calculateSellValue(PetDefinition def, int level) {
        // Base value by rarity
        long baseValue = switch (def.rarity()) {
            case COMMON -> 100L;
            case UNCOMMON -> 250L;
            case RARE -> 1000L;
            case EPIC -> 5000L;
            case LEGENDARY -> 25000L;
            case MYTHIC -> 100000L;
            case DIVINE -> 500000L;
            default -> 50L;
        };

        // Level multiplier: each level adds 1% more value
        double levelMultiplier = 1.0 + (level - 1) * 0.01;
        return Math.round(baseValue * levelMultiplier);
    }

    private String formatCoins(long amount) {
        if (amount < 1000) return String.valueOf(amount);
        if (amount < 1000000) return String.format("%.1fk", amount / 1000.0);
        if (amount < 1000000000) return String.format("%.1fM", amount / 1000000.0);
        return String.format("%.1fB", amount / 1000000000.0);
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
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Handle Sell Menu
        if (event.getInventory().getHolder() instanceof SellHolder) {
            event.setCancelled(true);
            handleSellClick(player, event);
            return;
        }

        // Handle Trade Menu
        if (event.getInventory().getHolder() instanceof TradeHolder) {
            event.setCancelled(true);
            handleTradeClick(player, event);
            return;
        }

        // Handle Main Pet Menu
        if (event.getInventory().getHolder() instanceof WithdrawHolder) {
            event.setCancelled(true);
            handleWithdrawClick(player, event);
            return;
        }

        if (!(event.getInventory().getHolder() instanceof Holder)) return;
        event.setCancelled(true);

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

        // Sell menu button
        if (event.getSlot() == 51 && clicked.getType() == Material.GOLD_INGOT) {
            openSellMenu(player);
            return;
        }

        // Trade menu button
        if (event.getSlot() == 52 && clicked.getType() == Material.EMERALD) {
            openTradeMenu(player);
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

    private void handleSellClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Back button
        if (event.getSlot() == 48 && clicked.getType() == Material.ARROW) {
            open(player);
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

        // Can't sell equipped pets
        String equipped = petManager.equippedPet(player);
        if (petId.equalsIgnoreCase(equipped)) {
            player.sendMessage(ChatColor.RED + "You cannot sell an equipped pet!");
            return;
        }

        PetDefinition def = petManager.allPets().stream().filter(p -> p.id().equalsIgnoreCase(petId)).findFirst().orElse(null);
        if (def == null) return;

        int level = petManager.getLevel(player, petId);
        long sellValue = calculateSellValue(def, level);

        // Confirm sell
        player.closeInventory();
        player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------------------");
        player.sendMessage(ChatColor.GOLD + "Sell Confirmation");
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "Pet: " + def.rarity().color() + def.displayName());
        player.sendMessage(ChatColor.GRAY + "Level: " + ChatColor.AQUA + level);
        player.sendMessage(ChatColor.GOLD + "Sell Value: " + ChatColor.YELLOW + formatCoins(sellValue));
        player.sendMessage("");
        player.sendMessage(Component.text(ChatColor.GREEN + "[CONFIRM SELL]")
                .clickEvent(ClickEvent.runCommand("/pet sellconfirm " + petId))
                .hoverEvent(HoverEvent.showText(Component.text("Click to sell"))));
        player.sendMessage(Component.text(ChatColor.RED + "[CANCEL]"));
        player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------------------");
    }

    private void handleTradeClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Back button
        if (event.getSlot() == 48 && clicked.getType() == Material.ARROW) {
            open(player);
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

        // Can't trade equipped pets
        String equipped = petManager.equippedPet(player);
        if (petId.equalsIgnoreCase(equipped)) {
            player.sendMessage(ChatColor.RED + "You cannot trade an equipped pet!");
            return;
        }

        // Create or update trade session
        PetTradeSession session = pendingTrades.computeIfAbsent(player.getUniqueId(),
            k -> new PetTradeSession(player.getUniqueId(), generateTradeCode()));
        session.selectedPetId = petId;

        PetDefinition def = petManager.allPets().stream().filter(p -> p.id().equalsIgnoreCase(petId)).findFirst().orElse(null);
        if (def != null) {
            player.sendMessage(ChatColor.GREEN + "Selected pet for trade: " + def.rarity().color() + def.displayName());
            player.sendMessage(ChatColor.YELLOW + "Your trade code: " + ChatColor.GOLD + session.tradeCode);
            player.sendMessage(ChatColor.GRAY + "Share this code with the other player!");
        }
        openTradeMenu(player);
    }

    private void handleWithdrawClick(Player player, InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof WithdrawHolder holder)) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (event.getSlot() == 11) {
            open(player);
            return;
        }

        if (event.getSlot() == 15) {
            String petId = holder.petId;
            String equipped = petManager.equippedPet(player);
            if (petId.equalsIgnoreCase(equipped)) {
                player.sendMessage(ChatColor.RED + "You cannot withdraw an equipped pet!");
                player.closeInventory();
                return;
            }

            io.papermc.Grivience.skyblock.profile.SkyBlockProfile profile = petManager.requireProfile(player);
            if (profile == null) return;

            if (player.getInventory().firstEmpty() == -1) {
                player.sendMessage(ChatColor.RED + "Your inventory is full!");
                player.closeInventory();
                return;
            }

            // Generate item WITH XP
            ItemStack petItem = petManager.createPetItem(petId, player, false);
            if (petItem != null) {
                profile.getPetData().remove(petId.toLowerCase(Locale.ROOT));
                petManager.saveProfile(profile);
                petManager.updateTotalPetScore(player);
                
                player.getInventory().addItem(petItem);
                player.sendMessage(ChatColor.GREEN + "Withdrew " + ChatColor.AQUA + petId + ChatColor.GREEN + " to your inventory!");
                player.closeInventory();
            }
        }
    }

    public boolean processTrade(Player player, String tradeCode) {
        // Find the trade session with this code
        for (Map.Entry<UUID, PetTradeSession> entry : pendingTrades.entrySet()) {
            if (entry.getValue().tradeCode.equalsIgnoreCase(tradeCode)) {
                UUID initiatorId = entry.getKey();
                Player initiator = Bukkit.getPlayer(initiatorId);

                if (initiator == null || !initiator.isOnline()) {
                    player.sendMessage(ChatColor.RED + "The trade initiator is no longer online!");
                    return false;
                }

                if (initiator.equals(player)) {
                    player.sendMessage(ChatColor.RED + "You cannot trade with yourself!");
                    return false;
                }

                // Verify both players have profiles
                io.papermc.Grivience.skyblock.profile.SkyBlockProfile initiatorProfile =
                    economyService.requireSelectedProfile(initiator);
                io.papermc.Grivience.skyblock.profile.SkyBlockProfile playerProfile =
                    economyService.requireSelectedProfile(player);

                if (initiatorProfile == null || playerProfile == null) {
                    player.sendMessage(ChatColor.RED + "Both players need Skyblock profiles!");
                    return false;
                }

                // Verify initiator still has the pet
                PetTradeSession session = entry.getValue();
                if (!initiatorProfile.getPetData().containsKey(session.selectedPetId)) {
                    player.sendMessage(ChatColor.RED + "The initiator no longer has that pet!");
                    pendingTrades.remove(initiatorId);
                    return false;
                }

                // Start the trade
                session.receiverId = player.getUniqueId();
                session.status = TradeStatus.AWAITING_CONFIRMATION;

                player.sendMessage(ChatColor.GREEN + "Trade found!");
                player.sendMessage(ChatColor.GRAY + "From: " + ChatColor.AQUA + initiator.getName());
                PetDefinition def = petManager.allPets().stream()
                        .filter(p -> p.id().equalsIgnoreCase(session.selectedPetId))
                        .findFirst().orElse(null);
                if (def != null) {
                    player.sendMessage(ChatColor.GRAY + "Pet: " + def.rarity().color() + def.displayName());
                }
                player.sendMessage("");
                player.sendMessage(Component.text(ChatColor.GREEN + "[ACCEPT TRADE]")
                    .clickEvent(ClickEvent.runCommand("/pet tradeaccept " + tradeCode))
                    .hoverEvent(HoverEvent.showText(Component.text("Accept the trade"))));
                player.sendMessage(Component.text(ChatColor.RED + "[DECLINE]"));

                initiator.sendMessage(ChatColor.GREEN + player.getName() + " wants to trade!");
                initiator.sendMessage(ChatColor.GRAY + "Waiting for them to accept...");

                return true;
            }
        }

        player.sendMessage(ChatColor.RED + "Invalid or expired trade code!");
        return false;
    }

    public boolean acceptTrade(Player player, String tradeCode) {
        for (Map.Entry<UUID, PetTradeSession> entry : pendingTrades.entrySet()) {
            if (entry.getValue().tradeCode.equalsIgnoreCase(tradeCode)) {
                PetTradeSession session = entry.getValue();

                if (!session.receiverId.equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "This trade is not for you!");
                    return false;
                }

                Player initiator = Bukkit.getPlayer(entry.getKey());
                if (initiator == null || !initiator.isOnline()) {
                    player.sendMessage(ChatColor.RED + "The trade initiator went offline!");
                    pendingTrades.remove(entry.getKey());
                    return false;
                }

                // Verify both players still have profiles
                io.papermc.Grivience.skyblock.profile.SkyBlockProfile initiatorProfile =
                    economyService.requireSelectedProfile(initiator);
                io.papermc.Grivience.skyblock.profile.SkyBlockProfile playerProfile =
                    economyService.requireSelectedProfile(player);

                if (initiatorProfile == null || playerProfile == null) {
                    player.sendMessage(ChatColor.RED + "Both players need Skyblock profiles!");
                    return false;
                }

                // Verify initiator still has the pet
                if (!initiatorProfile.getPetData().containsKey(session.selectedPetId)) {
                    player.sendMessage(ChatColor.RED + "The initiator no longer has that pet!");
                    pendingTrades.remove(entry.getKey());
                    return false;
                }

                // Execute the trade
                String petId = session.selectedPetId;

                // Transfer pet from initiator to receiver
                io.papermc.Grivience.skyblock.profile.SkyBlockProfile.PetData petData =
                    initiatorProfile.getPetData().remove(petId);
                if (petData != null) {
                    playerProfile.getPetData().put(petId, petData);
                    petData.setActive(false); // Reset active state
                }

                // Clear equipped if it was equipped
                if (petId.equalsIgnoreCase(initiatorProfile.getEquippedPet())) {
                    initiatorProfile.setEquippedPet(null);
                }

                // Save profiles
                plugin.getProfileManager().saveProfile(initiatorProfile);
                plugin.getProfileManager().saveProfile(playerProfile);

                // Notify both players
                PetDefinition def = petManager.allPets().stream()
                        .filter(p -> p.id().equalsIgnoreCase(petId))
                        .findFirst().orElse(null);
                String petName = def != null ? def.displayName() : petId;

                player.sendMessage(ChatColor.GREEN + "Trade completed!");
                player.sendMessage(ChatColor.GRAY + "Received: " + def.rarity().color() + petName);
                initiator.sendMessage(ChatColor.GREEN + "Trade completed!");
                initiator.sendMessage(ChatColor.RED + "Traded away: " + (def != null ? def.rarity().color() : "") + petName);

                // Clean up
                pendingTrades.remove(entry.getKey());

                return true;
            }
        }

        player.sendMessage(ChatColor.RED + "Trade not found or expired!");
        return false;
    }

    private String generateTradeCode() {
        return String.format("%06d", new Random().nextInt(1000000));
    }

    // Trade session class (not a record due to mutable state)
    private static class PetTradeSession {
        final UUID initiatorId;
        final String tradeCode;
        String selectedPetId;
        UUID receiverId;
        TradeStatus status;

        PetTradeSession(UUID initiatorId, String tradeCode) {
            this.initiatorId = initiatorId;
            this.tradeCode = tradeCode;
            this.selectedPetId = null;
            this.receiverId = null;
            this.status = TradeStatus.SELECTING_PET;
        }
    }

    private enum TradeStatus {
        SELECTING_PET,
        AWAITING_CONFIRMATION,
        COMPLETED
    }

    private static final class Holder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static final class SellHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static final class TradeHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static final class WithdrawHolder implements InventoryHolder {
        private final String petId;
        public WithdrawHolder(String petId) { this.petId = petId; }
        @Override
        public Inventory getInventory() { return null; }
    }
}
