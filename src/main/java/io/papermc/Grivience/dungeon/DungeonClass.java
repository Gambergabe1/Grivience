package io.papermc.Grivience.dungeon;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

import java.util.List;

/**
 * Represents the 5 core dungeon classes from Hypixel Skyblock.
 */
public enum DungeonClass {
    HEALER("Healer", NamedTextColor.LIGHT_PURPLE, Material.POTION, List.of(
            "&7A supportive class that focuses on",
            "&7keeping teammates alive.",
            "",
            "&6Passives:",
            "&e- Self-Healing: &7Heal yourself for 1% max HP/s.",
            "&e- Renew: &7Heal teammates for 0.5% max HP/s.",
            "",
            "&6Abilities:",
            "&e- Healing Circle: &7Create a circle that heals.",
            "&e- Wish: &7Instantly heal all teammates."
    )),
    MAGE("Mage", NamedTextColor.AQUA, Material.BLAZE_ROD, List.of(
            "&7A glass cannon class that uses mana",
            "&7to deal massive area damage.",
            "",
            "&6Passives:",
            "&e- Mana Affinity: &7Increases mana by 50%.",
            "&e- Mana Regen: &7Regenerate mana 2x faster.",
            "",
            "&6Abilities:",
            "&e- Guided Sheep: &7Launch a target-seeking sheep.",
            "&e- Thunderstorm: &7Call down lightning bolts."
    )),
    BERSERK("Berserk", NamedTextColor.RED, Material.IRON_SWORD, List.of(
            "&7A melee-focused class that gains",
            "&7strength as it deals damage.",
            "",
            "&6Passives:",
            "&e- Bloodlust: &7Heal 1% max HP on melee kill.",
            "&e- Lust for Blood: &7Melee damage increases by 1% per hit.",
            "",
            "&6Abilities:",
            "&e- Throwing Axe: &7Launch an axe at enemies.",
            "&e- Ragnarok: &7Briefly double your strength."
    )),
    ARCHER("Archer", NamedTextColor.GOLD, Material.BOW, List.of(
            "&7A ranged class that excels at",
            "&7dealing high single-target damage.",
            "",
            "&6Passives:",
            "&e- Double Shot: &710% chance to shoot 2 arrows.",
            "&e- Precise Shot: &7Bows deal +50% damage.",
            "",
            "&6Abilities:",
            "&e- Explosive Shot: &7Next arrow deals area damage.",
            "&e- Rapid Fire: &7Shoot arrows at high speed."
    )),
    TANK("Tank", NamedTextColor.GRAY, Material.IRON_CHESTPLATE, List.of(
            "&7A defensive class that absorbs damage",
            "&7for its teammates.",
            "",
            "&6Passives:",
            "&e- Protection: &7Increases defense by 50%.",
            "&e- Diversion: &7Absorb 20% of damage taken by nearby allies.",
            "",
            "&6Abilities:",
            "&e- Seismic Wave: &7Create a wave that knocks back enemies.",
            "&e- Castle: &7Become invulnerable for 5 seconds."
    ));

    private final String displayName;
    private final NamedTextColor color;
    private final Material icon;
    private final List<String> description;

    DungeonClass(String displayName, NamedTextColor color, Material icon, List<String> description) {
        this.displayName = displayName;
        this.color = color;
        this.icon = icon;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public NamedTextColor getColor() {
        return color;
    }

    public Material getIcon() {
        return icon;
    }

    public List<String> getDescription() {
        return description;
    }
}
