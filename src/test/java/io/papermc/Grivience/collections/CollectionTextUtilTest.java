package io.papermc.Grivience.collections;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionTextUtilTest {

    @Test
    void sanitizeDisplayTextRepairsBrokenLegacySequences() {
        String raw = "\u00C2\u00A7eWheat \u00E2\u20AC\u00A2";

        assertEquals(ChatColor.YELLOW + "Wheat *", CollectionTextUtil.sanitizeDisplayText(raw));
    }

    @Test
    void searchableTextDropsColorCodesAndNormalizesSpacing() {
        String raw = ChatColor.GOLD + "Wheat-Collection";

        assertEquals("wheat collection", CollectionTextUtil.searchableText(raw));
    }

    @Test
    void collectionDefinitionSanitizesNameDescriptionAndSubcategory() {
        CollectionDefinition definition = CollectionDefinition.builder()
                .id("wheat")
                .name("\u00C2\u00A7eWheat")
                .description("\u00C2\u00A77Harvested from crops")
                .icon(Material.WHEAT)
                .category(CollectionCategory.FARMING)
                .subcategory("\u00C2\u00A76Garden")
                .trackedItems(List.of("wheat"))
                .tiers(List.of(new CollectionTier(1, 50, List.of())))
                .build();

        assertEquals(ChatColor.YELLOW + "Wheat", definition.getName());
        assertEquals(ChatColor.GRAY + "Harvested from crops", definition.getDescription());
        assertEquals(ChatColor.GOLD + "Garden", definition.getSubcategory());
    }

    @Test
    void progressBarUsesAsciiPipes() {
        String progressBar = CollectionTextUtil.createProgressBar(50.0D, 4);

        assertTrue(progressBar.contains("||"));
        assertEquals(ChatColor.GREEN + "||" + ChatColor.GRAY + "||", progressBar);
    }
}
