package io.papermc.Grivience.skills;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SkillXpAdminCommand implements CommandExecutor, TabCompleter {
    private static final String ADMIN_PERMISSION = "grivience.admin";
    private final SkyblockSkillManager skillManager;

    public SkillXpAdminCommand(SkyblockSkillManager skillManager) {
        this.skillManager = skillManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        return switch (sub) {
            case "give" -> handleGive(sender, label, args);
            case "check" -> handleCheck(sender, label, args);
            default -> {
                sendUsage(sender, label);
                yield true;
            }
        };
    }

    private boolean handleGive(CommandSender sender, String label, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " give <player> <skill> <amount>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player must be online.");
            return true;
        }

        SkyblockSkill skill = SkyblockSkill.parse(args[2]);
        if (skill == null) {
            sender.sendMessage(ChatColor.RED + "Unknown skill: " + args[2]);
            return true;
        }

        long amount;
        try {
            amount = Long.parseLong(args[3]);
        } catch (NumberFormatException ignored) {
            sender.sendMessage(ChatColor.RED + "Amount must be a number.");
            return true;
        }

        if (amount <= 0L) {
            sender.sendMessage(ChatColor.RED + "Amount must be positive.");
            return true;
        }

        double oldXp = skillManager.getXp(target, skill);
        int oldLevel = skillManager.getLevel(target, skill);
        skillManager.addXp(target, skill, amount);
        double newXp = skillManager.getXp(target, skill);
        int newLevel = skillManager.getLevel(target, skill);

        sender.sendMessage(ChatColor.GREEN + "Granted " + format(amount) + " " + skill.getDisplayName() + " XP to " + target.getName() + ".");
        sender.sendMessage(ChatColor.GRAY + "Level " + oldLevel + " -> " + newLevel + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "XP " + format((long) oldXp) + " -> " + format((long) newXp));
        if (sender != target) {
            target.sendMessage(ChatColor.GREEN + "You received " + format(amount) + " " + skill.getDisplayName() + " XP.");
        }
        return true;
    }

    private boolean handleCheck(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " check <player> [skill]");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player must be online.");
            return true;
        }

        if (args.length >= 3) {
            SkyblockSkill skill = SkyblockSkill.parse(args[2]);
            if (skill == null) {
                sender.sendMessage(ChatColor.RED + "Unknown skill: " + args[2]);
                return true;
            }
            sendSkillDetails(sender, target, skill);
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "Skill Summary for " + target.getName() + ":");
        sender.sendMessage(ChatColor.GRAY + "Average: " + ChatColor.GREEN + String.format(Locale.US, "%.2f", skillManager.getSkillAverage(target)));
        sender.sendMessage(ChatColor.GRAY + "Total Levels: " + ChatColor.GREEN + skillManager.getTotalSkillLevels(target));
        for (SkyblockSkill skill : SkyblockSkill.values()) {
            int level = skillManager.getLevel(target, skill);
            sender.sendMessage(ChatColor.DARK_GRAY + " - " + ChatColor.YELLOW + skill.getDisplayName() + ChatColor.GRAY + ": " + ChatColor.GREEN + level);
        }
        return true;
    }

    private void sendSkillDetails(CommandSender sender, Player target, SkyblockSkill skill) {
        int level = skillManager.getLevel(target, skill);
        int max = skillManager.getMaxLevel(skill);
        double xp = skillManager.getXp(target, skill);
        double currentLevelXp = skillManager.getXpForLevel(skill, level);
        double nextLevelXp = skillManager.getXpForLevel(skill, Math.min(max, level + 1));
        double gained = Math.max(0.0D, xp - currentLevelXp);
        double required = level >= max ? 0.0D : Math.max(1.0D, nextLevelXp - currentLevelXp);
        double progress = level >= max ? 1.0D : Math.max(0.0D, Math.min(1.0D, gained / required));

        sender.sendMessage(ChatColor.GOLD + "Skill Check: " + target.getName() + " - " + skill.getDisplayName());
        sender.sendMessage(ChatColor.GRAY + "Level: " + ChatColor.GREEN + level + ChatColor.DARK_GRAY + "/" + ChatColor.GREEN + max);
        sender.sendMessage(ChatColor.GRAY + "XP: " + ChatColor.GREEN + format((long) xp));
        if (level < max) {
            sender.sendMessage(ChatColor.GRAY + "Progress: " + ChatColor.YELLOW + String.format(Locale.US, "%.1f%%", progress * 100.0D) +
                    ChatColor.DARK_GRAY + " (" + format((long) gained) + "/" + format((long) required) + ")");
        } else {
            sender.sendMessage(ChatColor.GOLD + "At max level.");
        }
    }

    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.YELLOW + "Usage:");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " give <player> <skill> <amount>");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " check <player> [skill]");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            return List.of();
        }

        if (args.length == 1) {
            return filterPrefix(List.of("give", "check"), args[0]);
        }

        if (args.length == 2) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return filterPrefix(names, args[1]);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 3 && ("give".equals(sub) || "check".equals(sub))) {
            List<String> skills = new ArrayList<>();
            for (SkyblockSkill skill : SkyblockSkill.values()) {
                skills.add(skill.name().toLowerCase(Locale.ROOT));
            }
            skills.add("catacombs");
            return filterPrefix(skills, args[2]);
        }

        if (args.length == 4 && "give".equals(sub)) {
            return filterPrefix(List.of("1", "10", "100", "1000"), args[3]);
        }

        return List.of();
    }

    private List<String> filterPrefix(List<String> values, String rawQuery) {
        String query = rawQuery == null ? "" : rawQuery.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ROOT).startsWith(query)) {
                out.add(value);
            }
        }
        return out;
    }

    private String format(long value) {
        return String.format(Locale.US, "%,d", Math.max(0L, value));
    }
}
