package io.papermc.Grivience.gui;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.dungeon.DungeonManager;
import io.papermc.Grivience.dungeon.FloorConfig;
import io.papermc.Grivience.party.Party;
import io.papermc.Grivience.party.PartyManager;
import io.papermc.Grivience.party.PartyManager.PartySnapshot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DungeonGuiManager implements Listener {
    private static final String TITLE_MAIN = ChatColor.DARK_GREEN + "Dungeon Nexus";
    private static final String TITLE_FLOORS = ChatColor.DARK_GREEN + "Available Dungeons";
    private static final String TITLE_PARTY_FINDER = ChatColor.DARK_GREEN + "Party Finder";
    private static final String TITLE_PARTY_INVITES = ChatColor.DARK_GREEN + "Invite Players";
    private static final long REQUEST_COOLDOWN_MILLIS = 15000L;

    private final GriviencePlugin plugin;
    private final PartyManager partyManager;
    private final DungeonManager dungeonManager;

    private final NamespacedKey actionKey;
    private final NamespacedKey valueKey;
    private final Map<String, Long> requestCooldownByPair = new ConcurrentHashMap<>();

    public DungeonGuiManager(GriviencePlugin plugin, PartyManager partyManager, DungeonManager dungeonManager) {
        this.plugin = plugin;
        this.partyManager = partyManager;
        this.dungeonManager = dungeonManager;
        this.actionKey = new NamespacedKey(plugin, "gui-action");
        this.valueKey = new NamespacedKey(plugin, "gui-value");
    }

    public void openMainMenu(Player player) {
        MenuHolder holder = new MenuHolder(MenuType.MAIN);
        Inventory inventory = Bukkit.createInventory(holder, 27, TITLE_MAIN);
        holder.inventory = inventory;

        fillInventory(inventory, decorativePane(Material.BLACK_STAINED_GLASS_PANE));
        for (int slot : List.of(1, 3, 5, 7, 19, 21, 23, 25)) {
            inventory.setItem(slot, decorativePane(Material.GREEN_STAINED_GLASS_PANE));
        }

        inventory.setItem(4, taggedItem(
                Material.NETHER_STAR,
                ChatColor.GREEN + "Temple Command",
                List.of(
                        ChatColor.GRAY + "Choose where to head next:",
                        ChatColor.GRAY + "start a run or find allies."
                ),
                "noop",
                "",
                true
        ));
        inventory.setItem(11, taggedItem(
                Material.ENCHANTED_BOOK,
                ChatColor.GOLD + "Browse Floors",
                List.of(
                        ChatColor.GRAY + "View all available dungeon floors.",
                        "",
                        ChatColor.YELLOW + "Click to open the floor browser"
                ),
                "open_floors",
                "",
                true
        ));
        inventory.setItem(15, taggedItem(
                Material.NETHER_STAR,
                ChatColor.AQUA + "Party Finder",
                List.of(
                        ChatColor.GRAY + "Browse active public parties.",
                        "",
                        ChatColor.YELLOW + "Click to open party listings"
                ),
                "open_party_finder",
                "",
                true
        ));
        inventory.setItem(13, taggedItem(
                Material.TOTEM_OF_UNDYING,
                ChatColor.GREEN + "Create New Party",
                List.of(
                        ChatColor.GRAY + "Create your own party instantly.",
                        "",
                        ChatColor.YELLOW + "Click to become party leader"
                ),
                "create_party",
                "",
                true
        ));
        inventory.setItem(22, playerStatusItem(player));
        inventory.setItem(26, taggedItem(
                Material.BARRIER,
                ChatColor.RED + "Close",
                List.of(ChatColor.GRAY + "Close this menu."),
                "close_menu",
                ""
        ));
        player.openInventory(inventory);
    }

    public void openFloorsMenu(Player player) {
        MenuHolder holder = new MenuHolder(MenuType.FLOORS);
        Inventory inventory = Bukkit.createInventory(holder, 54, TITLE_FLOORS);
        holder.inventory = inventory;

        fillInventory(inventory, decorativePane(Material.BLACK_STAINED_GLASS_PANE));

        int slot = 0;
        for (FloorConfig floor : dungeonManager.floors()) {
            if (slot >= 45) {
                break;
            }
            inventory.setItem(slot++, floorCard(floor));
        }

        fillRange(inventory, 45, 53, decorativePane(Material.GRAY_STAINED_GLASS_PANE));
        inventory.setItem(46, taggedItem(
                Material.FILLED_MAP,
                ChatColor.GOLD + "Floor Browser",
                List.of(
                        ChatColor.GRAY + "Configured Floors: " + ChatColor.YELLOW + dungeonManager.floors().size(),
                        ChatColor.GRAY + "Tip: review boss info before queueing."
                ),
                "noop",
                "",
                true
        ));
        inventory.setItem(45, taggedItem(
                Material.ARROW,
                ChatColor.YELLOW + "Back",
                List.of(ChatColor.GRAY + "Return to main menu"),
                "open_main",
                ""
        ));
        inventory.setItem(47, taggedItem(
                Material.NETHER_STAR,
                ChatColor.AQUA + "Party Finder",
                List.of(ChatColor.GRAY + "Switch to active party listings."),
                "open_party_finder",
                ""
        ));
        inventory.setItem(49, taggedItem(
                Material.CLOCK,
                ChatColor.AQUA + "Refresh Floors",
                List.of(ChatColor.GRAY + "Reload this floor list view."),
                "refresh_floors",
                ""
        ));
        inventory.setItem(51, taggedItem(
                Material.EMERALD,
                ChatColor.GREEN + "Create Party",
                List.of(ChatColor.GRAY + "Create your own party from here."),
                "create_party",
                ""
        ));
        inventory.setItem(53, taggedItem(
                Material.BARRIER,
                ChatColor.RED + "Close",
                List.of(ChatColor.GRAY + "Close this menu."),
                "close_menu",
                ""
        ));
        player.openInventory(inventory);
    }

    public void openPartyFinder(Player player) {
        MenuHolder holder = new MenuHolder(MenuType.PARTY_FINDER);
        Inventory inventory = Bukkit.createInventory(holder, 54, TITLE_PARTY_FINDER);
        holder.inventory = inventory;

        fillInventory(inventory, decorativePane(Material.BLACK_STAINED_GLASS_PANE));

        List<PartySnapshot> snapshots = new ArrayList<>();
        for (PartySnapshot snapshot : partyManager.partyFinderSnapshots()) {
            if (!dungeonManager.isInDungeon(snapshot.leaderId())) {
                snapshots.add(snapshot);
            }
        }

        int slot = 0;
        for (PartySnapshot snapshot : snapshots) {
            if (slot >= 45) {
                break;
            }
            inventory.setItem(slot++, partySnapshotItem(snapshot));
        }

        if (snapshots.isEmpty()) {
            inventory.setItem(22, taggedItem(
                    Material.BARRIER,
                    ChatColor.RED + "No Parties Found",
                    List.of(
                            ChatColor.GRAY + "Nobody is advertising a party right now.",
                            "",
                            ChatColor.YELLOW + "Click " + ChatColor.GREEN + "Create Party" + ChatColor.YELLOW + " below to start one."
                    ),
                    "noop",
                    "",
                    true
            ));
        }

        fillRange(inventory, 45, 53, decorativePane(Material.GRAY_STAINED_GLASS_PANE));
        inventory.setItem(46, taggedItem(
                Material.PAPER,
                ChatColor.GOLD + "Party Finder Tips",
                List.of(
                        ChatColor.GRAY + "Click a party head to send an invite request.",
                        ChatColor.GRAY + "Requests have a short per-party cooldown."
                ),
                "noop",
                "",
                true
        ));
        inventory.setItem(45, taggedItem(
                Material.ARROW,
                ChatColor.YELLOW + "Back",
                List.of(ChatColor.GRAY + "Return to main menu"),
                "open_main",
                ""
        ));
        inventory.setItem(47, taggedItem(
                Material.ENCHANTED_BOOK,
                ChatColor.GOLD + "Browse Floors",
                List.of(ChatColor.GRAY + "Switch to the floor browser."),
                "open_floors",
                ""
        ));
        inventory.setItem(49, taggedItem(
                Material.EMERALD,
                ChatColor.GREEN + "Create Party",
                List.of(ChatColor.GRAY + "Create your own party"),
                "create_party",
                ""
        ));
        inventory.setItem(50, taggedItem(
                Material.NAME_TAG,
                ChatColor.AQUA + "Invite Players",
                List.of(ChatColor.GRAY + "Open player invite list."),
                "open_party_invites",
                ""
        ));
        inventory.setItem(51, taggedItem(
                Material.NETHER_STAR,
                ChatColor.AQUA + "Refresh",
                List.of(ChatColor.GRAY + "Reload party list"),
                "open_party_finder",
                ""
        ));
        inventory.setItem(53, taggedItem(
                Material.BARRIER,
                ChatColor.RED + "Close",
                List.of(ChatColor.GRAY + "Close this menu."),
                "close_menu",
                ""
        ));
        player.openInventory(inventory);
    }

    public void openPartyInvitesMenu(Player player) {
        MenuHolder holder = new MenuHolder(MenuType.PARTY_INVITES);
        Inventory inventory = Bukkit.createInventory(holder, 54, TITLE_PARTY_INVITES);
        holder.inventory = inventory;

        fillInventory(inventory, decorativePane(Material.BLACK_STAINED_GLASS_PANE));

        Party party = partyManager.getParty(player.getUniqueId());
        boolean hasParty = party != null;
        boolean isLeader = hasParty && party.isLeader(player.getUniqueId());
        boolean inDungeon = dungeonManager.isInDungeon(player.getUniqueId());
        boolean partyFull = hasParty && party.size() >= partyManager.maxPartySize();

        List<Player> candidates = inviteCandidates(player);
        int slot = 0;
        if (!inDungeon && (!hasParty || isLeader) && !partyFull) {
            for (Player candidate : candidates) {
                if (slot >= 45) {
                    break;
                }
                inventory.setItem(slot++, inviteTargetItem(candidate));
            }
        }

        if (slot == 0) {
            inventory.setItem(22, inviteStatusItem(hasParty, isLeader, inDungeon, partyFull));
        }

        fillRange(inventory, 45, 53, decorativePane(Material.GRAY_STAINED_GLASS_PANE));
        inventory.setItem(45, taggedItem(
                Material.ARROW,
                ChatColor.YELLOW + "Back",
                List.of(ChatColor.GRAY + "Return to Party Finder."),
                "open_party_finder",
                ""
        ));
        inventory.setItem(47, taggedItem(
                Material.EMERALD,
                ChatColor.GREEN + "Create Party",
                List.of(ChatColor.GRAY + "Create a party if you do not have one."),
                "create_party",
                ""
        ));
        inventory.setItem(49, taggedItem(
                Material.CLOCK,
                ChatColor.AQUA + "Refresh Players",
                List.of(ChatColor.GRAY + "Reload invite candidates."),
                "open_party_invites",
                ""
        ));
        inventory.setItem(53, taggedItem(
                Material.BARRIER,
                ChatColor.RED + "Close",
                List.of(ChatColor.GRAY + "Close this menu."),
                "close_menu",
                ""
        ));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MenuHolder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() == null || event.getClickedInventory().getType() == InventoryType.PLAYER) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) {
            return;
        }
        ItemMeta meta = clicked.getItemMeta();
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if (action == null) {
            return;
        }
        String value = meta.getPersistentDataContainer().getOrDefault(valueKey, PersistentDataType.STRING, "");

        switch (action) {
            case "open_main" -> {
                playUiClick(player);
                openMainMenu(player);
            }
            case "open_floors" -> {
                playUiClick(player);
                openFloorsMenu(player);
            }
            case "refresh_floors" -> {
                playUiClick(player);
                openFloorsMenu(player);
            }
            case "open_party_finder" -> {
                playUiClick(player);
                openPartyFinder(player);
            }
            case "open_party_invites" -> {
                playUiClick(player);
                openPartyInvitesMenu(player);
            }
            case "create_party" -> {
                String error = partyManager.createParty(player);
                if (error != null) {
                    playUiError(player);
                    player.sendMessage(ChatColor.RED + error);
                } else {
                    playUiSuccess(player);
                    player.sendMessage(ChatColor.GREEN + "Party created.");
                    openPartyFinder(player);
                }
            }
            case "start_floor" -> {
                String error = dungeonManager.startDungeon(player, value);
                if (error != null) {
                    playUiError(player);
                    player.sendMessage(ChatColor.RED + error);
                } else {
                    playUiSuccess(player);
                    player.closeInventory();
                    player.sendMessage(ChatColor.GREEN + "Dungeon run started.");
                }
            }
            case "request_party" -> {
                requestPartyInvite(player, value);
                openPartyFinder(player);
            }
            case "invite_player" -> {
                invitePlayerFromGui(player, value);
                openPartyInvitesMenu(player);
            }
            case "close_menu" -> {
                playUiClick(player);
                player.closeInventory();
            }
            case "noop" -> player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5F, 0.8F);
            default -> {
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    private ItemStack partySnapshotItem(PartySnapshot snapshot) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta baseMeta = item.getItemMeta();
        if (baseMeta instanceof SkullMeta skullMeta) {
            OfflinePlayer leader = Bukkit.getOfflinePlayer(snapshot.leaderId());
            skullMeta.setOwningPlayer(leader);
            item.setItemMeta(skullMeta);
        }

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Leader: " + ChatColor.GOLD + snapshot.leaderName());
        lore.add(ChatColor.GRAY + "Members: " + ChatColor.AQUA + snapshot.size() + "/" + snapshot.maxSize());
        lore.add(ChatColor.GRAY + "Open Slots: " + ChatColor.GREEN + Math.max(0, snapshot.maxSize() - snapshot.size()));
        lore.add(ChatColor.GRAY + "Occupancy: " + occupancyBar(snapshot.size(), snapshot.maxSize()));
        lore.add("");
        lore.add(ChatColor.YELLOW + "Party Members:");

        List<String> names = snapshot.memberNames();
        int shown = Math.min(4, names.size());
        for (int i = 0; i < shown; i++) {
            lore.add(ChatColor.DARK_GRAY + " - " + ChatColor.WHITE + names.get(i));
        }
        if (names.size() > shown) {
            lore.add(ChatColor.DARK_GRAY + " - " + ChatColor.GRAY + "+" + (names.size() - shown) + " more");
        }
        lore.add("");
        lore.add(ChatColor.GREEN + "Click to request an invite");

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + snapshot.leaderName() + "'s Party");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "request_party");
        meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, snapshot.leaderId().toString());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack taggedItem(Material type, String name, List<String> lore, String action, String value) {
        return taggedItem(type, name, lore, action, value, false);
    }

    private ItemStack taggedItem(
            Material type,
            String name,
            List<String> lore,
            String action,
            String value,
            boolean glow
    ) {
        ItemStack item = new ItemStack(type);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        if (glow) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        }
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, value);
        item.setItemMeta(meta);
        return item;
    }

    private void requestPartyInvite(Player requester, String leaderIdRaw) {
        UUID leaderId;
        try {
            leaderId = UUID.fromString(leaderIdRaw);
        } catch (IllegalArgumentException ignored) {
            requester.sendMessage(ChatColor.RED + "That party listing is invalid.");
            return;
        }

        if (partyManager.isInParty(requester.getUniqueId())) {
            requester.sendMessage(ChatColor.RED + "You are already in a party.");
            return;
        }

        Party party = partyManager.getParty(leaderId);
        if (party == null) {
            requester.sendMessage(ChatColor.RED + "That party no longer exists.");
            return;
        }
        if (party.size() >= partyManager.maxPartySize()) {
            requester.sendMessage(ChatColor.RED + "That party is full.");
            return;
        }

        Player leader = Bukkit.getPlayer(leaderId);
        if (leader == null || !leader.isOnline()) {
            requester.sendMessage(ChatColor.RED + "That party leader is offline.");
            return;
        }

        String cooldownKey = requester.getUniqueId() + ":" + leaderId;
        long now = System.currentTimeMillis();
        long expiresAt = requestCooldownByPair.getOrDefault(cooldownKey, 0L);
        if (expiresAt > now) {
            requester.sendMessage(ChatColor.RED + "You already sent a recent request to this party.");
            return;
        }
        requestCooldownByPair.put(cooldownKey, now + REQUEST_COOLDOWN_MILLIS);

        leader.sendMessage(ChatColor.AQUA + "[Party Finder] " + ChatColor.YELLOW + requester.getName()
                + ChatColor.GRAY + " wants to join. Use " + ChatColor.GREEN + "/party invite " + requester.getName());
        requester.sendMessage(ChatColor.GREEN + "Join request sent to " + leader.getName() + ".");
        playUiSuccess(requester);
    }

    private void invitePlayerFromGui(Player inviter, String targetIdRaw) {
        if (dungeonManager.isInDungeon(inviter.getUniqueId())) {
            playUiError(inviter);
            inviter.sendMessage(ChatColor.RED + "Party edits are locked while in a dungeon run.");
            return;
        }

        UUID targetId;
        try {
            targetId = UUID.fromString(targetIdRaw);
        } catch (IllegalArgumentException ignored) {
            playUiError(inviter);
            inviter.sendMessage(ChatColor.RED + "That player entry is invalid.");
            return;
        }

        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) {
            playUiError(inviter);
            inviter.sendMessage(ChatColor.RED + "That player is no longer online.");
            return;
        }

        String error = partyManager.invite(inviter, target);
        if (error != null) {
            playUiError(inviter);
            inviter.sendMessage(ChatColor.RED + error);
            return;
        }

        playUiSuccess(inviter);
        inviter.sendMessage(ChatColor.GREEN + "Invited " + target.getName() + ".");
        target.sendMessage(ChatColor.GOLD + inviter.getName() + ChatColor.YELLOW + " invited you to a dungeon party.");
        target.sendMessage(ChatColor.GRAY + "Use " + ChatColor.GREEN + "/party accept " + inviter.getName() + ChatColor.GRAY + " to join.");
    }

    private List<Player> inviteCandidates(Player requester) {
        List<Player> candidates = new ArrayList<>();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(requester.getUniqueId())) {
                continue;
            }
            if (partyManager.isInParty(online.getUniqueId())) {
                continue;
            }
            if (dungeonManager.isInDungeon(online.getUniqueId())) {
                continue;
            }
            candidates.add(online);
        }
        candidates.sort((first, second) -> String.CASE_INSENSITIVE_ORDER.compare(first.getName(), second.getName()));
        return candidates;
    }

    private ItemStack inviteTargetItem(Player target) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta baseMeta = item.getItemMeta();
        if (baseMeta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(target);
            item.setItemMeta(skullMeta);
        }

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Invite " + target.getName());
        meta.setLore(List.of(
                ChatColor.GRAY + "Status: " + ChatColor.GREEN + "Available",
                ChatColor.GRAY + "Click to send party invite."
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "invite_player");
        meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, target.getUniqueId().toString());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack inviteStatusItem(boolean hasParty, boolean isLeader, boolean inDungeon, boolean partyFull) {
        if (inDungeon) {
            return taggedItem(
                    Material.BARRIER,
                    ChatColor.RED + "Invites Locked",
                    List.of(ChatColor.GRAY + "Party edits are disabled during dungeon runs."),
                    "noop",
                    "",
                    true
            );
        }
        if (hasParty && !isLeader) {
            return taggedItem(
                    Material.BARRIER,
                    ChatColor.RED + "Leader Only",
                    List.of(ChatColor.GRAY + "Only the party leader can invite players."),
                    "noop",
                    "",
                    true
            );
        }
        if (partyFull) {
            return taggedItem(
                    Material.BARRIER,
                    ChatColor.RED + "Party Full",
                    List.of(ChatColor.GRAY + "Your party is already at max size."),
                    "noop",
                    "",
                    true
            );
        }
        return taggedItem(
                Material.BARRIER,
                ChatColor.RED + "No Eligible Players",
                List.of(ChatColor.GRAY + "No online players are currently available to invite."),
                "noop",
                "",
                true
        );
    }

    private ItemStack playerStatusItem(Player player) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta baseMeta = item.getItemMeta();
        if (baseMeta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(player);
            item.setItemMeta(skullMeta);
        }

        Party party = partyManager.getParty(player.getUniqueId());
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Player: " + ChatColor.YELLOW + player.getName());
        if (party == null) {
            lore.add(ChatColor.GRAY + "Party: " + ChatColor.RED + "None");
        } else {
            lore.add(ChatColor.GRAY + "Party: " + ChatColor.GREEN + party.size() + "/" + partyManager.maxPartySize());
            lore.add(ChatColor.GRAY + "Leader: " + ChatColor.GOLD + partyManager.nameOf(party.leader()));
        }
        lore.add(ChatColor.GRAY + "Dungeon: " + (dungeonManager.isInDungeon(player.getUniqueId())
                ? ChatColor.RED + "In Run"
                : ChatColor.GREEN + "Idle"));
        lore.add("");
        lore.add(ChatColor.DARK_GRAY + "Tip: /party list for full details.");

        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Adventurer Profile");
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "noop");
        meta.getPersistentDataContainer().set(valueKey, PersistentDataType.STRING, "");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack floorCard(FloorConfig floor) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Party Size: " + ChatColor.AQUA + floor.minPartySize() + "-" + floor.maxPartySize());
        lore.add(ChatColor.GRAY + "Encounter Rooms: " + ChatColor.YELLOW + floor.encounterRooms() + ChatColor.DARK_GRAY + " + Boss");
        lore.add(ChatColor.GRAY + "Combat/Puzzle/Treasure: "
                + ChatColor.RED + floor.combatRooms()
                + ChatColor.GRAY + "/"
                + ChatColor.LIGHT_PURPLE + floor.puzzleRooms()
                + ChatColor.GRAY + "/"
                + ChatColor.GOLD + floor.treasureRooms());
        lore.add(ChatColor.GRAY + "Boss: " + ChatColor.LIGHT_PURPLE + floor.bossName());
        lore.add(ChatColor.GRAY + "Boss Health Scale: " + ChatColor.RED + "x" + formatMultiplier(floor.bossHealthMultiplier()));
        lore.add("");
        lore.add(ChatColor.GREEN + "Click to start this floor");

        return taggedItem(
                floor.floorMaterial(),
                ChatColor.GOLD + floor.id() + ChatColor.YELLOW + " - " + floor.displayName(),
                lore,
                "start_floor",
                floor.id(),
                true
        );
    }

    private ItemStack decorativePane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.BLACK + " ");
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    private void fillInventory(Inventory inventory, ItemStack item) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, item.clone());
        }
    }

    private void fillRange(Inventory inventory, int startSlot, int endSlot, ItemStack item) {
        for (int slot = startSlot; slot <= endSlot && slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, item.clone());
        }
    }

    private String occupancyBar(int size, int maxSize) {
        if (maxSize <= 0) {
            return ChatColor.DARK_GRAY + "n/a";
        }
        int safeSize = Math.max(0, Math.min(size, maxSize));
        int segments = 10;
        int filled = (int) Math.round((safeSize / (double) maxSize) * segments);
        return ChatColor.GREEN + "|".repeat(filled) + ChatColor.DARK_GRAY + "|".repeat(Math.max(0, segments - filled));
    }

    private String formatMultiplier(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private void playUiClick(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7F, 1.2F);
    }

    private void playUiSuccess(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 1.1F);
    }

    private void playUiError(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7F, 0.8F);
    }

    private enum MenuType {
        MAIN,
        FLOORS,
        PARTY_FINDER,
        PARTY_INVITES
    }

    private static final class MenuHolder implements InventoryHolder {
        private final MenuType type;
        private Inventory inventory;

        private MenuHolder(MenuType type) {
            this.type = type;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public MenuType type() {
            return type;
        }
    }
}
