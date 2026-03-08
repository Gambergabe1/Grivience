package io.papermc.Grivience.stats;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.dungeon.DungeonManager;
import io.papermc.Grivience.dungeon.DungeonSession;
import io.papermc.Grivience.mines.MiningEventManager;
import io.papermc.Grivience.skyblock.island.Island;
import io.papermc.Grivience.skyblock.island.IslandManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import io.papermc.Grivience.item.CustomItemService;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Renders a Skyblock-style sidebar scoreboard.
 */
public final class SkyblockScoreboardManager implements Listener {
    private static final String OBJECTIVE_ID = "griv_sb";
    private static final int MAX_LINES = 15;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DecimalFormat PURSE_INTEGER_FORMAT = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat PURSE_DECIMAL_FORMAT = new DecimalFormat("#,##0.0", DecimalFormatSymbols.getInstance(Locale.US));
    private static final DecimalFormat WHOLE_NUMBER_FORMAT = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));
    private static final long SKYBLOCK_DAY_MILLIS = 1_200_000L;
    private static final int SKYBLOCK_DAYS_PER_MONTH = 31;
    private static final String[] SKYBLOCK_MONTHS = {
            "Early Spring",
            "Spring",
            "Late Spring",
            "Early Summer",
            "Summer",
            "Late Summer",
            "Early Autumn",
            "Autumn",
            "Late Autumn",
            "Early Winter",
            "Winter",
            "Late Winter"
    };
    private static final String[] ENTRIES = {
            ChatColor.BLACK.toString(),
            ChatColor.DARK_BLUE.toString(),
            ChatColor.DARK_GREEN.toString(),
            ChatColor.DARK_AQUA.toString(),
            ChatColor.DARK_RED.toString(),
            ChatColor.DARK_PURPLE.toString(),
            ChatColor.GOLD.toString(),
            ChatColor.GRAY.toString(),
            ChatColor.DARK_GRAY.toString(),
            ChatColor.BLUE.toString(),
            ChatColor.GREEN.toString(),
            ChatColor.AQUA.toString(),
            ChatColor.RED.toString(),
            ChatColor.LIGHT_PURPLE.toString(),
            ChatColor.YELLOW.toString()
    };

    private final GriviencePlugin plugin;
    private final SkyblockLevelManager levelManager;
    private final IslandManager islandManager;
    private final DungeonManager dungeonManager;
    private final BitsManager bitsManager;
    private final io.papermc.Grivience.zone.ZoneManager zoneManager;
    private final Map<UUID, Scoreboard> scoreboardsByPlayer = new HashMap<>();

    private BukkitTask updateTask;

    private boolean enabled;
    private long updateTicks;
    private long defaultBits;
    private boolean showTimeLine;
    private boolean useSkyblockCalendar;
    private String title;
    private String footer;
    private String defaultObjective;
    private String profileLabel;

    public SkyblockScoreboardManager(
            GriviencePlugin plugin,
            SkyblockLevelManager levelManager,
            IslandManager islandManager,
            DungeonManager dungeonManager,
            BitsManager bitsManager,
            io.papermc.Grivience.zone.ZoneManager zoneManager
    ) {
        this.plugin = plugin;
        this.levelManager = levelManager;
        this.islandManager = islandManager;
        this.dungeonManager = dungeonManager;
        this.bitsManager = bitsManager;
        this.zoneManager = zoneManager;
    }

    public void start() {
        reload();
    }

    public void reload() {
        enabled = plugin.getConfig().getBoolean("scoreboard.custom.enabled", true);
        updateTicks = Math.max(1L, plugin.getConfig().getLong("scoreboard.custom.update-ticks", 20L));
        defaultBits = Math.max(0L, plugin.getConfig().getLong("scoreboard.custom.bits-default", 0L));
        showTimeLine = plugin.getConfig().getBoolean("scoreboard.custom.show-time-line", false);
        useSkyblockCalendar = plugin.getConfig().getBoolean("scoreboard.custom.use-skyblock-calendar", true);
        title = plugin.getConfig().getString("scoreboard.custom.title", "&e&lSkyblock");
        footer = plugin.getConfig().getString("scoreboard.custom.footer", "&eSkyblock");
        defaultObjective = plugin.getConfig().getString("scoreboard.custom.default-objective", "Reach Skyblock Level 5");
        profileLabel = plugin.getConfig().getString("scoreboard.custom.profile-label", "Profile");

        restartTask();
        if (!enabled) {
            clearAllSidebars();
        }
    }

    public void shutdown() {
        stopTask();
        clearAllSidebars();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!enabled) {
            return;
        }
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && enabled) {
                renderSidebar(player);
            }
        }, 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        scoreboardsByPlayer.remove(event.getPlayer().getUniqueId());
    }

    private void restartTask() {
        stopTask();
        if (!enabled) {
            return;
        }
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 5L, updateTicks);
    }

    private void stopTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    private void tick() {
        if (!enabled) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            renderSidebar(player);
        }
    }

    private void renderSidebar(Player player) {
        Scoreboard board = boardFor(player);
        if (board == null) {
            return;
        }

        Objective objective = board.getObjective(OBJECTIVE_ID);
        if (objective == null) {
            objective = board.registerNewObjective(OBJECTIVE_ID, "dummy", clip(color(title), 32));
        }
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.setDisplayName(clip(color(title), 32));

        List<String> lines = buildLines(player);
        int lineCount = Math.min(MAX_LINES, lines.size());

        for (int index = 0; index < lineCount; index++) {
            String entry = ENTRIES[index];
            Team team = board.getTeam(teamName(index));
            if (team == null) {
                team = board.registerNewTeam(teamName(index));
            }
            ensureEntryOwnership(board, team, entry);

            setTeamText(team, color(lines.get(index)));
            objective.getScore(entry).setScore(lineCount - index);
        }

        for (int index = lineCount; index < MAX_LINES; index++) {
            String entry = ENTRIES[index];
            board.resetScores(entry);
            Team team = board.getTeam(teamName(index));
            if (team != null) {
                team.removeEntry(entry);
                if (!Objects.equals(team.getPrefix(), "")) {
                    team.setPrefix("");
                }
                if (!Objects.equals(team.getSuffix(), "")) {
                    team.setSuffix("");
                }
            }
        }
    }

    private List<String> buildLines(Player player) {
        List<String> lines = new ArrayList<>();
        lines.add("&7" + resolveDateLine());
        if (showTimeLine) {
            lines.add("&8" + TIME_FORMAT.format(LocalTime.now()).toLowerCase(Locale.ROOT));
        }

        lines.add("");
        DungeonSession session = dungeonManager == null ? null : dungeonManager.getSession(player.getUniqueId());
        if (session != null) {
            appendDungeonLines(lines, player, session);
        } else {
            appendSkyblockLines(lines, player);
        }

        lines.add("");
        lines.add(footer);

        if (lines.size() > MAX_LINES) {
            return lines.subList(0, MAX_LINES);
        }
        return lines;
    }

    private void appendSkyblockLines(List<String> lines, Player player) {
        lines.add("&f\u23e3 &a" + resolveArea(player));
        lines.add("&7" + profileLabel + ": &a" + resolveProfile(player));
        lines.add("&7Purse: &6" + formatPurse(resolvePurse(player)));
        long playerBits = bitsManager != null ? bitsManager.getBits(player) : defaultBits;
        lines.add("&7Bits: &b" + formatWhole(playerBits));

        // Drill Fuel Display (Hypixel-style when holding a drill)
        ItemStack held = player.getInventory().getItemInMainHand();
        String drillId = plugin.getCustomItemService().itemId(held);
        if (drillId != null && drillId.endsWith("_DRILL")) {
            ItemMeta meta = held.getItemMeta();
            if (meta != null) {
                var pdc = meta.getPersistentDataContainer();
                int fuel = pdc.getOrDefault(new NamespacedKey(plugin, "drill-fuel"), PersistentDataType.INTEGER, 0);
                int max = pdc.getOrDefault(new NamespacedKey(plugin, "drill-fuel-max"), PersistentDataType.INTEGER, 20000);
                lines.add("&7Drill Fuel: &e" + formatWhole(fuel) + "&7/&e" + formatWhole(max));
            }
        }
        
        // Display Skyblock Level with proper Skyblock-style formatting
        int level = levelManager == null ? 0 : levelManager.getLevel(player);
        long xp = levelManager == null ? 0L : levelManager.getXp(player);
        long xpInto = levelManager == null ? 0L : levelManager.xpIntoCurrentLevel(player);
        long xpPerLevel = levelManager == null ? 100L : levelManager.getXpPerLevel();

        // Format: SB Level: 123✯
        ChatColor levelColor = levelManager == null ? ChatColor.GRAY : levelManager.getLevelColor(level);
        lines.add("&7SB Level: " + levelColor + level + "\u272f");

        // Add XP progress line for live display
        lines.add("&7XP: &b" + formatWhole(xpInto) + "&f/&b" + formatWhole(xpPerLevel) + " &7(&e" + Math.round((xpInto * 100.0) / Math.max(1, xpPerLevel)) + "%&7)");

        // Mining Event Display
        MiningEventManager eventManager = plugin.getMiningEventManager();
        if (eventManager != null && eventManager.getActiveEvent() != null) {
            MiningEventManager.MiningEvent active = eventManager.getActiveEvent();
            lines.add("");
            lines.add("&d&l" + active.displayName().toUpperCase(Locale.ROOT));
            long remaining = eventManager.getEventRemainingMillis() / 1000;
            lines.add("&fEnds in: &e" + formatElapsed(remaining));
            
            if (active == MiningEventManager.MiningEvent.KINGS_INSPECTION) {
                int mined = eventManager.getOresMinedDuringEvent();
                int goal = eventManager.getExtensionGoal();
                lines.add("&fOres: &a" + formatWhole(mined) + "&7/&a" + formatWhole(goal));
            }
        }

        lines.add("");
        lines.add("&eObjective");
        addWrappedPlainLines(lines, resolveObjective(player), 2, 26, "&f");
    }

    private void appendDungeonLines(List<String> lines, Player player, DungeonSession session) {
        String floorId = session.floor().id().toUpperCase(Locale.ROOT);
        lines.add("&f\u23e3 &cThe Catacombs (&a" + floorId + "&c)");
        lines.add("&7Cleared: &a" + session.getCompletionPercent() + "%");
        lines.add("&7Score: &b" + session.getEstimatedScore() + " &8(" + gradeColor(session.getEstimatedGrade()) + session.getEstimatedGrade() + "&8)");
        lines.add("&7Deaths: &c" + session.getDeaths());
        lines.add("&7Time Elapsed: &a" + formatElapsed(session.getElapsedSeconds()));
        lines.add("&7Rooms: &a" + session.getCompletedRooms() + "/" + session.getTotalRooms());
        lines.add("&7Stage: &f" + abbreviate(session.getStageLabel(), 24));
    }

    private void addWrappedPlainLines(
            List<String> lines,
            String text,
            int maxLines,
            int maxCharacters,
            String colorPrefix
    ) {
        List<String> wrapped = wrapPlainText(plain(text), maxLines, maxCharacters);
        if (wrapped.isEmpty()) {
            lines.add(colorPrefix + "Explore Skyblock");
            return;
        }
        for (String line : wrapped) {
            lines.add(colorPrefix + line);
        }
    }

    private List<String> wrapPlainText(String text, int maxLines, int maxCharacters) {
        List<String> output = new ArrayList<>();
        if (text == null || text.isBlank() || maxLines <= 0 || maxCharacters <= 1) {
            return output;
        }

        String[] words = text.trim().split("\\s+");
        StringBuilder current = new StringBuilder();
        boolean truncated = false;

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            int nextLength = current.length() == 0 ? word.length() : current.length() + 1 + word.length();
            if (nextLength <= maxCharacters) {
                if (current.length() > 0) {
                    current.append(' ');
                }
                current.append(word);
                continue;
            }

            if (current.length() == 0) {
                output.add(abbreviate(word, maxCharacters));
            } else {
                output.add(current.toString());
                current.setLength(0);
                i--;
            }

            if (output.size() >= maxLines) {
                truncated = true;
                break;
            }
        }

        if (!truncated && current.length() > 0 && output.size() < maxLines) {
            output.add(current.toString());
        } else if (current.length() > 0) {
            truncated = true;
        }

        if (truncated && !output.isEmpty()) {
            int last = output.size() - 1;
            output.set(last, abbreviate(output.get(last), maxCharacters));
            if (!output.get(last).endsWith("...")) {
                output.set(last, abbreviate(output.get(last) + "...", maxCharacters));
            }
        }

        return output;
    }

    private String resolveArea(Player player) {
        // Check for dungeon session first (highest priority)
        if (dungeonManager != null) {
            DungeonSession session = dungeonManager.getSession(player.getUniqueId());
            if (session != null) {
                return "The Catacombs (" + session.floor().id().toUpperCase(Locale.ROOT) + ")";
            }
        }

        // Check for custom zones (second priority)
        if (zoneManager != null && zoneManager.isEnabled()) {
            String zoneName = zoneManager.getZoneName(player);
            if (zoneName != null && !zoneName.equals(zoneManager.getDefaultZoneName())) {
                return zoneName;
            }
        }

        // Check for island (third priority)
        if (islandManager != null) {
            Island atLocation = islandManager.getIslandAt(player.getLocation());
            if (atLocation != null) {
                if (player.getUniqueId().equals(atLocation.getOwner())) {
                    return "Your Island";
                }
                return "Private Island";
            }
        }

        // Fallback to world-based detection
        String worldName = player.getWorld().getName();
        String hubWorld = plugin.getConfig().getString("skyblock.hub-world", "world");
        String minehubWorld = plugin.getConfig().getString("skyblock.minehub-world", "minehub_world");
        String farmhubWorld = plugin.getConfig().getString("skyblock.farmhub-world", "world");
        String islandWorld = plugin.getConfig().getString("skyblock.world-name", "skyblock_world");

        if (worldName.equalsIgnoreCase(hubWorld)) {
            return "Hub";
        }
        if (worldName.equalsIgnoreCase(minehubWorld)) {
            return "Mining Hub";
        }
        if (worldName.equalsIgnoreCase(farmhubWorld)) {
            return "Farm Hub";
        }
        if (worldName.equalsIgnoreCase(islandWorld)) {
            return "Private Island";
        }
        return readableWorldName(worldName);
    }

    private String resolveProfile(Player player) {
        io.papermc.Grivience.skyblock.profile.ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager != null) {
            io.papermc.Grivience.skyblock.profile.SkyBlockProfile profile = profileManager.getSelectedProfile(player);
            if (profile != null) {
                String name = profile.getProfileName();
                if (name != null && !name.isBlank()) {
                    return name;
                }
            }
        }

        // Legacy fallback: island-local profile label (older island system stored a "profileName").
        if (islandManager != null) {
            Island island = islandManager.getIsland(player);
            if (island != null) {
                String profile = island.getProfileName();
                if (profile != null && !profile.isBlank()) {
                    return profile;
                }
            }
        }
        return "Default";
    }

    private String resolveObjective(Player player) {
        if (dungeonManager != null) {
            DungeonSession session = dungeonManager.getSession(player.getUniqueId());
            if (session != null) {
                return "Clear " + session.floor().id().toUpperCase(Locale.ROOT);
            }
        }

        if (levelManager != null) {
            SkyblockLevelManager.FeatureUnlock next = levelManager.nextFeatureUnlock(player);
            if (next != null) {
                if (!next.unlocks().isEmpty()) {
                    return "Reach SB Level " + next.level() + " for " + next.unlocks().getFirst();
                }
                return "Reach SB Level " + next.level();
            }
        }

        return defaultObjective == null || defaultObjective.isBlank() ? "Explore Skyblock" : defaultObjective;
    }

    private double resolvePurse(Player player) {
        io.papermc.Grivience.skyblock.profile.ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager == null) {
            return 0.0D;
        }
        io.papermc.Grivience.skyblock.profile.SkyBlockProfile profile = profileManager.getSelectedProfile(player);
        if (profile == null) {
            return 0.0D;
        }
        return Math.max(0.0D, profile.getPurse());
    }

    private String formatPurse(double value) {
        double safe = Math.max(0.0D, value);
        if (Math.abs(safe - Math.rint(safe)) < 0.0001D) {
            return PURSE_INTEGER_FORMAT.format(safe);
        }
        return PURSE_DECIMAL_FORMAT.format(safe);
    }

    private String formatWhole(long value) {
        return WHOLE_NUMBER_FORMAT.format(Math.max(0L, value));
    }

    private Scoreboard boardFor(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            return null;
        }

        UUID playerId = player.getUniqueId();
        Scoreboard board = scoreboardsByPlayer.get(playerId);
        if (board == null) {
            board = manager.getNewScoreboard();
            scoreboardsByPlayer.put(playerId, board);
        }

        if (player.getScoreboard() != board) {
            player.setScoreboard(board);
        }
        return board;
    }

    private void ensureEntryOwnership(Scoreboard board, Team owner, String entry) {
        for (Team team : board.getTeams()) {
            if (team != owner && team.hasEntry(entry)) {
                team.removeEntry(entry);
            }
        }
        if (!owner.hasEntry(entry)) {
            owner.addEntry(entry);
        }
    }

    private void clearAllSidebars() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard main = manager == null ? null : manager.getMainScoreboard();

        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard managed = scoreboardsByPlayer.get(player.getUniqueId());
            if (managed != null && player.getScoreboard() == managed && main != null) {
                player.setScoreboard(main);
            }
        }
        scoreboardsByPlayer.clear();
    }

    private String teamName(int index) {
        return "gr_sb_" + index;
    }

    private void setTeamText(Team team, String line) {
        String safeLine = trimToSafeLength(line, 128);
        String prefix = safeLine;
        String suffix = "";

        if (safeLine.length() > 64) {
            int split = safeSplit(safeLine, 64);
            prefix = safeLine.substring(0, split);
            suffix = ChatColor.getLastColors(prefix) + safeLine.substring(split);
            suffix = trimToSafeLength(suffix, 64);
        }

        if (!Objects.equals(team.getPrefix(), prefix)) {
            team.setPrefix(prefix);
        }
        if (!Objects.equals(team.getSuffix(), suffix)) {
            team.setSuffix(suffix);
        }
    }

    private int safeSplit(String text, int preferred) {
        int split = Math.min(preferred, text.length());
        if (split > 0 && text.charAt(split - 1) == ChatColor.COLOR_CHAR) {
            split--;
        }
        return Math.max(0, split);
    }

    private String trimToSafeLength(String input, int maxLength) {
        if (input == null) {
            return "";
        }
        if (input.length() <= maxLength) {
            return input;
        }
        int cut = maxLength;
        if (cut > 0 && input.charAt(cut - 1) == ChatColor.COLOR_CHAR) {
            cut--;
        }
        if (cut <= 0) {
            return "";
        }
        return input.substring(0, cut);
    }

    private String clip(String input, int maxLength) {
        return trimToSafeLength(input, maxLength);
    }

    private String color(String raw) {
        return ChatColor.translateAlternateColorCodes('&', raw == null ? "" : raw);
    }

    private String plain(String raw) {
        return ChatColor.stripColor(color(raw == null ? "" : raw));
    }

    private String resolveDateLine() {
        if (!useSkyblockCalendar) {
            return DATE_FORMAT.format(LocalDate.now());
        }
        long dayIndex = Math.floorDiv(System.currentTimeMillis(), SKYBLOCK_DAY_MILLIS);
        int daysPerYear = SKYBLOCK_MONTHS.length * SKYBLOCK_DAYS_PER_MONTH;
        int dayInYear = (int) Math.floorMod(dayIndex, daysPerYear);
        int monthIndex = dayInYear / SKYBLOCK_DAYS_PER_MONTH;
        int dayOfMonth = (dayInYear % SKYBLOCK_DAYS_PER_MONTH) + 1;
        return SKYBLOCK_MONTHS[monthIndex] + " " + ordinal(dayOfMonth);
    }

    private String ordinal(int day) {
        int mod100 = day % 100;
        if (mod100 >= 11 && mod100 <= 13) {
            return day + "th";
        }
        return switch (day % 10) {
            case 1 -> day + "st";
            case 2 -> day + "nd";
            case 3 -> day + "rd";
            default -> day + "th";
        };
    }

    private String formatElapsed(long totalSeconds) {
        long safe = Math.max(0L, totalSeconds);
        long minutes = safe / 60L;
        long seconds = safe % 60L;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    private String abbreviate(String input, int maxLength) {
        if (input == null) {
            return "";
        }
        if (input.length() <= maxLength || maxLength <= 3) {
            return input.length() <= maxLength ? input : input.substring(0, Math.max(0, maxLength));
        }
        return input.substring(0, maxLength - 3) + "...";
    }

    private String gradeColor(String grade) {
        if (grade == null || grade.isBlank()) {
            return "&7";
        }
        return switch (grade.trim().toUpperCase(Locale.ROOT)) {
            case "S", "S+" -> "&6";
            case "A" -> "&a";
            case "B" -> "&e";
            case "C" -> "&6";
            default -> "&c";
        };
    }

    private String readableWorldName(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return "Unknown";
        }
        String normalized = worldName.replace('-', '_').replace(' ', '_').toLowerCase(Locale.ROOT);
        String[] parts = normalized.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? "Unknown" : builder.toString();
    }
}
