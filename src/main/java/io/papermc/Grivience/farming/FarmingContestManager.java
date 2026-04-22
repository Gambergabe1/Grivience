package io.papermc.Grivience.farming;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.gui.SkyblockGui;
import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import io.papermc.Grivience.skills.SkyblockSkill;
import io.papermc.Grivience.skills.SkyblockSkillManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class FarmingContestManager implements Listener {
    private static final String DATA_FILE_NAME = "farming-contests.yml";
    private static final String TITLE = SkyblockGui.title("Farming Contests");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("EEE h:mm a");

    private final GriviencePlugin plugin;
    private final NamespacedKey actionKey;
    private final DecimalFormat wholeNumberFormat = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));

    private final Map<UUID, ProfileContestData> profileDataById = new HashMap<>();
    private final Map<UUID, Long> requirementWarningAtMs = new HashMap<>();
    private ActiveContest activeContest;
    private org.bukkit.boss.BossBar activeBossBar;
    private String suppressedScheduledContestId;
    private BukkitTask tickTask;
    private BukkitTask autoSaveTask;
    private boolean dirty;

    private boolean enabled;
    private int minimumFarmingLevel;
    private long minimumCollectionIncrease;
    private int scheduleIntervalMinutes;
    private int scheduleDurationMinutes;
    private int scheduleOffsetMinutes;
    private long scheduleSeed;
    private List<FarmingContestCrop> cropPool = List.of(FarmingContestCrop.values());
    private double bronzePercent;
    private double silverPercent;
    private double goldPercent;
    private double platinumPercent;
    private double diamondPercent;
    private final EnumMap<FarmingContestRules.Bracket, Long> ticketRewards = new EnumMap<>(FarmingContestRules.Bracket.class);
    private boolean broadcastStart;
    private boolean broadcastEnd;
    private long requirementWarningCooldownMs;

    public FarmingContestManager(GriviencePlugin plugin) {
        this(plugin, true);
    }

    FarmingContestManager(GriviencePlugin plugin, boolean startRuntimeTasks) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "farmcontest-action");
        loadConfigValues();
        loadState();
        if (startRuntimeTasks) {
            tickContestState();
            startTasks();
        }
    }

    public void shutdown() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
        removeBossBar();
        saveState();
    }

    public void reload() {
        saveState();
        loadConfigValues();
        loadState();
        tickContestState();
    }

    public void recordHarvest(Player player, ItemStack stack) {
        if (stack == null) {
            return;
        }
        recordHarvest(player, List.of(stack));
    }

    public void recordHarvest(Player player, Collection<ItemStack> stacks) {
        if (!enabled || player == null || stacks == null || stacks.isEmpty()) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        ActiveContest contest = activeContest;
        if (contest == null) {
            return;
        }

        EnumMap<FarmingContestCrop, Long> harvested = harvestedScores(stacks, contest.crops);
        if (harvested.isEmpty()) {
            return;
        }
        if (!isEligibleToCompete(player)) {
            warnMinimumLevel(player);
            return;
        }

        UUID profileId = resolveContestProfileId(player);
        if (profileId == null) {
            return;
        }

        EnumMap<FarmingContestCrop, Long> scores = contest.scores.computeIfAbsent(profileId, ignored -> new EnumMap<>(FarmingContestCrop.class));
        for (Map.Entry<FarmingContestCrop, Long> entry : harvested.entrySet()) {
            long updated = Math.max(0L, scores.getOrDefault(entry.getKey(), 0L)) + Math.max(0L, entry.getValue());
            scores.put(entry.getKey(), updated);
        }
        dirty = true;
    }

    public List<String> buildCalendarLore(Player player) {
        List<String> lore = new ArrayList<>();
        ActiveContest contest = activeContest;
        long now = System.currentTimeMillis();
        if (!enabled) {
            lore.add(ChatColor.RED + "Farming contests are disabled.");
            lore.add("");
            lore.add(ChatColor.GRAY + "Admins can enable them in config.");
            return lore;
        }

        if (contest != null) {
            lore.add(ChatColor.GRAY + "Status: " + ChatColor.GREEN + "Active");
            lore.add(ChatColor.GRAY + "Ends in: " + ChatColor.YELLOW + formatDuration(contest.endAtMs - now));
            lore.add("");
            lore.add(ChatColor.GRAY + "Featured Crops:");
            for (FarmingContestCrop crop : contest.crops) {
                lore.add(ChatColor.GRAY + " - " + ChatColor.GOLD + crop.displayName());
            }
            if (player != null) {
                UUID profileId = resolveContestProfileId(player);
                long totalScore = totalScore(profileId, contest);
                lore.add("");
                lore.add(ChatColor.GRAY + "Your Total: " + ChatColor.YELLOW + formatWhole(totalScore));
            }
        } else {
            ScheduledContestWindow next = nextScheduledContest(now);
            lore.add(ChatColor.GRAY + "Status: " + ChatColor.YELLOW + "Waiting");
            if (next != null) {
                lore.add(ChatColor.GRAY + "Starts in: " + ChatColor.YELLOW + formatDuration(next.startAtMs - now));
                lore.add("");
                lore.add(ChatColor.GRAY + "Upcoming Crops:");
                for (FarmingContestCrop crop : next.crops) {
                    lore.add(ChatColor.GRAY + " - " + ChatColor.GOLD + crop.displayName());
                }
            } else {
                lore.add(ChatColor.RED + "No contest schedule available.");
            }
        }
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to open!");
        return lore;
    }

    public void sendStatus(CommandSender sender, Player viewer) {
        if (sender == null) {
            return;
        }

        sender.sendMessage(ChatColor.GOLD + "=== Farming Contest ===");
        if (!enabled) {
            sender.sendMessage(ChatColor.RED + "Farming contests are disabled.");
            return;
        }

        long now = System.currentTimeMillis();
        if (activeContest != null) {
            sender.sendMessage(ChatColor.GREEN + "Active Contest" + ChatColor.GRAY + " ends in " + ChatColor.YELLOW + formatDuration(activeContest.endAtMs - now));
            sender.sendMessage(ChatColor.GRAY + "Crops: " + ChatColor.GOLD + joinCropNames(activeContest.crops));
            if (viewer != null) {
                UUID profileId = resolveContestProfileId(viewer);
                for (FarmingContestCrop crop : activeContest.crops) {
                    long score = scoreFor(profileId, crop, activeContest);
                    sender.sendMessage(ChatColor.GRAY + " - " + crop.displayName() + ": " + ChatColor.YELLOW + formatWhole(score));
                }
            }
        } else {
            ScheduledContestWindow next = nextScheduledContest(now);
            if (next == null) {
                sender.sendMessage(ChatColor.RED + "No scheduled contest is available.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + "Next Contest" + ChatColor.GRAY + " starts in " + ChatColor.YELLOW + formatDuration(next.startAtMs - now));
                sender.sendMessage(ChatColor.GRAY + "Crops: " + ChatColor.GOLD + joinCropNames(next.crops));
            }
        }

        if (viewer != null) {
            UUID profileId = resolveContestProfileId(viewer);
            if (profileId != null) {
                ProfileContestData data = profileDataById.computeIfAbsent(profileId, ignored -> new ProfileContestData());
                sender.sendMessage(ChatColor.GRAY + "Tickets: " + ChatColor.AQUA + formatWhole(data.tickets));
                sender.sendMessage(ChatColor.GRAY + "Medals: "
                        + ChatColor.GOLD + formatWhole(data.bronzeMedals) + ChatColor.GRAY + " / "
                        + ChatColor.WHITE + formatWhole(data.silverMedals) + ChatColor.GRAY + " / "
                        + ChatColor.YELLOW + formatWhole(data.goldMedals));
            }
        }
    }

    public void sendSchedule(CommandSender sender) {
        if (sender == null) {
            return;
        }
        sender.sendMessage(ChatColor.GOLD + "=== Farming Contest Schedule ===");
        if (!enabled) {
            sender.sendMessage(ChatColor.RED + "Farming contests are disabled.");
            return;
        }
        long now = System.currentTimeMillis();
        List<ScheduledContestWindow> windows = upcomingScheduledContests(now, 3);
        if (windows.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "No scheduled contests are available.");
            return;
        }
        for (ScheduledContestWindow window : windows) {
            sender.sendMessage(ChatColor.YELLOW + formatAbsoluteTime(window.startAtMs)
                    + ChatColor.DARK_GRAY + " | "
                    + ChatColor.GOLD + joinCropNames(window.crops));
        }
    }

    public void sendProfileSummary(CommandSender sender, String targetLabel, UUID profileId) {
        if (sender == null || profileId == null) {
            return;
        }
        ProfileContestData data = profileDataById.computeIfAbsent(profileId, ignored -> new ProfileContestData());
        sender.sendMessage(ChatColor.GOLD + "=== Farming Contest Wallet: " + ChatColor.AQUA + targetLabel + ChatColor.GOLD + " ===");
        sender.sendMessage(ChatColor.GRAY + "Tickets: " + ChatColor.AQUA + formatWhole(data.tickets));
        sender.sendMessage(ChatColor.GRAY + "Bronze Medals: " + ChatColor.GOLD + formatWhole(data.bronzeMedals));
        sender.sendMessage(ChatColor.GRAY + "Silver Medals: " + ChatColor.WHITE + formatWhole(data.silverMedals));
        sender.sendMessage(ChatColor.GRAY + "Gold Medals: " + ChatColor.YELLOW + formatWhole(data.goldMedals));
    }

    public ContestProfileRef resolveProfileTarget(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        ProfileManager profileManager = plugin.getProfileManager();
        try {
            UUID uuid = UUID.fromString(input);
            if (profileManager != null) {
                SkyBlockProfile directProfile = profileManager.getProfile(uuid);
                if (directProfile != null) {
                    return new ContestProfileRef(directProfile.getProfileId(), profileLabel(directProfile));
                }

                SkyBlockProfile selected = profileManager.getSelectedProfile(uuid);
                if (selected != null) {
                    return new ContestProfileRef(selected.getProfileId(), profileLabel(selected));
                }
            }
        } catch (IllegalArgumentException ignored) {
        }

        Player online = Bukkit.getPlayerExact(input);
        if (online != null) {
            UUID profileId = resolveContestProfileId(online);
            if (profileId != null) {
                return new ContestProfileRef(profileId, online.getName() + " (" + selectedProfileName(online.getUniqueId()) + ")");
            }
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(input);
        if (offline != null && offline.getUniqueId() != null && profileManager != null) {
            SkyBlockProfile selected = profileManager.getSelectedProfile(offline.getUniqueId());
            if (selected != null) {
                String name = offline.getName() != null ? offline.getName() : selected.getOwnerId().toString();
                return new ContestProfileRef(selected.getProfileId(), name + " (" + selected.getProfileName() + ")");
            }
        }

        return null;
    }

    public boolean isContestActive() {
        return activeContest != null;
    }

    public long getContestRemainingMillis() {
        if (activeContest == null) return 0;
        return Math.max(0, activeContest.endAtMs - System.currentTimeMillis());
    }

    public List<FarmingContestCrop> getActiveContestCrops() {
        return activeContest != null ? activeContest.crops : List.of();
    }

    public long getScoreFor(Player player, FarmingContestCrop crop) {
        if (activeContest == null || player == null || crop == null) {
            return 0L;
        }
        UUID profileId = resolveContestProfileId(player);
        return scoreFor(profileId, crop, activeContest);
    }

    public UUID getResolveContestProfileId(Player player) {
        return resolveContestProfileId(player);
    }

    public boolean setCurrency(UUID profileId, ContestCurrency currency, long amount) {
        if (profileId == null || currency == null || amount < 0L) {
            return false;
        }
        ProfileContestData data = profileDataById.computeIfAbsent(profileId, ignored -> new ProfileContestData());
        currency.set(data, amount);
        dirty = true;
        return true;
    }

    public boolean addCurrency(UUID profileId, ContestCurrency currency, long amount) {
        if (profileId == null || currency == null || amount < 0L) {
            return false;
        }
        ProfileContestData data = profileDataById.computeIfAbsent(profileId, ignored -> new ProfileContestData());
        currency.set(data, currency.get(data) + amount);
        dirty = true;
        return true;
    }

    public boolean startForcedContest(List<FarmingContestCrop> crops, int durationMinutes, CommandSender sender) {
        if (!enabled || activeContest != null) {
            return false;
        }
        LinkedHashMap<String, FarmingContestCrop> unique = new LinkedHashMap<>();
        for (FarmingContestCrop crop : crops) {
            if (crop != null) {
                unique.put(crop.id(), crop);
            }
        }
        if (unique.size() < 3) {
            return false;
        }

        long now = System.currentTimeMillis();
        int actualMinutes = Math.max(1, durationMinutes);
        List<FarmingContestCrop> selected = new ArrayList<>(unique.values()).subList(0, 3);
        activeContest = new ActiveContest(
                "forced:" + now,
                true,
                now,
                now + Duration.ofMinutes(actualMinutes).toMillis(),
                List.copyOf(selected)
        );
        dirty = true;
        announceContestStart(activeContest, sender == null ? "Admin" : sender.getName());
        return true;
    }

    public boolean stopActiveContest(CommandSender sender, boolean rewardPlayers) {
        if (activeContest == null) {
            return false;
        }
        ActiveContest finishing = activeContest;
        activeContest = null;
        removeBossBar();
        suppressScheduledContestRestart(finishing);
        if (rewardPlayers) {
            finalizeContest(finishing, sender == null ? "Admin" : sender.getName());
        } else if (broadcastEnd) {
            Bukkit.broadcastMessage(ChatColor.RED + "Farming Contest cancelled by " + ChatColor.YELLOW + (sender == null ? "Admin" : sender.getName()) + ChatColor.RED + ".");
        }
        dirty = true;
        return true;
    }

    public boolean setActiveScore(UUID profileId, FarmingContestCrop crop, long amount, boolean additive) {
        if (activeContest == null || profileId == null || crop == null || !activeContest.crops.contains(crop) || amount < 0L) {
            return false;
        }
        EnumMap<FarmingContestCrop, Long> scores = activeContest.scores.computeIfAbsent(profileId, ignored -> new EnumMap<>(FarmingContestCrop.class));
        long base = additive ? scores.getOrDefault(crop, 0L) : 0L;
        scores.put(crop, Math.max(0L, base + amount));
        dirty = true;
        return true;
    }

    public void openMenu(Player player) {
        if (player == null) {
            return;
        }

        ContestMenuHolder holder = new ContestMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, 54, TITLE);
        holder.inventory = inventory;
        fillBackground(inventory);

        long now = System.currentTimeMillis();
        ActiveContest currentContest = activeContest;
        ScheduledContestWindow nextContest = nextScheduledContest(now);
        UUID viewerProfileId = resolveContestProfileId(player);
        ProfileContestData viewerData = profileDataById.computeIfAbsent(viewerProfileId == null ? player.getUniqueId() : viewerProfileId, ignored -> new ProfileContestData());

        inventory.setItem(13, buildSummaryItem(currentContest, nextContest, now));
        inventory.setItem(20, buildCropItem(currentContest, nextContest, viewerProfileId, viewerData, 0));
        inventory.setItem(22, buildCropItem(currentContest, nextContest, viewerProfileId, viewerData, 1));
        inventory.setItem(24, buildCropItem(currentContest, nextContest, viewerProfileId, viewerData, 2));
        inventory.setItem(30, buildWalletItem(viewerData));
        inventory.setItem(31, buildRulesItem(player));
        inventory.setItem(32, buildScheduleItem(now));
        inventory.setItem(34, buildPersonalBestItem(viewerData));
        inventory.setItem(48, createButton(Material.ARROW, ChatColor.YELLOW + "Back", List.of(ChatColor.GRAY + "Return to Calendar and Events."), "back"));
        inventory.setItem(49, createButton(Material.BARRIER, ChatColor.RED + "Close", List.of(ChatColor.GRAY + "Close this menu."), "close"));
        inventory.setItem(50, createButton(Material.SUNFLOWER, ChatColor.GREEN + "Refresh", List.of(ChatColor.GRAY + "Refresh current standings and schedule."), "refresh"));

        player.openInventory(inventory);
    }

    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        if (activeBossBar != null) {
            activeBossBar.addPlayer(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockDrop(BlockDropItemEvent event) {
        if (plugin.getConfig().getBoolean("skyblock.auto-pickup.block-drops", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        List<ItemStack> drops = new ArrayList<>();
        for (Item item : event.getItems()) {
            if (item == null) {
                continue;
            }
            ItemStack stack = item.getItemStack();
            if (stack != null && !stack.getType().isAir() && stack.getAmount() > 0) {
                drops.add(stack.clone());
            }
        }
        recordHarvest(player, drops);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ContestMenuHolder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        if (current == null) {
            return;
        }
        ItemMeta meta = current.getItemMeta();
        if (meta == null) {
            return;
        }

        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action == null || action.isBlank()) {
            return;
        }

        switch (action) {
            case "close" -> player.closeInventory();
            case "refresh" -> openMenu(player);
            case "back" -> {
                if (plugin.getSkyblockMenuManager() != null) {
                    plugin.getSkyblockMenuManager().openContestsMenu(player);
                } else {
                    player.closeInventory();
                }
            }
            default -> {
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ContestMenuHolder) {
            event.setCancelled(true);
        }
    }

    private void loadConfigValues() {
        enabled = plugin.getConfig().getBoolean("farming-contests.enabled", true);
        minimumFarmingLevel = Math.max(0, plugin.getConfig().getInt("farming-contests.minimum-farming-level", 10));
        minimumCollectionIncrease = Math.max(1L, plugin.getConfig().getLong("farming-contests.minimum-collection-increase", 100L));
        scheduleIntervalMinutes = Math.max(1, plugin.getConfig().getInt("farming-contests.schedule.interval-minutes", 20));
        scheduleDurationMinutes = Math.max(1, Math.min(scheduleIntervalMinutes, plugin.getConfig().getInt("farming-contests.schedule.duration-minutes", 20)));
        scheduleOffsetMinutes = Math.floorMod(plugin.getConfig().getInt("farming-contests.schedule.offset-minutes", 0), scheduleIntervalMinutes);
        scheduleSeed = plugin.getConfig().getLong("farming-contests.schedule.seed", 91_451L);
        cropPool = FarmingContestCrop.parsePool(plugin.getConfig().getStringList("farming-contests.crop-pool"));

        bronzePercent = clampPercent(plugin.getConfig().getDouble("farming-contests.rewards.thresholds.bronze-percent", 0.60D), 0.60D);
        silverPercent = clampPercent(plugin.getConfig().getDouble("farming-contests.rewards.thresholds.silver-percent", 0.30D), 0.30D);
        goldPercent = clampPercent(plugin.getConfig().getDouble("farming-contests.rewards.thresholds.gold-percent", 0.10D), 0.10D);
        platinumPercent = clampPercent(plugin.getConfig().getDouble("farming-contests.rewards.thresholds.platinum-percent", 0.05D), 0.05D);
        diamondPercent = clampPercent(plugin.getConfig().getDouble("farming-contests.rewards.thresholds.diamond-percent", 0.02D), 0.02D);

        ticketRewards.clear();
        ticketRewards.put(FarmingContestRules.Bracket.PARTICIPATION, Math.max(0L, plugin.getConfig().getLong("farming-contests.rewards.tickets.participation", 1L)));
        ticketRewards.put(FarmingContestRules.Bracket.BRONZE, Math.max(0L, plugin.getConfig().getLong("farming-contests.rewards.tickets.bronze", 10L)));
        ticketRewards.put(FarmingContestRules.Bracket.SILVER, Math.max(0L, plugin.getConfig().getLong("farming-contests.rewards.tickets.silver", 15L)));
        ticketRewards.put(FarmingContestRules.Bracket.GOLD, Math.max(0L, plugin.getConfig().getLong("farming-contests.rewards.tickets.gold", 25L)));
        ticketRewards.put(FarmingContestRules.Bracket.PLATINUM, Math.max(0L, plugin.getConfig().getLong("farming-contests.rewards.tickets.platinum", 30L)));
        ticketRewards.put(FarmingContestRules.Bracket.DIAMOND, Math.max(0L, plugin.getConfig().getLong("farming-contests.rewards.tickets.diamond", 35L)));

        broadcastStart = plugin.getConfig().getBoolean("farming-contests.announcements.broadcast-start", true);
        broadcastEnd = plugin.getConfig().getBoolean("farming-contests.announcements.broadcast-end", true);
        requirementWarningCooldownMs = Math.max(1_000L, plugin.getConfig().getLong("farming-contests.announcements.requirement-warning-cooldown-seconds", 15L) * 1000L);
    }

    private void startTasks() {
        if (tickTask != null) {
            tickTask.cancel();
        }
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickContestState, 20L, 20L);
        autoSaveTask = Bukkit.getScheduler().runTaskTimer(plugin, this::saveStateIfDirty, 1200L, 1200L);
    }

    private void tickContestState() {
        if (!enabled) {
            removeBossBar();
            return;
        }

        long now = System.currentTimeMillis();
        if (activeContest != null && now >= activeContest.endAtMs) {
            ActiveContest finishing = activeContest;
            activeContest = null;
            removeBossBar();
            finalizeContest(finishing, "Scheduler");
            dirty = true;
        }

        if (activeContest != null && activeContest.forced) {
            updateBossBar(now);
            return;
        }

        ScheduledContestWindow scheduled = scheduledContestAt(now);
        reconcileSuppressedScheduledContest(scheduled);
        if (scheduled == null) {
            if (activeContest != null && !activeContest.forced) {
                ActiveContest finishing = activeContest;
                activeContest = null;
                removeBossBar();
                finalizeContest(finishing, "Scheduler");
                dirty = true;
            }
            return;
        }

        if (activeContest == null) {
            if (Objects.equals(suppressedScheduledContestId, scheduled.id)) {
                return;
            }
            activeContest = new ActiveContest(scheduled.id, false, scheduled.startAtMs, scheduled.endAtMs, scheduled.crops);
            dirty = true;
            announceContestStart(activeContest, "Scheduler");
            updateBossBar(now);
            return;
        }

        if (!Objects.equals(activeContest.id, scheduled.id)) {
            ActiveContest finishing = activeContest;
            activeContest = new ActiveContest(scheduled.id, false, scheduled.startAtMs, scheduled.endAtMs, scheduled.crops);
            finalizeContest(finishing, "Scheduler");
            dirty = true;
            announceContestStart(activeContest, "Scheduler");
        }
        updateBossBar(now);
    }

    private void updateBossBar(long now) {
        if (activeContest == null) {
            removeBossBar();
            return;
        }

        String title = ChatColor.YELLOW + "" + ChatColor.BOLD + "FARMING CONTEST: " + ChatColor.GOLD + joinCropNames(activeContest.crops) 
                + ChatColor.DARK_GRAY + " | " + ChatColor.YELLOW + formatDuration(activeContest.endAtMs - now);
        
        double progress = 0.0;
        long duration = activeContest.endAtMs - activeContest.startAtMs;
        if (duration > 0) {
            progress = Math.max(0.0, Math.min(1.0, (double) (activeContest.endAtMs - now) / duration));
        }

        try {
            if (activeBossBar == null) {
                activeBossBar = Bukkit.createBossBar(title, org.bukkit.boss.BarColor.GREEN, org.bukkit.boss.BarStyle.SOLID);
                if (activeBossBar != null) {
                    Collection<? extends Player> online = Bukkit.getOnlinePlayers();
                    if (online != null) {
                        for (Player player : online) {
                            if (player != null) {
                                activeBossBar.addPlayer(player);
                            }
                        }
                    }
                }
            } else {
                activeBossBar.setTitle(title);
            }
            if (activeBossBar != null) {
                activeBossBar.setProgress(progress);
            }
        } catch (Exception ignored) {}
    }

    private void removeBossBar() {
        if (activeBossBar != null) {
            activeBossBar.removeAll();
            activeBossBar = null;
        }
    }

    private void finalizeContest(ActiveContest contest, String sourceName) {
        if (contest == null) {
            return;
        }

        Map<FarmingContestCrop, List<CropStanding>> standingsByCrop = new EnumMap<>(FarmingContestCrop.class);
        for (FarmingContestCrop crop : contest.crops) {
            standingsByCrop.put(crop, buildStandings(contest, crop));
        }

        Set<UUID> competitors = new HashSet<>(contest.scores.keySet());
        List<String> broadcastWinners = new ArrayList<>();
        for (FarmingContestCrop crop : contest.crops) {
            List<CropStanding> standings = standingsByCrop.get(crop);
            if (standings == null || standings.isEmpty()) {
                continue;
            }
            CropStanding winner = standings.get(0);
            broadcastWinners.add(crop.displayName() + ": " + winner.displayName + " (" + formatWhole(winner.score) + ")");
        }

        for (UUID profileId : competitors) {
            ProfileContestData data = profileDataById.computeIfAbsent(profileId, ignored -> new ProfileContestData());
            EnumMap<FarmingContestCrop, Long> scores = contest.scores.getOrDefault(profileId, new EnumMap<>(FarmingContestCrop.class));

            FarmingContestRules.Bracket bestBracket = FarmingContestRules.Bracket.NONE;
            FarmingContestCrop bestCrop = null;
            long bestScore = 0L;

            for (FarmingContestCrop crop : contest.crops) {
                long score = Math.max(0L, scores.getOrDefault(crop, 0L));
                if (score > 0L) {
                    long currentBest = data.personalBests.getOrDefault(crop, 0L);
                    if (score > currentBest) {
                        data.personalBests.put(crop, score);
                    }
                }
                if (score < minimumCollectionIncrease) {
                    continue;
                }
                List<CropStanding> standings = standingsByCrop.get(crop);
                CropStanding ownStanding = findStanding(standings, profileId);
                if (ownStanding == null) {
                    continue;
                }
                FarmingContestRules.Bracket bracket = ownStanding.bracket;
                if (bracket.tier() > bestBracket.tier() || (bracket == bestBracket && score > bestScore)) {
                    bestBracket = bracket;
                    bestCrop = crop;
                    bestScore = score;
                }
            }

            if (bestBracket == FarmingContestRules.Bracket.NONE && totalScore(profileId, contest) > 0L) {
                bestBracket = FarmingContestRules.Bracket.PARTICIPATION;
                bestCrop = bestCrop == null ? highestScoringCrop(scores) : bestCrop;
                bestScore = bestCrop == null ? 0L : scores.getOrDefault(bestCrop, 0L);
            }

            awardProfile(profileId, data, bestBracket);
            data.lastBracket = bestBracket;
            data.lastCrop = bestCrop;
            data.lastScore = bestScore;
            notifyProfileOwner(profileId, contest, bestBracket, bestCrop, bestScore);
        }

        if (broadcastEnd) {
            Bukkit.broadcastMessage(ChatColor.GOLD + "Farming Contest ended" + ChatColor.GRAY + " (" + sourceName + ").");
            if (!broadcastWinners.isEmpty()) {
                Bukkit.broadcastMessage(ChatColor.YELLOW + "Top Crops: " + ChatColor.GOLD + String.join(ChatColor.GRAY + " | " + ChatColor.GOLD, broadcastWinners));
            }
        }
        dirty = true;
    }

    private void awardProfile(UUID profileId, ProfileContestData data, FarmingContestRules.Bracket bracket) {
        if (profileId == null || data == null || bracket == null || bracket == FarmingContestRules.Bracket.NONE) {
            return;
        }
        data.tickets += ticketRewards.getOrDefault(bracket, 0L);
        switch (bracket) {
            case BRONZE -> data.bronzeMedals += 1L;
            case SILVER -> data.silverMedals += 1L;
            case GOLD, PLATINUM, DIAMOND -> data.goldMedals += 1L;
            default -> {
            }
        }
    }

    private List<CropStanding> buildStandings(ActiveContest contest, FarmingContestCrop crop) {
        List<CropStanding> standings = new ArrayList<>();
        if (contest == null || crop == null) {
            return standings;
        }

        for (Map.Entry<UUID, EnumMap<FarmingContestCrop, Long>> entry : contest.scores.entrySet()) {
            long score = Math.max(0L, entry.getValue().getOrDefault(crop, 0L));
            if (score < minimumCollectionIncrease) {
                continue;
            }
            standings.add(new CropStanding(entry.getKey(), score, displayName(entry.getKey())));
        }
        standings.sort(Comparator.comparingLong(CropStanding::score).reversed().thenComparing(standing -> standing.profileId.toString()));

        int nextPlacement = 0;
        long previousScore = Long.MIN_VALUE;
        for (int index = 0; index < standings.size(); index++) {
            CropStanding standing = standings.get(index);
            if (standing.score != previousScore) {
                nextPlacement = index + 1;
                previousScore = standing.score;
            }
            standing.placement = nextPlacement;
            standing.bracket = FarmingContestRules.resolveBracket(
                    standings.size(),
                    standing.placement,
                    bronzePercent,
                    silverPercent,
                    goldPercent,
                    platinumPercent,
                    diamondPercent
            );
        }
        return standings;
    }

    private CropStanding findStanding(List<CropStanding> standings, UUID profileId) {
        if (standings == null || profileId == null) {
            return null;
        }
        for (CropStanding standing : standings) {
            if (profileId.equals(standing.profileId)) {
                return standing;
            }
        }
        return null;
    }

    private FarmingContestCrop highestScoringCrop(EnumMap<FarmingContestCrop, Long> scores) {
        FarmingContestCrop best = null;
        long bestScore = 0L;
        for (Map.Entry<FarmingContestCrop, Long> entry : scores.entrySet()) {
            long value = Math.max(0L, entry.getValue());
            if (value > bestScore) {
                best = entry.getKey();
                bestScore = value;
            }
        }
        return best;
    }

    private void notifyProfileOwner(UUID profileId, ActiveContest contest, FarmingContestRules.Bracket bracket, FarmingContestCrop crop, long score) {
        if (profileId == null || bracket == null || bracket == FarmingContestRules.Bracket.NONE) {
            return;
        }
        Player online = onlineOwner(profileId);
        if (online == null) {
            return;
        }

        online.sendMessage(ChatColor.GOLD + "=== Farming Contest Result ===");
        online.sendMessage(ChatColor.GRAY + "Contest: " + ChatColor.GOLD + joinCropNames(contest.crops));
        online.sendMessage(ChatColor.GRAY + "Result: " + bracket.coloredName());
        if (crop != null) {
            online.sendMessage(ChatColor.GRAY + "Best Crop: " + ChatColor.GOLD + crop.displayName() + ChatColor.GRAY + " (" + ChatColor.YELLOW + formatWhole(score) + ChatColor.GRAY + ")");
        }
        long tickets = ticketRewards.getOrDefault(bracket, 0L);
        if (tickets > 0L) {
            online.sendMessage(ChatColor.GRAY + "Tickets Earned: " + ChatColor.AQUA + formatWhole(tickets));
        }
        if (bracket == FarmingContestRules.Bracket.BRONZE) {
            online.sendMessage(ChatColor.GRAY + "Medal Earned: " + ChatColor.GOLD + "Bronze");
        } else if (bracket == FarmingContestRules.Bracket.SILVER) {
            online.sendMessage(ChatColor.GRAY + "Medal Earned: " + ChatColor.WHITE + "Silver");
        } else if (bracket == FarmingContestRules.Bracket.GOLD
                || bracket == FarmingContestRules.Bracket.PLATINUM
                || bracket == FarmingContestRules.Bracket.DIAMOND) {
            online.sendMessage(ChatColor.GRAY + "Medal Earned: " + ChatColor.YELLOW + "Gold");
        }
        online.playSound(online.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.9F, 1.1F);
    }

    private void announceContestStart(ActiveContest contest, String sourceName) {
        if (!broadcastStart || contest == null) {
            return;
        }
        String prefix = contest.forced ? "Admin Farming Contest" : "Farming Contest";
        String msg1 = ChatColor.GOLD + prefix + ChatColor.GREEN + " started" + ChatColor.GRAY + " (" + sourceName + ").";
        String msg2 = ChatColor.GRAY + "Crops: " + ChatColor.GOLD + joinCropNames(contest.crops)
                + ChatColor.DARK_GRAY + " | "
                + ChatColor.GRAY + "Duration: " + ChatColor.YELLOW + formatDuration(contest.endAtMs - contest.startAtMs);
        
        try {
            Bukkit.broadcastMessage(msg1);
            Bukkit.broadcastMessage(msg2);
        } catch (Exception ignored) {}
    }

    private boolean isEligibleToCompete(Player player) {
        if (player == null || minimumFarmingLevel <= 0) {
            return true;
        }
        SkyblockSkillManager skillManager = plugin.getSkyblockLevelManager() == null ? null : plugin.getSkyblockLevelManager().getSkillManager();
        if (skillManager == null) {
            return true;
        }
        return skillManager.getLevel(player, SkyblockSkill.FARMING) >= minimumFarmingLevel;
    }

    private void warnMinimumLevel(Player player) {
        if (player == null || minimumFarmingLevel <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        Long lastWarning = requirementWarningAtMs.get(player.getUniqueId());
        if (lastWarning != null && (now - lastWarning) < requirementWarningCooldownMs) {
            return;
        }
        requirementWarningAtMs.put(player.getUniqueId(), now);
        player.sendMessage(ChatColor.RED + "You need Farming " + ChatColor.YELLOW + minimumFarmingLevel
                + ChatColor.RED + " to score in Farming Contests.");
    }

    private EnumMap<FarmingContestCrop, Long> harvestedScores(Collection<ItemStack> stacks, List<FarmingContestCrop> contestCrops) {
        EnumMap<FarmingContestCrop, Long> harvested = new EnumMap<>(FarmingContestCrop.class);
        if (stacks == null || stacks.isEmpty() || contestCrops == null || contestCrops.isEmpty()) {
            return harvested;
        }

        Set<FarmingContestCrop> featured = Set.copyOf(contestCrops);
        for (ItemStack stack : stacks) {
            if (stack == null || stack.getType().isAir() || stack.getAmount() <= 0) {
                continue;
            }
            FarmingContestCrop crop = FarmingContestCrop.fromHarvestMaterial(stack.getType());
            if (crop == null || !featured.contains(crop)) {
                continue;
            }
            harvested.put(crop, harvested.getOrDefault(crop, 0L) + stack.getAmount());
        }
        return harvested;
    }

    private UUID resolveContestProfileId(Player player) {
        if (player == null) {
            return null;
        }
        ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager == null) {
            return player.getUniqueId();
        }
        SkyBlockProfile profile = profileManager.getSelectedProfile(player);
        if (profile != null && profile.getProfileId() != null) {
            return profile.getProfileId();
        }
        return player.getUniqueId();
    }

    private long totalScore(UUID profileId, ActiveContest contest) {
        if (profileId == null || contest == null) {
            return 0L;
        }
        EnumMap<FarmingContestCrop, Long> scores = contest.scores.get(profileId);
        if (scores == null || scores.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (long value : scores.values()) {
            total += Math.max(0L, value);
        }
        return total;
    }

    private long scoreFor(UUID profileId, FarmingContestCrop crop, ActiveContest contest) {
        if (profileId == null || crop == null || contest == null) {
            return 0L;
        }
        return Math.max(0L, contest.scores.getOrDefault(profileId, new EnumMap<>(FarmingContestCrop.class)).getOrDefault(crop, 0L));
    }

    private ScheduledContestWindow scheduledContestAt(long atMs) {
        if (!enabled) {
            return null;
        }
        long minuteMs = 60_000L;
        long intervalMs = scheduleIntervalMinutes * minuteMs;
        long durationMs = scheduleDurationMinutes * minuteMs;
        long shifted = atMs - (scheduleOffsetMinutes * minuteMs);
        long slotIndex = Math.floorDiv(shifted, intervalMs);
        long slotStartMs = (slotIndex * intervalMs) + (scheduleOffsetMinutes * minuteMs);
        long slotEndMs = slotStartMs + durationMs;
        if (atMs < slotStartMs || atMs >= slotEndMs) {
            return null;
        }
        return new ScheduledContestWindow("scheduled:" + slotIndex, slotStartMs, slotEndMs, FarmingContestRules.selectContestCrops(cropPool, scheduleSeed, slotIndex, 3));
    }

    private ScheduledContestWindow nextScheduledContest(long fromMs) {
        List<ScheduledContestWindow> windows = upcomingScheduledContests(fromMs, 1);
        return windows.isEmpty() ? null : windows.get(0);
    }

    private void suppressScheduledContestRestart(ActiveContest contest) {
        String nextSuppressedId = null;
        if (contest != null && !contest.forced && isScheduledContestId(contest.id)) {
            nextSuppressedId = contest.id;
        } else {
            ScheduledContestWindow scheduled = scheduledContestAt(System.currentTimeMillis());
            if (scheduled != null) {
                nextSuppressedId = scheduled.id;
            }
        }
        updateSuppressedScheduledContestId(nextSuppressedId);
    }

    private void reconcileSuppressedScheduledContest(ScheduledContestWindow scheduled) {
        String nextSuppressedId = normalizedSuppressedScheduledContestId(
                suppressedScheduledContestId,
                scheduled == null ? null : scheduled.id
        );
        updateSuppressedScheduledContestId(nextSuppressedId);
    }

    private void updateSuppressedScheduledContestId(String nextSuppressedId) {
        if (nextSuppressedId != null && nextSuppressedId.isBlank()) {
            nextSuppressedId = null;
        }
        if (!Objects.equals(suppressedScheduledContestId, nextSuppressedId)) {
            suppressedScheduledContestId = nextSuppressedId;
            dirty = true;
        }
    }

    static String normalizedSuppressedScheduledContestId(String suppressedScheduledContestId, String scheduledContestId) {
        if (!isScheduledContestId(suppressedScheduledContestId)) {
            return null;
        }
        return Objects.equals(suppressedScheduledContestId, scheduledContestId) ? suppressedScheduledContestId : null;
    }

    private static boolean isScheduledContestId(String contestId) {
        return contestId != null && contestId.startsWith("scheduled:");
    }

    private List<ScheduledContestWindow> upcomingScheduledContests(long fromMs, int count) {
        List<ScheduledContestWindow> windows = new ArrayList<>();
        if (!enabled || count <= 0) {
            return windows;
        }

        long minuteMs = 60_000L;
        long intervalMs = scheduleIntervalMinutes * minuteMs;
        long shifted = fromMs - (scheduleOffsetMinutes * minuteMs);
        long baseSlot = Math.floorDiv(shifted, intervalMs);

        ScheduledContestWindow current = scheduledContestAt(fromMs);
        if (current != null) {
            windows.add(current);
        }

        long slot = baseSlot + 1L;
        while (windows.size() < count) {
            long startMs = (slot * intervalMs) + (scheduleOffsetMinutes * minuteMs);
            long endMs = startMs + (scheduleDurationMinutes * minuteMs);
            windows.add(new ScheduledContestWindow(
                    "scheduled:" + slot,
                    startMs,
                    endMs,
                    FarmingContestRules.selectContestCrops(cropPool, scheduleSeed, slot, 3)
            ));
            slot++;
        }
        return windows;
    }

    private ItemStack buildSummaryItem(ActiveContest currentContest, ScheduledContestWindow nextContest, long now) {
        if (currentContest != null) {
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Ends in: " + ChatColor.YELLOW + formatDuration(currentContest.endAtMs - now));
            lore.add(ChatColor.GRAY + "Featured: " + ChatColor.GOLD + joinCropNames(currentContest.crops));
            lore.add("");
            lore.add(ChatColor.GRAY + "Best eligible crop placement");
            lore.add(ChatColor.GRAY + "determines your final reward.");
            return createStaticItem(Material.CLOCK, ChatColor.GREEN + "Active Contest", lore);
        }

        List<String> lore = new ArrayList<>();
        if (nextContest == null) {
            lore.add(ChatColor.RED + "No schedule available.");
        } else {
            lore.add(ChatColor.GRAY + "Starts in: " + ChatColor.YELLOW + formatDuration(nextContest.startAtMs - now));
            lore.add(ChatColor.GRAY + "Begins: " + ChatColor.YELLOW + formatAbsoluteTime(nextContest.startAtMs));
            lore.add(ChatColor.GRAY + "Featured: " + ChatColor.GOLD + joinCropNames(nextContest.crops));
        }
        lore.add("");
        lore.add(ChatColor.GRAY + "Contests run every " + ChatColor.YELLOW + scheduleIntervalMinutes + ChatColor.GRAY + " minutes.");
        return createStaticItem(Material.CLOCK, ChatColor.YELLOW + "Next Contest", lore);
    }

    private ItemStack buildCropItem(ActiveContest currentContest, ScheduledContestWindow nextContest, UUID profileId, ProfileContestData data, int index) {
        FarmingContestCrop crop = null;
        boolean active = currentContest != null;
        if (active && index < currentContest.crops.size()) {
            crop = currentContest.crops.get(index);
        } else if (!active && nextContest != null && index < nextContest.crops.size()) {
            crop = nextContest.crops.get(index);
        }
        if (crop == null) {
            return createStaticItem(Material.GRAY_DYE, ChatColor.DARK_GRAY + "Empty Slot", List.of(ChatColor.GRAY + "Unused contest slot."));
        }

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Crop: " + ChatColor.GOLD + crop.displayName());
        if (active) {
            long score = scoreFor(profileId, crop, currentContest);
            CropStanding ownStanding = findStanding(buildStandings(currentContest, crop), profileId);
            CropStanding leader = firstStanding(currentContest, crop);
            lore.add(ChatColor.GRAY + "Your Score: " + ChatColor.YELLOW + formatWhole(score));
            if (ownStanding != null) {
                lore.add(ChatColor.GRAY + "Placement: " + ChatColor.YELLOW + "#" + ownStanding.placement + ChatColor.GRAY + " (" + ownStanding.bracket.coloredName() + ChatColor.GRAY + ")");
            } else {
                lore.add(ChatColor.GRAY + "Placement: " + ChatColor.DARK_GRAY + "Not eligible yet");
            }
            if (leader != null) {
                lore.add(ChatColor.GRAY + "Leader: " + ChatColor.GOLD + leader.displayName + ChatColor.GRAY + " (" + ChatColor.YELLOW + formatWhole(leader.score) + ChatColor.GRAY + ")");
            }
        } else {
            lore.add(ChatColor.GRAY + "Upcoming featured crop.");
        }
        lore.add(ChatColor.GRAY + "Personal Best: " + ChatColor.YELLOW + formatWhole(data.personalBests.getOrDefault(crop, 0L)));
        return createStaticItem(crop.icon(), ChatColor.GOLD + crop.displayName(), lore);
    }

    private ItemStack buildWalletItem(ProfileContestData data) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Cletus Tickets: " + ChatColor.AQUA + formatWhole(data.tickets));
        lore.add(ChatColor.GRAY + "Bronze Medals: " + ChatColor.GOLD + formatWhole(data.bronzeMedals));
        lore.add(ChatColor.GRAY + "Silver Medals: " + ChatColor.WHITE + formatWhole(data.silverMedals));
        lore.add(ChatColor.GRAY + "Gold Medals: " + ChatColor.YELLOW + formatWhole(data.goldMedals));
        if (data.lastBracket != null && data.lastBracket != FarmingContestRules.Bracket.NONE) {
            lore.add("");
            lore.add(ChatColor.GRAY + "Last Result: " + data.lastBracket.coloredName());
            if (data.lastCrop != null) {
                lore.add(ChatColor.GRAY + "Last Best Crop: " + ChatColor.GOLD + data.lastCrop.displayName());
            }
        }
        return createStaticItem(Material.GOLD_INGOT, ChatColor.YELLOW + "Contest Wallet", lore);
    }

    private ItemStack buildRulesItem(Player player) {
        List<String> lore = new ArrayList<>();
        int farmingLevel = 0;
        if (plugin.getSkyblockLevelManager() != null && plugin.getSkyblockLevelManager().getSkillManager() != null && player != null) {
            farmingLevel = plugin.getSkyblockLevelManager().getSkillManager().getLevel(player, SkyblockSkill.FARMING);
        }
        lore.add(ChatColor.GRAY + "Minimum Farming Level: " + ChatColor.YELLOW + minimumFarmingLevel);
        lore.add(ChatColor.GRAY + "Your Farming Level: " + ChatColor.YELLOW + farmingLevel);
        lore.add(ChatColor.GRAY + "Minimum Score for medals: " + ChatColor.YELLOW + formatWhole(minimumCollectionIncrease));
        lore.add("");
        lore.add(ChatColor.GRAY + "Best bracket across the");
        lore.add(ChatColor.GRAY + "featured crops pays out.");
        return createStaticItem(Material.BOOK, ChatColor.AQUA + "Contest Rules", lore);
    }

    private ItemStack buildScheduleItem(long now) {
        List<String> lore = new ArrayList<>();
        List<ScheduledContestWindow> windows = upcomingScheduledContests(now, 3);
        for (ScheduledContestWindow window : windows) {
            lore.add(ChatColor.YELLOW + formatAbsoluteTime(window.startAtMs));
            lore.add(ChatColor.GRAY + " " + joinCropNames(window.crops));
        }
        return createStaticItem(Material.PAPER, ChatColor.GREEN + "Upcoming Schedule", lore);
    }

    private ItemStack buildPersonalBestItem(ProfileContestData data) {
        List<Map.Entry<FarmingContestCrop, Long>> entries = new ArrayList<>(data.personalBests.entrySet());
        entries.sort(Map.Entry.<FarmingContestCrop, Long>comparingByValue().reversed());
        List<String> lore = new ArrayList<>();
        if (entries.isEmpty()) {
            lore.add(ChatColor.DARK_GRAY + "No personal bests yet.");
        } else {
            int shown = 0;
            for (Map.Entry<FarmingContestCrop, Long> entry : entries) {
                if (shown >= 5) {
                    break;
                }
                lore.add(ChatColor.GOLD + entry.getKey().displayName() + ChatColor.GRAY + ": " + ChatColor.YELLOW + formatWhole(entry.getValue()));
                shown++;
            }
        }
        return createStaticItem(Material.NETHER_STAR, ChatColor.LIGHT_PURPLE + "Personal Bests", lore);
    }

    private CropStanding firstStanding(ActiveContest contest, FarmingContestCrop crop) {
        List<CropStanding> standings = buildStandings(contest, crop);
        return standings.isEmpty() ? null : standings.get(0);
    }

    private void loadState() {
        profileDataById.clear();
        activeContest = null;
        suppressedScheduledContestId = null;

        File file = new File(plugin.getDataFolder(), DATA_FILE_NAME);
        if (!file.exists()) {
            dirty = false;
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection profiles = config.getConfigurationSection("profiles");
        if (profiles != null) {
            for (String key : profiles.getKeys(false)) {
                UUID profileId = parseUuid(key);
                if (profileId == null) {
                    continue;
                }
                ConfigurationSection section = profiles.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                ProfileContestData data = new ProfileContestData();
                data.tickets = Math.max(0L, section.getLong("tickets", 0L));
                data.bronzeMedals = Math.max(0L, section.getLong("medals.bronze", 0L));
                data.silverMedals = Math.max(0L, section.getLong("medals.silver", 0L));
                data.goldMedals = Math.max(0L, section.getLong("medals.gold", 0L));
                ConfigurationSection bests = section.getConfigurationSection("personal-bests");
                if (bests != null) {
                    for (String cropId : bests.getKeys(false)) {
                        FarmingContestCrop crop = FarmingContestCrop.fromInput(cropId);
                        if (crop != null) {
                            data.personalBests.put(crop, Math.max(0L, bests.getLong(cropId, 0L)));
                        }
                    }
                }
                String bracketName = section.getString("last-result.bracket", "NONE");
                try {
                    data.lastBracket = FarmingContestRules.Bracket.valueOf(bracketName.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                    data.lastBracket = FarmingContestRules.Bracket.NONE;
                }
                data.lastCrop = FarmingContestCrop.fromInput(section.getString("last-result.crop", ""));
                data.lastScore = Math.max(0L, section.getLong("last-result.score", 0L));
                profileDataById.put(profileId, data);
            }
        }

        ConfigurationSection active = config.getConfigurationSection("active");
        if (active != null && active.getBoolean("enabled", false)) {
            String id = active.getString("id", "");
            boolean forced = active.getBoolean("forced", false);
            long startAtMs = active.getLong("start-at-ms", 0L);
            long endAtMs = active.getLong("end-at-ms", 0L);
            List<FarmingContestCrop> crops = FarmingContestCrop.parsePool(active.getStringList("crops"));
            if (!id.isBlank() && startAtMs > 0L && endAtMs > startAtMs && crops.size() >= 3) {
                ActiveContest loaded = new ActiveContest(id, forced, startAtMs, endAtMs, List.copyOf(crops.subList(0, 3)));
                ConfigurationSection scores = active.getConfigurationSection("scores");
                if (scores != null) {
                    for (String profileKey : scores.getKeys(false)) {
                        UUID profileId = parseUuid(profileKey);
                        if (profileId == null) {
                            continue;
                        }
                        ConfigurationSection perCrop = scores.getConfigurationSection(profileKey);
                        if (perCrop == null) {
                            continue;
                        }
                        EnumMap<FarmingContestCrop, Long> values = new EnumMap<>(FarmingContestCrop.class);
                        for (String cropId : perCrop.getKeys(false)) {
                            FarmingContestCrop crop = FarmingContestCrop.fromInput(cropId);
                            if (crop != null) {
                                values.put(crop, Math.max(0L, perCrop.getLong(cropId, 0L)));
                            }
                        }
                        if (!values.isEmpty()) {
                            loaded.scores.put(profileId, values);
                        }
                    }
                }
                activeContest = loaded;
            }
        }
        String loadedSuppressedId = config.getString("scheduler.suppressed-scheduled-contest-id");
        suppressedScheduledContestId = isScheduledContestId(loadedSuppressedId) ? loadedSuppressedId : null;
        dirty = false;
    }

    private void saveStateIfDirty() {
        if (dirty) {
            saveState();
        }
    }

    private void saveState() {
        File file = new File(plugin.getDataFolder(), DATA_FILE_NAME);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, ProfileContestData> entry : profileDataById.entrySet()) {
            if (!entry.getValue().hasData()) {
                continue;
            }
            String base = "profiles." + entry.getKey() + ".";
            ProfileContestData data = entry.getValue();
            config.set(base + "tickets", data.tickets);
            config.set(base + "medals.bronze", data.bronzeMedals);
            config.set(base + "medals.silver", data.silverMedals);
            config.set(base + "medals.gold", data.goldMedals);
            for (Map.Entry<FarmingContestCrop, Long> best : data.personalBests.entrySet()) {
                if (best.getValue() > 0L) {
                    config.set(base + "personal-bests." + best.getKey().id(), best.getValue());
                }
            }
            config.set(base + "last-result.bracket", data.lastBracket.name());
            config.set(base + "last-result.crop", data.lastCrop == null ? null : data.lastCrop.id());
            config.set(base + "last-result.score", data.lastScore);
        }

        if (activeContest != null) {
            config.set("active.enabled", true);
            config.set("active.id", activeContest.id);
            config.set("active.forced", activeContest.forced);
            config.set("active.start-at-ms", activeContest.startAtMs);
            config.set("active.end-at-ms", activeContest.endAtMs);
            List<String> crops = new ArrayList<>();
            for (FarmingContestCrop crop : activeContest.crops) {
                crops.add(crop.id());
            }
            config.set("active.crops", crops);
            for (Map.Entry<UUID, EnumMap<FarmingContestCrop, Long>> entry : activeContest.scores.entrySet()) {
                for (Map.Entry<FarmingContestCrop, Long> score : entry.getValue().entrySet()) {
                    if (score.getValue() > 0L) {
                        config.set("active.scores." + entry.getKey() + "." + score.getKey().id(), score.getValue());
                    }
                }
            }
        } else {
            config.set("active.enabled", false);
        }
        config.set("scheduler.suppressed-scheduled-contest-id", suppressedScheduledContestId);

        try {
            config.save(file);
            dirty = false;
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save farming-contests.yml: " + exception.getMessage());
        }
    }

    private String joinCropNames(List<FarmingContestCrop> crops) {
        List<String> names = new ArrayList<>();
        if (crops != null) {
            for (FarmingContestCrop crop : crops) {
                if (crop != null) {
                    names.add(crop.displayName());
                }
            }
        }
        return String.join(", ", names);
    }

    private double clampPercent(double value, double fallback) {
        if (!Double.isFinite(value) || value <= 0.0D || value > 1.0D) {
            return fallback;
        }
        return value;
    }

    private String formatDuration(long millis) {
        long safeMillis = Math.max(0L, millis);
        long totalSeconds = safeMillis / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        if (minutes <= 0L) {
            return seconds + "s";
        }
        return minutes + "m " + seconds + "s";
    }

    private String formatAbsoluteTime(long millis) {
        return TIME_FORMAT.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()));
    }

    private String formatWhole(long value) {
        return wholeNumberFormat.format(Math.max(0L, value));
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = SkyblockGui.filler(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack border = SkyblockGui.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler.clone());
        }
        int[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53};
        for (int slot : borderSlots) {
            inventory.setItem(slot, border.clone());
        }
    }

    private ItemStack createStaticItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material == null ? Material.BARRIER : material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name == null ? " " : name);
            meta.setLore(lore == null ? List.of() : List.copyOf(lore));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createButton(Material material, String name, List<String> lore, String action) {
        ItemStack item = createStaticItem(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Player onlineOwner(UUID profileId) {
        ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager == null || profileId == null) {
            return null;
        }
        SkyBlockProfile profile = profileManager.getProfile(profileId);
        if (profile == null || profile.getOwnerId() == null) {
            return null;
        }
        return Bukkit.getPlayer(profile.getOwnerId());
    }

    private String displayName(UUID profileId) {
        ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager != null && profileId != null) {
            SkyBlockProfile profile = profileManager.getProfile(profileId);
            if (profile != null) {
                OfflinePlayer owner = Bukkit.getOfflinePlayer(profile.getOwnerId());
                String ownerName = owner != null && owner.getName() != null ? owner.getName() : profile.getOwnerId().toString();
                return ownerName;
            }
        }
        return profileId == null ? "Unknown" : profileId.toString();
    }

    private String selectedProfileName(UUID ownerId) {
        ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager == null || ownerId == null) {
            return "Profile";
        }
        SkyBlockProfile profile = profileManager.getSelectedProfile(ownerId);
        return profile == null ? "Profile" : profile.getProfileName();
    }

    private String profileLabel(SkyBlockProfile profile) {
        if (profile == null) {
            return "Unknown";
        }
        OfflinePlayer owner = Bukkit.getOfflinePlayer(profile.getOwnerId());
        String ownerName = owner != null && owner.getName() != null ? owner.getName() : profile.getOwnerId().toString();
        return ownerName + " (" + profile.getProfileName() + ")";
    }

    private UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public enum ContestCurrency {
        TICKETS {
            @Override
            long get(ProfileContestData data) {
                return data.tickets;
            }

            @Override
            void set(ProfileContestData data, long amount) {
                data.tickets = Math.max(0L, amount);
            }
        },
        BRONZE {
            @Override
            long get(ProfileContestData data) {
                return data.bronzeMedals;
            }

            @Override
            void set(ProfileContestData data, long amount) {
                data.bronzeMedals = Math.max(0L, amount);
            }
        },
        SILVER {
            @Override
            long get(ProfileContestData data) {
                return data.silverMedals;
            }

            @Override
            void set(ProfileContestData data, long amount) {
                data.silverMedals = Math.max(0L, amount);
            }
        },
        GOLD {
            @Override
            long get(ProfileContestData data) {
                return data.goldMedals;
            }

            @Override
            void set(ProfileContestData data, long amount) {
                data.goldMedals = Math.max(0L, amount);
            }
        };

        abstract long get(ProfileContestData data);

        abstract void set(ProfileContestData data, long amount);

        public static ContestCurrency fromInput(String input) {
            if (input == null || input.isBlank()) {
                return null;
            }
            return switch (input.trim().toLowerCase(Locale.ROOT)) {
                case "tickets", "ticket", "cletustickets", "cletus_tickets" -> TICKETS;
                case "bronze", "bronzemedals", "bronze_medals", "bronze_medal" -> BRONZE;
                case "silver", "silvermedals", "silver_medals", "silver_medal" -> SILVER;
                case "gold", "goldmedals", "gold_medals", "gold_medal" -> GOLD;
                default -> null;
            };
        }
    }

    public record ContestProfileRef(UUID profileId, String displayName) {
    }

    private static final class ContestMenuHolder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class ProfileContestData {
        private long tickets;
        private long bronzeMedals;
        private long silverMedals;
        private long goldMedals;
        private final EnumMap<FarmingContestCrop, Long> personalBests = new EnumMap<>(FarmingContestCrop.class);
        private FarmingContestRules.Bracket lastBracket = FarmingContestRules.Bracket.NONE;
        private FarmingContestCrop lastCrop;
        private long lastScore;

        private boolean hasData() {
            return tickets > 0L
                    || bronzeMedals > 0L
                    || silverMedals > 0L
                    || goldMedals > 0L
                    || !personalBests.isEmpty()
                    || lastBracket != FarmingContestRules.Bracket.NONE
                    || lastScore > 0L;
        }
    }

    private static final class ActiveContest {
        private final String id;
        private final boolean forced;
        private final long startAtMs;
        private final long endAtMs;
        private final List<FarmingContestCrop> crops;
        private final Map<UUID, EnumMap<FarmingContestCrop, Long>> scores = new HashMap<>();

        private ActiveContest(String id, boolean forced, long startAtMs, long endAtMs, List<FarmingContestCrop> crops) {
            this.id = id;
            this.forced = forced;
            this.startAtMs = startAtMs;
            this.endAtMs = endAtMs;
            this.crops = List.copyOf(crops);
        }
    }

    private static final class ScheduledContestWindow {
        private final String id;
        private final long startAtMs;
        private final long endAtMs;
        private final List<FarmingContestCrop> crops;

        private ScheduledContestWindow(String id, long startAtMs, long endAtMs, List<FarmingContestCrop> crops) {
            this.id = id;
            this.startAtMs = startAtMs;
            this.endAtMs = endAtMs;
            this.crops = List.copyOf(crops);
        }
    }

    private static final class CropStanding {
        private final UUID profileId;
        private final long score;
        private final String displayName;
        private int placement;
        private FarmingContestRules.Bracket bracket = FarmingContestRules.Bracket.NONE;

        private CropStanding(UUID profileId, long score, String displayName) {
            this.profileId = profileId;
            this.score = score;
            this.displayName = displayName;
        }

        private long score() {
            return score;
        }
    }
}
