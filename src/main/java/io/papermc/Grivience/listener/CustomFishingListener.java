package io.papermc.Grivience.listener;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.fishing.SeaCreatureManager;
import io.papermc.Grivience.item.CustomArmorManager;
import io.papermc.Grivience.item.CustomItemService;
import io.papermc.Grivience.pet.PetManager;
import io.papermc.Grivience.pet.PetStatBonuses;
import io.papermc.Grivience.skills.SkyblockSkill;
import io.papermc.Grivience.skills.SkyblockSkillManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Replaces vanilla fishing loot with configurable server-specific rewards.
 */
public final class CustomFishingListener implements Listener {
    private static final List<RewardEntry> COMMON_REWARDS = List.of(
            RewardEntry.material(Material.COD, 26, 1, 3),
            RewardEntry.material(Material.SALMON, 22, 1, 3),
            RewardEntry.material(Material.CLAY_BALL, 18, 2, 5),
            RewardEntry.material(Material.LILY_PAD, 14, 1, 2)
    );

    private static final List<RewardEntry> UNCOMMON_REWARDS = List.of(
            RewardEntry.material(Material.TROPICAL_FISH, 18, 1, 2),
            RewardEntry.material(Material.PUFFERFISH, 16, 1, 2),
            RewardEntry.material(Material.INK_SAC, 14, 1, 3),
            RewardEntry.material(Material.PRISMARINE_SHARD, 10, 1, 2)
    );

    private static final List<RewardEntry> RARE_REWARDS = List.of(
            RewardEntry.material(Material.PRISMARINE_CRYSTALS, 12, 1, 3),
            RewardEntry.material(Material.PRISMARINE_SHARD, 10, 2, 4),
            RewardEntry.custom("grappling_hook", 1, 1, 1)
    );

    private final GriviencePlugin plugin;
    private final SeaCreatureManager seaCreatureManager;

    public CustomFishingListener(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.seaCreatureManager = new SeaCreatureManager(plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCast(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.FISHING) {
            return;
        }

        Player player = event.getPlayer();
        FishHook hook = event.getHook();
        if (player == null || hook == null || !isEnabledFor(player.getWorld())) {
            return;
        }

        int baseMin = Math.max(20, plugin.getConfig().getInt("skyblock.custom-fishing.base-wait.min-ticks", 100));
        int baseMax = Math.max(baseMin, plugin.getConfig().getInt("skyblock.custom-fishing.base-wait.max-ticks", 260));
        double waitMultiplier = resolveWaitMultiplier(player);

        int minWait = Math.max(10, (int) Math.round(baseMin * waitMultiplier));
        int maxWait = Math.max(minWait, (int) Math.round(baseMax * waitMultiplier));

        hook.setApplyLure(false);
        hook.setSkyInfluenced(false);
        hook.setRainInfluenced(false);
        hook.setWaitTime(minWait, maxWait);
        hook.setLureTime(
                Math.max(5, plugin.getConfig().getInt("skyblock.custom-fishing.base-lure.min-ticks", 20)),
                Math.max(10, plugin.getConfig().getInt("skyblock.custom-fishing.base-lure.max-ticks", 45))
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCatch(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null || !isEnabledFor(player.getWorld())) {
            return;
        }
        if (!(event.getCaught() instanceof Item caughtItem)) {
            return;
        }

        FishHook hook = event.getHook();
        if (hook == null || !hook.getLocation().getBlock().isLiquid()) {
            return;
        }
        if (plugin.getConfig().getBoolean("skyblock.custom-fishing.require-open-water", true) && !hook.isInOpenWater()) {
            return;
        }

        if (ThreadLocalRandom.current().nextDouble() < 0.15) {
            caughtItem.remove();
            seaCreatureManager.spawnSeaCreature(hook.getLocation(), player);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_SPLASH, 1.0F, 1.0F);
            return;
        }

        RewardTier tier = rollTier(player, hook);
        ItemStack reward = rollReward(tier);
        if (reward == null || reward.getType().isAir()) {
            return;
        }

        caughtItem.setItemStack(reward);
        if (tier != RewardTier.COMMON) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5F, tier.pitch());
        }
        if (tier == RewardTier.RARE) {
            player.sendMessage(ChatColor.AQUA + "You reeled in a rare catch: " + readableName(reward) + ChatColor.AQUA + "!");
        }
    }

    private boolean isEnabledFor(World world) {
        if (world == null || !plugin.getConfig().getBoolean("skyblock.custom-fishing.enabled", true)) {
            return false;
        }

        Set<String> allowedWorlds = new LinkedHashSet<>();
        for (String configured : plugin.getConfig().getStringList("skyblock.custom-fishing.allowed-worlds")) {
            if (configured == null || configured.isBlank()) {
                continue;
            }
            String normalized = configured.trim();
            if (normalized.equalsIgnoreCase("hub")) {
                normalized = plugin.getConfig().getString("skyblock.hub-world", "world");
            } else if (normalized.equalsIgnoreCase("farmhub")) {
                normalized = plugin.getConfig().getString("skyblock.farmhub-world", "world");
            }
            allowedWorlds.add(normalized.toLowerCase(Locale.ROOT));
        }

        if (allowedWorlds.isEmpty()) {
            allowedWorlds.add(plugin.getConfig().getString("skyblock.hub-world", "world").toLowerCase(Locale.ROOT));
            allowedWorlds.add(plugin.getConfig().getString("skyblock.farmhub-world", "world").toLowerCase(Locale.ROOT));
        }

        return allowedWorlds.contains(world.getName().toLowerCase(Locale.ROOT));
    }

    private double resolveWaitMultiplier(Player player) {
        double multiplier = 1.0D;

        int lureLevel = player.getInventory().getItemInMainHand().getEnchantmentLevel(org.bukkit.enchantments.Enchantment.LURE);
        double lureReduction = plugin.getConfig().getDouble("skyblock.custom-fishing.speed.lure-level-reduction", 0.10D);
        multiplier -= Math.max(0.0D, lureLevel * lureReduction);

        int abyssalPieces = countArmorPieces(player, "abyssal_diver");
        if (abyssalPieces >= 2) {
            multiplier *= clamp(
                    plugin.getConfig().getDouble("skyblock.custom-fishing.speed.abyssal-diver-two-piece-multiplier", 0.80D),
                    0.25D,
                    1.0D
            );
        }

        return clamp(multiplier, 0.25D, 3.0D);
    }

    private RewardTier rollTier(Player player, FishHook hook) {
        double rareBoost = plugin.getConfig().getDouble("skyblock.custom-fishing.rare-loot.base-bonus", 0.0D);

        SkyblockSkillManager skillManager = plugin.getSkyblockSkillManager();
        if (skillManager != null) {
            rareBoost += skillManager.getPerkValue(player, SkyblockSkill.FISHING) / 100.0D;
        }

        PetManager petManager = plugin.getPetManager();
        if (petManager != null) {
            PetStatBonuses bonuses = petManager.equippedStatBonuses(player);
            rareBoost += bonuses.seaCreatureChance() / 200.0D;
        }

        if (countArmorPieces(player, "abyssal_diver") >= 4) {
            rareBoost += plugin.getConfig().getDouble("skyblock.custom-fishing.rare-loot.abyssal-diver-full-set-bonus", 0.08D);
        }

        World world = hook.getWorld();
        if (world != null && world.hasStorm()) {
            rareBoost += plugin.getConfig().getDouble("skyblock.custom-fishing.rare-loot.rain-bonus", 0.02D);
        }
        if (world != null && (world.getTime() >= 13000L || world.getTime() <= 1000L)) {
            rareBoost += plugin.getConfig().getDouble("skyblock.custom-fishing.rare-loot.night-bonus", 0.03D);
        }
        if (waterDepth(hook) >= plugin.getConfig().getInt("skyblock.custom-fishing.rare-loot.deep-water-threshold", 4)) {
            rareBoost += plugin.getConfig().getDouble("skyblock.custom-fishing.rare-loot.deep-water-bonus", 0.03D);
        }

        double[] weights = new double[] {58.0D, 28.0D, 14.0D};
        double modifier = clamp(rareBoost, 0.0D, 0.75D);
        weights[0] *= (1.0D - modifier * 0.65D);
        weights[1] *= (1.0D + modifier * 0.15D);
        weights[2] *= (1.0D + modifier * 0.80D);

        double total = 0.0D;
        for (double weight : weights) {
            total += weight;
        }

        double roll = ThreadLocalRandom.current().nextDouble(total);
        if (roll < weights[0]) {
            return RewardTier.COMMON;
        }
        roll -= weights[0];
        if (roll < weights[1]) {
            return RewardTier.UNCOMMON;
        }
        roll -= weights[1];
        return RewardTier.RARE;
    }

    private ItemStack rollReward(RewardTier tier) {
        List<RewardEntry> pool = switch (tier) {
            case COMMON -> COMMON_REWARDS;
            case UNCOMMON -> UNCOMMON_REWARDS;
            case RARE -> RARE_REWARDS;
        };

        RewardEntry entry = weightedEntry(pool);
        if (entry == null) {
            return null;
        }

        int amount = ThreadLocalRandom.current().nextInt(entry.minAmount(), entry.maxAmount() + 1);
        if (entry.customKey() != null) {
            CustomItemService itemService = plugin.getCustomItemService();
            ItemStack custom = itemService == null ? null : itemService.createItemByKey(entry.customKey());
            if (custom == null) {
                return null;
            }
            custom.setAmount(Math.min(amount, Math.max(1, custom.getMaxStackSize())));
            return custom;
        }

        Material material = entry.material();
        if (material == null) {
            return null;
        }
        return new ItemStack(material, Math.min(amount, Math.max(1, material.getMaxStackSize())));
    }

    private RewardEntry weightedEntry(List<RewardEntry> pool) {
        if (pool == null || pool.isEmpty()) {
            return null;
        }

        int totalWeight = 0;
        for (RewardEntry entry : pool) {
            totalWeight += Math.max(1, entry.weight());
        }

        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        for (RewardEntry entry : pool) {
            roll -= Math.max(1, entry.weight());
            if (roll < 0) {
                return entry;
            }
        }
        return pool.getLast();
    }

    private int countArmorPieces(Player player, String setId) {
        if (player == null || setId == null || setId.isBlank()) {
            return 0;
        }

        CustomArmorManager armorManager = plugin.getCustomArmorManager();
        if (armorManager == null) {
            return 0;
        }

        int pieces = 0;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece == null || piece.getType().isAir()) {
                continue;
            }
            String pieceSetId = armorManager.getArmorSetId(piece);
            if (pieceSetId != null && pieceSetId.equalsIgnoreCase(setId)) {
                pieces++;
            }
        }
        return pieces;
    }

    private int waterDepth(FishHook hook) {
        if (hook == null || hook.getWorld() == null) {
            return 0;
        }

        Block block = hook.getLocation().getBlock();
        if (!block.isLiquid()) {
            block = block.getRelative(0, -1, 0);
        }

        int depth = 0;
        while (depth < 12 && block.isLiquid()) {
            depth++;
            block = block.getRelative(0, -1, 0);
        }
        return depth;
    }

    private double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private String readableName(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return ChatColor.GRAY + "Nothing";
        }
        if (stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()) {
            return stack.getItemMeta().getDisplayName();
        }

        String[] parts = stack.getType().name().toLowerCase(Locale.ROOT).split("_");
        List<String> words = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            words.add(Character.toUpperCase(part.charAt(0)) + part.substring(1));
        }
        return ChatColor.WHITE + String.join(" ", words);
    }

    private enum RewardTier {
        COMMON(1.0F),
        UNCOMMON(1.2F),
        RARE(1.45F);

        private final float pitch;

        RewardTier(float pitch) {
            this.pitch = pitch;
        }

        public float pitch() {
            return pitch;
        }
    }

    private record RewardEntry(Material material, String customKey, int weight, int minAmount, int maxAmount) {
        private static RewardEntry material(Material material, int weight, int minAmount, int maxAmount) {
            return new RewardEntry(material, null, weight, minAmount, maxAmount);
        }

        private static RewardEntry custom(String customKey, int weight, int minAmount, int maxAmount) {
            return new RewardEntry(null, customKey, weight, minAmount, maxAmount);
        }
    }
}
