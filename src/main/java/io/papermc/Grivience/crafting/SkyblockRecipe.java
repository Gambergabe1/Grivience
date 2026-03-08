package io.papermc.Grivience.crafting;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.NamespacedKey;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SkyblockRecipe {

    private final NamespacedKey key;
    private final String name;
    private final ItemStack result;
    private final RecipeCategory category;
    private final RecipeShape shape;
    private final Map<Character, ItemStack> ingredients;
    private final String[] shapePattern;
    private final List<String> lore;
    private final int collectionTierRequired;
    private final String collectionId;
    private final boolean discoverable;
    private final boolean secret;
    private final int requiredSkyblockLevel;

    private SkyblockRecipe(Builder builder) {
        this.key = builder.key;
        this.name = builder.name;
        this.result = builder.result;
        this.category = builder.category;
        this.shape = builder.shape;
        this.ingredients = Collections.unmodifiableMap(new HashMap<>(builder.ingredients));
        this.shapePattern = builder.shapePattern;
        this.lore = Collections.unmodifiableList(builder.lore);
        this.collectionTierRequired = builder.collectionTierRequired;
        this.collectionId = builder.collectionId;
        this.discoverable = builder.discoverable;
        this.secret = builder.secret;
        this.requiredSkyblockLevel = builder.requiredSkyblockLevel;
    }

    public NamespacedKey getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public ItemStack getResult() {
        return result;
    }

    public RecipeCategory getCategory() {
        return category;
    }

    public RecipeShape getShape() {
        return shape;
    }

    public Map<Character, ItemStack> getIngredients() {
        return ingredients;
    }

    public String[] getShapePattern() {
        return shapePattern;
    }

    public List<String> getLore() {
        return lore;
    }

    public int getCollectionTierRequired() {
        return collectionTierRequired;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public boolean isDiscoverable() {
        return discoverable;
    }

    public boolean isSecret() {
        return secret;
    }

    public int getRequiredSkyblockLevel() {
        return requiredSkyblockLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkyblockRecipe that = (SkyblockRecipe) o;
        return key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private NamespacedKey key;
        private String name;
        private ItemStack result;
        private RecipeCategory category = RecipeCategory.SPECIAL; // Default
        private RecipeShape shape = RecipeShape.SHAPED_3X3;
        private String[] shapePattern;
        private final Map<Character, ItemStack> ingredients = new HashMap<>();
        private List<String> lore = Collections.emptyList();
        private int collectionTierRequired = 0;
        private String collectionId = null;
        private boolean discoverable = true;
        private boolean secret = false;
        private int requiredSkyblockLevel = 0;

        public Builder key(NamespacedKey key) {
            this.key = key;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder result(ItemStack result) {
            this.result = result;
            return this;
        }

        public Builder category(RecipeCategory category) {
            this.category = category;
            return this;
        }

        public Builder shape(RecipeShape shape) {
            this.shape = shape;
            return this;
        }

        public Builder shapeLines(String... lines) {
            this.shapePattern = lines;
            return this;
        }

        public Builder ingredient(char symbol, ItemStack item) {
            this.ingredients.put(symbol, item);
            return this;
        }

        public Builder lore(List<String> lore) {
            this.lore = lore;
            return this;
        }

        public Builder collectionTierRequired(int collectionTierRequired) {
            this.collectionTierRequired = collectionTierRequired;
            return this;
        }

        public Builder collectionId(String collectionId) {
            this.collectionId = collectionId;
            return this;
        }

        public Builder discoverable(boolean discoverable) {
            this.discoverable = discoverable;
            return this;
        }

        public Builder secret(boolean secret) {
            this.secret = secret;
            return this;
        }

        public Builder requiredSkyblockLevel(int requiredSkyblockLevel) {
            this.requiredSkyblockLevel = requiredSkyblockLevel;
            return this;
        }

        public SkyblockRecipe build() {
            Objects.requireNonNull(key, "Recipe key cannot be null");
            Objects.requireNonNull(name, "Recipe name cannot be null");
            Objects.requireNonNull(result, "Recipe result cannot be null");
            Objects.requireNonNull(shape, "Recipe shape cannot be null");
            // Ingredients can be empty for shapeless or special recipes, but not null.

            return new SkyblockRecipe(this);
        }
    }
}
