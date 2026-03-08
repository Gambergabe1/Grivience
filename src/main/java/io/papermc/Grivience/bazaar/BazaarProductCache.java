package io.papermc.Grivience.bazaar;

import org.bukkit.Material;

import java.util.*;

/**
 * Cache for Bazaar products with quick lookup capabilities.
 */
public final class BazaarProductCache {
    private final BazaarShopManager shopManager;
    
    public BazaarProductCache(BazaarShopManager shopManager) {
        this.shopManager = shopManager;
    }
    
    /**
     * Get a product by ID.
     */
    public BazaarProduct getProduct(String productId) {
        return shopManager.getProduct(productId);
    }
    
    /**
     * Get a product by custom item key.
     */
    public BazaarProduct getProductByCustomKey(String customKey) {
        return shopManager.getProductByCustomKey(customKey);
    }
    
    /**
     * Get a product by Material.
     */
    public BazaarProduct getProductByMaterial(Material material) {
        return shopManager.getProductByMaterial(material);
    }
    
    /**
     * Get all products.
     */
    public Collection<BazaarProduct> getAllProducts() {
        return shopManager.getAllProducts();
    }
    
    /**
     * Get products by category.
     */
    public List<BazaarProduct> getProductsByCategory(BazaarProduct.BazaarCategory category) {
        return shopManager.getProductsByCategory(category);
    }
    
    /**
     * Get products by subcategory.
     */
    public List<BazaarProduct> getProductsBySubcategory(BazaarProduct.BazaarSubcategory subcategory) {
        return shopManager.getAllProducts().stream()
            .filter(p -> p.getSubcategory() == subcategory)
            .toList();
    }
    
    /**
     * Search products by name.
     */
    public List<BazaarProduct> searchProducts(String query) {
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        return shopManager.getAllProducts().stream()
            .filter(p -> p.getProductName().toLowerCase(Locale.ROOT).contains(lowerQuery))
            .toList();
    }
    
    /**
     * Get all categories.
     */
    public List<BazaarProduct.BazaarCategory> getCategories() {
        return List.of(BazaarProduct.BazaarCategory.values());
    }
    
    /**
     * Get all subcategories for a category.
     */
    public List<BazaarProduct.BazaarSubcategory> getSubcategories(BazaarProduct.BazaarCategory category) {
        return Arrays.stream(BazaarProduct.BazaarSubcategory.values())
            .filter(s -> s.getParent() == category)
            .toList();
    }
}
