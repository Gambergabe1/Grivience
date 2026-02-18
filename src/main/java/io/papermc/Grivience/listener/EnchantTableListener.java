package io.papermc.Grivience.listener;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class EnchantTableListener implements Listener {
    private static final String TITLE = ChatColor.DARK_PURPLE + "Arcane Enchanting";
    private static final List<Integer> OPTION_SLOTS = List.of(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    );

    private final JavaPlugin plugin;
    private final NamespacedKey optionIdKey;
    private final NamespacedKey actionKey;
    private final Map<String, EnchantOption> configuredEcoOptions = new LinkedHashMap<>();

    private boolean enabled;
    private String ecoCommandTemplate;
    private int vanillaBaseCost;
    private int vanillaPerLevelCost;
    private int vanillaMaxCost;
    private double overallCostMultiplier;
    private double damageEnchantCostMultiplier;
    private double ecoCostMultiplier;

    public EnchantTableListener(JavaPlugin plugin) {
        this.plugin = plugin;
        this.optionIdKey = new NamespacedKey(plugin, "arcane-option-id");
        this.actionKey = new NamespacedKey(plugin, "arcane-action");
    }

    public void reloadFromConfig() {
        enabled = plugin.getConfig().getBoolean("enchant-table.enabled", true);
        ecoCommandTemplate = plugin.getConfig().getString("enchant-table.eco.command-template", "").trim();
        vanillaBaseCost = Math.max(1, plugin.getConfig().getInt("enchant-table.vanilla.base-cost", 4));
        vanillaPerLevelCost = Math.max(1, plugin.getConfig().getInt("enchant-table.vanilla.per-level-cost", 2));
        vanillaMaxCost = Math.max(1, plugin.getConfig().getInt("enchant-table.vanilla.max-cost", 30));
        overallCostMultiplier = sanitizeMultiplier(plugin.getConfig().getDouble("enchant-table.cost-multipliers.overall", 1.35D), 1.35D);
        damageEnchantCostMultiplier = sanitizeMultiplier(plugin.getConfig().getDouble("enchant-table.cost-multipliers.damage-enchants", 1.75D), 1.75D);
        ecoCostMultiplier = sanitizeMultiplier(plugin.getConfig().getDouble("enchant-table.cost-multipliers.eco", 1.25D), 1.25D);

        configuredEcoOptions.clear();
        ConfigurationSection optionsSection = plugin.getConfig().getConfigurationSection("enchant-table.options");
        if (optionsSection != null) {
            for (String key : optionsSection.getKeys(false)) {
                ConfigurationSection section = optionsSection.getConfigurationSection(key);
                if (section == null) {
                    continue;
                }
                EnchantOption option = parseOption(key, section);
                if (option != null && option.type() == OptionType.ECO) {
                    configuredEcoOptions.put(option.id(), option);
                }
            }
        }
        if (configuredEcoOptions.isEmpty()) {
            loadFallbackEcoOptions();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!enabled) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        if (event.getClickedBlock().getType() != Material.ENCHANTING_TABLE) {
            return;
        }
        event.setCancelled(true);
        openTable(event.getPlayer(), 0);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof ArcaneTableHolder holder)) {
            return;
        }

        event.setCancelled(true);
        if (!Objects.equals(event.getClickedInventory(), event.getView().getTopInventory())) {
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action != null) {
            switch (action) {
                case "page_prev" -> openTable(player, holder.page() - 1);
                case "page_next" -> openTable(player, holder.page() + 1);
                case "refresh" -> openTable(player, 0);
                case "close" -> player.closeInventory();
                default -> {
                }
            }
            return;
        }

        String optionId = meta.getPersistentDataContainer().get(optionIdKey, PersistentDataType.STRING);
        if (optionId == null) {
            return;
        }

        EnchantOption option = resolveOptionForPlayer(player, optionId);
        if (option == null) {
            player.sendMessage(ChatColor.RED + "That enchant option is unavailable for your held weapon.");
            openTable(player, holder.page());
            return;
        }

        applyOption(player, option);
        openTable(player, holder.page());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof ArcaneTableHolder) {
            event.setCancelled(true);
        }
    }

    private void openTable(Player player, int requestedPage) {
        ArcaneTableHolder holder = new ArcaneTableHolder();
        Inventory inventory = Bukkit.createInventory(holder, 54, TITLE);
        holder.inventory = inventory;

        List<EnchantOption> options = optionsForHeldItem(player);
        int totalPages = Math.max(1, (int) Math.ceil(options.size() / (double) OPTION_SLOTS.size()));
        int page = Math.max(0, Math.min(requestedPage, totalPages - 1));
        holder.page = page;
        holder.totalPages = totalPages;

        ItemStack filler = glass(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        inventory.setItem(4, namedItem(
                Material.ENCHANTED_BOOK,
                ChatColor.LIGHT_PURPLE + "Hypixel-Style Arcane Forge",
                List.of(
                        ChatColor.GRAY + "Showing all valid enchants",
                        ChatColor.GRAY + "for your currently held weapon.",
                        ChatColor.DARK_GRAY + "EcoEnchants options included"
                )
        ));

        ItemStack held = player.getInventory().getItemInMainHand();
        inventory.setItem(49, previewItem(held));

        int start = page * OPTION_SLOTS.size();
        int end = Math.min(start + OPTION_SLOTS.size(), options.size());
        int slotIndex = 0;
        for (int i = start; i < end; i++) {
            EnchantOption option = options.get(i);
            int slot = OPTION_SLOTS.get(slotIndex++);
            inventory.setItem(slot, optionItem(option, held));
        }

        inventory.setItem(45, actionItem(
                Material.ARROW,
                ChatColor.YELLOW + "Previous Page",
                List.of(ChatColor.GRAY + "Go back one page."),
                "page_prev"
        ));
        inventory.setItem(47, actionItem(
                Material.COMPASS,
                ChatColor.AQUA + "Refresh",
                List.of(ChatColor.GRAY + "Re-scan your held weapon."),
                "refresh"
        ));
        inventory.setItem(51, actionItem(
                Material.BARRIER,
                ChatColor.RED + "Close",
                List.of(ChatColor.GRAY + "Close arcane enchanting."),
                "close"
        ));
        inventory.setItem(53, actionItem(
                Material.ARROW,
                ChatColor.YELLOW + "Next Page",
                List.of(ChatColor.GRAY + "Go forward one page."),
                "page_next"
        ));
        inventory.setItem(46, glass(Material.PURPLE_STAINED_GLASS_PANE, " "));
        inventory.setItem(48, glass(Material.PURPLE_STAINED_GLASS_PANE, " "));
        inventory.setItem(50, glass(Material.PURPLE_STAINED_GLASS_PANE, " "));
        inventory.setItem(52, glass(Material.PURPLE_STAINED_GLASS_PANE, " "));
        inventory.setItem(49, namedItem(
                Material.EXPERIENCE_BOTTLE,
                ChatColor.AQUA + "Page " + ChatColor.YELLOW + (page + 1) + "/" + totalPages,
                List.of(
                        ChatColor.GRAY + "Available options: " + options.size(),
                        ChatColor.GRAY + "Your levels: " + ChatColor.YELLOW + player.getLevel()
                )
        ));

        player.openInventory(inventory);
    }

    private EnchantOption resolveOptionForPlayer(Player player, String optionId) {
        for (EnchantOption option : optionsForHeldItem(player)) {
            if (option.id().equals(optionId)) {
                return option;
            }
        }
        return null;
    }

    private List<EnchantOption> optionsForHeldItem(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            return List.of();
        }

        List<EnchantOption> options = new ArrayList<>(vanillaOptionsForItem(held));
        options.addAll(configuredEcoOptions.values());
        return options;
    }

    private List<EnchantOption> vanillaOptionsForItem(ItemStack held) {
        List<Enchantment> all = new ArrayList<>(List.of(Enchantment.values()));
        all.sort(Comparator.comparing(this::enchantSortKey));

        List<EnchantOption> options = new ArrayList<>();
        for (Enchantment enchantment : all) {
            if (enchantment == null || !enchantment.canEnchantItem(held)) {
                continue;
            }
            int level = Math.max(1, enchantment.getMaxLevel());
            int baseCost = vanillaBaseCost + (level * vanillaPerLevelCost);
            int cost = Math.min(vanillaMaxCost, scaleCost(baseCost, isDamageEnchantment(enchantment), false));
            String key = enchantSortKey(enchantment);
            String id = "vanilla:" + key + ":" + level;
            String display = ChatColor.GOLD + prettifyId(key) + " " + roman(level);
            List<String> lore = List.of(
                    ChatColor.GRAY + "Vanilla enchant",
                    ChatColor.DARK_GRAY + "Auto-generated for held weapon."
            );
            options.add(EnchantOption.vanilla(
                    id,
                    cost,
                    level,
                    iconForEnchantment(enchantment),
                    display,
                    lore,
                    enchantment
            ));
        }
        return options;
    }

    private void applyOption(Player player, EnchantOption option) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "Hold a weapon in your main hand first.");
            return;
        }
        if (player.getLevel() < option.costLevels()) {
            player.sendMessage(ChatColor.RED + "You need " + option.costLevels() + " levels for that enchant.");
            return;
        }

        boolean success = switch (option.type()) {
            case VANILLA -> applyVanillaEnchant(player, held, option);
            case ECO -> applyEcoEnchant(player, option);
        };
        if (!success) {
            return;
        }

        player.setLevel(Math.max(0, player.getLevel() - option.costLevels()));
        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0F, 1.1F);
        player.sendMessage(ChatColor.GREEN + "Applied " + option.displayName() + ChatColor.GREEN + ".");
    }

    private boolean applyVanillaEnchant(Player player, ItemStack held, EnchantOption option) {
        Enchantment enchantment = option.enchantment();
        if (enchantment == null) {
            player.sendMessage(ChatColor.RED + "This enchant option is invalid.");
            return false;
        }
        int current = held.getEnchantmentLevel(enchantment);
        if (current >= option.level()) {
            player.sendMessage(ChatColor.RED + "Your held item already has this enchant level or higher.");
            return false;
        }
        held.addUnsafeEnchantment(enchantment, option.level());
        return true;
    }

    private boolean applyEcoEnchant(Player player, EnchantOption option) {
        if (!Bukkit.getPluginManager().isPluginEnabled("EcoEnchants")) {
            player.sendMessage(ChatColor.RED + "EcoEnchants is not installed.");
            return false;
        }
        if (ecoCommandTemplate == null || ecoCommandTemplate.isBlank()) {
            player.sendMessage(ChatColor.RED + "Eco command template is not configured.");
            return false;
        }
        String command = ecoCommandTemplate
                .replace("{player}", player.getName())
                .replace("{enchant}", option.ecoEnchantId())
                .replace("{level}", Integer.toString(option.level()));
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        boolean executed = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        if (!executed) {
            player.sendMessage(ChatColor.RED + "EcoEnchants command failed. Check enchant-table.eco.command-template.");
        }
        return executed;
    }

    private EnchantOption parseOption(String id, ConfigurationSection section) {
        String typeRaw = section.getString("type", "ECO").toUpperCase(Locale.ROOT);
        OptionType type;
        try {
            type = OptionType.valueOf(typeRaw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
        if (type != OptionType.ECO) {
            return null;
        }

        int baseCostLevels = Math.max(1, section.getInt("cost-levels", 10));
        int level = Math.max(1, section.getInt("level", 1));
        Material icon = materialOrDefault(section.getString("icon"), Material.ENCHANTED_BOOK);
        String displayName = colorize(section.getString("display-name", prettifyId(id)));
        List<String> lore = colorizeList(section.getStringList("lore"));
        String ecoId = section.getString("eco-id", id);
        int costLevels = scaleCost(baseCostLevels, isDamageEcoEnchantId(ecoId), true);
        return EnchantOption.eco(id, costLevels, level, icon, displayName, lore, ecoId);
    }

    private void loadFallbackEcoOptions() {
        configuredEcoOptions.put("eco_lifesteal", EnchantOption.eco(
                "eco_lifesteal",
                scaleCost(10, true, true),
                1,
                Material.ENCHANTED_BOOK,
                ChatColor.LIGHT_PURPLE + "Eco: Lifesteal I",
                List.of(ChatColor.DARK_GRAY + "Requires EcoEnchants."),
                "lifesteal"
        ));
        configuredEcoOptions.put("eco_criticals", EnchantOption.eco(
                "eco_criticals",
                scaleCost(10, true, true),
                1,
                Material.ENCHANTED_BOOK,
                ChatColor.LIGHT_PURPLE + "Eco: Criticals I",
                List.of(ChatColor.DARK_GRAY + "Requires EcoEnchants."),
                "criticals"
        ));
    }

    private ItemStack optionItem(EnchantOption option, ItemStack held) {
        int currentLevel = option.type() == OptionType.VANILLA && option.enchantment() != null
                ? held.getEnchantmentLevel(option.enchantment())
                : 0;

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Cost: " + ChatColor.AQUA + option.costLevels() + " levels");
        lore.add(ChatColor.GRAY + "Target Level: " + ChatColor.YELLOW + option.level());
        if (option.type() == OptionType.VANILLA) {
            lore.add(ChatColor.GRAY + "Current Level: " + ChatColor.YELLOW + currentLevel);
        } else {
            lore.add(ChatColor.DARK_GRAY + "Eco ID: " + option.ecoEnchantId());
        }
        lore.addAll(option.lore());
        lore.add("");
        lore.add(ChatColor.GREEN + "Click to apply");

        ItemStack item = namedItem(option.icon(), option.displayName(), lore);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(optionIdKey, PersistentDataType.STRING, option.id());
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack actionItem(Material material, String name, List<String> lore, String action) {
        ItemStack item = namedItem(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack previewItem(ItemStack held) {
        if (held == null || held.getType() == Material.AIR) {
            return namedItem(
                    Material.BARRIER,
                    ChatColor.RED + "No Weapon Selected",
                    List.of(ChatColor.GRAY + "Hold a weapon in your main hand.")
            );
        }
        ItemStack preview = held.clone();
        ItemMeta meta = preview.getItemMeta();
        List<String> lore = meta.hasLore() && meta.getLore() != null
                ? new ArrayList<>(meta.getLore())
                : new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Selected by Arcane Forge");
        meta.setLore(lore);
        preview.setItemMeta(meta);
        return preview;
    }

    private ItemStack namedItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack glass(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private Material materialOrDefault(String raw, Material fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        Material parsed = Material.matchMaterial(raw.trim().toUpperCase(Locale.ROOT));
        return parsed == null ? fallback : parsed;
    }

    private String prettifyId(String id) {
        String[] parts = id.split("_");
        List<String> words = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            String lower = part.toLowerCase(Locale.ROOT);
            words.add(Character.toUpperCase(lower.charAt(0)) + lower.substring(1));
        }
        return String.join(" ", words);
    }

    private String enchantSortKey(Enchantment enchantment) {
        if (enchantment.getKey() != null) {
            return enchantment.getKey().getKey();
        }
        return "unknown_" + Integer.toHexString(System.identityHashCode(enchantment));
    }

    private int scaleCost(int baseCost, boolean damageEnchant, boolean ecoEnchant) {
        double scaled = Math.max(1.0D, baseCost) * overallCostMultiplier;
        if (damageEnchant) {
            scaled *= damageEnchantCostMultiplier;
        }
        if (ecoEnchant) {
            scaled *= ecoCostMultiplier;
        }
        return Math.max(1, (int) Math.ceil(scaled));
    }

    private boolean isDamageEnchantment(Enchantment enchantment) {
        String key = enchantSortKey(enchantment);
        return key.contains("sharpness")
                || key.contains("smite")
                || key.contains("bane_of_arthropods")
                || key.contains("sweeping")
                || key.contains("power")
                || key.contains("impaling")
                || key.contains("piercing")
                || key.contains("multishot")
                || key.contains("fire_aspect")
                || key.contains("density")
                || key.contains("breach")
                || key.contains("wind_burst");
    }

    private boolean isDamageEcoEnchantId(String ecoId) {
        if (ecoId == null || ecoId.isBlank()) {
            return false;
        }
        String normalized = ecoId.toLowerCase(Locale.ROOT);
        return normalized.contains("damage")
                || normalized.contains("dmg")
                || normalized.contains("attack")
                || normalized.contains("crit")
                || normalized.contains("critical")
                || normalized.contains("execute")
                || normalized.contains("lifesteal")
                || normalized.contains("sharp")
                || normalized.contains("smite")
                || normalized.contains("power")
                || normalized.contains("slayer")
                || normalized.contains("berserk")
                || normalized.contains("bleed")
                || normalized.contains("poison")
                || normalized.contains("fire")
                || normalized.contains("ice");
    }

    private double sanitizeMultiplier(double value, double fallback) {
        if (Double.isNaN(value) || Double.isInfinite(value) || value <= 0.0D) {
            return fallback;
        }
        return value;
    }

    private Material iconForEnchantment(Enchantment enchantment) {
        String key = enchantSortKey(enchantment);
        if (key.contains("sharpness") || key.contains("smite") || key.contains("sweeping")) {
            return Material.IRON_SWORD;
        }
        if (key.contains("power") || key.contains("flame") || key.contains("infinity") || key.contains("punch")) {
            return Material.BOW;
        }
        if (key.contains("trident") || key.contains("impaling") || key.contains("riptide") || key.contains("channeling")) {
            return Material.TRIDENT;
        }
        if (key.contains("fire")) {
            return Material.BLAZE_POWDER;
        }
        if (key.contains("looting") || key.contains("fortune")) {
            return Material.EMERALD;
        }
        if (key.contains("mending")) {
            return Material.EXPERIENCE_BOTTLE;
        }
        if (key.contains("unbreaking")) {
            return Material.ANVIL;
        }
        return Material.ENCHANTED_BOOK;
    }

    private String roman(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> Integer.toString(value);
        };
    }

    private String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    private List<String> colorizeList(List<String> input) {
        List<String> result = new ArrayList<>(input.size());
        for (String line : input) {
            result.add(colorize(line));
        }
        return result;
    }

    private enum OptionType {
        VANILLA,
        ECO
    }

    private record EnchantOption(
            String id,
            OptionType type,
            int costLevels,
            int level,
            Material icon,
            String displayName,
            List<String> lore,
            Enchantment enchantment,
            String ecoEnchantId
    ) {
        private static EnchantOption vanilla(
                String id,
                int costLevels,
                int level,
                Material icon,
                String displayName,
                List<String> lore,
                Enchantment enchantment
        ) {
            return new EnchantOption(id, OptionType.VANILLA, costLevels, level, icon, displayName, List.copyOf(lore), enchantment, "");
        }

        private static EnchantOption eco(
                String id,
                int costLevels,
                int level,
                Material icon,
                String displayName,
                List<String> lore,
                String ecoEnchantId
        ) {
            return new EnchantOption(id, OptionType.ECO, costLevels, level, icon, displayName, List.copyOf(lore), null, ecoEnchantId);
        }
    }

    private static final class ArcaneTableHolder implements InventoryHolder {
        private Inventory inventory;
        private int page;
        private int totalPages;

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public int page() {
            return page;
        }

        public int totalPages() {
            return totalPages;
        }
    }
}
