package io.papermc.Grivience.quest;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.gui.SkyblockGui;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class QuestGui implements Listener {
    private static final String PLAYER_TITLE = SkyblockGui.title("Quest Log");
    private static final String ADMIN_TITLE = SkyblockGui.title("Quest Editor");
    private static final String EDIT_TITLE_PREFIX = SkyblockGui.title("Edit: ");
    private static final String REWARD_TITLE_PREFIX = SkyblockGui.title("Rewards: ");
    private static final String OBJ_TITLE_PREFIX = SkyblockGui.title("Objectives: ");
    private static final String PRE_TITLE_PREFIX = SkyblockGui.title("Prereqs: ");
    private static final int PAGE_SIZE = 36;

    public enum QuestFilter { HUB, FARMHUB, MINEHUB }

    private final GriviencePlugin plugin;
    private final QuestManager questManager;
    private final Map<UUID, Prompt> prompts = new HashMap<>();

    public QuestGui(GriviencePlugin plugin, QuestManager questManager) {
        this.plugin = plugin;
        this.questManager = questManager;
    }

    public void openPlayerMenu(Player player) { openPlayerMenu(player, QuestFilter.HUB, 0); }

    public void openPlayerMenu(Player player, QuestFilter filter, int requestedPage) {
        List<ConversationQuest> filtered = filterQuests(player, questManager.questsSorted(), filter);
        int page = normalizePage(requestedPage, filtered.size());
        Inventory inv = Bukkit.createInventory(new PlayerMenuHolder(filter, page), 54, PLAYER_TITLE);
        fillSkyblockBackground(inv);
        
        inv.setItem(46, filterButton(QuestFilter.HUB, filter == QuestFilter.HUB));
        inv.setItem(47, filterButton(QuestFilter.FARMHUB, filter == QuestFilter.FARMHUB));
        inv.setItem(48, filterButton(QuestFilter.MINEHUB, filter == QuestFilter.MINEHUB));
        
        fillQuestPage(player, inv, filtered, page, false);
        inv.setItem(45, navItem(Material.ARROW, ChatColor.GREEN + "Prev Page"));
        inv.setItem(49, SkyblockGui.closeButton());
        inv.setItem(53, navItem(Material.ARROW, ChatColor.GREEN + "Next Page"));
        player.openInventory(inv);
    }

    private List<ConversationQuest> filterQuests(Player player, List<ConversationQuest> quests, QuestFilter filter) {
        String worldTarget = switch (filter) {
            case HUB -> "Hub";
            case FARMHUB -> "Farmhub";
            case MINEHUB -> "Minehub";
        };
        
        return quests.stream()
                .filter(q -> q.world() != null && q.world().equalsIgnoreCase(worldTarget))
                .filter(q -> q.enabled())
                .toList();
    }

    private ItemStack filterButton(QuestFilter filter, boolean active) {
        Material mat = switch (filter) {
            case HUB -> Material.STONE;
            case FARMHUB -> Material.GRASS_BLOCK;
            case MINEHUB -> Material.COBBLESTONE;
        };
        
        String name = (active ? ChatColor.GREEN : ChatColor.GRAY) + switch (filter) {
            case HUB -> "Hub Quests";
            case FARMHUB -> "Farmhub Quests";
            case MINEHUB -> "Minehub Quests";
        };
        
        ItemStack item = SkyblockGui.button(mat, name, List.of(ChatColor.GRAY + "Click to filter by world"));
        if (active) {
            ItemMeta meta = item.getItemMeta();
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void openAdminMenu(Player player) { openAdminMenu(player, 0); }

    public void openAdminMenu(Player player, int requestedPage) {
        if (!player.hasPermission("grivience.admin")) return;
        List<ConversationQuest> quests = questManager.questsSorted();
        int page = normalizePage(requestedPage, quests.size());
        Inventory inv = Bukkit.createInventory(new AdminMenuHolder(page), 54, ADMIN_TITLE);
        fillSkyblockBackground(inv);
        fillQuestPage(player, inv, quests, page, true);
        inv.setItem(45, navItem(Material.ARROW, ChatColor.GREEN + "Prev"));
        inv.setItem(48, navItem(Material.LIME_DYE, ChatColor.GREEN + "Player View"));
        inv.setItem(49, navItem(Material.EMERALD, ChatColor.AQUA + "Create Quest"));
        inv.setItem(50, navItem(Material.HOPPER, ChatColor.YELLOW + "Reload"));
        inv.setItem(53, navItem(Material.ARROW, ChatColor.GREEN + "Next"));
        player.openInventory(inv);
    }

    public void openQuestEditor(Player player, String questId) {
        ConversationQuest q = questManager.quest(questId);
        if (q == null) return;
        Inventory inv = Bukkit.createInventory(new EditHolder(q.id()), 54, EDIT_TITLE_PREFIX + q.id());
        fillSkyblockBackground(inv);
        inv.setItem(4, infoItem(q));
        inv.setItem(10, actionItem(Material.NAME_TAG, ChatColor.YELLOW + "Set Name", List.of(ChatColor.GRAY + stripColor(questManager.color(q.displayName())))));
        inv.setItem(11, actionItem(Material.PAPER, ChatColor.YELLOW + "Set Desc", List.of(ChatColor.GRAY + q.description())));
        inv.setItem(12, actionItem(Material.BOOK, ChatColor.YELLOW + "Objectives", List.of(ChatColor.GRAY + "Count: " + q.objectives().size())));
        inv.setItem(13, actionItem(Material.REPEATER, ChatColor.YELLOW + "Prereqs", List.of(ChatColor.GRAY + "Count: " + q.prerequisites().size())));
        inv.setItem(14, actionItem(q.repeatable() ? Material.CLOCK : Material.COBWEB, ChatColor.YELLOW + "Repeatable", List.of(statusLine("Repeatable", q.repeatable()))));
        inv.setItem(15, actionItem(q.enabled() ? Material.LIME_DYE : Material.GRAY_DYE, ChatColor.YELLOW + "Enabled", List.of(statusLine("Enabled", q.enabled()))));
        inv.setItem(16, actionItem(Material.CHEST, ChatColor.YELLOW + "Rewards", List.of(ChatColor.GRAY + "Count: " + q.rewardCommands().size())));
        inv.setItem(17, actionItem(Material.GRASS_BLOCK, ChatColor.YELLOW + "Set World", List.of(ChatColor.GRAY + "Current: " + ChatColor.WHITE + q.world())));
        inv.setItem(19, actionItem(Material.OAK_SIGN, ChatColor.YELLOW + "Starter NPC", List.of(ChatColor.GRAY + (q.hasStarterNpc() ? q.starterNpcId() : "none"))));
        inv.setItem(20, actionItem(Material.BELL, ChatColor.YELLOW + "Target NPC", List.of(ChatColor.GRAY + q.targetNpcId())));
        inv.setItem(31, actionItem(Material.EMERALD, ChatColor.GREEN + "Start (Self)", List.of()));
        inv.setItem(32, actionItem(Material.BARRIER, ChatColor.RED + "Cancel (Self)", List.of()));
        inv.setItem(22, actionItem(Material.TNT, ChatColor.RED + "Delete", List.of(ChatColor.GRAY + "Shift-Click")));
        inv.setItem(45, navItem(Material.ARROW, ChatColor.YELLOW + "Back"));
        inv.setItem(49, navItem(Material.BARRIER, ChatColor.RED + "Close"));
        player.openInventory(inv);
    }

    public void openRewardEditor(Player player, String qId, int page) {
        ConversationQuest q = questManager.quest(qId);
        if (q == null) return;
        Inventory inv = Bukkit.createInventory(new RewardsHolder(qId, page), 54, REWARD_TITLE_PREFIX + qId);
        fillSkyblockBackground(inv);
        List<String> rewards = q.rewardCommands();
        for (int i = 0; i < Math.min(PAGE_SIZE, rewards.size() - page * PAGE_SIZE); i++) {
            inv.setItem(i, actionItem(Material.PAPER, ChatColor.YELLOW + "Reward #" + (page * PAGE_SIZE + i + 1), List.of(ChatColor.GRAY + rewards.get(page * PAGE_SIZE + i))));
        }
        inv.setItem(48, navItem(Material.ARROW, ChatColor.YELLOW + "Back"));
        inv.setItem(49, navItem(Material.EMERALD, ChatColor.AQUA + "Add Reward"));
        player.openInventory(inv);
    }

    public void openObjectiveEditor(Player player, String qId) {
        ConversationQuest q = questManager.quest(qId);
        if (q == null) return;
        Inventory inv = Bukkit.createInventory(new ObjectivesHolder(qId), 54, OBJ_TITLE_PREFIX + qId);
        fillSkyblockBackground(inv);
        for (int i = 0; i < q.objectives().size(); i++) {
            QuestObjective o = q.objectives().get(i);
            inv.setItem(i, actionItem(Material.PAPER, ChatColor.AQUA + "Obj #" + (i + 1), List.of(ChatColor.GRAY + o.description(), ChatColor.GRAY + "Type: " + o.type())));
        }
        inv.setItem(48, navItem(Material.ARROW, ChatColor.YELLOW + "Back"));
        inv.setItem(49, navItem(Material.EMERALD, ChatColor.AQUA + "Add Obj"));
        player.openInventory(inv);
    }

    public void openPrerequisiteEditor(Player player, String qId) {
        ConversationQuest q = questManager.quest(qId);
        if (q == null) return;
        Inventory inv = Bukkit.createInventory(new PrerequisitesHolder(qId), 54, PRE_TITLE_PREFIX + qId);
        fillSkyblockBackground(inv);
        for (int i = 0; i < q.prerequisites().size(); i++) {
            inv.setItem(i, actionItem(Material.PAPER, ChatColor.AQUA + q.prerequisites().get(i), List.of(ChatColor.RED + "Click to remove")));
        }
        inv.setItem(48, navItem(Material.ARROW, ChatColor.YELLOW + "Back"));
        inv.setItem(49, navItem(Material.EMERALD, ChatColor.AQUA + "Add Prereq"));
        player.openInventory(inv);
    }

    public void openObjectiveTypeSelector(Player player, String qId) {
        Inventory inv = Bukkit.createInventory(new ObjectiveTypeHolder(qId), 27, "Select Type");
        fillSkyblockBackground(inv);
        int s = 10;
        for (QuestObjective.ObjectiveType t : QuestObjective.ObjectiveType.values()) {
            inv.setItem(s++, actionItem(Material.PAPER, ChatColor.YELLOW + t.name(), List.of()));
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof QuestHolder)) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        if (holder instanceof PlayerMenuHolder h) handlePlayerMenuClick(player, h, slot, event.isRightClick());
        else if (holder instanceof AdminMenuHolder h) handleAdminMenuClick(player, h, slot);
        else if (holder instanceof EditHolder h) handleEditClick(player, h, event);
        else if (holder instanceof RewardsHolder h) handleRewardClick(player, h, event);
        else if (holder instanceof ObjectivesHolder h) handleObjectiveClick(player, h, event);
        else if (holder instanceof ObjectiveTypeHolder h) handleObjectiveTypeClick(player, h, slot);
        else if (holder instanceof PrerequisitesHolder h) handlePrerequisiteClick(player, h, slot);
    }

    private void handlePlayerMenuClick(Player p, PlayerMenuHolder h, int s, boolean r) {
        if (s == 46) openPlayerMenu(p, QuestFilter.HUB, 0);
        else if (s == 47) openPlayerMenu(p, QuestFilter.FARMHUB, 0);
        else if (s == 48) openPlayerMenu(p, QuestFilter.MINEHUB, 0);
        else if (s == 45) openPlayerMenu(p, h.filter, h.page - 1);
        else if (s == 53) openPlayerMenu(p, h.filter, h.page + 1);
        else if (s == 49) p.closeInventory();
        else if (s < PAGE_SIZE) {
            ConversationQuest q = questAt(filterQuests(p, questManager.questsSorted(), h.filter), h.page, s);
            if (q != null) {
                if (r) questManager.cancelQuest(p, q.id(), true);
                else questManager.startQuest(p, q.id(), QuestTriggerSource.GUI, true);
                openPlayerMenu(p, h.filter, h.page);
            }
        }
    }

    private void handleAdminMenuClick(Player p, AdminMenuHolder h, int s) {
        if (s == 45) openAdminMenu(p, h.page - 1);
        else if (s == 53) openAdminMenu(p, h.page + 1);
        else if (s == 48) openPlayerMenu(p);
        else if (s == 49) queuePrompt(p, new Prompt(PromptType.CREATE_QUEST, "", h.page), ChatColor.YELLOW + "Quest ID:");
        else if (s == 50) { questManager.reload(); openAdminMenu(p, h.page); }
        else if (s < PAGE_SIZE) {
            ConversationQuest q = questAt(questManager.questsSorted(), h.page, s);
            if (q != null) openQuestEditor(p, q.id());
        }
    }

    private void handleEditClick(Player p, EditHolder h, InventoryClickEvent e) {
        String qId = h.questId;
        ConversationQuest q = questManager.quest(qId);
        if (q == null) { openAdminMenu(p); return; }
        switch (e.getRawSlot()) {
            case 10 -> queuePrompt(p, new Prompt(PromptType.SET_NAME, qId, 0), ChatColor.YELLOW + "Name:");
            case 11 -> queuePrompt(p, new Prompt(PromptType.SET_DESCRIPTION, qId, 0), ChatColor.YELLOW + "Desc:");
            case 12 -> openObjectiveEditor(p, qId);
            case 13 -> openPrerequisiteEditor(p, qId);
            case 14 -> { questManager.setRepeatable(qId, !q.repeatable()); openQuestEditor(p, qId); }
            case 15 -> { questManager.setEnabled(qId, !q.enabled()); openQuestEditor(p, qId); }
            case 16 -> openRewardEditor(p, qId, 0);
            case 17 -> queuePrompt(p, new Prompt(PromptType.SET_WORLD, qId, 0), ChatColor.YELLOW + "World (Hub, Farmhub, Minehub):");
            case 19 -> queuePrompt(p, new Prompt(PromptType.SET_STARTER, qId, 0), ChatColor.YELLOW + "Starter:");
            case 20 -> queuePrompt(p, new Prompt(PromptType.SET_TARGET, qId, 0), ChatColor.YELLOW + "Target:");
            case 31 -> { questManager.startQuest(p, qId, QuestTriggerSource.GUI, true); openQuestEditor(p, qId); }
            case 32 -> { questManager.cancelQuest(p, qId, true); openQuestEditor(p, qId); }
            case 22 -> { if (e.isShiftClick()) { questManager.deleteQuest(qId); openAdminMenu(p); } }
            case 45 -> openAdminMenu(p);
            case 49 -> p.closeInventory();
        }
    }

    private void handleRewardClick(Player p, RewardsHolder h, InventoryClickEvent e) {
        if (e.getRawSlot() == 48) openQuestEditor(p, h.questId);
        else if (e.getRawSlot() == 49) queuePrompt(p, new Prompt(PromptType.ADD_REWARD, h.questId, h.page), ChatColor.YELLOW + "Cmd:");
    }

    private void handleObjectiveClick(Player p, ObjectivesHolder h, InventoryClickEvent e) {
        if (e.getRawSlot() == 48) openQuestEditor(p, h.questId);
        else if (e.getRawSlot() == 49) openObjectiveTypeSelector(p, h.questId);
        else {
            ConversationQuest q = questManager.quest(h.questId);
            if (q != null && e.getRawSlot() < q.objectives().size()) { q.objectives().remove(e.getRawSlot()); openObjectiveEditor(p, h.questId); }
        }
    }

    private void handleObjectiveTypeClick(Player p, ObjectiveTypeHolder h, int s) {
        if (s >= 10 && s < 10 + QuestObjective.ObjectiveType.values().length) {
            Prompt pr = new Prompt(PromptType.ADD_OBJ_DESC, h.questId, 0);
            pr.tempType = QuestObjective.ObjectiveType.values()[s - 10];
            queuePrompt(p, pr, ChatColor.YELLOW + "Description:");
        }
    }

    private void handlePrerequisiteClick(Player p, PrerequisitesHolder h, int s) {
        if (s == 48) openQuestEditor(p, h.questId);
        else if (s == 49) queuePrompt(p, new Prompt(PromptType.ADD_PREREQUISITE, h.questId, 0), ChatColor.YELLOW + "Pre ID:");
        else {
            ConversationQuest q = questManager.quest(h.questId);
            if (q != null && s < q.prerequisites().size()) { q.prerequisites().remove(s); openPrerequisiteEditor(p, h.questId); }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Prompt p = prompts.remove(e.getPlayer().getUniqueId());
        if (p == null) return;
        e.setCancelled(true);
        Bukkit.getScheduler().runTask(plugin, () -> processPrompt(e.getPlayer(), p, e.getMessage().trim()));
    }

    private void processPrompt(Player p, Prompt pr, String m) {
        if (m.equalsIgnoreCase("cancel")) { reopenAfterPrompt(p, pr); return; }
        ConversationQuest q = questManager.quest(pr.questId);
        switch (pr.type) {
            case CREATE_QUEST -> { questManager.createQuest(m, m); openQuestEditor(p, m); }
            case SET_NAME -> { questManager.setDisplayName(pr.questId, m); openQuestEditor(p, pr.questId); }
            case SET_DESCRIPTION -> { questManager.setDescription(pr.questId, m); openQuestEditor(p, pr.questId); }
            case SET_WORLD -> { questManager.setWorld(pr.questId, m); openQuestEditor(p, pr.questId); }
            case ADD_REWARD -> { questManager.addRewardCommand(pr.questId, m); openRewardEditor(p, pr.questId, pr.page); }
            case ADD_OBJ_DESC -> { Prompt next = new Prompt(PromptType.ADD_OBJ_TARGET, pr.questId, 0); next.tempData = m; next.tempType = pr.tempType; queuePrompt(p, next, ChatColor.YELLOW + "Target:"); }
            case ADD_OBJ_TARGET -> { Prompt next = new Prompt(PromptType.ADD_OBJ_AMOUNT, pr.questId, 0); next.tempData = pr.tempData; next.tempData2 = m; next.tempType = pr.tempType; queuePrompt(p, next, ChatColor.YELLOW + "Amount:"); }
            case ADD_OBJ_AMOUNT -> {
                int a = 1; try { a = Integer.parseInt(m); } catch (Exception ignored) {}
                if (q != null) q.objectives().add(new QuestObjective(pr.tempType, pr.tempData2.equalsIgnoreCase("none") ? "" : pr.tempData2, a, pr.tempData));
                openObjectiveEditor(p, pr.questId);
            }
            case ADD_PREREQUISITE -> { if (q != null) q.prerequisites().add(m.toLowerCase()); openPrerequisiteEditor(p, pr.questId); }
            case SET_STARTER -> { questManager.setStarterNpc(pr.questId, m.equalsIgnoreCase("none") ? "" : m); openQuestEditor(p, pr.questId); }
            case SET_TARGET -> { questManager.setTargetNpc(pr.questId, m); openQuestEditor(p, pr.questId); }
        }
    }

    private void reopenAfterPrompt(Player p, Prompt pr) {
        switch (pr.type) {
            case CREATE_QUEST -> openAdminMenu(p, pr.page);
            case ADD_REWARD -> openRewardEditor(p, pr.questId, pr.page);
            case ADD_OBJ_DESC, ADD_OBJ_TARGET, ADD_OBJ_AMOUNT -> openObjectiveEditor(p, pr.questId);
            case ADD_PREREQUISITE -> openPrerequisiteEditor(p, pr.questId);
            default -> openQuestEditor(p, pr.questId);
        }
    }

    private void queuePrompt(Player p, Prompt pr, String... lines) {
        prompts.put(p.getUniqueId(), pr); p.closeInventory();
        for (String l : lines) p.sendMessage(l);
    }

    private void fillQuestPage(Player p, Inventory inv, List<ConversationQuest> qs, int pg, boolean admin) {
        int start = pg * PAGE_SIZE;
        for (int i = 0; i < Math.min(PAGE_SIZE, qs.size() - start); i++) {
            inv.setItem(i, admin ? buildAdminQuestItem(qs.get(start + i)) : buildPlayerQuestItem(p, qs.get(start + i)));
        }
    }

    private ItemStack buildPlayerQuestItem(Player p, ConversationQuest q) {
        boolean act = questManager.isQuestActive(p, q.id());
        boolean comp = questManager.hasCompletedQuest(p, q.id());
        List<String> lore = new ArrayList<>(List.of(ChatColor.GRAY + q.description(), ""));
        
        if (act) {
            QuestProgress prog = questManager.getProgress(p, q.id());
            if (prog != null) {
                if (!q.objectives().isEmpty()) {
                    lore.add(ChatColor.WHITE + "Objectives:");
                    for (int i = 0; i < q.objectives().size(); i++) {
                        QuestObjective obj = q.objectives().get(i);
                        int current = prog.getObjectiveProgress(i);
                        lore.add(ChatColor.GRAY + "- " + (obj.isComplete(current) ? ChatColor.STRIKETHROUGH : "") + obj.progressLabel(current));
                    }
                    lore.add("");
                } else if (q.hasTargetNpc()) {
                    lore.add(ChatColor.WHITE + "Objective:");
                    lore.add(ChatColor.GRAY + "- Talk to " + ChatColor.AQUA + q.targetNpcId());
                    lore.add("");
                }
            }
        }
        
        lore.add(ChatColor.GRAY + "Status: " + questManager.progressLabel(p, q));
        if (act) {
            lore.add("");
            lore.add(ChatColor.RED + "Right-Click to cancel!");
        } else if (!comp || q.repeatable()) {
            lore.add("");
            lore.add(ChatColor.YELLOW + "Click to start!");
        }

        return actionItem(act ? Material.COMPASS : (comp ? Material.LIME_DYE : Material.BOOK), questManager.color(q.displayName()), lore);
    }

    private ItemStack buildAdminQuestItem(ConversationQuest q) {
        return actionItem(q.enabled() ? Material.WRITTEN_BOOK : Material.BOOK, questManager.color(q.displayName()), List.of(ChatColor.GRAY + q.description(), ChatColor.DARK_GRAY + "ID: " + q.id(), statusLine("Enabled", q.enabled())));
    }

    private ItemStack infoItem(ConversationQuest q) { return actionItem(Material.WRITABLE_BOOK, ChatColor.AQUA + "Summary", List.of(ChatColor.GRAY + "ID: " + q.id())); }
    private ItemStack actionItem(Material m, String n, List<String> l) { ItemStack i = new ItemStack(m); ItemMeta mt = i.getItemMeta(); mt.setDisplayName(n); mt.setLore(l); i.setItemMeta(mt); return i; }
    private ItemStack navItem(Material m, String n) { return actionItem(m, n, List.of()); }
    private void fillSkyblockBackground(Inventory inv) { SkyblockGui.fillAll(inv, SkyblockGui.filler(Material.BLACK_STAINED_GLASS_PANE)); }
    private String statusLine(String l, boolean s) { return ChatColor.GRAY + l + ": " + (s ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"); }
    private String stripColor(String i) { return ChatColor.stripColor(i); }
    private ConversationQuest questAt(List<ConversationQuest> qs, int pg, int s) { int i = pg * PAGE_SIZE + s; return (i >= 0 && i < qs.size()) ? qs.get(i) : null; }
    private int normalizePage(int p, int t) { if (t <= 0) return 0; int m = (t - 1) / PAGE_SIZE; return Math.max(0, Math.min(p, m)); }
    private interface QuestHolder extends InventoryHolder { @Override default Inventory getInventory() { return null; } }
    private record PlayerMenuHolder(QuestFilter filter, int page) implements QuestHolder {}
    private record AdminMenuHolder(int page) implements QuestHolder {}
    private record EditHolder(String questId) implements QuestHolder {}
    private record RewardsHolder(String questId, int page) implements QuestHolder {}
    private record ObjectivesHolder(String questId) implements QuestHolder {}
    private record ObjectiveTypeHolder(String questId) implements QuestHolder {}
    private record PrerequisitesHolder(String questId) implements QuestHolder {}
    private static class Prompt { final PromptType type; final String questId; final int page; String tempData, tempData2; QuestObjective.ObjectiveType tempType; Prompt(PromptType t, String q, int p) { this.type = t; this.questId = q; this.page = p; } }
    private enum PromptType { CREATE_QUEST, SET_NAME, SET_DESCRIPTION, SET_WORLD, SET_STARTER, SET_TARGET, ADD_REWARD, ADD_OBJ_DESC, ADD_OBJ_TARGET, ADD_OBJ_AMOUNT, ADD_PREREQUISITE }
}
