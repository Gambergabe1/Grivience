package io.papermc.Grivience.dungeon;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.io.File;

public final class RewardChestManager implements Listener {
    private static final int INVENTORY_SIZE = 27;
    private final GriviencePlugin plugin;
    private final NamespacedKey optionKey;
    private final Random random = new Random();
    private int optionsPerRun = 5;
    private List<RewardEntry> pool = List.of();
    private final File lootFile;

    public RewardChestManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.optionKey = new NamespacedKey(plugin, "reward-option");
        this.lootFile = new File(plugin.getDataFolder(), "loot.yml");
        reloadFromConfig();
    }

    public void reloadFromConfig() {
        ConfigurationSection root = loadLootSection();
        optionsPerRun = root == null ? 5 : Math.max(3, root.getInt("options-per-run", 5));
        List<RewardEntry> loaded = new ArrayList<>();
        if (root != null) {
            ConfigurationSection pools = root.getConfigurationSection("pools");
            if (pools != null) {
                for (String id : pools.getKeys(false)) {
                    ConfigurationSection section = pools.getConfigurationSection(id);
                    if (section == null) {
                        continue;
                    }
                    int weight = Math.max(1, section.getInt("weight", 1));
                    Material icon = Material.matchMaterial(section.getString("icon", "CHEST"));
                    if (icon == null) {
                        icon = Material.CHEST;
                    }
                    String name = ChatColor.translateAlternateColorCodes('&', section.getString("name", "&eReward"));
                    List<String> lore = colorize(section.getStringList("lore"));
                    List<String> commands = section.getStringList("commands");
                    if (commands.isEmpty()) {
                        continue;
                    }
                    loaded.add(new RewardEntry(id, weight, icon, name, lore, commands));
                }
            }
        }
        if (loaded.isEmpty()) {
            pool = defaultPool();
        } else {
            pool = List.copyOf(loaded);
        }
    }

    public boolean hasPool() {
        return !pool.isEmpty();
    }

    private ConfigurationSection loadLootSection() {
        if (lootFile.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(lootFile);
            return yaml.getConfigurationSection("dungeons.reward-chest");
        }
        return plugin.getConfig().getConfigurationSection("dungeons.reward-chest");
    }

    public void openRewardChest(Player player, String floorId, String grade, int score, Runnable afterClaim) {
        List<RewardEntry> options = rollOptions();
        RewardHolder holder = new RewardHolder(afterClaim, floorId, grade, score);
        Inventory inv = Bukkit.createInventory(holder, INVENTORY_SIZE, ChatColor.GOLD + "Dungeon Rewards");
        holder.inventory = inv;

        ItemStack filler = decorativePane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inv.setItem(i, filler);
        }
        int[] slots = {10, 11, 12, 14, 15, 16};
        Map<Integer, RewardEntry> bySlot = new HashMap<>();
        for (int i = 0; i < options.size() && i < slots.length; i++) {
            int slot = slots[i];
            RewardEntry entry = options.get(i);
            ItemStack item = entry.toItem(optionKey, floorId, grade, score);
            inv.setItem(slot, item);
            bySlot.put(slot, entry);
        }
        holder.bySlot = bySlot;
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.9F, 1.1F);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof RewardHolder holder)) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (!holder.bySlot.containsKey(slot)) {
            return;
        }
        RewardEntry entry = holder.bySlot.get(slot);
        executeCommands(player, entry, holder.floorId, holder.grade, holder.score);
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F);
        holder.claimed = true;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof RewardHolder holder)) {
            return;
        }
        if (holder.afterClaim != null) {
            Bukkit.getScheduler().runTask(plugin, holder.afterClaim);
        }
    }

    private void executeCommands(Player player, RewardEntry entry, String floorId, String grade, int score) {
        for (String command : entry.commands()) {
            String parsed = command
                    .replace("{player}", player.getName())
                    .replace("{floor}", floorId)
                    .replace("{grade}", grade)
                    .replace("{score}", Integer.toString(score));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
        }
    }

    private List<RewardEntry> rollOptions() {
        List<RewardEntry> rolled = new ArrayList<>();
        if (pool.isEmpty()) {
            return rolled;
        }
        int totalWeight = pool.stream().mapToInt(RewardEntry::weight).sum();
        for (int i = 0; i < optionsPerRun; i++) {
            int pick = random.nextInt(totalWeight);
            int running = 0;
            for (RewardEntry entry : pool) {
                running += entry.weight();
                if (pick < running) {
                    rolled.add(entry);
                    break;
                }
            }
        }
        return rolled;
    }

    private ItemStack decorativePane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.BLACK + " ");
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private List<String> colorize(List<String> input) {
        List<String> out = new ArrayList<>(input.size());
        for (String line : input) {
            out.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        return out;
    }

    private List<RewardEntry> defaultPool() {
        List<RewardEntry> defaults = new ArrayList<>();
        defaults.add(new RewardEntry("coins_small", 60, Material.EMERALD, ChatColor.GREEN + "15,000 Coins",
                List.of(ChatColor.GRAY + "Vault deposit"), List.of("eco give {player} 15000")));
        defaults.add(new RewardEntry("coins_large", 25, Material.EMERALD_BLOCK, ChatColor.GOLD + "50,000 Coins",
                List.of(ChatColor.GRAY + "Big coin pouch"), List.of("eco give {player} 50000")));
        defaults.add(new RewardEntry("essence", 35, Material.EXPERIENCE_BOTTLE, ChatColor.AQUA + "8 Dungeon Essence",
                List.of(ChatColor.GRAY + "XP levels for crafting"), List.of("xp give {player} 8 levels")));
        defaults.add(new RewardEntry("book", 20, Material.ENCHANTED_BOOK, ChatColor.LIGHT_PURPLE + "Reforge Stone",
                List.of(ChatColor.GRAY + "Random reforge stone"), List.of("reforge give {player} random")));
        defaults.add(new RewardEntry("gear", 15, Material.NETHERITE_SCRAP, ChatColor.DARK_RED + "Boss Relic Cache",
                List.of(ChatColor.GRAY + "High-tier material"), List.of("give {player} netherite_ingot 2")));
        return defaults;
    }

    private static final class RewardHolder implements InventoryHolder {
        private final Runnable afterClaim;
        private final String floorId;
        private final String grade;
        private final int score;
        private Inventory inventory;
        private Map<Integer, RewardEntry> bySlot = Map.of();
        private boolean claimed;

        private RewardHolder(Runnable afterClaim, String floorId, String grade, int score) {
            this.afterClaim = afterClaim;
            this.floorId = floorId;
            this.grade = grade;
            this.score = score;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    public record RewardEntry(String id, int weight, Material icon, String name, List<String> lore, List<String> commands) {
        ItemStack toItem(NamespacedKey optionKey, String floorId, String grade, int score) {
            ItemStack item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(name + ChatColor.GRAY + " | " + ChatColor.YELLOW + grade);
            List<String> viewLore = new ArrayList<>();
            if (lore != null && !lore.isEmpty()) {
                viewLore.addAll(lore);
            }
            viewLore.add("");
            viewLore.add(ChatColor.GRAY + "Click to claim");
            meta.setLore(viewLore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(optionKey, PersistentDataType.STRING, id.toLowerCase(Locale.ROOT));
            item.setItemMeta(meta);
            return item;
        }
    }
}
