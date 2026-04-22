package io.papermc.Grivience.wizard;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.skyblock.economy.ProfileEconomyService;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import io.papermc.Grivience.stats.SkyblockCombatEngine;
import io.papermc.Grivience.stats.SkyblockManaManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WizardTowerManager implements Listener {
    private static final String CONFIG_BASE = "wizard-tower.";
    private static final String CONFIG_TIERS = CONFIG_BASE + "tiers";
    private static final String DATA_FILE_NAME = "wizard-tower-data.yml";
    private static final String GUI_TITLE = ChatColor.DARK_PURPLE + "Wizard Blessings";
    private static final int[] TIER_SLOTS = {10, 12, 14, 16, 28, 30, 32, 34};
    private static final int SUMMARY_SLOT = 4;
    private static final int STATUS_SLOT = 39;
    private static final int CLOSE_SLOT = 40;
    private static final long POTION_REFRESH_SECONDS = 4L;
    private static final long POTION_REFRESH_TASK_TICKS = 40L;
    private static final StatBonus NO_BONUS = new StatBonus(0.0D, 0.0D, 0.0D);

    private final GriviencePlugin plugin;
    private final ProfileEconomyService profileEconomy;
    private final Map<String, BlessingTier> tiers = new LinkedHashMap<>();
    private final Map<UUID, LinkedHashSet<String>> unlockedByProfile = new ConcurrentHashMap<>();
    private final Map<UUID, Long> promptCooldownByPlayer = new ConcurrentHashMap<>();

    private boolean enabled;
    private String trackedNpcId = "";
    private long interactionPromptCooldownMillis;
    private BukkitTask potionRefreshTask;
    private File dataFile;
    private FileConfiguration dataConfig;

    public WizardTowerManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.profileEconomy = new ProfileEconomyService(plugin);
        reload();
        loadData();
        startPotionRefreshTask();
    }

    public synchronized void reload() {
        FileConfiguration config = plugin.getConfig();
        enabled = config.getBoolean(CONFIG_BASE + "enabled", true);
        trackedNpcId = normalizeId(config.getString(CONFIG_BASE + "znpc-id", ""));
        interactionPromptCooldownMillis = Math.max(
                0L,
                config.getLong(CONFIG_BASE + "interaction-prompt-cooldown-seconds", 3L) * 1000L
        );
        loadTiers(config.getConfigurationSection(CONFIG_TIERS));
    }

    public void shutdown() {
        if (potionRefreshTask != null) {
            potionRefreshTask.cancel();
            potionRefreshTask = null;
        }
        saveData();
        promptCooldownByPlayer.clear();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getTrackedNpcId() {
        return trackedNpcId;
    }

    public synchronized void setTrackedNpcId(String rawNpcId) {
        trackedNpcId = normalizeId(rawNpcId);
        plugin.getConfig().set(CONFIG_BASE + "znpc-id", trackedNpcId);
        plugin.saveConfig();
    }

    public boolean isTrackedNpc(String rawNpcId) {
        String normalized = normalizeId(rawNpcId);
        return enabled && !trackedNpcId.isBlank() && trackedNpcId.equals(normalized);
    }

    public List<String> tierIds() {
        synchronized (this) {
            return new ArrayList<>(tiers.keySet());
        }
    }

    public List<BlessingTier> tiersSorted() {
        synchronized (this) {
            return new ArrayList<>(tiers.values());
        }
    }

    public void handleNpcInteraction(Player player, String rawNpcId) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (!isTrackedNpc(rawNpcId)) {
            return;
        }
        if (!shouldSendPrompt(player)) {
            return;
        }
        openGui(player);
    }

    public void openGui(Player player) {
        if (player == null) {
            return;
        }
        if (!enabled) {
            player.sendMessage(ChatColor.RED + "Wizard Tower is currently disabled.");
            return;
        }

        SkyBlockProfile selectedProfile = profileEconomy.requireSelectedProfile(player);
        if (selectedProfile == null) {
            return;
        }

        UUID profileId = selectedProfile.getProfileId();
        Set<String> unlocked = unlockedTierIds(profileId);

        WizardTowerHolder holder = new WizardTowerHolder();
        Inventory inventory = Bukkit.createInventory(holder, 45, GUI_TITLE);
        fillBackground(inventory);

        inventory.setItem(SUMMARY_SLOT, createSummaryItem(profileId, unlocked));
        inventory.setItem(STATUS_SLOT, createStatusItem(unlocked));
        inventory.setItem(CLOSE_SLOT, createSimpleItem(
                Material.BARRIER,
                ChatColor.RED + "Close",
                List.of(ChatColor.GRAY + "Close this menu.")
        ));

        List<BlessingTier> sorted = tiersSorted();
        int shown = Math.min(sorted.size(), TIER_SLOTS.length);
        for (int i = 0; i < shown; i++) {
            BlessingTier tier = sorted.get(i);
            int slot = TIER_SLOTS[i];
            boolean isUnlocked = unlocked.contains(tier.id());
            boolean canAfford = profileEconomy.has(player, tier.cost());
            inventory.setItem(slot, createTierItem(tier, isUnlocked, canAfford));
            holder.slotToTierId.put(slot, tier.id());
        }

        player.openInventory(inventory);
    }

    public void sendOverview(Player player, boolean includeUseHint) {
        openGui(player);
        if (!includeUseHint || player == null || !enabled) {
            return;
        }
        player.sendMessage(ChatColor.GRAY + "Blessings are " + ChatColor.GREEN + "permanent" + ChatColor.GRAY + " and " + ChatColor.GREEN + "stackable" + ChatColor.GRAY + " per profile.");
    }

    public void sendStatus(Player player) {
        if (player == null) {
            return;
        }
        if (!enabled) {
            player.sendMessage(ChatColor.RED + "Wizard Tower is currently disabled.");
            return;
        }

        SkyBlockProfile selectedProfile = profileEconomy.getSelectedProfile(player);
        if (selectedProfile == null) {
            player.sendMessage(ChatColor.RED + "Select a Skyblock profile first.");
            return;
        }

        UUID profileId = selectedProfile.getProfileId();
        List<BlessingTier> unlocked = unlockedTiersForProfile(profileId);
        if (unlocked.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No Wizard blessings unlocked on this profile.");
            return;
        }

        StatBonus total = statBonusForProfile(profileId);
        player.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Wizard Tower Status");
        player.sendMessage(ChatColor.GRAY + "Unlocked: " + ChatColor.GREEN + unlocked.size() + ChatColor.GRAY + "/" + ChatColor.AQUA + tiers.size());
        player.sendMessage(ChatColor.GRAY + "Total Bonus: " + ChatColor.GREEN + "+"
                + stripTrailingZeros(total.health()) + " Health"
                + ChatColor.GRAY + ", " + ChatColor.RED + "+"
                + stripTrailingZeros(total.combat()) + " Combat"
                + ChatColor.GRAY + ", " + ChatColor.AQUA + "+"
                + stripTrailingZeros(total.intelligence()) + " Intelligence");
        for (BlessingTier tier : unlocked) {
            player.sendMessage(ChatColor.AQUA + "- " + color(tier.displayName()));
        }
    }

    public PurchaseResult purchase(Player player, String tierId) {
        if (player == null) {
            return PurchaseResult.INVALID_SENDER;
        }
        if (!enabled) {
            player.sendMessage(ChatColor.RED + "Wizard Tower is currently disabled.");
            return PurchaseResult.DISABLED;
        }

        BlessingTier tier;
        synchronized (this) {
            tier = tiers.get(normalizeId(tierId));
        }
        if (tier == null) {
            player.sendMessage(ChatColor.RED + "Unknown blessing tier.");
            return PurchaseResult.TIER_NOT_FOUND;
        }

        SkyBlockProfile selectedProfile = profileEconomy.requireSelectedProfile(player);
        if (selectedProfile == null) {
            return PurchaseResult.NO_PROFILE;
        }

        UUID profileId = selectedProfile.getProfileId();
        LinkedHashSet<String> unlocked = unlockedByProfile.computeIfAbsent(profileId, ignored -> new LinkedHashSet<>());
        if (unlocked.contains(tier.id())) {
            player.sendMessage(ChatColor.YELLOW + "You already unlocked " + color(tier.displayName()) + ChatColor.YELLOW + ".");
            return PurchaseResult.ALREADY_UNLOCKED;
        }

        if (!profileEconomy.has(player, tier.cost())) {
            player.sendMessage(ChatColor.RED + "You need " + ChatColor.GOLD + formatCoins(tier.cost())
                    + ChatColor.RED + " coins to unlock " + color(tier.displayName()) + ChatColor.RED + ".");
            return PurchaseResult.NOT_ENOUGH_COINS;
        }

        if (!profileEconomy.withdraw(player, tier.cost())) {
            player.sendMessage(ChatColor.RED + "Unable to process coin payment.");
            return PurchaseResult.WITHDRAW_FAILED;
        }

        unlocked.add(tier.id());
        saveData();
        refreshPotionEffectsForSelectedProfile(player);
        refreshCombatAndMana(player);

        StatBonus total = statBonusForProfile(profileId);
        player.sendMessage(ChatColor.GREEN + "Unlocked " + color(tier.displayName())
                + ChatColor.GREEN + " for " + ChatColor.GOLD + formatCoins(tier.cost()) + ChatColor.GREEN + " coins.");
        player.sendMessage(ChatColor.GRAY + "New Total: " + ChatColor.GREEN + "+"
                + stripTrailingZeros(total.health()) + " Health"
                + ChatColor.GRAY + ", " + ChatColor.RED + "+"
                + stripTrailingZeros(total.combat()) + " Combat"
                + ChatColor.GRAY + ", " + ChatColor.AQUA + "+"
                + stripTrailingZeros(total.intelligence()) + " Intelligence");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.2f);
        return PurchaseResult.SUCCESS;
    }

    public StatBonus activeStatBonus(Player player) {
        SkyBlockProfile selectedProfile = profileEconomy.getSelectedProfile(player);
        if (selectedProfile == null || selectedProfile.getProfileId() == null) {
            return NO_BONUS;
        }
        return statBonusForProfile(selectedProfile.getProfileId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        refreshPotionEffectsForSelectedProfile(event.getPlayer());
        refreshCombatAndMana(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        promptCooldownByPlayer.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof WizardTowerHolder holder)) {
            return;
        }

        event.setCancelled(true);
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= top.getSize()) {
            return;
        }

        if (rawSlot == CLOSE_SLOT) {
            player.closeInventory();
            return;
        }
        if (rawSlot == STATUS_SLOT) {
            sendStatus(player);
            return;
        }

        String tierId = holder.slotToTierId.get(rawSlot);
        if (tierId == null) {
            return;
        }

        purchase(player, tierId);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                openGui(player);
            }
        });
    }

    private void startPotionRefreshTask() {
        if (potionRefreshTask != null) {
            potionRefreshTask.cancel();
        }
        potionRefreshTask = Bukkit.getScheduler().runTaskTimer(
                plugin,
                this::refreshPotionEffectsForOnlinePlayers,
                20L,
                POTION_REFRESH_TASK_TICKS
        );
    }

    private void refreshPotionEffectsForOnlinePlayers() {
        if (!enabled) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player == null || !player.isOnline()) {
                continue;
            }
            refreshPotionEffectsForSelectedProfile(player);
        }
    }

    private void refreshPotionEffectsForSelectedProfile(Player player) {
        if (player == null || !enabled) {
            return;
        }

        SkyBlockProfile selectedProfile = profileEconomy.getSelectedProfile(player);
        if (selectedProfile == null || selectedProfile.getProfileId() == null) {
            return;
        }

        PotionAmplifiers amplifiers = potionAmplifiersForProfile(selectedProfile.getProfileId());
        int durationTicks = (int) Math.max(20L, POTION_REFRESH_SECONDS * 20L);
        if (amplifiers.speedAmplifier() >= 0 && PotionEffectType.SPEED != null) {
            upsertPotionEffect(player, PotionEffectType.SPEED, durationTicks, amplifiers.speedAmplifier());
        }
        if (amplifiers.jumpAmplifier() >= 0 && PotionEffectType.JUMP_BOOST != null) {
            upsertPotionEffect(player, PotionEffectType.JUMP_BOOST, durationTicks, amplifiers.jumpAmplifier());
        }
    }

    private PotionAmplifiers potionAmplifiersForProfile(UUID profileId) {
        int speed = -1;
        int jump = -1;
        Set<String> unlocked = unlockedTierIds(profileId);
        for (String tierId : unlocked) {
            BlessingTier tier = tierById(tierId);
            if (tier == null) {
                continue;
            }
            speed = Math.max(speed, tier.speedAmplifier());
            jump = Math.max(jump, tier.jumpAmplifier());
        }
        return new PotionAmplifiers(speed, jump);
    }

    private void upsertPotionEffect(Player player, PotionEffectType type, int durationTicks, int amplifier) {
        PotionEffect existing = player.getPotionEffect(type);
        if (existing != null) {
            if (existing.getAmplifier() > amplifier) {
                return;
            }
            if (existing.getAmplifier() == amplifier && existing.getDuration() >= durationTicks - 10) {
                return;
            }
        }
        player.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, false, false, true), true);
    }

    private void refreshCombatAndMana(Player player) {
        SkyblockCombatEngine combatEngine = plugin.getSkyblockCombatEngine();
        if (combatEngine != null) {
            combatEngine.refreshNow(player);
        }

        SkyblockManaManager manaManager = plugin.getSkyblockManaManager();
        if (manaManager != null) {
            double current = manaManager.getMana(player);
            manaManager.setMana(player, current);
        }
    }

    private void loadTiers(ConfigurationSection section) {
        List<BlessingTier> loaded = new ArrayList<>();
        if (section != null) {
            for (String rawId : section.getKeys(false)) {
                ConfigurationSection tierSection = section.getConfigurationSection(rawId);
                if (tierSection == null) {
                    continue;
                }

                String id = normalizeId(rawId);
                if (id.isBlank()) {
                    continue;
                }

                String displayName = tierSection.getString("display-name", rawId);
                double cost = Math.max(0.0D, finiteOrZero(tierSection.getDouble("cost", 0.0D)));
                int speedAmplifier = clampAmplifier(tierSection.getInt("speed-amplifier", -1));
                int jumpAmplifier = clampAmplifier(tierSection.getInt("jump-amplifier", -1));
                double healthBonus = Math.max(0.0D, finiteOrZero(tierSection.getDouble("health-bonus", 0.0D)));
                double combatBonus = Math.max(0.0D, finiteOrZero(tierSection.getDouble("combat-bonus", 0.0D)));
                double intelligenceBonus = Math.max(0.0D, finiteOrZero(tierSection.getDouble("intelligence-bonus", 0.0D)));

                loaded.add(new BlessingTier(
                        id,
                        displayName,
                        cost,
                        speedAmplifier,
                        jumpAmplifier,
                        healthBonus,
                        combatBonus,
                        intelligenceBonus
                ));
            }
        }

        if (loaded.isEmpty()) {
            loaded = defaultTiers();
        }

        loaded.sort(Comparator
                .comparingDouble(BlessingTier::cost)
                .thenComparing(BlessingTier::id));

        tiers.clear();
        for (BlessingTier tier : loaded) {
            tiers.put(tier.id(), tier);
        }
    }

    private List<BlessingTier> defaultTiers() {
        List<BlessingTier> defaults = new ArrayList<>();
        defaults.add(new BlessingTier("apprentice", "&aApprentice Blessing", 15000.0D, 0, -1, 2.0D, 2.0D, 4.0D));
        defaults.add(new BlessingTier("adept", "&bAdept Blessing", 30000.0D, 0, 0, 3.0D, 3.0D, 6.0D));
        defaults.add(new BlessingTier("master", "&dMaster Blessing", 50000.0D, 1, 1, 5.0D, 5.0D, 10.0D));
        return defaults;
    }

    private synchronized void loadData() {
        if (dataFile == null) {
            dataFile = new File(plugin.getDataFolder(), DATA_FILE_NAME);
        }

        File parent = dataFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create Wizard Tower data file: " + e.getMessage());
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        unlockedByProfile.clear();

        ConfigurationSection profilesSection = dataConfig.getConfigurationSection("profiles");
        if (profilesSection == null) {
            return;
        }

        for (String key : profilesSection.getKeys(false)) {
            UUID profileId = parseUuid(key);
            if (profileId == null) {
                continue;
            }

            List<String> rawUnlocked = profilesSection.getStringList(key + ".unlocked");
            LinkedHashSet<String> normalized = new LinkedHashSet<>();
            for (String rawTier : rawUnlocked) {
                String tierId = normalizeId(rawTier);
                if (!tierId.isBlank() && tierById(tierId) != null) {
                    normalized.add(tierId);
                }
            }
            if (!normalized.isEmpty()) {
                unlockedByProfile.put(profileId, normalized);
            }
        }
    }

    private synchronized void saveData() {
        if (dataFile == null) {
            dataFile = new File(plugin.getDataFolder(), DATA_FILE_NAME);
        }
        if (dataConfig == null) {
            dataConfig = new YamlConfiguration();
        }

        dataConfig.set("profiles", null);
        for (Map.Entry<UUID, LinkedHashSet<String>> entry : unlockedByProfile.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            List<String> validTiers = new ArrayList<>();
            for (String tierId : entry.getValue()) {
                if (tierById(tierId) != null) {
                    validTiers.add(tierId);
                }
            }
            if (!validTiers.isEmpty()) {
                dataConfig.set("profiles." + entry.getKey() + ".unlocked", validTiers);
            }
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save Wizard Tower data: " + e.getMessage());
        }
    }

    private Set<String> unlockedTierIds(UUID profileId) {
        if (profileId == null) {
            return Set.of();
        }
        LinkedHashSet<String> unlocked = unlockedByProfile.get(profileId);
        if (unlocked == null || unlocked.isEmpty()) {
            return Set.of();
        }
        return new LinkedHashSet<>(unlocked);
    }

    private List<BlessingTier> unlockedTiersForProfile(UUID profileId) {
        List<BlessingTier> unlocked = new ArrayList<>();
        Set<String> unlockedIds = unlockedTierIds(profileId);
        for (BlessingTier tier : tiersSorted()) {
            if (unlockedIds.contains(tier.id())) {
                unlocked.add(tier);
            }
        }
        return unlocked;
    }

    private BlessingTier tierById(String tierId) {
        if (tierId == null) {
            return null;
        }
        synchronized (this) {
            return tiers.get(normalizeId(tierId));
        }
    }

    private StatBonus statBonusForProfile(UUID profileId) {
        if (profileId == null) {
            return NO_BONUS;
        }
        double health = 0.0D;
        double combat = 0.0D;
        double intelligence = 0.0D;
        for (String tierId : unlockedTierIds(profileId)) {
            BlessingTier tier = tierById(tierId);
            if (tier == null) {
                continue;
            }
            health += tier.healthBonus();
            combat += tier.combatBonus();
            intelligence += tier.intelligenceBonus();
        }
        return new StatBonus(health, combat, intelligence);
    }

    private boolean shouldSendPrompt(Player player) {
        long now = System.currentTimeMillis();
        if (interactionPromptCooldownMillis <= 0L) {
            return true;
        }
        long nextAllowed = promptCooldownByPlayer.getOrDefault(player.getUniqueId(), 0L);
        if (now < nextAllowed) {
            return false;
        }
        promptCooldownByPlayer.put(player.getUniqueId(), now + interactionPromptCooldownMillis);
        return true;
    }

    private void fillBackground(Inventory inventory) {
        ItemStack filler = createSimpleItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private ItemStack createSummaryItem(UUID profileId, Set<String> unlocked) {
        StatBonus total = statBonusForProfile(profileId);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Permanent profile unlocks.");
        lore.add(ChatColor.GRAY + "Unlocked: " + ChatColor.GREEN + unlocked.size()
                + ChatColor.GRAY + "/" + ChatColor.AQUA + tiers.size());
        lore.add("");
        lore.add(ChatColor.GRAY + "Total stacked bonuses:");
        lore.add(ChatColor.GREEN + "+" + stripTrailingZeros(total.health()) + " Health");
        lore.add(ChatColor.RED + "+" + stripTrailingZeros(total.combat()) + " Combat");
        lore.add(ChatColor.AQUA + "+" + stripTrailingZeros(total.intelligence()) + " Intelligence");
        return createSimpleItem(Material.NETHER_STAR, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Wizard Blessings", lore);
    }

    private ItemStack createStatusItem(Set<String> unlocked) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Blessings are permanent");
        lore.add(ChatColor.GRAY + "and stack together.");
        lore.add("");
        lore.add(ChatColor.GRAY + "Unlocked tiers: " + ChatColor.GREEN + unlocked.size());
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to print status in chat.");
        return createSimpleItem(Material.BOOK, ChatColor.GOLD + "Status", lore);
    }

    private ItemStack createTierItem(BlessingTier tier, boolean unlocked, boolean canAfford) {
        Material icon = unlocked ? Material.EMERALD_BLOCK : Material.ENCHANTED_BOOK;
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Cost: " + ChatColor.GOLD + formatCoins(tier.cost()) + " coins");
        lore.add("");
        lore.add(ChatColor.GRAY + "Bonuses:");
        lore.add(ChatColor.GREEN + "+" + stripTrailingZeros(tier.healthBonus()) + " Health");
        lore.add(ChatColor.RED + "+" + stripTrailingZeros(tier.combatBonus()) + " Combat");
        lore.add(ChatColor.AQUA + "+" + stripTrailingZeros(tier.intelligenceBonus()) + " Intelligence");
        if (tier.speedAmplifier() >= 0 || tier.jumpAmplifier() >= 0) {
            lore.add("");
            lore.add(ChatColor.GRAY + "Potion effects:");
            if (tier.speedAmplifier() >= 0) {
                lore.add(ChatColor.LIGHT_PURPLE + "Speed " + (tier.speedAmplifier() + 1));
            }
            if (tier.jumpAmplifier() >= 0) {
                lore.add(ChatColor.LIGHT_PURPLE + "Jump " + (tier.jumpAmplifier() + 1));
            }
        }
        lore.add("");
        if (unlocked) {
            lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "UNLOCKED");
            lore.add(ChatColor.GRAY + "Already permanently active.");
        } else if (canAfford) {
            lore.add(ChatColor.YELLOW + "Click to unlock permanently.");
        } else {
            lore.add(ChatColor.RED + "Not enough coins.");
        }
        return createSimpleItem(icon, color(tier.displayName()), lore);
    }

    private ItemStack createSimpleItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private String formatCoins(double amount) {
        return String.format(Locale.ROOT, "%,.0f", Math.max(0.0D, amount));
    }

    private String stripTrailingZeros(double value) {
        if (Math.rint(value) == value) {
            return String.format(Locale.ROOT, "%.0f", value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private String color(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }

    private String normalizeId(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0D;
    }

    private static int clampAmplifier(int amplifier) {
        if (amplifier < 0) {
            return -1;
        }
        return Math.min(amplifier, 10);
    }

    private UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (Exception ignored) {
            return null;
        }
    }

    public enum PurchaseResult {
        SUCCESS,
        DISABLED,
        INVALID_SENDER,
        TIER_NOT_FOUND,
        NO_PROFILE,
        ALREADY_UNLOCKED,
        NOT_ENOUGH_COINS,
        WITHDRAW_FAILED
    }

    public record BlessingTier(
            String id,
            String displayName,
            double cost,
            int speedAmplifier,
            int jumpAmplifier,
            double healthBonus,
            double combatBonus,
            double intelligenceBonus
    ) {
    }

    public record StatBonus(
            double health,
            double combat,
            double intelligence
    ) {
    }

    private record PotionAmplifiers(
            int speedAmplifier,
            int jumpAmplifier
    ) {
    }

    private static final class WizardTowerHolder implements InventoryHolder {
        private final Map<Integer, String> slotToTierId = new HashMap<>();

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
