package io.papermc.Grivience.auction.gui;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.auction.AuctionBid;
import io.papermc.Grivience.auction.AuctionCategory;
import io.papermc.Grivience.auction.AuctionItem;
import io.papermc.Grivience.auction.AuctionManager;
import io.papermc.Grivience.skyblock.profile.SkyBlockProfile;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class AuctionGuiManager implements Listener {

    private final GriviencePlugin plugin;
    private final AuctionManager auctionManager;

    public AuctionGuiManager(GriviencePlugin plugin, AuctionManager auctionManager) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
    }

    public void openMainMenu(Player player) {
        AuctionMainMenuGui gui = new AuctionMainMenuGui(player);
        player.openInventory(gui.getInventory());
    }

    public void openBrowser(Player player) {
        AuctionBrowserGui gui = new AuctionBrowserGui(player, auctionManager);
        player.openInventory(gui.getInventory());
    }

    public void openBrowser(Player player, AuctionCategory category) {
        AuctionBrowserGui gui = new AuctionBrowserGui(player, auctionManager);
        gui.setCategoryFilter(category);
        player.openInventory(gui.getInventory());
    }

    public void openInspect(Player player, AuctionItem item) {
        AuctionInspectGui gui = new AuctionInspectGui(player, item);
        player.openInventory(gui.getInventory());
    }

    public void openCreate(Player player) {
        AuctionCreateGui gui = new AuctionCreateGui(player);
        player.openInventory(gui.getInventory());
    }

    public void openManage(Player player) {
        AuctionManageGui gui = new AuctionManageGui(player, auctionManager);
        player.openInventory(gui.getInventory());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        InventoryHolder holder = event.getInventory().getHolder();
        
        if (holder instanceof AuctionMainMenuGui) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 11) openBrowser(player, AuctionCategory.WEAPONS);
            else if (slot == 12) openBrowser(player, AuctionCategory.ARMOR);
            else if (slot == 13) openBrowser(player, AuctionCategory.ACCESSORIES);
            else if (slot == 14) openBrowser(player, AuctionCategory.CONSUMABLES);
            else if (slot == 15) openBrowser(player, AuctionCategory.BLOCKS);
            else if (slot == 16) openBrowser(player, AuctionCategory.TOOLS_MISC);
            else if (slot == 31) openBrowser(player);
            else if (slot == 50) openManage(player);
            else if (slot == 49) player.closeInventory();
        }
        else if (holder instanceof AuctionBrowserGui gui) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            
            if (slot == 1) gui.cycleSort();
            else if (slot == 3) gui.cycleType();
            else if (slot == 7) gui.cycleCategory();
            else if (slot == 48) {
                if (gui.getPage() > 0) {
                    gui.setPage(gui.getPage() - 1);
                }
            } else if (slot == 50) {
                if ((gui.getPage() + 1) * 28 < gui.getFilteredAuctions().size()) {
                    gui.setPage(gui.getPage() + 1);
                }
            } else if (slot == 49) {
                openMainMenu(player);
            } else if (slot >= 10 && slot < 44 && slot % 9 != 0 && slot % 9 != 8) {
                // Clicked an auction
                ItemStack clicked = event.getCurrentItem();
                if (clicked != null && clicked.getType() != Material.AIR && clicked.getType() != Material.GRAY_STAINED_GLASS_PANE) {
                    int listIndex = gui.getPage() * 28 + getIndexFromSlot(slot);
                    List<AuctionItem> auctions = gui.getFilteredAuctions();
                    if (listIndex >= 0 && listIndex < auctions.size()) {
                        openInspect(player, auctions.get(listIndex));
                    }
                }
            }
        } 
        else if (holder instanceof AuctionInspectGui gui) {
            event.setCancelled(true);
            if (event.getRawSlot() == 49) {
                openBrowser(player);
            } else if (event.getRawSlot() == 31) {
                AuctionItem item = gui.getAuction();
                if (item.getStatus() != AuctionItem.AuctionStatus.ACTIVE) {
                    player.sendMessage(ChatColor.RED + "This auction is no longer active!");
                    openBrowser(player);
                    return;
                }
                if (item.getSeller().equals(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + "You cannot bid on your own auction!");
                    return;
                }

                SkyBlockProfile profile = plugin.getProfileManager().getSelectedProfile(player);
                if (profile == null) return;

                if (item.isBin()) {
                    long cost = item.getStartingBid();
                    if (profile.getPurse() >= cost) {
                        profile.setPurse(profile.getPurse() - cost);
                        item.setStatus(AuctionItem.AuctionStatus.SOLD);
                        player.getInventory().addItem(item.getItem());
                        player.sendMessage(ChatColor.GREEN + "You bought " + item.getItem().getType() + " for " + cost + " coins!");
                        auctionManager.save();
                        openBrowser(player);
                    } else {
                        player.sendMessage(ChatColor.RED + "You do not have enough coins!");
                    }
                } else {
                    long currentBid = item.getHighestBid() != null ? item.getHighestBid().amount() : 0;
                    long nextBid = currentBid == 0 ? item.getStartingBid() : (long) (currentBid * 1.1);
                    if (currentBid == nextBid) nextBid += 10;

                    if (profile.getPurse() >= nextBid) {
                        // Refund previous bidder if online
                        if (item.getHighestBid() != null) {
                            Player prevBidder = plugin.getServer().getPlayer(item.getHighestBid().bidder());
                            if (prevBidder != null) {
                                SkyBlockProfile prevProfile = plugin.getProfileManager().getSelectedProfile(prevBidder);
                                if (prevProfile != null) {
                                    prevProfile.setPurse(prevProfile.getPurse() + currentBid);
                                    prevBidder.sendMessage(ChatColor.YELLOW + "You were outbid on an auction! " + currentBid + " coins were returned to your purse.");
                                }
                            }
                        }

                        profile.setPurse(profile.getPurse() - nextBid);
                        item.addBid(new AuctionBid(player.getUniqueId(), player.getName(), nextBid, System.currentTimeMillis()));
                        player.sendMessage(ChatColor.GREEN + "You placed a bid of " + nextBid + " coins!");
                        auctionManager.save();
                        gui.update(); // refresh view
                    } else {
                        player.sendMessage(ChatColor.RED + "You do not have enough coins!");
                    }
                }
            }
        }
        else if (holder instanceof AuctionManageGui gui) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 49) {
                openMainMenu(player);
            } else if (slot == 48) {
                openCreate(player);
            } else {
                int[] slots = {20, 21, 22, 23, 24, 29, 30, 31, 32, 33};
                int listIndex = -1;
                for (int i = 0; i < slots.length; i++) {
                    if (slots[i] == slot) {
                        listIndex = i;
                        break;
                    }
                }
                
                if (listIndex != -1) {
                    List<AuctionItem> myAuctions = gui.getMyAuctions();
                    if (listIndex >= 0 && listIndex < myAuctions.size()) {
                        AuctionItem item = myAuctions.get(listIndex);
                        if (item.getSeller().equals(player.getUniqueId()) && !item.isSellerClaimed()) {
                            if (item.getStatus() == AuctionItem.AuctionStatus.SOLD || (item.getStatus() == AuctionItem.AuctionStatus.EXPIRED && !item.getBids().isEmpty())) {
                                long reward = item.getHighestBid() != null ? item.getHighestBid().amount() : item.getStartingBid();
                                SkyBlockProfile profile = plugin.getProfileManager().getSelectedProfile(player);
                                if (profile != null) {
                                    profile.setPurse(profile.getPurse() + reward);
                                    item.setSellerClaimed(true);
                                    player.sendMessage(ChatColor.GREEN + "Claimed " + reward + " coins from auction!");
                                    auctionManager.save();
                                    gui.update();
                                }
                            } else if (item.getStatus() == AuctionItem.AuctionStatus.EXPIRED && item.getBids().isEmpty()) {
                                player.getInventory().addItem(item.getItem());
                                item.setSellerClaimed(true);
                                player.sendMessage(ChatColor.GREEN + "Claimed returned item from expired auction!");
                                auctionManager.save();
                                gui.update();
                            }
                        }
                    }
                }
            }
        }
        else if (holder instanceof AuctionCreateGui gui) {
            if (event.getRawSlot() < event.getInventory().getSize()) {
                event.setCancelled(true);
                int slot = event.getRawSlot();
                
                if (slot == 13 && gui.getItemToAuction() != null) {
                    player.getInventory().addItem(gui.getItemToAuction());
                    gui.setItemToAuction(null);
                } else if (slot == 29) {
                    long delta = event.isShiftClick() ? 10000 : 500;
                    gui.changePrice(event.getClick().isRightClick() ? -delta : delta);
                } else if (slot == 31) {
                    long delta = event.isShiftClick() ? 24 : 1;
                    gui.changeDuration(event.getClick().isRightClick() ? -delta : delta);
                } else if (slot == 33) {
                    gui.toggleType();
                } else if (slot == 48) {
                    if (gui.getItemToAuction() == null) {
                        player.sendMessage(ChatColor.RED + "You must place an item to auction!");
                        return;
                    }
                    long durationMs = gui.getDurationHours() * 60 * 60 * 1000;
                    AuctionItem newAuction = new AuctionItem(player.getUniqueId(), gui.getItemToAuction(), gui.isBin(), gui.getPrice(), durationMs);
                    auctionManager.addAuction(newAuction);
                    gui.setItemToAuction(null);
                    player.sendMessage(ChatColor.GREEN + "Auction created successfully!");
                    openMainMenu(player);
                } else if (slot == 49) {
                    openMainMenu(player);
                }
            } else {
                // Clicked bottom inventory, put item into slot 13
                if (event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                    event.setCancelled(true);
                    if (gui.getItemToAuction() == null) {
                        gui.setItemToAuction(event.getCurrentItem().clone());
                        event.setCurrentItem(null);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof AuctionCreateGui gui) {
            if (gui.getItemToAuction() != null) {
                event.getPlayer().getInventory().addItem(gui.getItemToAuction());
            }
        }
    }

    private int getIndexFromSlot(int slot) {
        int row = (slot - 10) / 9;
        int col = (slot - 10) % 9;
        return row * 7 + col;
    }
}
