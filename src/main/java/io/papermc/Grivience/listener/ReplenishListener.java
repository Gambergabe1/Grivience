package io.papermc.Grivience.listener;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.enchantment.EnchantmentRegistry;
import io.papermc.Grivience.enchantment.SkyblockEnchantStorage;
import io.papermc.Grivience.enchantment.SkyblockEnchantment;
import io.papermc.Grivience.item.CustomArmorManager;
import io.papermc.Grivience.util.CropReplantUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

public final class ReplenishListener implements Listener {
    private static final String FARMHUB_AREA_BASE = "skyblock.farmhub-crop-area.";
    private static final String HUB_AREA_BASE = "skyblock.hub-crop-area.";

    private final GriviencePlugin plugin;
    private final SkyblockEnchantStorage enchantStorage;

    public ReplenishListener(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.enchantStorage = new SkyblockEnchantStorage(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCropDrop(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        Location location = event.getBlock().getLocation();
        if (isManagedCropArea(location)) {
            return;
        }

        Material cropType = event.getBlockState().getType();
        BlockData cropData = event.getBlockState().getBlockData();
        if (!CropReplantUtil.isReplantable(cropType) || !CropReplantUtil.isMature(cropType, cropData)) {
            return;
        }
        if (!hasActiveReplenish(player)) {
            return;
        }

        Material cost = CropReplantUtil.replantCost(cropType);
        if (cost == null) {
            return;
        }
        boolean paid = CropReplantUtil.removeOneFromDropEntities(event.getItems(), cost)
                || CropReplantUtil.removeOneFromInventory(player, cost);
        if (!paid) {
            return;
        }

        BlockData seedling = CropReplantUtil.seedlingData(cropType, cropData);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Block current = location.getBlock();
            if (!current.getType().isAir()) {
                return;
            }
            current.setBlockData(seedling, false);
        });
    }

    private boolean hasActiveReplenish(Player player) {
        if (player == null) {
            return false;
        }

        if (hasReplenishEnchant(player.getInventory().getItemInMainHand())) {
            return true;
        }
        return hasHarvesterInstantReplant(player);
    }

    private boolean hasReplenishEnchant(ItemStack item) {
        SkyblockEnchantment replenish = EnchantmentRegistry.get("replenish");
        return replenish != null && enchantStorage.getLevel(item, replenish) > 0;
    }

    private boolean hasHarvesterInstantReplant(Player player) {
        CustomArmorManager armorManager = plugin.getCustomArmorManager();
        if (armorManager == null || player == null) {
            return false;
        }

        int pieces = 0;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece == null) {
                continue;
            }
            if ("harvester_embrace".equalsIgnoreCase(armorManager.getArmorSetId(piece))) {
                pieces++;
            }
        }
        return pieces >= 4;
    }

    private boolean isManagedCropArea(Location location) {
        return isWithinArea(location, FARMHUB_AREA_BASE, "skyblock.farmhub-world")
                || isWithinArea(location, HUB_AREA_BASE, "skyblock.hub-world");
    }

    private boolean isWithinArea(Location location, String areaBase, String worldKey) {
        if (location == null || location.getWorld() == null || areaBase == null || areaBase.isBlank()) {
            return false;
        }
        if (!plugin.getConfig().getBoolean(areaBase + "enabled", false)) {
            return false;
        }

        String configuredWorld = plugin.getConfig().getString(areaBase + "world");
        if (configuredWorld == null || configuredWorld.isBlank()) {
            configuredWorld = plugin.getConfig().getString(worldKey, "world");
        }
        if (configuredWorld == null || !location.getWorld().getName().equalsIgnoreCase(configuredWorld)) {
            return false;
        }

        int minX = Math.min(plugin.getConfig().getInt(areaBase + "pos1.x"), plugin.getConfig().getInt(areaBase + "pos2.x"));
        int maxX = Math.max(plugin.getConfig().getInt(areaBase + "pos1.x"), plugin.getConfig().getInt(areaBase + "pos2.x"));
        int minY = Math.min(plugin.getConfig().getInt(areaBase + "pos1.y"), plugin.getConfig().getInt(areaBase + "pos2.y"));
        int maxY = Math.max(plugin.getConfig().getInt(areaBase + "pos1.y"), plugin.getConfig().getInt(areaBase + "pos2.y"));
        int minZ = Math.min(plugin.getConfig().getInt(areaBase + "pos1.z"), plugin.getConfig().getInt(areaBase + "pos2.z"));
        int maxZ = Math.max(plugin.getConfig().getInt(areaBase + "pos1.z"), plugin.getConfig().getInt(areaBase + "pos2.z"));

        return location.getBlockX() >= minX && location.getBlockX() <= maxX
                && location.getBlockY() >= minY && location.getBlockY() <= maxY
                && location.getBlockZ() >= minZ && location.getBlockZ() <= maxZ;
    }
}
