package io.papermc.Grivience.compactor;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.item.EnchantedFarmItemType;
import io.papermc.Grivience.minion.MinionManager;
import io.papermc.Grivience.skyblock.profile.ProfileManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Personal Compactor runtime and profile-scoped configuration.
 */
public final class PersonalCompactorManager {
    public static final int MAX_SLOTS = 12;
    public static final String USE_PERMISSION = "grivience.personalcompactor";
    private static final int[] GUI_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23};
    private static final String MENU_TITLE = ChatColor.DARK_GREEN + "Personal Compactor";

    private final GriviencePlugin plugin;
    private final Set<UUID> queuedCompaction = ConcurrentHashMap.newKeySet();
    private final Map<String, Rule> rulesByInput;
    private final Map<String, Rule> rulesByOutput;
    private final Map<Material, String> vanillaKeys;

    public PersonalCompactorManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.vanillaKeys = buildVanillaKeys();
        this.rulesByInput = buildRules();
        this.rulesByOutput = indexRulesByOutput(this.rulesByInput);
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("personal-compactor.enabled", true);
    }

    public boolean isCompactorItem(ItemStack stack) {
        return compactorType(stack) != null;
    }

    public PersonalCompactorType compactorType(ItemStack stack) {
        CustomItemService itemService = plugin.getCustomItemService();
        if (itemService == null || stack == null || stack.getType().isAir()) {
            return null;
        }
        return PersonalCompactorType.parse(itemService.itemId(stack));
    }

    public int unlockedSlots(Player player) {
        if (player == null || !isEnabled()) {
            return 0;
        }
        int best = 0;
        for (ItemStack stack : inventoryStacks(player)) {
            PersonalCompactorType type = compactorType(stack);
            if (type != null) {
                best = Math.max(best, type.slots());
            }
        }
        return Math.max(0, Math.min(MAX_SLOTS, best));
    }

    public void openMenu(Player player) {
        openMenu(player, null);
    }

    public void openMenu(Player player, Integer preferredSlot) {
        if (player == null) {
            return;
        }
        if (!isEnabled()) {
            player.sendMessage(ChatColor.RED + "Personal Compactor is currently disabled.");
            return;
        }
        if (!player.hasPermission(USE_PERMISSION)) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use Personal Compactor.");
            return;
        }

        int unlocked = unlockedSlots(player);
        if (unlocked <= 0) {
            player.sendMessage(ChatColor.RED + "You need a Personal Compactor item in your inventory to use this.");
            return;
        }

        Map<Integer, String> configured = configuredSlots(player);
        Integer selectedSlot = resolveSelectedSlot(unlocked, configured, preferredSlot);
        MenuHolder holder = new MenuHolder(player.getUniqueId(), selectedSlot);
        Inventory menu = Bukkit.createInventory(holder, 36, MENU_TITLE);
        holder.setInventory(menu);
        fillBackground(menu);
        renderHeader(menu, unlocked, configured.size(), selectedSlot);
        renderSlots(menu, unlocked, configured, selectedSlot);
        menu.setItem(31, button(Material.BARRIER, ChatColor.RED + "Close", List.of(ChatColor.GRAY + "Close this menu.")));
        player.openInventory(menu);
    }

    public boolean isMenu(Inventory inventory) {
        return inventory != null && inventory.getHolder() instanceof MenuHolder;
    }

    public boolean isMenuViewer(Inventory inventory, Player player) {
        if (inventory == null || player == null) {
            return false;
        }
        InventoryHolder holder = inventory.getHolder();
        if (!(holder instanceof MenuHolder menuHolder)) {
            return false;
        }
        return player.getUniqueId().equals(menuHolder.viewerId());
    }

    public Integer selectedSlot(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        InventoryHolder holder = inventory.getHolder();
        if (!(holder instanceof MenuHolder menuHolder)) {
            return null;
        }
        return menuHolder.selectedSlot();
    }

    public int compactorSlotFromGuiSlot(int guiSlot) {
        for (int i = 0; i < GUI_SLOTS.length; i++) {
            if (GUI_SLOTS[i] == guiSlot) {
                return i;
            }
        }
        return -1;
    }

    public String ruleOutputFromItem(ItemStack stack) {
        String key = keyFromItem(stack);
        if (key == null) {
            return null;
        }
        Rule byOutput = rulesByOutput.get(key);
        if (byOutput != null) {
            return byOutput.outputKey;
        }
        Rule byInput = rulesByInput.get(key);
        return byInput == null ? null : byInput.outputKey;
    }

    public int firstAvailableConfiguredSlot(Player player) {
        if (player == null) {
            return -1;
        }
        int unlocked = unlockedSlots(player);
        Map<Integer, String> configured = configuredSlots(player);
        for (int i = 0; i < unlocked; i++) {
            String key = configured.get(i);
            if (key == null || key.isBlank()) {
                return i;
            }
        }
        return -1;
    }

    public Map<Integer, String> configuredSlotsSnapshot(Player player) {
        return new LinkedHashMap<>(configuredSlots(player));
    }

    public boolean clearSlot(Player player, int slotIndex) {
        if (player == null || slotIndex < 0 || slotIndex >= MAX_SLOTS) {
            return false;
        }
        SkyBlockProfile profile = selectedProfile(player);
        if (profile == null) {
            return false;
        }
        profile.clearPersonalCompactorSlot(slotIndex);
        saveProfile(profile);
        return true;
    }

    public SetSlotResult setSlotFromItem(Player player, int slotIndex, ItemStack source) {
        if (player == null || source == null || source.getType().isAir()) {
            return SetSlotResult.INVALID_ITEM;
        }
        if (slotIndex < 0 || slotIndex >= MAX_SLOTS) {
            return SetSlotResult.INVALID_SLOT;
        }

        int unlocked = unlockedSlots(player);
        if (slotIndex >= unlocked) {
            return SetSlotResult.LOCKED_SLOT;
        }

        String outputKey = ruleOutputFromItem(source);
        if (outputKey == null) {
            return SetSlotResult.UNSUPPORTED_ITEM;
        }

        SkyBlockProfile profile = selectedProfile(player);
        if (profile == null) {
            return SetSlotResult.NO_PROFILE;
        }

        profile.setPersonalCompactorSlot(slotIndex, outputKey);
        saveProfile(profile);
        return SetSlotResult.SUCCESS;
    }

    public void queueCompaction(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        if (!player.hasPermission(USE_PERMISSION)) {
            return;
        }
        if (!isEnabled() || !plugin.getConfig().getBoolean("personal-compactor.auto-compact", true)) {
            return;
        }
        if (unlockedSlots(player) <= 0) {
            return;
        }

        UUID id = player.getUniqueId();
        if (!queuedCompaction.add(id)) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                queuedCompaction.remove(id);
                Player online = Bukkit.getPlayer(id);
                if (online == null || !online.isOnline()) {
                    return;
                }
                compactNow(online);
            } finally {
                queuedCompaction.remove(id);
            }
        });
    }

    public void compactNow(Player player) {
        if (player == null || !player.isOnline() || !isEnabled()) {
            return;
        }
        if (!player.hasPermission(USE_PERMISSION)) {
            return;
        }

        List<Rule> activeRules = activeRules(player);
        if (activeRules.isEmpty()) {
            return;
        }

        boolean changed = false;
        // Multiple passes allow chain compacting when multiple outputs are configured.
        for (int pass = 0; pass < 16; pass++) {
            boolean passChanged = false;
            for (Rule rule : activeRules) {
                if (rule == null || rule.inputAmount <= 0 || rule.outputAmount <= 0) {
                    continue;
                }

                int available = countKey(player, rule.inputKey);
                int crafts = available / rule.inputAmount;
                if (crafts <= 0) {
                    continue;
                }

                int removed = removeKey(player, rule.inputKey, crafts * rule.inputAmount);
                int successfulCrafts = removed / rule.inputAmount;
                if (successfulCrafts <= 0) {
                    continue;
                }

                addKey(player, rule.outputKey, successfulCrafts * rule.outputAmount);
                passChanged = true;
                changed = true;
            }
            if (!passChanged) {
                break;
            }
        }

        if (changed) {
            player.updateInventory();
        }
    }

    public void shutdown() {
        queuedCompaction.clear();
    }

    public record Rule(String inputKey, int inputAmount, String outputKey, int outputAmount) {}

    public enum SetSlotResult {
        SUCCESS,
        LOCKED_SLOT,
        INVALID_SLOT,
        INVALID_ITEM,
        UNSUPPORTED_ITEM,
        NO_PROFILE
    }

    static Integer resolveSelectedSlot(int unlocked, Map<Integer, String> configured, Integer preferredSlot) {
        if (unlocked <= 0) {
            return null;
        }
        if (preferredSlot != null && preferredSlot >= 0 && preferredSlot < unlocked) {
            return preferredSlot;
        }
        if (configured != null) {
            for (int i = 0; i < unlocked; i++) {
                String key = configured.get(i);
                if (key == null || key.isBlank()) {
                    return i;
                }
            }
        }
        return null;
    }

    public static final class MenuHolder implements InventoryHolder {
        private final UUID viewerId;
        private final Integer selectedSlot;
        private Inventory inventory;

        public MenuHolder(UUID viewerId, Integer selectedSlot) {
            this.viewerId = viewerId;
            this.selectedSlot = selectedSlot;
        }

        public UUID viewerId() {
            return viewerId;
        }

        public Integer selectedSlot() {
            return selectedSlot;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private void fillBackground(Inventory menu) {
        ItemStack filler = glass(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack border = glass(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 0; slot < menu.getSize(); slot++) {
            menu.setItem(slot, filler.clone());
        }
        for (int slot = 0; slot < menu.getSize(); slot++) {
            boolean top = slot < 9;
            boolean bottom = slot >= menu.getSize() - 9;
            boolean left = slot % 9 == 0;
            boolean right = slot % 9 == 8;
            if (top || bottom || left || right) {
                menu.setItem(slot, border.clone());
            }
        }
    }

    private void renderHeader(Inventory menu, int unlocked, int configuredCount, Integer selectedSlot) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Unlocked Slots: " + ChatColor.YELLOW + unlocked + ChatColor.GRAY + "/" + ChatColor.YELLOW + MAX_SLOTS);
        lore.add(ChatColor.GRAY + "Configured Slots: " + ChatColor.AQUA + configuredCount);
        lore.add(ChatColor.GRAY + "Selected Slot: " + (selectedSlot == null ? ChatColor.RED + "None" : ChatColor.YELLOW + Integer.toString(selectedSlot + 1)));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Left-click a slot to select it.");
        lore.add(ChatColor.YELLOW + "Then click a compacted item");
        lore.add(ChatColor.YELLOW + "in your inventory to bind it.");
        lore.add(ChatColor.YELLOW + "Right-click a slot to clear it.");
        menu.setItem(4, button(
                Material.DROPPER,
                ChatColor.GREEN + "" + ChatColor.BOLD + "Personal Compactor",
                lore
        ));
    }

    private void renderSlots(Inventory menu, int unlocked, Map<Integer, String> configured, Integer selectedSlot) {
        for (int i = 0; i < GUI_SLOTS.length; i++) {
            int guiSlot = GUI_SLOTS[i];
            boolean selected = selectedSlot != null && selectedSlot == i;
            if (i >= unlocked) {
                menu.setItem(guiSlot, button(
                        Material.RED_STAINED_GLASS_PANE,
                        ChatColor.RED + "Slot " + (i + 1) + " (Locked)",
                        List.of(ChatColor.GRAY + "Upgrade your Personal Compactor", ChatColor.GRAY + "to unlock this slot.")
                ));
                continue;
            }

            String outputKey = configured.get(i);
            Rule rule = outputKey == null ? null : rulesByOutput.get(outputKey);
            if (rule == null) {
                menu.setItem(guiSlot, button(
                        selected ? Material.YELLOW_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE,
                        (selected ? ChatColor.YELLOW : ChatColor.GREEN) + "Slot " + (i + 1) + (selected ? " (Selected)" : ""),
                        selected
                                ? List.of(
                                ChatColor.GRAY + "No target item configured.",
                                "",
                                ChatColor.GOLD + "Selected slot.",
                                ChatColor.YELLOW + "Click a compacted item in your inventory",
                                ChatColor.YELLOW + "to set what this slot produces."
                        )
                                : List.of(
                                ChatColor.GRAY + "No target item configured.",
                                "",
                                ChatColor.YELLOW + "Left-click to select this slot.",
                                ChatColor.DARK_GRAY + "Tip: Use the compacted output item."
                        )
                ));
                continue;
            }

            ItemStack icon = outputPreview(outputKey);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName((selected ? ChatColor.YELLOW : ChatColor.GOLD) + "Slot " + (i + 1) + ": " + ChatColor.YELLOW + prettyName(outputKey) + (selected ? ChatColor.GOLD + " (Selected)" : ""));
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Input: " + ChatColor.WHITE + prettyName(rule.inputKey));
                lore.add(ChatColor.GRAY + "Output: " + ChatColor.WHITE + prettyName(outputKey));
                lore.add(ChatColor.GRAY + "Recipe: " + ChatColor.WHITE + rule.inputAmount + " -> " + rule.outputAmount);
                lore.add("");
                if (selected) {
                    lore.add(ChatColor.GOLD + "Selected slot.");
                    lore.add(ChatColor.YELLOW + "Click a compacted item in your inventory");
                    lore.add(ChatColor.YELLOW + "to replace this slot's target.");
                    lore.add("");
                } else {
                    lore.add(ChatColor.YELLOW + "Left-click to select this slot.");
                }
                lore.add(ChatColor.YELLOW + "Right-click to clear this slot.");
                meta.setLore(lore);
                icon.setItemMeta(meta);
            }
            menu.setItem(guiSlot, icon);
        }
    }

    private Map<Integer, String> configuredSlots(Player player) {
        SkyBlockProfile profile = selectedProfile(player);
        if (profile == null) {
            return Map.of();
        }

        Map<Integer, String> normalized = new LinkedHashMap<>();
        boolean migrated = false;
        for (Map.Entry<Integer, String> entry : profile.getPersonalCompactorSlots().entrySet()) {
            Integer slot = entry.getKey();
            String key = normalizeKey(entry.getValue());
            if (slot == null || key == null || slot < 0 || slot >= MAX_SLOTS) {
                continue;
            }
            if (rulesByOutput.containsKey(key)) {
                normalized.put(slot, key);
                continue;
            }
            Rule legacyRule = rulesByInput.get(key);
            if (legacyRule == null || legacyRule.outputKey == null || !rulesByOutput.containsKey(legacyRule.outputKey)) {
                continue;
            }
            normalized.put(slot, legacyRule.outputKey);
            profile.setPersonalCompactorSlot(slot, legacyRule.outputKey);
            migrated = true;
        }
        if (migrated) {
            saveProfile(profile);
        }
        return normalized;
    }

    private List<Rule> activeRules(Player player) {
        int unlocked = unlockedSlots(player);
        if (unlocked <= 0) {
            return List.of();
        }
        Map<Integer, String> configured = configuredSlots(player);
        List<Rule> order = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (int i = 0; i < unlocked; i++) {
            String key = configured.get(i);
            if (key == null || !seen.add(key)) {
                continue;
            }
            Rule rule = rulesByOutput.get(key);
            if (rule != null) {
                order.add(rule);
            }
        }
        return order;
    }

    private SkyBlockProfile selectedProfile(Player player) {
        if (player == null) {
            return null;
        }
        ProfileManager profileManager = plugin.getProfileManager();
        return profileManager == null ? null : profileManager.getSelectedProfile(player);
    }

    private void saveProfile(SkyBlockProfile profile) {
        ProfileManager profileManager = plugin.getProfileManager();
        if (profileManager != null && profile != null) {
            profileManager.saveProfile(profile);
        }
    }

    private Collection<ItemStack> inventoryStacks(Player player) {
        List<ItemStack> all = new ArrayList<>();
        if (player == null) {
            return all;
        }
        all.addAll(Arrays.asList(player.getInventory().getStorageContents()));
        all.addAll(Arrays.asList(player.getInventory().getExtraContents()));
        return all;
    }

    private String keyFromItem(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return null;
        }

        MinionManager minionManager = plugin.getMinionManager();
        if (minionManager != null) {
            String ingredient = minionManager.readIngredientId(stack);
            String normalized = normalizeKey(ingredient);
            if (normalized != null) {
                return normalized;
            }
        }

        CustomItemService itemService = plugin.getCustomItemService();
        if (itemService != null) {
            String custom = normalizeKey(itemService.itemId(stack));
            if (custom != null) {
                return custom;
            }
        }

        if (!isPlainVanillaResource(stack)) {
            return null;
        }
        return vanillaKeys.get(stack.getType());
    }

    private boolean isPlainVanillaResource(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
            return true;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return true;
        }
        if (meta.hasDisplayName() || meta.hasLore()) {
            return false;
        }
        if (!meta.getEnchants().isEmpty()) {
            return false;
        }
        return meta.getPersistentDataContainer().isEmpty();
    }

    private int countKey(Player player, String key) {
        int total = 0;
        for (ItemStack stack : inventoryStacks(player)) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            String stackKey = keyFromItem(stack);
            if (key.equals(stackKey)) {
                total += Math.max(0, stack.getAmount());
            }
        }
        return total;
    }

    private int removeKey(Player player, String key, int amount) {
        if (player == null || amount <= 0) {
            return 0;
        }

        int remaining = amount;
        int removed = 0;
        var inventory = player.getInventory();

        ItemStack[] storage = inventory.getStorageContents();
        removed += removeFromArray(storage, key, remaining);
        remaining -= removed;
        inventory.setStorageContents(storage);

        if (remaining > 0) {
            ItemStack[] extra = inventory.getExtraContents();
            int removedExtra = removeFromArray(extra, key, remaining);
            removed += removedExtra;
            inventory.setExtraContents(extra);
        }

        return removed;
    }

    private int removeFromArray(ItemStack[] contents, String key, int amount) {
        if (contents == null || amount <= 0) {
            return 0;
        }

        int remaining = amount;
        int removed = 0;

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            String stackKey = keyFromItem(stack);
            if (!key.equals(stackKey)) {
                continue;
            }

            int take = Math.min(remaining, stack.getAmount());
            int next = stack.getAmount() - take;
            removed += take;
            remaining -= take;

            if (next <= 0) {
                contents[i] = null;
            } else {
                stack.setAmount(next);
                contents[i] = stack;
            }
        }

        return removed;
    }

    private void addKey(Player player, String key, int amount) {
        if (player == null || amount <= 0) {
            return;
        }

        ItemStack template = outputPreview(key);
        if (template == null || template.getType().isAir()) {
            return;
        }

        int maxStack = Math.max(1, template.getMaxStackSize());
        int remaining = amount;
        while (remaining > 0) {
            int stackAmount = Math.min(remaining, maxStack);
            ItemStack give = template.clone();
            give.setAmount(stackAmount);
            Map<Integer, ItemStack> leftovers = player.getInventory().addItem(give);
            for (ItemStack leftover : leftovers.values()) {
                if (leftover == null || leftover.getType().isAir() || leftover.getAmount() <= 0) {
                    continue;
                }
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
            remaining -= stackAmount;
        }
    }

    private ItemStack outputPreview(String key) {
        String normalized = normalizeKey(key);
        if (normalized == null) {
            return new ItemStack(Material.PAPER);
        }

        CustomItemService itemService = plugin.getCustomItemService();
        if (itemService != null) {
            ItemStack direct = itemService.createItemByKey(normalized);
            if (direct != null) {
                direct.setAmount(1);
                return direct;
            }
            if ("enchanted_hay_block".equals(normalized)) {
                ItemStack hay = itemService.createItemByKey("enchanted_hay_bale");
                if (hay != null) {
                    hay.setAmount(1);
                    return hay;
                }
            }
            if ("enchanted_nether_stalk".equals(normalized)) {
                ItemStack wart = itemService.createItemByKey("enchanted_nether_wart");
                if (wart != null) {
                    wart.setAmount(1);
                    return wart;
                }
            }
        }

        MinionManager minionManager = plugin.getMinionManager();
        if (minionManager != null) {
            ItemStack ingredient = minionManager.createIngredientItem(normalized, 1);
            if (ingredient != null) {
                ingredient.setAmount(1);
                return ingredient;
            }
        }

        Material material = materialFromKey(normalized);
        return new ItemStack(material == null ? Material.PAPER : material, 1);
    }

    private Material materialFromKey(String key) {
        for (Map.Entry<Material, String> entry : vanillaKeys.entrySet()) {
            if (entry.getValue().equals(key)) {
                return entry.getKey();
            }
        }
        return Material.matchMaterial(key.toUpperCase(Locale.ROOT));
    }

    private String prettyName(String key) {
        ItemStack stack = outputPreview(key);
        if (stack != null && stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
            String stripped = ChatColor.stripColor(stack.getItemMeta().getDisplayName());
            if (stripped != null && !stripped.isBlank()) {
                return stripped;
            }
        }
        String normalized = key == null ? "Unknown" : key.replace('_', ' ');
        String[] words = normalized.split("\\s+");
        List<String> parts = new ArrayList<>(words.length);
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            String lower = word.toLowerCase(Locale.ROOT);
            parts.add(Character.toUpperCase(lower.charAt(0)) + lower.substring(1));
        }
        return String.join(" ", parts);
    }

    private ItemStack glass(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack button(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String normalizeKey(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "carrot" -> "carrot_item";
            case "potato" -> "potato_item";
            case "enchanted_hay_bale" -> "enchanted_hay_block";
            case "enchanted_nether_wart" -> "enchanted_nether_stalk";
            default -> normalized;
        };
    }

    private Map<String, Rule> buildRules() {
        Map<String, Rule> rules = new LinkedHashMap<>();

        MinionManager minionManager = plugin.getMinionManager();
        if (minionManager != null) {
            for (MinionManager.SuperCompactorRuleInfo rule : minionManager.superCompactorRules()) {
                if (rule == null) {
                    continue;
                }
                putRule(rules, rule.inputId(), rule.inputAmount(), rule.outputId(), rule.outputAmount());
            }
        }

        // Fallback in case minion rules are unavailable.
        if (rules.isEmpty()) {
            for (EnchantedFarmItemType type : EnchantedFarmItemType.values()) {
                if (type.baseMaterial() != null) {
                    String input = vanillaKeysFromFarmMaterial(type.baseMaterial());
                    if (input == null) {
                        continue;
                    }
                    putRule(rules, input, 160, normalizeKey(type.id()), 1);
                    continue;
                }
                if (type.baseItemId() != null) {
                    putRule(rules, normalizeKey(type.baseItemId()), 160, normalizeKey(type.id()), 1);
                }
            }

            putRule(rules, "cobblestone", 160, "enchanted_cobblestone", 1);
            putRule(rules, "coal", 160, "enchanted_coal", 1);
            putRule(rules, "enchanted_coal", 160, "enchanted_coal_block", 1);
            putRule(rules, "iron_ingot", 160, "enchanted_iron", 1);
            putRule(rules, "enchanted_iron", 160, "enchanted_iron_block", 1);
            putRule(rules, "gold_ingot", 160, "enchanted_gold", 1);
            putRule(rules, "enchanted_gold", 160, "enchanted_gold_block", 1);
            putRule(rules, "redstone", 160, "enchanted_redstone", 1);
            putRule(rules, "enchanted_redstone", 160, "enchanted_redstone_block", 1);
            putRule(rules, "lapis_lazuli", 160, "enchanted_lapis_lazuli", 1);
            putRule(rules, "enchanted_lapis_lazuli", 160, "enchanted_lapis_lazuli_block", 1);
            putRule(rules, "diamond", 160, "enchanted_diamond", 1);
            putRule(rules, "enchanted_diamond", 160, "enchanted_diamond_block", 1);
            putRule(rules, "emerald", 160, "enchanted_emerald", 1);
            putRule(rules, "enchanted_emerald", 160, "enchanted_emerald_block", 1);
            putRule(rules, "mycelium", 160, "enchanted_mycelium", 1);
            putRule(rules, "golden_carrot", 160, "enchanted_golden_carrot", 1);
            putRule(rules, "sapphire", 160, "enchanted_sapphire", 1);
        }

        return Map.copyOf(rules);
    }

    private Map<String, Rule> indexRulesByOutput(Map<String, Rule> byInput) {
        Map<String, Rule> output = new LinkedHashMap<>();
        if (byInput == null || byInput.isEmpty()) {
            return Map.copyOf(output);
        }
        for (Rule rule : byInput.values()) {
            if (rule == null || rule.outputKey == null || rule.outputKey.isBlank()) {
                continue;
            }
            Rule existing = output.get(rule.outputKey);
            if (existing == null || rule.inputAmount < existing.inputAmount) {
                output.put(rule.outputKey, rule);
            }
        }
        return Map.copyOf(output);
    }

    private String vanillaKeysFromFarmMaterial(Material material) {
        if (material == null) {
            return null;
        }
        return switch (material) {
            case CARROT -> "carrot_item";
            case POTATO -> "potato_item";
            case NETHER_WART -> "nether_stalk";
            default -> vanillaKeys.get(material);
        };
    }

    private void putRule(Map<String, Rule> map, String input, int inputAmount, String output, int outputAmount) {
        String normalizedInput = normalizeKey(input);
        String normalizedOutput = normalizeKey(output);
        if (normalizedInput == null || normalizedOutput == null || inputAmount <= 0 || outputAmount <= 0) {
            return;
        }
        if (normalizedInput.equals(normalizedOutput)) {
            return;
        }
        map.putIfAbsent(normalizedInput, new Rule(normalizedInput, inputAmount, normalizedOutput, outputAmount));
    }

    private Map<Material, String> buildVanillaKeys() {
        Map<Material, String> map = new EnumMap<>(Material.class);
        map.put(Material.COBBLESTONE, "cobblestone");
        map.put(Material.COAL, "coal");
        map.put(Material.IRON_INGOT, "iron_ingot");
        map.put(Material.GOLD_INGOT, "gold_ingot");
        map.put(Material.REDSTONE, "redstone");
        map.put(Material.LAPIS_LAZULI, "lapis_lazuli");
        map.put(Material.DIAMOND, "diamond");
        map.put(Material.EMERALD, "emerald");
        map.put(Material.WHEAT, "wheat");
        map.put(Material.WHEAT_SEEDS, "wheat_seeds");
        map.put(Material.HAY_BLOCK, "hay_block");
        map.put(Material.CARROT, "carrot_item");
        map.put(Material.POTATO, "potato_item");
        map.put(Material.BAKED_POTATO, "baked_potato");
        map.put(Material.SUGAR_CANE, "sugar_cane");
        map.put(Material.NETHER_WART, "nether_stalk");
        map.put(Material.CACTUS, "cactus");
        map.put(Material.MELON_SLICE, "melon_slice");
        map.put(Material.PUMPKIN, "pumpkin");
        map.put(Material.COCOA_BEANS, "cocoa_beans");
        map.put(Material.RED_MUSHROOM, "red_mushroom");
        map.put(Material.BROWN_MUSHROOM, "brown_mushroom");
        map.put(Material.MYCELIUM, "mycelium");
        return Map.copyOf(map);
    }
}
