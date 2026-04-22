package io.papermc.Grivience.mines;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.MiningItemType;
import io.papermc.Grivience.skyblock.economy.ProfileEconomyService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
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
import java.util.List;
import java.util.Locale;

public final class DrillMechanicGui implements Listener {
    private static final String BAY_TITLE = ChatColor.DARK_AQUA + "Forge Terminal: Command";
    private static final String PARTS_TITLE = ChatColor.DARK_AQUA + "Forge Terminal: Engineering";
    private static final String PROJECT_TITLE = ChatColor.DARK_AQUA + "Forge Terminal: Synthesis";
    private static final String OVERDRIVE_TITLE = ChatColor.DARK_AQUA + "Forge Terminal: Overdrive";
    private static final String FUELING_TITLE = ChatColor.DARK_AQUA + "Forge Terminal: Fueling";
    private static final List<Integer> ENGINE_PART_SLOTS = List.of(10, 11, 12, 13, 14, 15, 16);
    private static final List<Integer> TANK_PART_SLOTS = List.of(28, 29, 30, 31, 32, 33, 34);
    private static final List<Integer> PROJECT_SLOTS = List.of(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    );
    private static final List<Integer> QUEUE_SLOTS = List.of(37, 40, 43);
    private static final int OVERDRIVE_HEAT_COST = DrillForgeManager.OVERDRIVE_HEAT_COST;
    private static final double OVERDRIVE_COIN_COST = DrillForgeManager.OVERDRIVE_COIN_COST;
    private static final List<DrillPartOption> DRILL_PART_OPTIONS = List.of(
            new DrillPartOption(MiningItemType.MITHRIL_ENGINE, true),
            new DrillPartOption(MiningItemType.TITANIUM_ENGINE, true),
            new DrillPartOption(MiningItemType.GEMSTONE_ENGINE, true),
            new DrillPartOption(MiningItemType.DIVAN_ENGINE, true),
            new DrillPartOption(MiningItemType.MEDIUM_FUEL_TANK, false),
            new DrillPartOption(MiningItemType.LARGE_FUEL_TANK, false)
    );

    private final GriviencePlugin plugin;
    private final CustomItemService itemService;
    private final ProfileEconomyService economyService;
    private final DrillForgeManager forgeManager;
    private final NamespacedKey drillFuelKey;
    private final NamespacedKey drillFuelMaxKey;

    private enum ForgeView {
        BAY,
        PARTS,
        PROJECTS,
        OVERDRIVE,
        FUELING
    }

    private enum ProjectAvailability {
        READY,
        QUEUE_FULL,
        NEEDS_ITEMS,
        NEEDS_COINS
    }

    private record DrillPartOption(MiningItemType type, boolean enginePart) {
        private String itemId() {
            return type.name();
        }
    }

    private record OwnedDrillPart(DrillPartOption option, int amount) {
    }

    public DrillMechanicGui(GriviencePlugin plugin, CustomItemService itemService, ProfileEconomyService economyService) {
        this.plugin = plugin;
        this.itemService = itemService;
        this.economyService = economyService;
        this.forgeManager = new DrillForgeManager(plugin, itemService, economyService);
        this.drillFuelKey = new NamespacedKey(plugin, "drill-fuel");
        this.drillFuelMaxKey = new NamespacedKey(plugin, "drill-fuel-max");
    }

    public void shutdown() {
        forgeManager.shutdown();
    }

    public void open(Player player) {
        openBay(player);
    }

    public void openParts(Player player) {
        if (player == null) {
            return;
        }
        DrillHolder holder = new DrillHolder(ForgeView.PARTS);
        Inventory inventory = createInventory(holder, ForgeView.PARTS);

        inventory.setItem(0, sectionLabel(Material.BLUE_STAINED_GLASS_PANE, ChatColor.AQUA + "Engine Inventory", "Detected engine modules within your current loadout."));
        inventory.setItem(4, createHeldDrillItem(player));
        inventory.setItem(8, sectionLabel(Material.BLUE_STAINED_GLASS_PANE, ChatColor.GOLD + "Tank Inventory", "Detected fuel tank modules within your current loadout."));
        inventory.setItem(19, createInstalledPartStatusItem(player, true));
        inventory.setItem(22, createPartsInventorySummaryItem(player));
        inventory.setItem(25, createInstalledPartStatusItem(player, false));
        inventory.setItem(40, createPartsGuideItem());
        inventory.setItem(45, navItem(Material.ARROW, ChatColor.GOLD + "Back to Hub", "Return to the main command terminal."));
        inventory.setItem(49, navItem(Material.BARRIER, ChatColor.RED + "Close Terminal", "Terminate connection to the Drill Forge."));
        inventory.setItem(53, navItem(Material.ANVIL, ChatColor.GOLD + "Synthesis Terminal", "Navigate to synthesis for new module production."));

        populateOwnedPartSlots(inventory, player, true);
        populateOwnedPartSlots(inventory, player, false);

        player.openInventory(inventory);
    }

    public void openProjects(Player player) {
        if (player == null) {
            return;
        }
        DrillHolder holder = new DrillHolder(ForgeView.PROJECTS);
        Inventory inventory = createInventory(holder, ForgeView.PROJECTS);

        inventory.setItem(0, sectionLabel(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "Project Catalog", "Available blueprints for advanced mining technology."));
        inventory.setItem(4, createForgeCoreItem(player));
        inventory.setItem(8, sectionLabel(Material.GRAY_STAINED_GLASS_PANE, ChatColor.GRAY + "System Information", "Operational status and forge telemetry."));
        inventory.setItem(28, createProjectCatalogItem());
        inventory.setItem(29, createQueueCapacityItem(player));
        inventory.setItem(30, createForgePurseItem(player));
        inventory.setItem(31, infoItem(Material.HOPPER, ChatColor.AQUA + "Claim Terminals", List.of(
                ChatColor.GRAY + "Finished projects are routed here.",
                ChatColor.GRAY + "Claiming projects increases operational",
                ChatColor.GRAY + "efficiency and Forge Heat."
        )));
        inventory.setItem(32, createHeatMeterItem(player));
        inventory.setItem(33, createForgeLoopItem());
        inventory.setItem(34, createProjectTipsItem());
        inventory.setItem(36, sectionLabel(Material.BLACK_STAINED_GLASS_PANE, ChatColor.DARK_GRAY + "Active Processors", "Current forging cycles in progress."));
        inventory.setItem(44, sectionLabel(Material.BLACK_STAINED_GLASS_PANE, ChatColor.DARK_GRAY + "Efficiency Protocol", "High Forge Heat optimizes project duration."));
        inventory.setItem(45, navItem(Material.ARROW, ChatColor.GOLD + "Back to Bay", "Return to refueling and drill fitting."));
        inventory.setItem(49, navItem(Material.BARRIER, ChatColor.RED + "Close", "Leave the Drill Forge."));
        inventory.setItem(53, navItem(Material.LIGHTNING_ROD, ChatColor.AQUA + "Overdrive Chamber", "Convert stored heat into a timed drill buff."));

        List<DrillForgeManager.ForgeProjectType> catalog = forgeManager.catalog();
        for (int index = 0; index < PROJECT_SLOTS.size(); index++) {
            int slot = PROJECT_SLOTS.get(index);
            inventory.setItem(slot, index < catalog.size() ? createProjectItem(player, catalog.get(index)) : spacer(Material.BLACK_STAINED_GLASS_PANE));
        }

        List<DrillForgeManager.ActiveProjectView> queue = forgeManager.activeProjects(player);
        for (int index = 0; index < QUEUE_SLOTS.size(); index++) {
            inventory.setItem(QUEUE_SLOTS.get(index), createQueueItem(player, queue, index));
        }

        player.openInventory(inventory);
    }

    public void openOverdrive(Player player) {
        if (player == null) {
            return;
        }
        DrillHolder holder = new DrillHolder(ForgeView.OVERDRIVE);
        Inventory inventory = createInventory(holder, ForgeView.OVERDRIVE);

        inventory.setItem(0, sectionLabel(Material.PURPLE_STAINED_GLASS_PANE, ChatColor.LIGHT_PURPLE + "Stored Heat", "Stored Forge Heat is the fuel for Overdrive."));
        inventory.setItem(4, createForgeCoreItem(player));
        inventory.setItem(8, sectionLabel(Material.MAGENTA_STAINED_GLASS_PANE, ChatColor.AQUA + "Overdrive Effects", "See exactly what Overdrive will do before you ignite it."));
        inventory.setItem(11, createHeatMeterItem(player));
        inventory.setItem(13, createOverdriveStatusItem(player));
        inventory.setItem(15, createOverdrivePerformanceItem(player));
        inventory.setItem(20, createOverdriveFuelSavingsItem(player));
        inventory.setItem(22, createOverdriveIgnitionItem(player));
        inventory.setItem(24, createOverdriveAbilityTuningItem(player));
        inventory.setItem(29, createOverdriveActivationGuideItem(player));
        inventory.setItem(31, createOverdriveEffectMatrixItem());
        inventory.setItem(33, createProjectQueuePreviewItem(player));
        inventory.setItem(36, sectionLabel(Material.PURPLE_STAINED_GLASS_PANE, ChatColor.LIGHT_PURPLE + "Need More Heat?", "If you are short on heat, finish more forge projects first."));
        inventory.setItem(44, sectionLabel(Material.YELLOW_STAINED_GLASS_PANE, ChatColor.GOLD + "Heat Reminder", "Heat spent here can be rebuilt by claiming projects."));
        inventory.setItem(45, navItem(Material.ARROW, ChatColor.GOLD + "Back to Bay", "Return to refueling and drill fitting."));
        inventory.setItem(49, navItem(Material.BARRIER, ChatColor.RED + "Close", "Leave the Drill Forge."));
        inventory.setItem(53, navItem(Material.SMITHING_TABLE, ChatColor.GOLD + "Project Queue", "Open forge projects and claim outputs."));

        player.openInventory(inventory);
    }

    public void sendStatus(Player player) {
        if (player == null) {
            return;
        }
        DrillForgeManager.ForgeSnapshot snapshot = forgeManager.snapshot(player);
        int activeTier = forgeManager.activeOverdriveTier(player);
        player.sendMessage(ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "=== Drill Forge ===");
        player.sendMessage(ChatColor.GRAY + "Forge Heat: " + ChatColor.GOLD + snapshot.heat() + ChatColor.GRAY + "/" + ChatColor.GOLD + DrillForgeManager.MAX_HEAT
                + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "Forge Speed: " + ChatColor.GREEN + "+" + snapshot.speedBonusPercent() + "%");
        player.sendMessage(nextHeatMilestoneLine(snapshot.heat()));
        player.sendMessage(ChatColor.GRAY + "Active Projects: " + ChatColor.YELLOW + snapshot.activeProjects() + ChatColor.GRAY + "/" + ChatColor.YELLOW + "3");
        player.sendMessage(ChatColor.GRAY + "Ready To Claim: " + ChatColor.GREEN + snapshot.readyProjects());
        player.sendMessage(ChatColor.GRAY + "Overdrive: " + (snapshot.overdriveRemainingMillis() > 0L
                ? ChatColor.AQUA + forgeManager.formatDuration(snapshot.overdriveRemainingMillis())
                + ChatColor.DARK_GRAY + " (" + overdriveTierDisplay(activeTier) + ChatColor.DARK_GRAY + ")"
                : ChatColor.RED + "Offline" + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "Ignite Now: " + overdrivePreviewDisplay(snapshot.heat())));
    }

    /**
     * Pull-based integration hook for third-party GUIs and HUDs.
     */
    public DrillGuiHooks.Snapshot getGuiHookSnapshot(Player player) {
        DrillForgeManager.ForgeSnapshot forgeSnapshot = forgeManager.snapshot(player);
        DrillGuiHooks.DrillSnapshot drillSnapshot = getDrillHookSnapshot(player);
        int nextMilestone = DrillForgeManager.nextHeatMilestone(forgeSnapshot.heat());
        int activeTier = forgeManager.activeOverdriveTier(player);
        List<DrillGuiHooks.QueueEntry> queue = new ArrayList<>();
        for (DrillForgeManager.ActiveProjectView project : forgeManager.activeProjects(player)) {
            int forecastHeat = Math.min(DrillForgeManager.MAX_HEAT, forgeSnapshot.heat() + project.type().heatGain());
            long totalDuration = Math.max(0L, project.readyAt() - project.startedAt());
            long remainingMillis = Math.max(0L, project.readyAt() - System.currentTimeMillis());
            queue.add(new DrillGuiHooks.QueueEntry(
                    project.type().id(),
                    plainText(project.type().displayName()),
                    project.type().outputItemId(),
                    project.ready(),
                    project.startedAt(),
                    project.readyAt(),
                    totalDuration,
                    remainingMillis,
                    project.type().heatGain(),
                    forecastHeat,
                    DrillForgeManager.speedBonusPercentFor(forecastHeat),
                    DrillForgeManager.overdriveTierForHeat(forecastHeat)
            ));
        }
        return new DrillGuiHooks.Snapshot(
                drillSnapshot,
                new DrillGuiHooks.ForgeSnapshot(
                        forgeSnapshot.heat(),
                        DrillForgeManager.MAX_HEAT,
                        forgeSnapshot.heatTier(),
                        forgeSnapshot.speedBonusPercent(),
                        forgeSnapshot.activeProjects(),
                        3,
                        forgeSnapshot.readyProjects(),
                        forgeSnapshot.totalClaims(),
                        forgeSnapshot.overdriveRemainingMillis() > 0L,
                        activeTier,
                        DrillForgeManager.overdriveTierForHeat(forgeSnapshot.heat()),
                        forgeSnapshot.overdriveRemainingMillis(),
                        DrillForgeManager.overdriveDurationMillisForHeat(forgeSnapshot.heat()),
                        nextMilestone,
                        nextMilestone < 0 ? "Max Level" : DrillForgeManager.nextHeatMilestoneName(forgeSnapshot.heat()),
                        Math.max(0, DrillForgeManager.OVERDRIVE_HEAT_COST - forgeSnapshot.heat()),
                        DrillForgeManager.OVERDRIVE_HEAT_COST,
                        DrillForgeManager.OVERDRIVE_COIN_COST
                ),
                List.copyOf(queue)
        );
    }

    /**
     * Pull-based integration hook for the held drill.
     */
    public DrillGuiHooks.DrillSnapshot getDrillHookSnapshot(Player player) {
        return buildDrillHookSnapshot(heldDrill(player), player);
    }

    /**
     * Pull-based integration hook for an arbitrary drill item.
     */
    public DrillGuiHooks.DrillSnapshot getDrillHookSnapshot(ItemStack drill) {
        return buildDrillHookSnapshot(drill, null);
    }

    public DrillForgeManager getForgeManager() {
        return forgeManager;
    }

    public long adjustedAbilityCooldownMillis(Player player, long baseCooldownMillis) {
        return forgeManager.adjustedAbilityCooldownMillis(player, baseCooldownMillis);
    }

    public int adjustedAbilityDurationTicks(Player player, int baseDurationTicks) {
        return forgeManager.adjustedAbilityDurationTicks(player, baseDurationTicks);
    }

    public int adjustedAbilityAmplifier(Player player, int baseAmplifier) {
        return forgeManager.adjustedAbilityAmplifier(player, baseAmplifier);
    }

    public int adjustedFuelCostPerBlock(Player player, int baseFuelCost) {
        return forgeManager.adjustedFuelCostPerBlock(player, baseFuelCost);
    }

    public String overdriveActionBarSuffix(Player player) {
        return forgeManager.overdriveActionBarSuffix(player);
    }

    public boolean isOverdriveActive(Player player) {
        return forgeManager.isOverdriveActive(player);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof DrillHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getInventory())) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot == 49) {
            playUiClick(player, 0.9F);
            player.closeInventory();
            return;
        }

        switch (holder.view) {
            case BAY -> handleBayClick(player, slot);
            case PARTS -> handlePartsClick(player, slot);
            case PROJECTS -> handleProjectsClick(player, slot);
            case OVERDRIVE -> handleOverdriveClick(player, slot);
            case FUELING -> handleFuelingClick(player, slot);
        }
    }

    private void openBay(Player player) {
        if (player == null) {
            return;
        }
        DrillHolder holder = new DrillHolder(ForgeView.BAY);
        Inventory inventory = createInventory(holder, ForgeView.BAY);

        inventory.setItem(4, createForgeCoreItem(player));
        inventory.setItem(13, createHeldDrillItem(player));

        inventory.setItem(20, navItem(Material.BLAZE_POWDER, ChatColor.YELLOW + "Fueling System", "Manage drill fuel levels and energy reserves."));
        inventory.setItem(22, navItem(Material.SMITHING_TABLE, ChatColor.BLUE + "Engineering Bay", "Install engines, tanks, and structural upgrades."));
        inventory.setItem(24, navItem(Material.ANVIL, ChatColor.GOLD + "Synthesis Terminal", "Synthesize advanced components and drill parts."));
        inventory.setItem(31, navItem(Material.LIGHTNING_ROD, ChatColor.AQUA + "Overdrive Chamber", "Engage temporary drill power spikes."));

        inventory.setItem(40, createBayGuideItem());
        inventory.setItem(49, navItem(Material.BARRIER, ChatColor.RED + "Close Terminal", "Terminate connection to the Drill Forge."));

        player.openInventory(inventory);
    }

    private Inventory createInventory(DrillHolder holder, ForgeView view) {
        Inventory inventory = Bukkit.createInventory(holder, 54, titleFor(view));
        holder.inventory = inventory;
        fillBackground(inventory, view);
        return inventory;
    }

    private String titleFor(ForgeView view) {
        return switch (view) {
            case PARTS -> PARTS_TITLE;
            case PROJECTS -> PROJECT_TITLE;
            case OVERDRIVE -> OVERDRIVE_TITLE;
            case BAY -> BAY_TITLE;
            case FUELING -> FUELING_TITLE;
        };
    }

    private void handleBayClick(Player player, int slot) {
        switch (slot) {
            case 20 -> {
                playUiClick(player, 1.0F);
                openFueling(player);
            }
            case 22 -> {
                playUiClick(player, 1.1F);
                openParts(player);
            }
            case 24 -> {
                playUiClick(player, 1.05F);
                openProjects(player);
            }
            case 31 -> {
                playUiClick(player, 1.15F);
                openOverdrive(player);
            }
        }
    }

    private void openFueling(Player player) {
        if (player == null) {
            return;
        }
        DrillHolder holder = new DrillHolder(ForgeView.FUELING);
        Inventory inventory = createInventory(holder, ForgeView.FUELING);

        inventory.setItem(4, createForgeCoreItem(player));
        inventory.setItem(13, createHeldDrillItem(player));

        inventory.setItem(20, createFuelOption(Material.COAL, "Coal Feed", 100, 50.0D));
        inventory.setItem(21, createFuelOption(Material.COAL_BLOCK, "Coal Block Feed", 1000, 450.0D));
        inventory.setItem(22, createFuelOption(itemService.createMiningItem(MiningItemType.VOLTA), 5000));
        inventory.setItem(23, createFuelOption(itemService.createMiningItem(MiningItemType.OIL_BARREL), 10000));
        inventory.setItem(24, createFullRefuelItem());

        inventory.setItem(29, createFuelTelemetryItem(player));
        inventory.setItem(31, createBayGuideItem());

        inventory.setItem(45, navItem(Material.ARROW, ChatColor.GOLD + "Back to Hub", "Return to the main command terminal."));
        inventory.setItem(49, navItem(Material.BARRIER, ChatColor.RED + "Close Terminal", "Terminate connection to the Drill Forge."));

        player.openInventory(inventory);
    }

    private void handleFuelingClick(Player player, int slot) {
        if (slot == 45) {
            playUiClick(player, 1.0F);
            openBay(player);
            return;
        }

        ItemStack drill = heldDrill(player);
        if (drill == null) {
            player.sendMessage(ChatColor.RED + "Hold a drill in your main hand to refuel.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8F, 0.8F);
            return;
        }

        boolean refresh = true;
        switch (slot) {
            case 20 -> refuelWithMaterial(player, drill, Material.COAL, 100, 50.0D);
            case 21 -> refuelWithMaterial(player, drill, Material.COAL_BLOCK, 1000, 450.0D);
            case 22 -> refuelWithCustomItem(player, drill, "VOLTA", 5000);
            case 23 -> refuelWithCustomItem(player, drill, "OIL_BARREL", 10000);
            case 24 -> instantFullRefuel(player, drill);
            default -> refresh = false;
        }
        if (refresh) {
            openFueling(player);
        }
    }

    private void handlePartsClick(Player player, int slot) {
        if (slot == 45) {
            playUiClick(player, 1.0F);
            openBay(player);
            return;
        }
        if (slot == 53) {
            playUiClick(player, 1.05F);
            openProjects(player);
            return;
        }

        OwnedDrillPart ownedPart = ownedPartForSlot(player, slot);
        if (ownedPart == null) {
            return;
        }

        ItemStack drill = heldDrill(player);
        if (drill == null) {
            player.sendMessage(ChatColor.RED + "Hold a drill in your main hand to install parts.");
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.8F, 0.8F);
            return;
        }

        installPart(player, drill, ownedPart.option().itemId(), ownedPart.option().enginePart());
        openParts(player);
    }

    private void handleProjectsClick(Player player, int slot) {
        if (slot == 45) {
            playUiClick(player, 1.0F);
            openBay(player);
            return;
        }
        if (slot == 53) {
            playUiClick(player, 1.15F);
            openOverdrive(player);
            return;
        }
        int projectIndex = PROJECT_SLOTS.indexOf(slot);
        if (projectIndex >= 0) {
            List<DrillForgeManager.ForgeProjectType> catalog = forgeManager.catalog();
            if (projectIndex < catalog.size()) {
                forgeManager.startProject(player, catalog.get(projectIndex));
                openProjects(player);
            }
            return;
        }
        int queueIndex = QUEUE_SLOTS.indexOf(slot);
        if (queueIndex >= 0) {
            forgeManager.claimProject(player, queueIndex);
            openProjects(player);
        }
    }

    private void handleOverdriveClick(Player player, int slot) {
        if (slot == 45) {
            playUiClick(player, 1.0F);
            openBay(player);
            return;
        }
        if (slot == 53) {
            playUiClick(player, 1.05F);
            openProjects(player);
            return;
        }
        if (slot == 22) {
            forgeManager.activateOverdrive(player);
            openOverdrive(player);
        }
    }

    private void playUiClick(Player player, float pitch) {
        if (player == null) {
            return;
        }
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8F, pitch);
    }

    private void fillBackground(Inventory inventory, ForgeView view) {
        Material frameMaterial = Material.BLACK_STAINED_GLASS_PANE;
        Material accentMaterial = switch (view) {
            case PARTS -> Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            case PROJECTS -> Material.GRAY_STAINED_GLASS_PANE;
            case OVERDRIVE -> Material.MAGENTA_STAINED_GLASS_PANE;
            case BAY -> Material.CYAN_STAINED_GLASS_PANE;
            case FUELING -> Material.YELLOW_STAINED_GLASS_PANE;
        };

        ItemStack base = spacer(Material.BLACK_STAINED_GLASS_PANE);
        ItemStack frame = spacer(frameMaterial);
        ItemStack accent = spacer(accentMaterial);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, base);
        }

        int[] borderSlots = {
                0, 1, 2, 3, 4, 5, 6, 7, 8,
                9, 17, 18, 26, 27, 35, 36, 44,
                45, 46, 47, 48, 49, 50, 51, 52, 53
        };
        for (int slot : borderSlots) {
            inventory.setItem(slot, frame);
        }

        int[] accentSlots = switch (view) {
            case PARTS -> new int[]{19, 22, 25, 40};
            case PROJECTS -> new int[]{28, 29, 30, 31, 32, 33, 34};
            case OVERDRIVE -> new int[]{11, 13, 15, 20, 22, 24, 29, 31, 33};
            case BAY -> new int[]{20, 22, 24, 31, 40};
            case FUELING -> new int[]{20, 21, 22, 23, 24, 29, 31};
        };
        for (int slot : accentSlots) {
            inventory.setItem(slot, accent);
        }
    }

    private ItemStack createForgeCoreItem(Player player) {
        DrillForgeManager.ForgeSnapshot snapshot = forgeManager.snapshot(player);
        int activeTier = forgeManager.activeOverdriveTier(player);
        ItemStack item = infoItem(
                Material.BLAST_FURNACE,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Central Processing Core",
                List.of(
                        ChatColor.GRAY + "Operational Heat Reservoir:",
                        heatBar(snapshot.heat()),
                        ChatColor.GRAY + "Efficiency Multiplier: " + ChatColor.GREEN + "+" + snapshot.speedBonusPercent() + "%",
                        nextHeatMilestoneLine(snapshot.heat()),
                        ChatColor.GRAY + "System Queue: " + ChatColor.YELLOW + snapshot.activeProjects() + ChatColor.DARK_GRAY + " / " + ChatColor.YELLOW + "3"
                                + ChatColor.DARK_GRAY + " | " + ChatColor.GREEN + snapshot.readyProjects() + ChatColor.GRAY + " process(es) ready",
                        ChatColor.GRAY + "Project History: " + ChatColor.GOLD + snapshot.totalClaims() + " completed",
                        ChatColor.GRAY + "Overdrive Status: " + (snapshot.overdriveRemainingMillis() > 0L
                                ? ChatColor.LIGHT_PURPLE + "ACTIVE (" + forgeManager.formatDuration(snapshot.overdriveRemainingMillis()) + ")"
                                + ChatColor.DARK_GRAY + " " + overdriveTierDisplay(activeTier)
                                : ChatColor.RED + "STANDBY" + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "Ready for Ignition")
                )
        );
        return highlight(item, snapshot.readyProjects() > 0 || snapshot.overdriveRemainingMillis() > 0L);
    }

    private ItemStack createHeldDrillItem(Player player) {
        ItemStack drill = heldDrill(player);
        if (drill == null) {
            return infoItem(
                    Material.BARRIER,
                    ChatColor.RED + "No Drill Equipped",
                    List.of(
                            ChatColor.GRAY + "Hold a drill in your main hand",
                            ChatColor.GRAY + "to refuel it or install parts.",
                            "",
                            ChatColor.DARK_GRAY + "The Bay reads the held drill live."
                    )
            );
        }
        ItemStack preview = drill.clone();
        ItemMeta meta = preview.getItemMeta();
        DrillStatProfile.Profile profile = drillProfile(drill);
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.AQUA + "Forge Telemetry");
            lore.add(ChatColor.GRAY + "Fuel: " + ChatColor.YELLOW + formatInt(drillFuel(drill)) + ChatColor.GRAY + "/" + ChatColor.YELLOW + formatInt(drillFuelMax(drill)));
            if (profile != null) {
                lore.add(ChatColor.GRAY + "Burn: " + ChatColor.YELLOW + profile.fuelCostPerBlock() + ChatColor.GRAY + "/block");
                lore.add(drillBurstLine(profile));
                lore.add(ChatColor.GRAY + "Burst Cooldown: " + ChatColor.AQUA + (profile.abilityCooldownMillis() / 1000L) + "s");
                lore.add(drillCrystalNodeLine(profile));
            }
            lore.add(ChatColor.GRAY + "Engine: " + partDisplayName(drillPartId(drill, true), ChatColor.RED + "Unfitted"));
            lore.add(ChatColor.GRAY + "Tank: " + partDisplayName(drillPartId(drill, false), ChatColor.GRAY + "Stock Reservoir"));
            lore.add(ChatColor.DARK_GRAY + "Use the left side for fuel and the right side for parts.");
            meta.setLore(lore);
            preview.setItemMeta(meta);
        }
        return preview;
    }

    private ItemStack createFuelTelemetryItem(Player player) {
        ItemStack drill = heldDrill(player);
        if (drill == null) {
            return infoItem(
                    Material.LAVA_BUCKET,
                    ChatColor.GOLD + "Fuel Reservoir",
                    List.of(
                            ChatColor.GRAY + "Insert a drill to read its live",
                            ChatColor.GRAY + "fuel reservoir and fill percentage."
                    )
            );
        }
        int fuel = drillFuel(drill);
        int maxFuel = drillFuelMax(drill);
        int percent = maxFuel <= 0 ? 0 : (int) Math.round((fuel * 100.0D) / Math.max(1, maxFuel));
        DrillStatProfile.Profile profile = drillProfile(drill);
        return infoItem(
                Material.LAVA_BUCKET,
                ChatColor.GOLD + "Fuel Reservoir",
                List.of(
                        ChatColor.GRAY + "Current Fuel: " + ChatColor.YELLOW + formatInt(fuel) + ChatColor.GRAY + "/" + ChatColor.YELLOW + formatInt(maxFuel),
                        ChatColor.GRAY + "Fill Level: " + fuelPercentColor(percent) + percent + "%",
                        meterBar(fuel, maxFuel, 12, ChatColor.GOLD, ChatColor.DARK_GRAY),
                        profile == null
                                ? ChatColor.GRAY + "Burn Rate: " + ChatColor.YELLOW + DrillStatProfile.BASE_FUEL_PER_BLOCK + ChatColor.GRAY + " fuel per block"
                                : ChatColor.GRAY + "Burn Rate: " + ChatColor.YELLOW + profile.fuelCostPerBlock() + ChatColor.GRAY + " fuel per block",
                        profile == null
                                ? ChatColor.GRAY + "Range: " + ChatColor.YELLOW + "Unknown"
                                : ChatColor.GRAY + "Range: " + ChatColor.YELLOW + formatInt(Math.max(0, fuel / Math.max(1, profile.fuelCostPerBlock()))) + ChatColor.GRAY + " blocks",
                        "",
                        ChatColor.DARK_GRAY + "Coal is cheap. Volta and barrels are expedition fuel."
                )
        );
    }

    private ItemStack createInstalledPartsItem(Player player) {
        ItemStack drill = heldDrill(player);
        if (drill == null) {
            return infoItem(
                    Material.PISTON,
                    ChatColor.BLUE + "Installed Parts",
                    List.of(
                            ChatColor.GRAY + "Insert a drill to view current",
                            ChatColor.GRAY + "engine and tank fitting."
                )
            );
        }
        DrillStatProfile.Profile profile = drillProfile(drill);
        return infoItem(
                Material.PISTON,
                ChatColor.BLUE + "Installed Parts",
                List.of(
                        ChatColor.GRAY + "Engine: " + partDisplayName(drillPartId(drill, true), ChatColor.RED + "Unfitted"),
                        ChatColor.GRAY + "Tank: " + partDisplayName(drillPartId(drill, false), ChatColor.GRAY + "Stock Reservoir"),
                        profile == null ? ChatColor.GRAY + "Mining Speed: " + ChatColor.GREEN + "+0"
                                : ChatColor.GRAY + "Mining Speed: " + ChatColor.GREEN + "+" + profile.miningSpeed(),
                        profile == null ? ChatColor.GRAY + "Fuel Burn: " + ChatColor.YELLOW + DrillStatProfile.BASE_FUEL_PER_BLOCK + ChatColor.GRAY + "/block"
                                : ChatColor.GRAY + "Fuel Burn: " + ChatColor.YELLOW + profile.fuelCostPerBlock() + ChatColor.GRAY + "/block",
                        profile == null ? ChatColor.GRAY + "Burst: " + ChatColor.AQUA + "Unavailable"
                                : drillBurstLine(profile),
                        profile == null ? ChatColor.GRAY + "Burst Cooldown: " + ChatColor.AQUA + "--"
                                : ChatColor.GRAY + "Burst Cooldown: " + ChatColor.AQUA + (profile.abilityCooldownMillis() / 1000L) + "s",
                        profile == null ? ChatColor.GRAY + "Crystal Nodes: " + ChatColor.GRAY + "Standard"
                                : drillCrystalNodeLine(profile),
                        "",
                        ChatColor.DARK_GRAY + "Open the Parts Locker to swap fittings."
                )
        );
    }

    private ItemStack createPartsLockerItem(Player player) {
        int ownedStacks = ownedDrillPartCount(player);
        int ownedTypes = ownedDrillParts(player, true).size() + ownedDrillParts(player, false).size();
        ItemStack item = infoItem(
                Material.CHEST,
                ChatColor.BLUE + "Drill Parts Locker",
                List.of(
                        ChatColor.GRAY + "Owned Part Stacks: " + ChatColor.YELLOW + formatInt(ownedStacks),
                        ChatColor.GRAY + "Unique Parts: " + ChatColor.YELLOW + ownedTypes,
                        ChatColor.GRAY + "Installed parts stay on the drill,",
                        ChatColor.GRAY + "while spare parts stay here.",
                        "",
                        ChatColor.YELLOW + "Click to open."
                )
        );
        return highlight(item, ownedStacks > 0);
    }

    private ItemStack createInstalledPartStatusItem(Player player, boolean enginePart) {
        ItemStack drill = heldDrill(player);
        String label = enginePart ? "Installed Engine" : "Installed Tank";
        String fallback = enginePart ? ChatColor.RED + "Unfitted" : ChatColor.GRAY + "Stock Reservoir";
        Material fallbackMaterial = enginePart ? Material.PISTON : Material.BUCKET;
        if (drill == null) {
            return infoItem(
                    fallbackMaterial,
                    ChatColor.AQUA + label,
                    List.of(
                            ChatColor.GRAY + "Hold a drill in your main hand",
                            ChatColor.GRAY + "to preview and swap this slot."
                    )
            );
        }

        String installedId = drillPartId(drill, enginePart);
        ItemStack item = installedId == null ? new ItemStack(fallbackMaterial) : itemService.createItemByKey(installedId);
        if (item == null || item.getType().isAir()) {
            item = new ItemStack(fallbackMaterial);
        } else {
            item = item.clone();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + label + ": " + partDisplayName(installedId, fallback));
            lore.add(ChatColor.DARK_GRAY + "Open the Parts Locker to swap fittings.");
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPartsInventorySummaryItem(Player player) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "The locker only shows parts you");
        lore.add(ChatColor.GRAY + "currently have in your inventory.");
        lore.add("");

        List<OwnedDrillPart> ownedParts = new ArrayList<>();
        ownedParts.addAll(ownedDrillParts(player, true));
        ownedParts.addAll(ownedDrillParts(player, false));
        if (ownedParts.isEmpty()) {
            lore.add(ChatColor.DARK_GRAY + "No spare drill parts found.");
        } else {
            int shown = 0;
            for (OwnedDrillPart owned : ownedParts) {
                if (shown >= 4) {
                    lore.add(ChatColor.DARK_GRAY + "+" + (ownedParts.size() - shown) + " more part types");
                    break;
                }
                lore.add(ChatColor.GRAY + "- " + partDisplayName(owned.option().itemId(), ChatColor.AQUA + owned.option().itemId().toLowerCase(Locale.ROOT).replace('_', ' '))
                        + ChatColor.DARK_GRAY + " x" + ChatColor.YELLOW + formatInt(owned.amount()));
                shown++;
            }
        }
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to open the locker.");
        return infoItem(Material.HOPPER, ChatColor.GOLD + "Inventory Summary", lore);
    }

    private ItemStack createPartsGuideItem() {
        return infoItem(
                Material.WRITABLE_BOOK,
                ChatColor.AQUA + "Parts Locker Guide",
                List.of(
                        ChatColor.GRAY + "1. Hold the drill you want to tune.",
                        ChatColor.GRAY + "2. Open the locker from the Bay.",
                        ChatColor.GRAY + "3. Click any owned part to install it.",
                        ChatColor.GRAY + "4. Forge stronger parts from Projects."
                )
        );
    }

    private ItemStack createPartsForgeHintItem() {
        return infoItem(
                Material.SMITHING_TABLE,
                ChatColor.LIGHT_PURPLE + "Need More Parts?",
                List.of(
                        ChatColor.GRAY + "The Project Queue crafts stronger",
                        ChatColor.GRAY + "engines and tanks for this locker.",
                        "",
                        ChatColor.DARK_GRAY + "Use Projects when the locker is empty."
                )
        );
    }

    private ItemStack createBayGuideItem() {
        return infoItem(
                Material.WRITABLE_BOOK,
                ChatColor.AQUA + "System Protocol",
                List.of(
                        ChatColor.GRAY + "1. Equip the target drill for calibration.",
                        ChatColor.GRAY + "2. Access Fueling to replenish energy.",
                        ChatColor.GRAY + "3. Access Engineering for module fitting.",
                        ChatColor.GRAY + "4. Initiate Synthesis for part production.",
                        "",
                        ChatColor.DARK_GRAY + "Monitor telemetry to ensure optimal",
                        ChatColor.DARK_GRAY + "thermal and mechanical throughput."
                )
        );
    }

    private ItemStack createQueueSummaryItem(Player player) {
        DrillForgeManager.ForgeSnapshot snapshot = forgeManager.snapshot(player);
        ItemStack item = infoItem(
                Material.CLOCK,
                ChatColor.AQUA + "Queue Summary",
                List.of(
                        ChatColor.GRAY + "Active Chambers: " + ChatColor.YELLOW + snapshot.activeProjects() + ChatColor.GRAY + "/3",
                        ChatColor.GRAY + "Ready Claims: " + ChatColor.GREEN + snapshot.readyProjects(),
                        nextReadyLine(player),
                        nextHeatMilestoneLine(snapshot.heat()),
                        "",
                        ChatColor.YELLOW + "Click to open the project queue."
                )
        );
        return highlight(item, snapshot.readyProjects() > 0);
    }

    private ItemStack createProjectQueuePreviewItem(Player player) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "A quick look at your live forge queue.");
        lore.add("");

        List<DrillForgeManager.ActiveProjectView> queue = forgeManager.activeProjects(player);
        if (queue.isEmpty()) {
            lore.add(ChatColor.DARK_GRAY + "No active projects.");
        } else {
            int displayed = 0;
            for (DrillForgeManager.ActiveProjectView project : queue) {
                if (displayed >= 3) {
                    break;
                }
                lore.add(ChatColor.GRAY + "- " + project.type().displayName() + ChatColor.DARK_GRAY + " : "
                        + (project.ready()
                        ? ChatColor.GREEN + "Ready"
                        : ChatColor.AQUA + forgeManager.formatDuration(project.readyAt() - System.currentTimeMillis())));
                displayed++;
            }
        }
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Claiming projects is how you build heat.");
        return infoItem(Material.HOPPER, ChatColor.GOLD + "Queue Preview", lore);
    }

    private ItemStack createForgeLoopItem() {
        return infoItem(
                Material.REPEATER,
                ChatColor.YELLOW + "Operational Loop",
                List.of(
                        ChatColor.GRAY + "1. Maintenance: Keep the drill fueled and optimized.",
                        ChatColor.GRAY + "2. Synthesis: Start projects to create advanced components.",
                        ChatColor.GRAY + "3. Extraction: Claim finished work to generate Forge Heat.",
                        ChatColor.GRAY + "4. Ignition: Spend heat to trigger drill Overdrive.",
                        "",
                        ChatColor.DARK_GRAY + "Mastering this loop ensures peak mining performance."
                )
        );
    }

    private ItemStack createHeatMeterItem(Player player) {
        DrillForgeManager.ForgeSnapshot snapshot = forgeManager.snapshot(player);
        int needed = Math.max(0, OVERDRIVE_HEAT_COST - snapshot.heat());
        return infoItem(
                Material.RESPAWN_ANCHOR,
                ChatColor.RED + "Thermal Core Status",
                List.of(
                        ChatColor.GRAY + "Operational Heat: " + ChatColor.YELLOW + snapshot.heat() + ChatColor.DARK_GRAY + " / " + ChatColor.GOLD + DrillForgeManager.MAX_HEAT,
                        meterBar(snapshot.heat(), DrillForgeManager.MAX_HEAT, 12, ChatColor.GOLD, ChatColor.DARK_GRAY),
                        ChatColor.GRAY + "Project Efficiency: " + ChatColor.GREEN + "+" + snapshot.speedBonusPercent() + "%",
                        nextHeatMilestoneLine(snapshot.heat()),
                        "",
                        ChatColor.GRAY + "System Readiness: " + (needed <= 0 ? ChatColor.GREEN + "OPTIMAL" : ChatColor.RED + "CALIBRATING"),
                        ChatColor.GRAY + "Overdrive Yield: " + overdrivePreviewDisplay(snapshot.heat())
                                + ChatColor.DARK_GRAY + " (" + ChatColor.AQUA + forgeManager.formatDuration(expectedOverdriveDurationMillis(snapshot.heat())) + ChatColor.DARK_GRAY + ")",
                        needed > 0 ? ChatColor.DARK_GRAY + "Inhibit: " + ChatColor.YELLOW + needed + ChatColor.DARK_GRAY + " heat units required for ignition." : ""
                )
        );
    }

    private ItemStack createProjectCatalogItem() {
        return infoItem(
                Material.BOOK,
                ChatColor.AQUA + "Operational Blueprints",
                List.of(
                        ChatColor.GRAY + "Utility: Process fuel, navigation, and enhancements.",
                        ChatColor.GRAY + "Engineering: Synthesize engines, tanks, and structural parts.",
                        "",
                        ChatColor.DARK_GRAY + "Thermal energy dissipation optimize future cycles.",
                        ChatColor.DARK_GRAY + "Mastering the forge is key to deep-mine survival."
                )
        );
    }

    private ItemStack createQueueCapacityItem(Player player) {
        DrillForgeManager.ForgeSnapshot snapshot = forgeManager.snapshot(player);
        int free = Math.max(0, 3 - snapshot.activeProjects());
        return infoItem(
                Material.CHEST_MINECART,
                ChatColor.AQUA + "Processing Capacity",
                List.of(
                        ChatColor.GRAY + "Operational Chambers: " + ChatColor.YELLOW + snapshot.activeProjects() + ChatColor.DARK_GRAY + " / " + ChatColor.YELLOW + "3",
                        ChatColor.GRAY + "Available Cycles: " + (free > 0 ? ChatColor.GREEN + String.valueOf(free) : ChatColor.RED + "NONE"),
                        ChatColor.GRAY + "Synchronized Outputs: " + ChatColor.GREEN + snapshot.readyProjects(),
                        "",
                        free > 0
                                ? ChatColor.YELLOW + "System ready for additional synthesis cycles."
                                : ChatColor.RED + "Wait for cycle completion or extract outputs."
                )
        );
    }

    private ItemStack createForgePurseItem(Player player) {
        double purse = economyService.purse(player);
        return infoItem(
                Material.GOLD_INGOT,
                ChatColor.GOLD + "Available Credits",
                List.of(
                        ChatColor.GRAY + "Operational Balance: " + ChatColor.GOLD + forgeManager.formatCoins(purse) + " coins",
                        "",
                        ChatColor.DARK_GRAY + "Project costs are processed upon",
                        ChatColor.DARK_GRAY + "cycle initiation.",
                        "",
                        ChatColor.DARK_GRAY + "Maintain a credit reserve for",
                        ChatColor.DARK_GRAY + "high-yield component forging."
                )
        );
    }

    private ItemStack createProjectTipsItem() {
        return infoItem(
                Material.COMPASS,
                ChatColor.YELLOW + "Protocol Guidance",
                List.of(
                        ChatColor.GRAY + "Prompt project extraction ensures heat accumulation.",
                        ChatColor.GRAY + "Use quick cycles to build thermal momentum.",
                        ChatColor.GRAY + "Save high heat for intensive extraction sessions."
                )
        );
    }

    private ItemStack createProjectItem(Player player, DrillForgeManager.ForgeProjectType type) {
        ItemStack icon = type.createOutput(itemService);
        if (icon == null || icon.getType().isAir()) {
            icon = new ItemStack(Material.SMITHING_TABLE);
        }
        ItemMeta meta = icon.getItemMeta();
        ProjectAvailability availability = projectAvailability(player, type);
        DrillForgeManager.ForgeSnapshot snapshot = forgeManager.snapshot(player);
        int forecastHeat = Math.min(DrillForgeManager.MAX_HEAT, snapshot.heat() + type.heatGain());
        if (meta != null) {
            meta.setDisplayName(type.displayName());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + type.description());
            lore.add("");
            lore.add(projectCategoryColor(type) + projectCategory(type));
            lore.add(ChatColor.GRAY + "Forge Time: " + ChatColor.AQUA + forgeManager.formatDuration(forgeManager.projectedDurationMillis(player, type)));
            lore.add(ChatColor.GRAY + "Coin Cost: " + ChatColor.GOLD + forgeManager.formatCoins(type.coinCost()));
            lore.add(ChatColor.GRAY + "Heat Gain: " + ChatColor.YELLOW + "+" + type.heatGain());
            lore.add(ChatColor.GRAY + "Heat After Claim: " + ChatColor.GOLD + forecastHeat + ChatColor.GRAY + "/" + ChatColor.GOLD + DrillForgeManager.MAX_HEAT);
            lore.add(ChatColor.GRAY + "Forge Speed After Claim: " + ChatColor.GREEN + "+" + DrillForgeManager.speedBonusPercentFor(forecastHeat) + "%");
            lore.add(ChatColor.GRAY + "Overdrive After Claim: " + overdrivePreviewDisplay(forecastHeat));
            lore.add("");
            lore.add(ChatColor.GRAY + "Ingredients:");
            for (DrillForgeManager.ForgeIngredient ingredient : type.ingredients()) {
                lore.add(formatIngredientLine(player, ingredient));
            }
            lore.add("");
            lore.add(projectAvailabilityLine(availability));
            if (availability == ProjectAvailability.READY) {
                lore.add(ChatColor.YELLOW + "Click to start forging.");
            }
            meta.setLore(lore);
            icon.setItemMeta(meta);
        }
        return highlight(icon, availability == ProjectAvailability.READY);
    }

    private ItemStack createQueueItem(Player player, List<DrillForgeManager.ActiveProjectView> queue, int index) {
        if (queue == null || index >= queue.size()) {
            return infoItem(
                    Material.GRAY_STAINED_GLASS_PANE,
                    ChatColor.DARK_GRAY + "Empty Chamber",
                    List.of(
                            ChatColor.GRAY + "Start a forge project to fill",
                            ChatColor.GRAY + "this claim chamber."
                    )
            );
        }
        DrillForgeManager.ActiveProjectView project = queue.get(index);
        ItemStack icon = project.type().createOutput(itemService);
        if (icon == null || icon.getType().isAir()) {
            icon = new ItemStack(Material.CLOCK);
        }
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            long now = System.currentTimeMillis();
            long total = Math.max(1L, project.readyAt() - project.startedAt());
            long elapsed = Math.max(0L, Math.min(total, now - project.startedAt()));
            DrillForgeManager.ForgeSnapshot snapshot = forgeManager.snapshot(player);
            int forecastHeat = Math.min(DrillForgeManager.MAX_HEAT, snapshot.heat() + project.type().heatGain());
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + project.type().description());
            lore.add("");
            lore.add(ChatColor.GRAY + "Chamber: " + ChatColor.YELLOW + (index + 1));
            lore.add(ChatColor.GRAY + "Heat On Claim: " + ChatColor.YELLOW + "+" + project.type().heatGain());
            lore.add(ChatColor.GRAY + "Heat After Claim: " + ChatColor.GOLD + forecastHeat + ChatColor.GRAY + "/" + ChatColor.GOLD + DrillForgeManager.MAX_HEAT);
            lore.add(ChatColor.GRAY + "Overdrive After Claim: " + overdrivePreviewDisplay(forecastHeat));
            lore.add(project.ready()
                    ? ChatColor.GREEN + "Ready to claim."
                    : ChatColor.GRAY + "Ready in: " + ChatColor.AQUA + forgeManager.formatDuration(project.readyAt() - now));
            lore.add(project.ready()
                    ? meterBar(1, 1, 12, ChatColor.GREEN, ChatColor.DARK_GRAY)
                    : meterBar(elapsed, total, 12, ChatColor.AQUA, ChatColor.DARK_GRAY));
            lore.add("");
            lore.add(project.ready()
                    ? ChatColor.YELLOW + "Click to collect output."
                    : ChatColor.DARK_GRAY + "This chamber is still forging.");
            meta.setDisplayName(project.type().displayName());
            meta.setLore(lore);
            icon.setItemMeta(meta);
        }
        return highlight(icon, project.ready());
    }

    private ItemStack createOverdriveStatusItem(Player player) {
        DrillForgeManager.ForgeSnapshot snapshot = forgeManager.snapshot(player);
        boolean active = snapshot.overdriveRemainingMillis() > 0L;
        int displayedTier = displayedOverdriveTier(player);
        return infoItem(
                Material.RESPAWN_ANCHOR,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Overdrive Status",
                List.of(
                        ChatColor.GRAY + "Status: " + (active
                                ? ChatColor.AQUA + forgeManager.formatDuration(snapshot.overdriveRemainingMillis())
                                : ChatColor.RED + "Offline"),
                        ChatColor.GRAY + (active ? "Current Level: " : "Ignite Now: ") + (active ? overdriveTierDisplay(displayedTier) : overdrivePreviewDisplay(snapshot.heat())),
                        ChatColor.GRAY + "Ignition Cost: " + ChatColor.GOLD + OVERDRIVE_HEAT_COST + ChatColor.GRAY + " heat",
                        ChatColor.GRAY + "Coin Cost: " + ChatColor.GOLD + forgeManager.formatCoins(OVERDRIVE_COIN_COST),
                        active
                                ? ChatColor.GRAY + "Heat Spent: " + ChatColor.GOLD + OVERDRIVE_HEAT_COST + ChatColor.GRAY + " on activation"
                                : ChatColor.GRAY + "If ignited now: " + ChatColor.AQUA + forgeManager.formatDuration(expectedOverdriveDurationMillis(snapshot.heat()))
                )
        );
    }

    private ItemStack createOverdriveIgnitionItem(Player player) {
        DrillForgeManager.ForgeSnapshot snapshot = forgeManager.snapshot(player);
        boolean active = forgeManager.isOverdriveActive(player);
        int previewTier = DrillForgeManager.overdriveTierForHeat(snapshot.heat());
        int fuelReduction = DrillForgeManager.overdriveFuelReduction(previewTier);
        long cooldownReduction = DrillForgeManager.overdriveCooldownReductionMillis(previewTier);
        int durationBonus = DrillForgeManager.overdriveAbilityDurationBonusTicks(previewTier) / 20;
        int amplifierBonus = DrillForgeManager.overdriveAbilityAmplifierBonus(previewTier);
        ItemStack item = infoItem(
                active ? Material.SOUL_LANTERN : Material.LIGHTNING_ROD,
                active ? ChatColor.AQUA + "Overdrive Running" : ChatColor.YELLOW + "Ignite Overdrive",
                active
                        ? List.of(
                        ChatColor.GRAY + "Your forge is already running hot.",
                        ChatColor.GRAY + "Remaining: " + ChatColor.AQUA + forgeManager.formatDuration(snapshot.overdriveRemainingMillis()),
                        ChatColor.GRAY + "Level: " + overdriveTierDisplay(forgeManager.activeOverdriveTier(player))
                )
                        : List.of(
                        ChatColor.GRAY + "Heat Bank: " + (snapshot.heat() >= OVERDRIVE_HEAT_COST
                                ? ChatColor.GREEN.toString() + snapshot.heat() + ChatColor.GRAY + "/" + ChatColor.GREEN + OVERDRIVE_HEAT_COST
                                : ChatColor.RED.toString() + snapshot.heat() + ChatColor.GRAY + "/" + ChatColor.RED + OVERDRIVE_HEAT_COST),
                        ChatColor.GRAY + "Coins Needed: " + (economyService.has(player, OVERDRIVE_COIN_COST)
                                ? ChatColor.GREEN + forgeManager.formatCoins(OVERDRIVE_COIN_COST)
                                : ChatColor.RED + forgeManager.formatCoins(OVERDRIVE_COIN_COST)),
                        ChatColor.GRAY + "Overdrive Level: " + overdrivePreviewDisplay(snapshot.heat()),
                        ChatColor.GRAY + "Duration: " + ChatColor.AQUA + forgeManager.formatDuration(expectedOverdriveDurationMillis(snapshot.heat())),
                        ChatColor.GRAY + "Fuel Burn: " + ChatColor.GREEN + "-" + fuelReduction + ChatColor.GRAY + " per block",
                        ChatColor.GRAY + "Cooldown: " + ChatColor.GREEN + "-" + (cooldownReduction / 1000L) + "s"
                                + ChatColor.DARK_GRAY + " | " + ChatColor.GRAY + "Ability: " + (amplifierBonus > 0
                                ? ChatColor.GREEN + "+" + amplifierBonus + " tier"
                                : ChatColor.YELLOW + "+" + durationBonus + "s duration"),
                        "",
                        snapshot.heat() >= OVERDRIVE_HEAT_COST
                                ? ChatColor.YELLOW + "Click to ignite."
                                : ChatColor.RED + "Claim more projects before igniting."
                )
        );
        return highlight(item, active || (snapshot.heat() >= OVERDRIVE_HEAT_COST && economyService.has(player, OVERDRIVE_COIN_COST)));
    }

    private ItemStack createOverdrivePerformanceItem(Player player) {
        boolean active = forgeManager.isOverdriveActive(player);
        int tier = displayedOverdriveTier(player);
        int sampleCooldown = (int) Math.max(0L, Math.max(12_000L, 40_000L - DrillForgeManager.overdriveCooldownReductionMillis(tier)) / 1000L);
        int sampleDuration = (160 + DrillForgeManager.overdriveAbilityDurationBonusTicks(tier)) / 20;
        int amplifierBonus = DrillForgeManager.overdriveAbilityAmplifierBonus(tier);
        return infoItem(
                Material.NETHER_STAR,
                ChatColor.LIGHT_PURPLE + "Overdrive Preview",
                List.of(
                        ChatColor.GRAY + "Level: " + overdriveTierDisplay(tier),
                        ChatColor.GRAY + "Example Cooldown: " + ChatColor.AQUA + sampleCooldown + "s",
                        ChatColor.GRAY + "Example Duration: " + ChatColor.AQUA + sampleDuration + "s",
                        ChatColor.GRAY + "Ability Power: " + (amplifierBonus > 0
                                ? ChatColor.GREEN + "+" + amplifierBonus + " amplifier tier"
                                : ChatColor.YELLOW + "Duration-focused at this charge"),
                        "",
                        ChatColor.DARK_GRAY + (active
                                ? "These are your live Overdrive numbers."
                                : "These are the numbers if you ignite right now.")
                )
        );
    }

    private ItemStack createOverdriveFuelSavingsItem(Player player) {
        int tier = displayedOverdriveTier(player);
        int sampleBase = 10;
        int sampleAdjusted = DrillForgeManager.adjustedFuelCostPerBlock(sampleBase, tier, true);
        return infoItem(
                Material.LAVA_BUCKET,
                ChatColor.GOLD + "Fuel Savings",
                List.of(
                        ChatColor.GRAY + "Level: " + overdriveTierDisplay(tier),
                        ChatColor.GRAY + "Sample Burn: " + ChatColor.YELLOW + sampleBase + ChatColor.GRAY + " -> "
                                + ChatColor.GREEN + sampleAdjusted + ChatColor.GRAY + " fuel per block",
                        ChatColor.GRAY + "Reduction: " + ChatColor.GREEN + "-" + DrillForgeManager.overdriveFuelReduction(tier) + ChatColor.GRAY + " fuel per block",
                        ChatColor.GRAY + "Burn never drops below 4."
                )
        );
    }

    private ItemStack createOverdriveAbilityTuningItem(Player player) {
        int tier = displayedOverdriveTier(player);
        return infoItem(
                Material.BEACON,
                ChatColor.AQUA + "Ability Bonus",
                List.of(
                        ChatColor.GRAY + "Level: " + overdriveTierDisplay(tier),
                        ChatColor.GRAY + "Cooldown: " + ChatColor.GREEN + "-" + (DrillForgeManager.overdriveCooldownReductionMillis(tier) / 1000L) + "s",
                        ChatColor.GRAY + "Duration: " + ChatColor.GREEN + "+" + (DrillForgeManager.overdriveAbilityDurationBonusTicks(tier) / 20) + "s",
                        ChatColor.GRAY + "Power: " + (DrillForgeManager.overdriveAbilityAmplifierBonus(tier) > 0
                                ? ChatColor.GREEN + "+" + DrillForgeManager.overdriveAbilityAmplifierBonus(tier) + " amplifier tier"
                                : ChatColor.YELLOW + "No amplifier bonus yet"),
                        "",
                        ChatColor.DARK_GRAY + "More stored heat gives a higher Overdrive level."
                )
        );
    }

    private ItemStack createOverdriveActivationGuideItem(Player player) {
        DrillForgeManager.ForgeSnapshot snapshot = forgeManager.snapshot(player);
        int neededHeat = Math.max(0, OVERDRIVE_HEAT_COST - snapshot.heat());
        return infoItem(
                Material.WRITABLE_BOOK,
                ChatColor.YELLOW + "How Overdrive Works",
                List.of(
                        ChatColor.GRAY + "1. Claim finished projects to build heat.",
                        ChatColor.GRAY + "2. Keep " + ChatColor.GOLD + OVERDRIVE_HEAT_COST + ChatColor.GRAY + " heat banked.",
                        ChatColor.GRAY + "3. Pay " + ChatColor.GOLD + forgeManager.formatCoins(OVERDRIVE_COIN_COST) + ChatColor.GRAY + " coins to ignite.",
                        nextHeatMilestoneLine(snapshot.heat()),
                        neededHeat > 0
                                ? ChatColor.RED + "You still need " + neededHeat + " more heat."
                                : ChatColor.AQUA + "You can ignite " + overdrivePreviewDisplay(snapshot.heat()) + ChatColor.AQUA + " right now."
                )
        );
    }

    private ItemStack createOverdriveEffectMatrixItem() {
        return infoItem(
                Material.ENCHANTED_BOOK,
                ChatColor.LIGHT_PURPLE + "Overdrive Levels",
                List.of(
                        ChatColor.GRAY + "More stored heat gives a higher",
                        ChatColor.GRAY + "Overdrive level when you ignite.",
                        "",
                        ChatColor.DARK_GRAY + "Low heat is a short boost. Full heat is the strongest version."
                )
        );
    }

    private ItemStack createOverdriveTeaserItem(Player player) {
        DrillForgeManager.ForgeSnapshot snapshot = forgeManager.snapshot(player);
        int needed = Math.max(0, OVERDRIVE_HEAT_COST - snapshot.heat());
        int previewTier = DrillForgeManager.overdriveTierForHeat(snapshot.heat());
        ItemStack item = infoItem(
                Material.LIGHTNING_ROD,
                ChatColor.AQUA + "Overdrive Ready?",
                List.of(
                        snapshot.overdriveRemainingMillis() > 0L
                                ? ChatColor.GRAY + "Live for " + ChatColor.AQUA + forgeManager.formatDuration(snapshot.overdriveRemainingMillis())
                                : ChatColor.GRAY + "Offline",
                        ChatColor.GRAY + "Level: " + (snapshot.overdriveRemainingMillis() > 0L
                                ? overdriveTierDisplay(forgeManager.activeOverdriveTier(player))
                                : overdrivePreviewDisplay(snapshot.heat())),
                        needed <= 0
                                ? ChatColor.GREEN + "Heat ready for ignition."
                                : ChatColor.GRAY + "Need " + ChatColor.YELLOW + needed + ChatColor.GRAY + " more heat.",
                        ChatColor.GRAY + "Preview Duration: " + ChatColor.AQUA + forgeManager.formatDuration(expectedOverdriveDurationMillis(snapshot.heat())),
                        ChatColor.DARK_GRAY + "Open the chamber to ignite."
                )
        );
        return highlight(item, snapshot.overdriveRemainingMillis() > 0L || needed <= 0);
    }

    private ItemStack createFuelOption(Material material, String name, int fuelAmount, double coinCost) {
        return infoItem(
                material,
                ChatColor.GREEN + name,
                List.of(
                        ChatColor.GRAY + "Fuel Added: " + ChatColor.YELLOW + forgeManager.formatInt(fuelAmount),
                        ChatColor.GRAY + "Coin Cost: " + ChatColor.GOLD + forgeManager.formatCoins(coinCost),
                        "",
                        ChatColor.YELLOW + "Click to apply."
                )
        );
    }

    private ItemStack createFuelOption(ItemStack baseItem, int fuelAmount) {
        ItemStack item = baseItem == null ? new ItemStack(Material.BLAZE_POWDER) : baseItem.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Fuel Added: " + ChatColor.YELLOW + forgeManager.formatInt(fuelAmount));
            lore.add(ChatColor.GRAY + "Consumes one matching canister from inventory.");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to consume.");
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createFullRefuelItem() {
        return infoItem(
                Material.LAVA_BUCKET,
                ChatColor.RED + "Instant Full Refuel",
                List.of(
                        ChatColor.GRAY + "Completely fills the held drill.",
                        ChatColor.GRAY + "Cost: " + ChatColor.GOLD + "2.5 coins per missing fuel",
                        "",
                        ChatColor.YELLOW + "Click to refill."
                )
        );
    }

    private ItemStack createUpgradeOption(MiningItemType type) {
        ItemStack item = itemService.createMiningItem(type);
        if (item == null) {
            item = new ItemStack(Material.SMITHING_TABLE);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Requires the part in your inventory.");
            lore.add(ChatColor.YELLOW + "Click to install on the held drill.");
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void populateOwnedPartSlots(Inventory inventory, Player player, boolean enginePart) {
        List<Integer> slots = enginePart ? ENGINE_PART_SLOTS : TANK_PART_SLOTS;
        List<OwnedDrillPart> ownedParts = ownedDrillParts(player, enginePart);
        if (ownedParts.isEmpty()) {
            inventory.setItem(enginePart ? 13 : 31, createPartsEmptyItem(enginePart));
            return;
        }
        for (int index = 0; index < Math.min(slots.size(), ownedParts.size()); index++) {
            inventory.setItem(slots.get(index), createOwnedPartItem(player, ownedParts.get(index)));
        }
    }

    private ItemStack createOwnedPartItem(Player player, OwnedDrillPart ownedPart) {
        ItemStack item = itemService.createMiningItem(ownedPart.option().type());
        if (item == null || item.getType().isAir()) {
            item = new ItemStack(ownedPart.option().enginePart() ? Material.PISTON : Material.BUCKET);
        } else {
            item = item.clone();
        }
        item.setAmount(Math.min(64, Math.max(1, ownedPart.amount())));

        ItemStack drill = heldDrill(player);
        String installedId = drillPartId(drill, ownedPart.option().enginePart());
        boolean installed = ownedPart.option().itemId().equalsIgnoreCase(installedId);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
            lore.add("");
            lore.add(ChatColor.GRAY + "Owned: " + ChatColor.YELLOW + formatInt(ownedPart.amount()));
            if (drill == null) {
                lore.add(ChatColor.RED + "Hold a drill in your main hand to install.");
            } else if (installed) {
                lore.add(ChatColor.AQUA + "Already installed on the held drill.");
            } else {
                lore.add(ChatColor.YELLOW + "Click to install on the held drill.");
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return highlight(item, drill != null && !installed);
    }

    private ItemStack createPartsEmptyItem(boolean enginePart) {
        String kind = enginePart ? "engine" : "tank";
        return infoItem(
                Material.GRAY_STAINED_GLASS_PANE,
                ChatColor.DARK_GRAY + "No " + (enginePart ? "Engine Parts" : "Tank Parts"),
                List.of(
                        ChatColor.GRAY + "You do not currently have any " + kind,
                        ChatColor.GRAY + "parts in your inventory.",
                        "",
                        ChatColor.DARK_GRAY + "Forge more from the Project Queue."
                )
        );
    }

    private ItemStack navItem(Material material, String name, String line) {
        return infoItem(
                material,
                name,
                List.of(
                        ChatColor.GRAY + line,
                        "",
                        ChatColor.YELLOW + "Click to open."
                )
        );
    }

    private ItemStack sectionLabel(Material material, String name, String line) {
        return infoItem(material, name, List.of(ChatColor.GRAY + line));
    }

    private ItemStack infoItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack spacer(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack highlight(ItemStack item, boolean enabled) {
        if (!enabled || item == null || !item.hasItemMeta()) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.addEnchant(Enchantment.LURE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private ProjectAvailability projectAvailability(Player player, DrillForgeManager.ForgeProjectType type) {
        if (player == null || type == null) {
            return ProjectAvailability.NEEDS_ITEMS;
        }
        if (forgeManager.snapshot(player).activeProjects() >= 3) {
            return ProjectAvailability.QUEUE_FULL;
        }
        if (!hasIngredients(player, type.ingredients())) {
            return ProjectAvailability.NEEDS_ITEMS;
        }
        if (!economyService.has(player, type.coinCost())) {
            return ProjectAvailability.NEEDS_COINS;
        }
        return ProjectAvailability.READY;
    }

    private String projectAvailabilityLine(ProjectAvailability availability) {
        return switch (availability) {
            case READY -> ChatColor.GREEN + "Status: Ready to forge";
            case QUEUE_FULL -> ChatColor.RED + "Status: Queue full";
            case NEEDS_COINS -> ChatColor.RED + "Status: Not enough coins";
            case NEEDS_ITEMS -> ChatColor.RED + "Status: Missing materials";
        };
    }

    private String projectCategory(DrillForgeManager.ForgeProjectType type) {
        return switch (type) {
            case VOLTA_INFUSION, OIL_BARREL_COMPRESSION, PROSPECTOR_COMPASS_MAPPING, MINING_SCROLL_COMPILATION, SPEED_BOOST_BLEND, HEAT_CATALYST_SYNTHESIS, FORTUNE_COOKIE_BAKING, DRILL_WARRANTY_FORGING, PERSONAL_COMPACTOR_5000_FORGING, PERSONAL_COMPACTOR_6000_FORGING, PERSONAL_COMPACTOR_7000_FORGING -> "Utility Project";
            case STABILITY_ANCHOR_FORGING, MEDIUM_TANK_EXPANSION, LARGE_TANK_REINFORCEMENT, REFINED_TITANIUM_FORGING, TITANIUM_BLOCK_FORGING -> "Core Component";
            case MITHRIL_ENGINE_RESONANCE, TITANIUM_ENGINE_REFINEMENT, GEMSTONE_ENGINE_ATTUNEMENT, DIVAN_ENGINE_ASCENSION -> "Engine Upgrade";
            case TITANIUM_DRILL_FORGING, GEMSTONE_DRILL_FORGING, GUARDIAN_HELM_FORGING, GUARDIAN_CHESTPLATE_FORGING, GUARDIAN_LEGGINGS_FORGING, GUARDIAN_BOOTS_FORGING -> "Gear Forging";
        };
    }

    private ChatColor projectCategoryColor(DrillForgeManager.ForgeProjectType type) {
        return switch (type) {
            case VOLTA_INFUSION, OIL_BARREL_COMPRESSION, PROSPECTOR_COMPASS_MAPPING, MINING_SCROLL_COMPILATION, SPEED_BOOST_BLEND, HEAT_CATALYST_SYNTHESIS, FORTUNE_COOKIE_BAKING, DRILL_WARRANTY_FORGING, PERSONAL_COMPACTOR_5000_FORGING, PERSONAL_COMPACTOR_6000_FORGING, PERSONAL_COMPACTOR_7000_FORGING -> ChatColor.AQUA;
            case STABILITY_ANCHOR_FORGING, MEDIUM_TANK_EXPANSION, LARGE_TANK_REINFORCEMENT, REFINED_TITANIUM_FORGING, TITANIUM_BLOCK_FORGING -> ChatColor.GOLD;
            case MITHRIL_ENGINE_RESONANCE, TITANIUM_ENGINE_REFINEMENT, GEMSTONE_ENGINE_ATTUNEMENT, DIVAN_ENGINE_ASCENSION, TITANIUM_DRILL_FORGING, GEMSTONE_DRILL_FORGING, GUARDIAN_HELM_FORGING, GUARDIAN_CHESTPLATE_FORGING, GUARDIAN_LEGGINGS_FORGING, GUARDIAN_BOOTS_FORGING -> ChatColor.LIGHT_PURPLE;
        };
    }

    private String formatIngredientLine(Player player, DrillForgeManager.ForgeIngredient ingredient) {
        int available = countIngredient(player, ingredient);
        int needed = ingredient.amount();
        ChatColor amountColor = available >= needed ? ChatColor.GREEN : ChatColor.RED;
        return ChatColor.DARK_GRAY + "- " + ChatColor.GRAY + ingredientName(ingredient)
                + ChatColor.DARK_GRAY + " (" + amountColor + available + ChatColor.DARK_GRAY + "/" + ChatColor.YELLOW + needed + ChatColor.DARK_GRAY + ")";
    }

    private String ingredientName(DrillForgeManager.ForgeIngredient ingredient) {
        if (ingredient == null) {
            return "Unknown";
        }
        if (ingredient.customItemId() != null) {
            ItemStack item = itemService.createItemByKey(ingredient.customItemId());
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                return item.getItemMeta().getDisplayName();
            }
            return ingredient.customItemId().toLowerCase(Locale.ROOT).replace('_', ' ');
        }
        return ingredient.material().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private boolean hasIngredients(Player player, List<DrillForgeManager.ForgeIngredient> ingredients) {
        if (player == null || ingredients == null) {
            return false;
        }
        for (DrillForgeManager.ForgeIngredient ingredient : ingredients) {
            if (countIngredient(player, ingredient) < ingredient.amount()) {
                return false;
            }
        }
        return true;
    }

    private int countIngredient(Player player, DrillForgeManager.ForgeIngredient ingredient) {
        if (player == null || ingredient == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            if (ingredient.customItemId() != null) {
                String itemId = itemService.itemId(stack);
                if (ingredient.customItemId().equalsIgnoreCase(itemId)) {
                    total += Math.max(1, stack.getAmount());
                }
                continue;
            }
            if (ingredient.material() == stack.getType() && itemService.itemId(stack) == null) {
                total += Math.max(1, stack.getAmount());
            }
        }
        return total;
    }

    private List<OwnedDrillPart> ownedDrillParts(Player player, boolean enginePart) {
        List<OwnedDrillPart> ownedParts = new ArrayList<>();
        for (DrillPartOption option : DRILL_PART_OPTIONS) {
            if (option.enginePart() != enginePart) {
                continue;
            }
            int amount = countCustomItem(player, option.itemId());
            if (amount > 0) {
                ownedParts.add(new OwnedDrillPart(option, amount));
            }
        }
        return ownedParts;
    }

    private int ownedDrillPartCount(Player player) {
        int total = 0;
        for (DrillPartOption option : DRILL_PART_OPTIONS) {
            total += countCustomItem(player, option.itemId());
        }
        return total;
    }

    private OwnedDrillPart ownedPartForSlot(Player player, int slot) {
        int engineIndex = ENGINE_PART_SLOTS.indexOf(slot);
        if (engineIndex >= 0) {
            List<OwnedDrillPart> engineParts = ownedDrillParts(player, true);
            return engineIndex < engineParts.size() ? engineParts.get(engineIndex) : null;
        }

        int tankIndex = TANK_PART_SLOTS.indexOf(slot);
        if (tankIndex >= 0) {
            List<OwnedDrillPart> tankParts = ownedDrillParts(player, false);
            return tankIndex < tankParts.size() ? tankParts.get(tankIndex) : null;
        }

        return null;
    }

    private int countCustomItem(Player player, String itemId) {
        if (player == null || itemId == null || itemId.isBlank()) {
            return 0;
        }
        int total = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            if (itemId.equalsIgnoreCase(itemService.itemId(stack))) {
                total += Math.max(1, stack.getAmount());
            }
        }
        return total;
    }

    private String heatBar(int heat) {
        return meterBar(Math.max(0, heat), DrillForgeManager.MAX_HEAT, 12, ChatColor.GOLD, ChatColor.DARK_GRAY)
                + ChatColor.DARK_GRAY + " " + ChatColor.GOLD + Math.max(0, heat) + ChatColor.GRAY + "/" + DrillForgeManager.MAX_HEAT;
    }

    private String meterBar(long current, long max, int segments, ChatColor filled, ChatColor empty) {
        long safeMax = Math.max(1L, max);
        int safeSegments = Math.max(6, segments);
        double ratio = Math.max(0.0D, Math.min(1.0D, current / (double) safeMax));
        int filledBars = (int) Math.round(ratio * safeSegments);
        StringBuilder builder = new StringBuilder(ChatColor.DARK_GRAY.toString()).append('[');
        for (int index = 0; index < safeSegments; index++) {
            builder.append(index < filledBars ? filled : empty).append('|');
        }
        builder.append(ChatColor.DARK_GRAY).append(']');
        return builder.toString();
    }

    private ChatColor fuelPercentColor(int percent) {
        if (percent >= 75) {
            return ChatColor.GREEN;
        }
        if (percent >= 35) {
            return ChatColor.YELLOW;
        }
        return ChatColor.RED;
    }

    private String nextHeatMilestoneLine(int heat) {
        int nextMilestone = DrillForgeManager.nextHeatMilestone(heat);
        if (heat < OVERDRIVE_HEAT_COST) {
            return ChatColor.GRAY + "Overdrive Unlock: " + ChatColor.YELLOW + OVERDRIVE_HEAT_COST + ChatColor.GRAY
                    + " heat (" + ChatColor.YELLOW + (OVERDRIVE_HEAT_COST - Math.max(0, heat)) + ChatColor.GRAY + " more)";
        }
        if (nextMilestone < 0) {
            return ChatColor.AQUA + "Next Overdrive Upgrade: Max level reached";
        }
        return ChatColor.GRAY + "Next Overdrive Upgrade: " + nextHeatMilestoneDisplay(heat)
                + ChatColor.GRAY + " in " + ChatColor.YELLOW + (nextMilestone - Math.max(0, heat)) + ChatColor.GRAY + " heat";
    }

    private String nextHeatMilestoneDisplay(int heat) {
        int nextMilestone = DrillForgeManager.nextHeatMilestone(heat);
        if (nextMilestone < 0) {
            return ChatColor.AQUA + "Max Level";
        }
        return overdriveTierDisplay(DrillForgeManager.overdriveTierForHeat(nextMilestone))
                + ChatColor.GRAY + " at " + ChatColor.YELLOW + nextMilestone + ChatColor.GRAY + " heat";
    }

    private int displayedOverdriveTier(Player player) {
        int activeTier = forgeManager.activeOverdriveTier(player);
        if (activeTier >= 0) {
            return activeTier;
        }
        DrillForgeManager.ForgeSnapshot snapshot = forgeManager.snapshot(player);
        return DrillForgeManager.overdriveTierForHeat(snapshot.heat());
    }

    private String overdrivePreviewDisplay(int heat) {
        if (heat < OVERDRIVE_HEAT_COST) {
            return ChatColor.RED + "Locked";
        }
        return overdriveTierDisplay(DrillForgeManager.overdriveTierForHeat(heat));
    }

    private String overdriveTierDisplay(int tier) {
        return DrillForgeManager.overdriveTierColor(tier) + DrillForgeManager.overdriveTierName(tier);
    }

    private String nextReadyLine(Player player) {
        List<DrillForgeManager.ActiveProjectView> queue = forgeManager.activeProjects(player);
        if (queue.isEmpty()) {
            return ChatColor.GRAY + "Next Claim: " + ChatColor.DARK_GRAY + "No active projects";
        }
        DrillForgeManager.ActiveProjectView next = queue.get(0);
        return ChatColor.GRAY + "Next Claim: " + (next.ready()
                ? ChatColor.GREEN + "Ready now"
                : ChatColor.AQUA + forgeManager.formatDuration(next.readyAt() - System.currentTimeMillis()));
    }

    private long expectedOverdriveDurationMillis(int currentHeat) {
        return DrillForgeManager.overdriveDurationMillisForHeat(currentHeat);
    }

    private DrillGuiHooks.DrillSnapshot buildDrillHookSnapshot(ItemStack drill, Player player) {
        DrillStatProfile.Profile profile = drillProfile(drill);
        if (drill == null || profile == null) {
            return new DrillGuiHooks.DrillSnapshot(
                    false,
                    "",
                    "",
                    "",
                    "",
                    0,
                    0,
                    0,
                    0,
                    null,
                    0,
                    0L,
                    0,
                    0
            );
        }
        int fuel = drillFuel(drill);
        int maxFuel = drillFuelMax(drill);
        int activeFuelCost = player == null
                ? profile.fuelCostPerBlock()
                : adjustedFuelCostPerBlock(player, profile.fuelCostPerBlock());
        long activeCooldown = player == null
                ? profile.abilityCooldownMillis()
                : adjustedAbilityCooldownMillis(player, profile.abilityCooldownMillis());
        int activeDuration = player == null
                ? profile.abilityDurationTicks()
                : adjustedAbilityDurationTicks(player, profile.abilityDurationTicks());
        int activeAmplifier = player == null
                ? profile.abilityAmplifier()
                : adjustedAbilityAmplifier(player, profile.abilityAmplifier());
        return new DrillGuiHooks.DrillSnapshot(
                true,
                itemService.itemId(drill),
                plainText(itemDisplayName(drill)),
                profile.engineId(),
                profile.tankId(),
                fuel,
                maxFuel,
                maxFuel <= 0 ? 0 : (int) Math.round((fuel * 100.0D) / Math.max(1, maxFuel)),
                Math.max(0, fuel / Math.max(1, activeFuelCost)),
                profile,
                activeFuelCost,
                activeCooldown,
                activeDuration,
                activeAmplifier
        );
    }

    private DrillStatProfile.Profile drillProfile(ItemStack drill) {
        if (!isDrill(drill)) {
            return null;
        }
        return DrillStatProfile.resolve(itemService.itemId(drill), drillPartId(drill, true), drillPartId(drill, false));
    }

    private String drillBurstLine(DrillStatProfile.Profile profile) {
        if (profile == null) {
            return ChatColor.GRAY + "Burst: " + ChatColor.AQUA + "Unavailable";
        }
        return ChatColor.GRAY + "Burst: " + ChatColor.AQUA + "Haste " + toRoman(profile.abilityAmplifier() + 1)
                + ChatColor.GRAY + " for " + ChatColor.AQUA + (profile.abilityDurationTicks() / 20) + "s";
    }

    private String drillCrystalNodeLine(DrillStatProfile.Profile profile) {
        if (profile == null || profile.crystalNodeHitReduction() <= 0) {
            return ChatColor.GRAY + "Crystal Nodes: " + ChatColor.GRAY + "Standard";
        }
        return ChatColor.GRAY + "Crystal Nodes: " + ChatColor.GREEN + "-" + profile.crystalNodeHitReduction() + ChatColor.GRAY + " hit(s)";
    }

    private String toRoman(int value) {
        return switch (Math.max(1, value)) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            default -> Integer.toString(value);
        };
    }

    private int drillFuel(ItemStack drill) {
        if (drill == null || !drill.hasItemMeta()) {
            return 0;
        }
        ItemMeta meta = drill.getItemMeta();
        if (meta == null) {
            return 0;
        }
        return meta.getPersistentDataContainer().getOrDefault(drillFuelKey, PersistentDataType.INTEGER, 0);
    }

    private int drillFuelMax(ItemStack drill) {
        if (drill == null || !drill.hasItemMeta()) {
            return 20000;
        }
        ItemMeta meta = drill.getItemMeta();
        if (meta == null) {
            return 20000;
        }
        return meta.getPersistentDataContainer().getOrDefault(drillFuelMaxKey, PersistentDataType.INTEGER, 20000);
    }

    private String drillPartId(ItemStack drill, boolean enginePart) {
        if (drill == null || !drill.hasItemMeta()) {
            return null;
        }
        ItemMeta meta = drill.getItemMeta();
        if (meta == null) {
            return null;
        }
        NamespacedKey key = enginePart ? itemService.getDrillEngineKey() : itemService.getDrillTankKey();
        return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    private String partDisplayName(String itemId, String fallback) {
        if (itemId == null || itemId.isBlank()) {
            return fallback;
        }
        ItemStack item = itemService.createItemByKey(itemId);
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return item.getItemMeta().getDisplayName();
        }
        return ChatColor.AQUA + itemId.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private String itemDisplayName(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return "";
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return "";
        }
        return meta.getDisplayName();
    }

    private String plainText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String stripped = ChatColor.stripColor(text);
        return stripped == null ? text : stripped;
    }

    private String formatInt(int value) {
        return forgeManager.formatInt(Math.max(0, value));
    }

    private ItemStack heldDrill(Player player) {
        if (player == null) {
            return null;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        return isDrill(held) ? held : null;
    }

    private boolean isDrill(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        String id = itemService.itemId(item);
        return id != null && id.endsWith("_DRILL");
    }

    private void refuelWithMaterial(Player player, ItemStack drill, Material material, int fuelAmount, double coinCost) {
        if (!economyService.has(player, coinCost)) {
            player.sendMessage(ChatColor.RED + "You do not have enough coins.");
            return;
        }
        if (!player.getInventory().contains(material)) {
            player.sendMessage(ChatColor.RED + "You do not have " + material.name().toLowerCase(Locale.ROOT).replace('_', ' ') + ".");
            return;
        }
        if (applyFuel(player, drill, fuelAmount)) {
            economyService.withdraw(player, coinCost);
            removeOne(player, material);
            player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 1.0F, 1.4F);
        }
    }

    private void refuelWithCustomItem(Player player, ItemStack drill, String itemId, int fuelAmount) {
        if (!hasCustomItem(player, itemId)) {
            player.sendMessage(ChatColor.RED + "You do not have " + itemId.toLowerCase(Locale.ROOT).replace('_', ' ') + ".");
            return;
        }
        if (applyFuel(player, drill, fuelAmount)) {
            removeOneCustom(player, itemId);
            player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 1.0F, 1.6F);
        }
    }

    private void instantFullRefuel(Player player, ItemStack drill) {
        ItemMeta meta = drill.getItemMeta();
        if (meta == null) {
            return;
        }
        int fuel = meta.getPersistentDataContainer().getOrDefault(drillFuelKey, PersistentDataType.INTEGER, 0);
        int max = meta.getPersistentDataContainer().getOrDefault(drillFuelMaxKey, PersistentDataType.INTEGER, 20000);
        int needed = Math.max(0, max - fuel);
        if (needed <= 0) {
            player.sendMessage(ChatColor.GRAY + "Your drill is already full.");
            return;
        }
        double cost = needed * 2.5D;
        if (!economyService.has(player, cost) || !economyService.withdraw(player, cost)) {
            player.sendMessage(ChatColor.RED + "You need " + ChatColor.GOLD + forgeManager.formatCoins(cost) + ChatColor.RED + " coins.");
            return;
        }
        meta.getPersistentDataContainer().set(drillFuelKey, PersistentDataType.INTEGER, max);
        drill.setItemMeta(meta);
        updateDrillLore(drill);
        player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 1.0F, 1.9F);
        player.sendMessage(ChatColor.YELLOW + "Instant refuel complete.");
    }

    private void installPart(Player player, ItemStack drill, String partId, boolean enginePart) {
        if (!hasCustomItem(player, partId)) {
            player.sendMessage(ChatColor.RED + "You do not have this drill part.");
            return;
        }
        ItemMeta meta = drill.getItemMeta();
        if (meta == null) {
            return;
        }
        var pdc = meta.getPersistentDataContainer();
        NamespacedKey key = enginePart ? itemService.getDrillEngineKey() : itemService.getDrillTankKey();
        String current = pdc.get(key, PersistentDataType.STRING);
        if (partId.equalsIgnoreCase(current)) {
            player.sendMessage(ChatColor.RED + "That part is already installed.");
            return;
        }
        pdc.set(key, PersistentDataType.STRING, partId);
        drill.setItemMeta(meta);
        updateDrillLore(drill);
        removeOneCustom(player, partId);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0F, 1.1F);
        player.sendMessage(ChatColor.GREEN + "Installed " + ChatColor.YELLOW + partId.replace('_', ' ') + ChatColor.GREEN + ".");
    }

    private boolean applyFuel(Player player, ItemStack drill, int amount) {
        ItemMeta meta = drill.getItemMeta();
        if (meta == null) {
            return false;
        }
        int fuel = meta.getPersistentDataContainer().getOrDefault(drillFuelKey, PersistentDataType.INTEGER, 0);
        int max = meta.getPersistentDataContainer().getOrDefault(drillFuelMaxKey, PersistentDataType.INTEGER, 20000);
        if (fuel >= max) {
            player.sendMessage(ChatColor.GRAY + "Your drill is already full.");
            return false;
        }
        int newFuel = Math.min(max, fuel + amount);
        meta.getPersistentDataContainer().set(drillFuelKey, PersistentDataType.INTEGER, newFuel);
        drill.setItemMeta(meta);
        updateDrillLore(drill);
        player.sendMessage(ChatColor.YELLOW + "Drill refueled: " + ChatColor.GREEN + "+" + forgeManager.formatInt(newFuel - fuel));
        return true;
    }

    private void updateDrillLore(ItemStack drill) {
        if (drill == null || !drill.hasItemMeta()) {
            return;
        }
        ItemMeta meta = drill.getItemMeta();
        if (meta == null) {
            return;
        }
        itemService.updateDrillLore(meta);
        drill.setItemMeta(meta);
    }

    private boolean hasCustomItem(Player player, String itemId) {
        return countCustomItem(player, itemId) > 0;
    }

    private void removeOne(Player player, Material material) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack == null || stack.getType() != material || itemService.itemId(stack) != null) {
                continue;
            }
            int remaining = stack.getAmount() - 1;
            if (remaining <= 0) {
                player.getInventory().setItem(slot, null);
            } else {
                stack.setAmount(remaining);
            }
            return;
        }
    }

    private void removeOneCustom(Player player, String itemId) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack == null || !itemId.equalsIgnoreCase(itemService.itemId(stack))) {
                continue;
            }
            int remaining = stack.getAmount() - 1;
            if (remaining <= 0) {
                player.getInventory().setItem(slot, null);
            } else {
                stack.setAmount(remaining);
            }
            return;
        }
    }

    private static final class DrillHolder implements InventoryHolder {
        private final ForgeView view;
        private Inventory inventory;

        private DrillHolder(ForgeView view) {
            this.view = view;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
