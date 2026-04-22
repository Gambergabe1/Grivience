package io.papermc.Grivience.collections;

import org.bukkit.ChatColor;

import java.util.Locale;

final class CollectionTextUtil {
    private static final String BROKEN_COLOR_PREFIX = "\u00C2" + ChatColor.COLOR_CHAR;
    private static final String BROKEN_BULLET = "\u00E2\u20AC\u00A2";
    private static final String BROKEN_BLOCK = "\u00E2\u2013\u02C6";

    private CollectionTextUtil() {
    }

    static String sanitizeDisplayText(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw == null ? "" : raw;
        }

        String sanitized = raw
                .replace(BROKEN_COLOR_PREFIX, String.valueOf(ChatColor.COLOR_CHAR))
                .replace(BROKEN_BULLET, "*")
                .replace(BROKEN_BLOCK, "|")
                .replace('\u2022', '*')
                .replace('\u2588', '|');
        return ChatColor.translateAlternateColorCodes('&', sanitized);
    }

    static String plainText(String raw) {
        String stripped = ChatColor.stripColor(sanitizeDisplayText(raw));
        return stripped == null ? "" : stripped.trim();
    }

    static String searchableText(String raw) {
        String normalized = plainText(raw)
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ');
        normalized = normalized.replaceAll("[^a-z0-9 ]", " ");
        return normalized.replaceAll("\\s+", " ").trim();
    }

    static String createProgressBar(double percent, int length) {
        double clamped = Math.max(0.0D, Math.min(100.0D, percent));
        int filled = (int) Math.round((clamped / 100.0D) * length);
        if (filled > length) {
            filled = length;
        }

        int empty = Math.max(0, length - filled);
        StringBuilder bar = new StringBuilder(length + 8);
        bar.append(ChatColor.GREEN);
        for (int i = 0; i < filled; i++) {
            bar.append('|');
        }
        bar.append(ChatColor.GRAY);
        for (int i = 0; i < empty; i++) {
            bar.append('|');
        }
        return bar.toString();
    }
}
