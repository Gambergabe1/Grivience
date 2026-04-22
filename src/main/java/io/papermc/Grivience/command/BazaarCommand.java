package io.papermc.Grivience.command;

import io.papermc.Grivience.bazaar.BazaarGuiManager;
import io.papermc.Grivience.bazaar.BazaarOrder;
import io.papermc.Grivience.bazaar.BazaarProduct;
import io.papermc.Grivience.bazaar.BazaarShopManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Bazaar command with Skyblock-accurate functionality.
 */
public final class BazaarCommand implements CommandExecutor, TabCompleter {
    private final BazaarShopManager shopManager;
    private final BazaarGuiManager guiManager;

    public BazaarCommand(BazaarShopManager shopManager, BazaarGuiManager guiManager) {
        this.shopManager = shopManager;
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) {
            return true;
        }
        
        if (!shopManager.isEnabled()) {
            player.sendMessage(ChatColor.RED + "Bazaar is disabled in config.");
            return true;
        }

        if (args.length == 0) {
            guiManager.openMain(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "menu", "main", "open" -> guiManager.openMain(player);
            
            case "godpotion" -> buyGodPotion(player);
            
            case "buy" -> {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /bazaar buy <product> <amount>");
                    return true;
                }
                String productId = args[1];
                int amount;
                try {
                    amount = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid amount.");
                    return true;
                }
                shopManager.instantBuy(player, productId, amount);
            }
            
            case "sell" -> {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "Usage: /bazaar sell <product> <amount>");
                    return true;
                }
                String productId = args[1];
                int amount;
                try {
                    amount = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid amount.");
                    return true;
                }
                shopManager.instantSell(player, productId, amount);
            }
            
            case "place" -> {
                if (args.length < 5) {
                    player.sendMessage(ChatColor.RED + "Usage: /bazaar place <buy|sell> <product> <amount> <price>");
                    return true;
                }
                String type = args[1].toLowerCase();
                String productId = args[2];
                int amount;
                double price;
                try {
                    amount = Integer.parseInt(args[3]);
                    price = Double.parseDouble(args[4]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Invalid amount or price.");
                    return true;
                }
                
                if (type.equals("buy")) {
                    shopManager.placeBuyOrder(player, productId, amount, price);
                } else if (type.equals("sell")) {
                    shopManager.placeSellOrder(player, productId, amount, price);
                } else {
                    player.sendMessage(ChatColor.RED + "Invalid type. Use 'buy' or 'sell'.");
                }
            }
            
            case "cancel" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /bazaar cancel <order_id>");
                    player.sendMessage(ChatColor.GRAY + "Use /bazaar orders to see your order IDs.");
                    return true;
                }
                String orderId = args[1];
                shopManager.cancelOrder(player, orderId);
            }
            
            case "orders" -> guiManager.openOrders(player);
            
            case "bag", "shoppingbag", "claim" -> guiManager.openShoppingBag(player);
            
            case "search" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /bazaar search <query>");
                    return true;
                }
                String query = String.join(" ", args).substring(args[0].length() + 1);
                List<BazaarProduct> results = shopManager.getProductCache().searchProducts(query);
                
                if (results.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "No items found matching \"" + query + "\"");
                } else {
                    player.sendMessage(ChatColor.GREEN + "Found " + results.size() + " items. Opening menu...");
                    guiManager.openCategory(player, results.get(0).getCategory(), 0);
                }
            }
            
            case "price" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /bazaar price <product>");
                    return true;
                }
                String productId = args[1];
                BazaarProduct product = shopManager.getProduct(productId);
                
                if (product == null) {
                    player.sendMessage(ChatColor.RED + "Product not found.");
                    return true;
                }
                
                player.sendMessage(ChatColor.GOLD + "=== " + product.getProductName() + " ===");
                player.sendMessage(ChatColor.GRAY + "Instant Buy: " + ChatColor.GOLD + shopManager.formatCoins(product.getInstantBuyPrice()));
                player.sendMessage(ChatColor.GRAY + "Instant Sell: " + ChatColor.GOLD + shopManager.formatCoins(product.getInstantSellPrice()));
                
                if (!Double.isNaN(product.getLowestSellOrder())) {
                    player.sendMessage(ChatColor.GRAY + "Lowest Sell Offer: " + ChatColor.GREEN + shopManager.formatCoins(product.getLowestSellOrder()));
                }
                if (!Double.isNaN(product.getHighestBuyOrder())) {
                    player.sendMessage(ChatColor.GRAY + "Highest Buy Offer: " + ChatColor.GOLD + shopManager.formatCoins(product.getHighestBuyOrder()));
                }
            }
            
            case "help" -> sendHelp(player, label);
            
            default -> {
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /" + label + " help.");
            }
        }
        return true;
    }
    
    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(ChatColor.RED + "This command can only be used in-game.");
        return null;
    }
    
    private void sendHelp(Player player, String label) {
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "=== Bazaar Help ===");
        player.sendMessage(ChatColor.YELLOW + "/" + label + ChatColor.GRAY + " - Open Bazaar menu");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " buy <product> <amount>" + ChatColor.GRAY + " - Instant buy");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " sell <product> <amount>" + ChatColor.GRAY + " - Instant sell");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " place <buy|sell> <product> <amount> <price>" + ChatColor.GRAY + " - Place order");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " cancel <order_id>" + ChatColor.GRAY + " - Cancel order");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " orders" + ChatColor.GRAY + " - View your orders");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " bag" + ChatColor.GRAY + " - View shopping bag");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " search <query>" + ChatColor.GRAY + " - Search items");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " price <product>" + ChatColor.GRAY + " - Check prices");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " godpotion" + ChatColor.GRAY + " - Buy a God Potion (1,000,000 coins)");
    }

    private void buyGodPotion(Player player) {
        double cost = 1000000.0;
        io.papermc.Grivience.skyblock.economy.ProfileEconomyService economy = new io.papermc.Grivience.skyblock.economy.ProfileEconomyService(shopManager.getPlugin());
        
        if (economy.purse(player) < cost) {
            player.sendMessage(ChatColor.RED + "You need " + String.format("%,.0f", cost) + " coins to buy a God Potion!");
            return;
        }

        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(ChatColor.RED + "Your inventory is full!");
            return;
        }

        if (economy.withdraw(player, cost)) {
            ItemStack potion = shopManager.getPlugin().getCustomItemService().createItemByKey("GOD_POTION");
            if (potion != null) {
                player.getInventory().addItem(potion);
                player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "PURCHASE! " + ChatColor.YELLOW + "You bought a God Potion for " + ChatColor.GOLD + String.format("%,.0f", cost) + " coins!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            } else {
                economy.deposit(player, cost);
                player.sendMessage(ChatColor.RED + "An error occurred while creating the God Potion. Your coins have been refunded.");
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> root = new ArrayList<>(List.of(
                "menu", "buy", "sell", "place", "cancel",
                "orders", "bag", "search", "price", "help", "godpotion"
            ));
            return filterPrefix(root, args[0]);
        }
        
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("buy") || sub.equals("sell") || sub.equals("price")) {
                List<String> products = new ArrayList<>();
                for (BazaarProduct product : shopManager.getAllProducts()) {
                    products.add(product.getProductId().toLowerCase());
                }
                return filterPrefix(products, args[1]);
            }
            if (sub.equals("place")) {
                return filterPrefix(List.of("buy", "sell"), args[1]);
            }
            if (sub.equals("cancel")) {
                List<String> orderIds = new ArrayList<>();
                if (sender instanceof Player player) {
                    java.util.UUID profileId = shopManager.getSelectedProfileId(player);
                    if (profileId != null) {
                        for (BazaarOrder order : shopManager.getOrderBook().getProfileOrders(profileId)) {
                            orderIds.add(order.getOrderId());
                        }
                    }
                }
                return filterPrefix(orderIds, args[1]);
            }
        }
        
        return List.of();
    }
    
    private List<String> filterPrefix(List<String> input, String prefix) {
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        List<String> filtered = new ArrayList<>();
        for (String candidate : input) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) {
                filtered.add(candidate);
            }
        }
        return filtered;
    }
}

