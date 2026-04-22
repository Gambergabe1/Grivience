package io.papermc.Grivience.auction;

import io.papermc.Grivience.GriviencePlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class AuctionManager {
    private final GriviencePlugin plugin;
    private final File dataFile;
    private final Map<UUID, AuctionItem> auctions = new HashMap<>();

    public AuctionManager(GriviencePlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "auctions.yml");
        load();
        
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tickAuctions, 20L, 20L);
    }

    private void tickAuctions() {
        boolean changed = false;
        for (AuctionItem item : auctions.values()) {
            if (item.getStatus() == AuctionItem.AuctionStatus.ACTIVE && item.isExpired()) {
                item.updateStatus();
                changed = true;
            }
        }
        if (changed) {
            save();
        }
    }

    public void addAuction(AuctionItem item) {
        auctions.put(item.getId(), item);
        save();
    }

    public AuctionItem getAuction(UUID id) {
        return auctions.get(id);
    }

    public List<AuctionItem> getActiveAuctions() {
        return auctions.values().stream()
                .filter(a -> a.getStatus() == AuctionItem.AuctionStatus.ACTIVE)
                .collect(Collectors.toList());
    }

    public List<AuctionItem> getPlayerAuctions(UUID playerId) {
        return auctions.values().stream()
                .filter(a -> a.getSeller().equals(playerId))
                .collect(Collectors.toList());
    }

    public List<AuctionItem> getPlayerBids(UUID playerId) {
        return auctions.values().stream()
                .filter(a -> a.getBids().stream().anyMatch(b -> b.bidder().equals(playerId)))
                .collect(Collectors.toList());
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (AuctionItem item : auctions.values()) {
            ConfigurationSection sec = yaml.createSection(item.getId().toString());
            sec.set("seller", item.getSeller().toString());
            sec.set("item", item.getItem());
            sec.set("bin", item.isBin());
            sec.set("startingBid", item.getStartingBid());
            sec.set("endTime", item.getEndTime());
            sec.set("status", item.getStatus().name());
            sec.set("sellerClaimed", item.isSellerClaimed());

            List<Map<String, Object>> bidList = new ArrayList<>();
            for (AuctionBid bid : item.getBids()) {
                Map<String, Object> b = new HashMap<>();
                b.put("bidder", bid.bidder().toString());
                b.put("bidderName", bid.bidderName());
                b.put("amount", bid.amount());
                b.put("timestamp", bid.timestamp());
                bidList.add(b);
            }
            sec.set("bids", bidList);
        }

        try {
            yaml.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save auctions.yml: " + e.getMessage());
        }
    }

    private synchronized void load() {
        if (!dataFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        
        for (String key : yaml.getKeys(false)) {
            ConfigurationSection sec = yaml.getConfigurationSection(key);
            if (sec == null) continue;

            try {
                UUID id = UUID.fromString(key);
                UUID seller = UUID.fromString(sec.getString("seller"));
                ItemStack item = sec.getItemStack("item");
                boolean bin = sec.getBoolean("bin");
                long startingBid = sec.getLong("startingBid");
                long endTime = sec.getLong("endTime");
                AuctionItem.AuctionStatus status = AuctionItem.AuctionStatus.valueOf(sec.getString("status", "ACTIVE"));
                boolean sellerClaimed = sec.getBoolean("sellerClaimed");

                List<AuctionBid> bids = new ArrayList<>();
                List<?> bidList = sec.getList("bids");
                if (bidList != null) {
                    for (Object obj : bidList) {
                        if (obj instanceof Map) {
                            Map<?, ?> map = (Map<?, ?>) obj;
                            UUID bidder = UUID.fromString((String) map.get("bidder"));
                            String bidderName = (String) map.get("bidderName");
                            long amount = ((Number) map.get("amount")).longValue();
                            long timestamp = ((Number) map.get("timestamp")).longValue();
                            bids.add(new AuctionBid(bidder, bidderName, amount, timestamp));
                        }
                    }
                }

                AuctionItem auction = new AuctionItem(id, seller, item, bin, startingBid, endTime, status, sellerClaimed, bids);
                auctions.put(id, auction);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load auction " + key + ": " + e.getMessage());
            }
        }
    }

    public void removeAuction(UUID id) {
        auctions.remove(id);
        save();
    }
}
