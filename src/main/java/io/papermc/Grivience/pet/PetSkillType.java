package io.papermc.Grivience.pet;

import java.util.Locale;

/**
 * Pet skill type used for Skyblock-like pet EXP rules.
 *
 * - ALL: gains full EXP from all skill types (not affected by non-matching penalty).
 * - Specific skills: gain full EXP only from matching skill type; other skill EXP is reduced.
 */
public enum PetSkillType {
    ALL,
    COMBAT,
    MINING,
    FORAGING,
    FARMING,
    FISHING,
    ENCHANTING,
    ALCHEMY,
    TAMING,
    HUNTING,
    DUNGEONEERING,
    CARPENTRY;

    public static PetSkillType parse(String raw, PetSkillType fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        try {
            return PetSkillType.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}


