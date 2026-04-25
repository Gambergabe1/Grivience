
IMPORTANT: The file content has been truncated.
Status: Showing lines 2800-3379 of 3379 total lines.
Action: To read more of the file, you can use the 'start_line' and 'end_line' parameters in a subsequent 'read_file' call. For example, to read the next section of the file, use start_line: 3380.

--- FILE CONTENT (truncated) ---
                        "&7Wear 4 pieces: &5Spell Efficiency &8(Spells cost 15% less mana)",
                        "",
                        "&5RARE &8LEGGINGS"
                ));

        addCustomArmorPieceDefaults(config, base + "pieces.boots.",
                "LEATHER_BOOTS", // Can be dyed purple
                "&5Arcane Boots",
                0, // Armor
                5.0D, // Health
                true, // Glowing
                List.of(
                        "&7Part of the &5Arcane Weaver &7set.",
                        "&8Silent steps for spellcasting.",
                        "",
                        "&7Defense: &a+0",
                        "&7Health: &a+5",
                        "",
                        "&6Set Bonus: &5Mystic Flow",
                        "&7Wear 2 pieces: &5Mana Regeneration &8(+10% per second)",
                        "&7Wear 4 pieces: &5Spell Efficiency &8(Spells cost 15% less mana)",
                        "",
                        "&5RARE &8BOOTS"
                ));
    }

    private void addCustomArmorPieceDefaults(
            org.bukkit.configuration.file.FileConfiguration config,
            String base,
            String material,
            String displayName,
            int armor,
            double health,
            boolean glowing,
            List<String> lore
    ) {
        config.addDefault(base + "material", material);
        config.addDefault(base + "display-name", displayName);
        config.addDefault(base + "armor", armor);
        config.addDefault(base + "health", health);
        config.addDefault(base + "glowing", glowing);
        config.addDefault(base + "lore", lore);
    }

    private void addFarmingArmorPieceDefaults(
            org.bukkit.configuration.file.FileConfiguration config,
            String base,
            String material,
            String displayName,
            int armor,
            double health,
            double farmingFortune,
            double mana,
            String color,
            List<String> lore
    ) {
        addCustomArmorPieceDefaults(config, base, material, displayName, armor, health, false, lore);
        if (farmingFortune > 0.0D) {
            config.addDefault(base + "farming-fortune", farmingFortune);
        }
        if (mana > 0.0D) {
            config.addDefault(base + "mana", mana);
        }
        if (color != null && !color.isBlank()) {
            config.addDefault(base + "color", color);
        }
    }

    private void addVoidMinerPieceDefaults(
            org.bukkit.configuration.file.FileConfiguration config,
            String base,
            String material,
            String displayName,
            int armor,
            double health,
            List<String> lore
    ) {
        config.addDefault(base + "material", material);
        config.addDefault(base + "display-name", displayName);
        config.addDefault(base + "armor", armor);
        config.addDefault(base + "health", health);
        config.addDefault(base + "glowing", true);
        config.addDefault(base + "lore", lore);
    }

    private void applySkyblockLevelingDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "skyblock-leveling.";
        config.addDefault(base + "xp-per-level", 100L);
        config.addDefault(base + "max-level", 521);
        config.addDefault(base + "notify-xp-gain", true);
        config.addDefault(base + "auto-save-interval-seconds", 300);

        // Skill leveling settings (Skyblock-style)
        config.addDefault(base + "skill-leveling.enabled", true);
        config.addDefault(base + "skill-leveling.max-level", 60);

        // Skyblock XP rewards for leveling up skills (Skyblock-style defaults)
        config.addDefault(base + "skill-level-xp-rewards.level-1-to-10", 5L);
        config.addDefault(base + "skill-level-xp-rewards.level-11-to-25", 10L);
        config.addDefault(base + "skill-level-xp-rewards.level-26-to-50", 20L);
        config.addDefault(base + "skill-level-xp-rewards.level-51-to-60", 30L);

        config.addDefault(base + "action-xp.combat-kill", 0L);
        config.addDefault(base + "action-xp.mining-ore", 0L);
        config.addDefault(base + "action-xp.foraging-log", 0L);
        config.addDefault(base + "action-xp.farming-harvest", 0L);
        config.addDefault(base + "action-xp.fishing-catch", 0L);
        config.addDefault(base + "action-xp.quest-complete", 0L);
        config.addDefault(base + "action-xp.dungeon-complete", 0L);
        config.addDefault(base + "action-xp.island-create", 0L);
        config.addDefault(base + "action-xp.island-upgrade", 0L);
        config.addDefault(base + "combat-skill-xp.base-per-kill", 1L);
        config.addDefault(base + "combat-skill-xp.extra-every-levels", 3);
        config.addDefault(base + "combat-skill-xp.extra-per-step", 1L);
        config.addDefault(base + "combat-skill-xp.empowered-bonus", 2L);
        config.addDefault(base + "combat-skill-xp.boss-base", 24L);
        config.addDefault(base + "combat-skill-xp.boss-extra-every-levels", 2);
        config.addDefault(base + "combat-skill-xp.boss-extra-per-step", 2L);
        config.addDefault(base + "combat-skill-xp.boss-default-level", 15);
        config.addDefault(base + "combat-skill-xp.custom-boss-ids", List.of("crimson_warden"));

        config.addDefault(base + "skill-actions-per-level.combat", 40L);
        config.addDefault(base + "skill-actions-per-level.mining", 140L);
        config.addDefault(base + "skill-actions-per-level.foraging", 140L);
        config.addDefault(base + "skill-actions-per-level.farming", 120L);
        config.addDefault(base + "skill-actions-per-level.fishing", 20L);
        config.addDefault(base + "skill-level-cap", 60);

        config.addDefault(base + "dungeons.catacombs-runs-per-level", 3L);
        config.addDefault(base + "dungeons.class-runs-per-level", 4L);
        config.addDefault(base + "dungeons.catacombs-level-cap", 50);
        config.addDefault(base + "dungeons.class-level-cap", 50);
        config.addDefault(base + "dungeons.class-level-xp-reward", 4L);
        config.addDefault(base + "dungeons.catacombs-level-xp-rewards.level-1-to-39", 20L);
        config.addDefault(base + "dungeons.catacombs-level-xp-rewards.level-40-to-50", 40L);
        config.addDefault(base + "dungeons.floor-completion-xp.entrance", 10L);
        config.addDefault(base + "dungeons.floor-completion-xp.floor-1", 10L);
        config.addDefault(base + "dungeons.floor-completion-xp.floor-2", 20L);
        config.addDefault(base + "dungeons.floor-completion-xp.floor-3", 20L);
        config.addDefault(base + "dungeons.floor-completion-xp.floor-4", 20L);
        config.addDefault(base + "dungeons.floor-completion-xp.floor-5", 30L);
        config.addDefault(base + "dungeons.floor-completion-xp.floor-6", 30L);
        config.addDefault(base + "dungeons.floor-completion-xp.floor-7", 30L);
        config.addDefault(base + "dungeons.floor-completion-xp.master-1", 50L);
        config.addDefault(base + "dungeons.floor-completion-xp.master-2", 50L);
        config.addDefault(base + "dungeons.floor-completion-xp.master-3", 50L);
        config.addDefault(base + "dungeons.floor-completion-xp.master-4", 50L);
        config.addDefault(base + "dungeons.floor-completion-xp.master-5", 50L);
        config.addDefault(base + "dungeons.floor-completion-xp.master-6", 50L);
        config.addDefault(base + "dungeons.floor-completion-xp.master-7", 50L);
        config.addDefault(base + "dungeons.objective-xp.first-s-rank", 20L);
        config.addDefault(base + "dungeons.objective-xp.first-a-rank", 10L);
        config.addDefault(base + "dungeons.objective-xp.score-200", 5L);
        config.addDefault(base + "dungeons.objective-xp.score-250", 10L);
        config.addDefault(base + "dungeons.objective-xp.score-300", 15L);

        config.addDefault(base + "bestiary.tier-size", 10);
        config.addDefault(base + "bestiary.family-tier-xp", 1L);
        config.addDefault(base + "bestiary.milestone-every-tiers", 10);
        config.addDefault(base + "bestiary.milestone-every-kills", 10);
        config.addDefault(base + "bestiary.milestone-xp", 10L);

        config.addDefault(base + "level-rewards.health-per-level", 5);
        config.addDefault(base + "level-rewards.strength-per-5-levels", 1);
        addLevelUnlockDefault(config, 3, "Community Shop");
        addLevelUnlockDefault(config, 5, "Auction House");
        addLevelUnlockDefault(config, 7, "Bazaar");
        addLevelUnlockDefault(config, 9, "Museum");
        addLevelUnlockDefault(config, 10, "Depth Strider Enchantment");
        addLevelUnlockDefault(config, 15, "Garden");
        addLevelUnlockDefault(config, 20, "Hex");
        addLevelUnlockDefault(config, 25, "Skill Average 15");
        addLevelUnlockDefault(config, 50, "Skill Average 20");
        addLevelUnlockDefault(config, 70, "Garden Level 5");
        addLevelUnlockDefault(config, 100, "Skill Average 25");
        addLevelUnlockDefault(config, 120, "Garden Level 10");
        addLevelUnlockDefault(config, 150, "Skill Average 30");
        addLevelUnlockDefault(config, 200, "Skill Average 35");
        addLevelUnlockDefault(config, 240, "Garden Level 15");
        addLevelUnlockDefault(config, 250, "Skill Average 40");
        addLevelUnlockDefault(config, 280, "Garden Level 20");
        addLevelUnlockDefault(config, 300, "Skill Average 45");
        addLevelUnlockDefault(config, 350, "Skill Average 50");
        addLevelUnlockDefault(config, 400, "Skill Average 55");
        addLevelUnlockDefault(config, 450, "Skill Average 60");
        addLevelUnlockDefault(config, 500, "Skill Average 65");

        addSkyblockTrackDefault(config, "core", "&6Core", "NETHER_STAR", "category_xp.core",
                List.of(1000L, 5000L, 10000L, 15000L, 17089L), List.of(0L, 0L, 0L, 0L, 0L),
                List.of("&7Profile and account progression."));
        addSkyblockTrackDefault(config, "event", "&dEvent", "FIREWORK_STAR", "category_xp.event",
                List.of(250L, 1000L, 2000L, 3000L, 3391L), List.of(0L, 0L, 0L, 0L, 0L),
                List.of("&7Seasonal event progression."));
        addSkyblockTrackDefault(config, "dungeon", "&5Dungeon", "WITHER_SKELETON_SKULL", "category_xp.dungeon",
                List.of(250L, 1000L, 2000L, 3200L, 3905L), List.of(0L, 0L, 0L, 0L, 0L),
                List.of("&7Catacombs and class progression."));
        addSkyblockTrackDefault(config, "essence", "&bEssence", "AMETHYST_SHARD", "category_xp.essence",
                List.of(100L, 300L, 600L, 900L, 1085L), List.of(0L, 0L, 0L, 0L, 0L),
                List.of("&7Essence-related progression."));
        addSkyblockTrackDefault(config, "slaying", "&cSlaying", "IRON_SWORD", "category_xp.slaying",
                List.of(500L, 2000L, 4000L, 6000L, 7410L), List.of(0L, 0L, 0L, 0L, 0L),
                List.of("&7Bestiary and boss progression."));
        addSkyblockTrackDefault(config, "skill", "&aSkill Related", "ENCHANTED_BOOK", "category_xp.skill",
                List.of(500L, 2500L, 5000L, 9000L, 10818L), List.of(0L, 0L, 0L, 0L, 0L),
                List.of("&7Skill-level progression."));
        addSkyblockTrackDefault(config, "misc", "&eMisc", "COMPASS", "category_xp.misc",
                List.of(250L, 1000L, 2500L, 4500L, 5782L), List.of(0L, 0L, 0L, 0L, 0L),
                List.of("&7Misc progression."));
        addSkyblockTrackDefault(config, "story", "&9Story", "WRITABLE_BOOK", "category_xp.story",
                List.of(100L, 400L, 900L, 1300L, 1590L), List.of(0L, 0L, 0L, 0L, 0L),
                List.of("&7Story progression."));
    }

    private void applySkyblockPetDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "skyblock-pets.";
        config.addDefault(base + "enabled", true);

        // Skyblock-accurate defaults: pets contribute stats via the SkyblockCombatStatsService, not via Bukkit potion effects/attributes.
        // Legacy effects are still supported for servers that configured pets.yml that way, but are disabled by default.
        config.addDefault(base + "legacy.potion-effects.enabled", false);
        config.addDefault(base + "legacy.attribute-modifiers.enabled", false);
        config.addDefault(base + "legacy.cleanup-on-join", true);
        config.addDefault(base + "legacy.cleanup-min-duration-ticks", 20 * 60 * 8);

        // Skill XP -> Pet XP (server-tunable). If these are unset, PetSkillXpListener falls back to skyblock-leveling.action-xp.*.
        config.addDefault(base + "skill-xp.combat-kill", 50L);
        config.addDefault(base + "skill-xp.mining-ore", 50L);
        config.addDefault(base + "skill-xp.foraging-log", 50L);
        config.addDefault(base + "skill-xp.farming-harvest", 50L);
        config.addDefault(base + "skill-xp.fishing-catch", 200L);

        // Skyblock-like multipliers for pet EXP earned from Skill XP.
        config.addDefault(base + "multipliers.mining-fishing", 1.5D);
        config.addDefault(base + "multipliers.non-matching", 1.0D / 3.0D);
        config.addDefault(base + "multipliers.non-matching-alchemy-enchanting", 1.0D / 12.0D);
    }

    private void applyMinionDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "minions.";
        config.addDefault(base + "enabled", true);
        config.addDefault(base + "tick-interval-ticks", 20L);
        config.addDefault(base + "auto-save-interval-seconds", 60);
        config.addDefault(base + "base-island-limit", 5);
        config.addDefault(base + "limit-per-island-level", 1);
        config.addDefault(base + "speed-multiplier", 1.0D);
        config.addDefault(base + "storage-multiplier", 1.0D);
        config.addDefault(base + "max-catchup-hours", 24);
        config.addDefault(base + "max-actions-per-tick", 3600);
    }

    private void applyWizardTowerDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "wizard-tower.";
        config.addDefault(base + "enabled", true);
        config.addDefault(base + "znpc-id", "");
        config.addDefault(base + "interaction-prompt-cooldown-seconds", 3L);

        String apprentice = base + "tiers.apprentice.";
        config.addDefault(apprentice + "display-name", "&aApprentice Blessing");
        config.addDefault(apprentice + "cost", 15000.0D);
        config.addDefault(apprentice + "speed-amplifier", 0);
        config.addDefault(apprentice + "jump-amplifier", -1);
        config.addDefault(apprentice + "health-bonus", 2.0D);
        config.addDefault(apprentice + "combat-bonus", 2.0D);
        config.addDefault(apprentice + "intelligence-bonus", 4.0D);

        String adept = base + "tiers.adept.";
        config.addDefault(adept + "display-name", "&bAdept Blessing");
        config.addDefault(adept + "cost", 30000.0D);
        config.addDefault(adept + "speed-amplifier", 0);
        config.addDefault(adept + "jump-amplifier", 0);
        config.addDefault(adept + "health-bonus", 3.0D);
        config.addDefault(adept + "combat-bonus", 3.0D);
        config.addDefault(adept + "intelligence-bonus", 6.0D);

        String master = base + "tiers.master.";
        config.addDefault(master + "display-name", "&dMaster Blessing");
        config.addDefault(master + "cost", 50000.0D);
        config.addDefault(master + "speed-amplifier", 1);
        config.addDefault(master + "jump-amplifier", 1);
        config.addDefault(master + "health-bonus", 5.0D);
        config.addDefault(master + "combat-bonus", 5.0D);
        config.addDefault(master + "intelligence-bonus", 10.0D);
    }

    private void applyFarmingContestDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "farming-contests.";
        config.addDefault(base + "enabled", true);
        config.addDefault(base + "minimum-farming-level", 10);
        config.addDefault(base + "minimum-collection-increase", 100L);
        config.addDefault(base + "schedule.interval-minutes", 90);
        config.addDefault(base + "schedule.duration-minutes", 20);
        config.addDefault(base + "schedule.offset-minutes", 0);
        config.addDefault(base + "schedule.seed", 91451L);
        config.addDefault(base + "crop-pool", List.of(
                "wheat",
                "carrot",
                "potato",
                "nether_wart",
                "sugar_cane",
                "cactus",
                "cocoa_beans",
                "melon",
                "pumpkin",
                "mushroom"
        ));
        config.addDefault(base + "rewards.thresholds.bronze-percent", 0.60D);
        config.addDefault(base + "rewards.thresholds.silver-percent", 0.30D);
        config.addDefault(base + "rewards.thresholds.gold-percent", 0.10D);
        config.addDefault(base + "rewards.thresholds.platinum-percent", 0.05D);
        config.addDefault(base + "rewards.thresholds.diamond-percent", 0.02D);
        config.addDefault(base + "rewards.tickets.participation", 1L);
        config.addDefault(base + "rewards.tickets.bronze", 10L);
        config.addDefault(base + "rewards.tickets.silver", 15L);
        config.addDefault(base + "rewards.tickets.gold", 25L);
        config.addDefault(base + "rewards.tickets.platinum", 30L);
        config.addDefault(base + "rewards.tickets.diamond", 35L);
        config.addDefault(base + "announcements.broadcast-start", true);
        config.addDefault(base + "announcements.broadcast-end", true);
        config.addDefault(base + "announcements.requirement-warning-cooldown-seconds", 15L);
    }

    private void addLevelUnlockDefault(
            org.bukkit.configuration.file.FileConfiguration config,
            int level,
            String unlock
    ) {
        config.addDefault("skyblock-leveling.level-rewards.feature-unlocks." + level, List.of(unlock));
    }

    private void addSkyblockTrackDefault(
            org.bukkit.configuration.file.FileConfiguration config,
            String id,
            String displayName,
            String icon,
            String counterKey,
            List<Long> milestones,
            List<Long> rewards,
            List<String> lore
    ) {
        String path = "skyblock-leveling.tracks." + id + ".";
        config.addDefault(path + "display-name", displayName);
        config.addDefault(path + "icon", icon);
        config.addDefault(path + "counter-key", counterKey);
        config.addDefault(path + "milestones", milestones);
        config.addDefault(path + "rewards", rewards);
        config.addDefault(path + "lore", lore);
    }

    private void applyScoreboardDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "scoreboard.custom.";
        config.addDefault(base + "enabled", true);
        config.addDefault(base + "title", "&e&lSkyblock");
        config.addDefault(base + "update-ticks", 20L);
        config.addDefault(base + "show-time-line", false);
        config.addDefault(base + "use-skyblock-calendar", true);
        config.addDefault(base + "profile-label", "Profile");
        config.addDefault(base + "bits-default", 0L);
        config.addDefault(base + "default-objective", "Reach Skyblock Level 5");
        config.addDefault(base + "footer", "&eSkyblock");
    }

    private void applyPerformanceMonitorDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        String base = "performance-monitor.";
        config.addDefault(base + "enabled", true);
        config.addDefault(base + "smoothing-factor", 0.20D);
        config.addDefault(base + "thresholds.warning-mspt", 47.5D);
        config.addDefault(base + "thresholds.critical-mspt", 55.0D);
        config.addDefault(base + "thresholds.recovery-mspt", 45.0D);
        config.addDefault(base + "transition-samples.degraded", 40);
        config.addDefault(base + "transition-samples.critical", 20);
        config.addDefault(base + "transition-samples.recovery", 60);
        config.addDefault(base + "adaptive-mitigation.enabled", true);
        config.addDefault(base + "logging.log-state-changes", true);
    }

    private void applyResourcePackDefaults(org.bukkit.configuration.file.FileConfiguration config) {
        config.addDefault("resource-pack.enabled", true);
        config.addDefault("resource-pack.url", "");
        config.addDefault("resource-pack.hash", "");
        config.addDefault("resource-pack.required", true);
        config.addDefault("resource-pack.prompt", "&eThis pack enables custom item textures. Accept?");
        config.addDefault("resource-pack.local.enabled", true);
        config.addDefault("resource-pack.local.file", "resource-pack.zip");
        config.addDefault("resource-pack.local.host", "localhost");
        config.addDefault("resource-pack.local.port", 8765);
        // Default model IDs for custom weapons (override in config if desired)
        int model = 1001;
        String base = "resource-pack.models.";
        config.addDefault(base + "wardens_cleaver", model++);
        config.addDefault(base + "oni_cleaver", model++);
        config.addDefault(base + "tengu_galeblade", model++);
        config.addDefault(base + "tengu_stormbow", model++);
        config.addDefault(base + "tengu_shortbow", model++);
        config.addDefault(base + "kappa_tidebreaker", model++);
        config.addDefault(base + "onryo_spiritblade", model++);
        config.addDefault(base + "onryo_shortbow", model++);
        config.addDefault(base + "jorogumo_stinger", model++);
        config.addDefault(base + "jorogumo_shortbow", model++);
        config.addDefault(base + "kitsune_fang", model++);
        config.addDefault(base + "kitsune_dawnbow", model++);
        config.addDefault(base + "kitsune_shortbow", model++);
        config.addDefault(base + "gashadokuro_nodachi", model++);
        config.addDefault(base + "flying_raijin", model++);
        // Optional name remaps for packs that select models by item name (e.g., Blades of Majestica)
        String names = "resource-pack.name-map.";
        config.addDefault(names + "wardens_cleaver", "Warden's Cleaver");
        config.addDefault(names + "oni_cleaver", "Demon Lord's Sword");
        config.addDefault(names + "tengu_galeblade", "Chrono Blade");
        config.addDefault(names + "tengu_stormbow", "Crescent Rose");
        config.addDefault(names + "tengu_shortbow", "Amethyst Shuriken");
        config.addDefault(names + "kappa_tidebreaker", "Ocean's Rage");
        config.addDefault(names + "onryo_spiritblade", "Ashura's Blade");
        config.addDefault(names + "onryo_shortbow", "Cyber Katana");
        config.addDefault(names + "jorogumo_stinger", "Demonic Blade");
        config.addDefault(names + "jorogumo_shortbow", "Demonic Cleaver");
        config.addDefault(names + "kitsune_fang", "Divine Justice");
        config.addDefault(names + "kitsune_dawnbow", "Edge Of The Astral Plane");
        config.addDefault(names + "kitsune_shortbow", "Ancient Royal Great Sword");
        config.addDefault(names + "gashadokuro_nodachi", "Demon Lord's Great Axe");
        config.addDefault(names + "flying_raijin", "Creation Splitter");
    }

    public io.papermc.Grivience.dungeon.DungeonManager getDungeonManager() {
        return dungeonManager;
    }

    public io.papermc.Grivience.skills.SkyblockSkillManager getSkyblockSkillManager() {
        return skyblockSkillManager;
    }

    public SkyblockMenuManager getSkyblockMenuManager() {
        return skyblockMenuManager;
    }

    public io.papermc.Grivience.nick.NickManager getNickManager() {
        return nickManager;
    }

    public io.papermc.Grivience.item.StaffManager getStaffManager() {
        return staffManager;
    }

    public io.papermc.Grivience.enchantment.EnchantmentManager getEnchantmentManager() {
        return enchantmentManager;
    }

    public MiningSystemManager getMiningSystemManager() {
        return miningSystemManager;
    }

    public MiningEventManager getMiningEventManager() {
        return miningEventManager;
    }

    public WizardTowerManager getWizardTowerManager() {
        return wizardTowerManager;
    }

    public InspectorShopGui getInspectorShopGui() {
        return inspectorShopGui;
    }

    public NpcSellShopGui getNpcSellShopGui() {
        return npcSellShopGui;
    }

    public DrillMechanicGui getDrillMechanicGui() {
        return drillMechanicGui;
    }

    public GlobalEventManager getGlobalEventManager() {
        return globalEventManager;
    }

    public SkyblockCombatEngine getSkyblockCombatEngine() {
        return skyblockCombatEngine;
    }

    public ServerPerformanceMonitor getServerPerformanceMonitor() {
        return serverPerformanceMonitor;
    }

    public CustomItemService getCustomItemService() {
        return customItemService;
    }

    public org.bukkit.NamespacedKey getGrapplingHookKey() {
        return grapplingHookKey;
    }

    private void ensureNewbieMinesZone() {
        if (zoneManager == null) return;

        String id = "newbie_mines";
        io.papermc.Grivience.zone.Zone zone = zoneManager.getZone(id);
        if (zone == null) {
            zone = zoneManager.createZone(id, "Newbie Mines", "§bNewbie Mines");
        }

        if (zone != null) {
            String worldName = "Hub 2";
            org.bukkit.World hub2 = getServer().getWorld(worldName);
            zone.setWorld(worldName);
            
            // Safe location creation even if world is not yet loaded
            zone.setPos1(new org.bukkit.Location(hub2, -86, 41, 193));
            zone.setPos2(new org.bukkit.Location(hub2, 76, 78, 305));
            zone.setPriority(50);
            zone.setColor(org.bukkit.ChatColor.AQUA);
            zoneManager.saveZones();
        }
    }

    private void ensureOaklysWoodDepoZone() {
        if (zoneManager == null) return;

        String id = "oaklys_wood_depo";
        io.papermc.Grivience.zone.Zone zone = zoneManager.getZone(id);
        if (zone == null) {
            zone = zoneManager.createZone(id, "Oaklys Wood Depo", "§6Oaklys Wood Depo");
        }

        if (zone != null) {
            String worldName = "Hub 2";
            org.bukkit.World hub2 = getServer().getWorld(worldName);
            zone.setWorld(worldName);
            
            zone.setPos1(new org.bukkit.Location(hub2, 195, 58, -96));
            zone.setPos2(new org.bukkit.Location(hub2, 414, 90, 70));
            zone.setPriority(50);
            zone.setColor(org.bukkit.ChatColor.GOLD);
            zoneManager.saveZones();
        }
    }

    private void ensureWheatAreaZone() {
        if (zoneManager == null) return;

        String id = "hub_wheat_area";
        io.papermc.Grivience.zone.Zone zone = zoneManager.getZone(id);
        if (zone == null) {
            zone = zoneManager.createZone(id, "Wheat Fields", "§eWheat Fields");
        }

        if (zone != null) {
            String worldName = "Hub 2";
            org.bukkit.World hub2 = getServer().getWorld(worldName);
            zone.setWorld(worldName);
            
            zone.setPos1(new org.bukkit.Location(hub2, -140, 113, 254));
            zone.setPos2(new org.bukkit.Location(hub2, -330, 58, 92));
            zone.setPriority(40);
            zone.setColor(org.bukkit.ChatColor.YELLOW);
            zoneManager.saveZones();
        }
    }

    private void ensureUndeadCemeteryZone() {
        if (zoneManager == null) return;

        String id = "undead_cemetery";
        io.papermc.Grivience.zone.Zone zone = zoneManager.getZone(id);
        if (zone == null) {
            zone = zoneManager.createZone(id, "Undead Cemetery", "§4Undead Cemetery");
        }

        if (zone != null) {
            String worldName = "Hub 2";
            org.bukkit.World hub2 = getServer().getWorld(worldName);
            zone.setWorld(worldName);
            
            zone.setPos1(new org.bukkit.Location(hub2, 101, 90, 257));
            zone.setPos2(new org.bukkit.Location(hub2, 448, 53, 135));
            zone.setPriority(50);
            zone.setColor(org.bukkit.ChatColor.DARK_RED);
            zoneManager.saveZones();
        }
    }
}

