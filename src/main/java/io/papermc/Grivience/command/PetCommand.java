package io.papermc.Grivience.command;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.pet.PetGui;
import io.papermc.Grivience.pet.PetManager;
import io.papermc.Grivience.skyblock.economy.ProfileEconomyService;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class PetCommand implements CommandExecutor, TabCompleter {
    private final PetGui gui;
    private final PetManager petManager;
    private final GriviencePlugin plugin;
    private final ProfileEconomyService economyService;

    public PetCommand(PetGui gui, PetManager petManager, GriviencePlugin plugin) {
        this.gui = gui;
        this.petManager = petManager;
        this.plugin = plugin;
        this.economyService = new ProfileEconomyService(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "In-game only.");
            return true;
        }

        if (args.length > 0) {
            String subCmd = args[0].toLowerCase();

            switch (subCmd) {
                case "reload" -> {
                    if (!sender.hasPermission("grivience.admin")) {
                        sender.sendMessage(ChatColor.RED + "No permission.");
                        return true;
                    }
                    petManager.reloadPets();
                    sender.sendMessage(ChatColor.GREEN + "Pet system reloaded!");
                    return true;
                }

                case "sell" -> {
                    if (args.length < 2) {
                        gui.openSellMenu(player);
                        return true;
                    }
                    // Direct sell by pet ID
                    String petId = args[1].toLowerCase();
                    return sellPetDirect(player, petId);
                }

                case "sellconfirm" -> {
                    if (args.length < 2) {
                        sender.sendMessage(ChatColor.RED + "Usage: /pet sellconfirm <petid>");
                        return true;
                    }
                    String petId = args[1].toLowerCase();
                    return sellPetDirect(player, petId);
                }

                case "trade" -> {
                    if (args.length < 2) {
                        gui.openTradeMenu(player);
                        return true;
                    }
                    String code = args[1];
                    return gui.processTrade(player, code);
                }

                case "tradeaccept" -> {
                    if (args.length < 2) {
                        sender.sendMessage(ChatColor.RED + "Usage: /pet tradeaccept <code>");
                        return true;
                    }
                    String code = args[1];
                    return gui.acceptTrade(player, code);
                }

                case "help" -> {
                    sendHelp(player);
                    return true;
                }

                default -> {
                    sender.sendMessage(ChatColor.RED + "Unknown command. Use /pet help");
                    return true;
                }
            }
        }

        gui.open(player);
        return true;
    }

    private boolean sellPetDirect(Player player, String petId) {
        SkyBlockProfile profile = economyService.requireSelectedProfile(player);
        if (profile == null) {
            return true;
        }

        // Check if player owns the pet
        if (!profile.getPetData().containsKey(petId)) {
            player.sendMessage(ChatColor.RED + "You don't own this pet!");
            return true;
        }

        // Check if pet is equipped
        String equipped = petManager.equippedPet(player);
        if (petId.equalsIgnoreCase(equipped)) {
            player.sendMessage(ChatColor.RED + "You cannot sell an equipped pet!");
            return true;
        }

        // Calculate sell value
        var def = petManager.allPets().stream()
                .filter(p -> p.id().equalsIgnoreCase(petId))
                .findFirst().orElse(null);

        if (def == null) {
            player.sendMessage(ChatColor.RED + "Pet not found!");
            return true;
        }

        int level = petManager.getLevel(player, petId);
        long sellValue = calculateSellValue(def, level);

        // Remove pet from profile
        profile.getPetData().remove(petId);
        plugin.getProfileManager().saveProfile(profile);

        // Add coins to purse
        economyService.deposit(player, sellValue);

        player.sendMessage(ChatColor.GREEN + "Pet sold!");
        player.sendMessage(ChatColor.GRAY + "Sold: " + def.rarity().color() + def.displayName());
        player.sendMessage(ChatColor.GOLD + "Coins earned: " + ChatColor.YELLOW + formatCoins(sellValue));

        return true;
    }

    private long calculateSellValue(io.papermc.Grivience.pet.PetDefinition def, int level) {
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
        double levelMultiplier = 1.0 + (level - 1) * 0.01;
        return Math.round(baseValue * levelMultiplier);
    }

    private String formatCoins(long amount) {
        if (amount < 1000) return String.valueOf(amount);
        if (amount < 1000000) return String.format("%.1fk", amount / 1000.0);
        if (amount < 1000000000) return String.format("%.1fM", amount / 1000000.0);
        return String.format("%.1fB", amount / 1000000000.0);
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------------------");
        player.sendMessage(ChatColor.GOLD + "Pet Commands");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "/pet" + ChatColor.GRAY + " - Open pet menu");
        player.sendMessage(ChatColor.YELLOW + "/pet help" + ChatColor.GRAY + " - Show this help");
        player.sendMessage(ChatColor.YELLOW + "/pet sell" + ChatColor.GRAY + " - Open sell menu");
        player.sendMessage(ChatColor.YELLOW + "/pet trade" + ChatColor.GRAY + " - Open trade menu");
        player.sendMessage(ChatColor.YELLOW + "/pet trade <code>" + ChatColor.GRAY + " - Join a trade");
        player.sendMessage(ChatColor.YELLOW + "/pet tradeaccept <code>" + ChatColor.GRAY + " - Accept a trade");
        player.sendMessage(ChatColor.DARK_GRAY + "--------------------------------------------------");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("reload");
            completions.add("sell");
            completions.add("trade");
            completions.add("tradeaccept");
            completions.add("help");
            if (sender.hasPermission("grivience.admin")) {
                completions.add("reload");
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("tradeaccept") || args[0].equalsIgnoreCase("trade")) {
                completions.add("<code>");
            } else if (args[0].equalsIgnoreCase("sell") || args[0].equalsIgnoreCase("sellconfirm")) {
                if (sender instanceof Player player) {
                    var profile = economyService.requireSelectedProfile(player);
                    if (profile != null) {
                        for (String petId : profile.getPetData().keySet()) {
                            completions.add(petId);
                        }
                    }
                }
            }
        }

        // Filter based on what the user typed
        String current = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(current));

        return completions;
    }
}
