package io.papermc.Grivience.quest;

import io.papermc.Grivience.GriviencePlugin;
import io.papermc.Grivience.gui.SkyblockGui;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
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
    private static final String EDIT_TITLE_PREFIX = SkyblockGui.title("Edit Quest: ");
    private static final String REWARD_TITLE_PREFIX = SkyblockGui.title("Rewards: ");
    private static final int PAGE_SIZE = 45;

    private final GriviencePlugin plugin;
    private final QuestManager questManager;
    private final Map<UUID, Prompt> prompts = new HashMap<>();

    public QuestGui(GriviencePlugin plugin, QuestManager questManager) {
        this.plugin = plugin;
        this.questManager = questManager;
    }

    public void openPlayerMenu(Player player) {
        openPlayerMenu(player, 0);
    }

    public void openPlayerMenu(Player player, int requestedPage) {
        List<ConversationQuest> quests = questManager.questsSorted();
        int page = normalizePage(requestedPage, quests.size());

        Inventory inventory = Bukkit.createInventory(new PlayerMenuHolder(page), 54, PLAYER_TITLE);
        fillSkyblockBackground(inventory);
        fillQuestPage(player, inventory, quests, page, false);

        inventory.setItem(45, SkyblockGui.button(Material.ARROW, ChatColor.GREEN + "Previous Page", List.of(ChatColor.GRAY + "View previous quests.")));
        inventory.setItem(49, SkyblockGui.closeButton());
        inventory.setItem(53, SkyblockGui.button(Material.ARROW, ChatColor.GREEN + "Next Page", List.of(ChatColor.GRAY + "View more quests.")));

        player.openInventory(inventory);
    }

    public void openAdminMenu(Player player) {
        openAdminMenu(player, 0);
    }

    public void openAdminMenu(Player player, int requestedPage) {
        if (!player.hasPermission("grivience.admin")) {
            player.sendMessage(ChatColor.RED + "You lack permission.");
            return;
        }

        List<ConversationQuest> quests = questManager.questsSorted();
        int page = normalizePage(requestedPage, quests.size());

        Inventory inventory = Bukkit.createInventory(new AdminMenuHolder(page), 54, ADMIN_TITLE);
        fillSkyblockBackground(inventory);
        fillQuestPage(player, inventory, quests, page, true);

        inventory.setItem(45, SkyblockGui.button(Material.ARROW, ChatColor.GREEN + "Previous Page", List.of(ChatColor.GRAY + "View previous quests.")));
        inventory.setItem(48, SkyblockGui.button(Material.LIME_DYE, ChatColor.GREEN + "Player View", List.of(ChatColor.GRAY + "Switch to the player quest log.")));
        inventory.setItem(49, SkyblockGui.button(Material.EMERALD, ChatColor.AQUA + "Create Quest", List.of(ChatColor.GRAY + "Create a new quest.")));
        inventory.setItem(50, SkyblockGui.button(Material.HOPPER, ChatColor.YELLOW + "Reload Quests", List.of(ChatColor.GRAY + "Reload quests from disk.")));
        inventory.setItem(53, SkyblockGui.button(Material.ARROW, ChatColor.GREEN + "Next Page", List.of(ChatColor.GRAY + "View more quests.")));

        player.openInventory(inventory);
    }

    public void openQuestEditor(Player player, String questId) {
        if (!player.hasPermission("grivience.admin")) {
            player.sendMessage(ChatColor.RED + "You lack permission.");
            return;
        }

        ConversationQuest quest = questManager.quest(questId);
        if (quest == null) {
            player.sendMessage(ChatColor.RED + "Quest not found: " + questId);
            return;
        }

        Inventory inventory = Bukkit.createInventory(new EditHolder(quest.id()), 54, EDIT_TITLE_PREFIX + quest.id());
        fillSkyblockBackground(inventory);

        inventory.setItem(4, infoItem(quest));
        inventory.setItem(10, actionItem(Material.NAME_TAG, ChatColor.YELLOW + "Set Display Name",
                List.of(ChatColor.GRAY + stripColor(questManager.color(quest.displayName())))));
        inventory.setItem(11, actionItem(Material.PAPER, ChatColor.YELLOW + "Set Description",
                List.of(ChatColor.GRAY + quest.description())));

        String starter = quest.hasStarterNpc() ? quest.starterNpcId() : "none";
        inventory.setItem(12, actionItem(Material.OAK_SIGN, ChatColor.YELLOW + "Set Starter NPC",
                List.of(ChatColor.GRAY + starter, ChatColor.DARK_GRAY + "Use 'none' to clear")));

        inventory.setItem(13, actionItem(Material.BELL, ChatColor.YELLOW + "Set Target NPC",
                List.of(ChatColor.GRAY + quest.targetNpcId(), ChatColor.DARK_GRAY + "Required for completion")));

        inventory.setItem(14, actionItem(
                quest.repeatable() ? Material.CLOCK : Material.COBWEB,
                ChatColor.YELLOW + "Toggle Repeatable",
                List.of(statusLine("Repeatable", quest.repeatable()))
        ));

        inventory.setItem(15, actionItem(
                quest.enabled() ? Material.LIME_DYE : Material.GRAY_DYE,
                ChatColor.YELLOW + "Toggle Enabled",
                List.of(statusLine("Enabled", quest.enabled()))
        ));

        inventory.setItem(16, actionItem(Material.CHEST, ChatColor.YELLOW + "Reward Commands",
                List.of(ChatColor.GRAY + "Count: " + ChatColor.AQUA + quest.rewardCommands().size(),
                        ChatColor.DARK_GRAY + "Click to edit")));

        inventory.setItem(31, actionItem(Material.EMERALD, ChatColor.GREEN + "Start Quest (Self)", List.of(ChatColor.GRAY + "Test start flow")));
        inventory.setItem(32, actionItem(Material.BARRIER, ChatColor.RED + "Cancel Quest (Self)", List.of(ChatColor.GRAY + "Clear active state")));
        inventory.setItem(33, actionItem(Material.BLAZE_ROD, ChatColor.AQUA + "Simulate Target Talk", List.of(ChatColor.GRAY + "Triggers /quest talk <target>")));

        inventory.setItem(22, actionItem(Material.TNT, ChatColor.RED + "Delete Quest",
                List.of(ChatColor.GRAY + "Shift + Left click to delete")));

        inventory.setItem(45, navItem(Material.ARROW, ChatColor.YELLOW + "Back"));
        inventory.setItem(49, navItem(Material.BARRIER, ChatColor.RED + "Close"));

        player.openInventory(inventory);
    }

    public void openRewardEditor(Player player, String questId, int requestedPage) {
        ConversationQuest quest = questManager.quest(questId);
        if (quest == null) {
            player.sendMessage(ChatColor.RED + "Quest not found: " + questId);
            return;
        }

        int page = normalizePage(requestedPage, quest.rewardCommands().size());
        Inventory inventory = Bukkit.createInventory(new RewardsHolder(quest.id(), page), 54, REWARD_TITLE_PREFIX + quest.id());
        fillSkyblockBackground(inventory);

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, quest.rewardCommands().size());
        for (int index = start; index < end; index++) {
            String rewardCommand = quest.rewardCommands().get(index);
            int slot = index - start;
            inventory.setItem(slot, actionItem(Material.PAPER, ChatColor.AQUA + "Reward #" + (index + 1),
                    List.of(ChatColor.GRAY + rewardCommand, ChatColor.DARK_GRAY + "Click to remove")));
        }

        inventory.setItem(45, navItem(Material.ARROW, ChatColor.YELLOW + "Previous"));
        inventory.setItem(48, navItem(Material.ARROW, ChatColor.YELLOW + "Back"));
        inventory.setItem(49, navItem(Material.EMERALD, ChatColor.AQUA + "Add Reward"));
        inventory.setItem(50, navItem(Material.BARRIER, ChatColor.RED + "Clear Rewards"));
        inventory.setItem(53, navItem(Material.ARROW, ChatColor.YELLOW + "Next"));

        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) {
            return;
        }
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof PlayerMenuHolder || holder instanceof AdminMenuHolder || holder instanceof EditHolder || holder instanceof RewardsHolder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) {
            return;
        }

        if (holder instanceof PlayerMenuHolder playerMenuHolder) {
            handlePlayerMenuClick(player, playerMenuHolder, event.getRawSlot(), event.isRightClick());
            return;
        }
        if (holder instanceof AdminMenuHolder adminMenuHolder) {
            handleAdminMenuClick(player, adminMenuHolder, event.getRawSlot());
            return;
        }
        if (holder instanceof EditHolder editHolder) {
            handleEditClick(player, editHolder, event);
            return;
        }
        if (holder instanceof RewardsHolder rewardsHolder) {
            handleRewardClick(player, rewardsHolder, event);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Prompt prompt = prompts.remove(event.getPlayer().getUniqueId());
        if (prompt == null) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage().trim();
        Bukkit.getScheduler().runTask(plugin, () -> processPrompt(event.getPlayer(), prompt, message));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        prompts.remove(event.getPlayer().getUniqueId());
    }

    private void handlePlayerMenuClick(Player player, PlayerMenuHolder holder, int rawSlot, boolean rightClick) {
        List<ConversationQuest> quests = questManager.questsSorted();
        int page = holder.page();

        if (rawSlot == 45) {
            openPlayerMenu(player, page - 1);
            return;
        }
        if (rawSlot == 49) {
            player.closeInventory();
            return;
        }
        if (rawSlot == 53) {
            openPlayerMenu(player, page + 1);
            return;
        }

        if (rawSlot >= PAGE_SIZE) {
            return;
        }

        ConversationQuest quest = questAt(quests, page, rawSlot);
        if (quest == null || !quest.enabled()) {
            return;
        }

        if (rightClick) {
            QuestManager.CancelResult cancelResult = questManager.cancelQuest(player, quest.id(), true);
            if (cancelResult == QuestManager.CancelResult.NOT_ACTIVE) {
                player.sendMessage(ChatColor.RED + "Quest is not active.");
            }
        } else {
            QuestManager.StartResult startResult = questManager.startQuest(player, quest.id(), QuestTriggerSource.GUI, true);
            if (startResult == QuestManager.StartResult.ALREADY_ACTIVE) {
                player.sendMessage(ChatColor.YELLOW + "Quest is already active.");
            } else if (startResult == QuestManager.StartResult.ALREADY_COMPLETED) {
                player.sendMessage(ChatColor.YELLOW + "You already completed this quest.");
            } else if (startResult == QuestManager.StartResult.QUEST_DISABLED) {
                player.sendMessage(ChatColor.RED + "This quest is disabled.");
            }
        }

        openPlayerMenu(player, page);
    }

    private void handleAdminMenuClick(Player player, AdminMenuHolder holder, int rawSlot) {
        if (!player.hasPermission("grivience.admin")) {
            player.sendMessage(ChatColor.RED + "You lack permission.");
            player.closeInventory();
            return;
        }

        List<ConversationQuest> quests = questManager.questsSorted();
        int page = holder.page();

        if (rawSlot == 45) {
            openAdminMenu(player, page - 1);
            return;
        }
        if (rawSlot == 48) {
            openPlayerMenu(player);
            return;
        }
        if (rawSlot == 49) {
            queuePrompt(player, new Prompt(PromptType.CREATE_QUEST, "", page),
                    ChatColor.YELLOW + "Type a new quest id in chat (letters/numbers/_).",
                    ChatColor.GRAY + "Type 'cancel' to abort.");
            return;
        }
        if (rawSlot == 50) {
            questManager.reload();
            player.sendMessage(ChatColor.GREEN + "Quest files reloaded.");
            openAdminMenu(player, page);
            return;
        }
        if (rawSlot == 53) {
            openAdminMenu(player, page + 1);
            return;
        }

        if (rawSlot >= PAGE_SIZE) {
            return;
        }

        ConversationQuest quest = questAt(quests, page, rawSlot);
        if (quest != null) {
            openQuestEditor(player, quest.id());
        }
    }

    private void handleEditClick(Player player, EditHolder holder, InventoryClickEvent event) {
        String questId = holder.questId();
        int rawSlot = event.getRawSlot();

        ConversationQuest quest = questManager.quest(questId);
        if (quest == null) {
            player.sendMessage(ChatColor.RED + "Quest no longer exists.");
            openAdminMenu(player);
            return;
        }

        switch (rawSlot) {
            case 10 -> queuePrompt(player, new Prompt(PromptType.SET_NAME, quest.id(), 0),
                    ChatColor.YELLOW + "Type the display name for quest '" + quest.id() + "'.",
                    ChatColor.GRAY + "Use '&' for colors. Type 'cancel' to abort.");
            case 11 -> queuePrompt(player, new Prompt(PromptType.SET_DESCRIPTION, quest.id(), 0),
                    ChatColor.YELLOW + "Type the description for quest '" + quest.id() + "'.",
                    ChatColor.GRAY + "Type 'cancel' to abort.");
            case 12 -> queuePrompt(player, new Prompt(PromptType.SET_STARTER, quest.id(), 0),
                    ChatColor.YELLOW + "Type starter ZNPCS NPC id for '" + quest.id() + "'.",
                    ChatColor.GRAY + "Type 'none' to clear.");
            case 13 -> queuePrompt(player, new Prompt(PromptType.SET_TARGET, quest.id(), 0),
                    ChatColor.YELLOW + "Type target ZNPCS NPC id for '" + quest.id() + "'.",
                    ChatColor.GRAY + "This is used for completion.");
            case 14 -> {
                questManager.setRepeatable(quest.id(), !quest.repeatable());
                openQuestEditor(player, quest.id());
            }
            case 15 -> {
                questManager.setEnabled(quest.id(), !quest.enabled());
                openQuestEditor(player, quest.id());
            }
            case 16 -> openRewardEditor(player, quest.id(), 0);
            case 22 -> {
                if (event.isShiftClick() && event.isLeftClick()) {
                    boolean deleted = questManager.deleteQuest(quest.id());
                    if (deleted) {
                        player.sendMessage(ChatColor.GREEN + "Deleted quest: " + quest.id());
                        openAdminMenu(player);
                    } else {
                        player.sendMessage(ChatColor.RED + "Failed to delete quest.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Shift + Left click to delete this quest.");
                }
            }
            case 31 -> {
                questManager.startQuest(player, quest.id(), QuestTriggerSource.GUI, true);
                openQuestEditor(player, quest.id());
            }
            case 32 -> {
                questManager.cancelQuest(player, quest.id(), true);
                openQuestEditor(player, quest.id());
            }
            case 33 -> {
                questManager.handleNpcConversation(player, quest.targetNpcId(), QuestTriggerSource.GUI, true);
                openQuestEditor(player, quest.id());
            }
            case 45 -> openAdminMenu(player);
            case 49 -> player.closeInventory();
            default -> {
            }
        }
    }

    private void handleRewardClick(Player player, RewardsHolder holder, InventoryClickEvent event) {
        String questId = holder.questId();
        ConversationQuest quest = questManager.quest(questId);
        if (quest == null) {
            player.sendMessage(ChatColor.RED + "Quest no longer exists.");
            openAdminMenu(player);
            return;
        }

        int page = holder.page();
        int rawSlot = event.getRawSlot();

        if (rawSlot == 45) {
            openRewardEditor(player, questId, page - 1);
            return;
        }
        if (rawSlot == 48) {
            openQuestEditor(player, questId);
            return;
        }
        if (rawSlot == 49) {
            queuePrompt(player, new Prompt(PromptType.ADD_REWARD, questId, page),
                    ChatColor.YELLOW + "Type reward command for quest '" + questId + "'.",
                    ChatColor.GRAY + "Example: eco give {player} 5000");
            return;
        }
        if (rawSlot == 50) {
            if (event.isShiftClick()) {
                questManager.clearRewardCommands(questId);
                player.sendMessage(ChatColor.GREEN + "Cleared reward commands.");
                openRewardEditor(player, questId, 0);
            } else {
                player.sendMessage(ChatColor.RED + "Shift-click to clear all rewards.");
            }
            return;
        }
        if (rawSlot == 53) {
            openRewardEditor(player, questId, page + 1);
            return;
        }

        if (rawSlot >= PAGE_SIZE) {
            return;
        }

        int rewardIndex = page * PAGE_SIZE + rawSlot;
        if (rewardIndex >= quest.rewardCommands().size()) {
            return;
        }

        if (questManager.removeRewardCommand(questId, rewardIndex)) {
            player.sendMessage(ChatColor.YELLOW + "Removed reward command #" + (rewardIndex + 1) + ".");
            openRewardEditor(player, questId, page);
        }
    }

    private void processPrompt(Player player, Prompt prompt, String message) {
        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(ChatColor.YELLOW + "Canceled.");
            reopenAfterPrompt(player, prompt);
            return;
        }

        switch (prompt.type()) {
            case CREATE_QUEST -> {
                String id = ConversationQuest.normalizeId(message);
                if (id.isBlank()) {
                    player.sendMessage(ChatColor.RED + "Invalid quest id.");
                    openAdminMenu(player, prompt.page());
                    return;
                }
                if (!questManager.createQuest(id, id)) {
                    player.sendMessage(ChatColor.RED + "Quest already exists or id is invalid.");
                    openAdminMenu(player, prompt.page());
                    return;
                }
                player.sendMessage(ChatColor.GREEN + "Created quest: " + id);
                openQuestEditor(player, id);
            }
            case SET_NAME -> {
                if (questManager.setDisplayName(prompt.questId(), message)) {
                    player.sendMessage(ChatColor.GREEN + "Display name updated.");
                } else {
                    player.sendMessage(ChatColor.RED + "Quest not found.");
                }
                openQuestEditor(player, prompt.questId());
            }
            case SET_DESCRIPTION -> {
                if (questManager.setDescription(prompt.questId(), message)) {
                    player.sendMessage(ChatColor.GREEN + "Description updated.");
                } else {
                    player.sendMessage(ChatColor.RED + "Quest not found.");
                }
                openQuestEditor(player, prompt.questId());
            }
            case SET_STARTER -> {
                String value = message.equalsIgnoreCase("none") ? "" : message;
                if (questManager.setStarterNpc(prompt.questId(), value)) {
                    player.sendMessage(ChatColor.GREEN + "Starter NPC updated.");
                } else {
                    player.sendMessage(ChatColor.RED + "Quest not found.");
                }
                openQuestEditor(player, prompt.questId());
            }
            case SET_TARGET -> {
                String targetId = ConversationQuest.normalizeNpcId(message);
                if (targetId.isBlank()) {
                    player.sendMessage(ChatColor.RED + "Target NPC id cannot be empty.");
                    openQuestEditor(player, prompt.questId());
                    return;
                }
                if (questManager.setTargetNpc(prompt.questId(), targetId)) {
                    player.sendMessage(ChatColor.GREEN + "Target NPC updated.");
                } else {
                    player.sendMessage(ChatColor.RED + "Quest not found.");
                }
                openQuestEditor(player, prompt.questId());
            }
            case ADD_REWARD -> {
                if (questManager.addRewardCommand(prompt.questId(), message)) {
                    player.sendMessage(ChatColor.GREEN + "Reward command added.");
                } else {
                    player.sendMessage(ChatColor.RED + "Could not add reward command.");
                }
                openRewardEditor(player, prompt.questId(), prompt.page());
            }
        }
    }

    private void reopenAfterPrompt(Player player, Prompt prompt) {
        switch (prompt.type()) {
            case CREATE_QUEST -> openAdminMenu(player, prompt.page());
            case ADD_REWARD -> openRewardEditor(player, prompt.questId(), prompt.page());
            default -> openQuestEditor(player, prompt.questId());
        }
    }

    private void queuePrompt(Player player, Prompt prompt, String... lines) {
        prompts.put(player.getUniqueId(), prompt);
        player.closeInventory();
        for (String line : lines) {
            player.sendMessage(line);
        }
    }

    private void fillQuestPage(Player player, Inventory inventory, List<ConversationQuest> quests, int page, boolean adminView) {
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, quests.size());
        for (int index = start; index < end; index++) {
            ConversationQuest quest = quests.get(index);
            int slot = index - start;
            ItemStack item = adminView
                    ? buildAdminQuestItem(quest)
                    : buildPlayerQuestItem(player, quest);
            inventory.setItem(slot, item);
        }
    }

    private ItemStack buildPlayerQuestItem(Player player, ConversationQuest quest) {
        boolean active = questManager.isQuestActive(player.getUniqueId(), quest.id());
        boolean completed = questManager.hasCompletedQuest(player.getUniqueId(), quest.id());

        Material material;
        if (!quest.enabled()) {
            material = Material.GRAY_DYE;
        } else if (active) {
            material = Material.COMPASS;
        } else if (completed && !quest.repeatable()) {
            material = Material.LIME_DYE;
        } else {
            material = Material.BOOK;
        }

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + quest.description());
        lore.add(ChatColor.DARK_GRAY + "ID: " + quest.id());
        lore.add(ChatColor.GRAY + "Status: " + questManager.progressLabel(player.getUniqueId(), quest));
        lore.add(ChatColor.GRAY + "Starter NPC: " + ChatColor.AQUA + (quest.hasStarterNpc() ? quest.starterNpcId() : "none"));
        lore.add(ChatColor.GRAY + "Target NPC: " + ChatColor.AQUA + quest.targetNpcId());
        lore.add("");
        lore.add(ChatColor.YELLOW + "Left-click: Start quest");
        lore.add(ChatColor.RED + "Right-click: Cancel quest");

        return actionItem(material, questManager.color(quest.displayName()), lore);
    }

    private ItemStack buildAdminQuestItem(ConversationQuest quest) {
        Material material = quest.enabled() ? Material.WRITTEN_BOOK : Material.BOOK;
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + quest.description());
        lore.add(ChatColor.DARK_GRAY + "ID: " + quest.id());
        lore.add(statusLine("Enabled", quest.enabled()));
        lore.add(statusLine("Repeatable", quest.repeatable()));
        lore.add(ChatColor.GRAY + "Starter NPC: " + ChatColor.AQUA + (quest.hasStarterNpc() ? quest.starterNpcId() : "none"));
        lore.add(ChatColor.GRAY + "Target NPC: " + ChatColor.AQUA + quest.targetNpcId());
        lore.add(ChatColor.GRAY + "Rewards: " + ChatColor.YELLOW + quest.rewardCommands().size());
        lore.add("");
        lore.add(ChatColor.YELLOW + "Click to edit");

        return actionItem(material, questManager.color(quest.displayName()), lore);
    }

    private ItemStack infoItem(ConversationQuest quest) {
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "ID: " + ChatColor.AQUA + quest.id());
        lore.add(ChatColor.GRAY + "Description: " + ChatColor.WHITE + quest.description());
        lore.add(ChatColor.GRAY + "Starter NPC: " + ChatColor.AQUA + (quest.hasStarterNpc() ? quest.starterNpcId() : "none"));
        lore.add(ChatColor.GRAY + "Target NPC: " + ChatColor.AQUA + quest.targetNpcId());
        lore.add(ChatColor.GRAY + "Repeatable: " + (quest.repeatable() ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
        lore.add(ChatColor.GRAY + "Enabled: " + (quest.enabled() ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
        lore.add(ChatColor.GRAY + "Rewards: " + ChatColor.YELLOW + quest.rewardCommands().size());
        lore.add("");
        lore.add(questManager.znpcsHint(quest));
        return actionItem(Material.WRITABLE_BOOK, ChatColor.AQUA + "Quest Summary", lore);
    }

    private ItemStack actionItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack navItem(Material material, String name) {
        return actionItem(material, name, List.of());
    }

    private void fillSkyblockBackground(Inventory inventory) {
        SkyblockGui.fillAll(inventory, SkyblockGui.filler(Material.BLACK_STAINED_GLASS_PANE));
        ItemStack border = SkyblockGui.filler(Material.GRAY_STAINED_GLASS_PANE);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            boolean top = slot < 9;
            boolean bottom = slot >= inventory.getSize() - 9;
            boolean left = slot % 9 == 0;
            boolean right = slot % 9 == 8;
            if (top || bottom || left || right) {
                inventory.setItem(slot, border.clone());
            }
        }
    }

    private String statusLine(String label, boolean state) {
        return ChatColor.GRAY + label + ": " + (state ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF");
    }

    private String stripColor(String input) {
        String stripped = ChatColor.stripColor(input);
        return stripped == null ? "" : stripped;
    }

    private ConversationQuest questAt(List<ConversationQuest> quests, int page, int slot) {
        int index = page * PAGE_SIZE + slot;
        if (index < 0 || index >= quests.size()) {
            return null;
        }
        return quests.get(index);
    }

    private int normalizePage(int requestedPage, int totalEntries) {
        if (totalEntries <= 0) {
            return 0;
        }
        int maxPage = (totalEntries - 1) / PAGE_SIZE;
        return Math.max(0, Math.min(requestedPage, maxPage));
    }

    private interface QuestHolder extends InventoryHolder {
        @Override
        default Inventory getInventory() {
            return null;
        }
    }

    private record PlayerMenuHolder(int page) implements QuestHolder {
    }

    private record AdminMenuHolder(int page) implements QuestHolder {
    }

    private record EditHolder(String questId) implements QuestHolder {
    }

    private record RewardsHolder(String questId, int page) implements QuestHolder {
    }

    private record Prompt(PromptType type, String questId, int page) {
    }

    private enum PromptType {
        CREATE_QUEST,
        SET_NAME,
        SET_DESCRIPTION,
        SET_STARTER,
        SET_TARGET,
        ADD_REWARD
    }
}
