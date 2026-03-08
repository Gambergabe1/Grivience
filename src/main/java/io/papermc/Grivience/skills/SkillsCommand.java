package io.papermc.Grivience.skills;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SkillsCommand implements CommandExecutor, TabCompleter {
    private final SkillsGui skillsGui;

    public SkillsCommand(SkillsGui skillsGui) {
        this.skillsGui = skillsGui;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length > 0) {
            SkyblockSkill skill = resolveSkill(args[0]);
            if (skill == null) {
                player.sendMessage(ChatColor.RED + "Unknown skill. Try: combat, mining, farming, foraging, fishing, enchanting, alchemy, taming, hunting, dungeoneering, carpentry.");
                return true;
            }
            skillsGui.openSkillDetails(player, skill);
            return true;
        }

        skillsGui.openMainMenu(player);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) {
            return List.of();
        }

        String query = args[0].toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();
        for (SkyblockSkill skill : SkyblockSkill.values()) {
            String key = skill.name().toLowerCase(Locale.ROOT);
            if (key.startsWith(query)) {
                options.add(key);
            }
        }
        if ("catacombs".startsWith(query)) {
            options.add("catacombs");
        }
        return options;
    }

    private SkyblockSkill resolveSkill(String raw) {
        SkyblockSkill parsed = SkyblockSkill.parse(raw);
        if (parsed != null) {
            return parsed;
        }

        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace("-", "").replace("_", "").replace(" ", "");
        if (normalized.equals("catacombs")) {
            return SkyblockSkill.DUNGEONEERING;
        }
        for (SkyblockSkill skill : SkyblockSkill.values()) {
            String display = skill.getDisplayName().toLowerCase(Locale.ROOT).replace(" ", "");
            if (display.equals(normalized)) {
                return skill;
            }
        }
        return null;
    }
}
